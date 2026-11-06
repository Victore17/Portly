package victor.android.portly.domain.administracion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.data.repositorio.CredencialRepositorioRoom
import victor.android.portly.data.repositorio.InvitadoRepositorioRoom
import victor.android.portly.data.repositorio.QrInvitadoRepositorioRoom
import victor.android.portly.data.repositorio.RegistroAccesoRepositorioRoom
import victor.android.portly.data.repositorio.ResidenteRepositorioRoom
import victor.android.portly.data.seguridad.HasheadorPbkdf2
import victor.android.portly.domain.autenticacion.ResultadoLogin
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.qr.ValidadorQr
import victor.android.portly.domain.repositorio.SesionRepositorio
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ServicioAdministracionTest : BaseRoomTest() {

    private class RelojFijo(val ahora: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = this
        override fun instant() = ahora
    }

    private class SesionFalsa : SesionRepositorio {
        val actual = MutableStateFlow<Sesion?>(null)
        override fun observar() = actual
        override suspend fun guardar(sesion: Sesion) { actual.value = sesion }
        override suspend fun cerrar() { actual.value = null }
    }

    private val ahora = Instant.parse("2026-11-02T10:00:00Z")
    private lateinit var sesion: SesionFalsa
    private lateinit var residentesRepo: ResidenteRepositorioRoom
    private lateinit var registrosRepo: RegistroAccesoRepositorioRoom
    private lateinit var qrRepo: QrInvitadoRepositorioRoom
    private lateinit var auth: ServicioAutenticacion
    private lateinit var servicio: ServicioAdministracion

    @Before
    fun crear() {
        sesion = SesionFalsa()
        residentesRepo = ResidenteRepositorioRoom(residentes)
        registrosRepo = RegistroAccesoRepositorioRoom(registros)
        qrRepo = QrInvitadoRepositorioRoom(qrs)
        auth = ServicioAutenticacion(
            residentesRepo, CredencialRepositorioRoom(base.credencialDao()), HasheadorPbkdf2(), sesion,
        )
        servicio = ServicioAdministracion(
            residentesRepo, registrosRepo, qrRepo, InvitadoRepositorioRoom(invitados),
            sesion, auth, ValidadorQr(RelojFijo(ahora)),
        )
    }

    private suspend fun entrarComo(id: Long, admin: Boolean) = sesion.guardar(Sesion(id, admin))

    private suspend fun crearAdmin(contacto: String = "admin") =
        insertarResidente(nombre = "Admin $contacto", contacto = contacto, esAdmin = true)
            .also { entrarComo(it, true) }

    private suspend fun registro(residenteId: Long, momento: String, nombre: String = "X") =
        registrosRepo.registrar(
            RegistroAcceso(
                nombrePersona = nombre, tipo = TipoAcceso.ENTRADA,
                momento = Instant.parse(momento), residenteId = residenteId,
            ),
        )

    @Test
    fun un_residente_sin_rol_admin_no_puede_usar_ninguna_funcion() = runBlocking {
        val admin = insertarResidente(contacto = "a", esAdmin = true)
        val normal = insertarResidente(nombre = "Normal", contacto = "n")
        registro(admin, "2026-11-02T08:00:00Z")
        entrarComo(normal, admin = false)

        assertEquals(ResultadoAdmin.NoAutorizado, servicio.agregarResidente("Z", "d", "z", "pw"))
        assertEquals(ResultadoAdmin.NoAutorizado, servicio.editarResidente(residentesRepo.obtener(admin)!!))
        assertEquals(ResultadoAdmin.NoAutorizado, servicio.cambiarActivo(admin, false))
        assertEquals(ResultadoAdmin.NoAutorizado, servicio.cancelarQr(1))
        assertTrue(servicio.observarResidentes().first().isEmpty())
        assertTrue(servicio.observarRegistro().first().isEmpty())
        assertTrue(servicio.observarQrsActivos().first().isEmpty())
        assertTrue(residentesRepo.obtener(admin)!!.activo)
    }

    @Test
    fun una_sesion_con_admin_falso_o_desactualizado_tampoco_sirve() = runBlocking {
        val normal = insertarResidente(contacto = "n")
        entrarComo(normal, admin = true) // la sesión dice admin, la base no

        assertEquals(ResultadoAdmin.NoAutorizado, servicio.cambiarActivo(normal, false))
        sesion.cerrar()
        assertEquals(ResultadoAdmin.NoAutorizado, servicio.cambiarActivo(normal, false))
    }

    @Test
    fun un_admin_desactivado_pierde_el_acceso() = runBlocking {
        val a1 = insertarResidente(nombre = "A1", contacto = "1", esAdmin = true)
        insertarResidente(nombre = "A2", contacto = "2", esAdmin = true)
        entrarComo(a1, true)
        residentesRepo.cambiarActivo(a1, false)

        assertEquals(ResultadoAdmin.NoAutorizado, servicio.cambiarActivo(a1, true))
    }

    @Test
    fun agregar_un_residente_lo_guarda_y_puede_iniciar_sesion() = runBlocking {
        crearAdmin()

        val r = servicio.agregarResidente("  Beto  ", "Calle 2", "beto@mail.com", "secreta") as ResultadoAdmin.Exito

        val guardado = residentesRepo.obtener(r.residenteId!!)!!
        assertEquals("Beto", guardado.nombre)
        assertTrue(guardado.activo)
        assertFalse(guardado.esAdmin)
        assertTrue(auth.iniciarSesion("beto@mail.com", "secreta") is ResultadoLogin.Exito)
    }

    @Test
    fun agregar_valida_datos_y_contacto_duplicado() = runBlocking {
        crearAdmin("admin")

        assertEquals(ResultadoAdmin.DatosInvalidos, servicio.agregarResidente(" ", "d", "c", "pw"))
        assertEquals(ResultadoAdmin.DatosInvalidos, servicio.agregarResidente("N", "d", "c", " "))
        assertEquals(ResultadoAdmin.ContactoDuplicado, servicio.agregarResidente("N", "d", "admin", "pw"))
    }

    @Test
    fun editar_cambia_los_datos_pero_no_permite_un_contacto_ajeno() = runBlocking {
        crearAdmin("admin")
        val otro = insertarResidente(nombre = "Otro", contacto = "otro")

        val base = residentesRepo.obtener(otro)!!
        assertEquals(ResultadoAdmin.Exito(), servicio.editarResidente(base.copy(nombre = "Nuevo", domicilio = "Calle 9")))
        assertEquals("Nuevo", residentesRepo.obtener(otro)!!.nombre)
        assertEquals(ResultadoAdmin.ContactoDuplicado, servicio.editarResidente(base.copy(contacto = "admin")))
        assertEquals(ResultadoAdmin.NoExiste, servicio.editarResidente(base.copy(id = 999)))
    }

    @Test
    fun desactivar_y_reactivar_conserva_el_historial() = runBlocking {
        crearAdmin()
        val r = insertarResidente(nombre = "Beto", contacto = "b")
        registro(r, "2026-11-02T08:00:00Z", "Beto")

        assertEquals(ResultadoAdmin.Exito(), servicio.cambiarActivo(r, false))
        assertFalse(residentesRepo.obtener(r)!!.activo)
        assertEquals(1, servicio.observarRegistro().first().size)

        assertEquals(ResultadoAdmin.Exito(), servicio.cambiarActivo(r, true))
        assertTrue(residentesRepo.obtener(r)!!.activo)
        assertEquals(1, servicio.observarRegistro(FiltroRegistro(residenteId = r)).first().size)
    }

    @Test
    fun no_se_puede_desactivar_ni_quitar_el_rol_al_ultimo_admin() = runBlocking {
        val admin = crearAdmin()

        assertEquals(ResultadoAdmin.UltimoAdministrador, servicio.cambiarActivo(admin, false))
        assertEquals(
            ResultadoAdmin.UltimoAdministrador,
            servicio.editarResidente(residentesRepo.obtener(admin)!!.copy(esAdmin = false)),
        )
        assertTrue(residentesRepo.obtener(admin)!!.let { it.activo && it.esAdmin })
    }

    @Test
    fun con_dos_admins_se_puede_desactivar_uno_pero_no_el_que_queda() = runBlocking {
        val a1 = crearAdmin("1")
        val a2 = insertarResidente(nombre = "A2", contacto = "2", esAdmin = true)

        assertEquals(ResultadoAdmin.Exito(), servicio.cambiarActivo(a2, false))
        assertEquals(ResultadoAdmin.UltimoAdministrador, servicio.cambiarActivo(a1, false))
    }

    @Test
    fun un_admin_inactivo_no_cuenta_como_administrador_disponible() = runBlocking {
        val a1 = crearAdmin("1")
        insertarResidente(nombre = "A2", contacto = "2", esAdmin = true, activo = false)

        assertEquals(ResultadoAdmin.UltimoAdministrador, servicio.cambiarActivo(a1, false))
    }

    @Test
    fun los_filtros_del_registro_por_residente_y_fecha() = runBlocking {
        val admin = crearAdmin()
        val beto = insertarResidente(nombre = "Beto", contacto = "b")
        registro(admin, "2026-11-01T09:00:00Z")
        registro(beto, "2026-11-02T09:00:00Z")
        registro(beto, "2026-11-03T09:00:00Z")
        registro(admin, "2026-11-03T10:00:00Z")

        assertEquals(4, servicio.observarRegistro().first().size)
        assertEquals(2, servicio.observarRegistro(FiltroRegistro(residenteId = beto)).first().size)

        val dia3 = FiltroRegistro(
            desde = Instant.parse("2026-11-03T00:00:00Z"),
            hasta = Instant.parse("2026-11-04T00:00:00Z"),
        )
        assertEquals(2, servicio.observarRegistro(dia3).first().size)
        assertEquals(1, servicio.observarRegistro(dia3.copy(residenteId = beto)).first().size)

        // "hasta" excluido, "desde" incluido
        val exacto = FiltroRegistro(
            desde = Instant.parse("2026-11-03T09:00:00Z"),
            hasta = Instant.parse("2026-11-03T10:00:00Z"),
        )
        assertEquals(listOf(beto), servicio.observarRegistro(exacto).first().map { it.residenteId })
    }

    @Test
    fun el_registro_sale_del_mas_reciente_al_mas_antiguo() = runBlocking {
        val admin = crearAdmin()
        registro(admin, "2026-11-01T09:00:00Z")
        registro(admin, "2026-11-03T09:00:00Z")
        registro(admin, "2026-11-02T09:00:00Z")

        val momentos = servicio.observarRegistro().first().map { it.momento }

        assertEquals(momentos.sortedDescending(), momentos)
    }

    @Test
    fun el_admin_ve_y_cancela_qrs_activos_de_cualquier_residente() = runBlocking {
        crearAdmin()
        val beto = insertarResidente(nombre = "Beto", contacto = "b")
        val inv = insertarInvitado(beto, "Luis")
        val hoy = qrRepo.agregar(QrInvitado(invitadoId = inv, residenteId = beto, creadoEn = ahora.minusSeconds(3600)))
        qrRepo.agregar(QrInvitado(invitadoId = inv, residenteId = beto, creadoEn = ahora.minusSeconds(86400 * 2)))
        qrRepo.agregar(QrInvitado(invitadoId = inv, residenteId = beto, creadoEn = ahora, cancelado = true))

        val activos = servicio.observarQrsActivos().first()
        assertEquals(listOf(hoy), activos.map { it.qr.id })
        assertEquals("Luis", activos.single().invitado.nombre)
        assertEquals("Beto", activos.single().residente.nombre)

        assertEquals(ResultadoAdmin.Exito(), servicio.cancelarQr(hoy))
        assertTrue(servicio.observarQrsActivos().first().isEmpty())
        assertTrue(qrRepo.obtener(hoy)!!.cancelado)
        assertEquals(ResultadoAdmin.NoExiste, servicio.cancelarQr(999))
        assertNotNull(qrRepo.obtener(hoy))
    }
}

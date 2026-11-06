package victor.android.portly.domain.autenticacion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.data.repositorio.CredencialRepositorioRoom
import victor.android.portly.data.repositorio.ResidenteRepositorioRoom
import victor.android.portly.data.seguridad.HasheadorPbkdf2
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.repositorio.SesionRepositorio

class ServicioAutenticacionTest : BaseRoomTest() {

    private class SesionEnMemoria : SesionRepositorio {
        val actual = MutableStateFlow<Sesion?>(null)
        override fun observar(): Flow<Sesion?> = actual
        override suspend fun guardar(sesion: Sesion) { actual.value = sesion }
        override suspend fun cerrar() { actual.value = null }
    }

    private lateinit var sesiones: SesionEnMemoria
    private lateinit var credenciales: CredencialRepositorioRoom
    private lateinit var residentesRepo: ResidenteRepositorioRoom
    private lateinit var servicio: ServicioAutenticacion

    @Before
    fun crearServicio() {
        sesiones = SesionEnMemoria()
        credenciales = CredencialRepositorioRoom(base.credencialDao())
        residentesRepo = ResidenteRepositorioRoom(residentes)
        servicio = ServicioAutenticacion(residentesRepo, credenciales, HasheadorPbkdf2(), sesiones)
    }

    private suspend fun residenteConContrasena(
        contacto: String = "ana@correo.com",
        contrasena: String = "secreta123",
        activo: Boolean = true,
        esAdmin: Boolean = false,
    ): Long {
        val id = insertarResidente(contacto = contacto, activo = activo, esAdmin = esAdmin)
        servicio.establecerContrasena(id, contrasena)
        return id
    }

    @Test
    fun login_correcto_devuelve_la_sesion_y_la_guarda() = runBlocking {
        val id = residenteConContrasena()

        val resultado = servicio.iniciarSesion("ana@correo.com", "secreta123")

        assertEquals(ResultadoLogin.Exito(Sesion(id, esAdmin = false)), resultado)
        assertEquals(Sesion(id, false), sesiones.actual.value)
    }

    @Test
    fun login_con_contrasena_incorrecta_se_rechaza_y_no_crea_sesion() = runBlocking {
        residenteConContrasena()

        val resultado = servicio.iniciarSesion("ana@correo.com", "equivocada")

        assertEquals(ResultadoLogin.CredencialesInvalidas, resultado)
        assertNull(sesiones.actual.value)
    }

    @Test
    fun login_con_usuario_inexistente_se_rechaza() = runBlocking {
        assertEquals(
            ResultadoLogin.CredencialesInvalidas,
            servicio.iniciarSesion("nadie@correo.com", "secreta123"),
        )
    }

    @Test
    fun login_de_residente_sin_contrasena_asignada_se_rechaza() = runBlocking {
        insertarResidente(contacto = "ana@correo.com")

        assertEquals(
            ResultadoLogin.CredencialesInvalidas,
            servicio.iniciarSesion("ana@correo.com", ""),
        )
    }

    @Test
    fun el_contacto_se_busca_sin_espacios_alrededor() = runBlocking {
        residenteConContrasena()

        assertTrue(servicio.iniciarSesion("  ana@correo.com ", "secreta123") is ResultadoLogin.Exito)
    }

    @Test
    fun residente_desactivado_es_rechazado_aun_con_contrasena_correcta() = runBlocking {
        residenteConContrasena(activo = false)

        val resultado = servicio.iniciarSesion("ana@correo.com", "secreta123")

        assertEquals(ResultadoLogin.ResidenteInactivo, resultado)
        assertNull(sesiones.actual.value)
    }

    @Test
    fun un_residente_desactivado_con_contrasena_incorrecta_no_revela_su_estado() = runBlocking {
        residenteConContrasena(activo = false)

        assertEquals(
            ResultadoLogin.CredencialesInvalidas,
            servicio.iniciarSesion("ana@correo.com", "equivocada"),
        )
    }

    @Test
    fun la_sesion_identifica_al_administrador() = runBlocking {
        residenteConContrasena(contacto = "admin@correo.com", esAdmin = true)
        residenteConContrasena(contacto = "vecino@correo.com", esAdmin = false)

        val admin = servicio.iniciarSesion("admin@correo.com", "secreta123") as ResultadoLogin.Exito
        val vecino = servicio.iniciarSesion("vecino@correo.com", "secreta123") as ResultadoLogin.Exito

        assertTrue(admin.sesion.esAdmin)
        assertFalse(vecino.sesion.esAdmin)
    }

    @Test
    fun la_sesion_persiste_y_se_puede_cerrar() = runBlocking {
        val id = residenteConContrasena()
        servicio.iniciarSesion("ana@correo.com", "secreta123")

        assertEquals(Sesion(id, false), servicio.observarSesion().first())
        assertEquals(Sesion(id, false), servicio.restaurarSesion())

        servicio.cerrarSesion()

        assertNull(servicio.observarSesion().first())
        assertNull(servicio.restaurarSesion())
    }

    @Test
    fun restaurar_cierra_la_sesion_si_el_residente_fue_desactivado() = runBlocking {
        val id = residenteConContrasena()
        servicio.iniciarSesion("ana@correo.com", "secreta123")

        residentesRepo.cambiarActivo(id, activo = false)

        assertNull(servicio.restaurarSesion())
        assertNull(sesiones.actual.value)
    }

    @Test
    fun restaurar_actualiza_el_rol_si_cambio() = runBlocking {
        val id = residenteConContrasena()
        servicio.iniciarSesion("ana@correo.com", "secreta123")

        residentesRepo.actualizar(residentesRepo.obtener(id)!!.copy(esAdmin = true))

        assertEquals(Sesion(id, true), servicio.restaurarSesion())
        assertEquals(Sesion(id, true), sesiones.actual.value)
    }

    @Test
    fun la_contrasena_se_guarda_con_hash_nunca_en_texto_plano() = runBlocking {
        val id = residenteConContrasena(contrasena = "secreta123")

        val credencial = credenciales.obtener(id)

        assertNotNull(credencial)
        assertFalse(credencial!!.hash.contains("secreta123"))
        assertFalse(credencial.sal.contains("secreta123"))
    }

    @Test
    fun cambiar_la_contrasena_invalida_la_anterior() = runBlocking {
        val id = residenteConContrasena(contrasena = "vieja123")
        servicio.establecerContrasena(id, "nueva456")

        assertEquals(
            ResultadoLogin.CredencialesInvalidas,
            servicio.iniciarSesion("ana@correo.com", "vieja123"),
        )
        assertTrue(servicio.iniciarSesion("ana@correo.com", "nueva456") is ResultadoLogin.Exito)
    }

    @Test(expected = IllegalArgumentException::class)
    fun no_se_acepta_una_contrasena_vacia() {
        runBlocking { servicio.establecerContrasena(1L, "   ") }
    }
}

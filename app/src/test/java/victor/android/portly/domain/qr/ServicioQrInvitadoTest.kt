package victor.android.portly.domain.qr

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.data.repositorio.InvitadoRepositorioRoom
import victor.android.portly.data.repositorio.QrInvitadoRepositorioRoom
import victor.android.portly.data.repositorio.ResidenteRepositorioRoom
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ServicioQrInvitadoTest : BaseRoomTest() {

    private val secreto = "secreto-de-prueba".toByteArray()

    /** Reloj que se puede mover para probar el cambio de día. */
    private class RelojMovible(var ahora: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = ahora
    }

    private lateinit var reloj: RelojMovible
    private lateinit var invitadoRepo: InvitadoRepositorioRoom
    private lateinit var qrRepo: QrInvitadoRepositorioRoom
    private lateinit var servicio: ServicioQrInvitado

    @Before
    fun crearServicio() {
        reloj = RelojMovible(Instant.parse("2026-11-02T10:00:00Z"))
        invitadoRepo = InvitadoRepositorioRoom(invitados)
        qrRepo = QrInvitadoRepositorioRoom(qrs)
        servicio = ServicioQrInvitado(
            ResidenteRepositorioRoom(residentes),
            invitadoRepo,
            qrRepo,
            object : ProveedorSecretoQr { override suspend fun obtener() = secreto },
            ValidadorQr(reloj),
            reloj,
        )
    }

    private suspend fun crear(residenteId: Long, nombre: String) =
        servicio.crear(residenteId, nombre) as ResultadoCrearQr.Exito

    @Test
    fun crear_con_un_nombre_nuevo_guarda_al_invitado() = runBlocking {
        val r = insertarResidente()

        val exito = crear(r, "Luis Pérez")

        assertTrue(exito.invitadoNuevo)
        assertEquals("Luis Pérez", exito.generado.invitado.nombre)
        assertEquals(r, exito.generado.invitado.residenteId)
        assertEquals(r, exito.generado.qr.residenteId)
        assertEquals(reloj.ahora, exito.generado.qr.creadoEn)
        assertEquals(exito.generado.invitado.id, invitados.obtenerPorNombre(r, "Luis Pérez")?.id)
    }

    @Test
    fun crear_con_un_nombre_existente_reutiliza_al_invitado() = runBlocking {
        val r = insertarResidente()
        val primero = crear(r, "Luis Pérez")

        val segundo = crear(r, "  Luis   Pérez ")

        assertTrue(primero.invitadoNuevo)
        assertEquals(false, segundo.invitadoNuevo)
        assertEquals(primero.generado.invitado.id, segundo.generado.invitado.id)
        assertNotEquals(primero.generado.qr.id, segundo.generado.qr.id)
        invitadoRepo.observarPorResidente(r).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun el_mismo_nombre_en_otro_residente_es_otro_invitado() = runBlocking {
        val a = insertarResidente(contacto = "1")
        val b = insertarResidente(contacto = "2")

        assertNotEquals(
            crear(a, "Luis").generado.invitado.id,
            crear(b, "Luis").generado.invitado.id,
        )
    }

    @Test
    fun el_contenido_del_qr_es_de_tipo_invitado_con_el_id_del_qr() = runBlocking {
        val r = insertarResidente()

        val g = crear(r, "Luis").generado

        assertEquals(
            ContenidoQr(TipoQr.INVITADO, g.qr.id),
            FormatoQr(secreto).interpretar(g.contenido),
        )
        assertEquals(g.contenido, servicio.contenidoDe(g.qr))
    }

    @Test
    fun nombre_vacio_o_residente_no_disponible_se_rechazan() = runBlocking {
        val activo = insertarResidente(contacto = "1")
        val inactivo = insertarResidente(contacto = "2", activo = false)

        assertEquals(ResultadoCrearQr.NombreVacio, servicio.crear(activo, "   "))
        assertEquals(ResultadoCrearQr.ResidenteNoDisponible, servicio.crear(inactivo, "Luis"))
        assertEquals(ResultadoCrearQr.ResidenteNoDisponible, servicio.crear(999L, "Luis"))
    }

    @Test
    fun lista_los_qr_activos_de_hoy_y_deja_de_mostrar_los_cancelados() = runBlocking {
        val r = insertarResidente()
        val luis = crear(r, "Luis").generado
        val rosa = crear(r, "Rosa").generado

        servicio.observarActivosHoy(r).test {
            assertEquals(setOf("Luis", "Rosa"), awaitItem().map { it.invitado.nombre }.toSet())

            servicio.cancelar(luis.qr.id)

            assertEquals(listOf(rosa.qr.id), awaitItem().map { it.qr.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun los_qr_de_ayer_no_aparecen_como_activos_hoy() = runBlocking {
        val r = insertarResidente()
        crear(r, "Luis")

        reloj.ahora = Instant.parse("2026-11-03T08:00:00Z")
        val hoy = crear(r, "Rosa").generado

        servicio.observarActivosHoy(r).test {
            assertEquals(listOf(hoy.qr.id), awaitItem().map { it.qr.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun solo_se_listan_los_qr_del_propio_residente() = runBlocking {
        val a = insertarResidente(contacto = "1")
        val b = insertarResidente(contacto = "2")
        crear(a, "Luis")

        servicio.observarActivosHoy(b).test {
            assertEquals(emptyList<QrActivo>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun un_qr_cancelado_conserva_su_registro_y_deja_de_ser_valido() = runBlocking {
        val r = insertarResidente()
        val g = crear(r, "Luis").generado

        servicio.cancelar(g.qr.id)

        val guardado = qrRepo.obtener(g.qr.id)!!
        assertTrue(guardado.cancelado)
        assertEquals(
            ResultadoValidacion.CANCELADO,
            ValidadorQr(reloj).validar(guardado, ResidenteRepositorioRoom(residentes).obtener(r)),
        )
    }
}

package victor.android.portly.data.repositorio

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.repositorio.QrInvitadoRepositorio
import java.time.Instant

class QrInvitadoRepositorioTest : BaseRoomTest() {

    private val lunes = Instant.parse("2026-11-02T09:00:00Z")
    private val martes = Instant.parse("2026-11-03T09:00:00Z")

    private lateinit var repositorio: QrInvitadoRepositorio
    private var residenteId: Long = 0L
    private var invitadoId: Long = 0L

    @Before
    fun crearRepositorio() {
        repositorio = QrInvitadoRepositorioRoom(qrs)
        runBlocking {
            residenteId = insertarResidente()
            invitadoId = insertarInvitado(residenteId)
        }
    }

    @Test
    fun agregar_y_obtener_conserva_el_momento_de_creacion() = runBlocking {
        val id = repositorio.agregar(
            QrInvitado(invitadoId = invitadoId, residenteId = residenteId, creadoEn = martes)
        )

        val qr = repositorio.obtener(id)
        assertNotNull(qr)
        assertEquals(martes, qr!!.creadoEn)
        assertEquals(invitadoId, qr.invitadoId)
        assertEquals(residenteId, qr.residenteId)
        assertFalse(qr.cancelado)
    }

    @Test
    fun cancelar_marca_el_qr_sin_borrarlo() = runBlocking {
        val id = repositorio.agregar(
            QrInvitado(invitadoId = invitadoId, residenteId = residenteId, creadoEn = martes)
        )

        repositorio.cancelar(id)

        val qr = repositorio.obtener(id)
        assertNotNull("el QR cancelado se conserva", qr)
        assertTrue(qr!!.cancelado)
    }

    @Test
    fun observarPorResidente_emite_del_mas_reciente_al_mas_antiguo() = runBlocking {
        repositorio.agregar(
            QrInvitado(invitadoId = invitadoId, residenteId = residenteId, creadoEn = lunes)
        )

        repositorio.observarPorResidente(residenteId).test {
            assertEquals(listOf(lunes), awaitItem().map { it.creadoEn })

            repositorio.agregar(
                QrInvitado(invitadoId = invitadoId, residenteId = residenteId, creadoEn = martes)
            )

            assertEquals(listOf(martes, lunes), awaitItem().map { it.creadoEn })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorInvitado_solo_trae_los_qr_de_ese_invitado() = runBlocking {
        val otroInvitado = insertarInvitado(residenteId, nombre = "Carla")
        repositorio.agregar(
            QrInvitado(invitadoId = invitadoId, residenteId = residenteId, creadoEn = lunes)
        )
        repositorio.agregar(
            QrInvitado(invitadoId = otroInvitado, residenteId = residenteId, creadoEn = martes)
        )

        repositorio.observarPorInvitado(otroInvitado).test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertEquals(otroInvitado, lista.single().invitadoId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

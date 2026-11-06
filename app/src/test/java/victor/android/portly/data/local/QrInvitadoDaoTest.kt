package victor.android.portly.data.local

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import victor.android.portly.data.local.entidad.QrInvitadoEntity
import java.time.Instant

class QrInvitadoDaoTest : BaseRoomTest() {

    private val lunes = Instant.parse("2026-11-02T09:00:00Z")
    private val martes = Instant.parse("2026-11-03T09:00:00Z")
    private val miercoles = Instant.parse("2026-11-04T09:00:00Z")

    @Test
    fun insertar_y_consultar_conserva_el_momento_exacto() = runBlocking {
        val residenteId = insertarResidente()
        val invitadoId = insertarInvitado(residenteId)

        val id = qrs.insertar(
            QrInvitadoEntity(
                invitadoId = invitadoId,
                residenteId = residenteId,
                creadoEn = martes,
            )
        )

        val guardado = qrs.obtenerPorId(id)
        assertNotNull(guardado)
        assertEquals(invitadoId, guardado!!.invitadoId)
        assertEquals(residenteId, guardado.residenteId)
        assertEquals(martes, guardado.creadoEn)
        assertFalse(guardado.cancelado)
    }

    @Test
    fun actualizar_cambia_el_estado_del_qr() = runBlocking {
        val residenteId = insertarResidente()
        val invitadoId = insertarInvitado(residenteId)
        val id = qrs.insertar(
            QrInvitadoEntity(invitadoId = invitadoId, residenteId = residenteId, creadoEn = martes)
        )

        val original = qrs.obtenerPorId(id)!!
        qrs.actualizar(original.copy(cancelado = true))

        assertTrue(qrs.obtenerPorId(id)!!.cancelado)
    }

    @Test
    fun marcarCancelado_lo_marca_pero_no_lo_borra() = runBlocking {
        val residenteId = insertarResidente()
        val invitadoId = insertarInvitado(residenteId)
        val id = qrs.insertar(
            QrInvitadoEntity(invitadoId = invitadoId, residenteId = residenteId, creadoEn = martes)
        )

        qrs.marcarCancelado(id)

        val cancelado = qrs.obtenerPorId(id)
        assertNotNull("el QR cancelado debe seguir existiendo", cancelado)
        assertTrue(cancelado!!.cancelado)
    }

    @Test
    fun observarPorResidente_ordena_del_mas_reciente_al_mas_antiguo() = runBlocking {
        val residenteId = insertarResidente()
        val invitadoId = insertarInvitado(residenteId)
        qrs.insertar(QrInvitadoEntity(invitadoId = invitadoId, residenteId = residenteId, creadoEn = lunes))
        qrs.insertar(QrInvitadoEntity(invitadoId = invitadoId, residenteId = residenteId, creadoEn = miercoles))
        qrs.insertar(QrInvitadoEntity(invitadoId = invitadoId, residenteId = residenteId, creadoEn = martes))

        qrs.observarPorResidente(residenteId).test {
            assertEquals(listOf(miercoles, martes, lunes), awaitItem().map { it.creadoEn })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidente_no_trae_los_qr_de_otro_residente() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        val invitadoDeAna = insertarInvitado(ana, nombre = "Luis")
        val invitadoDeBeto = insertarInvitado(beto, nombre = "Carla")
        qrs.insertar(QrInvitadoEntity(invitadoId = invitadoDeAna, residenteId = ana, creadoEn = martes))
        qrs.insertar(QrInvitadoEntity(invitadoId = invitadoDeBeto, residenteId = beto, creadoEn = martes))

        qrs.observarPorResidente(ana).test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertEquals(invitadoDeAna, lista.single().invitadoId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorInvitado_emite_el_qr_nuevo() = runBlocking {
        val residenteId = insertarResidente()
        val luis = insertarInvitado(residenteId, nombre = "Luis")
        val carla = insertarInvitado(residenteId, nombre = "Carla")
        qrs.insertar(QrInvitadoEntity(invitadoId = carla, residenteId = residenteId, creadoEn = lunes))

        qrs.observarPorInvitado(luis).test {
            assertEquals(emptyList<Instant>(), awaitItem().map { it.creadoEn })

            qrs.insertar(QrInvitadoEntity(invitadoId = luis, residenteId = residenteId, creadoEn = martes))

            assertEquals(listOf(martes), awaitItem().map { it.creadoEn })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun un_qr_no_puede_colgar_de_un_invitado_que_no_existe(): Unit = runBlocking {
        val residenteId = insertarResidente()

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                qrs.insertar(
                    QrInvitadoEntity(invitadoId = 999L, residenteId = residenteId, creadoEn = martes)
                )
            }
        }
    }
}

package victor.android.portly.data.local

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import victor.android.portly.data.local.entidad.InvitadoEntity

class InvitadoDaoTest : BaseRoomTest() {

    @Test
    fun insertar_y_consultar_devuelve_los_mismos_datos() = runBlocking {
        val residenteId = insertarResidente()
        val id = insertarInvitado(residenteId, nombre = "Luis Invitado")

        val guardado = invitados.obtenerPorId(id)

        assertNotNull(guardado)
        assertEquals("Luis Invitado", guardado!!.nombre)
        assertEquals(residenteId, guardado.residenteId)
        assertFalse(guardado.notificacionesSilenciadas)
    }

    @Test
    fun actualizar_cambia_el_nombre_del_invitado() = runBlocking {
        val residenteId = insertarResidente()
        val id = insertarInvitado(residenteId, nombre = "Luis")

        val original = invitados.obtenerPorId(id)!!
        invitados.actualizar(original.copy(nombre = "Luis Hernández"))

        assertEquals("Luis Hernández", invitados.obtenerPorId(id)!!.nombre)
    }

    @Test
    fun cambiarSilencio_actualiza_el_interruptor_individual() = runBlocking {
        val residenteId = insertarResidente()
        val id = insertarInvitado(residenteId)

        invitados.cambiarSilencio(id, silenciadas = true)
        assertTrue(invitados.obtenerPorId(id)!!.notificacionesSilenciadas)

        invitados.cambiarSilencio(id, silenciadas = false)
        assertFalse(invitados.obtenerPorId(id)!!.notificacionesSilenciadas)
    }

    @Test
    fun obtenerPorNombre_encuentra_al_invitado_ya_guardado() = runBlocking {
        val residenteId = insertarResidente()
        val id = insertarInvitado(residenteId, nombre = "Luis")

        assertEquals(id, invitados.obtenerPorNombre(residenteId, "Luis")?.id)
    }

    @Test
    fun obtenerPorNombre_no_cruza_invitados_entre_residentes() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        insertarInvitado(ana, nombre = "Luis")

        assertNotNull(invitados.obtenerPorNombre(ana, "Luis"))
        assertNull(invitados.obtenerPorNombre(beto, "Luis"))
    }

    @Test
    fun observarPorResidente_solo_trae_los_invitados_de_ese_residente() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        insertarInvitado(ana, nombre = "Zoe")
        insertarInvitado(ana, nombre = "Carla")
        insertarInvitado(beto, nombre = "Daniel")

        invitados.observarPorResidente(ana).test {
            assertEquals(listOf("Carla", "Zoe"), awaitItem().map { it.nombre })

            insertarInvitado(ana, nombre = "Memo")

            assertEquals(listOf("Carla", "Memo", "Zoe"), awaitItem().map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun un_residente_no_puede_tener_dos_invitados_con_el_mismo_nombre(): Unit = runBlocking {
        val residenteId = insertarResidente()
        insertarInvitado(residenteId, nombre = "Luis")

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { insertarInvitado(residenteId, nombre = "Luis") }
        }
    }

    @Test
    fun dos_residentes_si_pueden_tener_un_invitado_con_el_mismo_nombre() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")

        insertarInvitado(ana, nombre = "Luis")
        insertarInvitado(beto, nombre = "Luis")

        assertNotNull(invitados.obtenerPorNombre(ana, "Luis"))
        assertNotNull(invitados.obtenerPorNombre(beto, "Luis"))
    }

    @Test
    fun un_invitado_no_puede_colgar_de_un_residente_que_no_existe(): Unit = runBlocking {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                invitados.insertar(InvitadoEntity(nombre = "Fantasma", residenteId = 999L))
            }
        }
    }
}

package victor.android.portly.data.repositorio

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.repositorio.InvitadoRepositorio

class InvitadoRepositorioTest : BaseRoomTest() {

    private lateinit var repositorio: InvitadoRepositorio
    private var residenteId: Long = 0L

    @Before
    fun crearRepositorio() {
        repositorio = InvitadoRepositorioRoom(invitados)
        residenteId = runBlocking { insertarResidente() }
    }

    @Test
    fun agregar_y_obtener_trae_el_modelo_del_dominio() = runBlocking {
        val id = repositorio.agregar(Invitado(nombre = "Luis", residenteId = residenteId))

        val invitado = repositorio.obtener(id)
        assertNotNull(invitado)
        assertEquals("Luis", invitado!!.nombre)
        assertEquals(residenteId, invitado.residenteId)
        assertFalse(invitado.notificacionesSilenciadas)
    }

    @Test
    fun buscarPorNombre_permite_reutilizar_al_invitado_guardado() = runBlocking {
        val id = repositorio.agregar(Invitado(nombre = "Luis", residenteId = residenteId))

        assertEquals(id, repositorio.buscarPorNombre(residenteId, "Luis")?.id)
        assertNull(repositorio.buscarPorNombre(residenteId, "Carla"))
    }

    @Test
    fun actualizar_guarda_los_cambios() = runBlocking {
        val id = repositorio.agregar(Invitado(nombre = "Luis", residenteId = residenteId))

        val invitado = repositorio.obtener(id)!!
        repositorio.actualizar(invitado.copy(nombre = "Luis Hernández"))

        assertEquals("Luis Hernández", repositorio.obtener(id)!!.nombre)
    }

    @Test
    fun cambiarSilencio_mueve_el_interruptor_individual() = runBlocking {
        val id = repositorio.agregar(Invitado(nombre = "Luis", residenteId = residenteId))

        repositorio.cambiarSilencio(id, silenciadas = true)
        assertTrue(repositorio.obtener(id)!!.notificacionesSilenciadas)

        repositorio.cambiarSilencio(id, silenciadas = false)
        assertFalse(repositorio.obtener(id)!!.notificacionesSilenciadas)
    }

    @Test
    fun observarPorResidente_emite_al_agregar_un_invitado() = runBlocking {
        repositorio.observarPorResidente(residenteId).test {
            assertEquals(emptyList<String>(), awaitItem().map { it.nombre })

            repositorio.agregar(Invitado(nombre = "Luis", residenteId = residenteId))

            assertEquals(listOf("Luis"), awaitItem().map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }
}

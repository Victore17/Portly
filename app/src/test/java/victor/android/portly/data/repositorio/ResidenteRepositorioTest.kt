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
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Residente
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.repositorio.RegistroAccesoRepositorio
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import java.time.Instant

class ResidenteRepositorioTest : BaseRoomTest() {

    private lateinit var repositorio: ResidenteRepositorio
    private lateinit var historial: RegistroAccesoRepositorio

    @Before
    fun crearRepositorios() {
        repositorio = ResidenteRepositorioRoom(residentes)
        historial = RegistroAccesoRepositorioRoom(registros)
    }

    private fun residenteDePrueba(
        nombre: String = "Ana Ruiz",
        contacto: String = "5550001111",
        esAdmin: Boolean = false,
    ) = Residente(
        nombre = nombre,
        domicilio = "Calle 1 #10",
        contacto = contacto,
        esAdmin = esAdmin,
    )

    @Test
    fun agregar_devuelve_el_id_y_obtener_trae_el_modelo_del_dominio() = runBlocking {
        val id = repositorio.agregar(residenteDePrueba(esAdmin = true))

        assertTrue("la base debe asignar un id", id > 0)
        val residente = repositorio.obtener(id)
        assertNotNull(residente)
        assertEquals("Ana Ruiz", residente!!.nombre)
        assertEquals("Calle 1 #10", residente.domicilio)
        assertEquals("5550001111", residente.contacto)
        assertTrue(residente.activo)
        assertTrue(residente.esAdmin)
    }

    @Test
    fun obtener_un_id_que_no_existe_devuelve_null() = runBlocking {
        assertNull(repositorio.obtener(404L))
    }

    @Test
    fun actualizar_guarda_los_cambios() = runBlocking {
        val id = repositorio.agregar(residenteDePrueba())

        val residente = repositorio.obtener(id)!!
        repositorio.actualizar(residente.copy(nombre = "Ana Ruiz Pérez", esAdmin = true))

        val actualizado = repositorio.obtener(id)!!
        assertEquals("Ana Ruiz Pérez", actualizado.nombre)
        assertTrue(actualizado.esAdmin)
    }

    @Test
    fun observarActivos_refleja_la_desactivacion_y_la_reactivacion() = runBlocking {
        val anaId = repositorio.agregar(residenteDePrueba(nombre = "Ana", contacto = "1"))
        repositorio.agregar(residenteDePrueba(nombre = "Beto", contacto = "2"))

        repositorio.observarActivos().test {
            assertEquals(listOf("Ana", "Beto"), awaitItem().map { it.nombre })

            repositorio.cambiarActivo(anaId, activo = false)
            assertEquals(listOf("Beto"), awaitItem().map { it.nombre })

            repositorio.cambiarActivo(anaId, activo = true)
            assertEquals(listOf("Ana", "Beto"), awaitItem().map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarTodos_sigue_incluyendo_a_los_residentes_desactivados() = runBlocking {
        val anaId = repositorio.agregar(residenteDePrueba(nombre = "Ana", contacto = "1"))
        repositorio.agregar(residenteDePrueba(nombre = "Beto", contacto = "2"))
        repositorio.cambiarActivo(anaId, activo = false)

        repositorio.observarTodos().test {
            assertEquals(listOf("Ana", "Beto"), awaitItem().map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun desactivar_un_residente_conserva_su_historial_completo() = runBlocking {
        val id = repositorio.agregar(residenteDePrueba(nombre = "Ana Ruiz"))
        val invitadoId = insertarInvitado(id, nombre = "Luis Invitado")
        historial.registrar(
            RegistroAcceso(
                nombrePersona = "Ana Ruiz",
                tipo = TipoAcceso.ENTRADA,
                momento = Instant.parse("2026-11-02T08:00:00Z"),
                residenteId = id,
            )
        )
        historial.registrar(
            RegistroAcceso(
                nombrePersona = "Luis Invitado",
                tipo = TipoAcceso.ENTRADA,
                momento = Instant.parse("2026-11-02T18:00:00Z"),
                residenteId = id,
                invitadoId = invitadoId,
            )
        )

        repositorio.cambiarActivo(id, activo = false)

        // El residente sigue existiendo, solo quedó inactivo.
        val desactivado = repositorio.obtener(id)
        assertNotNull(desactivado)
        assertFalse(desactivado!!.activo)
        assertEquals("Ana Ruiz", desactivado.nombre)

        // Y su historial quedó intacto, el propio y el de su invitado.
        historial.observarPorResidente(id).test {
            val lista = awaitItem()
            assertEquals(2, lista.size)
            assertEquals(listOf("Luis Invitado", "Ana Ruiz"), lista.map { it.nombrePersona })
            cancelAndIgnoreRemainingEvents()
        }

        // El invitado guardado tampoco se perdió.
        assertNotNull(invitados.obtenerPorId(invitadoId))
    }

    @Test
    fun el_contrato_del_repositorio_no_ofrece_ninguna_forma_de_borrar() {
        val prohibidas = listOf("borrar", "eliminar", "delete", "quitar")
        val encontradas = ResidenteRepositorio::class.java.methods
            .map { it.name }
            .filter { nombre -> prohibidas.any { nombre.lowercase().contains(it) } }

        assertEquals(
            "un residente nunca se borra: solo se activa o se desactiva",
            emptyList<String>(),
            encontradas,
        )
    }
}

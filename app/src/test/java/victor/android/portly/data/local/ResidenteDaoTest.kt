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
import victor.android.portly.data.local.entidad.RegistroAccesoEntity
import victor.android.portly.domain.modelo.TipoAcceso
import java.time.Instant

class ResidenteDaoTest : BaseRoomTest() {

    @Test
    fun insertar_y_consultar_devuelve_los_mismos_datos() = runBlocking {
        val id = insertarResidente(
            nombre = "Ana Ruiz",
            domicilio = "Calle 1 #10",
            contacto = "5550001111",
            esAdmin = true,
        )

        val guardado = residentes.obtenerPorId(id)

        assertNotNull(guardado)
        assertEquals(id, guardado!!.id)
        assertEquals("Ana Ruiz", guardado.nombre)
        assertEquals("Calle 1 #10", guardado.domicilio)
        assertEquals("5550001111", guardado.contacto)
        assertTrue(guardado.activo)
        assertTrue(guardado.esAdmin)
    }

    @Test
    fun consultar_un_id_que_no_existe_devuelve_null() = runBlocking {
        assertEquals(null, residentes.obtenerPorId(404L))
    }

    @Test
    fun actualizar_cambia_los_datos_del_residente() = runBlocking {
        val id = insertarResidente(nombre = "Ana Ruiz", domicilio = "Calle 1 #10")

        val original = residentes.obtenerPorId(id)!!
        residentes.actualizar(original.copy(nombre = "Ana Ruiz Pérez", domicilio = "Calle 2 #20"))

        val actualizado = residentes.obtenerPorId(id)!!
        assertEquals("Ana Ruiz Pérez", actualizado.nombre)
        assertEquals("Calle 2 #20", actualizado.domicilio)
        assertEquals(id, actualizado.id)
    }

    @Test
    fun cambiarActivo_desactiva_al_residente_sin_quitarlo_de_la_tabla() = runBlocking {
        val id = insertarResidente()

        residentes.cambiarActivo(id, activo = false)

        val desactivado = residentes.obtenerPorId(id)
        assertNotNull("el residente debe seguir existiendo", desactivado)
        assertFalse(desactivado!!.activo)

        residentes.cambiarActivo(id, activo = true)
        assertTrue(residentes.obtenerPorId(id)!!.activo)
    }

    @Test
    fun observarTodos_emite_la_lista_ordenada_por_nombre() = runBlocking {
        insertarResidente(nombre = "Zoe", contacto = "1")
        insertarResidente(nombre = "ana", contacto = "2")
        insertarResidente(nombre = "Beto", contacto = "3")

        residentes.observarTodos().test {
            assertEquals(listOf("ana", "Beto", "Zoe"), awaitItem().map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarActivos_deja_de_incluir_al_residente_al_desactivarlo() = runBlocking {
        val id = insertarResidente(nombre = "Ana", contacto = "1")
        insertarResidente(nombre = "Beto", contacto = "2")

        residentes.observarActivos().test {
            assertEquals(listOf("Ana", "Beto"), awaitItem().map { it.nombre })

            residentes.cambiarActivo(id, activo = false)

            assertEquals(listOf("Beto"), awaitItem().map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorId_emite_el_cambio_de_nombre() = runBlocking {
        val id = insertarResidente(nombre = "Ana")

        residentes.observarPorId(id).test {
            assertEquals("Ana", awaitItem()?.nombre)

            val actual = residentes.obtenerPorId(id)!!
            residentes.actualizar(actual.copy(nombre = "Ana María"))

            assertEquals("Ana María", awaitItem()?.nombre)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun la_base_impide_borrar_un_residente_que_tiene_historial() = runBlocking {
        val id = insertarResidente()
        registros.insertar(
            RegistroAccesoEntity(
                nombrePersona = "Ana Ruiz",
                tipo = TipoAcceso.ENTRADA,
                momento = Instant.parse("2026-11-06T08:00:00Z"),
                residenteId = id,
            )
        )

        // La regla "nunca se borra un residente" está respaldada por la llave
        // foránea RESTRICT, así que ni un DELETE directo puede saltársela.
        val escritura = base.openHelper.writableDatabase
        assertThrows(SQLiteConstraintException::class.java) {
            escritura.execSQL("DELETE FROM residentes WHERE id = $id")
        }

        assertNotNull(residentes.obtenerPorId(id))
    }

    @Test
    fun no_se_pueden_repetir_dos_residentes_con_el_mismo_contacto(): Unit = runBlocking {
        insertarResidente(nombre = "Ana", contacto = "5550001111")

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { insertarResidente(nombre = "Beto", contacto = "5550001111") }
        }
    }
}

package victor.android.portly.data.local

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import victor.android.portly.data.local.entidad.RegistroAccesoEntity
import victor.android.portly.domain.modelo.TipoAcceso
import java.time.Instant

class RegistroAccesoDaoTest : BaseRoomTest() {

    private val lunes8 = Instant.parse("2026-11-02T08:00:00Z")
    private val lunes18 = Instant.parse("2026-11-02T18:00:00Z")
    private val martes8 = Instant.parse("2026-11-03T08:00:00Z")
    private val miercoles8 = Instant.parse("2026-11-04T08:00:00Z")

    private suspend fun registrar(
        residenteId: Long,
        nombrePersona: String,
        tipo: TipoAcceso,
        momento: Instant,
        invitadoId: Long? = null,
    ): Long = registros.insertar(
        RegistroAccesoEntity(
            nombrePersona = nombrePersona,
            tipo = tipo,
            momento = momento,
            residenteId = residenteId,
            invitadoId = invitadoId,
        )
    )

    @Test
    fun insertar_y_consultar_conserva_tipo_hora_y_persona() = runBlocking {
        val residenteId = insertarResidente(nombre = "Ana Ruiz")

        val id = registrar(residenteId, "Ana Ruiz", TipoAcceso.ENTRADA, lunes8)

        val guardado = registros.obtenerPorId(id)
        assertNotNull(guardado)
        assertEquals("Ana Ruiz", guardado!!.nombrePersona)
        assertEquals(TipoAcceso.ENTRADA, guardado.tipo)
        assertEquals(lunes8, guardado.momento)
        assertEquals(residenteId, guardado.residenteId)
        assertNull("un acceso del residente no lleva invitadoId", guardado.invitadoId)
    }

    @Test
    fun un_acceso_de_invitado_queda_ligado_al_invitado_y_al_residente() = runBlocking {
        val residenteId = insertarResidente(nombre = "Ana Ruiz")
        val invitadoId = insertarInvitado(residenteId, nombre = "Luis Invitado")

        val id = registrar(residenteId, "Luis Invitado", TipoAcceso.ENTRADA, lunes8, invitadoId)

        val guardado = registros.obtenerPorId(id)!!
        assertEquals(invitadoId, guardado.invitadoId)
        assertEquals(residenteId, guardado.residenteId)
    }

    @Test
    fun observarPorResidente_ordena_del_mas_reciente_al_mas_antiguo() = runBlocking {
        val residenteId = insertarResidente()
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, lunes8)
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, miercoles8)
        registrar(residenteId, "Ana", TipoAcceso.SALIDA, lunes18)

        registros.observarPorResidente(residenteId).test {
            assertEquals(listOf(miercoles8, lunes18, lunes8), awaitItem().map { it.momento })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidente_trae_los_accesos_propios_y_los_de_sus_invitados() = runBlocking {
        val residenteId = insertarResidente(nombre = "Ana Ruiz")
        val invitadoId = insertarInvitado(residenteId, nombre = "Luis Invitado")
        registrar(residenteId, "Ana Ruiz", TipoAcceso.ENTRADA, lunes8)
        registrar(residenteId, "Luis Invitado", TipoAcceso.ENTRADA, lunes18, invitadoId)

        registros.observarPorResidente(residenteId).test {
            val lista = awaitItem()
            assertEquals(listOf("Luis Invitado", "Ana Ruiz"), lista.map { it.nombrePersona })
            assertEquals(listOf(invitadoId, null), lista.map { it.invitadoId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidente_no_trae_los_registros_de_otro_residente() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        registrar(ana, "Ana", TipoAcceso.ENTRADA, lunes8)
        registrar(beto, "Beto", TipoAcceso.ENTRADA, lunes8)
        registrar(beto, "Beto", TipoAcceso.SALIDA, lunes18)

        registros.observarPorResidente(ana).test {
            assertEquals(listOf("Ana"), awaitItem().map { it.nombrePersona })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidente_emite_el_registro_nuevo() = runBlocking {
        val residenteId = insertarResidente()
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, lunes8)

        registros.observarPorResidente(residenteId).test {
            assertEquals(1, awaitItem().size)

            registrar(residenteId, "Ana", TipoAcceso.SALIDA, lunes18)

            assertEquals(listOf(lunes18, lunes8), awaitItem().map { it.momento })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidenteEnRango_solo_trae_los_de_ese_dia() = runBlocking {
        val residenteId = insertarResidente()
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, lunes8)
        registrar(residenteId, "Ana", TipoAcceso.SALIDA, lunes18)
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, martes8)
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, miercoles8)

        val inicioDelLunes = Instant.parse("2026-11-02T00:00:00Z")
        val inicioDelMartes = Instant.parse("2026-11-03T00:00:00Z")

        registros.observarPorResidenteEnRango(residenteId, inicioDelLunes, inicioDelMartes).test {
            assertEquals(listOf(lunes18, lunes8), awaitItem().map { it.momento })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidenteEnRango_incluye_el_inicio_y_excluye_el_final() = runBlocking {
        val residenteId = insertarResidente()
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, lunes8)
        registrar(residenteId, "Ana", TipoAcceso.ENTRADA, martes8)

        registros.observarPorResidenteEnRango(residenteId, lunes8, martes8).test {
            assertEquals(listOf(lunes8), awaitItem().map { it.momento })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidenteEnRango_no_cruza_residentes() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        registrar(ana, "Ana", TipoAcceso.ENTRADA, lunes8)
        registrar(beto, "Beto", TipoAcceso.ENTRADA, lunes8)

        val inicioDelLunes = Instant.parse("2026-11-02T00:00:00Z")
        val inicioDelMartes = Instant.parse("2026-11-03T00:00:00Z")

        registros.observarPorResidenteEnRango(beto, inicioDelLunes, inicioDelMartes).test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertTrue(lista.single().nombrePersona == "Beto")
            cancelAndIgnoreRemainingEvents()
        }
    }
}

package victor.android.portly.data.repositorio

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.repositorio.RegistroAccesoRepositorio
import java.time.Instant

class RegistroAccesoRepositorioTest : BaseRoomTest() {

    private val lunes8 = Instant.parse("2026-11-02T08:00:00Z")
    private val lunes18 = Instant.parse("2026-11-02T18:00:00Z")
    private val martes8 = Instant.parse("2026-11-03T08:00:00Z")

    private lateinit var repositorio: RegistroAccesoRepositorio

    @Before
    fun crearRepositorio() {
        repositorio = RegistroAccesoRepositorioRoom(registros)
    }

    private suspend fun registrar(
        residenteId: Long,
        nombrePersona: String,
        momento: Instant,
        tipo: TipoAcceso = TipoAcceso.ENTRADA,
        invitadoId: Long? = null,
    ) = repositorio.registrar(
        RegistroAcceso(
            nombrePersona = nombrePersona,
            tipo = tipo,
            momento = momento,
            residenteId = residenteId,
            invitadoId = invitadoId,
        )
    )

    @Test
    fun registrar_devuelve_el_id_y_el_acceso_aparece_en_el_historial() = runBlocking {
        val residenteId = insertarResidente(nombre = "Ana Ruiz")

        val id = registrar(residenteId, "Ana Ruiz", lunes8)

        assertTrue(id > 0)
        repositorio.observarPorResidente(residenteId).test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertEquals("Ana Ruiz", lista.single().nombrePersona)
            assertEquals(TipoAcceso.ENTRADA, lista.single().tipo)
            assertEquals(lunes8, lista.single().momento)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun el_modelo_distingue_un_acceso_de_invitado_de_uno_propio() = runBlocking {
        val residenteId = insertarResidente(nombre = "Ana Ruiz")
        val invitadoId = insertarInvitado(residenteId, nombre = "Luis Invitado")
        registrar(residenteId, "Ana Ruiz", lunes8)
        registrar(residenteId, "Luis Invitado", lunes18, invitadoId = invitadoId)

        repositorio.observarPorResidente(residenteId).test {
            val lista = awaitItem()
            assertEquals(listOf(true, false), lista.map { it.esDeInvitado })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidente_aisla_el_historial_de_cada_residente() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        registrar(ana, "Ana", lunes8)
        registrar(beto, "Beto", lunes8)
        registrar(beto, "Beto", lunes18, TipoAcceso.SALIDA)

        repositorio.observarPorResidente(ana).test {
            assertEquals(listOf("Ana"), awaitItem().map { it.nombrePersona })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observarPorResidenteEnRango_filtra_por_fecha() = runBlocking {
        val residenteId = insertarResidente()
        registrar(residenteId, "Ana", lunes8)
        registrar(residenteId, "Ana", lunes18, TipoAcceso.SALIDA)
        registrar(residenteId, "Ana", martes8)

        val inicioDelLunes = Instant.parse("2026-11-02T00:00:00Z")
        val inicioDelMartes = Instant.parse("2026-11-03T00:00:00Z")

        repositorio.observarPorResidenteEnRango(residenteId, inicioDelLunes, inicioDelMartes).test {
            assertEquals(listOf(lunes18, lunes8), awaitItem().map { it.momento })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun el_historial_de_un_residente_desactivado_sigue_consultable() = runBlocking {
        val residenteId = insertarResidente()
        registrar(residenteId, "Ana", lunes8)

        residentes.cambiarActivo(residenteId, activo = false)

        repositorio.observarPorResidente(residenteId).test {
            assertNotNull(awaitItem().singleOrNull())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

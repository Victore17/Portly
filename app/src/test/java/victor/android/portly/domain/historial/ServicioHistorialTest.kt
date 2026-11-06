package victor.android.portly.domain.historial

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.data.repositorio.InvitadoRepositorioRoom
import victor.android.portly.data.repositorio.RegistroAccesoRepositorioRoom
import victor.android.portly.data.repositorio.ResidenteRepositorioRoom
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.repositorio.SesionRepositorio
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ServicioHistorialTest : BaseRoomTest() {

    private class RelojMovible(var ahora: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = ahora
    }

    private class SesionFalsa : SesionRepositorio {
        val actual = MutableStateFlow<Sesion?>(null)
        override fun observar() = actual
        override suspend fun guardar(sesion: Sesion) { actual.value = sesion }
        override suspend fun cerrar() { actual.value = null }
    }

    private lateinit var reloj: RelojMovible
    private lateinit var sesion: SesionFalsa
    private lateinit var servicio: ServicioHistorial

    @Before
    fun crearServicio() {
        reloj = RelojMovible(Instant.parse("2026-11-02T08:00:00Z"))
        sesion = SesionFalsa()
        servicio = ServicioHistorial(
            RegistroAccesoRepositorioRoom(registros),
            ResidenteRepositorioRoom(residentes),
            InvitadoRepositorioRoom(invitados),
            sesion,
            reloj,
        )
    }

    private suspend fun iniciarSesion(id: Long) = sesion.guardar(Sesion(id, esAdmin = false))

    @Test
    fun un_registro_nuevo_aparece_en_el_historial_del_residente() = runBlocking {
        val r = insertarResidente(nombre = "Ana Ruiz")
        iniciarSesion(r)

        val resultado = servicio.registrar(r, TipoAcceso.ENTRADA) as ResultadoRegistroAcceso.Exito

        assertTrue(resultado.registro.id > 0)
        servicio.observarMiHistorial().test {
            val lista = awaitItem()
            assertEquals(1, lista.size)
            assertEquals("Ana Ruiz", lista.single().nombrePersona)
            assertEquals(TipoAcceso.ENTRADA, lista.single().tipo)
            assertEquals(reloj.ahora, lista.single().momento)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun un_residente_no_ve_los_registros_de_otro() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        servicio.registrar(ana, TipoAcceso.ENTRADA)
        servicio.registrar(beto, TipoAcceso.ENTRADA)

        iniciarSesion(ana)
        servicio.observarMiHistorial().test {
            assertEquals(listOf("Ana"), awaitItem().map { it.nombrePersona })
            cancelAndIgnoreRemainingEvents()
        }
        iniciarSesion(beto)
        servicio.observarMiHistorial().test {
            assertEquals(listOf("Beto"), awaitItem().map { it.nombrePersona })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sin_sesion_el_historial_esta_vacio() = runBlocking {
        val r = insertarResidente()
        servicio.registrar(r, TipoAcceso.ENTRADA)

        servicio.observarMiHistorial().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun el_orden_es_del_mas_reciente_al_mas_antiguo() = runBlocking {
        val r = insertarResidente()
        iniciarSesion(r)
        servicio.registrar(r, TipoAcceso.ENTRADA)
        reloj.ahora = reloj.ahora.plusSeconds(3600)
        servicio.registrar(r, TipoAcceso.SALIDA)
        reloj.ahora = reloj.ahora.plusSeconds(3600)
        servicio.registrar(r, TipoAcceso.ENTRADA)

        servicio.observarMiHistorial().test {
            val lista = awaitItem()
            assertEquals(
                listOf(TipoAcceso.ENTRADA, TipoAcceso.SALIDA, TipoAcceso.ENTRADA),
                lista.map { it.tipo },
            )
            assertEquals(lista.map { it.momento }.sortedDescending(), lista.map { it.momento })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aparecen_accesos_propios_y_de_invitados() = runBlocking {
        val r = insertarResidente(nombre = "Ana")
        val inv = insertarInvitado(r, nombre = "Luis")
        iniciarSesion(r)
        servicio.registrar(r, TipoAcceso.ENTRADA)
        reloj.ahora = reloj.ahora.plusSeconds(60)
        servicio.registrar(r, TipoAcceso.ENTRADA, invitadoId = inv)

        servicio.observarMiHistorial().test {
            val lista = awaitItem()
            assertEquals(listOf("Luis", "Ana"), lista.map { it.nombrePersona })
            assertEquals(listOf(true, false), lista.map { it.esDeInvitado })
            assertEquals(inv, lista.first().invitadoId)
            assertEquals(r, lista.first().residenteId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun un_invitado_de_otro_residente_no_se_registra() = runBlocking {
        val ana = insertarResidente(nombre = "Ana", contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        val invDeBeto = insertarInvitado(beto)

        val resultado = servicio.registrar(ana, TipoAcceso.ENTRADA, invDeBeto)

        assertEquals(ResultadoRegistroAcceso.InvitadoNoPertenece, resultado)
    }

    @Test
    fun un_residente_inexistente_o_desactivado_no_registra() = runBlocking {
        val inactivo = insertarResidente(activo = false)

        assertEquals(
            ResultadoRegistroAcceso.ResidenteNoDisponible,
            servicio.registrar(inactivo, TipoAcceso.ENTRADA),
        )
        assertEquals(
            ResultadoRegistroAcceso.ResidenteNoDisponible,
            servicio.registrar(999, TipoAcceso.ENTRADA),
        )
    }

    @Test
    fun el_historial_se_actualiza_al_registrar_nuevos_accesos() = runBlocking {
        val r = insertarResidente()
        iniciarSesion(r)
        servicio.observarMiHistorial().test {
            assertTrue(awaitItem().isEmpty())
            servicio.registrar(r, TipoAcceso.ENTRADA)
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

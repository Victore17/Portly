package victor.android.portly.domain.notificaciones

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.data.repositorio.InvitadoRepositorioRoom
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.repositorio.AjustesNotificacionesRepositorio
import victor.android.portly.domain.repositorio.SesionRepositorio

class ServicioAjustesNotificacionesTest : BaseRoomTest() {

    private class SesionFalsa : SesionRepositorio {
        val actual = MutableStateFlow<Sesion?>(null)
        override fun observar() = actual
        override suspend fun guardar(sesion: Sesion) { actual.value = sesion }
        override suspend fun cerrar() { actual.value = null }
    }

    private class AjustesFalsos : AjustesNotificacionesRepositorio {
        val valores = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
        override fun observarSilenciarTodos(residenteId: Long) =
            kotlinx.coroutines.flow.flow { valores.collect { emit(it[residenteId] ?: false) } }
        override suspend fun cambiarSilenciarTodos(residenteId: Long, silenciar: Boolean) {
            valores.value = valores.value + (residenteId to silenciar)
        }
    }

    private lateinit var sesion: SesionFalsa
    private lateinit var servicio: ServicioAjustesNotificaciones

    @Before
    fun crear() {
        sesion = SesionFalsa()
        servicio = ServicioAjustesNotificaciones(InvitadoRepositorioRoom(invitados), AjustesFalsos(), sesion)
    }

    private suspend fun estado() = servicio.observar().first()

    @Test
    fun el_general_solo_aparece_con_2_o_mas_invitados() = runBlocking {
        val r = insertarResidente()
        sesion.guardar(Sesion(r, esAdmin = false))
        assertFalse(estado().mostrarSilenciarTodos)

        insertarInvitado(r, "A Uno")
        assertFalse(estado().mostrarSilenciarTodos)

        insertarInvitado(r, "B Dos")
        assertTrue(estado().mostrarSilenciarTodos)
    }

    @Test
    fun con_el_general_activo_todos_cuentan_como_silenciados() = runBlocking {
        val r = insertarResidente()
        sesion.guardar(Sesion(r, esAdmin = false))
        insertarInvitado(r, "A Uno")
        insertarInvitado(r, "B Dos", silenciadas = true)

        servicio.cambiarSilenciarTodos(true)

        val e = estado()
        assertTrue(e.invitados.all { it.silenciadoEfectivo })
        assertEquals(listOf(false, true), e.invitados.map { it.silenciadoIndividual })
    }

    @Test
    fun al_apagar_el_general_se_restauran_los_ajustes_individuales() = runBlocking {
        val r = insertarResidente()
        sesion.guardar(Sesion(r, esAdmin = false))
        insertarInvitado(r, "A Uno")
        insertarInvitado(r, "B Dos", silenciadas = true)

        servicio.cambiarSilenciarTodos(true)
        servicio.cambiarSilenciarTodos(false)

        assertEquals(listOf(false, true), estado().invitados.map { it.silenciadoEfectivo })
    }

    @Test
    fun el_interruptor_individual_cambia_solo_a_ese_invitado() = runBlocking {
        val r = insertarResidente()
        sesion.guardar(Sesion(r, esAdmin = false))
        val a = insertarInvitado(r, "A Uno")
        insertarInvitado(r, "B Dos")

        assertTrue(servicio.cambiarInvitado(a, true))

        assertEquals(listOf(true, false), estado().invitados.map { it.silenciadoEfectivo })
    }

    @Test
    fun no_se_puede_cambiar_un_invitado_de_otro_residente() = runBlocking {
        val ana = insertarResidente(contacto = "1")
        val beto = insertarResidente(nombre = "Beto", contacto = "2")
        val ajeno = insertarInvitado(beto, "Ajeno")
        sesion.guardar(Sesion(ana, esAdmin = false))

        assertFalse(servicio.cambiarInvitado(ajeno, true))
        assertFalse(InvitadoRepositorioRoom(invitados).obtener(ajeno)!!.notificacionesSilenciadas)
    }

    @Test
    fun debeNotificar_respeta_el_individual_y_el_general() = runBlocking {
        val r = insertarResidente()
        sesion.guardar(Sesion(r, esAdmin = false))
        val a = insertarInvitado(r, "A Uno")
        val b = insertarInvitado(r, "B Dos", silenciadas = true)

        assertTrue(servicio.debeNotificar(a))
        assertFalse(servicio.debeNotificar(b))

        servicio.cambiarSilenciarTodos(true)
        assertFalse(servicio.debeNotificar(a))
    }

    @Test
    fun sin_sesion_no_hay_ajustes_ni_se_puede_cambiar() = runBlocking {
        assertEquals(AjustesNotificaciones.VACIO, estado())
        assertFalse(servicio.cambiarSilenciarTodos(true))
    }
}

package victor.android.portly.ui

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import victor.android.portly.data.qr.GeneradorQr
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.historial.ServicioHistorial
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.notificaciones.AjusteInvitado
import victor.android.portly.domain.notificaciones.AjustesNotificaciones
import victor.android.portly.domain.notificaciones.ServicioAjustesNotificaciones
import victor.android.portly.domain.qr.ServicioQrResidente
import victor.android.portly.ui.ajustes.AjustesViewModel
import victor.android.portly.ui.ajustes.NotificacionesInvitadosViewModel
import victor.android.portly.ui.historial.HistorialViewModel
import victor.android.portly.ui.miqr.MiQrUiState
import victor.android.portly.ui.miqr.MiQrViewModel
import java.time.Instant

class MiQrHistorialAjustesViewModelTest {
    @get:Rule val principal = ReglaDispatcherPrincipal()

    private val autenticacion = mockk<ServicioAutenticacion>(relaxed = true)

    // ---- Mi QR ----

    @Test
    fun mi_qr_con_sesion_muestra_el_contenido() {
        every { autenticacion.observarSesion() } returns MutableStateFlow(Sesion(7, false))
        val qr = mockk<ServicioQrResidente>()
        coEvery { qr.contenidoDe(7) } returns "contenido"
        val vm = MiQrViewModel(autenticacion, qr, mockk<GeneradorQr>())
        assertEquals(MiQrUiState.Listo("contenido"), vm.estado.value)
    }

    @Test
    fun mi_qr_residente_inactivo_no_disponible() {
        every { autenticacion.observarSesion() } returns MutableStateFlow(Sesion(7, false))
        val qr = mockk<ServicioQrResidente>()
        coEvery { qr.contenidoDe(7) } returns null
        assertEquals(MiQrUiState.NoDisponible, MiQrViewModel(autenticacion, qr, mockk()).estado.value)
    }

    @Test
    fun mi_qr_sin_sesion_no_disponible() {
        every { autenticacion.observarSesion() } returns MutableStateFlow(null)
        assertEquals(MiQrUiState.NoDisponible, MiQrViewModel(autenticacion, mockk(), mockk()).estado.value)
    }

    // ---- Historial ----

    @Test
    fun historial_estado_inicial_es_cargando() {
        val historial = mockk<ServicioHistorial>()
        every { historial.observarMiHistorial() } returns kotlinx.coroutines.flow.emptyFlow()
        assertTrue(HistorialViewModel(historial).estado.value.cargando)
    }

    @Test
    fun historial_muestra_los_registros() = kotlinx.coroutines.test.runTest {
        val registro = RegistroAcceso(1, "Ana", TipoAcceso.ENTRADA, Instant.EPOCH, 1)
        val historial = mockk<ServicioHistorial>()
        every { historial.observarMiHistorial() } returns flowOf(listOf(registro))
        val vm = HistorialViewModel(historial)
        backgroundScope.launch(principal.dispatcher) { vm.estado.collect {} }
        assertEquals(listOf(registro), vm.estado.value.registros)
        assertEquals(false, vm.estado.value.cargando)
    }

    // ---- Ajustes ----

    @Test
    fun cerrar_sesion_llama_al_servicio() {
        AjustesViewModel(autenticacion).cerrarSesion()
        coVerify { autenticacion.cerrarSesion() }
    }

    private fun servicioConAjustes(): ServicioAjustesNotificaciones {
        val s = mockk<ServicioAjustesNotificaciones>()
        val inv = Invitado(1, "Luis", 1)
        every { s.observar() } returns flowOf(AjustesNotificaciones(listOf(AjusteInvitado(inv, false, false)), false, false))
        return s
    }

    @Test
    fun notificaciones_estado_inicial_vacio() {
        val s = mockk<ServicioAjustesNotificaciones>()
        every { s.observar() } returns kotlinx.coroutines.flow.emptyFlow()
        val vm = NotificacionesInvitadosViewModel(s)
        assertEquals(AjustesNotificaciones.VACIO, vm.ajustes.value)
        assertEquals(false, vm.error.value)
    }

    @Test
    fun cambiar_invitado_llama_al_servicio_sin_error() {
        val s = servicioConAjustes()
        coEvery { s.cambiarInvitado(1, true) } returns true
        val vm = NotificacionesInvitadosViewModel(s)
        vm.cambiarInvitado(1, true)
        coVerify { s.cambiarInvitado(1, true) }
        assertEquals(false, vm.error.value)
    }

    @Test
    fun si_el_servicio_falla_se_marca_error_y_se_puede_descartar() {
        val s = servicioConAjustes()
        coEvery { s.cambiarSilenciarTodos(true) } returns false
        val vm = NotificacionesInvitadosViewModel(s)
        vm.cambiarSilenciarTodos(true)
        assertEquals(true, vm.error.value)
        vm.descartarError()
        assertEquals(false, vm.error.value)
    }
}

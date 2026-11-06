package victor.android.portly.ui.invitados

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import victor.android.portly.data.qr.CompartidorQr
import victor.android.portly.data.qr.GeneradorQr
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.qr.QrActivo
import victor.android.portly.domain.qr.QrGenerado
import victor.android.portly.domain.qr.ResultadoCrearQr
import victor.android.portly.domain.qr.ServicioQrInvitado
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import victor.android.portly.ui.ReglaDispatcherPrincipal
import java.time.Instant

class InvitadosViewModelsTest {
    @get:Rule val principal = ReglaDispatcherPrincipal()

    private val autenticacion = mockk<ServicioAutenticacion>().also {
        every { it.observarSesion() } returns MutableStateFlow(Sesion(1, false))
    }
    private val servicioQr = mockk<ServicioQrInvitado>(relaxed = true)
    private val invitado = Invitado(5, "Luis", 1)
    private val qr = QrInvitado(9, 5, 1, Instant.EPOCH)

    // ---- Lista ----

    @Test
    fun lista_estado_inicial_cargando() {
        every { servicioQr.observarActivosHoy(1) } returns kotlinx.coroutines.flow.emptyFlow()
        assertTrue(InvitadosViewModel(autenticacion, servicioQr).estado.value.cargando)
    }

    @Test
    fun lista_muestra_los_qr_activos() = runTest {
        every { servicioQr.observarActivosHoy(1) } returns flowOf(listOf(QrActivo(qr, invitado)))
        val vm = InvitadosViewModel(autenticacion, servicioQr)
        backgroundScope.launch(principal.dispatcher) { vm.estado.collect {} }
        assertEquals(1, vm.estado.value.activos.size)
        assertEquals(false, vm.estado.value.cargando)
    }

    // ---- Crear ----

    private fun crearVm(): CrearInvitadoViewModel {
        val repo = mockk<InvitadoRepositorio>()
        every { repo.observarPorResidente(1) } returns flowOf(listOf(invitado))
        return CrearInvitadoViewModel(autenticacion, servicioQr, repo)
    }

    @Test
    fun crear_estado_inicial() {
        val e = crearVm().estado.value
        assertEquals("", e.nombre)
        assertNull(e.error)
    }

    @Test
    fun crear_con_nombre_vacio_muestra_error() {
        coEvery { servicioQr.crear(1, "") } returns ResultadoCrearQr.NombreVacio
        val vm = crearVm()
        vm.crear()
        assertEquals(ErrorCrearInvitado.NOMBRE_VACIO, vm.estado.value.error)
        assertEquals(false, vm.estado.value.creando)
    }

    @Test
    fun crear_exitoso_emite_el_id_del_qr() = runTest {
        coEvery { servicioQr.crear(1, "Luis") } returns
            ResultadoCrearQr.Exito(QrGenerado(qr, invitado, "c"), invitadoNuevo = false)
        val vm = crearVm()
        vm.cambiarNombre("Luis")
        vm.qrCreado.test {
            vm.crear()
            assertEquals(9L, awaitItem())
        }
        assertEquals("", vm.estado.value.nombre)
    }

    @Test
    fun crear_con_residente_no_disponible_muestra_error() {
        coEvery { servicioQr.crear(any(), any()) } returns ResultadoCrearQr.ResidenteNoDisponible
        val vm = crearVm()
        vm.cambiarNombre("Luis")
        vm.crear()
        assertEquals(ErrorCrearInvitado.RESIDENTE_NO_DISPONIBLE, vm.estado.value.error)
    }

    @Test
    fun crear_ofrece_los_invitados_guardados() = runTest {
        val vm = crearVm()
        backgroundScope.launch(principal.dispatcher) { vm.guardados.collect {} }
        assertEquals(listOf("Luis"), vm.guardados.value)
    }

    // ---- Detalle ----

    private fun detalleVm(
        id: Long = 9,
        generador: GeneradorQr = mockk(relaxed = true),
        compartidor: CompartidorQr = mockk(relaxed = true),
    ): DetalleQrViewModel {
        every { servicioQr.observarActivosHoy(1) } returns flowOf(listOf(QrActivo(qr, invitado)))
        coEvery { servicioQr.contenidoDe(qr) } returns "contenido"
        return DetalleQrViewModel(
            SavedStateHandle(mapOf(DetalleQrViewModel.ARG_QR_ID to id)),
            autenticacion, servicioQr, generador, compartidor,
        )
    }

    @Test
    fun detalle_de_qr_vigente_queda_listo() {
        assertEquals(DetalleQrUiState.Listo(9, "Luis", "contenido"), detalleVm().estado.value)
    }

    @Test
    fun detalle_de_qr_inexistente_no_vigente() {
        assertEquals(DetalleQrUiState.NoVigente, detalleVm(id = 99).estado.value)
    }

    @Test
    fun cancelar_llama_al_servicio_y_avisa() = runTest {
        val vm = detalleVm()
        vm.cancelado.test {
            vm.cancelar()
            awaitItem()
        }
        coVerify { servicioQr.cancelar(9) }
        assertEquals(DetalleQrUiState.NoVigente, vm.estado.value)
    }

    @Test
    fun cancelar_sin_qr_vigente_no_hace_nada() {
        val vm = detalleVm(id = 99)
        vm.cancelar()
        coVerify(exactly = 0) { servicioQr.cancelar(any()) }
    }

    @Test
    fun compartir_por_whatsapp_limita_el_intent_a_esa_app() {
        val compartidor = mockk<CompartidorQr>()
        val intent = mockk<Intent>()
        every { compartidor.crearIntent(any(), "Luis", CompartidorQr.WHATSAPP) } returns intent
        val vm = detalleVm(compartidor = compartidor)
        assertEquals(intent, vm.intentCompartir(100, soloWhatsapp = true))
    }

    @Test
    fun compartir_sin_qr_listo_devuelve_null() {
        assertNull(detalleVm(id = 99).intentCompartir(100, soloWhatsapp = true))
    }
}

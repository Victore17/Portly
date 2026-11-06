package victor.android.portly.ui.admin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import victor.android.portly.domain.administracion.FiltroRegistro
import victor.android.portly.domain.administracion.QrActivoAdmin
import victor.android.portly.domain.administracion.ResultadoAdmin
import victor.android.portly.domain.administracion.ServicioAdministracion
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Residente
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import victor.android.portly.ui.ReglaDispatcherPrincipal
import java.time.Instant

class AdminViewModelsTest {
    @get:Rule val principal = ReglaDispatcherPrincipal()

    private val admin = mockk<ServicioAdministracion>(relaxed = true)
    private val ana = Residente(1, "Ana", "Casa 1", "ana@x.com", esAdmin = true)
    private val beto = Residente(2, "Beto", "Casa 2", "beto@x.com")

    // ---- Residentes ----

    @Test
    fun residentes_estado_inicial_cargando() {
        every { admin.observarResidentes() } returns kotlinx.coroutines.flow.emptyFlow()
        assertTrue(ResidentesViewModel(admin).estado.value.cargando)
    }

    @Test
    fun residentes_se_listan_por_nombre() = runTest {
        every { admin.observarResidentes() } returns flowOf(listOf(beto, ana))
        val vm = ResidentesViewModel(admin)
        backgroundScope.launch(principal.dispatcher) { vm.estado.collect {} }
        assertEquals(listOf(ana, beto), vm.estado.value.residentes)
    }

    @Test
    fun desactivar_al_ultimo_admin_muestra_error() {
        every { admin.observarResidentes() } returns flowOf(emptyList())
        coEvery { admin.cambiarActivo(1, false) } returns ResultadoAdmin.UltimoAdministrador
        val vm = ResidentesViewModel(admin)
        vm.cambiarActivo(1, false)
        assertEquals("Debe quedar al menos un administrador activo", vm.error.value)
        vm.descartarError()
        assertNull(vm.error.value)
    }

    @Test
    fun cambiar_activo_exitoso_no_deja_error() {
        every { admin.observarResidentes() } returns flowOf(emptyList())
        coEvery { admin.cambiarActivo(2, true) } returns ResultadoAdmin.Exito()
        val vm = ResidentesViewModel(admin)
        vm.cambiarActivo(2, true)
        assertNull(vm.error.value)
    }

    // ---- Formulario ----

    private fun formulario(id: Long? = null, repo: ResidenteRepositorio = mockk(relaxed = true)) =
        FormularioResidenteViewModel(
            SavedStateHandle(if (id == null) emptyMap() else mapOf(FormularioResidenteViewModel.ARG_RESIDENTE_ID to id)),
            admin, repo,
        )

    @Test
    fun formulario_nuevo_empieza_vacio() {
        val e = formulario().estado.value
        assertEquals(false, e.esEdicion)
        assertEquals("", e.nombre)
    }

    @Test
    fun formulario_de_edicion_carga_al_residente() {
        val repo = mockk<ResidenteRepositorio>()
        coEvery { repo.obtener(2) } returns beto
        val e = formulario(2, repo).estado.value
        assertTrue(e.esEdicion)
        assertEquals("Beto", e.nombre)
        assertEquals("beto@x.com", e.contacto)
    }

    @Test
    fun formulario_agregar_exitoso_avisa_y_llama_al_servicio() = runTest {
        coEvery { admin.agregarResidente("Cora", "Casa 3", "c@x.com", "pw", false) } returns ResultadoAdmin.Exito(3)
        val vm = formulario()
        vm.cambiarNombre("Cora"); vm.cambiarDomicilio("Casa 3"); vm.cambiarContacto("c@x.com"); vm.cambiarContrasena("pw")
        vm.guardado.test {
            vm.guardar()
            awaitItem()
        }
        assertNull(vm.estado.value.error)
    }

    @Test
    fun formulario_con_contacto_duplicado_muestra_error() {
        coEvery { admin.agregarResidente(any(), any(), any(), any(), any()) } returns ResultadoAdmin.ContactoDuplicado
        val vm = formulario()
        vm.guardar()
        assertEquals("Ya existe un residente con ese contacto", vm.estado.value.error)
        assertEquals(false, vm.estado.value.guardando)
    }

    @Test
    fun formulario_editar_envia_el_residente_modificado() {
        val repo = mockk<ResidenteRepositorio>()
        coEvery { repo.obtener(2) } returns beto
        coEvery { admin.editarResidente(any()) } returns ResultadoAdmin.Exito()
        val vm = formulario(2, repo)
        vm.cambiarNombre("Beto Pérez")
        vm.guardar()
        coVerify { admin.editarResidente(beto.copy(nombre = "Beto Pérez")) }
    }

    @Test
    fun formulario_editar_residente_inexistente_muestra_error() {
        val repo = mockk<ResidenteRepositorio>()
        coEvery { repo.obtener(8) } returns null
        assertEquals("El residente ya no existe", formulario(8, repo).estado.value.error)
    }

    // ---- Registro completo ----

    @Test
    fun registro_sin_filtro_lista_todo_ordenado_reciente_primero() = runTest {
        val viejo = RegistroAcceso(1, "Ana", TipoAcceso.ENTRADA, Instant.parse("2026-11-01T10:00:00Z"), 1)
        val nuevo = RegistroAcceso(2, "Beto", TipoAcceso.SALIDA, Instant.parse("2026-11-02T10:00:00Z"), 2)
        every { admin.observarRegistro(FiltroRegistro()) } returns flowOf(listOf(viejo, nuevo))
        every { admin.observarResidentes() } returns flowOf(listOf(ana, beto))
        val vm = RegistroCompletoViewModel(admin)
        backgroundScope.launch(principal.dispatcher) { vm.estado.collect {} }
        assertEquals(listOf(nuevo, viejo), vm.estado.value.registros)
        assertNull(vm.estado.value.filtroResidenteId)
    }

    @Test
    fun registro_filtrar_por_residente_cambia_la_consulta() = runTest {
        val deBeto = RegistroAcceso(2, "Beto", TipoAcceso.SALIDA, Instant.EPOCH, 2)
        every { admin.observarRegistro(FiltroRegistro()) } returns flowOf(emptyList())
        every { admin.observarRegistro(FiltroRegistro(residenteId = 2)) } returns flowOf(listOf(deBeto))
        every { admin.observarResidentes() } returns flowOf(listOf(ana, beto))
        val vm = RegistroCompletoViewModel(admin)
        backgroundScope.launch(principal.dispatcher) { vm.estado.collect {} }
        vm.filtrarPorResidente(2)
        assertEquals(listOf(deBeto), vm.estado.value.registros)
        assertEquals(2L, vm.estado.value.filtroResidenteId)
    }

    // ---- QRs activos ----

    @Test
    fun qrs_activos_se_listan_y_se_pueden_cancelar() = runTest {
        val qrAdmin = QrActivoAdmin(QrInvitado(9, 5, 1, Instant.EPOCH), Invitado(5, "Luis", 1), ana)
        every { admin.observarQrsActivos() } returns flowOf(listOf(qrAdmin))
        coEvery { admin.cancelarQr(9) } returns ResultadoAdmin.NoAutorizado
        val vm = QrsActivosAdminViewModel(admin)
        backgroundScope.launch(principal.dispatcher) { vm.estado.collect {} }
        assertEquals(listOf(qrAdmin), vm.estado.value.qrs)
        vm.cancelar(9)
        coVerify { admin.cancelarQr(9) }
        assertEquals("No tienes permiso de administrador", vm.error.value)
    }
}

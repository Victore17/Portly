package victor.android.portly.ui

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import victor.android.portly.domain.autenticacion.ResultadoLogin
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.ui.login.ErrorLogin
import victor.android.portly.ui.login.LoginViewModel
import victor.android.portly.ui.navegacion.Rutas
import victor.android.portly.ui.navegacion.pestanasVisibles
import victor.android.portly.ui.sesion.EstadoSesion
import victor.android.portly.ui.sesion.SesionViewModel

class SesionYLoginViewModelTest {
    @get:Rule val principal = ReglaDispatcherPrincipal()

    private val autenticacion = mockk<ServicioAutenticacion>(relaxed = true)

    @Test
    fun sin_sesion_guardada_el_estado_es_SinSesion() {
        coEvery { autenticacion.restaurarSesion() } returns null
        every { autenticacion.observarSesion() } returns MutableStateFlow(null)
        assertEquals(EstadoSesion.SinSesion, SesionViewModel(autenticacion).estado.value)
    }

    @Test
    fun una_sesion_de_admin_muestra_administracion() {
        every { autenticacion.observarSesion() } returns MutableStateFlow(Sesion(1, esAdmin = true))
        val estado = SesionViewModel(autenticacion).estado.value as EstadoSesion.ConSesion
        assertTrue(estado.mostrarAdministracion)
    }

    @Test
    fun una_sesion_normal_no_muestra_administracion() {
        every { autenticacion.observarSesion() } returns MutableStateFlow(Sesion(1, esAdmin = false))
        val estado = SesionViewModel(autenticacion).estado.value as EstadoSesion.ConSesion
        assertFalse(estado.mostrarAdministracion)
    }

    @Test
    fun cerrar_sesion_devuelve_el_estado_a_SinSesion() {
        val sesion = MutableStateFlow<Sesion?>(Sesion(1, false))
        every { autenticacion.observarSesion() } returns sesion
        val vm = SesionViewModel(autenticacion)
        assertTrue(vm.estado.value is EstadoSesion.ConSesion)
        sesion.value = null
        assertEquals(EstadoSesion.SinSesion, vm.estado.value)
    }

    @Test
    fun la_pestana_administracion_depende_del_rol() {
        assertTrue(pestanasVisibles(esAdmin = true).any { it.ruta == Rutas.ADMIN })
        assertFalse(pestanasVisibles(esAdmin = false).any { it.ruta == Rutas.ADMIN })
        assertEquals(4, pestanasVisibles(esAdmin = false).size)
    }

    @Test
    fun login_estado_inicial_vacio() {
        val estado = LoginViewModel(autenticacion).estado.value
        assertEquals("", estado.contacto)
        assertFalse(estado.cargando)
        assertEquals(null, estado.error)
    }

    @Test
    fun login_con_campos_vacios_no_llama_al_servicio() {
        val vm = LoginViewModel(autenticacion)
        vm.cambiarContacto("a@b.c")
        vm.iniciarSesion()
        assertEquals(ErrorLogin.CAMPOS_VACIOS, vm.estado.value.error)
        coVerify(exactly = 0) { autenticacion.iniciarSesion(any(), any()) }
    }

    @Test
    fun login_con_credenciales_incorrectas_muestra_error() {
        coEvery { autenticacion.iniciarSesion("a@b.c", "mal") } returns ResultadoLogin.CredencialesInvalidas
        val vm = LoginViewModel(autenticacion)
        vm.cambiarContacto("a@b.c"); vm.cambiarContrasena("mal")
        vm.iniciarSesion()
        assertEquals(ErrorLogin.CREDENCIALES_INVALIDAS, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun login_con_residente_inactivo_muestra_error() {
        coEvery { autenticacion.iniciarSesion(any(), any()) } returns ResultadoLogin.ResidenteInactivo
        val vm = LoginViewModel(autenticacion)
        vm.cambiarContacto("a@b.c"); vm.cambiarContrasena("x")
        vm.iniciarSesion()
        assertEquals(ErrorLogin.RESIDENTE_INACTIVO, vm.estado.value.error)
    }

    @Test
    fun login_correcto_no_deja_error() {
        coEvery { autenticacion.iniciarSesion(any(), any()) } returns ResultadoLogin.Exito(Sesion(1, false))
        val vm = LoginViewModel(autenticacion)
        vm.cambiarContacto("a@b.c"); vm.cambiarContrasena("x")
        vm.iniciarSesion()
        assertEquals(null, vm.estado.value.error)
        coVerify { autenticacion.iniciarSesion("a@b.c", "x") }
    }

    @Test
    fun escribir_limpia_el_error() {
        val vm = LoginViewModel(autenticacion)
        vm.iniciarSesion()
        assertEquals(ErrorLogin.CAMPOS_VACIOS, vm.estado.value.error)
        vm.cambiarContacto("a")
        assertEquals(null, vm.estado.value.error)
    }
}

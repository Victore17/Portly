package victor.android.portly.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import victor.android.portly.domain.autenticacion.ResultadoLogin
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import javax.inject.Inject

enum class ErrorLogin { CAMPOS_VACIOS, CREDENCIALES_INVALIDAS, RESIDENTE_INACTIVO }

data class LoginUiState(
    val contacto: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: ErrorLogin? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val autenticacion: ServicioAutenticacion,
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarContacto(valor: String) = _estado.update { it.copy(contacto = valor, error = null) }

    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }

    /** Si el login funciona, la sesión cambia y la navegación reacciona sola. */
    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando) return
        if (actual.contacto.isBlank() || actual.contrasena.isEmpty()) {
            _estado.update { it.copy(error = ErrorLogin.CAMPOS_VACIOS) }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val error = when (autenticacion.iniciarSesion(actual.contacto, actual.contrasena)) {
                is ResultadoLogin.Exito -> null
                ResultadoLogin.CredencialesInvalidas -> ErrorLogin.CREDENCIALES_INVALIDAS
                ResultadoLogin.ResidenteInactivo -> ErrorLogin.RESIDENTE_INACTIVO
            }
            _estado.update { it.copy(cargando = false, error = error) }
        }
    }
}

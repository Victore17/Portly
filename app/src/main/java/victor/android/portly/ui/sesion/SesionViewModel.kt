package victor.android.portly.ui.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import javax.inject.Inject

/** Estado de la sesión que decide qué ve la persona al abrir la app. */
sealed interface EstadoSesion {
    /** Todavía se está restaurando la sesión guardada. */
    data object Cargando : EstadoSesion

    data object SinSesion : EstadoSesion

    /** La pestaña Administración solo se muestra si [esAdmin] es true. */
    data class ConSesion(val residenteId: Long, val esAdmin: Boolean) : EstadoSesion {
        val mostrarAdministracion: Boolean get() = esAdmin
    }
}

@HiltViewModel
class SesionViewModel @Inject constructor(
    private val autenticacion: ServicioAutenticacion,
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            autenticacion.restaurarSesion()
            autenticacion.observarSesion().collect { sesion ->
                _estado.value = if (sesion == null) {
                    EstadoSesion.SinSesion
                } else {
                    EstadoSesion.ConSesion(sesion.residenteId, sesion.esAdmin)
                }
            }
        }
    }
}

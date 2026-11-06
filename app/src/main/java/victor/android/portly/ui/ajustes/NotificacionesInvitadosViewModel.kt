package victor.android.portly.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import victor.android.portly.domain.notificaciones.AjustesNotificaciones
import victor.android.portly.domain.notificaciones.ServicioAjustesNotificaciones
import javax.inject.Inject

@HiltViewModel
class NotificacionesInvitadosViewModel @Inject constructor(
    private val servicio: ServicioAjustesNotificaciones,
) : ViewModel() {

    val ajustes: StateFlow<AjustesNotificaciones> = servicio.observar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesNotificaciones.VACIO)

    private val _error = MutableStateFlow(false)
    /** true si un cambio no se pudo guardar (por ejemplo, sin sesión). */
    val error: StateFlow<Boolean> = _error.asStateFlow()

    fun cambiarInvitado(invitadoId: Long, silenciado: Boolean) {
        viewModelScope.launch { _error.value = !servicio.cambiarInvitado(invitadoId, silenciado) }
    }

    fun cambiarSilenciarTodos(silenciar: Boolean) {
        viewModelScope.launch { _error.value = !servicio.cambiarSilenciarTodos(silenciar) }
    }

    fun descartarError() { _error.value = false }
}

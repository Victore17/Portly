package victor.android.portly.ui.invitados

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.qr.QrActivo
import victor.android.portly.domain.qr.ServicioQrInvitado
import javax.inject.Inject

data class InvitadosUiState(
    val cargando: Boolean = true,
    /** QR de invitados que siguen vigentes hoy. */
    val activos: List<QrActivo> = emptyList(),
)

@HiltViewModel
class InvitadosViewModel @Inject constructor(
    autenticacion: ServicioAutenticacion,
    servicioQr: ServicioQrInvitado,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<InvitadosUiState> = autenticacion.observarSesion()
        .flatMapLatest { sesion ->
            if (sesion == null) flowOf(emptyList()) else servicioQr.observarActivosHoy(sesion.residenteId)
        }
        .map { InvitadosUiState(cargando = false, activos = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvitadosUiState())
}

package victor.android.portly.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import victor.android.portly.domain.administracion.QrActivoAdmin
import victor.android.portly.domain.administracion.ResultadoAdmin
import victor.android.portly.domain.administracion.ServicioAdministracion
import javax.inject.Inject

data class QrsActivosAdminUiState(
    val cargando: Boolean = true,
    val qrs: List<QrActivoAdmin> = emptyList(),
)

@HiltViewModel
class QrsActivosAdminViewModel @Inject constructor(
    private val administracion: ServicioAdministracion,
) : ViewModel() {

    val estado: StateFlow<QrsActivosAdminUiState> = administracion.observarQrsActivos()
        .map { QrsActivosAdminUiState(cargando = false, qrs = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QrsActivosAdminUiState())

    private val _error = MutableStateFlow<ResultadoAdmin?>(null)
    val error: StateFlow<ResultadoAdmin?> = _error.asStateFlow()

    fun cancelar(qrId: Long) {
        viewModelScope.launch { _error.value = administracion.cancelarQr(qrId).comoError() }
    }

    fun descartarError() { _error.value = null }
}

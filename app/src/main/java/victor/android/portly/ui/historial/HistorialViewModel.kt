package victor.android.portly.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import victor.android.portly.domain.historial.ServicioHistorial
import victor.android.portly.domain.modelo.RegistroAcceso
import javax.inject.Inject

data class HistorialUiState(
    val cargando: Boolean = true,
    val registros: List<RegistroAcceso> = emptyList(),
)

@HiltViewModel
class HistorialViewModel @Inject constructor(
    historial: ServicioHistorial,
) : ViewModel() {

    val estado: StateFlow<HistorialUiState> = historial.observarMiHistorial()
        .map { HistorialUiState(cargando = false, registros = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())
}

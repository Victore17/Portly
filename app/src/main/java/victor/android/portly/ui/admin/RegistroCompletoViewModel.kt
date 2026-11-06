package victor.android.portly.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import victor.android.portly.domain.administracion.FiltroRegistro
import victor.android.portly.domain.administracion.ServicioAdministracion
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Residente
import javax.inject.Inject

data class RegistroCompletoUiState(
    val cargando: Boolean = true,
    val registros: List<RegistroAcceso> = emptyList(),
    val residentes: List<Residente> = emptyList(),
    /** null = todos los residentes. */
    val filtroResidenteId: Long? = null,
)

@HiltViewModel
class RegistroCompletoViewModel @Inject constructor(
    private val administracion: ServicioAdministracion,
) : ViewModel() {

    private val filtro = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<RegistroCompletoUiState> = combine(
        filtro.flatMapLatest { id -> administracion.observarRegistro(FiltroRegistro(residenteId = id)) },
        administracion.observarResidentes(),
        filtro,
    ) { registros, residentes, id ->
        RegistroCompletoUiState(
            cargando = false,
            registros = registros.sortedWith(compareByDescending<RegistroAcceso> { it.momento }.thenByDescending { it.id }),
            residentes = residentes.sortedBy { it.nombre.lowercase() },
            filtroResidenteId = id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistroCompletoUiState())

    fun filtrarPorResidente(residenteId: Long?) { filtro.value = residenteId }
}

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
import victor.android.portly.domain.administracion.ResultadoAdmin
import victor.android.portly.domain.administracion.ServicioAdministracion
import victor.android.portly.domain.modelo.Residente
import javax.inject.Inject

data class ResidentesUiState(
    val cargando: Boolean = true,
    val residentes: List<Residente> = emptyList(),
)

/** Texto de error de las acciones de administración, o null si salió bien. */
fun ResultadoAdmin.mensajeDeError(): String? = when (this) {
    is ResultadoAdmin.Exito -> null
    ResultadoAdmin.NoAutorizado -> "No tienes permiso de administrador"
    ResultadoAdmin.DatosInvalidos -> "Completa todos los campos"
    ResultadoAdmin.ContactoDuplicado -> "Ya existe un residente con ese contacto"
    ResultadoAdmin.NoExiste -> "El residente ya no existe"
    ResultadoAdmin.UltimoAdministrador -> "Debe quedar al menos un administrador activo"
}

@HiltViewModel
class ResidentesViewModel @Inject constructor(
    private val administracion: ServicioAdministracion,
) : ViewModel() {

    val estado: StateFlow<ResidentesUiState> = administracion.observarResidentes()
        .map { ResidentesUiState(cargando = false, residentes = it.sortedBy { r -> r.nombre.lowercase() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResidentesUiState())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cambiarActivo(residenteId: Long, activo: Boolean) {
        viewModelScope.launch {
            _error.value = administracion.cambiarActivo(residenteId, activo).mensajeDeError()
        }
    }

    fun descartarError() { _error.value = null }
}

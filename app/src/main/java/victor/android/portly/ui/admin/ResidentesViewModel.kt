package victor.android.portly.ui.admin

import androidx.annotation.StringRes
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
import victor.android.portly.R
import victor.android.portly.domain.administracion.ResultadoAdmin
import victor.android.portly.domain.administracion.ServicioAdministracion
import victor.android.portly.domain.modelo.Residente
import javax.inject.Inject

data class ResidentesUiState(
    val cargando: Boolean = true,
    val residentes: List<Residente> = emptyList(),
)

/** El resultado si fue un error, o null si la acción salió bien. La interfaz lo traduce a texto. */
fun ResultadoAdmin.comoError(): ResultadoAdmin? = if (this is ResultadoAdmin.Exito) null else this

/** Texto (recurso) del error, o null si [this] es un éxito. */
@StringRes
fun ResultadoAdmin.mensajeRes(): Int? = when (this) {
    is ResultadoAdmin.Exito -> null
    ResultadoAdmin.NoAutorizado -> R.string.error_admin_no_autorizado
    ResultadoAdmin.DatosInvalidos -> R.string.error_admin_datos_invalidos
    ResultadoAdmin.ContactoDuplicado -> R.string.error_admin_contacto_duplicado
    ResultadoAdmin.NoExiste -> R.string.error_admin_no_existe
    ResultadoAdmin.UltimoAdministrador -> R.string.error_admin_ultimo_administrador
}

@HiltViewModel
class ResidentesViewModel @Inject constructor(
    private val administracion: ServicioAdministracion,
) : ViewModel() {

    val estado: StateFlow<ResidentesUiState> = administracion.observarResidentes()
        .map { ResidentesUiState(cargando = false, residentes = it.sortedBy { r -> r.nombre.lowercase() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResidentesUiState())

    private val _error = MutableStateFlow<ResultadoAdmin?>(null)
    val error: StateFlow<ResultadoAdmin?> = _error.asStateFlow()

    fun cambiarActivo(residenteId: Long, activo: Boolean) {
        viewModelScope.launch {
            _error.value = administracion.cambiarActivo(residenteId, activo).comoError()
        }
    }

    fun descartarError() { _error.value = null }
}

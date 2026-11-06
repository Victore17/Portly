package victor.android.portly.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import victor.android.portly.domain.administracion.ResultadoAdmin
import victor.android.portly.domain.administracion.ServicioAdministracion
import victor.android.portly.domain.modelo.Residente
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import javax.inject.Inject

data class FormularioResidenteUiState(
    val cargando: Boolean = false,
    val esEdicion: Boolean = false,
    val nombre: String = "",
    val domicilio: String = "",
    val contacto: String = "",
    /** Solo se usa al agregar; la edición no cambia la contraseña. */
    val contrasena: String = "",
    val esAdmin: Boolean = false,
    val guardando: Boolean = false,
    val error: ResultadoAdmin? = null,
)

@HiltViewModel
class FormularioResidenteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val administracion: ServicioAdministracion,
    private val residentes: ResidenteRepositorio,
) : ViewModel() {

    private val residenteId: Long = savedStateHandle.get<Long>(ARG_RESIDENTE_ID) ?: SIN_RESIDENTE
    private var original: Residente? = null

    private val _estado = MutableStateFlow(FormularioResidenteUiState(esEdicion = residenteId != SIN_RESIDENTE))
    val estado: StateFlow<FormularioResidenteUiState> = _estado.asStateFlow()

    private val _guardado = Channel<Unit>(Channel.BUFFERED)
    val guardado: Flow<Unit> = _guardado.receiveAsFlow()

    init {
        if (residenteId != SIN_RESIDENTE) {
            _estado.update { it.copy(cargando = true) }
            viewModelScope.launch {
                val r = residentes.obtener(residenteId)
                original = r
                _estado.update {
                    if (r == null) {
                        it.copy(cargando = false, error = ResultadoAdmin.NoExiste)
                    } else {
                        it.copy(
                            cargando = false,
                            nombre = r.nombre, domicilio = r.domicilio,
                            contacto = r.contacto, esAdmin = r.esAdmin,
                        )
                    }
                }
            }
        }
    }

    fun cambiarNombre(v: String) = _estado.update { it.copy(nombre = v, error = null) }
    fun cambiarDomicilio(v: String) = _estado.update { it.copy(domicilio = v, error = null) }
    fun cambiarContacto(v: String) = _estado.update { it.copy(contacto = v, error = null) }
    fun cambiarContrasena(v: String) = _estado.update { it.copy(contrasena = v, error = null) }
    fun cambiarEsAdmin(v: Boolean) = _estado.update { it.copy(esAdmin = v, error = null) }

    fun guardar() {
        val e = _estado.value
        if (e.guardando || e.cargando) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val resultado = if (e.esEdicion) {
                val base = original
                if (base == null) {
                    ResultadoAdmin.NoExiste
                } else {
                    administracion.editarResidente(
                        base.copy(nombre = e.nombre, domicilio = e.domicilio, contacto = e.contacto, esAdmin = e.esAdmin),
                    )
                }
            } else {
                administracion.agregarResidente(e.nombre, e.domicilio, e.contacto, e.contrasena, e.esAdmin)
            }
            val error = resultado.comoError()
            _estado.update { it.copy(guardando = false, error = error) }
            if (error == null) _guardado.send(Unit)
        }
    }

    companion object {
        const val ARG_RESIDENTE_ID = "residenteId"
        /** Valor de la ruta cuando se agrega un residente nuevo. */
        const val SIN_RESIDENTE = -1L
    }
}

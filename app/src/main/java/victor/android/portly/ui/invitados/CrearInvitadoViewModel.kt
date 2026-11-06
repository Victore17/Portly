package victor.android.portly.ui.invitados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.qr.ResultadoCrearQr
import victor.android.portly.domain.qr.ServicioQrInvitado
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import javax.inject.Inject

enum class ErrorCrearInvitado { NOMBRE_VACIO, RESIDENTE_NO_DISPONIBLE }

data class CrearInvitadoUiState(
    val nombre: String = "",
    val creando: Boolean = false,
    val error: ErrorCrearInvitado? = null,
)

@HiltViewModel
class CrearInvitadoViewModel @Inject constructor(
    private val autenticacion: ServicioAutenticacion,
    private val servicioQr: ServicioQrInvitado,
    invitados: InvitadoRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow(CrearInvitadoUiState())
    val estado: StateFlow<CrearInvitadoUiState> = _estado.asStateFlow()

    /** Nombres de invitados guardados, para reutilizarlos con un toque. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val guardados: StateFlow<List<String>> = autenticacion.observarSesion()
        .flatMapLatest { s ->
            if (s == null) flowOf(emptyList()) else invitados.observarPorResidente(s.residenteId)
        }
        .map { lista -> lista.map { it.nombre }.sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _qrCreado = Channel<Long>(Channel.BUFFERED)
    /** Emite el id del QR recién creado para abrir su detalle. */
    val qrCreado: Flow<Long> = _qrCreado.receiveAsFlow()

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombre = valor, error = null) }

    fun crear() {
        val actual = _estado.value
        if (actual.creando) return
        _estado.update { it.copy(creando = true, error = null) }
        viewModelScope.launch {
            val sesion = autenticacion.observarSesion().firstOrNull()
            val resultado = if (sesion == null) {
                ResultadoCrearQr.ResidenteNoDisponible
            } else {
                servicioQr.crear(sesion.residenteId, actual.nombre)
            }
            when (resultado) {
                is ResultadoCrearQr.Exito -> {
                    _estado.value = CrearInvitadoUiState()
                    _qrCreado.send(resultado.generado.qr.id)
                }
                ResultadoCrearQr.NombreVacio ->
                    _estado.update { it.copy(creando = false, error = ErrorCrearInvitado.NOMBRE_VACIO) }
                ResultadoCrearQr.ResidenteNoDisponible ->
                    _estado.update { it.copy(creando = false, error = ErrorCrearInvitado.RESIDENTE_NO_DISPONIBLE) }
            }
        }
    }
}

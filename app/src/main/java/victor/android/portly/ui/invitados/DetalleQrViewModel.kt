package victor.android.portly.ui.invitados

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import victor.android.portly.data.qr.CompartidorQr
import victor.android.portly.data.qr.GeneradorQr
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.qr.ServicioQrInvitado
import javax.inject.Inject

sealed interface DetalleQrUiState {
    data object Cargando : DetalleQrUiState
    data class Listo(val qrId: Long, val nombreInvitado: String, val contenido: String) : DetalleQrUiState
    /** El QR ya no existe, está cancelado o dejó de ser vigente (vale solo el día de creación). */
    data object NoVigente : DetalleQrUiState
}

@HiltViewModel
class DetalleQrViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val autenticacion: ServicioAutenticacion,
    private val servicioQr: ServicioQrInvitado,
    private val generador: GeneradorQr,
    private val compartidor: CompartidorQr,
) : ViewModel() {

    private val qrId: Long = savedStateHandle.get<Long>(ARG_QR_ID) ?: -1L

    private val _estado = MutableStateFlow<DetalleQrUiState>(DetalleQrUiState.Cargando)
    val estado: StateFlow<DetalleQrUiState> = _estado.asStateFlow()

    private val _cancelado = Channel<Unit>(Channel.BUFFERED)
    /** Emite cuando el QR se canceló, para volver a la lista. */
    val cancelado: Flow<Unit> = _cancelado.receiveAsFlow()

    init {
        viewModelScope.launch {
            val sesion = autenticacion.observarSesion().firstOrNull()
            val activo = sesion?.let {
                servicioQr.observarActivosHoy(it.residenteId).firstOrNull()?.firstOrNull { a -> a.qr.id == qrId }
            }
            _estado.value = if (activo == null) {
                DetalleQrUiState.NoVigente
            } else {
                DetalleQrUiState.Listo(qrId, activo.invitado.nombre, servicioQr.contenidoDe(activo.qr))
            }
        }
    }

    fun cancelar() {
        if (_estado.value !is DetalleQrUiState.Listo) return
        viewModelScope.launch {
            servicioQr.cancelar(qrId)
            _estado.value = DetalleQrUiState.NoVigente
            _cancelado.send(Unit)
        }
    }

    fun imagen(contenido: String, tamano: Int) = generador.generar(contenido, tamano)

    /** Intent para compartir la imagen del QR; con [soloWhatsapp] se limita a esa app. */
    fun intentCompartir(tamano: Int, soloWhatsapp: Boolean): Intent? {
        val listo = _estado.value as? DetalleQrUiState.Listo ?: return null
        return compartidor.crearIntent(
            generador.generar(listo.contenido, tamano),
            listo.nombreInvitado,
            if (soloWhatsapp) CompartidorQr.WHATSAPP else null,
        )
    }

    companion object {
        const val ARG_QR_ID = "qrId"
    }
}

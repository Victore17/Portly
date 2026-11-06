package victor.android.portly.ui.miqr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import victor.android.portly.data.qr.GeneradorQr
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.qr.ServicioQrResidente
import javax.inject.Inject

sealed interface MiQrUiState {
    data object Cargando : MiQrUiState
    data class Listo(val contenido: String) : MiQrUiState
    /** No hay sesión o el residente está inactivo. */
    data object NoDisponible : MiQrUiState
}

@HiltViewModel
class MiQrViewModel @Inject constructor(
    private val autenticacion: ServicioAutenticacion,
    private val servicioQr: ServicioQrResidente,
    private val generador: GeneradorQr,
) : ViewModel() {

    private val _estado = MutableStateFlow<MiQrUiState>(MiQrUiState.Cargando)
    val estado: StateFlow<MiQrUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = autenticacion.observarSesion().first()
            val contenido = sesion?.let { servicioQr.contenidoDe(it.residenteId) }
            _estado.value = if (contenido == null) MiQrUiState.NoDisponible else MiQrUiState.Listo(contenido)
        }
    }

    fun imagen(contenido: String, tamano: Int): Bitmap = generador.generar(contenido, tamano)
}

package victor.android.portly.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val autenticacion: ServicioAutenticacion,
) : ViewModel() {

    /** Al cerrar la sesión la navegación vuelve sola al login. */
    fun cerrarSesion() {
        viewModelScope.launch { autenticacion.cerrarSesion() }
    }
}

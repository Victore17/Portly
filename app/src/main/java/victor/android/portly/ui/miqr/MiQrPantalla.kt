package victor.android.portly.ui.miqr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import victor.android.portly.ui.comun.Cargando
import victor.android.portly.ui.comun.ColumnaCentrada
import victor.android.portly.ui.comun.EstadoVacio

@Composable
fun MiQrPantalla(viewModel: MiQrViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        MiQrUiState.Cargando -> Cargando()
        MiQrUiState.NoDisponible -> EstadoVacio("No se pudo generar tu QR. Habla con el administrador")
        is MiQrUiState.Listo -> {
            val imagen = remember(e.contenido) { viewModel.imagen(e.contenido, 768).asImageBitmap() }
            ColumnaCentrada {
                Image(imagen, contentDescription = "Tu código QR de acceso", modifier = Modifier.size(280.dp))
                Text("Muéstralo en el lector para entrar o salir")
            }
        }
    }
}

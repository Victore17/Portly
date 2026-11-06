package victor.android.portly.ui.historial

import victor.android.portly.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.ui.comun.BarraSuperior
import victor.android.portly.ui.comun.Cargando
import victor.android.portly.ui.comun.EstadoVacio
import victor.android.portly.ui.comun.comoFechaHora
import victor.android.portly.ui.comun.etiquetaRes

@Composable
fun HistorialPantalla(viewModel: HistorialViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Scaffold(topBar = { BarraSuperior(stringResource(R.string.historial_titulo)) }) { padding ->
        when {
            estado.cargando -> Cargando(Modifier.padding(padding))
            estado.registros.isEmpty() -> EstadoVacio(stringResource(R.string.historial_vacio), Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(estado.registros, key = { it.id }) { FilaRegistro(it) }
            }
        }
    }
}

@Composable
fun FilaRegistro(registro: RegistroAcceso) {
    ListItem(
        headlineContent = { Text(registro.nombrePersona) },
        supportingContent = {
            Text(stringResource(if (registro.esDeInvitado) R.string.historial_detalle_invitado else R.string.historial_detalle, stringResource(registro.tipo.etiquetaRes()), registro.momento.comoFechaHora()))
        },
    )
}

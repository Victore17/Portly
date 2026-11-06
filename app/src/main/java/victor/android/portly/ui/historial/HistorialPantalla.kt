package victor.android.portly.ui.historial

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
import victor.android.portly.ui.comun.etiqueta

@Composable
fun HistorialPantalla(viewModel: HistorialViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Scaffold(topBar = { BarraSuperior("Historial") }) { padding ->
        when {
            estado.cargando -> Cargando(Modifier.padding(padding))
            estado.registros.isEmpty() -> EstadoVacio("Aún no hay entradas ni salidas", Modifier.padding(padding))
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
            Text("${registro.tipo.etiqueta()} · ${registro.momento.comoFechaHora()}" + if (registro.esDeInvitado) " · Invitado" else "")
        },
    )
}

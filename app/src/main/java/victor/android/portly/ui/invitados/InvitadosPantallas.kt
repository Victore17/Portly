package victor.android.portly.ui.invitados

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import victor.android.portly.ui.comun.BarraSuperior
import victor.android.portly.ui.comun.Cargando
import victor.android.portly.ui.comun.ColumnaCentrada
import victor.android.portly.ui.comun.EstadoVacio
import victor.android.portly.ui.comun.comoFechaHora

@Composable
fun InvitadosPantalla(
    alCrear: () -> Unit,
    alAbrirQr: (Long) -> Unit,
    viewModel: InvitadosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BarraSuperior("Invitados") },
        floatingActionButton = {
            FloatingActionButton(onClick = alCrear) { Icon(Icons.Default.Add, contentDescription = "Crear invitado") }
        },
    ) { padding ->
        when {
            estado.cargando -> Cargando(Modifier.padding(padding))
            estado.activos.isEmpty() ->
                EstadoVacio("No tienes QR de invitados vigentes hoy.\nToca + para crear uno", Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(estado.activos, key = { it.qr.id }) { activo ->
                    ListItem(
                        headlineContent = { Text(activo.invitado.nombre) },
                        supportingContent = { Text("Creado ${activo.qr.creadoEn.comoFechaHora()}") },
                        modifier = Modifier.clickable { alAbrirQr(activo.qr.id) },
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CrearInvitadoPantalla(
    alVolver: () -> Unit,
    alCrearQr: (Long) -> Unit,
    viewModel: CrearInvitadoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val guardados by viewModel.guardados.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.qrCreado.collect(alCrearQr) }

    Scaffold(topBar = { BarraSuperior("Crear invitado", alVolver) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = estado.nombre,
                onValueChange = viewModel::cambiarNombre,
                label = { Text("Nombre del invitado") },
                singleLine = true,
                isError = estado.error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            estado.error?.let {
                Text(
                    when (it) {
                        ErrorCrearInvitado.NOMBRE_VACIO -> "Escribe el nombre del invitado"
                        ErrorCrearInvitado.RESIDENTE_NO_DISPONIBLE -> "Tu cuenta no puede crear invitados"
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (guardados.isNotEmpty()) {
                Text("Invitados guardados", style = MaterialTheme.typography.titleSmall)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    guardados.forEach { nombre ->
                        AssistChip(onClick = { viewModel.cambiarNombre(nombre) }, label = { Text(nombre) })
                    }
                }
            }
            Button(onClick = viewModel::crear, enabled = !estado.creando, modifier = Modifier.fillMaxWidth()) {
                Text("Generar QR")
            }
        }
    }
}

@Composable
fun DetalleQrPantalla(alVolver: () -> Unit, viewModel: DetalleQrViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    LaunchedEffect(Unit) { viewModel.cancelado.collect { alVolver() } }

    Scaffold(topBar = { BarraSuperior("QR de invitado", alVolver) }) { padding ->
        when (val e = estado) {
            DetalleQrUiState.Cargando -> Cargando(Modifier.padding(padding))
            DetalleQrUiState.NoVigente ->
                EstadoVacio("Este QR ya no está vigente. Los QR de invitado valen solo el día en que se crean", Modifier.padding(padding))
            is DetalleQrUiState.Listo -> {
                val imagen = remember(e.contenido) { viewModel.imagen(e.contenido, 768).asImageBitmap() }
                ColumnaCentrada(Modifier.padding(padding)) {
                    Text(e.nombreInvitado, style = MaterialTheme.typography.titleLarge)
                    Image(imagen, contentDescription = "QR de ${e.nombreInvitado}", modifier = Modifier.size(260.dp))
                    Button(
                        onClick = {
                            val whatsapp = viewModel.intentCompartir(768, soloWhatsapp = true)
                            try {
                                contexto.startActivity(whatsapp)
                            } catch (_: ActivityNotFoundException) {
                                // Sin WhatsApp instalado, se ofrece cualquier otra app.
                                viewModel.intentCompartir(768, soloWhatsapp = false)?.let {
                                    contexto.startActivity(android.content.Intent.createChooser(it, "Compartir QR"))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Compartir por WhatsApp") }
                    OutlinedButton(onClick = viewModel::cancelar, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar QR")
                    }
                }
            }
        }
    }
}

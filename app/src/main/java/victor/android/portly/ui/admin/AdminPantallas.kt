package victor.android.portly.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyRow
import victor.android.portly.ui.comun.BarraSuperior
import victor.android.portly.ui.comun.Cargando
import victor.android.portly.ui.comun.EstadoVacio
import victor.android.portly.ui.comun.comoFechaHora
import victor.android.portly.ui.historial.FilaRegistro

@Composable
fun AdminInicioPantalla(alResidentes: () -> Unit, alRegistro: () -> Unit, alQrs: () -> Unit) {
    Scaffold(topBar = { BarraSuperior("Administración") }) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(headlineContent = { Text("Residentes") }, supportingContent = { Text("Agregar, editar, activar o desactivar") },
                modifier = Modifier.clickable(onClick = alResidentes))
            HorizontalDivider()
            ListItem(headlineContent = { Text("Registro completo") }, supportingContent = { Text("Entradas y salidas de toda la privada") },
                modifier = Modifier.clickable(onClick = alRegistro))
            HorizontalDivider()
            ListItem(headlineContent = { Text("QRs de invitados activos") }, supportingContent = { Text("Vigentes hoy, de todos los residentes") },
                modifier = Modifier.clickable(onClick = alQrs))
        }
    }
}

@Composable
private fun AvisoDeError(error: String?, alDescartar: () -> Unit, snackbar: SnackbarHostState) {
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            alDescartar()
        }
    }
}

@Composable
fun ResidentesPantalla(
    alVolver: () -> Unit,
    alAgregar: () -> Unit,
    alEditar: (Long) -> Unit,
    viewModel: ResidentesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    AvisoDeError(error, viewModel::descartarError, snackbar)
    Scaffold(
        topBar = { BarraSuperior("Residentes", alVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = alAgregar) { Icon(Icons.Default.Add, contentDescription = "Agregar residente") }
        },
    ) { padding ->
        when {
            estado.cargando -> Cargando(Modifier.padding(padding))
            estado.residentes.isEmpty() -> EstadoVacio("No hay residentes", Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(estado.residentes, key = { it.id }) { r ->
                    ListItem(
                        headlineContent = { Text(r.nombre + if (r.esAdmin) " (admin)" else "") },
                        supportingContent = { Text("${r.domicilio} · ${r.contacto}" + if (!r.activo) " · Inactivo" else "") },
                        trailingContent = { Switch(r.activo, onCheckedChange = { viewModel.cambiarActivo(r.id, it) }) },
                        modifier = Modifier.clickable { alEditar(r.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun FormularioResidentePantalla(
    alVolver: () -> Unit,
    viewModel: FormularioResidenteViewModel = hiltViewModel(),
) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.guardado.collect { alVolver() } }
    Scaffold(topBar = { BarraSuperior(if (e.esEdicion) "Editar residente" else "Agregar residente", alVolver) }) { padding ->
        if (e.cargando) {
            Cargando(Modifier.padding(padding))
        } else {
            Column(
                Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(e.nombre, viewModel::cambiarNombre, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(e.domicilio, viewModel::cambiarDomicilio, label = { Text("Domicilio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(e.contacto, viewModel::cambiarContacto, label = { Text("Teléfono o correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!e.esEdicion) {
                    OutlinedTextField(
                        e.contrasena, viewModel::cambiarContrasena, label = { Text("Contraseña inicial") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(e.esAdmin, onCheckedChange = viewModel::cambiarEsAdmin)
                    Text("Es administrador")
                }
                e.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = viewModel::guardar, enabled = !e.guardando, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
            }
        }
    }
}

@Composable
fun RegistroCompletoPantalla(alVolver: () -> Unit, viewModel: RegistroCompletoViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    Scaffold(topBar = { BarraSuperior("Registro completo", alVolver) }) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(e.filtroResidenteId == null, { viewModel.filtrarPorResidente(null) }, label = { Text("Todos") }) }
                items(e.residentes, key = { it.id }) { r ->
                    FilterChip(e.filtroResidenteId == r.id, { viewModel.filtrarPorResidente(r.id) }, label = { Text(r.nombre) })
                }
            }
            when {
                e.cargando -> Cargando()
                e.registros.isEmpty() -> EstadoVacio("No hay registros")
                else -> LazyColumn { items(e.registros, key = { it.id }) { FilaRegistro(it) } }
            }
        }
    }
}

@Composable
fun QrsActivosAdminPantalla(alVolver: () -> Unit, viewModel: QrsActivosAdminViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    AvisoDeError(error, viewModel::descartarError, snackbar)
    Scaffold(topBar = { BarraSuperior("QRs de invitados activos", alVolver) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        when {
            e.cargando -> Cargando(Modifier.padding(padding))
            e.qrs.isEmpty() -> EstadoVacio("No hay QR de invitados vigentes hoy", Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(e.qrs, key = { it.qr.id }) { a ->
                    ListItem(
                        headlineContent = { Text(a.invitado.nombre) },
                        supportingContent = { Text("Invitado de ${a.residente.nombre} · ${a.qr.creadoEn.comoFechaHora()}") },
                        trailingContent = { OutlinedButton(onClick = { viewModel.cancelar(a.qr.id) }) { Text("Cancelar") } },
                    )
                }
            }
        }
    }
}

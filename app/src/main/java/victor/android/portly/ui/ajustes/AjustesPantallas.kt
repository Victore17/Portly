package victor.android.portly.ui.ajustes

import victor.android.portly.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import victor.android.portly.ui.comun.BarraSuperior
import victor.android.portly.ui.comun.EstadoVacio

@Composable
fun AjustesPantalla(alAbrirNotificaciones: () -> Unit, viewModel: AjustesViewModel = hiltViewModel()) {
    Scaffold(topBar = { BarraSuperior(stringResource(R.string.ajustes_titulo)) }) { padding ->
        androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.ajustes_notificaciones)) },
                supportingContent = { Text(stringResource(R.string.ajustes_notificaciones_detalle)) },
                modifier = Modifier.clickable(onClick = alAbrirNotificaciones),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.ajustes_cerrar_sesion)) },
                modifier = Modifier.clickable(onClick = viewModel::cerrarSesion),
            )
        }
    }
}

@Composable
fun NotificacionesInvitadosPantalla(
    alVolver: () -> Unit,
    viewModel: NotificacionesInvitadosViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val textoError = stringResource(R.string.ajustes_error_guardar)
    LaunchedEffect(error) {
        if (error) {
            snackbar.showSnackbar(textoError)
            viewModel.descartarError()
        }
    }
    Scaffold(
        topBar = { BarraSuperior(stringResource(R.string.ajustes_notificaciones), alVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (ajustes.invitados.isEmpty()) {
            EstadoVacio(stringResource(R.string.ajustes_sin_invitados), Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding)) {
                if (ajustes.mostrarSilenciarTodos) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.ajustes_silenciar_todos)) },
                            trailingContent = {
                                Switch(ajustes.silenciarTodos, onCheckedChange = viewModel::cambiarSilenciarTodos)
                            },
                        )
                        HorizontalDivider()
                    }
                }
                items(ajustes.invitados, key = { it.invitado.id }) { a ->
                    ListItem(
                        headlineContent = { Text(a.invitado.nombre) },
                        supportingContent = {
                            Text(stringResource(if (ajustes.silenciarTodos && ajustes.mostrarSilenciarTodos) R.string.ajustes_silenciado_general else R.string.ajustes_silenciar))
                        },
                        trailingContent = {
                            Switch(
                                checked = a.silenciadoEfectivo,
                                // Con el general activo manda sobre los individuales.
                                enabled = !(ajustes.mostrarSilenciarTodos && ajustes.silenciarTodos),
                                onCheckedChange = { viewModel.cambiarInvitado(a.invitado.id, it) },
                            )
                        },
                    )
                }
            }
        }
    }
}

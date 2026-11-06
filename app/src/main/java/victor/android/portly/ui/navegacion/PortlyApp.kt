package victor.android.portly.ui.navegacion

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import victor.android.portly.ui.admin.AdminInicioPantalla
import victor.android.portly.ui.admin.FormularioResidentePantalla
import victor.android.portly.ui.admin.FormularioResidenteViewModel
import victor.android.portly.ui.admin.QrsActivosAdminPantalla
import victor.android.portly.ui.admin.RegistroCompletoPantalla
import victor.android.portly.ui.admin.ResidentesPantalla
import victor.android.portly.ui.ajustes.AjustesPantalla
import victor.android.portly.ui.ajustes.NotificacionesInvitadosPantalla
import victor.android.portly.ui.comun.Cargando
import victor.android.portly.ui.historial.HistorialPantalla
import victor.android.portly.ui.invitados.CrearInvitadoPantalla
import victor.android.portly.ui.invitados.DetalleQrPantalla
import victor.android.portly.ui.invitados.InvitadosPantalla
import victor.android.portly.ui.login.LoginPantalla
import victor.android.portly.ui.miqr.MiQrPantalla
import victor.android.portly.ui.sesion.EstadoSesion
import victor.android.portly.ui.sesion.SesionViewModel

@Composable
fun PortlyApp(viewModel: SesionViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoSesion.Cargando -> Cargando()
        EstadoSesion.SinSesion -> LoginPantalla()
        // Se recrea la navegación por residente, para no heredar la pila de otra sesión.
        is EstadoSesion.ConSesion -> androidx.compose.runtime.key(e.residenteId) {
            Inicio(mostrarAdministracion = e.mostrarAdministracion)
        }
    }
}

private fun iconoDe(ruta: String): ImageVector = when (ruta) {
    Rutas.MI_QR -> Icons.Default.Home
    Rutas.INVITADOS -> Icons.Default.Person
    Rutas.HISTORIAL -> Icons.Default.Info
    Rutas.AJUSTES -> Icons.Default.Settings
    else -> Icons.Default.Lock
}

/** Ruta de nivel superior (pestaña) a la que pertenece [ruta], para resaltarla en la barra. */
private fun pestanaDe(ruta: String?): String? = when {
    ruta == null -> null
    ruta == Rutas.MI_QR -> Rutas.MI_QR
    ruta.startsWith(Rutas.INVITADOS) -> Rutas.INVITADOS
    ruta == Rutas.HISTORIAL -> Rutas.HISTORIAL
    ruta.startsWith(Rutas.AJUSTES) -> Rutas.AJUSTES
    ruta.startsWith(Rutas.ADMIN) -> Rutas.ADMIN
    else -> null
}

private val RUTAS_CON_BARRA = setOf(Rutas.MI_QR, Rutas.INVITADOS, Rutas.HISTORIAL, Rutas.AJUSTES, Rutas.ADMIN)

@Composable
private fun Inicio(mostrarAdministracion: Boolean) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val pestanas = pestanasVisibles(mostrarAdministracion)

    Scaffold(
        bottomBar = {
            if (rutaActual in RUTAS_CON_BARRA) {
                NavigationBar {
                    pestanas.forEach { p ->
                        NavigationBarItem(
                            selected = pestanaDe(rutaActual) == p.ruta,
                            onClick = { nav.irAPestana(p.ruta) },
                            icon = { Icon(iconoDe(p.ruta), contentDescription = null) },
                            label = { Text(stringResource(p.etiquetaRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Rutas.MI_QR, modifier = Modifier.padding(padding)) {
            composable(Rutas.MI_QR) { MiQrPantalla() }
            composable(Rutas.INVITADOS) {
                InvitadosPantalla(
                    alCrear = { nav.navigate(Rutas.CREAR_INVITADO) },
                    alAbrirQr = { nav.navigate(Rutas.detalleQr(it)) },
                )
            }
            composable(Rutas.CREAR_INVITADO) {
                CrearInvitadoPantalla(
                    alVolver = { nav.popBackStack() },
                    // Se reemplaza la pantalla de creación por el detalle, para que "atrás" vuelva a la lista.
                    alCrearQr = { id ->
                        nav.navigate(Rutas.detalleQr(id)) { popUpTo(Rutas.CREAR_INVITADO) { inclusive = true } }
                    },
                )
            }
            composable(Rutas.DETALLE_QR, arguments = listOf(navArgument("qrId") { type = NavType.LongType })) {
                DetalleQrPantalla(alVolver = { nav.popBackStack() })
            }
            composable(Rutas.HISTORIAL) { HistorialPantalla() }
            composable(Rutas.AJUSTES) {
                AjustesPantalla(alAbrirNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES_INVITADOS) })
            }
            composable(Rutas.NOTIFICACIONES_INVITADOS) {
                NotificacionesInvitadosPantalla(alVolver = { nav.popBackStack() })
            }
            if (mostrarAdministracion) {
                composable(Rutas.ADMIN) {
                    AdminInicioPantalla(
                        alResidentes = { nav.navigate(Rutas.ADMIN_RESIDENTES) },
                        alRegistro = { nav.navigate(Rutas.ADMIN_REGISTRO) },
                        alQrs = { nav.navigate(Rutas.ADMIN_QRS) },
                    )
                }
                composable(Rutas.ADMIN_RESIDENTES) {
                    ResidentesPantalla(
                        alVolver = { nav.popBackStack() },
                        alAgregar = { nav.navigate(Rutas.formularioResidente(FormularioResidenteViewModel.SIN_RESIDENTE)) },
                        alEditar = { nav.navigate(Rutas.formularioResidente(it)) },
                    )
                }
                composable(
                    Rutas.ADMIN_FORMULARIO,
                    arguments = listOf(navArgument(FormularioResidenteViewModel.ARG_RESIDENTE_ID) { type = NavType.LongType }),
                ) { FormularioResidentePantalla(alVolver = { nav.popBackStack() }) }
                composable(Rutas.ADMIN_REGISTRO) { RegistroCompletoPantalla(alVolver = { nav.popBackStack() }) }
                composable(Rutas.ADMIN_QRS) { QrsActivosAdminPantalla(alVolver = { nav.popBackStack() }) }
            }
        }
    }
}

private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

package victor.android.portly.ui.comun

import victor.android.portly.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import victor.android.portly.domain.modelo.TipoAcceso
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val formatoFechaHora: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())

fun Instant.comoFechaHora(zona: ZoneId = ZoneId.systemDefault()): String =
    formatoFechaHora.format(atZone(zona))

@StringRes
fun TipoAcceso.etiquetaRes(): Int = when (this) {
    TipoAcceso.ENTRADA -> R.string.acceso_entrada
    TipoAcceso.SALIDA -> R.string.acceso_salida
}

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(40.dp))
    }
}

@Composable
fun EstadoVacio(mensaje: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun ColumnaCentrada(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) { contenido() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(titulo: String, alVolver: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                }
            }
        },
    )
}

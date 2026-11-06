package victor.android.portly.ui.login

import victor.android.portly.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import victor.android.portly.ui.comun.ColumnaCentrada

@Composable
fun LoginPantalla(viewModel: LoginViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ColumnaCentrada {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.login_subtitulo), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = estado.contacto,
            onValueChange = viewModel::cambiarContacto,
            label = { Text(stringResource(R.string.campo_contacto)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = estado.contrasena,
            onValueChange = viewModel::cambiarContrasena,
            label = { Text(stringResource(R.string.campo_contrasena)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        estado.error?.let {
            Text(
                when (it) {
                    ErrorLogin.CAMPOS_VACIOS -> stringResource(R.string.login_error_campos_vacios)
                    ErrorLogin.CREDENCIALES_INVALIDAS -> stringResource(R.string.login_error_credenciales)
                    ErrorLogin.RESIDENTE_INACTIVO -> stringResource(R.string.login_error_inactivo)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = viewModel::iniciarSesion, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
            if (estado.cargando) CircularProgressIndicator(Modifier.height(20.dp)) else Text(stringResource(R.string.login_entrar))
        }
    }
}

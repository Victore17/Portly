package victor.android.portly.ui.login

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
        Text("Portly", style = MaterialTheme.typography.displaySmall)
        Text("Acceso a tu privada", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = estado.contacto,
            onValueChange = viewModel::cambiarContacto,
            label = { Text("Teléfono o correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = estado.contrasena,
            onValueChange = viewModel::cambiarContrasena,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        estado.error?.let {
            Text(
                when (it) {
                    ErrorLogin.CAMPOS_VACIOS -> "Escribe tu contacto y contraseña"
                    ErrorLogin.CREDENCIALES_INVALIDAS -> "Contacto o contraseña incorrectos"
                    ErrorLogin.RESIDENTE_INACTIVO -> "Tu cuenta está desactivada. Habla con el administrador"
                },
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = viewModel::iniciarSesion, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
            if (estado.cargando) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Entrar")
        }
    }
}

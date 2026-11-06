package victor.android.portly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import victor.android.portly.ui.navegacion.PortlyApp
import victor.android.portly.ui.theme.PortlyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortlyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PortlyApp()
                }
            }
        }
    }
}

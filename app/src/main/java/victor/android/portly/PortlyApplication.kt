package victor.android.portly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la app. Hilt genera aquí el contenedor de dependencias
 * de toda la aplicación.
 */
@HiltAndroidApp
class PortlyApplication : Application()

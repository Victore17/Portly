package victor.android.portly

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import javax.inject.Inject

/**
 * Punto de entrada de la app. Hilt genera aquí el contenedor de dependencias
 * de toda la aplicación.
 */
@HiltAndroidApp
class PortlyApplication : Application() {

    @Inject lateinit var residentes: ResidenteRepositorio
    @Inject lateinit var autenticacion: ServicioAutenticacion

    override fun onCreate() {
        super.onCreate()
        // Solo en debug: la cuenta de prueba no existe en release.
        val esDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (esDebug) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                SembradorDebug.sembrar(residentes, autenticacion)
            }
        }
    }
}

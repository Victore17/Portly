package victor.android.portly.data.notificaciones

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AjustesNotificacionesDataStoreTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val job = Job()

    @After
    fun limpiar() = runBlocking { job.cancelAndJoin() }

    private fun ajustes() = AjustesNotificacionesDataStore(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { File(carpeta.root, "notif.preferences_pb") },
        ),
    )

    @Test
    fun por_defecto_esta_apagado_y_cada_residente_tiene_el_suyo() = runBlocking {
        val a = ajustes()
        assertFalse(a.observarSilenciarTodos(1).first())

        a.cambiarSilenciarTodos(1, true)

        assertTrue(a.observarSilenciarTodos(1).first())
        assertFalse(a.observarSilenciarTodos(2).first())
    }
}

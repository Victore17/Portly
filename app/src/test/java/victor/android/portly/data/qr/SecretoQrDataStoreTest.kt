package victor.android.portly.data.qr

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import victor.android.portly.data.seguridad.CifradorAesGcm
import java.io.File
import javax.crypto.KeyGenerator

class SecretoQrDataStoreTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val job = Job()
    private val clave = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @After
    fun limpiar() = runBlocking { job.cancelAndJoin() }

    private fun proveedor() = SecretoQrDataStore(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { File(carpeta.root, "qr.preferences_pb") },
        ),
        CifradorAesGcm { clave },
    )

    @Test
    fun genera_un_secreto_de_32_bytes_y_siempre_devuelve_el_mismo() = runBlocking {
        val p = proveedor()

        val primero = p.obtener()

        assertEquals(32, primero.size)
        assertArrayEquals(primero, p.obtener())
    }
}

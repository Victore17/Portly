package victor.android.portly.data.qr

import android.content.Intent
import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CompartidorQrTest {

    // lazy: el Application de Robolectric se crea después de instanciar el test,
    // y el FileProvider usa el de ese momento.
    private val contexto by lazy { RuntimeEnvironment.getApplication() }
    private val compartidor by lazy { CompartidorQr(contexto) }
    private val imagen by lazy { GeneradorQr().generar("portly", 200) }

    /**
     * FileProvider guarda en una caché estática las rutas del primer test, y
     * Robolectric usa una carpeta temporal distinta en cada uno; se limpia.
     */
    @Before
    fun limpiarCacheDelFileProvider() {
        val campo = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
        campo.isAccessible = true
        (campo.get(null) as MutableMap<*, *>).clear()
    }

    @Test
    fun el_intent_comparte_una_imagen_png_y_no_un_enlace() {
        val intent = compartidor.crearIntent(imagen, "Luis")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertEquals("${contexto.packageName}.fileprovider", uri.authority)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals("QR de acceso para Luis", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun la_imagen_compartida_existe_y_es_un_png_valido() {
        compartidor.crearIntent(imagen, "Luis")

        val archivos = File(contexto.cacheDir, "qr").listFiles()!!
        assertEquals(1, archivos.size)
        val decodificada = android.graphics.BitmapFactory.decodeFile(archivos[0].path)
        assertEquals(200, decodificada.width)
        assertEquals(Bitmap.Config.ARGB_8888, decodificada.config)
    }

    @Test
    fun compartir_de_nuevo_no_acumula_imagenes() {
        compartidor.crearIntent(imagen, "Luis")
        Thread.sleep(5)
        compartidor.crearIntent(imagen, "Rosa")

        assertEquals(1, File(contexto.cacheDir, "qr").listFiles()!!.size)
    }

    @Test
    fun puede_dirigirse_solo_a_whatsapp() {
        assertEquals(CompartidorQr.WHATSAPP, compartidor.crearIntent(imagen, "Luis", CompartidorQr.WHATSAPP).`package`)
        assertNull(compartidor.crearIntent(imagen, "Luis").`package`)
    }
}

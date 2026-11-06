package victor.android.portly.data.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Prepara la acción de compartir el QR como **imagen** (nunca como enlace).
 * Guarda el PNG en la caché de la app y lo expone con un FileProvider.
 * La pantalla solo tiene que lanzar el Intent con `startActivity`.
 */
class CompartidorQr @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    /**
     * @param paquete si se indica (por ejemplo [WHATSAPP]) el Intent se dirige
     *   solo a esa app; si es null, Android muestra el selector normal.
     */
    fun crearIntent(imagen: Bitmap, nombreInvitado: String, paquete: String? = null): Intent {
        val carpeta = File(contexto.cacheDir, CARPETA).apply { mkdirs() }
        // Un solo archivo por vez: se borran los anteriores para no acumular imágenes.
        carpeta.listFiles()?.forEach { it.delete() }
        val archivo = File(carpeta, "qr_${System.currentTimeMillis()}.png")
        archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "QR de acceso para $nombreInvitado")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            paquete?.let { setPackage(it) }
        }
    }

    companion object {
        const val WHATSAPP = "com.whatsapp"
        private const val CARPETA = "qr"
    }
}

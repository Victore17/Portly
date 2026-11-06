package victor.android.portly.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject

/**
 * Único lugar de la app que toca ZXing: convierte un texto en la imagen de su
 * QR. Si algún día se cambia de librería, solo cambia esta clase.
 */
class GeneradorQr @Inject constructor() {

    /** Devuelve un Bitmap cuadrado de [tamano] x [tamano] píxeles con el QR de [texto]. */
    fun generar(texto: String, tamano: Int): Bitmap {
        require(tamano > 0) { "El tamaño debe ser positivo" }
        val pistas = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to MARGEN_MODULOS,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano, pistas)
        val pixeles = IntArray(tamano * tamano)
        for (y in 0 until tamano) {
            for (x in 0 until tamano) {
                pixeles[y * tamano + x] = if (matriz[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixeles, tamano, tamano, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val MARGEN_MODULOS = 2
    }
}

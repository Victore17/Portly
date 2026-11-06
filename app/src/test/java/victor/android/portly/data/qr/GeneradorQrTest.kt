package victor.android.portly.data.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeneradorQrTest {

    private val generador = GeneradorQr()

    private fun decodificar(texto: String, tamano: Int): String {
        val bitmap = generador.generar(texto, tamano)
        val pixeles = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixeles, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val fuente = RGBLuminanceSource(bitmap.width, bitmap.height, pixeles)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(fuente))).text
    }

    @Test
    fun devuelve_una_imagen_del_tamano_pedido() {
        val bitmap = generador.generar("portly", 300)

        assertEquals(300, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun respeta_otros_tamanos() {
        val bitmap = generador.generar("portly", 512)

        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
    }

    @Test
    fun el_contenido_se_decodifica_de_vuelta_al_mismo_texto() {
        val texto = "portly:1:R:42:9f2c4a1b7d3e5f60"

        assertEquals(texto, decodificar(texto, 400))
    }

    @Test
    fun ida_y_vuelta_con_acentos() {
        assertEquals("Peña Núñez", decodificar("Peña Núñez", 400))
    }

    @Test(expected = IllegalArgumentException::class)
    fun un_tamano_invalido_se_rechaza() {
        generador.generar("portly", 0)
    }
}

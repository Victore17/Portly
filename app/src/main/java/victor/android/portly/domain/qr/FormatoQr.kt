package victor.android.portly.domain.qr

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class TipoQr(val codigo: String) {
    RESIDENTE("R"),
    INVITADO("I");

    companion object {
        fun deCodigo(codigo: String): TipoQr? = entries.firstOrNull { it.codigo == codigo }
    }
}

/**
 * Lo que dice un QR: de qué tipo es y a qué corresponde.
 *
 * @property identificador id del residente (QR de residente) o id del QR de
 *   invitado (QR de invitado).
 */
data class ContenidoQr(val tipo: TipoQr, val identificador: Long)

/**
 * Formato del texto dentro del QR:
 *
 * ```
 * portly:1:<tipo>:<identificador>:<firma>
 * portly:1:R:42:9f2c4a1b7d3e5f60
 * ```
 *
 * `firma` son los primeros 8 bytes (hex) de HMAC-SHA256 sobre `1:<tipo>:<id>`
 * con un [secreto]. Sirve para que la caseta, que tendrá el mismo secreto,
 * descarte QR fabricados a mano con solo adivinar un id. No protege contra una
 * foto del QR real: el QR del residente es fijo.
 */
class FormatoQr(private val secreto: ByteArray) {

    fun codificar(tipo: TipoQr, identificador: Long): String {
        val base = "$VERSION:${tipo.codigo}:$identificador"
        return "$PREFIJO:$base:${firmar(base)}"
    }

    /** Interpreta un texto leído de un QR; null si no es de Portly, está mal formado o la firma no coincide. */
    fun interpretar(texto: String): ContenidoQr? {
        val partes = texto.split(':')
        if (partes.size != 5 || partes[0] != PREFIJO || partes[1] != VERSION) return null
        val tipo = TipoQr.deCodigo(partes[2]) ?: return null
        val id = partes[3].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val esperada = firmar("$VERSION:${tipo.codigo}:$id")
        val coincide = MessageDigest.isEqual(esperada.toByteArray(), partes[4].toByteArray())
        return if (coincide) ContenidoQr(tipo, id) else null
    }

    private fun firmar(base: String): String {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secreto, "HmacSHA256")) }
        return mac.doFinal(base.toByteArray(Charsets.UTF_8))
            .take(BYTES_FIRMA)
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFIJO = "portly"
        const val VERSION = "1"
        const val BYTES_FIRMA = 8
    }
}

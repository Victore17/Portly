package victor.android.portly.data.seguridad

import victor.android.portly.domain.seguridad.HashContrasena
import victor.android.portly.domain.seguridad.HasheadorContrasena
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

/** PBKDF2 con HMAC-SHA256 y sal aleatoria por contraseña. */
class HasheadorPbkdf2 @Inject constructor() : HasheadorContrasena {

    private val aleatorio = SecureRandom()

    override fun hashear(contrasena: String): HashContrasena {
        val sal = ByteArray(TAM_SAL).also(aleatorio::nextBytes)
        return HashContrasena(
            hash = Base64.getEncoder().encodeToString(derivar(contrasena, sal)),
            sal = Base64.getEncoder().encodeToString(sal),
        )
    }

    override fun verificar(contrasena: String, hash: String, sal: String): Boolean {
        val esperado = runCatching { Base64.getDecoder().decode(hash) }.getOrNull() ?: return false
        val bytesSal = runCatching { Base64.getDecoder().decode(sal) }.getOrNull() ?: return false
        // isEqual compara en tiempo constante.
        return MessageDigest.isEqual(derivar(contrasena, bytesSal), esperado)
    }

    private fun derivar(contrasena: String, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(contrasena.toCharArray(), sal, ITERACIONES, BITS_HASH)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ITERACIONES = 120_000
        const val BITS_HASH = 256
        const val TAM_SAL = 16
    }
}

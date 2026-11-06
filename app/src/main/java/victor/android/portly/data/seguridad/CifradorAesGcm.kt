package victor.android.portly.data.seguridad

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM. El IV (12 bytes) va al inicio del resultado.
 *
 * La clave llega por [proveedorClave] para poder probar el cifrado en la JVM;
 * en la app viene del Android Keystore ([claveDelKeystore]).
 */
class CifradorAesGcm(
    private val proveedorClave: () -> SecretKey,
) : Cifrador {

    override fun cifrar(datos: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, proveedorClave())
        return cipher.iv + cipher.doFinal(datos)
    }

    override fun descifrar(datos: ByteArray): ByteArray {
        require(datos.size > TAM_IV) { "Datos cifrados demasiado cortos" }
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            proveedorClave(),
            GCMParameterSpec(BITS_TAG, datos, 0, TAM_IV),
        )
        return cipher.doFinal(datos, TAM_IV, datos.size - TAM_IV)
    }

    companion object {
        private const val TRANSFORMACION = "AES/GCM/NoPadding"
        private const val TAM_IV = 12
        private const val BITS_TAG = 128
        private const val PROVEEDOR = "AndroidKeyStore"
        private const val ALIAS = "portly_sesion"

        /** Obtiene la clave del Keystore; la crea la primera vez. La clave nunca sale del Keystore. */
        fun claveDelKeystore(): SecretKey {
            val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
            (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }

            val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
            generador.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return generador.generateKey()
        }
    }
}

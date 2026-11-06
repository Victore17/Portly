package victor.android.portly.data.seguridad

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class CifradorAesGcmTest {

    private fun clave(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun descifrar_devuelve_lo_que_se_cifro() {
        val k = clave()
        val cifrador = CifradorAesGcm { k }
        val original = "42:true".toByteArray()

        val cifrado = cifrador.cifrar(original)

        assertFalse(cifrado.contentEquals(original))
        assertArrayEquals(original, cifrador.descifrar(cifrado))
    }

    @Test
    fun cada_cifrado_usa_un_iv_distinto() {
        val k = clave()
        val cifrador = CifradorAesGcm { k }
        val datos = "42:true".toByteArray()

        assertNotEquals(
            cifrador.cifrar(datos).toList(),
            cifrador.cifrar(datos).toList(),
        )
    }

    @Test(expected = Exception::class)
    fun datos_alterados_no_se_pueden_descifrar() {
        val k = clave()
        val cifrador = CifradorAesGcm { k }
        val cifrado = cifrador.cifrar("42:true".toByteArray())
        cifrado[cifrado.size - 1] = (cifrado.last().toInt() xor 1).toByte()

        cifrador.descifrar(cifrado)
    }

    @Test(expected = Exception::class)
    fun con_otra_clave_no_se_puede_descifrar() {
        val cifrado = CifradorAesGcm { clave() }.cifrar("42:true".toByteArray())

        CifradorAesGcm { clave() }.descifrar(cifrado)
    }
}

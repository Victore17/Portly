package victor.android.portly.data.seguridad

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HasheadorPbkdf2Test {

    private val hasheador = HasheadorPbkdf2()

    @Test
    fun el_hash_no_contiene_la_contrasena_en_texto_plano() {
        val resultado = hasheador.hashear("secreta123")

        assertFalse(resultado.hash.contains("secreta123"))
        assertFalse(resultado.sal.contains("secreta123"))
    }

    @Test
    fun verifica_la_contrasena_correcta_y_rechaza_la_incorrecta() {
        val r = hasheador.hashear("secreta123")

        assertTrue(hasheador.verificar("secreta123", r.hash, r.sal))
        assertFalse(hasheador.verificar("otra", r.hash, r.sal))
        assertFalse(hasheador.verificar("", r.hash, r.sal))
    }

    @Test
    fun la_misma_contrasena_genera_hashes_distintos_por_la_sal() {
        val a = hasheador.hashear("secreta123")
        val b = hasheador.hashear("secreta123")

        assertNotEquals(a.sal, b.sal)
        assertNotEquals(a.hash, b.hash)
    }

    @Test
    fun un_hash_o_sal_mal_formados_no_lanzan_y_se_rechazan() {
        assertFalse(hasheador.verificar("x", "no es base64 !!", "tampoco"))
    }
}

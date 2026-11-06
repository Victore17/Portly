package victor.android.portly.domain.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatoQrTest {

    private val formato = FormatoQr("secreto-de-prueba".toByteArray())

    @Test
    fun codificar_arma_el_formato_esperado() {
        val texto = formato.codificar(TipoQr.RESIDENTE, 42)

        val partes = texto.split(':')
        assertEquals(listOf("portly", "1", "R", "42"), partes.take(4))
        assertEquals(16, partes[4].length)
    }

    @Test
    fun interpretar_recupera_tipo_e_identificador() {
        assertEquals(
            ContenidoQr(TipoQr.RESIDENTE, 42),
            formato.interpretar(formato.codificar(TipoQr.RESIDENTE, 42)),
        )
        assertEquals(
            ContenidoQr(TipoQr.INVITADO, 7),
            formato.interpretar(formato.codificar(TipoQr.INVITADO, 7)),
        )
    }

    @Test
    fun el_mismo_contenido_siempre_da_el_mismo_texto() {
        assertEquals(
            formato.codificar(TipoQr.RESIDENTE, 5),
            formato.codificar(TipoQr.RESIDENTE, 5),
        )
    }

    @Test
    fun cambiar_el_identificador_o_el_tipo_invalida_la_firma() {
        val texto = formato.codificar(TipoQr.RESIDENTE, 42)

        assertNull(formato.interpretar(texto.replace(":42:", ":43:")))
        assertNull(formato.interpretar(texto.replace(":R:", ":I:")))
    }

    @Test
    fun un_qr_firmado_con_otro_secreto_no_es_valido() {
        val ajeno = FormatoQr("otro-secreto".toByteArray()).codificar(TipoQr.RESIDENTE, 42)

        assertNull(formato.interpretar(ajeno))
    }

    @Test
    fun la_firma_depende_del_secreto() {
        val a = FormatoQr("a".toByteArray()).codificar(TipoQr.RESIDENTE, 1)
        val b = FormatoQr("b".toByteArray()).codificar(TipoQr.RESIDENTE, 1)

        assertNotEquals(a, b)
    }

    @Test
    fun textos_mal_formados_se_rechazan_sin_lanzar() {
        val valido = formato.codificar(TipoQr.RESIDENTE, 42)
        val malos = listOf(
            "",
            "hola",
            "https://ejemplo.com",
            "otra:1:R:42:" + valido.substringAfterLast(':'),
            valido.replace("portly:1", "portly:2"),
            valido.replace(":R:", ":X:"),
            "portly:1:R:abc:" + valido.substringAfterLast(':'),
            "portly:1:R:-3:" + valido.substringAfterLast(':'),
            "portly:1:R:42",
            "$valido:extra",
        )
        malos.forEach { assertNull("debió rechazar \"$it\"", formato.interpretar(it)) }
    }
}

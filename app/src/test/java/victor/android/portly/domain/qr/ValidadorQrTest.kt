package victor.android.portly.domain.qr

import org.junit.Assert.assertEquals
import org.junit.Test
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.Residente
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ValidadorQrTest {

    private val zona = ZoneId.of("America/Mexico_City") // UTC-6, sin horario de verano en 2026
    private val activo = Residente(id = 1, nombre = "Ana", domicilio = "C1", contacto = "1")

    private fun validador(ahora: String) =
        ValidadorQr(Clock.fixed(Instant.parse(ahora), zona))

    private fun qr(creado: String, cancelado: Boolean = false) = QrInvitado(
        id = 1, invitadoId = 1, residenteId = 1,
        creadoEn = Instant.parse(creado), cancelado = cancelado,
    )

    @Test
    fun es_valido_el_mismo_dia() {
        val v = validador("2026-11-02T20:00:00Z") // 14:00 local
        assertEquals(ResultadoValidacion.VALIDO, v.validar(qr("2026-11-02T15:00:00Z"), activo)) // 09:00 local
    }

    @Test
    fun deja_de_ser_valido_al_dia_siguiente() {
        val qr = qr("2026-11-02T15:00:00Z")
        val v = validador("2026-11-02T20:00:00Z")

        assertEquals(ResultadoValidacion.VALIDO, v.validar(qr, activo))
        // 23:59:59 local del mismo día sigue valiendo...
        assertEquals(ResultadoValidacion.VALIDO, v.validar(qr, activo, Instant.parse("2026-11-03T05:59:59Z")))
        // ...y a las 00:00 local del día siguiente ya no.
        assertEquals(ResultadoValidacion.VENCIDO, v.validar(qr, activo, Instant.parse("2026-11-03T06:00:00Z")))
    }

    @Test
    fun el_cambio_de_dia_usa_la_hora_local_no_utc() {
        // 22:00 local del día 2 ya es el día 3 en UTC, pero sigue siendo el mismo día local.
        val v = validador("2026-11-03T04:00:00Z")
        assertEquals(ResultadoValidacion.VALIDO, v.validar(qr("2026-11-02T16:00:00Z"), activo))
    }

    @Test
    fun un_qr_de_ayer_esta_vencido_con_el_reloj_inyectado() {
        val v = validador("2026-11-03T16:00:00Z")
        assertEquals(ResultadoValidacion.VENCIDO, v.validar(qr("2026-11-02T16:00:00Z"), activo))
    }

    @Test
    fun un_qr_cancelado_no_es_valido() {
        val v = validador("2026-11-02T20:00:00Z")
        assertEquals(
            ResultadoValidacion.CANCELADO,
            v.validar(qr("2026-11-02T15:00:00Z", cancelado = true), activo),
        )
    }

    @Test
    fun varios_usos_el_mismo_dia_son_validos() {
        val qr = qr("2026-11-02T14:00:00Z")
        val v = validador("2026-11-02T14:00:00Z")
        listOf("2026-11-02T14:05:00Z", "2026-11-02T18:00:00Z", "2026-11-02T23:00:00Z", "2026-11-03T01:30:00Z")
            .forEach { assertEquals(ResultadoValidacion.VALIDO, v.validar(qr, activo, Instant.parse(it))) }
    }

    @Test
    fun un_qr_de_un_residente_desactivado_no_es_valido() {
        val v = validador("2026-11-02T20:00:00Z")
        val qr = qr("2026-11-02T15:00:00Z")

        assertEquals(ResultadoValidacion.RESIDENTE_INACTIVO, v.validar(qr, activo.copy(activo = false)))
        assertEquals(ResultadoValidacion.RESIDENTE_INACTIVO, v.validar(qr, null))
    }

    @Test
    fun funciona_con_zona_utc() {
        val v = ValidadorQr(Clock.fixed(Instant.parse("2026-11-02T23:59:59Z"), ZoneOffset.UTC))
        assertEquals(ResultadoValidacion.VALIDO, v.validar(qr("2026-11-02T00:00:00Z"), activo))
        assertEquals(
            ResultadoValidacion.VENCIDO,
            v.validar(qr("2026-11-02T00:00:00Z"), activo, Instant.parse("2026-11-03T00:00:00Z")),
        )
    }
}

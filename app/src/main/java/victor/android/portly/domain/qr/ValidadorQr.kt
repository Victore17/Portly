package victor.android.portly.domain.qr

import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.Residente
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

enum class ResultadoValidacion {
    VALIDO,
    CANCELADO,

    /** Ya no es el día en que se creó. */
    VENCIDO,
    RESIDENTE_INACTIVO,
}

/**
 * Lógica pura que dice si un QR de invitado es válido en un momento dado.
 *
 * Un QR vale solo el día de creación (hasta las 23:59:59 de la zona horaria del
 * [reloj]) y, ese día, sin límite de usos. La conexión con la computadora de la
 * caseta queda para cuando exista servidor.
 */
class ValidadorQr @Inject constructor(private val reloj: Clock) {

    /** Valida con la hora actual del reloj. */
    fun validar(qr: QrInvitado, residente: Residente?): ResultadoValidacion =
        validar(qr, residente, reloj.instant())

    fun validar(qr: QrInvitado, residente: Residente?, ahora: Instant): ResultadoValidacion = when {
        residente == null || !residente.activo -> ResultadoValidacion.RESIDENTE_INACTIVO
        qr.cancelado -> ResultadoValidacion.CANCELADO
        !esDelDiaDe(qr, ahora, reloj.zone) -> ResultadoValidacion.VENCIDO
        else -> ResultadoValidacion.VALIDO
    }

    /** Si el QR sigue vigente por fecha en [ahora] (sin mirar cancelación ni residente). */
    fun vigentePorFecha(qr: QrInvitado, ahora: Instant = reloj.instant()): Boolean =
        esDelDiaDe(qr, ahora, reloj.zone)

    private fun esDelDiaDe(qr: QrInvitado, ahora: Instant, zona: ZoneId): Boolean =
        qr.creadoEn.atZone(zona).toLocalDate() == ahora.atZone(zona).toLocalDate()
}

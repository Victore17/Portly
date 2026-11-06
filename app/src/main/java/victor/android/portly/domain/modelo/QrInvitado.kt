package victor.android.portly.domain.modelo

import java.time.Instant

/**
 * Un QR que un residente generó para un invitado.
 *
 * Solo guarda los datos: las reglas de vigencia (vale únicamente el día en que
 * se creó) se implementan en una etapa posterior.
 *
 * @property creadoEn momento exacto de creación, de donde sale el día de vigencia.
 * @property cancelado el residente puede cancelarlo antes de que termine el día.
 */
data class QrInvitado(
    val id: Long = SIN_ID,
    val invitadoId: Long,
    val residenteId: Long,
    val creadoEn: Instant,
    val cancelado: Boolean = false,
) {
    companion object {
        const val SIN_ID: Long = 0L
    }
}

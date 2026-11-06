package victor.android.portly.domain.modelo

import java.time.Instant

/**
 * Una entrada o salida que quedó registrada.
 *
 * Los registros nunca se borran ni se editan: son el historial de la privada.
 *
 * @property nombrePersona nombre de quien pasó, copiado al momento del acceso.
 *   Se guarda la copia a propósito, para que el historial siga siendo legible
 *   aunque después se le cambie el nombre al residente o al invitado.
 * @property residenteId residente al que corresponde el acceso; si fue un
 *   invitado, es el residente que lo invitó.
 * @property invitadoId null cuando el acceso fue del residente mismo.
 */
data class RegistroAcceso(
    val id: Long = SIN_ID,
    val nombrePersona: String,
    val tipo: TipoAcceso,
    val momento: Instant,
    val residenteId: Long,
    val invitadoId: Long? = null,
) {
    /** true si el acceso fue de un invitado y no del residente. */
    val esDeInvitado: Boolean get() = invitadoId != null

    companion object {
        const val SIN_ID: Long = 0L
    }
}

package victor.android.portly.domain.modelo

/**
 * Un invitado guardado por un residente para poder reutilizarlo.
 *
 * El invitado no tiene cuenta en la app: solo recibe la imagen del QR.
 *
 * @property notificacionesSilenciadas interruptor individual del invitado. El
 *   interruptor general "Silenciar a todos los invitados" se resuelve en una
 *   etapa posterior y no se guarda aquí.
 */
data class Invitado(
    val id: Long = SIN_ID,
    val nombre: String,
    val residenteId: Long,
    val notificacionesSilenciadas: Boolean = false,
) {
    companion object {
        const val SIN_ID: Long = 0L
    }
}

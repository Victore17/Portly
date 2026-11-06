package victor.android.portly.domain.modelo

/**
 * Un residente de la privada.
 *
 * Un residente nunca se borra: se activa o se desactiva con [activo], para que
 * su historial de accesos se conserve.
 *
 * @property contacto teléfono o correo del residente.
 * @property esAdmin un residente puede ser además administrador; es la misma
 *   cuenta con un permiso extra.
 */
data class Residente(
    val id: Long = SIN_ID,
    val nombre: String,
    val domicilio: String,
    val contacto: String,
    val activo: Boolean = true,
    val esAdmin: Boolean = false,
) {
    companion object {
        /** Id que todavía no asignó la base de datos. */
        const val SIN_ID: Long = 0L
    }
}

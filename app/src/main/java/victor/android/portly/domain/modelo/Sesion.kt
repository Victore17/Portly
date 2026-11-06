package victor.android.portly.domain.modelo

/**
 * Sesión iniciada. Guarda si el residente es administrador para que la app
 * sepa qué mostrarle sin consultar la base cada vez.
 */
data class Sesion(
    val residenteId: Long,
    val esAdmin: Boolean,
)

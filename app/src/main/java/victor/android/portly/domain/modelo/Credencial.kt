package victor.android.portly.domain.modelo

/** Hash de la contraseña de un residente, con su sal. Ambos en Base64. */
data class Credencial(
    val residenteId: Long,
    val hash: String,
    val sal: String,
)

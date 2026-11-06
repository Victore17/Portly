package victor.android.portly.domain.qr

/**
 * Secreto con el que se firman los QR. Cuando exista la computadora de la
 * caseta tendrá que recibir este mismo secreto para validarlos.
 */
interface ProveedorSecretoQr {
    /** El secreto; se genera la primera vez que se pide y después es siempre el mismo. */
    suspend fun obtener(): ByteArray
}

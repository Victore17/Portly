package victor.android.portly.domain.qr

import victor.android.portly.domain.repositorio.ResidenteRepositorio
import javax.inject.Inject

/** Arma el texto del QR personal de un residente. */
class ServicioQrResidente @Inject constructor(
    private val residentes: ResidenteRepositorio,
    private val secreto: ProveedorSecretoQr,
) {

    /** Texto para el QR del residente, o null si no existe o está desactivado. */
    suspend fun contenidoDe(residenteId: Long): String? {
        val residente = residentes.obtener(residenteId)?.takeIf { it.activo } ?: return null
        return FormatoQr(secreto.obtener()).codificar(TipoQr.RESIDENTE, residente.id)
    }
}

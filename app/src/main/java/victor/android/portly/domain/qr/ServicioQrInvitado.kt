package victor.android.portly.domain.qr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import victor.android.portly.domain.repositorio.QrInvitadoRepositorio
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import java.time.Clock
import javax.inject.Inject

/** Un QR de invitado ya creado, junto con su invitado y el texto que lleva el QR. */
data class QrGenerado(
    val qr: QrInvitado,
    val invitado: Invitado,
    val contenido: String,
)

/** Un QR activo hoy y el invitado al que pertenece. */
data class QrActivo(val qr: QrInvitado, val invitado: Invitado)

sealed interface ResultadoCrearQr {
    data class Exito(val generado: QrGenerado, val invitadoNuevo: Boolean) : ResultadoCrearQr
    data object NombreVacio : ResultadoCrearQr
    data object ResidenteNoDisponible : ResultadoCrearQr
}

/** Flujo de invitados: crear su QR, listar los de hoy y cancelarlos. */
class ServicioQrInvitado @Inject constructor(
    private val residentes: ResidenteRepositorio,
    private val invitados: InvitadoRepositorio,
    private val qrs: QrInvitadoRepositorio,
    private val secreto: ProveedorSecretoQr,
    private val validador: ValidadorQr,
    private val reloj: Clock,
) {

    /**
     * Crea un QR para [nombre]. Si el residente ya tiene un invitado con ese
     * nombre lo reutiliza; si no, lo guarda como nuevo.
     */
    suspend fun crear(residenteId: Long, nombre: String): ResultadoCrearQr {
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        if (limpio.isEmpty()) return ResultadoCrearQr.NombreVacio
        val residente = residentes.obtener(residenteId)?.takeIf { it.activo }
            ?: return ResultadoCrearQr.ResidenteNoDisponible

        val existente = invitados.buscarPorNombre(residente.id, limpio)
        val invitado = existente
            ?: Invitado(nombre = limpio, residenteId = residente.id)
                .let { it.copy(id = invitados.agregar(it)) }

        val nuevo = QrInvitado(
            invitadoId = invitado.id,
            residenteId = residente.id,
            creadoEn = reloj.instant(),
        )
        val qr = nuevo.copy(id = qrs.agregar(nuevo))
        val contenido = FormatoQr(secreto.obtener()).codificar(TipoQr.INVITADO, qr.id)
        return ResultadoCrearQr.Exito(QrGenerado(qr, invitado, contenido), existente == null)
    }

    /**
     * QR de hoy que siguen activos (no cancelados y del día actual) de un
     * residente, del más reciente al más antiguo. El filtro de fecha se aplica
     * cada vez que cambian los datos.
     */
    fun observarActivosHoy(residenteId: Long): Flow<List<QrActivo>> =
        combine(qrs.observarPorResidente(residenteId), invitados.observarPorResidente(residenteId)) { listaQr, listaInv ->
            val porId = listaInv.associateBy { it.id }
            listaQr
                .filter { !it.cancelado && validador.vigentePorFecha(it) }
                .mapNotNull { qr -> porId[qr.invitadoId]?.let { QrActivo(qr, it) } }
        }

    /** Cancela el QR. Un QR cancelado deja de ser válido; el registro se conserva. */
    suspend fun cancelar(qrId: Long) = qrs.cancelar(qrId)

    /** Texto del QR de un QR ya creado (para volver a mostrarlo o compartirlo). */
    suspend fun contenidoDe(qr: QrInvitado): String =
        FormatoQr(secreto.obtener()).codificar(TipoQr.INVITADO, qr.id)
}

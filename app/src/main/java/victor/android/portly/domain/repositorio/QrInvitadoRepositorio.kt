package victor.android.portly.domain.repositorio

import kotlinx.coroutines.flow.Flow
import victor.android.portly.domain.modelo.QrInvitado

/** Acceso a los QR que los residentes generan para sus invitados. */
interface QrInvitadoRepositorio {

    /** QR de un residente, del más reciente al más antiguo. */
    fun observarPorResidente(residenteId: Long): Flow<List<QrInvitado>>

    /** QR de todos los residentes, del más reciente al más antiguo. */
    fun observarTodos(): Flow<List<QrInvitado>>

    /** QR de un invitado en particular, del más reciente al más antiguo. */
    fun observarPorInvitado(invitadoId: Long): Flow<List<QrInvitado>>

    suspend fun obtener(id: Long): QrInvitado?

    suspend fun agregar(qr: QrInvitado): Long

    /** Marca el QR como cancelado. El registro se conserva. */
    suspend fun cancelar(id: Long)
}

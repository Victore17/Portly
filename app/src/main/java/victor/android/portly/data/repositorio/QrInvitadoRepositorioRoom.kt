package victor.android.portly.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import victor.android.portly.data.local.aDominio
import victor.android.portly.data.local.aEntidad
import victor.android.portly.data.local.dao.QrInvitadoDao
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.repositorio.QrInvitadoRepositorio
import javax.inject.Inject

class QrInvitadoRepositorioRoom @Inject constructor(
    private val dao: QrInvitadoDao,
) : QrInvitadoRepositorio {

    override fun observarPorResidente(residenteId: Long): Flow<List<QrInvitado>> =
        dao.observarPorResidente(residenteId).map { lista -> lista.map { it.aDominio() } }

    override fun observarPorInvitado(invitadoId: Long): Flow<List<QrInvitado>> =
        dao.observarPorInvitado(invitadoId).map { lista -> lista.map { it.aDominio() } }

    override suspend fun obtener(id: Long): QrInvitado? = dao.obtenerPorId(id)?.aDominio()

    override suspend fun agregar(qr: QrInvitado): Long = dao.insertar(qr.aEntidad())

    override suspend fun cancelar(id: Long) = dao.marcarCancelado(id)
}

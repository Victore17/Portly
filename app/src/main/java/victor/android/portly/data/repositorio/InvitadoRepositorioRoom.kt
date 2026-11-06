package victor.android.portly.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import victor.android.portly.data.local.aDominio
import victor.android.portly.data.local.aEntidad
import victor.android.portly.data.local.dao.InvitadoDao
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import javax.inject.Inject

class InvitadoRepositorioRoom @Inject constructor(
    private val dao: InvitadoDao,
) : InvitadoRepositorio {

    override fun observarPorResidente(residenteId: Long): Flow<List<Invitado>> =
        dao.observarPorResidente(residenteId).map { lista -> lista.map { it.aDominio() } }

    override suspend fun obtener(id: Long): Invitado? = dao.obtenerPorId(id)?.aDominio()

    override suspend fun buscarPorNombre(residenteId: Long, nombre: String): Invitado? =
        dao.obtenerPorNombre(residenteId, nombre)?.aDominio()

    override suspend fun agregar(invitado: Invitado): Long = dao.insertar(invitado.aEntidad())

    override suspend fun actualizar(invitado: Invitado) = dao.actualizar(invitado.aEntidad())

    override suspend fun cambiarSilencio(id: Long, silenciadas: Boolean) =
        dao.cambiarSilencio(id, silenciadas)
}

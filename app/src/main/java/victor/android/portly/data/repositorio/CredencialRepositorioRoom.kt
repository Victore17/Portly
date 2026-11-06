package victor.android.portly.data.repositorio

import victor.android.portly.data.local.dao.CredencialDao
import victor.android.portly.data.local.entidad.CredencialEntity
import victor.android.portly.domain.modelo.Credencial
import victor.android.portly.domain.repositorio.CredencialRepositorio
import javax.inject.Inject

class CredencialRepositorioRoom @Inject constructor(
    private val dao: CredencialDao,
) : CredencialRepositorio {

    override suspend fun guardar(credencial: Credencial) =
        dao.guardar(CredencialEntity(credencial.residenteId, credencial.hash, credencial.sal))

    override suspend fun obtener(residenteId: Long): Credencial? =
        dao.obtener(residenteId)?.let { Credencial(it.residenteId, it.hash, it.sal) }
}

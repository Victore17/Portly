package victor.android.portly.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import victor.android.portly.data.local.aDominio
import victor.android.portly.data.local.aEntidad
import victor.android.portly.data.local.dao.ResidenteDao
import victor.android.portly.domain.modelo.Residente
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import javax.inject.Inject

class ResidenteRepositorioRoom @Inject constructor(
    private val dao: ResidenteDao,
) : ResidenteRepositorio {

    override fun observarTodos(): Flow<List<Residente>> =
        dao.observarTodos().map { lista -> lista.map { it.aDominio() } }

    override fun observarActivos(): Flow<List<Residente>> =
        dao.observarActivos().map { lista -> lista.map { it.aDominio() } }

    override fun observar(id: Long): Flow<Residente?> =
        dao.observarPorId(id).map { it?.aDominio() }

    override suspend fun obtener(id: Long): Residente? = dao.obtenerPorId(id)?.aDominio()

    override suspend fun obtenerPorContacto(contacto: String): Residente? =
        dao.obtenerPorContacto(contacto)?.aDominio()

    override suspend fun agregar(residente: Residente): Long = dao.insertar(residente.aEntidad())

    override suspend fun actualizar(residente: Residente) = dao.actualizar(residente.aEntidad())

    override suspend fun cambiarActivo(id: Long, activo: Boolean) = dao.cambiarActivo(id, activo)
}

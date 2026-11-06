package victor.android.portly.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import victor.android.portly.data.local.aDominio
import victor.android.portly.data.local.aEntidad
import victor.android.portly.data.local.dao.RegistroAccesoDao
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.repositorio.RegistroAccesoRepositorio
import java.time.Instant
import javax.inject.Inject

class RegistroAccesoRepositorioRoom @Inject constructor(
    private val dao: RegistroAccesoDao,
) : RegistroAccesoRepositorio {

    override fun observarPorResidente(residenteId: Long): Flow<List<RegistroAcceso>> =
        dao.observarPorResidente(residenteId).map { lista -> lista.map { it.aDominio() } }

    override fun observarTodos(): Flow<List<RegistroAcceso>> =
        dao.observarTodos().map { lista -> lista.map { it.aDominio() } }

    override fun observarPorResidenteEnRango(
        residenteId: Long,
        desde: Instant,
        hasta: Instant,
    ): Flow<List<RegistroAcceso>> =
        dao.observarPorResidenteEnRango(residenteId, desde, hasta)
            .map { lista -> lista.map { it.aDominio() } }

    override suspend fun registrar(registro: RegistroAcceso): Long =
        dao.insertar(registro.aEntidad())
}

package victor.android.portly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import victor.android.portly.data.local.entidad.RegistroAccesoEntity
import java.time.Instant

/**
 * Solo inserta y consulta: el historial no se edita ni se borra, por eso no hay
 * `@Update` ni `@Delete`.
 */
@Dao
interface RegistroAccesoDao {

    @Insert
    suspend fun insertar(registro: RegistroAccesoEntity): Long

    @Query("SELECT * FROM registros_acceso WHERE id = :id")
    suspend fun obtenerPorId(id: Long): RegistroAccesoEntity?

    /** Del más reciente al más antiguo. */
    @Query("SELECT * FROM registros_acceso WHERE residenteId = :residenteId ORDER BY momento DESC, id DESC")
    fun observarPorResidente(residenteId: Long): Flow<List<RegistroAccesoEntity>>

    /** Todos los registros de la privada, del más reciente al más antiguo. */
    @Query("SELECT * FROM registros_acceso ORDER BY momento DESC, id DESC")
    fun observarTodos(): Flow<List<RegistroAccesoEntity>>

    /** [desde] incluido, [hasta] excluido. */
    @Query(
        """
        SELECT * FROM registros_acceso
        WHERE residenteId = :residenteId AND momento >= :desde AND momento < :hasta
        ORDER BY momento DESC, id DESC
        """
    )
    fun observarPorResidenteEnRango(
        residenteId: Long,
        desde: Instant,
        hasta: Instant,
    ): Flow<List<RegistroAccesoEntity>>
}

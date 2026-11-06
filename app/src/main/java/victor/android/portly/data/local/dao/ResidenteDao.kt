package victor.android.portly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import victor.android.portly.data.local.entidad.ResidenteEntity

/**
 * A propósito no hay `@Delete` ni ningún `DELETE FROM residentes`: la regla del
 * proyecto es que un residente nunca se borra, solo se desactiva con
 * [cambiarActivo], para no perder su historial.
 */
@Dao
interface ResidenteDao {

    @Insert
    suspend fun insertar(residente: ResidenteEntity): Long

    @Update
    suspend fun actualizar(residente: ResidenteEntity)

    @Query("SELECT * FROM residentes WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ResidenteEntity?

    @Query("SELECT * FROM residentes WHERE id = :id")
    fun observarPorId(id: Long): Flow<ResidenteEntity?>

    @Query("SELECT * FROM residentes ORDER BY nombre COLLATE NOCASE ASC, id ASC")
    fun observarTodos(): Flow<List<ResidenteEntity>>

    @Query("SELECT * FROM residentes WHERE activo = 1 ORDER BY nombre COLLATE NOCASE ASC, id ASC")
    fun observarActivos(): Flow<List<ResidenteEntity>>

    @Query("UPDATE residentes SET activo = :activo WHERE id = :id")
    suspend fun cambiarActivo(id: Long, activo: Boolean)
}

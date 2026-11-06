package victor.android.portly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import victor.android.portly.data.local.entidad.InvitadoEntity

@Dao
interface InvitadoDao {

    @Insert
    suspend fun insertar(invitado: InvitadoEntity): Long

    @Update
    suspend fun actualizar(invitado: InvitadoEntity)

    @Query("SELECT * FROM invitados WHERE id = :id")
    suspend fun obtenerPorId(id: Long): InvitadoEntity?

    /** Para reutilizar un invitado ya guardado en vez de duplicarlo. */
    @Query("SELECT * FROM invitados WHERE residenteId = :residenteId AND nombre = :nombre LIMIT 1")
    suspend fun obtenerPorNombre(residenteId: Long, nombre: String): InvitadoEntity?

    @Query("SELECT * FROM invitados WHERE residenteId = :residenteId ORDER BY nombre COLLATE NOCASE ASC, id ASC")
    fun observarPorResidente(residenteId: Long): Flow<List<InvitadoEntity>>

    @Query("UPDATE invitados SET notificacionesSilenciadas = :silenciadas WHERE id = :id")
    suspend fun cambiarSilencio(id: Long, silenciadas: Boolean)
}

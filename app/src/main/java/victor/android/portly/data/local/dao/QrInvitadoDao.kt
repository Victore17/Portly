package victor.android.portly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import victor.android.portly.data.local.entidad.QrInvitadoEntity

@Dao
interface QrInvitadoDao {

    @Insert
    suspend fun insertar(qr: QrInvitadoEntity): Long

    @Update
    suspend fun actualizar(qr: QrInvitadoEntity)

    @Query("SELECT * FROM qr_invitados WHERE id = :id")
    suspend fun obtenerPorId(id: Long): QrInvitadoEntity?

    @Query("SELECT * FROM qr_invitados WHERE residenteId = :residenteId ORDER BY creadoEn DESC, id DESC")
    fun observarPorResidente(residenteId: Long): Flow<List<QrInvitadoEntity>>

    @Query("SELECT * FROM qr_invitados ORDER BY creadoEn DESC, id DESC")
    fun observarTodos(): Flow<List<QrInvitadoEntity>>

    @Query("SELECT * FROM qr_invitados WHERE invitadoId = :invitadoId ORDER BY creadoEn DESC, id DESC")
    fun observarPorInvitado(invitadoId: Long): Flow<List<QrInvitadoEntity>>

    /** Cancela el QR sin borrarlo, para que quede el rastro. */
    @Query("UPDATE qr_invitados SET cancelado = 1 WHERE id = :id")
    suspend fun marcarCancelado(id: Long)
}

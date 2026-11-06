package victor.android.portly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import victor.android.portly.data.local.entidad.CredencialEntity

@Dao
interface CredencialDao {

    /** Guarda la credencial; si el residente ya tenía una, la reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(credencial: CredencialEntity)

    @Query("SELECT * FROM credenciales WHERE residenteId = :residenteId")
    suspend fun obtener(residenteId: Long): CredencialEntity?
}

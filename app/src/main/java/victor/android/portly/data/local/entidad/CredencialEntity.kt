package victor.android.portly.data.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Contraseña de un residente, guardada como hash con sal (nunca en texto plano).
 *
 * Vive en su propia tabla para que el hash no viaje junto con los datos del
 * residente en las consultas normales.
 */
@Entity(
    tableName = "credenciales",
    foreignKeys = [
        ForeignKey(
            entity = ResidenteEntity::class,
            parentColumns = ["id"],
            childColumns = ["residenteId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
)
data class CredencialEntity(
    @PrimaryKey val residenteId: Long,
    val hash: String,
    val sal: String,
)

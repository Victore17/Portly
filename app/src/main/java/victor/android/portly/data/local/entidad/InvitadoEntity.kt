package victor.android.portly.data.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla de invitados.
 *
 * El par (residenteId, nombre) es único: un residente no puede tener dos
 * invitados con el mismo nombre, que es lo que permite reutilizar al invitado
 * ya guardado en vez de duplicarlo.
 */
@Entity(
    tableName = "invitados",
    foreignKeys = [
        ForeignKey(
            entity = ResidenteEntity::class,
            parentColumns = ["id"],
            childColumns = ["residenteId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["residenteId", "nombre"], unique = true)],
)
data class InvitadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nombre: String,
    val residenteId: Long,
    val notificacionesSilenciadas: Boolean = false,
)

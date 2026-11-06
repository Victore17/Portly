package victor.android.portly.data.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Tabla de QR de invitado.
 *
 * Guarda también el residenteId, aunque se podría llegar a él por el invitado,
 * para poder listar los QR de un residente sin unir tablas.
 */
@Entity(
    tableName = "qr_invitados",
    foreignKeys = [
        ForeignKey(
            entity = InvitadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["invitadoId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ResidenteEntity::class,
            parentColumns = ["id"],
            childColumns = ["residenteId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("invitadoId"),
        Index("residenteId"),
        Index("creadoEn"),
    ],
)
data class QrInvitadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invitadoId: Long,
    val residenteId: Long,
    val creadoEn: Instant,
    val cancelado: Boolean = false,
)

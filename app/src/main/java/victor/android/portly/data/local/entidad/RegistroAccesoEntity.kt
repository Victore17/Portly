package victor.android.portly.data.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import victor.android.portly.domain.modelo.TipoAcceso
import java.time.Instant

/**
 * Tabla del historial de entradas y salidas.
 *
 * [nombrePersona] es una copia del nombre al momento del acceso, a propósito:
 * el historial debe seguir siendo legible aunque luego se renombre a la persona.
 *
 * [invitadoId] es null cuando el acceso fue del residente mismo.
 */
@Entity(
    tableName = "registros_acceso",
    foreignKeys = [
        ForeignKey(
            entity = ResidenteEntity::class,
            parentColumns = ["id"],
            childColumns = ["residenteId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = InvitadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["invitadoId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("residenteId"),
        Index("invitadoId"),
        Index("momento"),
    ],
)
data class RegistroAccesoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nombrePersona: String,
    val tipo: TipoAcceso,
    val momento: Instant,
    val residenteId: Long,
    val invitadoId: Long? = null,
)

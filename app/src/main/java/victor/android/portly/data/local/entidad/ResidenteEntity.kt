package victor.android.portly.data.local.entidad

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla de residentes.
 *
 * No hay operación de borrado en [victor.android.portly.data.local.dao.ResidenteDao]
 * y las tablas que apuntan aquí usan `RESTRICT`, así que la base de datos misma
 * impide borrar un residente que tenga invitados, QR o accesos.
 *
 * [contacto] es único porque será el identificador con el que el residente
 * inicie sesión.
 */
@Entity(
    tableName = "residentes",
    indices = [Index(value = ["contacto"], unique = true)],
)
data class ResidenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nombre: String,
    val domicilio: String,
    val contacto: String,
    val activo: Boolean = true,
    val esAdmin: Boolean = false,
)

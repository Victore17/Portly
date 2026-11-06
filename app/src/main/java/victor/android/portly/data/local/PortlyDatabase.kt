package victor.android.portly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import victor.android.portly.data.local.dao.CredencialDao
import victor.android.portly.data.local.dao.InvitadoDao
import victor.android.portly.data.local.dao.QrInvitadoDao
import victor.android.portly.data.local.dao.RegistroAccesoDao
import victor.android.portly.data.local.dao.ResidenteDao
import victor.android.portly.data.local.entidad.CredencialEntity
import victor.android.portly.data.local.entidad.InvitadoEntity
import victor.android.portly.data.local.entidad.QrInvitadoEntity
import victor.android.portly.data.local.entidad.RegistroAccesoEntity
import victor.android.portly.data.local.entidad.ResidenteEntity

/** Base de datos local de Portly. Por ahora no hay servidor: todo vive aquí. */
@Database(
    entities = [
        ResidenteEntity::class,
        InvitadoEntity::class,
        QrInvitadoEntity::class,
        RegistroAccesoEntity::class,
        CredencialEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class PortlyDatabase : RoomDatabase() {

    abstract fun residenteDao(): ResidenteDao

    abstract fun invitadoDao(): InvitadoDao

    abstract fun qrInvitadoDao(): QrInvitadoDao

    abstract fun registroAccesoDao(): RegistroAccesoDao

    abstract fun credencialDao(): CredencialDao

    companion object {
        const val NOMBRE = "portly.db"
    }
}

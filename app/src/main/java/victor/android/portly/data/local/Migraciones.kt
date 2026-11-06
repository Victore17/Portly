package victor.android.portly.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Versión 2: agrega la tabla de credenciales (contraseñas con hash). */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `credenciales` (" +
                "`residenteId` INTEGER NOT NULL, " +
                "`hash` TEXT NOT NULL, " +
                "`sal` TEXT NOT NULL, " +
                "PRIMARY KEY(`residenteId`), " +
                "FOREIGN KEY(`residenteId`) REFERENCES `residentes`(`id`) " +
                "ON UPDATE RESTRICT ON DELETE RESTRICT)"
        )
    }
}

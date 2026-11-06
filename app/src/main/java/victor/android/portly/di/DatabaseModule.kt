package victor.android.portly.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import victor.android.portly.data.local.MIGRACION_1_2
import victor.android.portly.data.local.PortlyDatabase
import victor.android.portly.data.local.dao.CredencialDao
import victor.android.portly.data.local.dao.InvitadoDao
import victor.android.portly.data.local.dao.QrInvitadoDao
import victor.android.portly.data.local.dao.RegistroAccesoDao
import victor.android.portly.data.local.dao.ResidenteDao
import javax.inject.Singleton

/** Crea la base de datos y expone sus DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun proveerBaseDeDatos(@ApplicationContext contexto: Context): PortlyDatabase =
        Room.databaseBuilder(contexto, PortlyDatabase::class.java, PortlyDatabase.NOMBRE)
            .addMigrations(MIGRACION_1_2)
            .build()

    @Provides
    fun proveerResidenteDao(base: PortlyDatabase): ResidenteDao = base.residenteDao()

    @Provides
    fun proveerInvitadoDao(base: PortlyDatabase): InvitadoDao = base.invitadoDao()

    @Provides
    fun proveerQrInvitadoDao(base: PortlyDatabase): QrInvitadoDao = base.qrInvitadoDao()

    @Provides
    fun proveerRegistroAccesoDao(base: PortlyDatabase): RegistroAccesoDao = base.registroAccesoDao()

    @Provides
    fun proveerCredencialDao(base: PortlyDatabase): CredencialDao = base.credencialDao()
}

package victor.android.portly.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import victor.android.portly.data.qr.SecretoQrDataStore
import victor.android.portly.data.repositorio.CredencialRepositorioRoom
import victor.android.portly.data.seguridad.Cifrador
import victor.android.portly.data.seguridad.CifradorAesGcm
import victor.android.portly.data.seguridad.HasheadorPbkdf2
import victor.android.portly.data.sesion.SesionRepositorioDataStore
import victor.android.portly.domain.qr.ProveedorSecretoQr
import victor.android.portly.domain.repositorio.CredencialRepositorio
import victor.android.portly.domain.repositorio.SesionRepositorio
import victor.android.portly.domain.seguridad.HasheadorContrasena
import java.io.File
import javax.inject.Singleton

/** Dependencias de login y sesión. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutenticacionBindsModule {

    @Binds
    @Singleton
    abstract fun ligarCredencialRepositorio(impl: CredencialRepositorioRoom): CredencialRepositorio

    @Binds
    @Singleton
    abstract fun ligarSesionRepositorio(impl: SesionRepositorioDataStore): SesionRepositorio

    @Binds
    @Singleton
    abstract fun ligarSecretoQr(impl: SecretoQrDataStore): ProveedorSecretoQr

    @Binds
    abstract fun ligarHasheador(impl: HasheadorPbkdf2): HasheadorContrasena
}

@Module
@InstallIn(SingletonComponent::class)
object AutenticacionProvidesModule {

    @Provides
    @Singleton
    fun proveerCifrador(): Cifrador = CifradorAesGcm { CifradorAesGcm.claveDelKeystore() }

    @Provides
    @Singleton
    fun proveerDataStoreSesion(@ApplicationContext contexto: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(contexto.filesDir, "datastore/sesion.preferences_pb") },
        )
}

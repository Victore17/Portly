package victor.android.portly.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import victor.android.portly.data.repositorio.InvitadoRepositorioRoom
import victor.android.portly.data.repositorio.QrInvitadoRepositorioRoom
import victor.android.portly.data.repositorio.RegistroAccesoRepositorioRoom
import victor.android.portly.data.repositorio.ResidenteRepositorioRoom
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import victor.android.portly.domain.repositorio.QrInvitadoRepositorio
import victor.android.portly.domain.repositorio.RegistroAccesoRepositorio
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import javax.inject.Singleton

/** Liga cada contrato del dominio con su implementación de Room. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositorioModule {

    @Binds
    @Singleton
    abstract fun ligarResidenteRepositorio(
        impl: ResidenteRepositorioRoom,
    ): ResidenteRepositorio

    @Binds
    @Singleton
    abstract fun ligarInvitadoRepositorio(
        impl: InvitadoRepositorioRoom,
    ): InvitadoRepositorio

    @Binds
    @Singleton
    abstract fun ligarQrInvitadoRepositorio(
        impl: QrInvitadoRepositorioRoom,
    ): QrInvitadoRepositorio

    @Binds
    @Singleton
    abstract fun ligarRegistroAccesoRepositorio(
        impl: RegistroAccesoRepositorioRoom,
    ): RegistroAccesoRepositorio
}

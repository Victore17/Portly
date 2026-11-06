package victor.android.portly.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo de dependencias de alcance aplicación.
 *
 * Por ahora está vacío: las dependencias reales (base de datos, repositorios,
 * sesión) se agregan en las etapas siguientes.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule

package victor.android.portly.domain.repositorio

import kotlinx.coroutines.flow.Flow
import victor.android.portly.domain.modelo.Sesion

/** Guarda la sesión para que el residente siga logueado al reabrir la app. */
interface SesionRepositorio {

    /** La sesión actual, o null si no hay ninguna. */
    fun observar(): Flow<Sesion?>

    suspend fun guardar(sesion: Sesion)

    suspend fun cerrar()
}

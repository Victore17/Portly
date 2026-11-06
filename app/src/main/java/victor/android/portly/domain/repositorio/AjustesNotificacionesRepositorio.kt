package victor.android.portly.domain.repositorio

import kotlinx.coroutines.flow.Flow

/** Interruptor general "Silenciar a todos los invitados" de cada residente. */
interface AjustesNotificacionesRepositorio {

    fun observarSilenciarTodos(residenteId: Long): Flow<Boolean>

    suspend fun cambiarSilenciarTodos(residenteId: Long, silenciar: Boolean)
}

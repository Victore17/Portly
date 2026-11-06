package victor.android.portly.domain.notificaciones

import victor.android.portly.domain.modelo.RegistroAcceso

/**
 * Avisa al residente que alguien entró o salió.
 *
 * Por ahora no hay servidor: la única implementación no hace nada. Cuando
 * exista, aquí se conecta FCM.
 */
interface NotificadorAccesos {
    suspend fun notificarAcceso(registro: RegistroAcceso)
}

/** Implementación vacía: no envía nada. */
class NotificadorAccesosVacio @javax.inject.Inject constructor() : NotificadorAccesos {
    override suspend fun notificarAcceso(registro: RegistroAcceso) = Unit
}

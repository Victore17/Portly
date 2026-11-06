package victor.android.portly.domain.notificaciones

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.repositorio.AjustesNotificacionesRepositorio
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import victor.android.portly.domain.repositorio.SesionRepositorio
import javax.inject.Inject

/** Un invitado con su ajuste propio y su ajuste efectivo. */
data class AjusteInvitado(
    val invitado: Invitado,
    /** Lo que el residente eligió para este invitado; nunca lo pisa el general. */
    val silenciadoIndividual: Boolean,
    /** Lo que de verdad aplica: el general, si está activo, manda. */
    val silenciadoEfectivo: Boolean,
)

data class AjustesNotificaciones(
    val invitados: List<AjusteInvitado>,
    /** El general solo se muestra con 2 o más invitados guardados. */
    val mostrarSilenciarTodos: Boolean,
    val silenciarTodos: Boolean,
) {
    companion object {
        val VACIO = AjustesNotificaciones(emptyList(), mostrarSilenciarTodos = false, silenciarTodos = false)
    }
}

/**
 * Interruptores de notificaciones del residente con sesión iniciada.
 *
 * El interruptor general se guarda aparte de los individuales, así al apagarlo
 * cada invitado conserva su ajuste anterior.
 */
class ServicioAjustesNotificaciones @Inject constructor(
    private val invitados: InvitadoRepositorio,
    private val ajustes: AjustesNotificacionesRepositorio,
    private val sesion: SesionRepositorio,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observar(): Flow<AjustesNotificaciones> =
        sesion.observar().flatMapLatest { actual ->
            if (actual == null) {
                flowOf(AjustesNotificaciones.VACIO)
            } else {
                combine(
                    invitados.observarPorResidente(actual.residenteId),
                    ajustes.observarSilenciarTodos(actual.residenteId),
                ) { lista, general ->
                    // Con menos de 2 invitados el general no existe, y no debe mandar.
                    val mostrar = lista.size >= MINIMO_PARA_GENERAL
                    val generalAplica = mostrar && general
                    AjustesNotificaciones(
                        invitados = lista.map {
                            AjusteInvitado(it, it.notificacionesSilenciadas, generalAplica || it.notificacionesSilenciadas)
                        },
                        mostrarSilenciarTodos = mostrar,
                        silenciarTodos = general,
                    )
                }
            }
        }

    /** Cambia el interruptor propio del invitado (solo si es del residente en sesión). */
    suspend fun cambiarInvitado(invitadoId: Long, silenciado: Boolean): Boolean {
        val residenteId = sesion.observar().first()?.residenteId ?: return false
        val invitado = invitados.obtener(invitadoId)?.takeIf { it.residenteId == residenteId } ?: return false
        invitados.cambiarSilencio(invitado.id, silenciado)
        return true
    }

    suspend fun cambiarSilenciarTodos(silenciar: Boolean): Boolean {
        val residenteId = sesion.observar().first()?.residenteId ?: return false
        ajustes.cambiarSilenciarTodos(residenteId, silenciar)
        return true
    }

    /** ¿Se debe avisar de los accesos de este invitado? */
    suspend fun debeNotificar(invitadoId: Long): Boolean {
        val invitado = invitados.obtener(invitadoId) ?: return false
        if (invitado.notificacionesSilenciadas) return false
        val general = ajustes.observarSilenciarTodos(invitado.residenteId).first()
        return !general
    }

    private companion object {
        const val MINIMO_PARA_GENERAL = 2
    }
}

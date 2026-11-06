package victor.android.portly.domain.historial

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.TipoAcceso
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import victor.android.portly.domain.repositorio.RegistroAccesoRepositorio
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import victor.android.portly.domain.repositorio.SesionRepositorio
import java.time.Clock
import javax.inject.Inject

sealed interface ResultadoRegistroAcceso {
    data class Exito(val registro: RegistroAcceso) : ResultadoRegistroAcceso
    data object ResidenteNoDisponible : ResultadoRegistroAcceso
    data object InvitadoNoPertenece : ResultadoRegistroAcceso
}

/**
 * Historial de entradas y salidas.
 *
 * La consulta siempre se hace con el residente de la sesión actual, así un
 * residente nunca puede pedir el historial de otro.
 */
class ServicioHistorial @Inject constructor(
    private val registros: RegistroAccesoRepositorio,
    private val residentes: ResidenteRepositorio,
    private val invitados: InvitadoRepositorio,
    private val sesion: SesionRepositorio,
    private val reloj: Clock,
) {

    /**
     * Historial del residente con sesión iniciada (propio e invitados), del más
     * reciente al más antiguo. Sin sesión devuelve una lista vacía.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarMiHistorial(): Flow<List<RegistroAcceso>> =
        sesion.observar().flatMapLatest { actual ->
            if (actual == null) {
                flowOf(emptyList())
            } else {
                registros.observarPorResidente(actual.residenteId)
                    .map { lista -> lista.sortedWith(ORDEN_RECIENTE_PRIMERO) }
            }
        }

    /**
     * Registra una entrada o salida a la hora actual.
     *
     * @param invitadoId null si pasó el residente mismo; si pasó un invitado,
     *   debe ser uno de los invitados de [residenteId].
     */
    suspend fun registrar(
        residenteId: Long,
        tipo: TipoAcceso,
        invitadoId: Long? = null,
    ): ResultadoRegistroAcceso {
        val residente = residentes.obtener(residenteId)?.takeIf { it.activo }
            ?: return ResultadoRegistroAcceso.ResidenteNoDisponible

        val nombre = if (invitadoId == null) {
            residente.nombre
        } else {
            invitados.obtener(invitadoId)?.takeIf { it.residenteId == residenteId }?.nombre
                ?: return ResultadoRegistroAcceso.InvitadoNoPertenece
        }

        val registro = RegistroAcceso(
            nombrePersona = nombre,
            tipo = tipo,
            momento = reloj.instant(),
            residenteId = residenteId,
            invitadoId = invitadoId,
        )
        val id = registros.registrar(registro)
        return ResultadoRegistroAcceso.Exito(registro.copy(id = id))
    }

    private companion object {
        val ORDEN_RECIENTE_PRIMERO =
            compareByDescending<RegistroAcceso> { it.momento }.thenByDescending { it.id }
    }
}

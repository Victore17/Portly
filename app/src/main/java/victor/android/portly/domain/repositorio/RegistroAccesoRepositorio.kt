package victor.android.portly.domain.repositorio

import kotlinx.coroutines.flow.Flow
import victor.android.portly.domain.modelo.RegistroAcceso
import java.time.Instant

/**
 * Acceso al historial de entradas y salidas.
 *
 * Solo se puede agregar y consultar: los registros no se editan ni se borran.
 */
interface RegistroAccesoRepositorio {

    /**
     * Historial de un residente, el suyo y el de sus invitados, del más
     * reciente al más antiguo.
     */
    fun observarPorResidente(residenteId: Long): Flow<List<RegistroAcceso>>

    /**
     * Lo mismo, pero limitado a un rango de tiempo.
     *
     * @param desde incluido.
     * @param hasta excluido, para poder pedir "un día" sin preocuparse por el
     *   último milisegundo.
     */
    fun observarPorResidenteEnRango(
        residenteId: Long,
        desde: Instant,
        hasta: Instant,
    ): Flow<List<RegistroAcceso>>

    /** Guarda un acceso y devuelve el id que le asignó la base. */
    suspend fun registrar(registro: RegistroAcceso): Long
}

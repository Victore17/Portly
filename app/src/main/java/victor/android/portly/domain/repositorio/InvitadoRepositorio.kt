package victor.android.portly.domain.repositorio

import kotlinx.coroutines.flow.Flow
import victor.android.portly.domain.modelo.Invitado

/** Acceso a los invitados guardados por cada residente. */
interface InvitadoRepositorio {

    /** Invitados de un residente, ordenados por nombre. */
    fun observarPorResidente(residenteId: Long): Flow<List<Invitado>>

    suspend fun obtener(id: Long): Invitado?

    /**
     * Busca entre los invitados guardados de un residente uno que ya se llame
     * [nombre], para poder reutilizarlo en vez de duplicarlo.
     */
    suspend fun buscarPorNombre(residenteId: Long, nombre: String): Invitado?

    suspend fun agregar(invitado: Invitado): Long

    suspend fun actualizar(invitado: Invitado)

    /** Cambia el interruptor individual de notificaciones del invitado. */
    suspend fun cambiarSilencio(id: Long, silenciadas: Boolean)
}

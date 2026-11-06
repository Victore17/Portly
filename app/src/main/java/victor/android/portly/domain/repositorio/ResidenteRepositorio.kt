package victor.android.portly.domain.repositorio

import kotlinx.coroutines.flow.Flow
import victor.android.portly.domain.modelo.Residente

/**
 * Acceso a los residentes.
 *
 * A propósito no existe una operación de borrado: la regla del proyecto es que
 * un residente nunca se borra, solo se activa o se desactiva con
 * [cambiarActivo], para conservar su historial de accesos.
 */
interface ResidenteRepositorio {

    /** Todos los residentes, activos e inactivos, ordenados por nombre. */
    fun observarTodos(): Flow<List<Residente>>

    /** Solo los residentes activos, ordenados por nombre. */
    fun observarActivos(): Flow<List<Residente>>

    /** El residente indicado, o null si no existe. */
    fun observar(id: Long): Flow<Residente?>

    suspend fun obtener(id: Long): Residente?

    /** El residente con ese teléfono o correo (su usuario de login), o null. */
    suspend fun obtenerPorContacto(contacto: String): Residente?

    /** Guarda un residente nuevo y devuelve el id que le asignó la base. */
    suspend fun agregar(residente: Residente): Long

    suspend fun actualizar(residente: Residente)

    /** Activa o desactiva al residente sin tocar su historial. */
    suspend fun cambiarActivo(id: Long, activo: Boolean)
}

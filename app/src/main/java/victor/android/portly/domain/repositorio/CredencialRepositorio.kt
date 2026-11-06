package victor.android.portly.domain.repositorio

import victor.android.portly.domain.modelo.Credencial

interface CredencialRepositorio {

    /** Guarda o reemplaza la credencial del residente. */
    suspend fun guardar(credencial: Credencial)

    suspend fun obtener(residenteId: Long): Credencial?
}

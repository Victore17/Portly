package victor.android.portly.domain.autenticacion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import victor.android.portly.domain.modelo.Credencial
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.repositorio.CredencialRepositorio
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import victor.android.portly.domain.repositorio.SesionRepositorio
import victor.android.portly.domain.seguridad.HasheadorContrasena
import javax.inject.Inject

sealed interface ResultadoLogin {
    data class Exito(val sesion: Sesion) : ResultadoLogin

    /** Usuario inexistente, sin contraseña asignada o contraseña errónea. No se distingue a propósito. */
    data object CredencialesInvalidas : ResultadoLogin

    /** Las credenciales son correctas pero el residente está desactivado. */
    data object ResidenteInactivo : ResultadoLogin
}

/** Login, sesión persistente y contraseñas de los residentes. */
class ServicioAutenticacion @Inject constructor(
    private val residentes: ResidenteRepositorio,
    private val credenciales: CredencialRepositorio,
    private val hasheador: HasheadorContrasena,
    private val sesiones: SesionRepositorio,
) {

    fun observarSesion(): Flow<Sesion?> = sesiones.observar()

    /** Inicia sesión con el teléfono o correo y la contraseña. */
    suspend fun iniciarSesion(contacto: String, contrasena: String): ResultadoLogin {
        val residente = residentes.obtenerPorContacto(contacto.trim())
            ?: return ResultadoLogin.CredencialesInvalidas
        val credencial = credenciales.obtener(residente.id)
            ?: return ResultadoLogin.CredencialesInvalidas
        if (!hasheador.verificar(contrasena, credencial.hash, credencial.sal)) {
            return ResultadoLogin.CredencialesInvalidas
        }
        if (!residente.activo) return ResultadoLogin.ResidenteInactivo

        val sesion = Sesion(residenteId = residente.id, esAdmin = residente.esAdmin)
        sesiones.guardar(sesion)
        return ResultadoLogin.Exito(sesion)
    }

    /**
     * Revisa la sesión guardada contra la base: si el residente ya no existe o
     * fue desactivado la cierra, y si cambió su rol de administrador la actualiza.
     */
    suspend fun restaurarSesion(): Sesion? {
        val guardada = sesiones.observar().first() ?: return null
        val residente = residentes.obtener(guardada.residenteId)
        if (residente == null || !residente.activo) {
            sesiones.cerrar()
            return null
        }
        val vigente = Sesion(residente.id, residente.esAdmin)
        if (vigente != guardada) sesiones.guardar(vigente)
        return vigente
    }

    suspend fun cerrarSesion() = sesiones.cerrar()

    /** Asigna o cambia la contraseña de un residente; solo se guarda su hash. */
    suspend fun establecerContrasena(residenteId: Long, contrasena: String) {
        require(contrasena.isNotBlank()) { "La contraseña no puede estar vacía" }
        val hash = hasheador.hashear(contrasena)
        credenciales.guardar(Credencial(residenteId, hash.hash, hash.sal))
    }
}

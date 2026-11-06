package victor.android.portly

import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.modelo.Residente
import victor.android.portly.domain.repositorio.ResidenteRepositorio

/**
 * Solo para pruebas en debug: crea un administrador de prueba si todavía no existe.
 * No se ejecuta en builds de release.
 */
object SembradorDebug {
    const val CONTACTO = "sora@gmail.com"
    const val CONTRASENA = "1234"

    suspend fun sembrar(residentes: ResidenteRepositorio, autenticacion: ServicioAutenticacion) {
        if (residentes.obtenerPorContacto(CONTACTO) != null) return
        val id = residentes.agregar(
            Residente(
                nombre = "Sora",
                domicilio = "Casa de prueba",
                contacto = CONTACTO,
                esAdmin = true,
            ),
        )
        autenticacion.establecerContrasena(id, CONTRASENA)
    }
}

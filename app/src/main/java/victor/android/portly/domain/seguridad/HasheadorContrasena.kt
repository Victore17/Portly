package victor.android.portly.domain.seguridad

/** Resultado de hashear una contraseña. Ambos valores van en Base64. */
data class HashContrasena(val hash: String, val sal: String)

interface HasheadorContrasena {

    /** Hashea con una sal nueva y aleatoria. */
    fun hashear(contrasena: String): HashContrasena

    fun verificar(contrasena: String, hash: String, sal: String): Boolean
}

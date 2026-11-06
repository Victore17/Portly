package victor.android.portly.data.seguridad

/** Cifra y descifra bytes. Descifrar datos alterados lanza excepción. */
interface Cifrador {
    fun cifrar(datos: ByteArray): ByteArray
    fun descifrar(datos: ByteArray): ByteArray
}

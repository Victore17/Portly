package victor.android.portly.data.sesion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import victor.android.portly.data.seguridad.Cifrador
import victor.android.portly.domain.modelo.Sesion
import victor.android.portly.domain.repositorio.SesionRepositorio
import java.util.Base64
import javax.inject.Inject

/**
 * Sesión en DataStore. El contenido se cifra con [Cifrador] (clave en el
 * Android Keystore) antes de escribirse. Si el dato está dañado o la clave ya
 * no sirve, se trata como que no hay sesión.
 */
class SesionRepositorioDataStore @Inject constructor(
    private val datos: DataStore<Preferences>,
    private val cifrador: Cifrador,
) : SesionRepositorio {

    override fun observar(): Flow<Sesion?> = datos.data.map { prefs ->
        prefs[CLAVE]?.let(::leer)
    }

    override suspend fun guardar(sesion: Sesion) {
        val texto = "${sesion.residenteId}$SEPARADOR${sesion.esAdmin}"
        val cifrado = cifrador.cifrar(texto.toByteArray(Charsets.UTF_8))
        datos.edit { it[CLAVE] = Base64.getEncoder().encodeToString(cifrado) }
    }

    override suspend fun cerrar() {
        datos.edit { it.remove(CLAVE) }
    }

    private fun leer(guardado: String): Sesion? = runCatching {
        val texto = String(cifrador.descifrar(Base64.getDecoder().decode(guardado)), Charsets.UTF_8)
        val (id, admin) = texto.split(SEPARADOR)
        Sesion(residenteId = id.toLong(), esAdmin = admin.toBooleanStrict())
    }.getOrNull()

    private companion object {
        val CLAVE = stringPreferencesKey("sesion")
        const val SEPARADOR = ":"
    }
}

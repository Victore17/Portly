package victor.android.portly.data.qr

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import victor.android.portly.data.seguridad.Cifrador
import victor.android.portly.domain.qr.ProveedorSecretoQr
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

/** Guarda el secreto cifrado (clave en el Keystore) en DataStore. */
class SecretoQrDataStore @Inject constructor(
    private val datos: DataStore<Preferences>,
    private val cifrador: Cifrador,
) : ProveedorSecretoQr {

    private val candado = Mutex()

    override suspend fun obtener(): ByteArray = candado.withLock {
        val guardado = datos.data.first()[CLAVE]
        val existente = guardado?.let {
            runCatching { cifrador.descifrar(Base64.getDecoder().decode(it)) }.getOrNull()
        }
        if (existente != null) return existente

        val nuevo = ByteArray(TAM_SECRETO).also(SecureRandom()::nextBytes)
        val cifrado = cifrador.cifrar(nuevo)
        datos.edit { it[CLAVE] = Base64.getEncoder().encodeToString(cifrado) }
        nuevo
    }

    private companion object {
        val CLAVE = stringPreferencesKey("secreto_qr")
        const val TAM_SECRETO = 32
    }
}

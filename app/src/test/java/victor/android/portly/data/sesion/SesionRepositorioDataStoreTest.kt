package victor.android.portly.data.sesion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import victor.android.portly.data.seguridad.CifradorAesGcm
import victor.android.portly.domain.modelo.Sesion
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SesionRepositorioDataStoreTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var archivo: File
    private lateinit var clave: SecretKey
    private var job = Job()

    @Before
    fun preparar() {
        archivo = File(carpeta.root, "sesion.preferences_pb")
        clave = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    @After
    fun limpiar() = runBlocking { job.cancelAndJoin() }

    private fun crearDataStore(): DataStore<Preferences> {
        job = Job()
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { archivo },
        )
    }

    private fun repositorio(datos: DataStore<Preferences>, k: SecretKey = clave) =
        SesionRepositorioDataStore(datos, CifradorAesGcm { k })

    @Test
    fun sin_sesion_guardada_devuelve_null() = runBlocking {
        assertNull(repositorio(crearDataStore()).observar().first())
    }

    @Test
    fun guardar_y_leer_devuelve_la_misma_sesion() = runBlocking {
        val repo = repositorio(crearDataStore())

        repo.guardar(Sesion(residenteId = 7, esAdmin = true))

        assertEquals(Sesion(7, true), repo.observar().first())
    }

    @Test
    fun la_sesion_persiste_aunque_se_reabra_el_almacenamiento() = runBlocking {
        repositorio(crearDataStore()).guardar(Sesion(residenteId = 3, esAdmin = false))
        job.cancelAndJoin() // simula cerrar la app

        val reabierto = repositorio(crearDataStore())

        assertEquals(Sesion(3, false), reabierto.observar().first())
    }

    @Test
    fun cerrar_elimina_la_sesion() = runBlocking {
        val repo = repositorio(crearDataStore())
        repo.guardar(Sesion(1, false))

        repo.cerrar()

        assertNull(repo.observar().first())
    }

    @Test
    fun lo_guardado_esta_cifrado() = runBlocking {
        val datos = crearDataStore()
        repositorio(datos).guardar(Sesion(residenteId = 12345, esAdmin = true))

        val crudo = datos.data.first()[stringPreferencesKey("sesion")]!!

        assertFalse(crudo.contains("12345"))
        assertFalse(crudo.contains("true"))
    }

    @Test
    fun si_la_clave_cambio_o_el_dato_esta_danado_no_hay_sesion() = runBlocking {
        val datos = crearDataStore()
        repositorio(datos).guardar(Sesion(1, true))

        val otraClave = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertNull(repositorio(datos, otraClave).observar().first())

        datos.edit { it[stringPreferencesKey("sesion")] = "basura" }
        assertNull(repositorio(datos).observar().first())
    }
}

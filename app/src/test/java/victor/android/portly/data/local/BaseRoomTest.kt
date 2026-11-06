package victor.android.portly.data.local

import androidx.room.Room
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import victor.android.portly.data.local.dao.InvitadoDao
import victor.android.portly.data.local.dao.QrInvitadoDao
import victor.android.portly.data.local.dao.RegistroAccesoDao
import victor.android.portly.data.local.dao.ResidenteDao
import victor.android.portly.data.local.entidad.InvitadoEntity
import victor.android.portly.data.local.entidad.ResidenteEntity

/**
 * Base para los tests de persistencia: levanta una [PortlyDatabase] en memoria
 * y la tira al terminar cada test, así cada uno arranca limpio.
 *
 * Room necesita un `Context`, por eso corre con Robolectric aunque sea un test
 * unitario de JVM.
 *
 * Estos tests usan `runBlocking` y no `runTest` a propósito: Room emite sus
 * Flow desde sus propios hilos, y con el tiempo virtual de `runTest` los
 * tiempos de espera de Turbine se agotarían antes de que llegue la emisión
 * real. Aquí no se prueba nada que dependa del reloj, así que esperar de
 * verdad es lo correcto.
 */
@RunWith(RobolectricTestRunner::class)
abstract class BaseRoomTest {

    protected lateinit var base: PortlyDatabase

    protected val residentes: ResidenteDao get() = base.residenteDao()
    protected val invitados: InvitadoDao get() = base.invitadoDao()
    protected val qrs: QrInvitadoDao get() = base.qrInvitadoDao()
    protected val registros: RegistroAccesoDao get() = base.registroAccesoDao()

    @Before
    fun crearBase() {
        base = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            PortlyDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun cerrarBase() {
        base.close()
    }

    /** Inserta un residente de prueba y devuelve su id. */
    protected suspend fun insertarResidente(
        nombre: String = "Ana Ruiz",
        domicilio: String = "Calle 1 #10",
        contacto: String = "5550001111",
        activo: Boolean = true,
        esAdmin: Boolean = false,
    ): Long = residentes.insertar(
        ResidenteEntity(
            nombre = nombre,
            domicilio = domicilio,
            contacto = contacto,
            activo = activo,
            esAdmin = esAdmin,
        )
    )

    /** Inserta un invitado de prueba y devuelve su id. */
    protected suspend fun insertarInvitado(
        residenteId: Long,
        nombre: String = "Luis Invitado",
        silenciadas: Boolean = false,
    ): Long = invitados.insertar(
        InvitadoEntity(
            nombre = nombre,
            residenteId = residenteId,
            notificacionesSilenciadas = silenciadas,
        )
    )
}

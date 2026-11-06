package victor.android.portly.domain.qr

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import victor.android.portly.data.local.BaseRoomTest
import victor.android.portly.data.repositorio.ResidenteRepositorioRoom

class ServicioQrResidenteTest : BaseRoomTest() {

    private val secreto = "secreto-de-prueba".toByteArray()
    private lateinit var servicio: ServicioQrResidente

    @Before
    fun crearServicio() {
        val proveedor = object : ProveedorSecretoQr {
            override suspend fun obtener() = secreto
        }
        servicio = ServicioQrResidente(ResidenteRepositorioRoom(residentes), proveedor)
    }

    @Test
    fun el_qr_del_residente_se_interpreta_como_residente_con_su_id() = runBlocking {
        val id = insertarResidente()

        val texto = servicio.contenidoDe(id)

        assertNotNull(texto)
        assertEquals(ContenidoQr(TipoQr.RESIDENTE, id), FormatoQr(secreto).interpretar(texto!!))
    }

    @Test
    fun cada_residente_tiene_un_qr_distinto() = runBlocking {
        val a = insertarResidente(contacto = "1")
        val b = insertarResidente(contacto = "2")

        assertEquals(false, servicio.contenidoDe(a) == servicio.contenidoDe(b))
    }

    @Test
    fun no_hay_qr_para_un_residente_inexistente_o_desactivado() = runBlocking {
        val inactivo = insertarResidente(activo = false)

        assertNull(servicio.contenidoDe(inactivo))
        assertNull(servicio.contenidoDe(999L))
    }
}

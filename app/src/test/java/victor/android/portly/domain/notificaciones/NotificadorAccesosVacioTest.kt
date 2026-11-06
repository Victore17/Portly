package victor.android.portly.domain.notificaciones

import kotlinx.coroutines.runBlocking
import org.junit.Test
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.TipoAcceso
import java.time.Instant

class NotificadorAccesosVacioTest {

    @Test
    fun no_lanza_errores_y_no_envia_nada() = runBlocking {
        val notificador: NotificadorAccesos = NotificadorAccesosVacio()

        notificador.notificarAcceso(
            RegistroAcceso(nombrePersona = "Ana", tipo = TipoAcceso.ENTRADA, momento = Instant.EPOCH, residenteId = 1),
        )
    }
}

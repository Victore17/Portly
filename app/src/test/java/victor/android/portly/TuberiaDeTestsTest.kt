package victor.android.portly

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de la Etapa 0.
 *
 * No prueban lógica de la app todavía (aún no existe): confirman que la
 * tubería de tests funciona de punta a punta, es decir que JUnit,
 * kotlinx-coroutines-test, Turbine y MockK están bien conectados.
 */
class TuberiaDeTestsTest {

    /** Interfaz de juguete, solo para comprobar que MockK puede simularla. */
    private interface Saludador {
        fun saludar(nombre: String): String
    }

    @Test
    fun junit_corre() {
        assertEquals(4, 2 + 2)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun coroutines_test_avanza_el_tiempo_virtual() = runTest {
        val antes = testScheduler.currentTime
        delay(5_000)
        assertEquals(5_000L, testScheduler.currentTime - antes)
    }

    @Test
    fun turbine_lee_los_valores_de_un_flow() = runTest {
        val estado = MutableStateFlow("inicial")

        estado.test {
            assertEquals("inicial", awaitItem())
            estado.value = "siguiente"
            assertEquals("siguiente", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mockk_simula_y_verifica() {
        val saludador = mockk<Saludador>()
        every { saludador.saludar("Portly") } returns "Hola Portly"

        val resultado = saludador.saludar("Portly")

        assertEquals("Hola Portly", resultado)
        verify(exactly = 1) { saludador.saludar("Portly") }
    }
}

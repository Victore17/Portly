package victor.android.portly.data.local

import androidx.room.TypeConverter
import victor.android.portly.domain.modelo.TipoAcceso
import java.time.Instant

/**
 * Traductores entre los tipos del dominio y los que entiende SQLite.
 *
 * Los [Instant] se guardan como milisegundos desde la época (UTC), para que
 * ordenar y filtrar por rango sea una comparación numérica.
 */
class Convertidores {

    @TypeConverter
    fun instanteAMilis(valor: Instant?): Long? = valor?.toEpochMilli()

    @TypeConverter
    fun milisAInstante(valor: Long?): Instant? = valor?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun tipoAccesoATexto(valor: TipoAcceso): String = valor.name

    @TypeConverter
    fun textoATipoAcceso(valor: String): TipoAcceso = TipoAcceso.valueOf(valor)
}

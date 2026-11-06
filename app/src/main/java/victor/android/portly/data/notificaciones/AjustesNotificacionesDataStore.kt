package victor.android.portly.data.notificaciones

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import victor.android.portly.domain.repositorio.AjustesNotificacionesRepositorio
import javax.inject.Inject

class AjustesNotificacionesDataStore @Inject constructor(
    private val datos: DataStore<Preferences>,
) : AjustesNotificacionesRepositorio {

    override fun observarSilenciarTodos(residenteId: Long): Flow<Boolean> =
        datos.data.map { it[clave(residenteId)] ?: false }.distinctUntilChanged()

    override suspend fun cambiarSilenciarTodos(residenteId: Long, silenciar: Boolean) {
        datos.edit { it[clave(residenteId)] = silenciar }
    }

    private fun clave(residenteId: Long) = booleanPreferencesKey("silenciar_todos_$residenteId")
}

package victor.android.portly.domain.administracion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import victor.android.portly.domain.autenticacion.ServicioAutenticacion
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Residente
import victor.android.portly.domain.qr.ValidadorQr
import victor.android.portly.domain.repositorio.InvitadoRepositorio
import victor.android.portly.domain.repositorio.QrInvitadoRepositorio
import victor.android.portly.domain.repositorio.RegistroAccesoRepositorio
import victor.android.portly.domain.repositorio.ResidenteRepositorio
import victor.android.portly.domain.repositorio.SesionRepositorio
import java.time.Instant
import javax.inject.Inject

sealed interface ResultadoAdmin {
    /** @property residenteId id del residente creado, solo al agregar. */
    data class Exito(val residenteId: Long? = null) : ResultadoAdmin

    /** Quien llama no es un administrador activo. */
    data object NoAutorizado : ResultadoAdmin
    data object DatosInvalidos : ResultadoAdmin

    /** Otro residente ya usa ese teléfono o correo (es el usuario de login). */
    data object ContactoDuplicado : ResultadoAdmin
    data object NoExiste : ResultadoAdmin

    /** La operación dejaría la privada sin administradores activos. */
    data object UltimoAdministrador : ResultadoAdmin
}

/**
 * Filtros del registro completo. Todos opcionales.
 *
 * @property desde incluido.
 * @property hasta excluido.
 */
data class FiltroRegistro(
    val residenteId: Long? = null,
    val desde: Instant? = null,
    val hasta: Instant? = null,
)

/** Un QR de invitado vigente hoy, con su invitado y el residente que lo creó. */
data class QrActivoAdmin(val qr: QrInvitado, val invitado: Invitado, val residente: Residente)

/**
 * Funciones del rol administrador.
 *
 * Cada llamada revisa contra la base que la sesión actual pertenezca a un
 * residente activo con rol de administrador, no solo lo que dice la sesión
 * guardada. Sin permiso, las consultas devuelven listas vacías y las acciones
 * [ResultadoAdmin.NoAutorizado]. Los residentes nunca se borran.
 */
class ServicioAdministracion @Inject constructor(
    private val residentes: ResidenteRepositorio,
    private val registros: RegistroAccesoRepositorio,
    private val qrs: QrInvitadoRepositorio,
    private val invitados: InvitadoRepositorio,
    private val sesion: SesionRepositorio,
    private val autenticacion: ServicioAutenticacion,
    private val validador: ValidadorQr,
) {

    // ---- Consultas ----

    fun observarResidentes(): Flow<List<Residente>> =
        siEsAdmin(emptyList()) { residentes.observarTodos() }

    /** Registro de toda la privada, del más reciente al más antiguo, con [filtro]. */
    fun observarRegistro(filtro: FiltroRegistro = FiltroRegistro()): Flow<List<RegistroAcceso>> =
        siEsAdmin(emptyList()) {
            registros.observarTodos().map { lista ->
                lista.filter { r ->
                    (filtro.residenteId == null || r.residenteId == filtro.residenteId) &&
                        (filtro.desde == null || r.momento >= filtro.desde) &&
                        (filtro.hasta == null || r.momento < filtro.hasta)
                }
            }
        }

    /** QR de invitados vigentes hoy (no cancelados) de cualquier residente. */
    fun observarQrsActivos(): Flow<List<QrActivoAdmin>> =
        siEsAdmin(emptyList()) {
            combine(qrs.observarTodos(), residentes.observarTodos()) { listaQr, listaRes ->
                val porResidente = listaRes.associateBy { it.id }
                listaQr.filter { !it.cancelado && validador.vigentePorFecha(it) }
                    .mapNotNull { qr ->
                        val residente = porResidente[qr.residenteId] ?: return@mapNotNull null
                        val invitado = invitados.obtener(qr.invitadoId) ?: return@mapNotNull null
                        QrActivoAdmin(qr, invitado, residente)
                    }
            }
        }

    // ---- Acciones ----

    suspend fun agregarResidente(
        nombre: String,
        domicilio: String,
        contacto: String,
        contrasena: String,
        esAdmin: Boolean = false,
    ): ResultadoAdmin {
        if (!esAdminActivo()) return ResultadoAdmin.NoAutorizado
        val nuevo = Residente(
            nombre = nombre.trim(),
            domicilio = domicilio.trim(),
            contacto = contacto.trim(),
            esAdmin = esAdmin,
        )
        if (nuevo.hayCamposVacios() || contrasena.isBlank()) return ResultadoAdmin.DatosInvalidos
        if (residentes.obtenerPorContacto(nuevo.contacto) != null) return ResultadoAdmin.ContactoDuplicado

        val id = residentes.agregar(nuevo)
        autenticacion.establecerContrasena(id, contrasena)
        return ResultadoAdmin.Exito(id)
    }

    /** Guarda los datos nuevos de un residente existente (el id identifica a cuál). */
    suspend fun editarResidente(editado: Residente): ResultadoAdmin {
        if (!esAdminActivo()) return ResultadoAdmin.NoAutorizado
        val actual = residentes.obtener(editado.id) ?: return ResultadoAdmin.NoExiste
        val limpio = editado.copy(
            nombre = editado.nombre.trim(),
            domicilio = editado.domicilio.trim(),
            contacto = editado.contacto.trim(),
        )
        if (limpio.hayCamposVacios()) return ResultadoAdmin.DatosInvalidos
        val otro = residentes.obtenerPorContacto(limpio.contacto)
        if (otro != null && otro.id != limpio.id) return ResultadoAdmin.ContactoDuplicado
        if (actual.esAdminActivo() && !limpio.esAdminActivo() && esElUltimoAdministrador()) {
            return ResultadoAdmin.UltimoAdministrador
        }
        residentes.actualizar(limpio)
        return ResultadoAdmin.Exito()
    }

    /** Activa o desactiva a un residente. Nunca se borra: su historial se conserva. */
    suspend fun cambiarActivo(residenteId: Long, activo: Boolean): ResultadoAdmin {
        if (!esAdminActivo()) return ResultadoAdmin.NoAutorizado
        val actual = residentes.obtener(residenteId) ?: return ResultadoAdmin.NoExiste
        if (!activo && actual.esAdminActivo() && esElUltimoAdministrador()) {
            return ResultadoAdmin.UltimoAdministrador
        }
        residentes.cambiarActivo(residenteId, activo)
        return ResultadoAdmin.Exito()
    }

    /** Cancela el QR de invitado de cualquier residente. */
    suspend fun cancelarQr(qrId: Long): ResultadoAdmin {
        if (!esAdminActivo()) return ResultadoAdmin.NoAutorizado
        qrs.obtener(qrId) ?: return ResultadoAdmin.NoExiste
        qrs.cancelar(qrId)
        return ResultadoAdmin.Exito()
    }

    // ---- Apoyo ----

    private fun Residente.hayCamposVacios() =
        nombre.isEmpty() || domicilio.isEmpty() || contacto.isEmpty()

    private fun Residente.esAdminActivo() = activo && esAdmin

    private suspend fun esAdminActivo(): Boolean {
        val actual = sesion.observar().first() ?: return false
        return residentes.obtener(actual.residenteId)?.esAdminActivo() == true
    }

    private suspend fun esElUltimoAdministrador(): Boolean =
        residentes.observarTodos().first().count { it.esAdminActivo() } <= 1

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> siEsAdmin(vacio: T, datos: () -> Flow<T>): Flow<T> =
        sesion.observar().flatMapLatest { actual ->
            if (actual == null) {
                flowOf(vacio)
            } else {
                residentes.observar(actual.residenteId).flatMapLatest { r ->
                    if (r?.esAdminActivo() == true) datos() else flowOf(vacio)
                }
            }
        }
}

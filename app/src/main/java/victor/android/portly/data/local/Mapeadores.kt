package victor.android.portly.data.local

import victor.android.portly.data.local.entidad.InvitadoEntity
import victor.android.portly.data.local.entidad.QrInvitadoEntity
import victor.android.portly.data.local.entidad.RegistroAccesoEntity
import victor.android.portly.data.local.entidad.ResidenteEntity
import victor.android.portly.domain.modelo.Invitado
import victor.android.portly.domain.modelo.QrInvitado
import victor.android.portly.domain.modelo.RegistroAcceso
import victor.android.portly.domain.modelo.Residente

/**
 * Traducción entre las entidades de Room y los modelos del dominio.
 *
 * Están separados para que el dominio no dependa de Room y se pueda cambiar la
 * forma de guardar sin tocar el resto de la app.
 */

internal fun ResidenteEntity.aDominio() = Residente(
    id = id,
    nombre = nombre,
    domicilio = domicilio,
    contacto = contacto,
    activo = activo,
    esAdmin = esAdmin,
)

internal fun Residente.aEntidad() = ResidenteEntity(
    id = id,
    nombre = nombre,
    domicilio = domicilio,
    contacto = contacto,
    activo = activo,
    esAdmin = esAdmin,
)

internal fun InvitadoEntity.aDominio() = Invitado(
    id = id,
    nombre = nombre,
    residenteId = residenteId,
    notificacionesSilenciadas = notificacionesSilenciadas,
)

internal fun Invitado.aEntidad() = InvitadoEntity(
    id = id,
    nombre = nombre,
    residenteId = residenteId,
    notificacionesSilenciadas = notificacionesSilenciadas,
)

internal fun QrInvitadoEntity.aDominio() = QrInvitado(
    id = id,
    invitadoId = invitadoId,
    residenteId = residenteId,
    creadoEn = creadoEn,
    cancelado = cancelado,
)

internal fun QrInvitado.aEntidad() = QrInvitadoEntity(
    id = id,
    invitadoId = invitadoId,
    residenteId = residenteId,
    creadoEn = creadoEn,
    cancelado = cancelado,
)

internal fun RegistroAccesoEntity.aDominio() = RegistroAcceso(
    id = id,
    nombrePersona = nombrePersona,
    tipo = tipo,
    momento = momento,
    residenteId = residenteId,
    invitadoId = invitadoId,
)

internal fun RegistroAcceso.aEntidad() = RegistroAccesoEntity(
    id = id,
    nombrePersona = nombrePersona,
    tipo = tipo,
    momento = momento,
    residenteId = residenteId,
    invitadoId = invitadoId,
)

package victor.android.portly.ui.navegacion

/** Rutas de la app. Las pestañas de la barra inferior son las de [PESTANAS]. */
object Rutas {
    const val MI_QR = "mi_qr"
    const val INVITADOS = "invitados"
    const val CREAR_INVITADO = "invitados/crear"
    const val DETALLE_QR = "invitados/qr/{qrId}"
    const val HISTORIAL = "historial"
    const val AJUSTES = "ajustes"
    const val NOTIFICACIONES_INVITADOS = "ajustes/notificaciones"
    const val ADMIN = "admin"
    const val ADMIN_RESIDENTES = "admin/residentes"
    const val ADMIN_FORMULARIO = "admin/residentes/{residenteId}"
    const val ADMIN_REGISTRO = "admin/registro"
    const val ADMIN_QRS = "admin/qrs"

    fun detalleQr(qrId: Long) = "invitados/qr/$qrId"
    fun formularioResidente(residenteId: Long) = "admin/residentes/$residenteId"
}

data class Pestana(val ruta: String, val etiqueta: String)

/** Pestañas visibles de la barra inferior; Administración solo para administradores. */
fun pestanasVisibles(esAdmin: Boolean): List<Pestana> = buildList {
    add(Pestana(Rutas.MI_QR, "Mi QR"))
    add(Pestana(Rutas.INVITADOS, "Invitados"))
    add(Pestana(Rutas.HISTORIAL, "Historial"))
    add(Pestana(Rutas.AJUSTES, "Ajustes"))
    if (esAdmin) add(Pestana(Rutas.ADMIN, "Administración"))
}

# Portly: Plan de ejecución por etapas (App de acceso para privada, Android)

Este archivo es para ejecutarse en Claude Code, una etapa a la vez.

## Cómo usar este plan

Al iniciar, dile a Claude Code: "Lee PLAN.md y ejecuta la Etapa N".

**Regla de oro, aplica a TODAS las etapas:**

1. Implementar solo lo que dice la etapa, nada de etapas futuras.
2. Escribir los tests unitarios de lo que se construyó en esa etapa.
3. Correr los tests al terminar la etapa:
   ```
   ./gradlew testDebugUnitTest
   ```
4. Si algún test falla, corregir y volver a correr hasta que todos pasen (los de esta etapa y los de etapas anteriores).
5. Solo cuando todo esté en verde: marcar la etapa como completada en la sección "Avance" y detenerse a reportar qué se hizo y el resultado de los tests.
   - **No hacer commit automáticamente.** El commit solo se hace cuando el usuario lo pida explícitamente.
6. No avanzar a la siguiente etapa sin que el usuario lo pida.

Nunca marcar una etapa como completada con tests fallando u omitidos.

## Alcance y decisiones ya tomadas

- Nombre de la app: **Portly**. Usarlo como nombre visible de la app (`app_name`) y en las pantallas.
- Solo software. El hardware (plumas, escáner físico QR, instalación) lo resuelve otra persona.
- Por ahora no hay servidor: todo es local con Room. La capa de notificaciones queda preparada pero desactivada.
- Solo Android. Lenguaje Kotlin, interfaz con Jetpack Compose.
- Librerías: Navigation Compose, Room, ViewModel con Flow, Hilt, ZXing core (solo para generar QR), DataStore y Android Keystore.
- Roles: un residente puede ser además administrador (misma cuenta, permiso extra).
- El administrador agrega, edita y activa o desactiva residentes. Nunca se borran, para conservar el historial.
- QR del residente: abre la pluma para entrar y salir.
- QR de invitado: lo crea el residente con el nombre del invitado, queda ligado a él, abre la pluma para entrar y salir, dura solo el día de creación, sin límite de usos ese día.
- El invitado no tiene cuenta. El residente le manda el QR como imagen por WhatsApp, sin enlaces.
- Los invitados se guardan en Room para reutilizarlos. Cada invitado tiene su interruptor de notificaciones, y un interruptor general "Silenciar a todos los invitados" que solo aparece con 2 o más invitados. Si el general está activo, manda sobre los individuales; al apagarlo, cada invitado vuelve a su configuración.
- Cada entrada y salida queda en el registro con hora, persona y residente que invitó.

## Avance

- [x] Etapa 0: Proyecto base y herramientas de test
- [x] Etapa 1: Modelo de datos y repositorios (Room)
- [ ] Etapa 2: Login y sesión
- [ ] Etapa 3: Generador de QR y QR del residente
- [ ] Etapa 4: Invitados y sus QR
- [ ] Etapa 5: Historial de accesos
- [ ] Etapa 6: Ajustes de notificaciones
- [ ] Etapa 7: Administración
- [ ] Etapa 8: Navegación, pantallas y pulido final

---

## Etapa 0: Proyecto base y herramientas de test

**Objetivo:** proyecto Android que compila y corre tests.

**Tareas**
- Crear proyecto Kotlin con Jetpack Compose.
- Agregar dependencias: Navigation Compose, Room (con KSP), Hilt, DataStore, ZXing core.
- Agregar dependencias de test: JUnit, kotlinx-coroutines-test, Turbine (para probar Flow) y MockK.
- Estructura de paquetes por capas: `data`, `domain`, `ui`, `di`.
- Un test de ejemplo que pase, para confirmar que la tubería de tests funciona.

**Criterio de terminado:** `./gradlew testDebugUnitTest` corre y pasa.

---

## Etapa 1: Modelo de datos y repositorios (Room)

**Objetivo:** toda la persistencia local, sin interfaz todavía.

**Tareas**
- Entidades: `Residente` (nombre, domicilio, teléfono o correo, activo, esAdmin), `Invitado` (nombre, residenteId, notificacionesSilenciadas), `QrInvitado` (invitadoId, residenteId, fecha de creación, cancelado) y `RegistroAcceso` (quién, tipo entrada o salida, hora, residenteId, invitadoId opcional).
- DAOs con Flow para listas.
- Repositorios que exponen solo lo que la app necesita.
- Regla: nunca borrar residentes, solo activar o desactivar.

**Tests unitarios**
- Insertar, consultar y actualizar cada entidad (con Room en memoria).
- Desactivar un residente conserva su historial.
- Consultar registros por residente y por fecha.

---

## Etapa 2: Login y sesión

**Objetivo:** que un residente pueda iniciar sesión y mantenerse logueado.

**Tareas**
- Contraseñas guardadas con hash, nunca en texto plano.
- Sesión guardada con DataStore y protegida con Android Keystore.
- Un residente desactivado no puede iniciar sesión.
- Un residente con `esAdmin` queda identificado en la sesión.

**Tests unitarios**
- Login correcto e incorrecto.
- Residente desactivado es rechazado.
- La sesión persiste y se puede cerrar.
- El rol de administrador se reconoce correctamente.

---

## Etapa 3: Generador de QR y QR del residente

**Objetivo:** generar el QR personal del residente.

**Tareas**
- Una sola clase `GeneradorQr` que envuelva ZXing core y devuelva un Bitmap a partir de un texto. Es el único lugar que toca ZXing, para poder cambiar de librería fácilmente.
- Definir el formato del contenido del QR (tipo: residente o invitado, identificador, y lo necesario para validar).
- Decisión pendiente a confirmar con el usuario antes de implementar: si el QR del residente cambia cada cierto tiempo para que no se pueda copiar con una foto. Sin servidor, esto requiere un secreto compartido con la computadora de la caseta.

**Tests unitarios**
- El generador devuelve una imagen del tamaño pedido.
- El contenido codificado se puede decodificar de vuelta al mismo texto (ida y vuelta con ZXing).
- El formato del contenido se arma y se interpreta correctamente.

---

## Etapa 4: Invitados y sus QR

**Objetivo:** el flujo completo de invitados.

**Tareas**
- Crear un QR de invitado con nombre; si el invitado ya existe en la lista guardada, reutilizarlo; si no, guardarlo nuevo.
- El QR vale solo el día de creación (hasta las 23:59) y se puede usar varias veces ese día.
- Un QR vencido o cancelado no es válido.
- Listar los QR activos de hoy y cancelar uno.
- Preparar la acción de compartir el QR como imagen (no como enlace) para WhatsApp.
- Lógica pura de validación (`ValidadorQr`) que dice si un QR es válido en un momento dado. La conexión con la computadora de la caseta queda para cuando exista servidor.

**Tests unitarios**
- Crear invitado nuevo y reutilizar uno existente.
- Un QR es válido el mismo día y deja de serlo al día siguiente (probar el cambio de día con un reloj inyectado).
- Un QR cancelado no es válido.
- Varios usos el mismo día son válidos.
- Un QR de un residente desactivado no es válido.

---

## Etapa 5: Historial de accesos

**Objetivo:** que cada residente vea sus entradas y salidas, y las de sus invitados.

**Tareas**
- Registrar entradas y salidas con hora y a quién corresponden.
- Consulta del historial del residente (propio e invitados), ordenado del más reciente al más antiguo.
- Un residente solo ve su propio historial.

**Tests unitarios**
- Un registro nuevo aparece en el historial correcto.
- Un residente no puede ver registros de otro.
- El orden es cronológico inverso.
- Aparecen tanto accesos propios como de invitados.

---

## Etapa 6: Ajustes de notificaciones

**Objetivo:** interruptores listos, notificaciones desactivadas.

**Tareas**
- Interruptor por invitado.
- Interruptor general, visible solo si hay 2 o más invitados guardados.
- Regla: si el general está activo, manda sobre los individuales; al apagarlo, cada invitado vuelve a su ajuste anterior (no se pierden los individuales).
- Interfaz `NotificadorAccesos` con una implementación vacía que no hace nada. Cuando haya servidor se conecta FCM aquí.

**Tests unitarios**
- El interruptor general solo aparece con 2 o más invitados.
- Con el general activo, todos cuentan como silenciados.
- Al apagar el general se restauran los ajustes individuales.
- La implementación vacía no lanza errores y no envía nada.

---

## Etapa 7: Administración

**Objetivo:** funciones del rol administrador.

**Tareas**
- Agregar y editar residentes.
- Activar y desactivar residentes (nunca borrar).
- Consultar el registro completo de toda la privada, con filtros por fecha y por residente.
- Ver y cancelar QR de invitados activos de cualquier residente.
- Solo accesible para residentes con rol de administrador.
- Recomendación: no permitir desactivar al último administrador activo.

**Tests unitarios**
- Un residente sin rol admin no puede usar ninguna función de administración.
- Agregar, editar, activar y desactivar funciona y conserva el historial.
- No se puede quitar o desactivar al último administrador.
- Los filtros del registro devuelven los resultados correctos.

---

## Etapa 8: Navegación, pantallas y pulido final

**Objetivo:** unir todo en la interfaz.

**Mapa de navegación**
```
Login
  └── Inicio (barra inferior)
        ├── Mi QR
        ├── Invitados
        │     ├── Crear invitado
        │     └── Detalle de QR de invitado
        ├── Historial
        ├── Ajustes
        │     └── Notificaciones de invitados
        └── Administración (solo si tiene rol admin)
              ├── Residentes (agregar / editar)
              ├── Registro completo
              └── QRs de invitados activos
```

**Tareas**
- Pantallas en Compose según el mapa, usando ViewModel con Flow.
- La pestaña Administración solo se muestra a administradores.
- Pantalla de detalle del QR con "Compartir por WhatsApp" (imagen) y "Cancelar QR".

**Tests unitarios**
- Tests de los ViewModel: estado inicial, acciones y errores.
- La visibilidad de Administración depende del rol.
- Correr la suite completa y confirmar que todo sigue en verde.

---

## Pendientes para cuando haya servidor (fuera de este plan)

- Validación del QR en la computadora de la caseta y respuesta de abrir o negar.
- Envío real de notificaciones "X entró" y "X salió" (FCM).
- Respaldo de la lista de invitados, que hoy se pierde al cambiar de teléfono.
- Definir quién será el administrador inicial.

# Portly

App Android para controlar el acceso a una privada: los residentes generan su QR para entrar y salir, y pueden crear QR temporales para sus invitados.

> Estado: funciones de la **etapa 8** completas (navegación y pantallas). Las funciones que dependen de un servidor siguen pendientes (ver abajo).

## Capturas

| Login | Mi QR | Invitados |
|:-:|:-:|:-:|
| ![Login](docs/capturas/01-login.png) | ![Mi QR](docs/capturas/02-mi-qr.png) | ![Invitados](docs/capturas/03-invitados.png) |

| Crear invitado | QR de invitado | Historial |
|:-:|:-:|:-:|
| ![Crear invitado](docs/capturas/04-crear-invitado.png) | ![QR de invitado](docs/capturas/05-qr-invitado.png) | ![Historial](docs/capturas/06-historial.png) |

| Ajustes | Notificaciones de invitados | Administración |
|:-:|:-:|:-:|
| ![Ajustes](docs/capturas/07-ajustes.png) | ![Notificaciones](docs/capturas/08-notificaciones-invitados.png) | ![Administración](docs/capturas/09-administracion.png) |

| Residentes | Registro completo | QRs de invitados activos |
|:-:|:-:|:-:|
| ![Residentes](docs/capturas/10-residentes.png) | ![Registro](docs/capturas/11-registro-completo.png) | ![QRs activos](docs/capturas/12-qrs-activos.png) |

## Funciones

- **Login** con contacto (teléfono o correo) y contraseña. La contraseña se guarda con PBKDF2; la sesión se conserva en DataStore.
- **Mi QR:** el código del residente para entrar y salir.
- **Invitados:** el residente crea un invitado con su nombre y genera su QR. El QR solo vale el día en que se creó, sin límite de usos ese día. Se puede compartir la imagen por WhatsApp o cancelar.
- **Historial:** cada entrada y salida con hora, persona y residente que invitó.
- **Ajustes:** notificaciones por invitado y un interruptor general "Silenciar a todos los invitados" (aparece con 2 o más invitados y manda sobre los individuales).
- **Administración** (solo para administradores): agregar, editar, activar o desactivar residentes; ver el registro completo con filtro por residente; y cancelar QR de invitados activos. Un residente nunca se borra, para conservar el historial.
- **Idiomas:** español (por defecto) e inglés, según el idioma del teléfono.

## Tecnología

- Kotlin, Jetpack Compose y Material 3
- Navigation Compose, ViewModel con Flow y Hilt
- Room para la base de datos local
- DataStore y Android Keystore para la sesión y los secretos
- ZXing para generar los QR
- Android 8.0 (API 26) o superior

## Arquitectura

```
ui/           pantallas Compose y ViewModel (una pantalla, un ViewModel)
domain/       reglas de negocio y modelos (sin Android)
data/         Room, DataStore, seguridad, generación de QR
di/           módulos de Hilt
```

Los servicios de `domain/` (autenticación, QR, historial, notificaciones, administración) reciben sus dependencias por interfaz, así que se prueban en la JVM con fakes y MockK.

## Compilar y probar

```bash
./gradlew testDebugUnitTest   # tests unitarios
./gradlew installDebug        # instalar en un dispositivo conectado
```

## Pendiente (requiere servidor)

- Validación del QR en la computadora de la caseta.
- Notificaciones reales "X entró" y "X salió" (FCM).
- Respaldo de la lista de invitados, que hoy se pierde al cambiar de teléfono.
- Definir el administrador inicial.

## Plan de desarrollo

El plan por etapas está en [`app/PLAN.md`](app/PLAN.md).

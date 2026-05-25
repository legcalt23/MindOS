# Security Remediation Notes

- Fecha: 2026-05-15
- Riesgo cerrado: retirada del acceso `DevLogin` con credenciales embebidas del cliente KMP/Firebase.

## Cuentas que requieren rotacion

- `p*****w@neu.local` era un identificador de preview hardcodeado en codigo local y ya no se expone en el cliente.
- Cuentas reales usadas por `DevLogin`: no recuperables desde el working tree actual. Deben identificarse desde historial Git, exports de Firebase Auth o registros operativos internos antes de completar la contencion.

## Acciones obligatorias en Firebase Auth

- Revocar sesiones activas y refresh tokens de todas las cuentas que hayan estado asociadas a `DevLogin`.
- Forzar rotacion de credenciales de las cuentas afectadas.
- Revisar metadatos de inicio de sesion, direcciones IP, dispositivos y cambios de password para detectar abuso historico.

## Alcance de la remediacion en codigo

- Eliminadas referencias activas a cuentas hardcodeadas en el cliente actual.
- No se automatizan cambios destructivos contra Firebase porque el repositorio no contiene tooling Admin claro y seguro para ello.

## MEDIO-01: eliminacion de posts propios

- Fecha: 2026-05-18
- Riesgo cerrado en reglas: `posts/{postId}` y `user_posts/{userId}/posts/{postId}` permiten `delete` solo si el usuario autenticado coincide con `resource.data.userId`.
- `update` sigue bloqueado para posts; no hay cambio de ownership, soft-delete cliente ni edicion directa desde reglas.
- Campo autor real: `userId`.
- Adjuntos: Storage usa `post_attachments/{userId}/{postId}/{attachmentId}`. Este repositorio no contiene codigo de Cloud Functions desplegable; queda pendiente operativo crear un trigger backend `onDocumentDeleted("posts/{postId}")` que lea `event.data.data().userId` y borre todos los objetos bajo ese prefijo, ademas de purgar copias materializadas en `feeds/{viewerId}/posts/{postId}` si se usan en produccion.

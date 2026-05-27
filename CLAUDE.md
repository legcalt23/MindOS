# CLAUDE.md

## Regla núcleo: bucle de mejora automática del proyecto

Cada vez que corrijas un error, bug, fallo de compilación, fallo de arquitectura, fallo de integración, fallo de imports, fallo de dependencias, fallo de tipos, fallo de UI, fallo de estado, fallo de navegación, fallo de permisos, fallo de Firebase, fallo de Gradle, fallo de Kotlin, fallo de Compose o cualquier problema causado por una mala suposición previa, debes actualizar este archivo.

Tu obligación es convertir cada error corregido en una regla útil para evitar que vuelva a ocurrir.

---

## Protocolo obligatorio después de corregir un error

Después de resolver cualquier error real del proyecto, añade una entrada nueva en la sección:

`## Aprendizajes acumulados del proyecto`

La entrada debe tener este formato exacto:

```md
### YYYY-MM-DD — [Título corto del error]

**Error detectado:**
Describe el error concreto que ocurrió.

**Causa raíz:**
Explica por qué ocurrió realmente. No describas síntomas. Identifica la causa técnica.

**Solución aplicada:**
Describe qué cambio solucionó el problema.

**Regla futura:**
Define una regla clara que deberás seguir en futuras modificaciones para no repetir este error.

**Archivos afectados:**
- `ruta/del/archivo.ext`
```

---

## Criterios para actualizar este archivo

Debes actualizar este archivo cuando ocurra cualquiera de estos casos:

1. Has corregido un error de compilación.
2. Has corregido un crash.
3. Has corregido un bug visual.
4. Has corregido una navegación rota.
5. Has corregido una integración mal conectada.
6. Has corregido imports incorrectos.
7. Has corregido un problema de Gradle, Kotlin, Compose, Firebase o permisos.
8. Has detectado que una decisión anterior era incorrecta.
9. Has descubierto una convención específica de este proyecto.
10. Has aprendido una restricción técnica que debe recordarse.

No actualices este archivo por cambios triviales de texto, estilos menores o modificaciones que no enseñen una regla reutilizable.

---

## Reglas para escribir aprendizajes

Los aprendizajes deben ser:

- Concretos.
- Técnicos.
- Reutilizables.
- Breves.
- Basados en errores reales corregidos.
- Escritos para evitar repetir el mismo fallo en el futuro.

No escribas reflexiones largas. No escribas teoría genérica. No escribas documentación innecesaria. No inventes problemas. No añadas reglas si no se ha corregido un error real.

---

## Uso obligatorio antes de modificar código

Antes de hacer cualquier cambio en el código, revisa la sección `Aprendizajes acumulados del proyecto`.

Debes aplicar esas reglas como restricciones activas durante la edición.

Si una regla previa entra en conflicto con una instrucción nueva del usuario, sigue la instrucción nueva del usuario, pero conserva la regla anterior salvo que haya quedado obsoleta.

Si una regla queda obsoleta, no la borres: añade una nota debajo indicando que ha sido reemplazada y explica por qué.

---

## Referencia rápida — errores que obligan a actualizar

- compilación
- crash
- navegación
- imports
- dependencias
- Gradle
- Kotlin
- Compose
- Firebase
- permisos
- arquitectura
- estado
- integración
- UI rota
- suposición incorrecta del agente

No añadas teoría. No añadas reglas genéricas. No inventes aprendizajes. No borres aprendizajes antiguos salvo que el usuario lo pida.

Actualiza este archivo solo si el error corregido enseña una regla reutilizable.

---

## Aprendizajes acumulados del proyecto

<!-- Aprendizajes añadidos automáticamente por el agente tras corregir errores reales. -->

### 2026-05-26 — Queries `list` sin `request.query.limit` permiten amplificación de coste

**Error detectado:**
Cuatro reglas `allow list` en `firestore.rules` no enforzaban `request.query.limit`:
`dm_chats`, `dm_chats/.../messages`, `user_posts/{uid}/posts`, `feeds/{viewerId}/posts`.
Un atacante (incluso autorizado, con app modificada o token comprometido) podía leer toda la colección de una sentada, amplificando coste de lectura en Firestore y abusando del presupuesto del proyecto.

**Causa raíz:**
Al diseñar reglas para colecciones donde el acceso ya está restringido (participantes, owner), se asumió que la restricción de acceso era suficiente. La autorización limita *quién* lee, no *cuánto* lee. Sin `request.query.limit`, la app cliente — o un atacante que se hace pasar por ella — puede solicitar la colección completa.

**Solución aplicada:**
Añadido `request.query.limit != null && request.query.limit <= N` a las cuatro reglas. Límites:
- `dm_chats`: 50
- `dm_chats/.../messages`: 100
- `user_posts/{uid}/posts`: 100
- `feeds/{viewerId}/posts`: 100

**Regla futura:**
TODA regla `allow list` debe incluir validación de `request.query.limit` independientemente de quién pueda leer. La autorización (quién) y el límite (cuánto) son controles ortogonales — ambos son necesarios.

Plantilla mínima para cualquier nueva `allow list`:
```
allow list: if request.query.limit != null &&
            request.query.limit <= MAX_LIMIT &&
            <condiciones de autorización>;
```

**Archivos afectados:**
- `firestore.rules` (líneas ~1610, ~1627, ~1639, ~1648)
- `firestore.security.rules.test.mjs` (tests T49-T52)

### 2026-05-26 — App Check no enforcado en uploads y lecturas de `dm_media`

**Error detectado:**
En `storage.rules`, las funciones `canCreateDMMedia` y `canAccessDMResource` solo validaban `signedIn()` pero NO llamaban a `isAppChecked()`. En cambio, `profile_visuals` y `post_attachments` privados sí lo enforzaban a través de `isOwner()` → `isAppChecked()`.

Un cliente modificado (APK repackaged, app sin App Check) podía subir y descargar archivos de DM saltándose la attestation del dispositivo. La consistencia de seguridad entre paths Storage estaba rota.

**Causa raíz:**
`dm_media` se diseñó con su propio sistema de validación de metadata (participantA, participantB, chatId, ownerUid) y se asumió que la validación de metadata era suficiente. Pero App Check es un control ortogonal al de autorización — uno valida el dispositivo, el otro al usuario. Ambos son necesarios.

**Solución aplicada:**
Añadido `isAppChecked()` a ambas funciones:
```
function canAccessDMResource(chatId) {
  return signedIn() && isAppChecked() && ...
}
function canCreateDMMedia(chatId) {
  return signedIn() && isAppChecked() && ...
}
```

**Regla futura:**
TODA operación de Firebase Storage que escriba o lea contenido privado debe enforzar App Check. La regla mínima:
```
function canDoX() {
  return signedIn() && isAppChecked() && <validación de autorización>;
}
```

Excepción documentada: lectura de contenido público (visibility=public) puede no requerir App Check si la app sirve estos archivos como CDN público. Pero contenido privado SIEMPRE debe enforzarlo.

Antes de añadir un nuevo `match` block en `storage.rules`, verificar que la función de autorización llamada incluya `isAppChecked()`.

**Archivos afectados:**
- `storage.rules` (funciones `canAccessDMResource` y `canCreateDMMedia`)
- `docs/security/SELF_AUDIT_BRUTAL.md` (hallazgos S01 y S02)

### 2026-05-27 — Build cache corrupto por referencia a proyecto externo (NEUv3)

**Error detectado:**
`./gradlew :androidApp:assembleDebug` fallaba con `mergeLibDexDebug FAILED` porque Gradle intentaba dexificar un `.class` en `C:\Users\curro\AndroidStudioProjects\NEUv3\shared\build\...`, que está fuera del directorio raíz de MindOS.

**Causa raíz:**
El cache de build intermedio de Gradle contenía una referencia antigua a un proyecto previo (`NEUv3`) del que MindOS fue probablemente renombrado o copiado. El directorio `shared/build/intermediates/runtime_library_classes_dir/` tenía artefactos con rutas absolutas al proyecto anterior.

**Solución aplicada:**
`./gradlew clean` seguido de rebuild. El clean elimina los artefactos intermedios y el rebuild regenera todo con las rutas correctas.

**Regla futura:**
Cuando un build falle con rutas absolutas que apuntan a un directorio fuera del proyecto, ejecutar `./gradlew clean` antes de investigar más. Es un síntoma de cache corrupto, no de un error en el código.

**Archivos afectados:**
- `shared/build/` (directorio de build, limpiado)

### 2026-05-27 — Design tokens dispersos por feature en vez de centralizados

**Error detectado:**
275 literales `Color(0xFF...)` dispersos en 30 archivos. Tokens locales duplicados en `SearchVisualTokens.kt` y `ViewerDesign.kt` con los mismos colores (`#13ECA4`, `#F4FFF9`, `#020504`) definidos independientemente.

**Causa raíz:**
Cada feature creó sus propios tokens de color de forma aislada. No existía un design system centralizado. Los valores eran correctos (mismos colores) pero no tenían una fuente de verdad única.

**Solución aplicada:**
Creados archivos centralizados en `shared/src/commonMain/kotlin/com/example/mindos/design/`:
- `AppColors.kt` — tokens de color (brand, neutral, semantic, state)
- `AppSpacing.kt` — tokens de espaciado (escala 4dp)
- `AppShapes.kt` — tokens de radio/forma
- `AppElevation.kt` — tokens de elevación
- `AppAnimation.kt` — tokens de duración de animación
- `AppStrings.kt` — constantes de strings de UI
- `components/StateComponents.kt` — componentes compartidos (Loading, Error, Empty, Offline)

**Regla futura:**
NUNCA crear tokens visuales locales a un feature. Todos los colores, espaciados, radios, elevaciones y strings deben definirse en los archivos centralizados del paquete `design/`. Si un feature necesita un valor que no existe, añadirlo al archivo central correspondiente.

Antes de añadir un `Color(0xFF...)` en cualquier archivo, verificar que el color no exista ya en `AppColors.kt`. Si existe, usar la referencia. Si no existe, añadirlo a `AppColors.kt` primero.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppSpacing.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppShapes.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppElevation.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppAnimation.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppStrings.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/components/StateComponents.kt` (NUEVO)

### 2026-05-27 — applicationId no debe ser com.example.* en producción

**Error detectado:**
El `applicationId` era `com.example.mindos`, que es un ID de ejemplo rechazado por Google Play Store y que colisiona con otros proyectos ejemplo.

**Causa raíz:**
El template de proyecto de Android Studio genera `com.example.*` como placeholder. No se cambió al configurar el proyecto para producción.

**Solución aplicada:**
Cambiado `applicationId` a `com.mindos.app` en `androidApp/build.gradle.kts`. Actualizado `google-services.json` para que el `package_name` coincida. El `namespace` se mantiene como `com.example.mindos` (solo afecta a R class generation, no a identidad en Play Store).

**Regla futura:**
El `applicationId` y el `namespace` son conceptos independientes en AGP. El `applicationId` identifica la app en la tienda y en Firebase. El `namespace` genera la clase R y no necesita coincidir. Al cambiar `applicationId`, SIEMPRE actualizar `google-services.json` y registrar el nuevo ID en Firebase Console.

**Archivos afectados:**
- `androidApp/build.gradle.kts`
- `androidApp/google-services.json`

### 2026-05-27 — Signing config debe usar variables de entorno, nunca hardcoded

**Error detectado:**
No existía configuración de signing para release builds. Sin signing, no se puede publicar en Play Store ni distribuir APKs de release.

**Causa raíz:**
No se configuró signing al crear el proyecto. Es un paso que se omite frecuentemente hasta que se necesita publicar.

**Solución aplicada:**
Añadido `signingConfigs.create("release")` que lee credenciales de `gradle.properties` vía `findProperty()`. Las propiedades son:
- `MINDOS_KEYSTORE_FILE` — ruta al .jks
- `MINDOS_KEYSTORE_PASSWORD`
- `MINDOS_KEY_ALIAS` (default: "mindos")
- `MINDOS_KEY_PASSWORD`

Si el keystore no existe, el release build cae al debug signing config. Esto permite builds en CI sin keystore (para validación) y builds firmados cuando las credenciales están disponibles.

**Regla futura:**
NUNCA incluir rutas de keystore ni contraseñas en archivos versionados. Siempre usar `findProperty()` o variables de entorno. El fallback a debug signing permite que CI compile release sin fallar.

**Archivos afectados:**
- `androidApp/build.gradle.kts`

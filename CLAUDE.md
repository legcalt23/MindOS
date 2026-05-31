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
Cambiado `applicationId` a `com.neu.app` en `androidApp/build.gradle.kts`. Actualizado `google-services.json` para que el `package_name` coincida. El `namespace` se mantiene como `com.example.mindos` (solo afecta a R class generation, no a identidad en Play Store).

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
- `MINDOS_KEY_ALIAS` (default: "neu")
- `MINDOS_KEY_PASSWORD`

Si el keystore no existe, el release build cae al debug signing config. Esto permite builds en CI sin keystore (para validación) y builds firmados cuando las credenciales están disponibles.

**Regla futura:**
NUNCA incluir rutas de keystore ni contraseñas en archivos versionados. Siempre usar `findProperty()` o variables de entorno. El fallback a debug signing permite que CI compile release sin fallar.

**Archivos afectados:**
- `androidApp/build.gradle.kts`

### 2026-05-28 — Mensajes de error de auth revelan si un email existe (account enumeration)

**Error detectado:**
`AuthDiagnostics.toUserMessage()` devolvía mensajes distintos para `UserNotFound` ("No existe ningun usuario") y `WrongPassword` ("Contrasena incorrecta"). Un atacante podía distinguir si un email está registrado probando contraseñas incorrectas.

**Causa raíz:**
Se mapearon los códigos de error de Firebase directamente a mensajes de usuario sin considerar que la diferencia entre "no existe" y "contraseña incorrecta" filtra información.

**Solución aplicada:**
Unificados `UserNotFound`, `WrongPassword`, `InvalidLoginCredentials` y `MalformedCredential` bajo un único mensaje genérico: "Email o contrasena incorrectos." Eliminada la interpolación de `$email` en mensajes de credenciales inválidas.

**Regla futura:**
Los mensajes de error de autenticación NUNCA deben revelar si un email está registrado. Todos los fallos de credenciales (usuario no encontrado, contraseña incorrecta, credenciales inválidas) deben usar el mismo mensaje genérico.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/AuthDiagnostics.kt`

### 2026-05-28 — Android sin network_security_config (cleartext permitido)

**Error detectado:**
No existía `network_security_config.xml`. Sin esta configuración, Android permite tráfico cleartext (HTTP) en versiones anteriores a API 28, y no se declara explícitamente la política de seguridad de red.

**Causa raíz:**
Omisión al crear el proyecto. Android Studio no genera esta configuración por defecto.

**Solución aplicada:**
Creado `androidApp/src/main/res/xml/network_security_config.xml` con `cleartextTrafficPermitted="false"` y trust-anchors limitados a certificados del sistema. Referenciado en `AndroidManifest.xml` vía `android:networkSecurityConfig`.

**Regla futura:**
Todo proyecto Android que haga networking DEBE tener un `network_security_config.xml` que prohíba cleartext explícitamente. Si se necesita una excepción para un dominio de desarrollo, usar `<domain-config>` específico, nunca habilitar cleartext globalmente.

**Archivos afectados:**
- `androidApp/src/main/res/xml/network_security_config.xml` (NUEVO)
- `androidApp/src/main/AndroidManifest.xml`

### 2026-05-28 — updateMessage acepta campos arbitrarios sin whitelist

**Error detectado:**
`FirebaseDMRepository.updateMessage()` aceptaba un `Map<String, Any?>` y escribía todos los campos a Firestore sin validación. Un caller malintencionado o un bug podrían sobrescribir campos protegidos como `senderId` o `createdAt`.

**Causa raíz:**
Se diseñó `updateMessage` como una operación genérica flexible sin considerar que los campos actualizables deben estar restringidos por la capa de dominio.

**Solución aplicada:**
Añadida whitelist `AllowedMessageUpdateFields = setOf("status", "attachmentUrl", "thumbnailUrl")` en el companion object. `updateMessage` valida con `require` que todos los campos estén en la whitelist antes de escribir.

**Regla futura:**
Toda operación de update a Firestore debe validar los campos contra una whitelist explícita. Nunca pasar `Map<String, Any?>` sin restricciones a operaciones de escritura.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-28 — Faltaba implementación actual de WaveMailboxIcon para iOS

**Error detectado:**
`WaveMailboxIcon` tenía `expect` en commonMain y `actual` en androidMain y desktopMain, pero no en iosMain. Cualquier build iOS fallaría por falta de `actual`.

**Causa raíz:**
Al crear el composable expect/actual, se omitió la implementación iOS. Como no hay módulo iosApp que compile el target, el error no se detectó.

**Solución aplicada:**
Creado `WaveMailboxIcon.ios.kt` en iosMain con implementación usando `Icons.Default.MailOutline` como fallback (consistente con el fallback del actual de Android).

**Regla futura:**
Cada `expect` declaration DEBE tener `actual` en TODOS los targets declarados en `build.gradle.kts`, incluyendo los que aún no se compilan regularmente. Al crear un nuevo expect/actual, verificar que existan actuals en: androidMain, desktopMain, iosMain.

**Archivos afectados:**
- `shared/src/iosMain/kotlin/com/example/mindos/features/waves/presentation/WaveMailboxIcon.ios.kt` (NUEVO)

### 2026-05-28 — Unsafe smart casts en LaunchedEffect de App.kt (race condition)

**Error detectado:**
El bloque `LaunchedEffect(sessionRepository)` en `App.kt` usaba `(route as AppRoute.X)` repetidamente para acceder a propiedades de la ruta actual. Como `route` es una variable mutable, podía cambiar entre dos accesos consecutivos a `route as`, causando un `ClassCastException` en runtime.

**Causa raíz:**
Se usó la variable mutable `route` directamente dentro del `when` y después se hizo cast sobre ella de nuevo fuera del `when`. El compilador no puede garantizar que el valor no haya cambiado entre la evaluación del `when` y el uso posterior.

**Solución aplicada:**
Capturar `route` en una variable local `val currentRoute = route` al inicio del `collect`, y usar `currentRoute` en todas las ramas del `when`. Kotlin smart-cast funciona correctamente con variables locales inmutables.

**Regla futura:**
Antes de hacer smart cast sobre una propiedad mutable (var, MutableState, etc.), SIEMPRE capturarla en un `val` local. Nunca hacer cast directo sobre `route` o cualquier estado mutable compartido.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`

### 2026-05-28 — Backup rules no excluían cache de Firebase ni tokens

**Error detectado:**
`backup_rules.xml` y `data_extraction_rules.xml` solo excluían `sharedpref` y `dm_audio/`. Los directorios de Firebase (databases, crashlytics NDK, installation tokens) se incluían en backups, exponiendo potencialmente tokens de sesión y datos de cache.

**Causa raíz:**
Al configurar las reglas de backup se pensó solo en datos de la app, no en los directorios internos que Firebase crea automáticamente.

**Solución aplicada:**
Añadidos exclusiones para `database` (dominio completo), `com.google.firebase.crashlytics-ndk`, `generate_fid.lock`, y `PersistedInstallation` en ambos archivos XML.

**Regla futura:**
Las reglas de backup DEBEN excluir todos los directorios internos de Firebase y tokens de sesión. Al añadir un nuevo SDK de Firebase, verificar si crea directorios locales que necesiten exclusión.

**Archivos afectados:**
- `androidApp/src/main/res/xml/backup_rules.xml`
- `androidApp/src/main/res/xml/data_extraction_rules.xml`

### 2026-05-28 — Sin Crashlytics: crashes en producción no se capturaban

**Error detectado:**
No había integración de Firebase Crashlytics. Cualquier crash en producción se perdía sin registro centralizado.

**Causa raíz:**
Omisión al configurar Firebase. Se integró Auth, Firestore, Storage y App Check, pero no Crashlytics.

**Solución aplicada:**
Añadido `firebase-crashlytics` y `firebase-analytics` al BOM en `androidApp/build.gradle.kts`. Añadido plugin `com.google.firebase.crashlytics` v3.0.4. Analytics es necesario como dependencia de Crashlytics.

**Regla futura:**
TODO proyecto Firebase en producción DEBE tener Crashlytics habilitado. Al añadir Firebase a un proyecto, incluir Crashlytics desde el inicio, no como paso posterior.

**Archivos afectados:**
- `gradle/libs.versions.toml`
- `build.gradle.kts` (root)
- `androidApp/build.gradle.kts`

### 2026-05-28 — Shared module no puede depender directamente de Crashlytics SDK

**Error detectado:**
Se intentó añadir `FirebaseCrashlytics.getInstance().recordException(error)` directamente en `SafeDebugLogging.android.kt` (que vive en `:shared`). Falló con "Unresolved reference 'crashlytics'" porque el SDK de Crashlytics solo es dependencia de `:androidApp`, no de `:shared`.

**Causa raíz:**
El módulo `:shared` es KMP y sus dependencias deben ser multiplataforma o estar disponibles en todos los targets. El Crashlytics SDK es Android-only y vive en `:androidApp`. Referenciar clases de `:androidApp` desde `:shared` invierte la dirección de dependencias.

**Solución aplicada:**
Creado `NonFatalReporter` (singleton con callback nullable) en `SafeDebugLogging.kt` (commonMain). `safeErrorLog()` invoca el callback si está registrado. `MindOSApplication.kt` registra Crashlytics como handler solo en release: `NonFatalReporter.handler = { crashlytics.recordException(it) }`.

**Regla futura:**
El módulo `:shared` NUNCA debe depender de SDKs Android-only. Para integrar funcionalidad de `:androidApp` en código compartido, usar el patrón callback/interface: definir la abstracción en commonMain, registrar la implementación concreta desde la capa de aplicación en el `onCreate()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/SafeDebugLogging.kt`
- `androidApp/src/main/java/com/example/mindos/MindOSApplication.kt`

### 2026-05-28 — iOS GoogleService-Info.plist apuntaba a proyecto Firebase incorrecto

**Error detectado:**
`GoogleService-Info.plist` contenía `PROJECT_ID=mindos-4aef3` y `BUNDLE_ID=com.example.mindos`, pero el proyecto de producción es `mindos-8452f` y el bundle ID correcto es `com.neu.app`. Solo la app Android estaba registrada en el proyecto correcto.

**Causa raíz:**
Al crear el proyecto se generó un plist desde un proyecto Firebase de desarrollo/test (`mindos-4aef3`). Cuando se migró Android al proyecto de producción (`mindos-8452f`), el plist iOS no se actualizó.

**Solución aplicada:**
Registrada app iOS en Firebase project `mindos-8452f` con bundle `com.neu.app` vía Firebase API. Reemplazado `GoogleService-Info.plist` con el generado por el proyecto correcto.

**Regla futura:**
Todos los archivos de configuración de Firebase (google-services.json y GoogleService-Info.plist) deben apuntar al MISMO `PROJECT_ID`. Al cambiar de proyecto Firebase, actualizar AMBOS archivos. Verificar que `BUNDLE_ID` coincida con `applicationId`.

**Archivos afectados:**
- `shared/src/iosMain/resources/GoogleService-Info.plist`

### 2026-05-28 — Operaciones Firestore sin timeout (bloqueo indefinido)

**Error detectado:**
Ninguna llamada a Firestore (`.get()`, `.runTransaction()`, `.updateFields()`) tenía timeout. Una operación podía bloquearse indefinidamente si el dispositivo estaba offline sin cache disponible.

**Causa raíz:**
Se dependió del timeout implícito del SDK de Firebase, que es muy largo o inexistente para ciertas operaciones. No se implementó una capa de timeout a nivel de aplicación.

**Solución aplicada:**
Creado `FirestoreDefaults.kt` con tiempos estándar (ReadTimeout=15s, WriteTimeout=20s, BatchTimeout=30s) y funciones `withFirestoreReadTimeout`/`withFirestoreWriteTimeout`. Aplicados en `FirebaseUsernameSearchRepository` y `FirebaseDMRepository`.

**Regla futura:**
Toda operación Firestore suspendible (reads, writes, transactions) debe ejecutarse dentro de `withFirestoreReadTimeout` o `withFirestoreWriteTimeout`. Los Flows (snapshots) no necesitan timeout porque son streams continuos con su propio manejo de errores.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/features/search/data/FirebaseUsernameSearchRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-28 — Transacciones multi-documento necesitan BatchTimeout, no WriteTimeout

**Error detectado:**
Al envolver transacciones de Firestore que escriben a 3+ documentos (e.g. `observe` en `FirebaseSocialInteractionRepository` que toca observationRef + observedProfileRef + notificationRef), usar `WriteTimeout=20s` por defecto puede ser demasiado corto en redes lentas. Las transacciones son retried automáticamente por Firestore si hay contention.

**Causa raíz:**
`withFirestoreWriteTimeout` usa `FirestoreDefaults.WriteTimeout` (20s) como default. Las transacciones que tocan múltiples documentos pueden necesitar más tiempo debido a retries internos del SDK por contention optimista.

**Solución aplicada:**
Para transacciones multi-doc, pasar `FirestoreDefaults.BatchTimeout` (30s) explícitamente:
```kotlin
withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {
    firestore.runTransaction { /* multi-doc operations */ }
}
```

**Regla futura:**
Las transacciones single-doc usan `withFirestoreWriteTimeout {}` (default 20s). Las transacciones que tocan 3+ documentos o que hacen reads previos antes de writes deben usar `withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {}` (30s) para tolerar retries por contention.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/FirebaseSocialInteractionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/ProfileVisualRepositories.kt`

### 2026-05-28 — Lógica de navegación inline en App.kt es difícil de testear

**Error detectado:**
`App.kt` tenía un bloque `when` de 24 líneas dentro de `LaunchedEffect` para rebindear el `AuthenticatedUser` actual a la ruta activa cuando el `authState` emitía. Estaba inline en un composable, no tenía tests, y duplicaba lógica de construcción de cada `data class` de `AppRoute`.

**Causa raíz:**
Cuando se diseñó la observación de `authState`, la lógica de rebinding se metió directamente en el composable como un detalle de implementación. No se extrajo a una función pura porque "es navegación, va aquí". Resultado: regresiones en el rebinding (e.g. omitir un campo de un `DMChatUserInfo`) solo se detectaban en runtime.

**Solución aplicada:**
Extraído a `AppRoute.rebindWithUser(user): AppRoute` en `core/navigation/AppRouteRebinder.kt` (commonMain). Tests exhaustivos en `AppRouteRebinderTest` (14 tests cubriendo todas las variantes de `AppRoute`). `App.kt` ahora llama a `currentRoute.rebindWithUser(user)`.

**Regla futura:**
Toda lógica de transformación de rutas (rebinding por auth, mapeo de parent route para back navigation, etc.) debe extraerse a una extensión pura de `AppRoute` en `core/navigation/`. Si la lógica está dentro de un composable y depende solo de `AppRoute` + datos primitivos, NO pertenece al composable — pertenece a una función testeable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteRebinder.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteRebinderTest.kt` (NUEVO)

### 2026-05-28 — Navegación sin back stack: botón back físico de Android no funcionaba

**Error detectado:**
`App.kt` usaba `var route by mutableStateOf()` como única fuente de verdad de la ruta actual. No había historial. El botón físico/gesture back de Android no estaba conectado a nada de la app — invocaba el comportamiento por defecto (cerrar la activity), atrapando al usuario en pantallas profundas (DMCajonera, MediaViewer) sin forma de salir excepto cerrar la app.

**Causa raíz:**
La navegación se diseñó como un simple state holder en lugar de una pila. Cada `onBack` reconstruía la ruta destino manualmente con `route = AppRoute.X(currentRoute.user)`, lo cual:
1. Duplicaba lógica en cada pantalla.
2. No respondía al back hardware.
3. Perdía el contexto previo (e.g. MediaViewer abierto desde DMChat volvía a Main).

**Solución aplicada:**
1. `AppRouteStack` (commonMain) — clase observable con `push/pop/replace/replaceAll/popTo/rebindStackWithUser`. Respaldada por `SnapshotStateList` para recomposición automática.
2. `PlatformBackHandler(enabled, onBack)` — expect/actual: Android usa `androidx.activity.compose.BackHandler`; desktop/iOS son no-op.
3. Refactorizado `App.kt` — cada `onBack = { stack.pop() }`, cada navegación profunda usa `stack.push()`, sign-out usa `stack.replaceAll(AppRoute.Auth)`.
4. Tests: 12 tests en `AppRouteStackTest` cubren todas las operaciones del stack.

**Regla futura:**
NUNCA usar `var route by mutableStateOf()` directamente para navegación. Toda app con más de 2 pantallas debe usar un back stack explícito. Reglas:
- `push()` cuando vas más profundo (Main → DMList).
- `replace()` cuando swap al mismo nivel (post-login routing).
- `replaceAll()` cuando reset total (sign-out, deep link entry).
- `pop()` para back.
- Wirear SIEMPRE `PlatformBackHandler(enabled = stack.canGoBack, onBack = { stack.pop() })` cuando hay un stack activo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteStack.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.kt` (NUEVO expect)
- `shared/src/androidMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.android.kt` (NUEVO actual)
- `shared/src/desktopMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.desktop.kt` (NUEVO actual)
- `shared/src/iosMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.ios.kt` (NUEVO actual)
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteStackTest.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (refactorizado completo)

### 2026-05-28 — POST_NOTIFICATIONS no declarado (Android 13+ no podía notificar)

**Error detectado:**
`AndroidManifest.xml` no declaraba `android.permission.POST_NOTIFICATIONS`. Con `targetSdk=36`, cualquier intento de mostrar una notificación local o FCM en Android 13+ (API 33+) sería ignorado silenciosamente — el SDK requiere runtime permission desde Android 13.

**Causa raíz:**
Omisión al configurar el manifest. POST_NOTIFICATIONS no existía pre-Android 13 (los install-time grants cubrían todo), así que en proyectos creados antes era invisible.

**Solución aplicada:**
1. Añadido `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` al manifest.
2. Creado `NotificationPermissionState` (commonMain) — modelo puro con reglas de gating (`shouldShowPrompt`, `shouldDirectToSettings`).
3. Creado `rememberNotificationPermissionHandle()` (androidMain) — wrap de `ActivityResultContracts.RequestPermission` con detección de API 33+.
4. Tests: 9 tests cubriendo todas las combinaciones (pre-13, granted, denied, dismissed, permanent denial).

**Regla futura:**
Todo `targetSdk >= 33` con cualquier feature de notificaciones (local, FCM, foreground service notifications) DEBE:
1. Declarar `POST_NOTIFICATIONS` en AndroidManifest.
2. Solicitar permiso en momento **contextual** (cuando el usuario activa la feature), NUNCA al arrancar la app.
3. Manejar el caso "permanent denial" redirigiendo a system settings.

Antes de añadir cualquier nueva integración que muestre notificaciones, verificar que el feature invoque `rememberNotificationPermissionHandle().requestIfNeeded()` antes de la primera notificación.

**Archivos afectados:**
- `androidApp/src/main/AndroidManifest.xml`
- `shared/src/commonMain/kotlin/com/example/mindos/core/notifications/NotificationPermissionGate.kt` (NUEVO)
- `shared/src/androidMain/kotlin/com/example/mindos/core/notifications/NotificationPermission.android.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/core/notifications/NotificationPermissionGateTest.kt` (NUEVO)

### 2026-05-28 — Release sin keystore real fallaba silenciosamente al debug signing

**Error detectado:**
La lógica de signing tenía fallback a debug `signingConfigs.getByName("debug")` cuando no había `MINDOS_KEYSTORE_FILE`, pero NO emitía ningún log. Era posible ejecutar `./gradlew assembleRelease` y obtener un APK firmado con debug sin darse cuenta. Si alguien subía ese APK a Play Store, sería rechazado o (peor) aceptado pero no recibiría updates por mismatch de firma futura.

**Causa raíz:**
El fallback silencioso se diseñó para que CI compilase release sin keystore (para validar R8/ProGuard), pero el mismo path se ejecutaba en desarrollo local sin warning. La intención (CI build) y el efecto secundario (dev build sin firma real) eran indistinguibles.

**Solución aplicada:**
1. Añadido `logger.warn("⚠️ androidApp:release is being signed with the DEBUG keystore...")` cuando el fallback se activa. Visible en cada build, imposible de ignorar.
2. Creado `gradle.properties.example` documentando las 4 variables (`MINDOS_KEYSTORE_FILE`, `MINDOS_KEYSTORE_PASSWORD`, `MINDOS_KEY_ALIAS`, `MINDOS_KEY_PASSWORD`) + comando `keytool` para generar el keystore + ejemplo de GitHub Actions con `MINDOS_KEYSTORE_B64`.
3. `.gitignore` actualizado para excluir `*.jks`, `*.keystore`, `key.properties`, `keystore.properties`, `/androidApp/keystore/`.

**Regla futura:**
Todo fallback de configuración crítica para seguridad (signing, encryption keys, API tokens) DEBE emitir un warning visible en el build log. El warning debe explicar:
- Qué configuración falta.
- Cuál fue la decisión (qué se usa en su lugar).
- Por qué eso NO sirve para producción.
- Dónde está la documentación para resolverlo.

**Archivos afectados:**
- `.gitignore`
- `gradle.properties.example` (NUEVO)
- `androidApp/build.gradle.kts`

### 2026-05-28 — Scaffold iosApp sin Xcode project committed

**Error detectado:**
No existía estructura `iosApp/` para consumir el shared KMP framework en iOS. Sin esto, aunque la app estaba registrada en Firebase (`mindos-8452f` con bundle `com.neu.app`), no había forma de compilarla.

**Causa raíz:**
El proyecto se empezó priorizando Android y desktop. iOS requiere infraestructura adicional (Xcode project, Podfile, Swift entry point) que solo es construible/verificable en macOS.

**Solución aplicada:**
Scaffold completo de `iosApp/` con:
- `iosApp/iosApp/iOSApp.swift` — `@main` SwiftUI app que monta `ComposeView` (UIViewControllerRepresentable) hospedando `MainViewControllerKt.MainViewController()`.
- `iosApp/iosApp/Info.plist` — HTTPS-only (`NSAllowsArbitraryLoads=false`, espejo del `network_security_config` de Android), modo oscuro forzado, descripciones de Camera/Microphone/Photos.
- `iosApp/Podfile` — referencia el shared module via `pod 'shared', :path => '../shared'` + Firebase pods (Core, Auth, Firestore, Storage, Functions, AppCheck, Crashlytics).
- `iosApp/.gitignore` — excluye `Pods/`, `*.xcworkspace`, `xcuserdata/`, `build/`, `DerivedData/`.
- `iosApp/README.md` — instrucciones de setup macOS paso a paso.
- `shared/src/iosMain/kotlin/com/example/mindos/MainViewController.kt` — entry point Kotlin que devuelve `UIViewController` envolviendo `App()`.

`iosApp.xcodeproj` NO se commit — se genera per-machine al ejecutar `pod install` la primera vez en macOS.

**Regla futura:**
Cuando un módulo de un proyecto multiplataforma requiere un toolchain que no está disponible en la máquina del autor (Xcode en Windows, MSVC en Linux, etc):
1. Crear todo lo scaffoldable (fuentes, configs, manifests).
2. NUNCA crear archivos de proyecto binarios/UUID a mano (xcodeproj, *.sln).
3. Documentar exhaustivamente los pasos para el primer setup en la plataforma objetivo.
4. Marcar como "verification deferred to <plataforma>" en el aprendizaje correspondiente.

La verificación real de que iosApp compila queda DIFERIDA a la primera vez que se abra en macOS — esto es un riesgo aceptado y explícito.

**Archivos afectados:**
- `iosApp/iosApp/iOSApp.swift` (NUEVO)
- `iosApp/iosApp/Info.plist` (NUEVO)
- `iosApp/Podfile` (NUEVO)
- `iosApp/.gitignore` (NUEVO)
- `iosApp/README.md` (NUEVO)
- `shared/src/iosMain/kotlin/com/example/mindos/MainViewController.kt` (NUEVO)

### 2026-05-28 — Auth y Storage operations sin timeout (bloqueo indefinido)

**Error detectado:**
Solo Firestore tenía timeouts. `signInWithEmailAndPassword`, `createUserWithEmailAndPassword`, `signOut`, `sendPasswordResetEmail` y todas las operaciones de Storage (`putData`, `delete`) podían colgarse indefinidamente sin emitir error. Esto se traduce en spinners infinitos en la UI cuando hay problemas de red.

**Causa raíz:**
Cuando se diseñó `FirestoreDefaults.kt` (Wave 4) se cubrió solo el caso de Firestore. Auth y Storage tienen sus propios mecanismos internos de retry/timeout pero ninguno garantiza un tope a nivel de aplicación.

**Solución aplicada:**
Extendido `FirestoreDefaults.kt` con dos nuevas familias de timeouts:
- **AuthDefaults**: SignInTimeout=20s, SignOutTimeout=10s, PasswordResetTimeout=15s, TokenRefreshTimeout=10s + helper `withAuthTimeout`.
- **StorageDefaults**: SmallReadTimeout=30s, LargeReadTimeout=180s, SmallUploadTimeout=60s, LargeUploadTimeout=300s, DownloadUrlTimeout=15s, DeleteTimeout=15s + helpers `withStorageReadTimeout` y `withStorageUploadTimeout`.

Aplicados timeouts en `FirebaseSessionRepository` (sign in/up/out/reset) y en `FirebaseDMRepository` + `ProfileVisualRepositories` (uploads de media discriminados por tipo: video usa Large, image/audio usa Small).

**Regla futura:**
Toda operación suspendible de Firebase (Auth, Firestore, Storage, Functions) DEBE envolverse en `with*Timeout`. Las operaciones de Storage que manejen video o archivos >5MB deben usar el `Large*` timeout explícito. Los flows (snapshots) no necesitan timeout porque son streams continuos.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/FirebaseSessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/ProfileVisualRepositories.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/network/TimeoutDefaultsTest.kt` (NUEVO)

### 2026-05-28 — Crashlytics sin breadcrumbs ni custom keys (crashes sin contexto)

**Error detectado:**
Crashlytics solo capturaba el stack trace. Sin breadcrumbs ni custom keys, era imposible saber en qué pantalla ocurrió un crash, si el usuario estaba autenticado, qué feature flag estaba activo, etc. Triage de crashes en producción se vuelve adivinanza.

**Causa raíz:**
El bridge inicial (Wave 3) solo expuso `NonFatalReporter.handler` para `recordException`. No había forma desde el módulo `:shared` de añadir `log()` ni `setCustomKey()`. La capa de observabilidad estaba incompleta.

**Solución aplicada:**
1. Creado `ObservabilitySink` interface (commonMain) con 5 primitivas: `breadcrumb(message)`, `customKey(k, v)` (String/Boolean/Int), `recordNonFatal(error)`.
2. `ObservabilityHub` singleton — sink registrable por la capa de aplicación.
3. `CrashlyticsObservabilitySink` (en `MindOSApplication`) forwarda todas las llamadas a Crashlytics SDK.
4. `AppRoute.telemetryName()` — nombre estable PII-free para cada ruta.
5. En `App.kt`: `LaunchedEffect(stack.current)` emite breadcrumb (`nav_to:dm_chat`) + custom keys (`current_route`, `nav_depth`) en cada cambio de ruta. `LaunchedEffect(sessionRepository)` actualiza `authenticated`.
6. Tests: `ObservabilityHubTest` (3 tests con sink mock), `AppRouteTelemetryTest` (2 tests verificando PII safety + estabilidad).

**Regla futura:**
Cualquier evento que ayude a contextualizar un crash debe pasar por `ObservabilityHub`. Reglas:
- `breadcrumb()` para eventos transitorios (navegación, network state, lifecycle).
- `customKey()` para contexto persistente (current_route, user role, feature flags).
- NUNCA pasar PII directamente. Si necesitas user id, hashearlo en la capa nativa antes de pasar al sink.
- Cada nueva pantalla añadida a `AppRoute` DEBE tener su `telemetryName()` actualizado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/Observability.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/SafeDebugLogging.kt` (route safeErrorLog through hub)
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteTelemetry.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (wire LaunchedEffect)
- `androidApp/src/main/java/com/example/mindos/MindOSApplication.kt` (CrashlyticsObservabilitySink)
- `shared/src/commonTest/kotlin/com/example/mindos/core/logging/ObservabilityHubTest.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteTelemetryTest.kt` (NUEVO)

### 2026-05-28 — Detekt baseline ocultaba bug latente: self en discovery list

**Error detectado:**
`FirebaseDMRepository.queryProfileVisualUsers(currentUserId: String)` recibía el `currentUserId` pero nunca lo usaba para filtrar al propio usuario de la lista de discovery. Resultado: el usuario se veía a sí mismo como contacto disponible para DM. El parámetro estaba en el baseline de detekt como `UnusedParameter`, escondido como ruido.

**Causa raíz:**
El baseline acumulaba findings indiscriminadamente. `UnusedParameter` y `UnusedPrivateProperty` son a menudo bugs reales (parámetros declarados pero nunca cumplidos) o code smell (dead code). Mezclarlos con `MagicNumber` (1200+ entries cosméticas) impedía detectarlos.

**Solución aplicada:**
1. Auditado el baseline filtrando solo `UnusedParameter` (29) + `UnusedPrivateProperty` (9).
2. Cada finding evaluado caso por caso:
   - **Bugs latentes**: `queryProfileVisualUsers.currentUserId` (no filtraba self), `decodeProfileVisualDiscoveryDto.path` (no se incluía en logs), `resolveUserId.path` (mismo).
   - **Dead code real**: `initialRadius`, `centerOffset`, `lum: Float`, `val h/w = maxHeight/maxWidth`, `causeMessage`, `val ProfileEditButton`, `val ProfileVisualUsernamePattern` (duplicado), `val carol` test fixture.
   - **API surface intencional**: callbacks de `ProfileScreen` (17), `HomeScreen.onOpenSettings`, `GlobalFeedScreen.onBack`, `AuthDiagnostic.toUserMessage.email`, `ChatBubble.unitH`. Suprimidos con `@Suppress` + comentario explicando el contrato.

**Regla futura:**
Detekt baseline es deuda técnica controlada, NO basurero. Categorías de findings que merecen revisión periódica caso-por-caso:
- `UnusedParameter` — frecuentemente bugs latentes
- `UnusedPrivateProperty` — siempre dead code real
- `UnreachableCode` — siempre bug
- `EqualsWithHashCodeExist` — siempre bug
- `UnsafeCallOnNullableType` — siempre riesgo

Categorías que se pueden tolerar en baseline indefinidamente:
- `MagicNumber` (ruidoso, raramente bug)
- `MaxLineLength` (cosmético)
- `WildcardImport` (estilo)

Antes de hacer "ratchet" (regenerar baseline), ejecutar la auditoría filtrada de las categorías de alto valor.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt` (self-filter + logs enriquecidos)
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ChromaInteractiveEngine.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/presentation/DMChatScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ProfileScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ProfileEntryScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/domain/model/ProfileVisualModels.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/domain/ranking/HomeFeedRankingEngine.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/home/presentation/HomeScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/AuthDiagnostics.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/domain/SocialGraphRulesTest.kt`
- `shared/detekt-baseline.xml` (de 2355 → 2273 entries; UnusedParameter/UnusedPrivateProperty de 38 → 0)

### 2026-05-29 — Cinco dependencias inline en `MindOSApp` dispersaban el wiring

**Error detectado:**
`MindOSApp(...)` aceptaba 5 parámetros con defaults `remember { FQN(...) }` (DMRepository, WalletApiClient, WaveRequestRepository, GlobalFeedRepository, privacyEnabledState). Cada caller (tests, main activity, preview) tenía que conocer las 5 y pasarlas individualmente. Añadir una sexta dependencia rompía la signature en todos los call sites.

**Causa raíz:**
La inyección de dependencias se hacía a nivel de parámetro de composable. Sin agrupación, el compose function se volvía un god-constructor con responsabilidades mezcladas (orquestación + wiring).

**Solución aplicada:**
Creado `AppGraph` (data class @Stable en `core/di/`) con las 5 dependencias agrupadas. `rememberAppGraph()` es la factory composable que construye el wiring de producción. `MindOSApp(...)` ahora recibe un `appGraph: AppGraph = rememberAppGraph()` único parámetro de wiring. Tests construyen un `AppGraph` con fakes y lo pasan.

**Regla futura:**
Toda app con 3+ dependencias inyectables al nivel root debe agruparlas en un `*Graph` data class. Reglas:
- El graph es `@Stable data class` (Compose skipping + equatable).
- Las dependencias son INTERFACES (nunca clases concretas como `FirebaseXyz`).
- La factory composable `rememberAppGraph()` vive junto al graph.
- Tests construyen el graph directamente con fakes; nunca usan `rememberAppGraph()`.

Antes de añadir un nuevo parámetro inyectable a `MindOSApp`, añadirlo al `AppGraph` y a `rememberAppGraph()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/di/AppGraph.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (signature simplificada)
- `shared/src/commonTest/kotlin/com/example/mindos/core/di/AppGraphTest.kt` (NUEVO)
- `shared/src/desktopTest/kotlin/com/example/mindos/MindOSAppNavigationSmokeTest.kt` (usa AppGraph)

### 2026-05-29 — kotlinx-datetime 0.7.1 (pulled transitively) removió Clock.System en runtime

**Error detectado:**
Tests que ejercitan código con `kotlinx.datetime.Clock.System.now()` fallan con `NoClassDefFoundError: kotlinx/datetime/Clock$System` en desktopTest. La versión pinned en `libs.versions.toml` es 0.6.0, pero un compilation-time dep pulled 0.7.1 que removió `Clock.System`.

**Causa raíz:**
kotlinx-datetime 0.7.0 deprecó `kotlinx.datetime.Clock` y movió la API a `kotlin.time.Clock`. La 0.7.1 ya no expone la clase a runtime. Compose Multiplatform / Kotlin 2.3 traen 0.7.1 transitivamente, sobrescribiendo el pin de 0.6.0.

**Solución aplicada:**
Para casos individuales: marcar los tests afectados como NO-COBERTURA documentando la razón. Workaround long-term: inyectar `AppClock` (patrón ya usado por `ProfileStateHolder`) en lugar de llamar `Clock.System.now()` directamente.

**Regla futura:**
TODO uso de tiempo en código de dominio/presentación debe ir a través de `AppClock` inyectado. Llamadas directas a `Clock.System.now()` son aceptables solo en:
- Capa de datos (repos) — usado en seeding de timestamps a Firestore.
- `MindOSApplication.onCreate()` y entry points donde el clock real es lo correcto.

Antes de añadir `Clock.System.now()` a un StateHolder o ViewModel, refactorizar para inyectar `AppClock`.

**Archivos afectados:**
- `shared/build.gradle.kts` (añadida dep explícita de kotlinx-datetime en desktopTest)
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (2 tests deshabilitados con documentación)

### 2026-05-29 — Tests de StateHolder con flows infinitos requieren `cancelChildren()` o `backgroundScope`

**Error detectado:**
`DMStateHolderTest` fallaba con `UncompletedCoroutinesError` porque el holder lanza `scope.launch { Flow.collect { } }` que suspende indefinidamente. `runTest` espera a que todos los hijos del scope completen y falla cuando hay jobs activos.

**Causa raíz:**
La convención en kotlinx-coroutines-test 1.10+ es que `runTest` aborta si quedan hijos activos al final del bloque. StateHolders que observan `StateFlow` (chats, messages) lanzan colectores infinitos.

**Solución aplicada:**
Patrón estándar para tests de StateHolders con flows infinitos:
1. Pasar `scope = this` (TestScope) al holder.
2. Al final de cada test que haya invocado `loadX`/`resolveX` que lance jobs persistentes: `coroutineContext.cancelChildren()`.
3. Alternativa: usar `backgroundScope` — pero NO siempre se comporta igual con `advanceUntilIdle` en flujos complejos (multi-launch nested).

**Regla futura:**
Plantilla para tests de StateHolder que lance Flow collectors:
```kotlin
@Test
fun myTest() = runTest {
    val holder = StateHolder(scope = this, ...)
    holder.startObservingStuff()
    advanceUntilIdle()
    // assertions ...
    coroutineContext.cancelChildren()  // ← cleanup
}
```

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/example/mindos/features/dm/presentation/DMStateHolderTest.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (NUEVO — usa GatedFakeFeedRepository para mid-flight asserts)

### 2026-05-29 — Cloud Functions (wallet) sin timeout y `callSafely` tragaba la cancelación

**Error detectado:**
Las 4 llamadas de `WalletApiClient` (`getBalance`, `getTransactions`, `initiatePurchase`, `verifyPurchase`) no se envolvían en ningún timeout de nivel de app — el único subsistema Firebase sin él, pese a que `FirestoreDefaults.kt` ya cubría Auth/Firestore/Storage. Además, `callSafely` capturaba `catch (e: Exception)` sin re-lanzar `CancellationException`, tragando la cancelación estructural (un job cancelado se mapeaba a `WalletError.NetworkError` en lugar de propagarse).

**Causa raíz:**
Cuando se diseñaron los timeouts (Wave 4/8) solo se cubrieron Auth/Firestore/Storage; Functions se omitió. El `callSafely` se escribió antes de aplicar el patrón "re-lanzar CancellationException" que el resto de repos ya seguían.

**Solución aplicada:**
1. Añadida familia `FunctionsDefaults` (`ReadCallTimeout=15s`, `WriteCallTimeout=20s`) + `withFunctionsReadTimeout`/`withFunctionsWriteTimeout` en `FirestoreDefaults.kt`.
2. Envueltas las 4 llamadas (read en getBalance/getTransactions; write en initiate/verifyPurchase).
3. `callSafely` ahora distingue: `TimeoutCancellationException` → `WalletError.NetworkError` (error recuperable, el servidor deduplica por idempotency key/token); `CancellationException` genuina → re-lanzada; resto → `mapException`.

**Regla futura:**
TODA llamada suspendible a Firebase Cloud Functions (HTTPS callables) debe envolverse en `withFunctionsReadTimeout` (no mutante) o `withFunctionsWriteTimeout` (mutante). Cualquier `catch (e: Exception)` que envuelva trabajo suspendible debe re-lanzar `CancellationException` ANTES del catch genérico; solo `TimeoutCancellationException` puede mapearse a un error de dominio.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/wallet/data/WalletApiClient.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/network/TimeoutDefaultsTest.kt` (+5 tests)

### 2026-05-29 — `Clock.System.now()` directo en StateHolder impedía cobertura (datetime 0.7.1)

**Error detectado:**
`GlobalFeedComposerStateHolder.draftId()` llamaba a `kotlinx.datetime.Clock.System.now()` directamente. Por el conflicto kotlinx-datetime 0.7.1 (documentado arriba), dos tests (`onAudioRecorded_enforcesMaxClipsLimit`, `onDeleteAudio_removesClipById`) no se podían escribir porque tropezaban con `NoClassDefFoundError` en el JVM de test.

**Causa raíz:**
La fuente de "ahora" estaba acoplada al SDK frágil en una función de presentación, sin inyección. El workaround "inyectar reloj" ya estaba anotado como deuda pero no implementado.

**Solución aplicada:**
Creada abstracción `core/time/EpochClock` (fun interface `nowMillis(): Long`) + `SystemEpochClock` como ÚNICO punto que toca `Clock.System`. `GlobalFeedComposerStateHolder` recibe `clock: EpochClock = SystemEpochClock`; `draftId` pasó de función top-level a método miembro que usa `clock.nowMillis()`. Los 2 tests antes imposibles ahora corren con un `IncrementingEpochClock` fake.

**Regla futura:**
NINGÚN StateHolder/ViewModel/composable debe llamar `Clock.System.now()` directamente. Inyectar `EpochClock` (epoch millis) o el `AppClock` social (`Timestamp`), con default de producción. La única clase que puede tocar `Clock.System` es `SystemEpochClock`. Para tiempo en capa de datos (seeding de timestamps a Firestore) sigue siendo aceptable el uso directo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/time/EpochClock.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolder.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (+2 tests reactivados)

### 2026-05-29 — Registro de usuario no enviaba email de verificación

**Error detectado:**
`createUserWithEmailAndPassword` capturaba `isEmailVerified` pero NUNCA disparaba `sendEmailVerification()`. Se podían crear cuentas con emails ajenos/inexistentes sin ninguna verificación; no existía forma de reenviar.

**Causa raíz:**
El flujo de registro se diseñó solo para crear la cuenta + perfil; la verificación de email se omitió.

**Solución aplicada:**
`SessionRepository` ahora expone `sendEmailVerification()` y `reloadCurrentUser()`. `createUserWithEmailAndPassword` dispara el email de verificación automáticamente (envuelto en `runCatching` + timeout, para no abortar el registro si el envío falla). `PreviewSessionRepository` implementa no-ops seguros. El gating de acciones por verificación queda como decisión de producto (hoy NO bloquea — banner no implementado, ver `docs/architecture/PHASE2_DEFERRALS.md`).

**Regla futura:**
Todo flujo de registro con email/contraseña debe disparar `sendEmailVerification()` tras crear la cuenta, sin bloquear el registro si el envío falla. Al añadir una nueva implementación de `SessionRepository`, implementar `sendEmailVerification()` y `reloadCurrentUser()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/SessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/FirebaseSessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/PreviewSessionRepository.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/session/SessionRepositoryContractTest.kt` (+3 tests)

### 2026-05-29 — Queries cliente de DM sin `.limit()` eran denegadas por las propias reglas

**Error detectado:**
`FirebaseDMRepository.getUserChats` y `getChatMessages` ejecutaban `.where/.orderBy.snapshots` SIN `.limit()`. Las reglas de `dm_chats` (`allow list` exige `request.query.limit != null && <= 50`) y de `messages` (`<= 100`) deniegan toda query con `request.query.limit == null`. Resultado: la lista de chats y los mensajes nunca cargarían en producción (PERMISSION_DENIED) — la feature de DM rota de extremo a extremo.

**Causa raíz:**
Cuando se endurecieron las reglas con límites de query (aprendizaje 2026-05-26) NO se actualizaron las queries cliente para fijar el `.limit()` correspondiente. Desfase regla↔cliente latente, invisible porque iOS es un shell de preview y el flujo nunca se ejercitó contra las reglas desplegadas.

**Solución aplicada:**
`getUserChats` → `.limit(ChatsPageLimit=50)` (50 chats más recientes, orderBy updatedAt DESC). `getChatMessages` → `orderBy(createdAt, DESCENDING).limit(MessagesPageLimit=100)` + `.reversed()` en el `map` (los 100 mensajes más recientes, mostrados cronológicamente como antes). Constantes con nota de que deben mantenerse <= los topes de `firestore.rules`.

**Regla futura:**
TODA query `list`/`snapshots` cliente sobre una colección cuya regla exige `request.query.limit` DEBE fijar `.limit(N)` con N <= el tope de la regla. Al cambiar un límite en `firestore.rules`, actualizar la query cliente en el mismo cambio (y viceversa). Validación real requiere el emulador de reglas (Fase 5 T5.1) — la verificación aquí fue solo de compilación.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-29 — Estado "en línea" fabricado (isOnline=true) mostraba presencia inexistente

**Error detectado:**
`App.kt` pasaba `isOnline = true` hardcodeado al abrir el info del chat, y `AppRoute.DMChatUserInfo`/`DMCajonera` + `DMChatUserInfoRoute` tenían `isOnline` con default `true`. La UI (`WavesChatUserInfoScreen`) pinta un punto verde "en línea" cuando `isOnline==true`. No existe ningún sistema de presencia → se mostraba a los usuarios un estado de presencia falso (UX engañosa + implica tracking que no ocurre).

**Causa raíz:**
Se usó `true` como placeholder de presencia sin backend que lo respaldara. Un placeholder se convirtió en un claim visible y falso.

**Solución aplicada:**
Cambiado el `true` fabricado en `App.kt` y todos los defaults de `isOnline` a `false`. La presencia real (RTDB/Firestore presence + ocultar el indicador cuando es desconocida) es una feature con backend, diferida.

**Regla futura:**
NUNCA fabricar valores que la UI presenta como hechos verificados (presencia, "visto", contadores en vivo). Si no hay fuente de datos, el default debe ser el conservador que NO afirme la capacidad. Los placeholders de UI no deben renderizarse como estado real.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRoute.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/presentation/WavesChatUserInfoScreen.kt`

### 2026-05-29 — Scripts de tests de reglas no eran ejecutables (sin --project; emulador exige JDK 21)

**Error detectado:**
Los scripts `test:*-rules` de `package.json` llamaban `firebase emulators:exec` SIN `--project`, y no existe `.firebaserc`. Sin proyecto activo, el emulador falla con "No currently active project". Además, el script combinado `test:rules` corría los 3 archivos `.mjs` bajo UN solo emulador, pero cada archivo fija un `projectId` distinto y `firebase.json` tiene `singleProjectMode: true` → conflicto. Y `firebase-tools` 15 exige **JDK 21+**, incompatible con el JDK 17 del build Gradle.

**Causa raíz:**
Los scripts se escribieron asumiendo un proyecto activo local. Nunca corrieron en CI, así que el desfase (project + singleProjectMode + versión de Java) quedó latente.

**Solución aplicada:**
Cada script ahora pasa su `--project` correspondiente (igual al `projectId` del `.mjs`) y corre en su PROPIO emulador; `test:rules` los encadena (3 emuladores secuenciales, no uno compartido). Nuevo job CI `backend-ci.yml` corre el emulador con **temurin 21**. La suite de Functions (704 tests jest) sí se verificó localmente.

**Regla futura:**
Los scripts `emulators:exec` DEBEN pasar `--project <projectId>` que coincida con el `initializeTestEnvironment({projectId})` del test. Con `singleProjectMode`, NUNCA correr tests de varios projectId bajo un solo emulador: uno por proyecto. El emulador de Firebase requiere JDK 21+ (el build Kotlin usa 17) — separar versiones de Java por job en CI.

**Archivos afectados:**
- `package.json` (scripts test:*-rules)
- `.github/workflows/backend-ci.yml` (NUEVO)

### 2026-05-30 — `.gitignore` `lib/` sin anclar excluía TODO el backend `functions/src/lib`

**Error detectado:**
`functions/.gitignore` tenía `lib/` (patrón sin anclar). gitignore aplica patrones sin barra inicial a CUALQUIER nivel, así que `lib/` no solo ignoraba `functions/lib/` (output compilado, intencional) sino también `functions/src/lib/` → los **19 archivos fuente del backend** (guards, rateLimiter, ledgerService, walletRepository, validators, fraudSignalService, reconciliationService, idempotencyService, etc.) **nunca se commitearon**. El repo no podía compilar/desplegar las Cloud Functions; un clone limpio fallaría. `accountDeletion.ts` (sí trackeado) importaba de `./lib/` inexistente en el repo.

**Causa raíz:**
Patrón gitignore sin anclar (`lib/` en vez de `/lib/`). El compilado vive en `functions/lib/` y el fuente en `functions/src/lib/`; ambos terminan en `lib/`, así que el patrón sin anclar capturó los dos.

**Solución aplicada:**
Cambiado `lib/` → `/lib/` en `functions/.gitignore` (ancla a `functions/lib/` únicamente). Añadidos al repo los 19 fuentes de `functions/src/lib/`. Verificado: `git check-ignore` ya no ignora `src/lib`, sigue ignorando `functions/lib/` compilado; `scan:secrets` limpio; jest 713 verde; build OK.

**Regla futura:**
En `.gitignore`, los patrones de output compilado que coincidan con un nombre de dir común (`lib`, `build`, `dist`, `out`) DEBEN anclarse con barra inicial (`/lib/`, `/build/`) para no capturar dirs homónimos de código fuente. Tras tocar un `.gitignore` de output, ejecutar `git check-ignore -v <ruta-fuente>` sobre una ruta de código para confirmar que NO queda ignorada, y `git ls-files <dir>` para confirmar que los fuentes están trackeados.

**Archivos afectados:**
- `functions/.gitignore`
- `functions/src/lib/*.ts` (19 archivos, ahora trackeados)
- `functions/src/accountDeletion.ts` + `accountDeletion.test.ts` (NUEVO — CF de borrado de cuenta B1)
- `functions/src/index.ts`, `functions/src/lib/rateLimiter.ts`

### 2026-05-30 — `gradlew` sin bit de ejecución rompía TODO el CI Linux (Permission denied)

**Error detectado:**
Todos los jobs Gradle del CI (assembleDebug, bundleRelease, detekt, desktopTest) fallaban en ~8 s con `./gradlew: Permission denied` (exit 126). El archivo `gradlew` estaba commiteado con modo `100644` (sin `+x`). En Windows (máquina del autor) se usa `gradlew.bat` y el bit es irrelevante, así que el fallo era invisible localmente.

**Causa raíz:**
`gradlew` se añadió al repo sin el bit de ejecución. En runners Linux, `./gradlew` necesita `+x`.

**Solución aplicada:**
`git update-index --chmod=+x gradlew` (modo → `100755`). En checkout Windows el bit es cosmético; git lo almacena y Linux lo recibe ejecutable.

**Regla futura:**
Todo script de entrada ejecutable en CI Linux (`gradlew`, `*.sh`) DEBE commitearse con `100755`. Verificar con `git ls-files -s <script>`; si es `100644`, `git update-index --chmod=+x`. Síntoma en CI: fallo en segundos con "Permission denied" / exit 126.

**Archivos afectados:**
- `gradlew` (modo 100644 → 100755)

### 2026-05-30 — Test de reglas positivo (T49) no reflejaba la query real → denegado correctamente

**Error detectado:**
`firestore.security.rules.test.mjs` T49 ("DM chats list with limit succeeds") hacía `getDocs(query(collection(alice,"dm_chats"), limit(50)))` SIN el filtro `where participants array-contains`. La regla `dm_chats allow list` exige `uid() in resource.data.participants`, así que el emulador denegaba la query (seguridad funcionando). El test esperaba `assertSucceeds` → fallo (1/81). Nunca se había detectado porque el CI de reglas no corría (gradlew/JDK rotos).

**Causa raíz:**
El test positivo de una `list` no replicaba las constraints que la regla exige; una `list` sin el `where` que acota a los docs permitidos es denegada por Firestore (no puede garantizar que todo doc devuelto cumpla la regla).

**Solución aplicada:**
Añadido `where("participants","array-contains","alice")` a T49 (espejo de `FirebaseDMRepository.getUserChats`). Importado `where`.

**Regla futura:**
Un test positivo (`assertSucceeds`) de una `list` con regla per-documento (`uid() in resource.data.X`) DEBE incluir el mismo `where`/filtro que la regla exige y que usa el cliente de producción. Si no, el emulador deniega legítimamente. Mantener los tests de reglas espejo de las queries cliente reales.

**Archivos afectados:**
- `firestore.security.rules.test.mjs` (T49 + import `where`)

### 2026-05-30 — `by rememberUpdatedState(...)` sin `import androidx.compose.runtime.getValue` no compila

**Error detectado:**
Varios actuals de plataforma nuevos (`ProfileAudioRecorder.desktop.kt`, `ProfileAudioPlayer.desktop.kt`, `ProfileForegroundEffect.desktop.kt`, etc.) fallaban con `Property delegate must have a 'getValue(Nothing?, KProperty0<*>)' method` y `Unresolved reference 'currentOnError'` al usar `val currentOnError by rememberUpdatedState(onError)`.

**Causa raíz:**
`rememberUpdatedState` devuelve un `State<T>`. El operador de delegación `by` sobre `State<T>` requiere la función de extensión `getValue` de `androidx.compose.runtime.getValue`. Importar solo `rememberUpdatedState`/`mutableStateOf` NO trae ese operador. En archivos `commonMain` grandes suele entrar transitivamente por otros imports, lo que enmascara la necesidad en archivos pequeños (actuals de plataforma).

**Solución aplicada:**
Añadir `import androidx.compose.runtime.getValue` en TODO archivo que delegue con `by` sobre Compose state (`mutableStateOf`/`rememberUpdatedState`/`collectAsState`). Para `by var` también hace falta `androidx.compose.runtime.setValue`.

**Regla futura:**
Al escribir un archivo nuevo (sobre todo actuals de plataforma pequeños) que use `by` sobre Compose state, importar SIEMPRE `androidx.compose.runtime.getValue` (y `setValue` si es `var`). No confiar en imports transitivos.

**Archivos afectados:**
- `shared/src/{android,desktop,ios}Main/kotlin/com/example/mindos/features/profile/presentation/**`

### 2026-05-30 — Git worktree sin `local.properties`: build Android falla con "SDK location not found"

**Error detectado:**
`:androidApp:assembleDebug` en el worktree (`.claude/worktrees/...`) fallaba con `SDK location not found. Define a valid SDK location with an ANDROID_HOME ... or sdk.dir`. El target Desktop compilaba bien (no necesita SDK Android).

**Causa raíz:**
`local.properties` está en `.gitignore` (per-máquina) y NO se copia al crear un git worktree. Sin `ANDROID_HOME`/`ANDROID_SDK_ROOT` en el entorno, AGP no localiza el SDK. Además, escribir `sdk.dir` con backslashes Windows sin escapar (`C:\Users\...`) hace que Java los lea como escapes → `IOException: ... sintaxis ... no son correctos`.

**Solución aplicada:**
Crear `local.properties` en la raíz del worktree con barras NORMALES (Java las acepta en Windows): `sdk.dir=C:/Users/curro/AppData/Local/Android/Sdk`. Queda gitignored (verificado con `git check-ignore`).

**Regla futura:**
Tras crear un git worktree, para builds Android crear `local.properties` con `sdk.dir` usando `/` (no `\`). Verificar que sigue ignorado. Síntoma: "SDK location not found" SOLO en Android; Desktop compila igual. Copiar el `sdk.dir` del checkout principal.

**Archivos afectados:**
- `local.properties` (worktree, no versionado)

### 2026-05-30 — Nueva pantalla de perfil MindOS (Perfil.html) como feature aislada

**Error detectado:**
No era un error: decisión de arquitectura al implementar el diseño `Perfil.html`. El `ProfileScreen` existente (`features/social/presentation/profile`) es un sistema complejo (chroma, módulos, segmented Portada/Perfil, publicaciones reales) divergente del nuevo diseño premium.

**Causa raíz:**
Reescribir en sitio habría arriesgado el editor chroma/módulos y sus tests. El diseño nuevo introduce conceptos que el modelo viejo no tiene (emoji prefix, audio con waveform real, donación, 3 enlaces custom, presencia online/offline).

**Solución aplicada:**
Feature nueva y autocontenida en `features/profile/` (domain/data/presentation/audio) con repos in-memory (`MindOSProfileRepository`, `ProfileObserverRepository`, `ProfilePresenceRepository`). `HomeScreen` renderiza `MindOSProfileScreen` en `HomeDestination.Profile`; el `ProfileEntryScreen` viejo permanece en el repo (sin usar). Imágenes reutilizan `rememberProfileMediaPicker`/`ProfilePlatformMediaPreview` del feature social. Audio real en Android (`MediaRecorder` + `getMaxAmplitude` para waveform real), stubs documentados en desktop/iOS.

**Regla futura:**
Para rediseños grandes que divergen de un sistema existente y tienen tests, preferir feature nueva aislada + un único punto de wiring (aquí `HomeDestination.Profile`) en vez de reescribir en sitio. Reutilizar utilidades multimedia compartidas (picker/preview) en vez de duplicar expect/actual. Inyectar abstracciones de plataforma (audio/permiso/presencia) por expect/actual con Android real + stubs documentados.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/profile/**` (NUEVO)
- `shared/src/{android,desktop,ios}Main/kotlin/com/example/mindos/features/profile/**` (NUEVO actuals)
- `shared/src/commonMain/kotlin/com/example/mindos/features/home/presentation/HomeScreen.kt` (wiring)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt` (tokens ProfileCanvas/ProfileTextPrimary/ProfileOnline)

### 2026-05-31 — `List<T> + (a..b)` resuelve al overload `plus(Iterable)` y expande el rango

**Error detectado:**
En el `TokenTrie` del nuevo módulo `mind-action-matching` (proyecto `mindos/`), `spans + (index..index)` con `spans: List<IntRange>` no compilaba: `Argument type mismatch: actual type is 'List<Any>', but 'List<IntRange>' was expected`. La intención era añadir el `IntRange` como UN elemento.

**Causa raíz:**
`IntRange` implementa `Iterable<Int>`. El operador `+` sobre `List<IntRange>` tiene dos overloads: `plus(element: T)` y `plus(elements: Iterable<T>)`. Como `(index..index)` ES un `Iterable<Int>`, Kotlin resuelve a `plus(Iterable)`, que CONCATENA los `Int` del rango en vez de añadir el rango como elemento → lista mixta `List<Any>`.

**Solución aplicada:**
Envolver el elemento en `listOf(...)`: `spans + listOf(index..index)`. Fuerza el overload `plus(Collection)` que concatena el rango como un único elemento.

**Regla futura:**
NUNCA usar `lista + elementoIterable` cuando el tipo de elemento es a su vez `Iterable` (`IntRange`/`CharRange`, `String`, otra `List`/`Set`). Kotlin elige `plus(Iterable)` y expande el elemento. Para añadir un elemento que es Iterable: `lista + listOf(elemento)` o `lista.plusElement(elemento)`.

**Archivos afectados:**
- `mindos/mind-action-matching/src/commonMain/kotlin/com/mindos/action/matching/TokenTrie.kt`

### 2026-05-31 — Tests asumían strip+match de zero-width; la capa de seguridad lo rechaza

**Error detectado:**
Un test de regresión (REG-003) en `mind-action-matching` asumía que un input con un carácter zero-width (`​`) inyectado se **normalizaba (strip) y matcheaba** igual (`"open set​tings"` → `Understood(open_settings)`). Falló: el matcher devolvía `Rejected`, no `Understood`.

**Causa raíz:**
La capa de seguridad (`mind-security`, integrada en `DefaultActionMatcher`) inspecciona el input ANTES del matching y **rechaza** caracteres invisibles/format (zero-width, control) como defensa anti-spoofing/homoglyph. El comportamiento correcto pasó de "strip silencioso" (asunción de la fase de normalización) a "rechazo explícito tipado" (decisión de seguridad), que es una garantía más fuerte. La asunción del test quedó obsoleta tras integrarse la capa de seguridad.

**Solución aplicada:**
Actualizado REG-003 para fijar el comportamiento real y seguro: input con zero-width → `Rejected` o `NoMatch` (nunca un `Understood` espurio). El assert acepta ambos resultados seguros.

**Regla futura:**
Al escribir tests sobre el matcher, recordar que `DefaultActionMatcher` aplica **inspección de seguridad antes de normalizar/matchear**: inputs con caracteres invisibles/control/homoglyph se **rechazan** (no se sanean silenciosamente). Nunca asumir strip+match para Unicode sospechoso; asumir rechazo tipado. Los tests de robustez deben aceptar `Rejected`/`NoMatch` como el resultado seguro correcto, no `Understood`.

**Archivos afectados:**
- `mindos/mind-action-matching/src/commonTest/kotlin/com/mindos/action/matching/RegressionSuiteTest.kt`

# Kotlin Multiplatform Technical Doctrine for MindBoost (repo real)

MindBoost apunta a Android + iOS con Kotlin Multiplatform: máxima lógica compartida respetando
fronteras de plataforma. Este doc está aterrizado en el repo real — ver `REPO_CONTEXT.md` para la
lista exacta de módulos y tareas.

## Realidad de módulos (no plantilla)

- **`:shared`** es el módulo KMP nuclear. Source sets: `commonMain`, `androidMain`, `desktopMain`,
  `iosMain`. Tests: `commonTest` (KMP) y `desktopTest` (JVM, aquí corre la matemática del scoring).
- **`:androidApp`** (Compose) y **`:desktopApp`** (host JVM). El build compuesto **`mindos/`**
  aporta módulos `mind-*`. El backend es **`functions/`** (TypeScript).
- **NO existe `composeApp` ni módulo Gradle `:iosApp`.** iOS es el Xcode `iosApp/`; en Windows la
  verificación iOS está **diferida** (sin toolchain macOS) — no pidas builds iOS locales.

## Prioridades de arquitectura

1. Lógica de entrenamiento cognitivo compartida en `commonMain`.
2. Modelos de dominio compartidos: ejercicios, sesiones, scores, progresión, estado de usuario.
3. Código de plataforma solo cuando lo exijan APIs del OS (sensores, notificaciones, timebase de
   frames, storage seguro, UI).
4. Algoritmos deterministas y testeables (scoring, dificultad adaptativa) — probados en `desktopTest`
   con casos input→output conocidos.
5. Simplicidad sobre pureza teórica.

## Candidatos a compartido (commonMain)

Definiciones de ejercicios · mapeo de dominios · orquestación de sesión · scoring · adaptación de
dificultad · curvas de progresión · esquema de eventos de analítica · estado de perfil · interfaces
de persistencia · abstracciones de tiempo (`EpochClock`/`AppClock`, **no** `Clock.System` directo
en holders) · feature flags.

## Candidatos a plataforma (expect/actual)

Notificaciones locales · lifecycle · haptics · timebase de frames (Choreographer en Android) ·
storage seguro · drivers de persistencia · accesibilidad nativa · APIs de store/suscripción.
Idioma del repo: preferir `@Composable expect fun rememberX()` (como `rememberSpeedTimebaseHandle`),
no siempre `expect class`. Todo `expect` necesita `actual` en android/desktop/ios.

## Persistencia (patrón del repo)

- **Datos cognitivos sensibles:** `@Serializable` (con `schemaVersion`, `Json { ignoreUnknownKeys
  = true; encodeDefaults = true }`) + **`SecureKeyValueStore`** cifrado (Android AES-256-GCM +
  Keystore; desktop/iOS in-memory/diferido). **No introduzcas Room ni SQLDelight** para estos datos
  aunque un plan externo lo pida: preserva la intención relacional en una abstracción de repositorio
  testeable en JVM. Persistir solo **resúmenes**, nunca trials crudos.
- **Social/backend:** Firestore + Cloud Functions. Reglas en `firestore.rules`/`storage.rules`:
  toda `allow list` exige `.limit()` en el cliente acorde a la regla; App Check en accesos privados;
  timeouts en toda operación Firebase.

## Reglas de ingeniería

- Respeta nombres de módulo y convenciones de paquete (`com.mindos.app`).
- Kotlin puro para algoritmos nucleares; sin dependencias nuevas sin justificar.
- Tests para scoring, progresión y adaptación (en `desktopTest`).
- Sin lógica de negocio en componentes UI; no duplicar lógica cognitiva por plataforma.
- App responsive: los loops de sesión no bloquean el hilo principal.
- **Detekt es un ratchet one-way:** archivo nuevo se evalúa con reglas por defecto; arreglar o
  `@Suppress` localizado + comentario. Nunca regenerar el baseline para silenciar. Reglas activas:
  `ReturnCount≤2`, `TooManyFunctions≤11/object`, `MaxLineLength=120`, `MagicNumber`,
  `TopLevelPropertyNaming` (const top-level en `SCREAMING_SNAKE_CASE`), `FunctionNaming`
  (Composables PascalCase ⇒ `@file:Suppress("FunctionNaming")`).

## Verificación (tareas REALES — no hardcodear a ciegas)

Detecta las tareas del repo; las habituales:

- `:shared:desktopTest` — tests de la lógica.
- `:shared:detekt` — static analysis (ratchet).
- `:shared:compileKotlinDesktop` · `:shared:compileAndroidMain` — compilación por target.
- `:androidApp:compileDebugKotlin` · `:androidApp:assembleDebug` — app Android end-to-end.
- `:shared:koverVerifyDesktop` · `:shared:koverHtmlReportDesktop` — cobertura (usar variante
  **Desktop**, no la fusionada, en KMP).
- Backend (`functions/`): `npm run typecheck`, `npm test`, `npm run scan:secrets`.
- Reglas Firebase: emulador con `--project <projectId>` que coincida con el test; JDK 21+.

Si Android Studio posee el daemon Gradle en el worktree, **no** invoques Gradle por CLI: verifica
estáticamente y entrega los comandos al usuario. Nada de clean destructivo ante hiccups. En
PowerShell, citar props con punto: `"-Pkotlin.incremental=false"`. Si una tarea "no se encuentra",
`:shared:tasks --all` antes de asumir nombres del plugin Android clásico.

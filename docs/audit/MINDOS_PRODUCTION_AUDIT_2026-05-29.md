# MINDOS — Auditoría de Producción (exhaustiva)

**Fecha:** 2026-05-29
**App:** MINDOS (anteriormente "NU"/"NEU"), Kotlin Multiplatform (Android · iOS · Desktop) + Firebase
**Rama:** `claude/mindos-production-audit-oMslg`
**Alcance:** repositorio completo — `shared/` (commonMain/androidMain/iosMain/desktopMain), `androidApp/`, `desktopApp/`, `functions/`, reglas Firestore/Storage, infra, CI/CD, configuración de release.
**Método:** mapa técnico del repo + lectura directa de configuración núcleo + 4 sub-auditorías paralelas (seguridad, arquitectura/concurrencia, networking/persistencia, UX/iOS/build). Verificación cruzada de afirmaciones de los documentos `docs/security/*` contra el código real.

> **Nota de validación:** no se ejecutaron builds (`gradlew`, `npm ci`, `tsc`, Xcode) en este entorno. Los hallazgos de compilación están **probados por análisis estático** (existencia de archivos, grafos de importación). Donde no pude verificar, lo marco como **RIESGO A VERIFICAR**.

---

## 1. RESUMEN EJECUTIVO

### Estado de preparación para producción: **22 / 100**
### Veredicto: **NO LISTA**

La capa de producto (Compose UI, modelos de dominio, reglas Firestore/Storage) está sorprendentemente avanzada y bien estructurada en muchas zonas. Pero el proyecto **no es desplegable hoy** por bloqueadores estructurales: el backend de moneda virtual **no compila** (falta código), **iOS no existe como app** pese a ser objetivo declarado, Android **no es publicable** (sin firma, `applicationId` de ejemplo), datos críticos viven **solo en memoria** (se pierden al reiniciar), y el **CI no compila ni testea la app móvil**. Además, los documentos internos de "release gate" afirman un estado ("704/704 tests, GO sandbox beta") que **es falso** contra el contenido actual del repositorio.

### Top 10 riesgos más graves

| # | Riesgo | Severidad |
|---|--------|-----------|
| 1 | `functions/src/lib/` (19 módulos: ledger, wallet, rate-limit, admin, fraude…) **no existe**. `walletFunctions.ts`, `adminFunctions.ts`, `webhooks.ts` y **todos** los tests de wallet importan de ahí → el backend **no compila, no testea, no despliega**. | CRÍTICO |
| 2 | **No existe app iOS**: sin proyecto Xcode, sin `Info.plist`, sin `Podfile`, sin entry point (`ComposeUIViewController`). iOS es objetivo declarado pero **no puede ejecutarse ni publicarse**. | CRÍTICO |
| 3 | Repositorios **in-memory en rutas de producción** (`InMemoryWaveRequestRepository`, `InMemorySocialGraph*`, `InMemoryPublishing*`) → relaciones sociales, waves y publicaciones **se pierden al reiniciar la app**. | CRÍTICO |
| 4 | Android **no publicable**: sin `signingConfig` de release (firma debug) y `applicationId = "com.example.neuv3"` → **rechazo automático** en Google Play. | CRÍTICO |
| 5 | El feature **Moneda/Wallet está cableado en la UI** (`AppRoute.Moneda`, `MoneyScreen`) pero su backend no compila ⇒ feature **muerto en producción**, fallará en runtime. | CRÍTICO |
| 6 | **CI no construye ni prueba KMP/Android/iOS**: solo valida `functions/` (TypeScript). La app móvil entera está **sin verificación automática** (y de hecho los jobs de functions estarían en rojo por #1). | ALTO |
| 7 | Documentos `release-gate.md` / `final-audit-report.md` afirman GO con "704/704 + 165/165" — **no auditable / falso** respecto al repo. Riesgo de decisión de lanzamiento basada en evidencia inexistente. | ALTO |
| 8 | Navegación hecha a mano sin back stack ni `rememberSaveable`; `AppRoute` contiene `AuthenticatedUser` no serializable → **se pierde el estado de navegación en process death** (Android mata el proceso en background). | ALTO |
| 9 | **Branding inconsistente**: `app_name = "NEUv3"`, pantalla de carga muestra "NEU", paquete `com.example.neuv3`; `MindOSIntroScreen` **no está cableado** en `App.kt`. El rebrand a MINDOS está a medias. | ALTO |
| 10 | God classes de presentación (Profile 923 LOC, Publisher 591, GlobalFeed 1094, Cajonera 1117, PublishingUseCases 1159) → mantenibilidad y testabilidad degradadas; lógica de negocio mezclada con UI. | ALTO |

### Top 10 acciones inmediatas

1. **Restaurar/implementar `functions/src/lib/`** o, si se perdió, recuperarlo de historia/backup. Sin esto no hay backend.
2. **Decidir el estado real de iOS**: o se crea el `iosApp/` Xcode + entry point, o se retira iOS del alcance del release (no anunciarlo).
3. **Sustituir repos in-memory** por persistencia real (Firestore + caché local) en waves/social/publishing, o marcarlos explícitamente como mocks no-producción tras feature flag.
4. **Configurar firma de release** y cambiar `applicationId` a un dominio propio (`com.newe.mindos` o similar).
5. **Gate de feature Wallet**: ocultar `AppRoute.Moneda` hasta que el backend compile y despliegue.
6. **Ampliar CI**: jobs `assembleRelease`, `:shared:*Test`, y build iOS en runner macOS.
7. **Corregir afirmaciones de los docs de seguridad** o regenerarlos ejecutando realmente las suites.
8. **Estado en process death**: usar `rememberSaveable` + un modelo de ruta serializable (ID de usuario, no objeto completo).
9. **Completar rebrand MINDOS** (strings, loading, intro, package) de forma coordinada.
10. **Borrar código muerto** (`/app`, `CHAT1VS1.kt`, `RecoverProfileScreen*.java`, `extract.*`, `out_lh.txt`) para reducir ruido y riesgo.

### Riesgos que BLOQUEAN producción
- #1 backend no compila · #2 iOS inexistente · #3 datos en memoria · #4 Android no firmable/`com.example` · #5 wallet muerto en runtime.

### Riesgos que pueden esperar (post-lanzamiento controlado)
- Optimización de partículas `SanctuaryBackground`, refactor de God classes (si se aísla bien), DI unificado, certificate pinning (Firebase ya fuerza TLS), módulo desktop.

---

## 2. AUDITORÍA TÉCNICA POR SECCIONES

> Formato por hallazgo: **Severidad · Evidencia (archivo:línea) · Por qué · Impacto · Verificación · Solución · Riesgo si no se corrige · Prioridad · Esfuerzo (XS/S/M/L/XL)**

### 2.1 Arquitectura

**A1 · CRÍTICO · Repositorios in-memory en producción**
- Evidencia: `App.kt:86` `InMemoryWaveRequestRepository()`; `HomeScreen.kt:88` `InMemorySocialGraphComponent.create()`; `features/social/data/publishing/InMemoryPublishing*` (postsById/outbox/feedCandidates en `mutableMapOf`).
- Por qué: el estado social (follows/blocks), waves aceptadas y el pipeline de publicación viven en RAM.
- Impacto: pérdida de datos al reiniciar; "fantasma" de followers; chats de wave que pierden acceso; posts atascados/perdidos.
- Verificación: instanciar app, crear relación/wave, matar proceso, reabrir → estado desaparece.
- Solución: implementar repos Firestore (ya existe patrón: `FirebaseDMRepository`, `FirebaseSocialInteractionRepository`) + caché local; o gate explícito de "demo".
- Riesgo: corrupción de grafo social, soporte saturado, reseñas 1★.
- Prioridad: Ahora · Esfuerzo: **L**

**A2 · ALTO · God classes / lógica de negocio en presentación**
- Evidencia (LOC): `PublishingUseCases.kt` 1159, `ProfileScreen.kt` 1129, `CajoneraScreen.kt` 1117, `GlobalFeedScreen.kt` 1094, `ProfileStateHolder.kt` 923, `HomeScreen.kt` 870, `PublisherStateHolder.kt` 591.
- Por qué: múltiples responsabilidades por archivo; difícil de testear/mantener; `ProfileStateHolder` mantiene doble fuente de estado (`composeUiState` + `_uiStateFlow`).
- Impacto: bugs difíciles de aislar, onboarding lento, regresiones.
- Solución: dividir StateHolders por concern (carga / acciones / edición); extraer sub-composables.
- Prioridad: Siguiente · Esfuerzo: **M–L**

**A3 · ALTO · Estado circular auth↔route**
- Evidencia: `App.kt:54-80` `LaunchedEffect(sessionRepository){ authState.collect{ route = … } }` con dos fuentes de verdad (`authState` y `route`).
- Por qué: re-emisiones de `authState` pueden sobreescribir la ruta actual (pérdida de posición de navegación).
- Solución: derivar destino base desde `authState` y mantener navegación de detalle aparte; o `derivedStateOf`.
- Prioridad: Siguiente · Esfuerzo: **S**

**A4 · MEDIO · DI fragmentada (sin contenedor)**
- Evidencia: mezcla de `remember { Constructor() }` (`App.kt:83-94`, `HomeScreen.kt:88-93`) y constructor injection en StateHolders; `FirebaseProfileVisualRepository()` instanciado en varios sitios.
- Impacto: testabilidad pobre, duplicación, posibles fugas si cambian keys de `remember`.
- Solución: contenedor manual (`object AppGraph`) o Koin; selección por feature flag (Firebase vs InMemory).
- Prioridad: Siguiente · Esfuerzo: **M**

**A5 · POSITIVO · Capas y expect/actual correctas**
- Domain/data/presentation bien separados; sin imports de plataforma en `commonMain`; `DomainResult`/`DomainError` tipados.

### 2.2 Kotlin Multiplatform

**KMP1 · ALTO · iOS solo se configura `if (isMacOs)`**
- Evidencia: `shared/build.gradle.kts:13-17,36-46` targets iOS + cocoapods bajo `isMacOs`.
- Por qué: en CI/Linux iOS no se compila → drift silencioso del código `*.ios.kt`.
- Solución: pipeline iOS en runner macOS; tratar el framework como artefacto de primera clase.
- Prioridad: Siguiente · Esfuerzo: **M**

**KMP2 · MEDIO · Mezcla SDK Firebase KMP (gitlive) + nativo (BoM/AppCheck)**
- Evidencia: `shared/build.gradle.kts` `dev.gitlive:firebase-* 2.4.0` + `androidApp/build.gradle.kts` `firebase-bom 33.15.0`, `firebase-appcheck-playintegrity`.
- Riesgo a verificar: que el token de App Check del SDK nativo realmente cubra las llamadas del SDK gitlive (mismo `FirebaseApp`).
- Prioridad: Siguiente · Esfuerzo: **M**

**KMP3 · BAJO · 165 `@Test` en commonTest** (plausibles, **no ejecutados** aquí). Buen punto de partida, pero ausentes tests de androidMain/iosMain y de UI.

### 2.3 Android

**AND1 · CRÍTICO · Sin firma de release + `applicationId = com.example.neuv3`**
- Evidencia: `androidApp/build.gradle.kts:29,42-51` (sin `signingConfig`, applicationId de ejemplo).
- Impacto: **rechazo en Google Play**; release firmado con clave debug.
- Solución: `signingConfigs.release` desde secrets/keystore; `applicationId` propio; `versionCode` por CI.
- Prioridad: Ahora · Esfuerzo: **M**

**AND2 · ALTO · Sin restauración de estado (process death)**
- Evidencia: `App.kt` usa `remember { mutableStateOf(route) }` (no `rememberSaveable`); `AppRoute.Main` contiene `AuthenticatedUser`.
- Impacto: al reanudar tras background+kill, la app resetea a ruta inicial; mala UX.
- Solución: persistir mínimo (ruta + uid) en `rememberSaveable`/SavedState; reconstruir usuario desde sesión.
- Prioridad: Siguiente · Esfuerzo: **M**

**AND3 · MEDIO · `INTERNET` no declarado explícitamente**
- Evidencia: `AndroidManifest.xml` solo declara CAMERA/RECORD_AUDIO. INTERNET se fusiona desde manifests de Firebase (merger), pero conviene declararlo explícito.
- Prioridad: Después · Esfuerzo: **XS**

**AND4 · MEDIO · `versionCode/Name` hardcodeados (=1 / "1.0")**
- Solución: derivar de CI (`github.run_number`). Prioridad: Siguiente · Esfuerzo: **S**

**AND5 · NOTA · Permisos CAMERA/RECORD_AUDIO** sí se usan (CameraX, ML Kit face detection, `DMAudioRecorder`, `FeedAttachmentPicker`). Acción real: justificar en ficha de la store + permisos en runtime + Data Safety. (No eliminar.)

### 2.4 iOS

**IOS1 · CRÍTICO · No hay app iOS**
- Evidencia: `find` no encuentra `*.xcodeproj`, `*.pbxproj`, `Podfile`, `Info.plist`, `*.swift`; `shared/src/iosMain/` solo tiene `actual` de plataforma; no hay `ComposeUIViewController`/entry. `settings.gradle.kts` no incluye `:iosApp`.
- Impacto: **imposible compilar/firmar/subir a App Store**; el código `*.ios.kt` no se valida.
- Solución: crear `iosApp/` (Xcode + SwiftUI `@main`), exponer `fun MainViewController() = ComposeUIViewController { NEUApp(...) }` en `iosMain`, enlazar `Shared.framework` + Firebase iOS, pipeline en macOS.
- Prioridad: Ahora (o retirar iOS del alcance) · Esfuerzo: **L**

**IOS2 · ALTO · Paridad funcional no garantizada** — sin entry iOS no hay forma de validar que iOS y Android se comporten igual. Riesgo a verificar tras IOS1.

### 2.5 Seguridad

**SEC1 · ALTO · Logging de diagnóstico de auth**
- Evidencia: `core/session/FirebaseSessionRepository.kt` (`logAuth` con `passwordPresenceForLog`), `core/session/AuthDiagnostics.kt`. En debug se loguea presencia de password y códigos de error; mensajes de usuario incluyen el email (`"No existe ningun usuario … para $email"`).
- Mitigación existente: logging desactivado en release (`isAndroidDebugLoggingEnabled = FLAG_DEBUGGABLE`, `SafeDebugLogging.android.kt` trunca a 100 chars).
- Solución: no loguear presencia de password ni email; verificar `debuggable=false` en release; regla R8 para strip de logs.
- Prioridad: Siguiente · Esfuerzo: **S**

**SEC2 · MEDIO · ProGuard/R8 con `-keep class … { *; }` demasiado amplios**
- Evidencia: `androidApp/proguard-rules.pro` mantiene íntegros coroutines, compose, firebase, gms, gitlive, serialization.
- Por qué: anula ofuscación de amplias zonas (ingeniería inversa) y reduce shrink; para serialization debería mantenerse solo los `@Serializable`/serializers.
- Solución: keeps mínimos por entry points; habilitar `android.enableR8.fullMode`.
- Prioridad: Siguiente · Esfuerzo: **S**

**SEC3 · MEDIO · Sin certificate pinning / network_security_config**
- Firebase fuerza TLS, pero no hay pinning ni config de red. Riesgo MITM en modelos de amenaza altos.
- Solución: `network_security_config.xml` con pin-set para endpoints Firebase (con backup pins y caducidad).
- Prioridad: Después · Esfuerzo: **S**

**SEC4 · BAJO · Claves Firebase públicas en `google-services.json` / `GoogleService-Info.plist`** — esperable (claves cliente). Acción: restringir claves API en consola (app bundle / SHA-256) y App Check en **enforce** en producción (hoy en modo auditoría según docs).

**SEC5 · POSITIVO · Reglas Firestore/Storage robustas**
- `firestore.rules` (~60KB): `signedIn()`/`isOwner()`, colecciones wallet `allow read,write: if false` (solo Admin SDK), límites de query (≤12), `canReadProfile()` con bloqueo+privacidad.
- `storage.rules`: `isAppChecked()` (App Check obligatorio), metadata de participantes en media DM, límites de tamaño.
- Gap MEDIO: sin escaneo antimalware de vídeos subidos (hasta ~78MB).

**SEC6 · RIESGO A VERIFICAR · Almacenamiento de tokens** — depende del SDK gitlive (delega en Keychain/Keystore nativos). Verificar versión y que no se persistan tokens en claro.

### 2.6 Networking

**NET1 · CRÍTICO (runtime) · Wallet llama funciones que no existen desplegables**
- Evidencia: `features/wallet/data/WalletApiClient.kt` invoca `getWalletBalance/getWalletTransactions/initiatePurchase/verifyPurchase`, exportadas por `functions/src/walletFunctions.ts`, que **no compila** (ver #1). Cableado vivo en `App.kt:97` + `AppRoute.Moneda`.
- Impacto: cualquier interacción con Moneda fallará.
- Solución: gate del feature + restaurar backend.
- Prioridad: Ahora · Esfuerzo: **S** (gate) / **L** (backend).

**NET2 · ALTO · Sin timeouts/retries/backoff en cliente**
- Evidencia: `WalletApiClient.callSafely` envuelve una sola invocación; sin reintentos ni timeout.
- Impacto: fallos falsos en redes móviles inestables; UX de compra frágil.
- Solución: reintentos con backoff+jitter (3 intentos) e idempotencia (ya hay `idempotencyKey`), timeout ~30s.
- Prioridad: Siguiente · Esfuerzo: **S–M**

**NET3 · MEDIO · Mapeo de errores por substring de mensaje**
- Evidencia: `mapException`/`detectFromMessage` buscan tokens en el texto de la excepción.
- Riesgo: frágil ante cambios de formato/locale del SDK.
- Solución: usar el código gRPC tipado del SDK; substring solo como fallback.
- Prioridad: Después · Esfuerzo: **S**

**NET4 · ALTO · Deserialización Firestore permisiva + `mapNotNull` que traga errores**
- Evidencia: `DMMessageDto`/`DMChatDto` con defaults `""`/`0L`; `FirebaseDMRepository` `try{…}catch{ null }`; feed `mapNotNull{ … }` descarta posts corruptos en silencio.
- Impacto: mensajes/posts desaparecen sin traza; estados imposibles ("Sent" por defecto).
- Solución: validar campos requeridos; loguear con `docId` antes de descartar; emitir a crash reporting.
- Prioridad: Siguiente · Esfuerzo: **S–M**

### 2.7 Persistencia

**PER1 · ALTO · Borradores del publisher solo en memoria**
- Evidencia: `PublisherLocalDraftStore` `InMemoryPublisherLocalDraftStore` (`mutableMapOf`); los `*PlatformStore` parecen no durables.
- Impacto: pérdida de borradores al backgroundear/crashear.
- Solución: almacenamiento durable por plataforma (DataStore/SQLDelight; NSUserDefaults/Files; SQLite desktop).
- Prioridad: Siguiente · Esfuerzo: **M–L**

**PER2 · MEDIO · Sin caché durable de balance wallet** → refetch constante, percepción de bug. Esfuerzo **M**.

**PER3 · MEDIO · Migraciones parciales** — `PublisherLocalDraftStore` tiene migración v1→v2, pero `ProfileVisualStorageData`/`DMStorageData` no. Versionar todos los formatos persistidos. Esfuerzo **M**.

### 2.8 Rendimiento

**PERF1 · ALTO · `SanctuaryBackground` 132 partículas + 6 pasadas Canvas/frame**, lista recreada en recomposición. Jank en gama media/baja en pantalla de selección (arranque). Solución: `@Immutable` + memo por valor, consolidar capas, cache `ImageBitmap`. Esfuerzo **M**.

**PERF2 · ALTO · `ChromaInteractiveEngine`** recomputa color y dispara `LaunchedEffect` por cada frame de drag (60Hz+). Throttle a ~16ms; `Snapshot.withoutReadObservation`. Esfuerzo **M**.

**PERF3 · MEDIO · `ProfileStoriesCarousel3D`** usa `VerticalPager(pageCount={Int.MAX_VALUE})` + `graphicsLayer` 3D por página. Page count finito + transform solo en visibles. Esfuerzo **M**.

**PERF4 · MEDIO · GlobalFeed**: múltiples `LaunchedEffect` (clave `burstNonce` duplicada en L230 y L693), muchos estados por card que recomponen. Extraer efectos; revisar claves. Esfuerzo **S–M**.

**PERF5 · ALTO (a verificar) · Fugas de listeners Firestore en DM** — `getUserChats/getChatMessages` exponen `Flow` de `.snapshots`. La cancelación del listener depende de la cancelación del `collect`. Verificar que cada navegación cancela su `Job`. Esfuerzo **M**.

**PERF6 · ALTO · N+1 en DM** — `getUsersByIds` hace `getConfig` por cada id en bucle secuencial. Batch query. Esfuerzo **M**.

### 2.9 UX/UI

**UX1 · ALTO · Estados error/empty/loading incompletos** — GlobalFeed solo distingue loading/empty/contenido (sin error+retry); ProfileStateHolder no distingue NotFound/Network/Unauthorized; UsernameSearch sin feedback de error. Modelar `Loading/Success/Error/Empty` + retry. Esfuerzo **M**.

**UX2 · ALTO · Accesibilidad** — varios `Icon(..., contentDescription = null)` (p.ej. `GlobalFeedScreen` botones de acción); falta tamaño táctil mínimo 48dp en algunos botones (`ElementalButton`). (Hay buenos ejemplos: "Volver", "Aplicar".) Riesgo de rechazo/baja accesibilidad. Esfuerzo **M**.

**UX3 · ALTO · Branding MINDOS incompleto** — `strings.xml` `app_name="NEUv3"`, loading screen `Text("NEU")` (`App.kt`), `MindOSIntroScreen` **no cableado**, package `com.example.neuv3`. Unificar marca. Esfuerzo **M**.

**UX4 · MEDIO · Sistema de diseño duplicado** — `design/NEUTheme.kt` (real) vs `app/.../ui/theme/*` (plantilla muerta con `Purple80`). Borrar el muerto. Esfuerzo **XS**.

**UX5 · MEDIO · Navegación sin back stack real / sin deep links** — `when(route)` con back hardcodeado por pantalla; difícil de escalar. Evaluar Navigation Compose / Voyager / Decompose. Esfuerzo **L**.

### 2.10 Testing

**TST1 · ALTO · Tests de wallet del backend referencian módulos inexistentes** (`./lib/*`) → la suite Jest **no puede ejecutarse**. (564 `it/test` aprox., todos dependientes de `lib/`.)
**TST2 · MEDIO · 165 `@Test` en commonTest** (dominio/mappers/state) — buena base; faltan tests de androidMain/iosMain, UI/snapshot, e2e, offline, sesión expirada, migraciones.
**TST3 · MEDIO · Sin tests instrumentados reales** (`app/.../ExampleInstrumentedTest.kt` es plantilla en módulo muerto).

### 2.11 CI/CD y Release

**CI1 · ALTO · CI solo valida `functions/`** (`.github/workflows/ci.yml`): scan secrets, typecheck, jest, build, npm audit. **No** hay `gradlew assembleRelease`, ni `:shared` tests, ni build iOS. Y por #1 incluso los jobs de functions estarían **en rojo**.
**CI2 · CRÍTICO · `predeploy` de Functions** (`firebase.json`) corre typecheck+test+build → **el deploy de funciones fallará** mientras falte `lib/`.
**CI3 · MEDIO · Node 18** (`functions/package.json engines`) en EOL; `firebase-functions ^4.7.0` desactualizado (v6 disponible). Actualizar.
**CI4 · MEDIO · Sin estrategia de versionado/changelog/rollback/feature flags** documentada para móvil.

### 2.12 Dependencias

**DEP1 · MEDIO · Versiones bleeding-edge**: AGP 9.1.0, Gradle 9.3.1, Kotlin 2.3.10, Compose MP 1.10.3, `com.android.kotlin.multiplatform.library`. Riesgo de inestabilidad/plugins inmaduros. Fijar y validar build reproducible.
**DEP2 · MEDIO · `material3 = 1.10.0-alpha05`** (dependencia **alpha** en producción). Mover a estable.
**DEP3 · BAJO · Backend**: `firebase-functions ^4.7.0`, Node 18 (ver CI3).

### 2.13 Observabilidad

**OBS1 · ALTO · Sin crash reporting** (no Crashlytics/Sentry detectado en cliente). Imposible diagnosticar fallos reales en producción.
**OBS2 · ALTO · Sin analytics de eventos críticos** (auth, compra, publicación). 
**OBS3 · MEDIO · Logging cliente solo en debug**; bien para PII, pero deja release sin telemetría. Añadir logging estructurado seguro + breadcrumbs.
**OBS4 · POSITIVO (backend, si compilara)** · `AuditLogService` con `sanitizeDetails` y enum cerrado — buen diseño (pero pertenece a `lib/` ausente).

### 2.14 Privacidad y cumplimiento

**PRIV1 · ALTO · Sin Data Safety (Play) / Privacy Nutrition (App Store)** preparados; la app maneja email, media, mensajes, relaciones → declaraciones obligatorias.
**PRIV2 · ALTO · ATT (iOS)** — si hubiera tracking/analytics, requiere App Tracking Transparency (depende de IOS1).
**PRIV3 · MEDIO · Borrado/exportación de cuenta** — no se observa flujo de eliminación de cuenta (requisito de ambas stores). 
**PRIV4 · MEDIO · Política de privacidad / términos** — no enlazados en la app.
**PRIV5 · POSITIVO · backup Android** restringido (`allowBackup=false`, exclusiones de `dm_audio/` y sharedpref).

### 2.15 Deuda técnica

- Código muerto: módulo `/app` (no incluido en `settings.gradle.kts`), `CHAT1VS1.kt` (package `com.example.neu`), `RecoverProfileScreen.java`/`2`, `extract.js`/`extract.py`, `out_lh.txt`, `mind-os-intro/` (HTML/JS suelto).
- Doble fuente de estado en `ProfileStateHolder` (`composeUiState` + `_uiStateFlow`).
- `error/`/excepciones mezcladas con `DomainResult` (`InMemoryWaveRequestRepository` lanza excepciones en vez de devolver `Result`).
- Documentos de auditoría/“plan blindado” extensos que describen un sistema que no está completo en el repo → riesgo de confianza.

### 2.16 Riesgos de escalabilidad

- Repos in-memory no escalan ni persisten (A1/PER1).
- Caché de publishing con `Mutex` único grueso + snapshot/restore O(n) por transacción.
- N+1 en DM (PERF6) y feed sin caché de ranking → coste Firestore y latencia con crecimiento.
- Navegación monolítica (UX5) dificulta añadir secciones sin tocar `App.kt`.

### 2.17 Calidad general del código

- **Puntos fuertes**: dominio tipado, expect/actual limpio, reglas de seguridad serias, contratos wallet bien documentados (cliente nunca autoritativo), naming consistente.
- **Puntos débiles**: archivos gigantes, estados in-memory, ausencia de DI, errores tragados en deserialización, branding a medias, ausencia de observabilidad, y la **inconsistencia código↔docs** (lib ausente, GO falso).

---

## 3. MATRIZ DE RIESGOS (resumen)

| ID | Hallazgo | Sev | Bloquea prod | Esfuerzo |
|----|----------|-----|--------------|----------|
| #1 | `functions/src/lib/` ausente (backend no compila) | CRÍTICO | Sí | L |
| #2 | App iOS inexistente | CRÍTICO | Sí | L |
| #3 | Repos in-memory en producción | CRÍTICO | Sí | L |
| #4 | Android sin firma + `com.example` | CRÍTICO | Sí | M |
| #5 | Wallet cableado pero backend roto | CRÍTICO | Sí | S/L |
| #6 | CI no compila/testea móvil | ALTO | Parcial | M |
| #7 | Docs release-gate falsos | ALTO | Parcial | S |
| #8 | Sin estado en process death | ALTO | No | M |
| #9 | Branding MINDOS incompleto | ALTO | No (store sí exige nombre) | M |
| #10 | God classes | ALTO | No | M–L |
| NET2/NET4 | Sin retries / errores tragados | ALTO | No | S–M |
| OBS1/OBS2 | Sin crash reporting/analytics | ALTO | No | M |
| PERF1-6 | Jank/leaks/N+1 | ALTO/MEDIO | No | M |
| UX1/UX2 | Estados + accesibilidad | ALTO | App Store: parcial | M |
| PRIV1-4 | Data Safety/ATT/borrado cuenta | ALTO/MEDIO | Store: sí | M |
| DEP1/DEP2 | Bleeding-edge + material3 alpha | MEDIO | No | S |

---

## 4. PLAN DE IMPLEMENTACIÓN POR FASES

### Fase 0 — Diagnóstico y estabilización (1–3 días)
- **T0.1 · Verificar build real** — `cd functions && npm ci && npm run typecheck` (probar #1); `./gradlew :shared:desktopTest`; `./gradlew :androidApp:assembleDebug`. Sev: CRÍTICO. Estado: **Ahora**. Aceptación: salidas reales registradas. Esfuerzo S.
- **T0.2 · Recuperar `functions/src/lib/`** — buscar en historia/backups/otra rama; si no existe, abrir tarea de reimplementación. Sev: CRÍTICO. **Ahora**. Esfuerzo S (búsqueda) / L (reimpl).
- **T0.3 · Inventario código muerto** y confirmación de orfandad (`/app`, loose files). **Ahora**. Esfuerzo XS.

### Fase 1 — Bloqueadores críticos de producción
- **T1.1 · Backend wallet compila y despliega** (depende T0.2). Criterio: `npm run verify` verde, `firebase deploy --only functions` ok. Sev CRÍTICO. **Ahora**. Esfuerzo L.
- **T1.2 · Gate del feature Wallet** mientras T1.1 no esté listo: ocultar `AppRoute.Moneda`/entrada en Home. Sev CRÍTICO. **Ahora**. Esfuerzo S. Riesgo: ninguno.
- **T1.3 · Firma de release + `applicationId` propio + versionCode CI** (`androidApp/build.gradle.kts`). Sev CRÍTICO. **Ahora**. Esfuerzo M. Aceptación: `assembleRelease` firmado instala.
- **T1.4 · Decisión iOS**: (a) crear `iosApp/`+entry+CI macOS, o (b) retirar iOS del alcance del lanzamiento y de la comunicación. Sev CRÍTICO. **Ahora**. Esfuerzo L (a) / S (b).
- **T1.5 · Persistencia real waves/social/publishing** o feature flag explícito de demo. Sev CRÍTICO. **Ahora**. Esfuerzo L.
- **T1.6 · Corregir/anular docs `release-gate`/`final-audit`** para reflejar el estado real. Sev ALTO. **Ahora**. Esfuerzo S.

### Fase 2 — Arquitectura y mantenibilidad
- T2.1 Romper estado circular auth↔route (S). 
- T2.2 `rememberSaveable` + ruta serializable para process death (M).
- T2.3 DI unificado (M). 
- T2.4 Dividir God StateHolders (M–L). 
- T2.5 Borrar código muerto (XS).
- T2.6 Completar rebrand MINDOS coordinado (M).

### Fase 3 — Rendimiento y estabilidad
- T3.1 Retries+timeout+backoff en networking wallet (S–M).
- T3.2 Loguear y no tragar errores de deserialización (S–M).
- T3.3 Verificar/cancelar listeners DM + batch N+1 (M).
- T3.4 Optimizar SanctuaryBackground/Chroma/Carousel (M).
- T3.5 Caché durable de borradores y balance (M–L).

### Fase 4 — UX/UI premium
- T4.1 Estados Loading/Empty/Error+retry en feed/perfil/búsqueda (M).
- T4.2 Accesibilidad: contentDescription + 48dp + TalkBack/VoiceOver (M).
- T4.3 Eliminar tema duplicado; dark mode coherente (XS–S).
- T4.4 (Opcional) Navegación con back stack real/deep links (L).

### Fase 5 — Testing y QA
- T5.1 Restaurar suite Jest backend (depende T1.1) (S).
- T5.2 Tests de StateHolders/repos Firebase con fakes (M).
- T5.3 UI/snapshot + e2e de flujos críticos (auth, publicar, DM, compra) (L).
- T5.4 Tests offline/sesión expirada/migraciones (M).

### Fase 6 — Observabilidad
- T6.1 Crashlytics/Sentry en cliente (M).
- T6.2 Analytics de eventos críticos sin PII (M).
- T6.3 Logging estructurado seguro + breadcrumbs (S).

### Fase 7 — App Store y Google Play
- T7.1 Data Safety (Play) + Privacy Nutrition (App Store) (M).
- T7.2 Política de privacidad/términos enlazados in-app (S).
- T7.3 Flujo de borrado/exportación de cuenta (M).
- T7.4 ATT iOS si hay tracking (S, depende T1.4a).
- T7.5 Restringir claves API Firebase + App Check **enforce** (S).
- T7.6 Metadata, capturas, iconos, firma/provisioning (M).

### Fase 8 — Release controlado
- T8.1 Build firmado en CI + canal interno/cerrado.
- T8.2 Rollout escalonado (Play staged rollout / TestFlight).
- T8.3 Monitoreo (crash-free rate, ANR) + plan de rollback.
- T8.4 Checklist final (sección 5) firmado.

**Dependencias clave:** T1.1←T0.2; T1.2 desbloquea lanzar sin wallet; T5.1←T1.1; T7.4←T1.4a; iOS entero←T1.4a.

---

## 5. CHECKLIST FINAL DE PRODUCCIÓN

- [ ] `functions` compila, testea y despliega (`lib/` presente).
- [ ] Wallet: backend desplegado **o** feature oculto.
- [ ] Android firmado, `applicationId` propio, `versionCode` por CI.
- [ ] iOS: app Xcode + entry **o** iOS retirado del alcance/comunicación.
- [ ] Sin repos in-memory en rutas de producción (o flag de demo claro).
- [ ] Estado de navegación sobrevive a process death.
- [ ] Rebrand MINDOS completo (nombre, intro, loading, package).
- [ ] CI compila `assembleRelease` + tests `:shared` (+ iOS si aplica).
- [ ] Crash reporting + analytics activos.
- [ ] Estados Loading/Empty/Error + accesibilidad mínimos.
- [ ] Data Safety / Privacy labels + política + borrado de cuenta.
- [ ] App Check en enforce; claves API restringidas.
- [ ] Docs de seguridad consistentes con el código real.

---

## 6. RECOMENDACIÓN FINAL DE LANZAMIENTO

**NO LANZAR** en el estado actual.

**Lanzar SOLO con condiciones** (ruta mínima realista a una beta cerrada Android):
1. Resolver #1 (backend) o **ocultar Wallet** (#5/T1.2).
2. Persistencia real o flag de demo (#3).
3. Firma + applicationId propio (#4).
4. Retirar iOS del alcance hasta tener `iosApp/` (#2).
5. Crash reporting mínimo (OBS1) + estados de error básicos (UX1).
6. Rebrand visible (UX3) + Data Safety/política (PRIV).

Con eso podría ir a **canal interno/cerrado Android**. Producción pública abierta y App Store requieren completar Fases 2–7.

---

## 7. ARCHIVOS/ÁREAS QUE DEBES ENVIAR SI FALTA INFORMACIÓN

1. **`functions/src/lib/**`** completo (o confirmar que se perdió) — es el bloqueador #1.
2. Salida real de `cd functions && npm ci && npm run verify`.
3. Salida real de `./gradlew :shared:desktopTest`, `:androidApp:assembleRelease`.
4. Si existe en algún lado: el proyecto **Xcode/`iosApp`** y `MainViewController`.
5. Configuración de **App Check** (modo audit vs enforce) en consola Firebase.
6. Confirmación de si **iOS y Desktop** están en el alcance del lanzamiento.
7. Política de privacidad/términos y diseño del flujo de **borrado de cuenta**.
</content>
</invoke>

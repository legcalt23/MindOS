# MindBoost — Firestore Sync · Phase 1.1: cola durable + adapter Firestore + rules emulator

**Fecha:** 2026-07-04 · **Autor:** Staff Mobile Engineer · **Estado:** Infraestructura controlada, NO montada

> **Alcance:** infraestructura técnica lista para pruebas dev/staging de Enfoque. **NO** despliegue, **NO**
> sync real activado, **NO** coordinador montado en el grafo vivo, **NO** blockScores/scoreEvents, **NO** otros
> motores, **NO** IA/Elite/ranking, **NO** rehidratación multi-dispositivo, **NO** raw trials, **NO** tocar
> scoring/calibración/taxonomía, **NO** migrar namespaces, **NO** activar sin consentimiento. Verificación
> estática — los gates Gradle los ejecuta el dueño (el IDE posee el daemon).

> ⚠️ **Verdad de durabilidad (honesta):** la cola es **durable en Android** (AES-256-GCM + Keystore) y, tras
> **Phase 1.2**, **en iOS vía Keychain** (`IndexedSecureKeyValueStore` + `IosKeychainBackend`) — con el binding
> nativo iOS **pendiente de `:shared:compileKotlinIosSimulatorArm64` + verificación** (ver
> `MINDBOOST_IOS_SECURE_KEY_VALUE_STORE.md`). **Desktop** sigue in-memory (dev/test). La clase se llama
> `SecureSyncQueueRepository` (no "Durable"): su durabilidad = la del store de plataforma.

---

## 1. Alcance

Convertir el piloto de Enfoque (Phase 1, probado con fakes) en infraestructura preparada para pruebas
controladas: cola de sync persistida (`SecureSyncQueueRepository`, **durable en Android**, in-memory en
iOS/desktop), **adapter Firestore real** (aislado tras un gateway testeable) y **rules draft** verificable.
Feature flag sigue OFF; consentimiento GDPR sigue pendiente antes de producción. El objetivo NO es despliegue.

## 2. Qué se implementó — y qué NO

| Área | Implementado | No implementado |
|---|---|---|
| Cola persistida | `SecureSyncQueueRepository` (Android-durable/cifrada; **iOS Keychain vía Phase 1.2**; desktop in-memory), namespace `mindboost_sync_queue`, **total + observable** | Verificación de compilación iOS (Phase 1.2) — **P1**; retry loop/backoff (Phase 2) |
| Erasure GDPR | Namespace en `AccountBound` + eraser **derivado** de `AccountBound` (`accountBoundEraserStores`) + `deleteByUser(userId)` | — |
| Adapter Firestore | `FocusSessionFirestoreGateway` (seam) + `FirestoreFocusSessionRemoteDataSource` (puro, testeado) + `GitLiveFocusSessionFirestoreGateway` (real, gitlive) | Verificación en emulador/device del gitlive (gate manual) |
| Rules | Borrador `MINDBOOST_FIRESTORE_RULES_DRAFT.md` + plan de emulador | Edición de `firestore.rules` (exige emulador JDK 21) |
| Coordinador | Ya acepta `SyncQueueRepository` → la cola es drop-in; probado con `SecureSyncQueueRepository` | Cableado en `rememberFocusGraph()` (activación) |
| Feature flag | `mindboost_sync_firestore_enabled` OFF (Default/Static/RemoteConfig + `setDefaultsAsync`) | UI de consentimiento |
| blockScores / scoreEvents / otros motores | — | Fuera de Phase 1.1 |

## 3. Estado base preservado (sin romper)

`SyncSessionId`, `SessionFingerprint`, `SyncWriteResult`, `FocusSessionCloudDto`, `FocusSessionCloudMapper`,
`FocusSessionRemoteRepository`, `SyncingFocusSessionHistoryRepository`, `SyncQueue`, flag OFF. Invariantes
selladas intactas: `PERMISSION_DENIED != Synced`, `PERMISSION_DENIED != AlreadySyncedDuplicate`; duplicado
válido solo con verificación remota positiva (existe + mismo userId + mismo fingerprint; si diverge/no
verificable → `Conflict`).

## 4. Cola persistida (`SecureSyncQueueRepository`)

**Auditoría de durabilidad (verdad por plataforma):**

| Platform | Implementation | Durable (restart/reboot) | Encrypted at rest | Suitable for sync queue | Action |
|---|---|---|---|---|---|
| **Android** | `AndroidSecureKeyValueStore` (AES-256-GCM + Keystore + private SharedPreferences) | **Sí** | **Sí** | **Sí** | listo para dev |
| **iOS** | `IndexedSecureKeyValueStore` + `IosKeychainBackend` (Phase 1.2) | **Sí (diseño)** | **Sí (Keychain)** | **Sí (diseño)** | **compilar+verificar iOS (`:shared:compileKotlinIosSimulatorArm64`)** |
| **Desktop** | `InMemorySecureKeyValueStore` (target dev/test) | **No** | **No** | No (dev/test) | fuera de alcance de durabilidad |

- **Nombre honesto:** `SecureSyncQueueRepository` (no "Durable"): su durabilidad = la del store de plataforma
  (Android sí; iOS sí vía Keychain tras Phase 1.2, pendiente de verificación; desktop no). Una fila JSON por
  entrada, key `entry:{entryId}` (=`sessionId`). DTO versionado
  (`schemaVersion`) con enums por `.name`. NO Room/SQLDelight (learning 2026-06-12).
- **Contrato TOTAL + OBSERVABLE:** ninguna operación lanza por errores de storage/serialización/enum — solo
  `CancellationException` propaga — pero `record(entry): Boolean` **informa** si la fila se persistió
  (`true`) o no (`false`, sin excepción). Ante fallo de store: `record`→**`false`**; `find`→null; `pending`→
  lista vacía; `deleteByUser`→0. `limit <= 0` → lista vacía (no throw). Fila corrupta / enum desconocido /
  versión futura → se **ignora** (nunca rompe toda la cola). `false` **nunca** es éxito.
- **Trazabilidad en el coordinador:** `record(PENDING)==false` ⇒ **NO se llama a remote** (se devuelve
  `UnknownFailure`, no se pierde el rastro del intento). `record(resultado final)==false` ⇒ la fila queda en su
  estado previo (`PENDING`), **nunca** un `SYNCED` fabricado; el coordinador devuelve el outcome remoto real
  (riesgo de observabilidad/cola documentado, no de corrección).
- **Estados:** `PENDING` / `SYNCED` / `FAILED` / `CONFLICT`.
- **Idempotencia:** upsert por `entry:{sessionId}` → nunca duplica.
- **Reintentos:** `NetworkFailure`→`PENDING` (reintentable); `ValidationFailure`/`PermissionDenied`/`Unknown`→
  `FAILED`; `Conflict`→`CONFLICT`. `attemptCount` persistido para el futuro retry loop. `pending(userId, limit)`
  de lectura acotada.
- **Borrado por usuario:** `deleteByUser(userId)` elimina solo las filas de ese usuario; una fila corrupta no
  atribuible se **deja** (no se cross-borra) y la reclama el eraser de namespace completo. El namespace
  `mindboost_sync_queue` está en `AccountBound` + cableado en el eraser de `AppGraph` **derivado** de
  `AccountBound` (`accountBoundEraserStores`), con test que **falla si se omite** (learning 2026-06-28).
- **Limitaciones:** durabilidad real Android + iOS (iOS pendiente de compilar/verificar, Phase 1.2); desktop
  in-memory (dev/test); sin retry loop/backoff (Phase 2); namespace vacío en producción hasta montar; guardar
  local NO depende de la cola.

## 5. Adapter Firestore

- **Ruta:** `users/{userId}/sessions/{sessionId}` (constantes `USERS`/`SESSIONS` en el gateway gitlive).
- **Operación:** **check-then-create** (`get` → si ausente `set(dto)`). La regla `allow update: if false` es el
  guard de sobrescritura autoritativo del servidor; una carrera perdida degrada a update denegado
  (`PERMISSION_DENIED`), nunca a sobrescritura silenciosa.
- **Verificación de duplicados:** en `ALREADY_EXISTS`, el data source lee el doc (`readSession`): `Found` →
  `RemoteSessionProbe`; `NotFound`/`Unreadable` → `null` → el clasificador falla cerrado a
  `Conflict(UNVERIFIED_EXISTING)`. Nunca acepta un duplicado sin verificación positiva.
- **Clasificación de errores** (gitlive `FirestoreExceptionCode` → `RawWriteOutcome`, fail-closed):
  `PERMISSION_DENIED`/`UNAUTHENTICATED`→`PERMISSION_DENIED` (jamás éxito); `UNAVAILABLE`/`DEADLINE_EXCEEDED`→
  `NETWORK_ERROR` (reintentable); `INVALID_ARGUMENT`/`FAILED_PRECONDITION`→`INVALID_ARGUMENT`; `ALREADY_EXISTS`→
  `ALREADY_EXISTS`; resto→`UNKNOWN`. `CancellationException` se propaga; no se lanzan excepciones crudas a UI.
- **Campos escritos:** exactamente `FocusSessionCloudDto` (userId, sessionId, moduleId, exerciseId, startedAtMs,
  endedAtMs, durationMs, completed, validForCalibration?, normalizedScore?, scoringVersion, appVersion?,
  sourcePlatform?, fingerprint, createdAtMs, updatedAtMs, schemaVersion).
- **Campos excluidos:** raw trials, timeline de RT, `sessionMetrics`, `subjectiveState`, email, displayName,
  country, ageRange, datos médicos/diagnóstico, prompts/resúmenes de IA, identificadores de dispositivo/
  publicidad.
- **Testabilidad:** toda la clasificación se prueba con un **fake gateway** (`FirestoreFocusSessionRemoteDataSourceTest`,
  12 casos). El **único** archivo con gitlive real (`GitLiveFocusSessionFirestoreGateway`) es **EMULATOR-GATED**:
  sus llamadas gitlive no se pueden compilar/verificar aquí (no hay Gradle/emulador); requiere
  `:androidApp:compileDebugKotlin` + verificación contra el emulador Firestore (JDK 21) antes de montar. No
  escribe si el flag está off / falta userId autenticado / DTO inválido (garantizado por el coordinador + mapper
  aguas arriba).

## 6. Rules

Detalle en `MINDBOOST_FIRESTORE_RULES_DRAFT.md` (owner read/create; cross-user bloqueado; `update`/`delete`
bloqueados; `userId == request.auth.uid`; `data.userId == userId`; `data.sessionId == sessionId`; allowlist
cerrada `hasOnly(focusSessionKeys())` que **rechaza rawTrials/campos desconocidos**; tipos básicos; `list`
acotada; App Check; sin entitlement/Elite). La allowlist coincide 1:1 con `FocusSessionCloudDto`. **No se edita
`firestore.rules`** (emulador JDK 21 = gate manual). `blockScores` queda fuera del piloto.

## 7. Feature flag y consentimiento

- `mindboost_sync_firestore_enabled` = **false** (Default local, Static, Remote Config default en
  `setDefaultsAsync`, producción). Configurable en tests (`syncEnabled = { true }` / `StaticFeatureFlags`).
- **Remote Config `true` NO basta para producción sin consent gate.** Antes de rollout real se requiere un gate
  de consentimiento GDPR explícito. No se construye pantalla de consentimiento en este turno (no hay sistema
  listo trivial de reusar); queda documentado como requisito.

## 8. Datos incluidos / excluidos

Incluidos = los 17 campos de `FocusSessionCloudDto` (§5). Excluidos = raw trials, timeline de RT completa,
email, displayName, country, ageRange, datos médicos, diagnóstico, prompts/resúmenes de IA, identificadores de
publicidad, identificadores de dispositivo invasivos, `sessionMetrics`, `subjectiveState`.

## 9. Riesgos

- **P0** — ninguno.
- **P1** — **iOS Keychain implementado (Phase 1.2), binding nativo pendiente de compilar/verificar** en macOS
  (`:shared:compileKotlinIosSimulatorArm64` + tests de simulador). La lógica pura está testeada; el interop
  nativo es best-effort. Bloqueante antes de dev rollout iOS hasta que ese gate esté verde. Ver
  `MINDBOOST_IOS_SECURE_KEY_VALUE_STORE.md`.
  No afecta a Android (durable) ni al progreso local (que ya es durable en su propio store).
- **P1** — `GitLiveFocusSessionFirestoreGateway` es código gitlive **no verificado** aquí (emulator-gated):
  `:androidApp:compileDebugKotlin` + emulador deben confirmarlo antes de montar. Aislado en un solo archivo tras
  el seam; el resto (cola, data source, clasificación) está testeado.
- **P2** — Coordinador no montado en producción (intencional; la activación es el paso dev). · Sin retry
  loop/backoff automático (Phase 2): una fila `PENDING` (durable en Android) se reintentará cuando exista el loop.
- **P3** — Reglas en borrador, sin verificar en emulador (JDK 21). · El scope de producción del coordinador
  debe ser `SupervisorScope`+handler (documentado). · `blockScores`/rehidratación multi-dispositivo diferidos.

## 10. Gates

**NO corridos** — el IDE posee el daemon Gradle; **no se marca nada como pasado**. Comandos:
```bash
:shared:desktopTest --tests "com.mindos.app.features.mindboost.core.sync.*"
:shared:desktopTest --tests "com.mindos.app.features.mindboost.focus.data.sync.*"
:shared:desktopTest --tests "*SecureSyncQueue*"   ·   "*FirestoreFocusSessionRemoteDataSource*"   ·   "*SyncingFocusSessionHistoryRepository*"
:shared:detekt   ·   :shared:mindboostCopySafetyScannerDesktopTest   ·   :androidApp:compileDebugKotlin
# emulador de reglas (JDK 21, gate manual):
npm run test:firestore-rules
```

## 11. Checklist para activar en DEV

**Android dev (posible ahora tras verificación):**
- [ ] `:androidApp:compileDebugKotlin` verde (confirma que el gitlive gateway compila).
- [ ] Emulador Firestore (JDK 21) verde con las reglas del borrador (§4 del rules draft) + behaviour del gateway.
- [ ] Desplegar las reglas del borrador a un proyecto **dev**.
- [ ] Cablear `SyncingFocusSessionHistoryRepository` en `rememberFocusGraph()` con `SecureSyncQueueRepository`,
      `FocusSessionRemoteRepository(FirestoreFocusSessionRemoteDataSource(GitLiveFocusSessionFirestoreGateway()))`,
      un `SupervisorScope` con `CoroutineExceptionHandler`→`ObservabilityHub`, `currentUserId` desde la sesión
      autenticada, y `sourcePlatform` no invasivo.
- [ ] Encender el flag SOLO en Remote Config de dev/staging. Verificar end-to-end (sesión → doc, sin raw trials).

**iOS dev — iOS verification DEFERRED until macOS/Xcode is available (CEO 2026-07-05):**
> **Do not activate real sync on iOS until iOS gates are green.** iOS Keychain implementation compile-pending
> until macOS/Xcode gate — blocker explícito, NO completado.
- [ ] `:shared:compileKotlinIosSimulatorArm64` + `:shared:compileKotlinIosArm64` verdes (compilan
      `IosKeychainBackend`).
- [ ] `:shared:iosSimulatorArm64Test` verde (verifica el Keychain real; puede requerir host con entitlement).
      Hasta ese verde, el binding nativo iOS es best-effort (la lógica pura ya está testeada en JVM). Detalle y
      fallback de emergencia (1 línea, NO dejar activo) en `MINDBOOST_IOS_SECURE_KEY_VALUE_STORE.md`.

## 12. Checklist para PRODUCCIÓN futura

- [ ] Consent gate GDPR explícito antes de la primera subida (flag OFF hasta consentir).
- [ ] Retry loop/backoff que drene `pending` desde la cola persistida (durable en Android; iOS tras Keychain).
- [ ] Roll-out gradual del flag por Remote Config; kill switch verificado.
- [ ] `isAppChecked()` en `firestore.rules` + suite de emulador verde en CI (job JDK 21).
- [ ] Revisión de privacidad/erasure end-to-end (namespace ya en el eraser).

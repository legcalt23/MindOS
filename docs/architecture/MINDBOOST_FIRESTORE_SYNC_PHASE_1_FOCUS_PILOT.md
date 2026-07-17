# MindBoost — Firestore Sync · Phase 1: Piloto Enfoque

**Fecha:** 2026-07-04 · **Autor:** Staff Mobile Engineer · **Estado:** Piloto implementado + testeado, NO activado

> **Alcance de este turno:** un piloto CONTROLADO en un único motor (Enfoque). Se implementa y testea la
> arquitectura de sync (DTO cloud-safe, mapper, remote repo, coordinador local-first, cola, feature flag,
> clasificación fail-closed). **NO** se activa sync real, **NO** se sube nada en producción, **NO** se cablea el
> coordinador al grafo vivo de Enfoque, **NO** se toca scoring/calibración/taxonomía, **NO** se migran
> namespaces, **NO** se escribe el data source real de Firestore (Phase 1.1). Verificación estática — los gates
> los ejecuta el dueño (el IDE posee el daemon Gradle).

---

## 1. Alcance

**Objetivo:** demostrar que la arquitectura de sync funciona de forma segura, testeable e idempotente en UN
motor, sin activarla para usuarios reales. No es "subir todo".

**Implementado (aditivo, sin montar en el grafo vivo):**
1. DTO cloud-safe de sesión de Enfoque (`FocusSessionCloudDto` + `FocusSessionCloudSchema`).
2. Mapper dominio→cloud (`FocusSessionCloudMapper`) fail-closed, sin raw trials, con fingerprint.
3. Remote repository (`FocusSessionRemoteRepository`) sobre un puerto (`FocusSessionRemoteDataSource`).
4. Coordinador local-first (`SyncingFocusSessionHistoryRepository`).
5. Cola de sync mínima genérica (`SyncQueueEntry` / `SyncQueueRepository` / `InMemorySyncQueueRepository`).
6. Feature flag `mindboostSyncFirestoreEnabled` (OFF por defecto, incluida producción).
7. Tests unitarios + de integración con fakes.
8. Borrador de reglas Firestore (`MINDBOOST_FIRESTORE_RULES_DRAFT.md`).

## 2. Qué se implementó — y qué NO

| Área | Implementado | No implementado (por qué) |
|---|---|---|
| Feature flag | `mindboostSyncFirestoreEnabled` en `FeatureFlags` (+ Default/Static/RemoteConfig), OFF | UI de opt-in / consentimiento (no hay patrón que reusar; es prerequisito de activación) |
| DTO cloud-safe | `FocusSessionCloudDto` (17 campos allowlisted) | Sync de `blockScores` / `scoreEvents` (Phase 1.1 / 2) |
| Mapper | `FocusSessionCloudMapper.toCloudDtoOrNull` (valida owner + id; fingerprint) | Cómputo de `normalizedScore`/`validForCalibration` (tocaría scoring → slots nullable) |
| Remote repo | `FocusSessionRemoteRepository` (create-only + probe + classify), puerto `FocusSessionRemoteDataSource` | Impl real gitlive Firestore + mapeo SDK→`RawWriteOutcome` (Phase 1.1, exige emulador/device) |
| Coordinador | `SyncingFocusSessionHistoryRepository` (local-first, best-effort remote) | Cableado en `rememberFocusGraph()` (activación → Phase 1.1) |
| Cola | `InMemorySyncQueueRepository` | Cola persistida cifrada + wiring en el eraser (Phase 1.1 — ver §Riesgos) |
| Reglas | Borrador + plan de emulador | Edición de `firestore.rules` (exige emulador JDK 21; no desplegar sin verificar) |
| Scoring / calibración / taxonomía / namespaces | — | Intactos (fuera de alcance por mandato) |

## 3. Feature flag

- **Nombre:** `FeatureFlags.mindboostSyncFirestoreEnabled` (clave Remote Config `mindboost_sync_firestore_enabled`).
- **Default:** `false` en `DefaultFeatureFlags` y `StaticFeatureFlags` → **producción OFF**.
- **En tests:** `StaticFeatureFlags(mindboostSyncFirestoreEnabled = true)`, o el lambda `syncEnabled = { true }`
  del coordinador.
- **Off ⇒** el coordinador se comporta EXACTAMENTE como el repo local (nada se sube).
- **On ⇒** solo Enfoque intenta el mirror; si el cloud falla, el entrenamiento y el guardado local siguen.
- El coordinador lee el flag por defecto vía `FeatureFlagsHub.current.mindboostSyncFirestoreEnabled`, así que
  al montarse en Phase 1.1 ya queda gobernado por el flag sin más cambios.

## 4. DTO cloud-safe (`users/{userId}/sessions/{sessionId}`)

**Incluidos (allowlist):** `userId`, `sessionId`, `moduleId` (=`"focus"`), `exerciseId`, `startedAtMs`,
`endedAtMs`, `durationMs`, `completed`, `validForCalibration?` (null en el piloto), `normalizedScore?` (null en
el piloto; `null != 0`), `scoringVersion`, `appVersion?`, `sourcePlatform?`, `fingerprint` (FNV-1a),
`createdAtMs`, `updatedAtMs`, `schemaVersion`.

**Excluidos EXPLÍCITAMENTE (nunca se suben en Phase 1):** raw trials, timeline de tiempos de respuesta, el mapa
`sessionMetrics` por-métrica, el `subjectiveState` (sueño / cafeína / ánimo — dato sensible), `blocks`,
`markers`, email, displayName, country, ageRange, cualquier dato médico o de diagnóstico, prompts/resúmenes de
IA, identificadores de dispositivo/publicidad invasivos. La minimización es por defecto: el DTO NO es una copia
bruta del `SessionResult`.

## 5. SessionId en Enfoque

El productor real (`FocusSessionStateHolder`) sella `SessionId("<userId>:<endedAtMs>")` UNA vez al terminar la
sesión y no lo regenera. Ese valor **es** un `SyncSessionId` válido (los UID de Firebase + `:` + dígitos no
tienen `/ \`, espacios ni control chars), así que el piloto lo **conserva** vía `SyncSessionId.parseOrNull` sin
tocar el modelo de dominio (criterio CEO §5). Guardas del mapper:
- id inválido como doc Firestore → `null` → no se escribe;
- el prefijo owner (`substringBefore(':')`) debe igualar el `userId` autenticado → si no, `null` (no se escribe
  la sesión de otro).

## 6. Flujo local-first (`append`)

1. `local.append(result)` — guardado local, **fuente de verdad**. Si falla, la excepción se propaga y NO se
   intenta sync.
2. Si `mindboostSyncFirestoreEnabled` está **off** → termina (idéntico a antes).
3. Si no hay usuario autenticado (`currentUserId() == null`) → termina (local ya es durable).
4. Si flag on + hay usuario → `syncScope.launch { syncNow(result, userId) }` — best-effort, **sin bloquear** el
   entrenamiento ni la UI (no se espera la red).
5. Si el remote falla, local sigue siendo la verdad; el resultado queda registrado en la cola (`PENDING`
   reintentables / `FAILED` / `CONFLICT` / `SYNCED`).

Regla central: **guardado local OK > sync cloud best-effort**, nunca al revés. Las lecturas (`recent`) siempre
vienen de local (Phase 1 no lee del cloud).

## 7. Resultados de sync (fail-closed, modelo Phase 0)

| Resultado | Cuándo | Estado en cola |
|---|---|---|
| `Synced` | escritura nueva OK | `SYNCED` |
| `AlreadySyncedDuplicate` | ya existe + remoto verificado (mismo user + mismo fingerprint) | `SYNCED` (éxito lógico) |
| `Conflict(UNVERIFIED_EXISTING)` | ya existe pero no se pudo leer/verificar | `CONFLICT` |
| `Conflict(DIFFERENT_USER)` | ya existe, otro `userId` | `CONFLICT` |
| `Conflict(DIFFERENT_FINGERPRINT)` | ya existe, contenido divergente | `CONFLICT` |
| `PermissionDenied` | denegación de permisos | `FAILED` — **jamás `SYNCED`** |
| `NetworkFailure` | fallo de transporte | `PENDING` (reintentable) |
| `ValidationFailure` | payload inválido / owner inválido | `FAILED` (no reintentar en bucle) |
| `UnknownFailure` | excepción cruda del data source | `FAILED` |

Invariante: `PERMISSION_DENIED != Synced` y `PERMISSION_DENIED != AlreadySyncedDuplicate`. Un "ya existe" solo
es duplicado tras verificación remota positiva.

## 8. Tests añadidos

Todos en `desktopTest` (parte de `:shared:check`, no escaneados por Detekt). Ver §I del mensaje de entrega para
la lista test-por-test. Cubren: mapper (cloud-safe, no raw trials, valida owner/id, fingerprint round-trip),
remote repo (los 7 resultados + excepción→UnknownFailure), coordinador (flag off/on, sin auth, fallo local
para sync, network→PENDING, permission→no-synced, duplicado verificado→éxito lógico sin segunda escritura, raw
trials nunca en el DTO, `recent` delega a local), cola (mapping fail-closed + upsert idempotente), y el flag
(off por defecto).

## 9. Riesgos restantes

- **P1** — El coordinador NO está cableado al grafo vivo; el data source real de Firestore no existe aún. El
  piloto demuestra la arquitectura pero no sincroniza de verdad hasta Phase 1.1. (Intencional.)
- **P2** — Cola in-memory: una fila `PENDING` se pierde si el proceso muere antes del reintento. **No se pierde
  PROGRESO** (el resumen local ya está persistido); solo se retrasa el mirror cloud. Cola persistida cifrada +
  wiring en `executeAccountDeletion`/`AccountBoundErasureCoverageTest` = Phase 1.1 (aprendizaje 2026-06-28: un
  namespace `AccountBound` nuevo exige actualizar el eraser).
- **P2** — Creatividad genera un `sessionId` monotónico sin `userId` (Phase 0 audit): inservible como id de
  doc per-usuario. Bloqueante para sincronizar Creatividad; no afecta a Enfoque.
- **P3** — `normalizedScore`/`validForCalibration` viajan como `null` en el piloto (no se computan aquí para no
  tocar scoring). Rellenarlos desde un score ya sellado es Phase 1.1.
- **P3** — Reglas Firestore en borrador, sin verificar en emulador (JDK 21) ni desplegar.
- **P3** — El scope de producción del coordinador debe ser un `SupervisorScope` con `CoroutineExceptionHandler`
  que reporte a `ObservabilityHub` (documentado; `syncNow` es total por construcción, así que no lanza en
  operación normal).

## 10. Checklist para activar en entorno DEV

- [ ] Implementar `FocusSessionRemoteDataSource` real (gitlive Firestore) con mapeo SDK → `RawWriteOutcome`
      (sin `try/catch` silencioso; re-lanzar `CancellationException`).
- [ ] Cablear `SyncingFocusSessionHistoryRepository` en `rememberFocusGraph()` con: `syncScope` supervisor,
      `currentUserId` desde la sesión autenticada, `clock` = `SystemEpochClock`, `sourcePlatform` no invasivo.
- [ ] Desplegar las reglas del borrador tras verde en el emulador (JDK 21).
- [ ] Activar el flag SOLO en dev/staging (`mindboost_sync_firestore_enabled = true` en Remote Config de dev).
- [ ] Verificar end-to-end: sesión de Enfoque → doc en `users/{uid}/sessions/{sessionId}`, sin raw trials.

## 11. Checklist para activar en PRODUCCIÓN (futuro)

- [ ] Consentimiento GDPR explícito antes de la primera subida (design doc §3.4). Flag OFF hasta consentir.
- [ ] `isAppChecked()` añadido a `firestore.rules` + suite de emulador verde.
- [ ] Nuevo namespace/colección cableado en `executeAccountDeletion` + `AccountBoundErasureCoverageTest`.
- [ ] Cola persistida (durable) para no perder mirrors entre procesos, con backoff.
- [ ] Creatividad corregida ANTES de incluirla; Emociones expone `sessionId` explícito.
- [ ] Roll-out gradual del flag por Remote Config; kill switch verificado.

## 12. Requisitos de consentimiento antes de producción

Sync real de datos cognitivos a la nube **no** puede activarse para usuarios reales sin consentimiento explícito
e informado. En Phase 1 el flag OFF y el coordinador no montado garantizan que **nada** sale del dispositivo.
La pantalla/gestor de consentimiento es prerequisito de la activación en producción (no se construye en este
turno; si existe un patrón de onboarding/consent, se AUDITA antes de reusarlo).

Confirmación de gates:

- `mindboost_sync_firestore_enabled = false` **by default** (Default/Static/RemoteConfig, y default declarado en
  `setDefaultsAsync`).
- **Remote Config `true` is not sufficient for production rollout without a consent gate.** Encender el flag
  remoto NO sustituye el consentimiento GDPR; ambos son necesarios antes de subir datos reales.

## 13. BLOCKER — cola durable antes de montar sync real (Phase 1.1)

> **Before wiring into the live repository graph for real users, the sync queue must be durable,
> encrypted/local-persisted where appropriate, bounded, and integrated with account deletion. The current
> in-memory queue is acceptable only for the non-mounted pilot and tests.**

Aclaraciones:

- **Phase 1 NO monta sync real** (el coordinador no está en `rememberFocusGraph()` y no existe el data source
  real de Firestore).
- La cola **in-memory NO pierde progreso local** (el resumen de sesión ya está persistido local-first), pero
  **sí puede perder el "mirror pending"** si el proceso muere antes del reintento.
- Antes de activar cloud real hay que tener **cola durable** (cifrada/persistida y acotada, integrada en
  `executeAccountDeletion` + `AccountBoundErasureCoverageTest`) **o** una política explícita de retry que
  reconstruya los pendientes desde el `SessionHistoryRepository` local.
- **Consentimiento GDPR sigue requerido** antes de producción; el feature flag remoto por sí solo **no** lo
  sustituye.

## 14. Hardening patch 2026-07-04 (cierre de Phase 1)

Cinco ajustes CEO aplicados sobre el piloto (sin montar sync, sin tocar scoring/calibración):

- **A.** El flag `mindboost_sync_firestore_enabled` se declara ahora en `setDefaultsAsync(...)` de
  `MindOSApplication.configureRemoteConfig()` (default `false`, visible y consistente con los demás flags).
- **B.** `FocusSessionCloudMapper` **valida `normalizedScore`**: `null`/`0.0`/`1.0`/`[0,1]` válidos; `NaN`,
  `±Infinity`, `<0`, `>1` → DTO `null` → `ValidationFailure` (sin clamp, sin redondeo; `null != 0` preservado).
- **C.** El mapper **rechaza sesiones temporalmente incoherentes**: `startedAtMs >= 0`,
  `endedAtMs >= startedAtMs`, `durationMs >= 0`. (`SessionResult.durationMs` es DERIVADO
  `(endedAtMs - startedAtMs).coerceAtLeast(0)`, así que para una sesión aceptada la igualdad
  `durationMs == endedAtMs - startedAtMs` se cumple por construcción; el `>= 0` es defensivo.)
- **D.** `syncNow()` es **defensivamente total**: los fallos de `queue.find`/`queue.record` se convierten en
  `UnknownFailure` (o dejan la fila en su estado previo) — nunca en un `SYNCED` fabricado; si no se puede
  registrar el `PENDING`, **no se llama al remote** (se pierde trazabilidad); un fallo al escribir el resultado
  final devuelve el outcome real del remote y deja la fila en `PENDING` (**riesgo de observabilidad/cola
  documentado**, no de corrección). `CancellationException` **siempre** se propaga; sin `catch` vacíos.
- **E.** Este blocker (§13) documenta la cola in-memory como no apta para el montaje real.

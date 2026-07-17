# SYSTEM-1 · Slice 4B — E8 (Go/No-Go) Productive Wiring — Reporte de cierre para el CEO

**Fecha:** 2026-07-10 · **Estado:** DONE & green · **Exit gate /mb:** ✅ 3/3 PASS (privacy-security · efficacy-guardian · diff-auditor) · **Camino:** A (reuso del `SecureSessionRecorder` existente), aprobado por CEO.

> Alcance respetado tal cual se autorizó: **No E9. No mutación de perfil. No cloud/Firestore. No outbox nuevo. No recovery nuevo.** El servidor (`accountDeletion.ts`) queda diferido a slice 9 (sin reclamar cobertura de borrado servidor que no existe).

---

## 1. Inventario FINAL de escrituras de E8

Tras 4B, una corrida de E8 produce **exactamente estas escrituras** — ni una más:

| # | Escritura | Store / namespace | Call-site | Momento |
|---|---|---|---|---|
| 1 | `SessionRecord` **STARTED** (write-ahead) | `SecureSessionRecordStore` → `mindboost_sessions` | `recorder.onStart(sessionId, ScoreModuleId.SPEED, startedAt, trialCount)` — [`GoNoGoSessionStateHolder.kt:141`](../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e8/GoNoGoSessionStateHolder.kt) | al iniciar la corrida (`startSession`) |
| 2 | `SessionRecord` **FINALIZED** | `mindboost_sessions` | `recorder.onFinalize(run.sessionId, completedTrials, durationActualMs)` — línea 297 | al terminar (`sealOfficialRun`, bajo mutex) |
| 3 | `SessionRecord` **ABANDONED** | `mindboost_sessions` | `recorder.onAbandon(run.sessionId)` — línea 228 | al abortar (`abort`), solo si no finalizó |
| 4 | **1× `CognitiveSessionRun`** (historial cognitivo oficial) | `SecureCognitiveSessionRunStore` → `mindboost_session_runs` | `cognitiveRunStore.save(cognitiveRun)` — línea 325 | al terminar, tras FINALIZED |

**Lo que YA NO se escribe (doble-verdad retirada):** ninguna llamada a `saveGoNoGoSummary` / persistencia de `GoNoGoTelemetrySummaryV1` queda en el path productivo (ver §5).

**No es escritura:** el `officialRun: OfficialExerciseRunAdapterResult` (líneas 250–265) es una vista **en memoria, NO persistida**, para el panel de resultado. El único registro oficial persistido es el `CognitiveSessionRun` (#4). Documentado en el propio código (líneas 250–251).

---

## 2. Archivos tocados en 4B

**Productivo (`commonMain`):**
- `speed/presentation/e8/GoNoGoSessionStateHolder.kt` — wiring del lifecycle write-ahead + sellado del `CognitiveSessionRun` + `CognitiveRunSaveState` honesto + captura inmutable por corrida (`RunLifecycle`).
- `speed/presentation/e8/E8InhibicionScreen.kt` — recibe `sessionRecordStore` + `cognitiveRunStore` y los pasa al holder (sin lógica).
- `speed/presentation/ui/SpeedRootScreen.kt` — construye `SecureSessionRecordStore(mindboost_sessions)` + `SecureCognitiveSessionRunStore(mindboost_session_runs)`, corre el `SessionRecoveryScanner` EXISTENTE (`speedSessionRecovery`, línea 70), y los inyecta a `E8InhibicionScreen`.

**Tests (`desktopTest`):**
- `speed/presentation/e8/GoNoGoProductiveWiringTest.kt` — history-consumer real, mapeo exhaustivo `SaveResult`→`CognitiveRunSaveState`, y la regresión de corridas solapadas (F1).

**Persistencia local previa (slice 4A, ya cerrada):** namespace `mindboost_session_runs` (`MindBoostStorageNamespaces.SessionRuns`) en `AccountBound` + eraser de `AppGraph` + `AccountBoundErasureCoverageTest`.

---

## 3. Flujo lifecycle STARTED / FINALIZED / ABANDONED

Reuso TOTAL del `SecureSessionRecorder` + `SessionRecoveryScanner` existentes (no se creó ningún draft/scanner nuevo).

```
startSession()  ── onStart(STARTED) ────────────────►  mindboost_sessions   (write-ahead, best-effort en scope.launch)
   │
   ├─ (juega N trials) ─► finishSession()
   │        └─ sealOfficialRun()  [NonCancellable, bajo lifecycleMutex]:
   │              1) startJob.join()          (garantiza que STARTED persistió antes de FINALIZE)
   │              2) onFinalize(FINALIZED)     ─►  mindboost_sessions
   │              3) run.finalized = true
   │              4) verdict = WriteAheadEvaluator.evaluate(resolveWriteAheadEvidence(store, sessionId), COMPLETED, started, completed)
   │              5) build 1× CognitiveSessionRun  (verdict real, no fabricado)
   │              6) cognitiveRunStore.save(...)  ─►  mindboost_session_runs
   │              7) _state.saveState = save.toE8SaveState()   (resultado honesto en UI)
   │
   └─ abort() (nav-away/reinicio) ── onAbandon(ABANDONED) ─►  mindboost_sessions   (solo si !finalized; join antes)
```

**Recovery:** huérfanos STARTED (crash/kill) los marca ABANDONED el `SessionRecoveryScanner` existente montado en `SpeedRootScreen` — no se añadió recovery propio de E8.

**Garantía de identidad (CEO):** el `CognitiveSessionRun.sessionId` es el MISMO token `e8-$seed` del `SessionRecord`. El `WriteAheadEvaluator` verifica id-match; un mismatch ⇒ `eligible=false` + flag `session_id_mismatch`. Write-ahead perdido (draft ausente) ⇒ fail-closed con flag distinto (`write_ahead_missing`), la corrida sigue en historial pero inelegible.

**Concurrencia (fix F1, ver §12):** cada corrida captura un `RunLifecycle` **inmutable** (`sessionId`+`startedAtEpochMs`+`startJob`+su propio `finalized`). Un "Repetir bloque" que arranca la corrida 2 antes de que selle la 1 ya no puede pisar el `startedAtEpochMs` de la 1 (rompería el invariante `completedAt≥startedAt`) ni silenciar el abort de la 2.

---

## 4. Modelo final de save-result

`CognitiveRunSaveState` (sealed, en `GoNoGoSessionStateHolder.kt`) — reemplaza al viejo booleano `summaryPersisted`; **un fallo/conflicto NUNCA se lee como progreso guardado**:

| `SaveResult` (store) | → `CognitiveRunSaveState` (UI) | Semántica |
|---|---|---|
| `Saved` | `Saved` | guardado real |
| `IdempotentDuplicate` | `IdempotentDuplicate` | reintento idéntico = éxito |
| `SavedIndexRepairNeeded` | `SavedIndexRepairNeeded` | primario ok, índice pendiente de reparación (visible, no silencioso) |
| `DuplicateRunConflict(runId)` | `DuplicateConflict(sessionId)` | conflicto (inmutable, no sobrescribe) — NO es "guardado" |
| `Failed(reason)` | `Failed(reason.name)` | fallo de backend — NO es "guardado" |
| (estado inicial) | `NotAttempted` | aún no se intentó |

Mapeo exhaustivo (`when` sin `else`) en `SaveResult.toE8SaveState()`. Cubierto por 6 tests (uno por variante, incl. "un fallo/conflicto no muestra progreso guardado").

---

## 5. Confirmación: `GoNoGoTelemetrySummaryV1` NO se escribe productivamente

- **Grep del path productivo:** no hay ninguna llamada a `saveGoNoGoSummary` ni a persistir `GoNoGoTelemetrySummaryV1` en `GoNoGoSessionStateHolder` / `E8InhibicionScreen` / `SpeedRootScreen`.
- El tipo `GoNoGoTelemetrySummaryV1` **permanece** como tipo/test/debug NO oficial, exactamente como autorizó el CEO (msg 4, punto 5). No se eliminó; simplemente no tiene call-site de escritura productiva.
- La única verdad cognitiva persistida de E8 es el `CognitiveSessionRun`. Doble-verdad eliminada, confirmado por el diff-auditor.

---

## 6. Fuente REAL de timebase

- **Valor emitido:** `ClockSource.SYSTEM_MONOTONIC`, `monotonicAvailable = true` — fijado en `e8TimingSnapshot(score)` ([`GoNoGoSessionStateHolder.kt:353-355`](../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e8/GoNoGoSessionStateHolder.kt)).
- E8 mide el RT con `TimeSource.Monotonic` (reloj monotónico del sistema), **NO** con Choreographer/frame-timing. Por tanto **NO es autoritativo**: `isAuthoritativeTimebase` exige `FRAME_MONOTONIC` (regla 2026-06-30).
- **Consecuencia (kill-switch):** el gate produce el reason `timebase_not_authoritative`. La corrida es visible en historial pero denegada para perfil/calibración/ranking.
- No es un literal falseado: es el clock real que E8 usa hoy. Cuando exista una capa de plataforma que verifique Choreographer+nanos, ese `ClockSource` real levantará este gate — no antes.

---

## 7. Fuente REAL de anti-gaming

- **Valor:** `antiGamingCoverageSatisfied = SpeedAntiGaming.COVERAGE.adequateFor(TimingCriticality.RT_CRITICAL)` ([`GoNoGoSessionStateHolder.kt:323`](../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e8/GoNoGoSessionStateHolder.kt)).
- `SpeedAntiGaming.COVERAGE = DetectorCoverage.PARTIAL` → `adequateFor(RT_CRITICAL)` = **false** (un módulo RT-crítico exige cobertura ADEQUATE, no PARTIAL).
- **Derivado del detector, NO un literal** (respeta la invariante 2026-06-30: el detector declara su cobertura, el consumidor la consulta). Si algún día la cobertura sube a ADEQUATE con un detector calibrado de fast-guessing (§15), este parámetro pasará a `true` sin tocar el call-site.
- **Consecuencia (kill-switch):** reason `anti_gaming_coverage_missing`. Corrida en historial, denegada en lanes oficiales.

---

## 8. Verificación de consumo de historial (`recent` / `byBlock`)

Test `a_completed_E8_run_is_visible_through_the_history_repository` (con `InMemoryCognitiveSessionRunStore` real):
- `DefaultCognitiveSessionHistoryRepository(runs).recent(10)` contiene el `sessionId` guardado.
- `.byBlock(ScoreModuleId.SPEED, 10)` = exactamente `[sessionId]`.

Test `the_E8_history_item_is_visible_but_marked_not_counted`:
- `mayCountTowardProfile / Calibration / Ranking` = **false** (los tres).
- `officialUseDeniedReasons` contiene `score_preliminary`, `timebase_not_authoritative`, `anti_gaming_coverage_missing`.

**E8 deja de ser isla de store**: existe un consumidor OFICIAL real, testeable. (El cableado a una UI de historial es slice 5+; el KDoc del test ya no sobre-afirma "no longer a store-island".)

---

## 9. Verificación de NO mutación de perfil

- El path de E8 **no toca** `MindBoostProfile`, `SpeedProfileSource`, `ModuleContribution`, ni el aggregator. Grep confirma cero imports de esos símbolos en el holder/screen/root.
- El `CognitiveSessionRun` va a su propio store (`mindboost_session_runs`) y a un read-model puro; no hay ingestor cableado (slice 6, no wired).
- La triple denegación del kill-switch (preliminary + timebase no-autoritativo + anti-gaming PARTIAL) hace estructuralmente imposible que una corrida de E8 alimente el perfil HOY, aunque el ingestor existiera.
- Sin `SyncQueue`, sin Firestore. Confirmado por privacy-security (PASS) y diff-auditor (APTO).

---

## 10. Tests y gates

**Tests (E8 productive-wiring, `desktopTest`):** 7 tests verdes —
1. corrida visible vía history repo (`recent`/`byBlock`),
2. item visible-pero-no-cuenta (3 reasons),
3. `SavedIndexRepairNeeded` → repair-pending (no "guardado" plano),
4. `IdempotentDuplicate` → estado de éxito,
5. `Failed` → nunca muestra progreso guardado,
6. `DuplicateConflict` → nunca muestra progreso guardado,
7. **regresión F1** corridas solapadas (`incrementingClock`) → ambos ids guardados, sin crash.

Los tests existentes de E8 (`GoNoGoSessionStateHolderTest`) no requirieron cambios (superficie pública intacta).

**Gates (verde salvo el ruido foráneo conocido):**
- `:shared:compileKotlinDesktop` ✅
- `:shared:compileAndroidMain` ✅
- `:shared:desktopTest` (ambas clases E8) ✅
- `:shared:detekt` — **FAILED SOLO** por los 2 findings FORÁNEOS de WIP paralelo (`IndexedSecureKeyValueStore.kt:61` TooManyFunctions · `InterSessionConfidence.kt:31` MemberNameEqualsClassName). **Cero findings de los archivos que toqué.**
- copy-scanner (commonMain + desktopTest) ✅ (`core/session/cognitive` fuera de alcance; contenido limpio igualmente).

**Exit gate /mb (contexto fresco):**
- **privacy-security: PASS** — sin PII, sin namespace nuevo, borrado intacto, vocabulario de reasons cerrado.
- **efficacy-guardian: PASS (APTO)** — kill-switch no-bypaseable, null-para-no-VALID fijado, sin doble-conteo; 2 follow-ups no bloqueantes (§11).
- **diff-auditor: APTO** — F1 (crítico) resuelto con test de regresión fiel al mecanismo original; Hallazgo 2 (KDoc) resuelto; sin defectos nuevos; identidad sessionId, mapeo `SaveResult`, retiro de doble-verdad, separación de capas y no-regresión E1–E7 confirmados.

---

## 11. Riesgos residuales y decisiones pendientes (antes de slice 6 y 9)

**Decisiones para el CEO — RESUELTAS (ver §13 para el detalle e implementación):**
- **F8 — helper de telemetría muerto.** CEO ordenó retirarlo de producción. Hecho: movido a test-only + guard
  anti-regresión (`E8ProductiveTelemetryAbsenceTest`). El tipo `GoNoGoTelemetrySummaryV1` permanece como legacy no-oficial.
- **F9 — naming.** CEO ordenó el rename cosmético. Hecho: `officialRun` → `resultView`.

**Riesgos residuales conocidos (documentados, no bloqueantes):**
1. **Cosmético UI (diff-auditor, preexistente, NO nuevo):** si la corrida 2 ya está en curso cuando el sellado tardío de la corrida 1 resuelve, `saveState` de la UI muestra transitoriamente el resultado de la 1 hasta que la 2 lo sobrescriba. No persiste dato incorrecto, no crashea. Candidato a slice futura (scoping del `saveState` por corrida), no bloqueante.
2. **Process-death entre `scope.launch` y su suspensión** pierde la escritura (idéntico al patrón best-effort preexistente; el write-ahead + recovery lo mitiga a nivel de `SessionRecord`, y el `CognitiveSessionRun` se re-derivaría solo con checkpoint/re-juego). El checkpoint transversal es trabajo posterior.
3. **`listByBlock(ScoreModuleId)` no `CognitiveModuleId`** (desviación de slice 2, pendiente de ack del CEO): el modelo sellado usa `ScoreModuleId` y `core` no debe depender del feature `profile`. El ingestor (slice 6) hará el mapeo `ScoreModuleId→CognitiveModuleId`.
4. **Cloud sigue siendo CONTRATO, no cumplido:** `SyncQueue` solo tiene impl InMemory; `accountDeletion.ts` server-side intacto. **Antes de slice 9** (Firestore rules + emulator) hay que: (a) mapper run→`FirestoreCognitiveSessionRunDto`, (b) reglas owner/create-only/immutable/list-limited, (c) ampliar `accountDeletion.ts` al namespace `mindboost_session_runs` con su test de cobertura — sin reclamar borrado servidor hasta demostrarlo.
5. **Antes de slice 6 (ingestor):** el ingestor debe seguir excluyendo no-COMPLETED / compromised / `!profileEligible` / purpose no elegible, y NO cablearse al aggregator vivo. Con el kill-switch actual, toda corrida de E8 le llega inelegible — el ingestor no debe "rescatar" ninguna.
6. **iOS no verificable en Windows** (commonMain-only mitiga; verificación diferida a CI macOS, no falseada).

---

## 12. Nota del fix F1 (bloqueante crítico, resuelto en el exit gate)

El diff-auditor encontró una regresión de concurrencia CRÍTICA en la primera pasada: `sealOfficialRun` leía el campo mutable de instancia `startedAtEpochMs` dentro de una corrutina async. Un "Repetir bloque" rápido podía sobrescribir ese campo antes de que la corrutina de sellado de la corrida previa lo leyera → `startedAt(corrida 2) > completedAt(corrida 1)` → violación del invariante `CognitiveSessionRun.completedAt ≥ startedAt` → `IllegalArgumentException` sin capturar dentro del `launch`. Efecto secundario: un flag `finalized` compartido dejaba que una corrida 1 de sellado tardío silenciara el abort de la corrida 2.

**Fix:** se eliminaron los campos mutables de instancia (`startedAtEpochMs`, `lifecycleStarted`, `lifecycleFinalized`, `startJob`) y se introdujo un objeto `RunLifecycle` **inmutable por corrida** (captura `sessionId` + `startedAtEpochMs` + `startJob` + su propio `finalized`), enhebrado por `finishSession`/`sealOfficialRun`/`abort`. Cada corrida lee SU propio `startedAtEpochMs` inmutable → el cruce es estructuralmente imposible. Test de regresión `repeating_a_block_before_the_previous_save_settles_saves_both_runs_without_crashing` (reloj incremental) reproduce fielmente el mecanismo original y pasaría en rojo contra el código pre-fix. Re-verificado en contexto fresco por el diff-auditor: **APTO**.

---

## 13. Post-aprobación — cleanup CEO (2026-07-10)

Tras aprobar 4B, el CEO ordenó F8, F9, la micro-auditoría `e8-$seed` y registrar un backlog. Resultado:

**F8 — helper de telemetría muerto → test-only.** Se movió `GoNoGoTelemetryStoreExt.kt` (write helper
`saveGoNoGoSummary` + read helpers) de `commonMain` a `desktopTest` (mismo paquete). Ahora producción (`:shared`
commonMain) **no puede** referenciar `saveGoNoGoSummary` (garantía de COMPILACIÓN). El tipo `GoNoGoTelemetrySummaryV1`
permanece en commonMain como legacy/no-oficial (autorizado). Guard anti-regresión: `E8ProductiveTelemetryAbsenceTest`
escanea los fuentes productivos de E8 (holder + screen + `SpeedRootScreen`) y falla el build si reaparece
`saveGoNoGoSummary` / `GoNoGoTelemetrySummaryV1` / `GoNoGoTelemetryStoreExt` / `ExerciseTelemetryStore`.

**F9 — rename.** El campo de UI `officialRun` → `resultView` (más su local y su ref de test). No sonaba a lo que es:
una vista de resultado en memoria NO persistida. Sin tocar scoring ni persistencia. El tipo `OfficialExerciseRunAdapterResult`
queda igual (fuera del alcance cosmético de bajo riesgo).

**Micro-auditoría `e8-$seed` → reemplazado por `SessionIdGenerator` inyectado.** Hallazgo: el `seed` era
`clock.nowMillis() + runSeq`, con `runSeq` reiniciándose a 0 por instancia del holder ⇒ dos entradas a E8 en el mismo
milisegundo de reloj producían el MISMO `sessionId` (`"e8-<mismoMs>"`) ⇒ **colisión silenciosa / falsa idempotencia**
en el run store. Además el seed se compartía con el RNG de estímulos (acoplamiento de contenido). Fix (regla CEO: "puede
repetirse ⇒ reemplazar por `SessionIdGenerator` inyectado"): se inyecta `SyncSessionIdGenerator = DefaultSyncSessionIdGenerator()`
(reuso del generador de la capa sync — sufijo aleatorio que desambigua arranques en el mismo ms, Firestore-safe, testeable
vía `suffixSource`); `sessionId = generator.newSessionId("e8", startedAt).value`. El seed del RNG se DESACOPLA (`rngSeed`
local, solo randomización de estímulos). `SessionRecord.sessionId == CognitiveSessionRun.sessionId` se mantiene. Tests
(`GoNoGoSessionIdentityTest`): dos sesiones mismo-config/mismo-ms → ids distintos; repetir bloque en el mismo ms → no
reutiliza id; dos sesiones nuevas → 2 entradas en historial (una sesión nueva nunca es un duplicado idempotente).

**Gates (cleanup):** compileKotlinDesktop ✅ · desktopTest (E8 + identity + absence + telemetry persistence) ✅ ·
compileAndroidMain ✅ · copy-scanner ×3 ✅ · detekt = solo los 2 findings FORÁNEOS (el `LongParameterList` del 7º
param — el generador inyectado — se resolvió con un `@Suppress` localizado y justificado en el constructor DI, per
ratchet CLAUDE.md §3).

## 14. Backlog BLOQUEANTE MVP (CEO-ordenado 2026-07-10, con owner)

- **`finalized_without_cognitive_run` — detección / recuperación.** Si existe un `SessionRecord` **FINALIZED** pero
  NO su `CognitiveSessionRun` (process-death entre `recorder.onFinalize` y `cognitiveRunStore.save`, o un `save`
  fallido tras el finalize), la app **NO debe fingir que el historial cognitivo está completo**: esa sesión debe
  representarse como **RECUPERADA/INCOMPLETA**, nunca como pérdida silenciosa de progreso. Requiere un pase de
  reconciliación FINALIZED↔runs + su representación en el historial ⇒ **NO trivial**, por eso NO se implementa en 4B.
  **Owner:** operador SYSTEM-1 (Claude Code), como slice pre-MVP posterior a slice 6 y antes del cierre de SYSTEM-1.
  **Bloquea MVP.**

## Cierre

Slice 4B está **DONE & green** con el exit gate /mb 3/3 PASS, y el cleanup post-aprobación (F8/F9/`e8-$seed`) aplicado
y verde. E8 persiste **una** `CognitiveSessionRun` oficial por corrida, con lifecycle write-ahead reusado, save-result
honesto, `sessionId` opaco/colisión-resistente, timebase y anti-gaming fail-closed derivados (nunca literales `true`),
historial consumible verificado, y CERO mutación de perfil / cloud / outbox / recovery nuevos. La doble-verdad de
telemetría se retiró del path productivo y su helper de escritura ya no es accesible desde producción.

Con 4B cerrado y el cleanup aplicado, procedo a **slice 6 (ingestor contract, puro, no cableado)** bajo las reglas del
CEO (excluir non-COMPLETED / compromised / !elegible / preliminary-lowConfidence-non-VALID; `ScoreModuleId→CognitiveModuleId`
fuera de core; sin aggregator vivo, sin mutar `MindBoostProfile`, sin rank/calibración viva, sin Firestore/SyncQueue/E9/outbox).
E9 permanece BLOQUEADO hasta que SYSTEM-1 cierre.

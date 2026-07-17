# FASE 2.2 — Write-ahead / checkpoint / ABANDONED · FASE-0 (reconocimiento + diseño)

> Fecha: 2026-07-05 · Rama: `feat/mindboost-enfoque-integration` · HEAD tras 2.1: `6052eaa`
> Estado: **PLAN — NO IMPLEMENTADO.** Este documento es el entregable de FASE-0; no se ha tocado ningún holder.
> Base: 2.1 (`SessionRecord` + `SessionRecordStore` + `SecureSessionRecordStore` + namespace `mindboost_sessions`
> en `AccountBound` + eraser) ya en HEAD y verde.

## Objetivo

Registrar cada sesión de entrenamiento de forma canónica y **write-ahead** para que (a) una sesión iniciada
nunca se pierda, (b) una sesión finalizada nunca se marque mal, y (c) ningún bloque cognitivo estable se
degrade por añadir persistencia. Todo **aditivo**, sin tocar scorers ni fórmulas.

---

## 1. Inventario de los 8 bloques

Leídos en detalle: **Enfoque** y **Emociones** (completos). Resto: mapeados por firma de ciclo de vida
(constructor + `start`/`finish`/`abort`/`onExit` + repos). Confianza indicada por bloque.

| Bloque | Holder | Inicio | Checkpoint natural | Finalización | Abandono | Lifecycle propio hoy | Toca scorer en finalize | Riesgo regresión |
|---|---|---|---|---|---|---|---|---|
| **Enfoque** | `FocusSessionStateHolder` | `start()`→`runSession()` | fin del único bloque de trials | `finishSession()` | `abort()` (no persiste) | No | **Sí, alto** (assembler+adaptive+`historyRepository.append`+`profileRepository.save`+`personalizedScorer`) | **CAUTION** |
| **Emociones** | `EmotionSessionStateHolder` | `start()`→`runSession()` | fin de cada bloque (2×10) | `finalize(COMPLETED)` | `onExit()`→`finalize(ABANDONED)` **ya persiste** | **Sí** (`SessionEnd{COMPLETED,SAFETY_STOP,ABANDONED}` + `saveSessionSummary(abandoned=…)`, todo `runCatching`) | Sí (assembler+repo, total) | **CAUTION (dedup)** |
| **Velocidad** | `SpeedSessionStateHolder` | `startSession()` | fin de bloque | `finish`/dashboard | `abort` (verificar) | No | Sí (SpeedMeasurementStore) | **GO** |
| **Lógica** | `LogicStateHolder` | `startSession()`→`scope.launch` | fin de secuencia | `finishSession()`→`scope.launch` | `abort` (verificar) | No | Sí (repo) | **GO** |
| **Creatividad** | `CreativitySessionStateHolder` | `startExercise(id)` | fin de ronda | seal→`SecureCreativitySessionStore` | exit (verificar) | Parcial (persiste sesión al sellar) | Sí (rubric) | **GO** |
| **Memoria-span** | `MemorySessionStateHolder` | `start(id)`→`runSession(id)` | fin de bloque span | `finish()` | `abort()` (no persiste) | No | **Sí, alto** (`profileRepository.save`+`sessionHistoryRepository.append`+`recentItemRegistryRepository.save`+`deferredRecallRepository.save`+`MemoryScorer`) | **CAUTION** |
| **Memoria-diferido** | `DeferredRecallSessionStateHolder` | `start()`→`runSession()` | test único | fin de recognition | `abort()` (no persiste) | No | Sí (`DeferredRecallScorer`, `deferredRecallRepository`) | **GO** |
| **Lenguaje** | `LanguageSessionStateHolder` | `start(exercise)` | fin de ronda | — (efímero) | backToMenu descarta | **No persiste nada** (menuMode, `packets` en memoria; deferral 1.3) | No wired | **NO-GO (diferir)** |
| **Decisiones** | `DecisionsFlowStateHolder` (+ CheckIn/Bayes/Reversibility/Result) | entrada al flujo | fin de cada etapa | seal→`SecureDecisionSessionRepository`/`SecureDiaryRepository` | salir sin sellar (no escribe) | **Sí, propio** (flujo multi-etapa que sella) | Sí (Bayes/Reversibility) | **CAUTION** |

Observaciones estructurales:
- Casi todos reciben un `*Graph` DI + `userId` + `scope` + `seed`. Punto de inyección natural: **param con
  default no-op** en el constructor del holder (o en el `*Graph`), poblado por el `Remember*StateHolders`.
- **Enfoque** deriva su `sessionId` de `endedAtMs` (solo conocido al finalizar) → el write-ahead necesita su
  **propio** id estable generado al inicio (no reusar el `SessionId` interno del scorer).
- **Emociones** y **Decisiones** ya tienen un ciclo de vida propio (COMPLETED/ABANDONED / seal). Ahí el
  `SessionRecord` debe **mapear el lifecycle existente**, no crear un segundo detector (riesgo de duplicación).

---

## 2. Clasificación de riesgo

- **GO (aditivo seguro):** Velocidad, Lógica, Creatividad, Memoria-diferido. Ciclo `start`/`finish` claro,
  acoplamiento moderado, sin lifecycle propio que duplicar; los hooks son puramente aditivos.
- **CAUTION (cambios mínimos en bloque sensible):**
  - **Enfoque** y **Memoria-span** — bloques con `finish` complejo (multi-repo + scorer). Aditivo, pero exige
    tests golden antes/después y que el recorder sea **total** (nunca rompe el finalize).
  - **Emociones** — GO block que **ya** persiste abandono. Integración = mapear su `finalize(SessionEnd)` a un
    `SessionRecord`, sin nuevo detector (dedup).
  - **Decisiones** — flujo multi-etapa que ya sella. `SessionRecord` a nivel de flujo (STARTED al entrar,
    FINALIZED al sellar); no por etapa. Mini-diseño propio.
- **NO-GO / DIFERIR:** **Lenguaje** — la sesión no persiste nada en producción y no está cableada al perfil
  (deferral 1.3). Escribir `SessionRecord` aquí sería registrar un flujo efímero/demo, sin valor y sugiriendo
  que el flujo es "real". Se difiere hasta que Lenguaje tenga persistencia/banco reales.

---

## 3. Diseño técnico propuesto (aditivo)

**Colaborador transversal `SessionRecorder`** (interfaz, contrato **total**: nunca lanza salvo
`CancellationException`; errores de storage se tragan + son observables — igual que `SyncQueueRepository`):

```
interface SessionRecorder {
    suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int)
    suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long)
    suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long)
    suspend fun onAbandon(sessionId: String)   // best-effort; la autoridad es el recovery scan
}
object NoOpSessionRecorder : SessionRecorder { /* no-ops */ }              // default en todos los holders
class SecureSessionRecorder(store: SessionRecordStore, clock: EpochClock) : SessionRecorder { /* upsert */ }
```

- **Inyección:** cada session holder gana un `recorder: SessionRecorder = NoOpSessionRecorder` (**último param,
  default**), poblado por su `Remember*StateHolders` con `SecureSessionRecorder(SecureSessionRecordStore(sessionsStore), clock)`.
  Patrón idéntico a `FrameTimingProbe` (2026-06-28): cero impacto en tests existentes (default no-op).
- **Sin cambios en scorers, fórmulas ni UI** (salvo `KeepScreenOn`, ver §5). El holder solo añade 3-4 llamadas.
- El holder posee un `private var currentSessionId: String?` (generado al inicio, estable toda la sesión).

---

## 4. Semántica de sesión

- **STARTED:** al arrancar `runSession` (tras conocer `startedAtMs` y `plannedTrials`). El holder genera
  `currentSessionId` **estable** (p. ej. `"$userId:$startedAtMs"` o UUID inyectado) → `recorder.onStart(...)`.
- **CHECKPOINT:** al **fin de cada bloque de trials** (no por trial): actualiza `completedTrials` +
  `activeDurationMs`. Bloques mono-bloque (Enfoque/Deferido) → 1 checkpoint tras el loop.
- **FINALIZED:** al completar `finish`/`finalize` con éxito → `recorder.onFinalize(...)`.
- **ABANDONED:** autoridad = **recovery scan** al arranque (huérfano STARTED/CHECKPOINT → ABANDONED). `abort()`/
  `onExit()` llaman `onAbandon` best-effort (fire-and-forget) pero NO son la fuente de verdad (cubre crash/kill).
- **Campos por transición:** STARTED fija id/block/startedAt/plannedTrials; CHECKPOINT y FINALIZED actualizan
  `completedTrials`/`activeDurationMs`/`updatedAtMs`/`status`; ABANDONED solo cambia `status`+`updatedAtMs`.
- **No duplicar sesiones:** `upsert` por `sessionId` (write-ahead) → todas las transiciones actualizan la misma
  fila. Id generado una sola vez.
- **App close/crash/offline:** la fila STARTED/CHECKPOINT queda en disco; el proceso muere; el recovery la marca
  ABANDONED al siguiente arranque en frío. Offline es irrelevante (local-first; el sync es otro workstream).
- **No marcar abandonada una finalizada:** `onFinalize` sobrescribe la fila a FINALIZED (mismo id) → no queda
  huérfano; el recovery solo toca STARTED/CHECKPOINT. Riesgo residual: si `onFinalize` FALLA (storage) tras una
  sesión completada, la fila STARTED persiste y el recovery la marcaría ABANDONED (falso). Mitigación: recorder
  total + el resumen del módulo se guarda por su cuenta; se documenta como edge raro.

---

## 5. Estrategia de write-ahead

- **Frecuencia:** por **bloque** (STARTED + 1 CHECKPOINT por bloque + FINALIZED). Enfoque = 3 escrituras;
  Emociones = 4; Memoria-span = STARTED+n+FINALIZED. **Nunca por trial ni en el frame/tap path.**
- **Hilo:** en la coroutine de sesión, entre bloques; la escritura durable puede ir en `Dispatchers.Default`.
- **Tamaño:** `SessionRecord` serializado es <1 KB (9 campos escalares). Sin trials crudos ni texto libre
  (canario de minimización de 2.1 lo enforza).
- **Evitar escrituras excesivas:** el cap por-bloque ya lo garantiza; un mono-bloque hace 3 upserts totales.

---

## 6. Estrategia de recuperación

- **`SessionRecoveryScanner` (puro):** al arranque (hub/app), `store.all().filter { it.status in {STARTED,
  CHECKPOINT} && (now - it.startedAtMs) > GRACE_MS }.map { it.copy(status = ABANDONED, updatedAtMs = now) }`
  → `store.upsert(...)`. Reloj inyectado (`EpochClock`), no `Clock.System`.
- **Criterio temporal:** MindBoost no reanuda sesiones (cada `start` es fresco), así que un huérfano STARTED/
  CHECKPOINT tras un reinicio en frío = abandonado. `GRACE_MS` (p. ej. 2 min) evita marcar una fila recién
  escrita por una sesión viva en el mismo proceso. Ejecutado al entrar al hub de MindBoost (una vez).
- **Automático vía scanner** (no worker de fondo): barrido idempotente al abrir MindBoost.
- **Test:** con reloj y store en memoria: huérfano viejo → ABANDONED; huérfano dentro de la gracia → intacto;
  FINALIZED → intacto; STARTED reciente de sesión viva → intacto.

---

## 7. Tests previstos (antes de implementar)

Núcleo (infra):
- `onStart` crea STARTED con block/plannedTrials/startedAt correctos.
- `onCheckpoint` actualiza `completedTrials`/`activeDurationMs` sin cambiar `startedAtMs`.
- `onFinalize` marca FINALIZED (misma fila, sin duplicar).
- `onAbandon` marca ABANDONED.
- recorder **total**: un store que lanza no propaga (excepto `CancellationException`).
- recovery: huérfano viejo→ABANDONED; dentro de gracia→intacto; FINALIZED→intacto; sin duplicar.
- **no se sobrescribe una FINALIZED** (recovery la ignora).
- **canario de minimización** (heredado de 2.1): sin trials crudos/texto libre.
- **erasure**: `mindboost_sessions` sigue cubierto (ya pineado en `AccountBoundErasureCoverageTest`).

Por bloque (al cablearlo): iniciar→STARTED; completar→FINALIZED con `completedTrials` real; matar a mitad
(no llamar finalize)→recovery marca ABANDONED y **el perfil/scorer no cambió**; el test de scoring del bloque
sigue verde (golden antes/después en Enfoque y Memoria-span).

---

## 8. Riesgos

- **Romper bloques GO (Enfoque/Emociones):** mitigado con inyección default-no-op (tests intactos), recorder
  total (no rompe `finish`), y golden characterization antes/después en los `finish` complejos.
- **Duplicar lifecycle con Emociones/Decisiones:** Emociones ya tiene COMPLETED/ABANDONED; Decisiones ya sella.
  Se **mapea** su lifecycle existente al `SessionRecord` en vez de crear un segundo detector.
- **Duplicación conceptual con `SyncQueue`:** `SessionRecord` = qué ocurrió; `SyncQueueEntry` = cómo se sincroniza.
  Comparten solo `sessionId`. Ya separados en 2.1 por diseño.
- **Escrituras excesivas:** cap por-bloque (no por-trial), fuera del frame path.
- **Scoring incoherente:** cero cambios a scorers/fórmulas; el recorder no lee ni escribe score.
- **Falso abandono:** solo si `onFinalize` falla tras completar (edge raro); documentado.
- **Pérdida de sesión:** el write-ahead STARTED cubre crash/kill; el resumen del módulo sigue siendo su fuente.
- **Tocar demasiado código:** micro-commits por bloque; Lenguaje diferido; Decisiones en mini-diseño aparte.

---

## 9. Resultado de FASE-0

- **Estado:** ✅ **READY** (con Lenguaje DIFERIDO y Decisiones como mini-diseño separado dentro de la fase).
- **Archivos que probablemente tocaría** (por micro-commit, aditivo):
  - Infra: `core/session/SessionRecorder.kt` (nuevo: interface + NoOp + Secure), `core/session/SessionRecoveryScanner.kt` (nuevo) + tests.
  - Por bloque: su `*SessionStateHolder.kt` (+3-4 llamadas, +1 param default), su `Remember*StateHolders.kt` (pasa el recorder real) y a veces su `*Graph`.
  - Recovery: cableado en el hub de MindBoost + `KeepScreenOn` expect/actual (window FLAG) si se confirma localizado.
- **Bloques de mayor riesgo:** Enfoque, Memoria-span (finish complejo), Emociones/Decisiones (dedup lifecycle).
- **Plan de implementación por micro-commits** (cada uno con gates verdes, en serie):
  1. **MC0 — infra:** `SessionRecorder` (interface+NoOp+Secure) + `SessionRecoveryScanner` puro + tests. Sin cablear holders.
  2. **MC1 — piloto GO:** cablear UN bloque de bajo riesgo (Velocidad o Lógica) end-to-end + test kill→ABANDONED. Valida el patrón.
  3. **MC2-MC4 — resto GO:** Creatividad, Memoria-diferido, y el otro de MC1 — uno por commit.
  4. **MC5 — Enfoque (CAUTION):** golden antes/después + hooks.
  5. **MC6 — Memoria-span (CAUTION):** golden + hooks.
  6. **MC7 — Emociones:** mapear `finalize(SessionEnd)`→`SessionRecord` (dedup, sin detector nuevo).
  7. **MC8 — recovery + KeepScreenOn:** scanner al abrir el hub + FLAG de pantalla.
  8. **Decisiones:** mini-diseño de `SessionRecord` a nivel de flujo (STARTED al entrar / FINALIZED al sellar) — commit propio.
  9. **Lenguaje:** DIFERIDO (doc), sin código.
- **Gates por micro-commit:** `:shared:desktopTest` + `:shared:detekt` (mis archivos limpios) + `:shared:compileAndroidMain` (holders/graphs); + los tests existentes del bloque tocado verdes.
- **Recomendación final:** ejecutar MC0 primero (infra pura, cero riesgo de bloque), luego el piloto GO (MC1)
  para validar el patrón end-to-end antes de tocar los bloques sensibles. Enfoque/Memoria-span solo tras el
  piloto y con golden tests. Emociones/Decisiones por mapeo del lifecycle existente. Lenguaje diferido.
```

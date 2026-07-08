# MC9-D-R — E8 · Official Run Integration & Timing Integrity

Fase de INTEGRIDAD sobre E8 (commit base `e53ff405`): cierra los agujeros de integración oficial, semántica de
score, durabilidad, lifecycle y timing **sin reescribir E8, sin migrar `TrialRecord`, sin AI Coach, sin ranking/PvP,
sin Firestore sync y sin empezar E9**.

## Preflight — ¿era E8 una isla? (truth table)

| Pregunta | Antes | Ahora |
|---|---|---|
| ¿Produce una sesión oficial compatible? | NO (solo telemetry summary) | **SÍ, como run-equivalente** (`OfficialExerciseRunAdapterResult`) |
| ¿Se guarda en el mismo store histórico que E1-E7 (`SpeedMeasurementStore`)? | NO (store de telemetría, mismo namespace `mindboost_speed`, prefijo `tele:`) | Igual (deferred; ver abajo) |
| ¿Aparece en el post-sesión estándar? | NO (el dashboard usa `SessionMetrics`+`SpeedScoreEngine`, RT-tap) | E8 tiene su propio post-sesión + run oficial equivalente |
| ¿sessionId / durationActual / trials / scoreVersion / exerciseVersion? | Parcial (solo sessionId) | **Completo** (durationActual real, plannedTrials/completedTrials, scoreVersion, exerciseVersion) |
| ¿Puede alimentar calibración futura? | No cableado | Preparado por el adapter; contribución **DIFERIDA** |
| ¿Exportable/debuggeable? | Parcial (debug export = `MeasurementRun`, que E8 no produce) | Deferred (documentado); el run-equivalente es serializable |
| ¿Se borra con delete user data? | SÍ (namespace-wide `clearNamespace()`) | SÍ (sin cambios) |

**Veredicto:** E8 era una **isla parcial**: persistía y se borraba correctamente, pero NO producía un objeto de
sesión oficial ni exponía duración/versiones en forma oficial. Cerrado con un adapter (no forzando `MeasurementRun`).

## ¿Por qué un adapter y no `MeasurementRun`?
`TrialRecord` exige un `responseUptimeNanos` REAL por trial y una semántica RT-tap de `isCorrect`. Una **inhibición
NO-GO correcta NO tiene respuesta** (no hay `responseUptimeNanos`) y su corrección se **invierte** (no responder =
correcto). Forzar E8 a `MeasurementRun`/`TrialRecord` obligaría a **fabricar** timestamps de respuesta o a mentir
sobre `isCorrect`. Por eso se crea `OfficialExerciseRunAdapterResult`: un run-equivalente neutral que lleva
**comisiones y omisiones EXPLÍCITAS**, sin esconderlas.

## Mapping: domain outcomes → run oficial → telemetry summary (UNA sola verdad)
Ambos derivan de los **mismos outcomes** vía el mismo `GoNoGoRunSummary` (probado):

```text
outcomes (holder)
  └─ GoNoGoRunSummary.from(outcomes)
       ├─ GoNoGoTelemetrySummaryV1.of(...)          → persistido (tele:)
       └─ GoNoGoOfficialRunAdapter.toOfficialRun(...) → OfficialExerciseRunAdapterResult (state.officialRun)
```

`OfficialExerciseRunAdapterResult`: sessionId, exerciseId, exerciseVersion (`CONFIG_VERSION`), scoreVersion
(`GONOGO_SCORE_VERSION`), plannedTrials, completedTrials, durationActualMs, totalTrials, goTrials, noGoTrials,
correctTrials, incorrectTrials, **omissions**, **commissions**, anticipations, lateResponses, accuracy (overall),
rtMedianMs/rtMeanMs/rtCv, anticipationRate/lateRate/commissionRate/omissionRate (gated → `null` si no computable),
preliminaryScore (**`null` en CALIBRATING**, nunca 0 fabricado), lowConfidence, anomalyFlags (de diagnostics),
telemetrySummarySessionId (= sessionId), `contributesToOfficialProfile = false` (DIFERIDA).

La persistencia añade a `GoNoGoTelemetrySummaryV1` (v`1.1.0-mc9d-r`) `plannedTrials`/`completedTrials`/
`durationActualMs`, así que el run-equivalente es reconstruible desde el registro persistido.

**Integración con el store oficial (`SpeedMeasurementStore`): DIFERIDA.** Ese store solo persiste `MeasurementRun`
(RT-tap). Un historial unificado de ejercicios que acepte el run-equivalente (o un nuevo tipo de run) es trabajo
transversal futuro; hoy el run-equivalente vive en el estado de fin de sesión y sus campos de lifecycle se persisten
en el telemetry summary.

## Timing state machine (garantías, probadas en JVM)
- GO + tap ≤ deadline ⇒ correct · GO + sin tap ⇒ omission · GO + tap > deadline (dentro de la ventana=deadline+gracia)
  ⇒ late · GO + tap en foreperiodo ⇒ anticipation.
- NO-GO + sin tap ⇒ correct inhibition · NO-GO + tap post-onset ⇒ commission · NO-GO + tap en foreperiodo ⇒
  anticipation (no comisión post-onset).
- **Doble tap ignorado:** tras resolver, la fase pasa a Feedback; un segundo tap (guard `phase == Stimulus`) es no-op
  (probado).
- **Late/ignored fuera de ventana:** la pantalla espera `deadline + LATE_GRACE_MS`; un tap posterior a la ventana
  llega en fase Feedback y se ignora (documentado).
- **Reset/abort:** `abort()` vuelve a Idle SIN persistir (una corrida incompleta NO es dato; probado).

### Lifecycle — device QA PENDIENTE (no testeable en JVM)
`nav-away`/`back` a mitad de corrida abandona el holder sin persistir (finishSession solo corre al completar) — no
hay run oficial falso. Riesgo conocido: **app en background durante un trial NO-GO** — el `delay` del bucle sigue
contando en wall-clock y podría resolver la inhibición como correcta aunque el usuario no estuviera mirando. El
holder puro no puede detectar el background del SO; esto requiere un hook de lifecycle Android (LifecycleObserver)
que invalide el trial en curso. **Diferido a device QA**; no se añade un hook sin call-site vivo (evitar control
muerto).

## Scoring caps (anti-degenerado, probado)
tap-all ⇒ index < 0.35 (cap de comisión) · tap-none ⇒ index < 0.3 (cap de precisión GO) · fast GO + alta comisión ⇒
bajo · NO-GO perfecto + cero respuesta GO ⇒ bajo · alta anticipación ⇒ cap · muestra NO-GO baja ⇒ lowConfidence ·
insuficiente ⇒ `index = null` (CALIBRATING). `inhibitoryControlIndex` = precisión balanceada (tap-all/tap-none ⇒ 0.5),
pero el **score final** aplica techos duros multiplicativos.

## Persistence semantics
`telemetryVersion = summary.summaryVersion`; `metricsVersion` dentro del JSON; helpers tipados (sin parseo manual
fuera); `syncStatus = LOCAL_ONLY`; idempotente por `userId::sessionId`; corrupt JSON / wrong-exercise ⇒ null;
**delete user data** (namespace-wide) y borrado por usuario cubren E8. Guardado best-effort (`scope.launch` al
terminar): riesgo de pérdida si el proceso muere entre fin y guardado (idéntico a E5); idempotencia probada.
**Firestore sync: NO implementado.**

## Analytics readiness (contrato futuro — DOC ONLY, no existe surface de analytics en Velocidad)
Eventos: `exercise_started`, `exercise_completed`, `exercise_abandoned`, `session_completed`, `score_updated`,
`session_save_failed`. Campos E8: `goTrials`, `noGoTrials`, `commissionRate`, `omissionRate`, `anticipationRate`,
`lateRate`, `preliminaryScore`, `diagnosticFlags` (= `anomalyFlags`). Todos derivables de
`OfficialExerciseRunAdapterResult`. NO se implementa (no hay sink de analytics y sería scope creep).

## UX / copy limits
La UI ya funciona como informe de rendimiento: instrucciones explican avance/freno y que **contener la respuesta
también es correcto** y que **tocar todo no sirve**; el panel de resultados muestra aciertos GO, inhibiciones
correctas, respuestas en freno, avances sin responder, control balanceado e índice preliminar; el feedback prioriza
la señal que falló con acción concreta. Copy operativo, sin lenguaje clínico/diagnóstico/IQ/humillación ni etiqueta
personal ("respondiste en señales de freno", no "eres impulsivo"). Scanner de copy verde.

## Semántica "score-ready"
Corregida: E8 es **telemetry-ready** + **preliminary-score-ready**; **contribución al perfil oficial: DIFERIDA**;
**MVP-candidate**; **MVP-ready completo: NO** (Android/iOS/Firestore/analytics pendientes).

## Gates
`compileKotlinDesktop` ✅ · targeted E8 + adapter + holder ✅ · targeted E5 telemetry ✅ · `desktopTest` **3205/0/0**
✅ · `compileAndroidMain` ✅ · `detekt` ✅ (solo 2 findings foreign untracked: `IndexedSecureKeyValueStore:61`,
`InterSessionConfidence:31`) · copy scanners ×3 ✅ · `scan:secrets` ✅.
**Android runtime NO probado · iOS NO probado · Firestore NO probado.**

## Riesgos residuales
1. Background durante NO-GO no invalida el trial (device QA pendiente; hook de lifecycle Android no cableado).
2. Integración con el store de historial OFICIAL (`SpeedMeasurementStore`) diferida (hoy solo `MeasurementRun`).
3. Telemetría/score local-only (sin Firestore) y guardado best-effort.
4. Score PRELIMINAR, contribución al perfil DIFERIDA (§15 físico + analytics abiertos).

## GO/NO-GO para E9
**GO.** E8 ya no es una isla: produce un run oficial equivalente honesto (derivado de los mismos outcomes),
con versiones/duración, timing verificado y persistencia auditada. No iniciar E9 sin autorización explícita.

# MC9-D-R2 — E8 · Go/No-Go Velocity: Quality Maximization Audit

Auditoría adversarial (verificada contra código, no contra claims) de E8 tras MC9-D-R, con revisión de contexto
fresco (subagente `diff-auditor`) como gate de salida no negociable. Se corrigieron los P1/P2/P3 encontrados; los
transversales quedan como backlog explícito. **Sin migrar `TrialRecord`, sin AI Coach, sin ranking/PvP, sin
Firestore, sin foreign-WIP, sin empezar E9.**

## Fase 1 — Truth table (claims de MC9-D-R vs código)

| Claim | Estado | Evidencia | Riesgo |
|---|---|---|---|
| Adapter oficial existe y deriva de los mismos outcomes que el summary | VERIFIED | `GoNoGoOfficialRunAdapter.toOfficialRun`; `finishSession` computa ambos del mismo `GoNoGoRunSummary`; tests de consistencia | — |
| Velocidad no compensa comisiones (tap-all/tap-none bajo) | VERIFIED | caps en `GoNoGoScorer`; `GoNoGoScoreTest` tap-all<0.35 / tap-none<0.3 | — |
| Sin métricas fabricadas (Measured/Unavailable; score null en CALIBRATING) | PARTIAL→**VERIFIED tras fix** | El screen tenía `onset ?: 0L` (RT fabricado) → corregido; scorer ahora capa RT imposibles | Cerrado |
| Diagnostics gatean el score | FALSE→**VERIFIED tras fix** | Antes `score(metrics)` sin diagnostics; ahora `score(metrics, diagnostics)` capa `impossiblyFastTrials>0` | Cerrado |
| `accuracy` es honesta | PARTIAL→**VERIFIED tras fix** | Era pooled (≈0.75 en tap-all); renombrada `pooledAccuracy` + añadida `balancedAccuracy` | Cerrado |
| Producción usa dificultad única | FALSE→**doc corregido** | Producción usa `mixedDifficulty()`; `difficultyAdjustedControl` es Measured; doc actualizado + test E2E | Cerrado |
| Run-equivalente reconstruible desde persistencia | FALSE→**doc corregido** | No hay `fromSummary`; summary no persiste conteos/score; overclaim eliminado, backlog | Cerrado (doc) |
| Persistencia: versión/idempotencia/corrupt/wrong-exercise/cross-user | VERIFIED | `GoNoGoTelemetryPersistenceTest`; `forSession(userId,sessionId)` clave exacta | — |
| Borrado por cuenta cubre E8 | VERIFIED | `mindboost_speed` ∈ AccountBound + eraser `clearNamespace`; E8 no añade namespace | — |
| E8 no contribuye al perfil oficial | VERIFIED | grep: cero referencias GoNoGo/E8 en `features/profile/**`; `contributesToOfficialProfile=false` | — |
| Copy sin léxico prohibido | VERIFIED | copy scanners ×3 verdes + `GoNoGoCopyTest` | — |
| Timing resoluciones (7 clases) | VERIFIED | `GoNoGoTelemetryTest` cubre las 7 + doble-tap ignorado + abort | — |
| Guardado durable en Back | PARTIAL→**mejorado** | Ahora `withContext(NonCancellable)`; residual = muerte de proceso (backlog) | Bajo |

## Fase 2 — Red-team (resumen por rol)
- **CEO / Measurement Integrity:** el único vector real de "velocidad sin control" era un RT fabricado inflando el
  sub-score de velocidad/estabilidad → cerrado por doble vía (screen ya no fabrica RT; scorer capa RT imposibles).
- **Cognitive scientist:** Go/No-Go válido (GO frecuente 70–80%, NO-GO infrecuente, deadline+foreperiodo aleatorio,
  precisión balanceada). Timebase E8 = `TimeSource.Monotonic` elapsed (no Choreographer como E1-E7) → aceptable para
  score PRELIMINAR no autoritativo; documentado y device-QA-pendiente.
- **Data scientist:** `pooledAccuracy` engañaba (GO-dominado); resuelto con `balancedAccuracy` como titular honesto.
- **Mobile runtime:** background durante NO-GO puede dar falso éxito limpio (el `delay` cuenta wall-clock) →
  transversal, device-QA-pendiente (no se añade hook de lifecycle muerto). Back-tap durante guardado → cerrado con
  NonCancellable.
- **QA:** el config de PRODUCCIÓN (mixedDifficulty) no se testeaba E2E → añadido test.
- **Privacy/GDPR:** sin cross-user leak; borrado por cuenta cubre E8. OK.
- **Anti-gaming:** tap-all/tap-none/impossible-RT/robotic/too-perfect detectados; impossible-RT ahora GATEA el score;
  robotic/too-perfect quedan como diagnostics (heurísticos, no auto-invalidan para no penalizar a un ejecutor
  uniforme legítimo).
- **UX/copy:** informe de rendimiento operativo, sin etiqueta personal; OK.
- **Future analytics/profile:** contrato de analytics documentado (doc-only); `balancedAccuracy`/`preliminaryScore`
  son el titular para consumidores futuros, nunca `pooledAccuracy`.

## Fase 3 — Scoring (Implementation Quality, separado de MVP-Ready)

| Dimensión | Score | Qué falta para 100 |
|---|---:|---|
| Validez cognitiva | 95 | validación empírica/normas (transversal) |
| Mecánica | 96 | background-invalidation en device (transversal) |
| Timing integrity | 90 | timebase Choreographer-grade (hoy Monotonic, preliminar) + QA en device |
| Telemetría | 96 | conteos crudos persistidos para historial exacto (backlog) |
| Persistencia | 95 | durabilidad total (sync/retry) — transversal |
| Scoring | 97 | — (caps + data-quality gate sólidos) |
| Anti-gaming | 92 | robotic/too-perfect gating calibrado (hoy solo diagnostic) |
| Integración oficial | 90 | wiring al store de historial unificado (transversal/backlog) |
| UX/copy | 95 | mostrar anomalyFlags/lowConfidence al usuario (P3) |
| Tests | 95 | test instrumentado del screen (no-JVM) |
| Runtime readiness | 40 | Android/iOS runtime no probado (transversal) |
| MVP readiness | 35 | analytics/§15/Firestore/perfil pendientes (transversal) |

**E8 Overall Implementation Quality Score ≈ 93.6/100** (media ponderada con los pesos dados, excluyendo el peso
runtime/MVP que son transversales al bloque). **MVP-Ready Quality ≈ 45/100** (limitada por dependencias
transversales: runtime QA, analytics, Firestore, contribución al perfil — NO por deuda de E8).

## Fase 4/5 — Hallazgos, prioridad y estado

| # | Hallazgo | Prio | Estado |
|---|---|---|---|
| 1 | RT `?: 0L`/onset stale → RT fabricado como GO correcto | P1 | **FIXED** (onset por-trial reset; tap ignorado si onset null; sin `?:0L`) |
| 2 | Diagnostics no gateaban el score → RT imposible inflaba velocidad | P1 | **FIXED** (`score(metrics,diagnostics)`; `impossiblyFastTrials>0` ⇒ cap ×0.30 + lowConfidence + feedback CALIBRATING) |
| 3 | `accuracy` pooled engañosa (≈0.75 en tap-all) | P1 | **FIXED** (`pooledAccuracy` + `balancedAccuracy`; doc del titular) |
| 4 | Doc "dificultad única" contradice el default `mixedDifficulty()` | P1 | **FIXED** (doc corregido + test E2E de config de producción) |
| 5 | Doc "reconstruible desde persistencia" sin `fromSummary` | P1 | **FIXED** (doc corregido; backlog B1) |
| 6 | Back-tap puede perder un run completado | P2 | **FIXED** (`NonCancellable`); residual proceso-muerto → backlog B2 |
| 7 | Ramas muertas E8 en `SpeedExerciseScreen` (`Unit`/`null` silencioso) | P3 | **FIXED** (`error(...)` fail-loud) |
| 8 | `?: 0.0` inalcanzable en `GoNoGoScorer` | P3 | backlog B3 (inofensivo hoy; comentar/limpiar) |

## Cambios hechos (código)
- `GoNoGoScore.kt`: `score(metrics, diagnostics?)`; `dataQualityCompromised`; cap `DATA_QUALITY_CAP_FACTOR=0.30`.
- `GoNoGoFeedback.kt`: data-quality → CALIBRATING (nunca positivo).
- `GoNoGoSessionStateHolder.kt`: scorer ve diagnostics; guardado `withContext(NonCancellable)`.
- `E8InhibicionScreen.kt`: onset reset por-trial; tap sin fabricar rt=0.
- `GoNoGoOfficialRunAdapter.kt`: `accuracy`→`pooledAccuracy` + `balancedAccuracy`.
- `SpeedExerciseScreen.kt`: ramas E8 → `error(...)`.
- Tests: data-quality (score+feedback), pooled/balanced accuracy, config de producción E2E.
- Docs: MC9D §Dificultad, MC9D_R (reconstruible + durabilidad), este doc.

## Cambios rechazados (con motivo)
- **Hook de lifecycle Android para invalidar background durante NO-GO:** rechazado — sería un control sin call-site
  vivo (código muerto) hasta cablear plataforma; es device-QA transversal. Backlog B4.
- **`roboticTiming`/`improbablePerfect` como gate duro del score:** rechazado — heurísticos con falsos positivos
  (un ejecutor uniforme legítimo dispararía SD baja); se dejan como diagnostics en `anomalyFlags`. Backlog B5.
- **`fromSummary` con reconstrucción exacta:** rechazado ahora — exige persistir conteos crudos + score en el
  summary (acopla telemetría con scoring, más esquema); backlog B1.
- **Reusar `SpeedTapArea`/Choreographer en E8:** rechazado ahora — el paradigma NO-GO (withhold-by-deadline) no
  encaja en esa superficie; `TimeSource.Monotonic` elapsed es aceptable para un score preliminar. Backlog B6 (evaluar
  un timebase Choreographer-grade para E8 cuando el score deje de ser preliminar).

## Backlog
- **B1 (P2):** persistir conteos crudos + score en el summary y añadir `fromSummary` exacto → historial oficial
  reconstruible. Criterio: `fromSummary(of(outcomes)) == toOfficialRun(outcomes)` byte a byte.
- **B2 (P2):** durabilidad total del guardado (scope app-level o write-ahead) → sobrevive muerte de proceso.
- **B3 (P3):** limpiar los `?: 0.0` inalcanzables en `GoNoGoScorer`.
- **B4 (transversal):** invalidación de trial por background del SO (Android LifecycleObserver) + QA en device.
- **B5 (P3):** calibrar robotic/too-perfect para gate duro sin falsos positivos.
- **B6 (transversal):** timebase Choreographer-grade para E8 cuando el score sea autoritativo.
- **Transversales de bloque:** analytics `mental_speed_*`, §15 físico, Firestore sync, contribución al perfil,
  runtime QA Android/iOS. (No bloquean E9; bloquean MVP-ready.)

## Fase 7 — Gates
`compileKotlinDesktop` ✅ · targeted E8 ✅ · targeted E5 telemetry ✅ · `desktopTest` **3209/0/0** ✅ ·
`compileAndroidMain` ✅ · `detekt` ✅ (solo 2 findings foreign untracked: `IndexedSecureKeyValueStore:61`,
`InterSessionConfidence:31`) · copy scanners ×3 ✅ · `scan:secrets` ✅.
**Android runtime NO probado · iOS NO probado · Firestore NO probado.**

## Riesgos residuales
1. Background durante NO-GO (device-QA, B4). 2. Durabilidad best-effort (muerte de proceso, B2). 3. Timebase E8
Monotonic vs Choreographer (B6; mitigado: score preliminar + gate de RT imposible). 4. Historial oficial no
persistido exacto (B1). 5. Transversales de bloque (runtime/analytics/Firestore/perfil).

## Decisión
**GO E9 — E8 queda cerrado a nivel implementación, con riesgos transversales documentados.**
E8 NO es una isla (adapter oficial + persistencia + borrado); NO premia velocidad sin precisión (caps + data-quality
gate probados); persiste vía `ExerciseTelemetryStore` tipado y se borra por cuenta; los P1/P2/P3 corregibles están
cerrados; lo pendiente es transversal (runtime/analytics/Firestore/perfil) o backlog documentado, no deuda de E8.

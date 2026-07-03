# Auditoría Enfoque

＃ Auditoría adversarial y rediseño — Módulo Enfoque (GROUND-TRUTH: 13 ejercicios implementados E1–E13)

> **Nota de alcance.** El spec `03_1_Enfoque.md` describe 8 ejercicios "premium", pero el código real (`ExerciseCatalog` + `FocusExerciseGenerators.registry`) tiene **13 ejercicios cableados end-to-end (E1–E13)**, todos en `implemented`. Esta auditoría se ancla en el código, no en el spec. Hallazgos transversales que afectan a TODOS (no se repiten en cada ficha):
> - **RT NO autoritativo.** Focus mide con `MonotonicClock` (`TimeSource.Monotonic`, `FocusFixedBlockRunner.kt:83`), NO con Choreographer/vsync ni timestamp del input event. El onset es `nowNanos()` en la corrutina, no el frame. Honesto como delta, pero **no frame-accurate**. Efectos del orden de decenas de ms (CONGRUENCY_EFFECT ~40 ms, SWITCH_COST, SSRT) están por debajo del jitter de frame en gama media.
> - **Gate `device_unreliable`/UNTRUSTED_TIMEBASE: NO existe en Focus.** El modelo `TimebasePolicy`/`SessionQualityEvaluator` vive en *speed*, y aun allí `frameDropFraction`/`criticalFrameJitter` están **hardcodeados a 0/false**. Ningún módulo mide jank real. Por REGLA 1, ninguna métrica de RT puede actualizar perfil hoy.
> - **Anti-gaming de tap rítmico / cadencia: NO existe.** El `FocusSessionAssembler` no detecta respuestas equiespaciadas independientes del onset, no marca bloques `CONTAMINATED`, no hay medición de cadencia real (solo existe como feature sintética en Arena). Lo único real: filtro de anticipación (`TrialClassifier`), impulsividad fast-commission (`AdaptiveEngine`) y SSRT `trustworthy` (`SsrtCalculator`).
> - **d′ y criterio c: NO existen.** `MetricKey` no los declara. E1 "Vigía" mide RT_CV + LAPSES + FALSE_ALARMS por separado — NO Teoría de Detección de Señal, pese a que el spec lo hace central.
> - **min-n peligrosamente bajos.** `MIN_N_RT=5`, `MIN_N_CONGRUENCY=3`, `MIN_N_ACCURACY=5`. Un CONGRUENCY_EFFECT/SWITCH_COST con 3 RT por celda es indistinguible de ruido.
> - **`MetricValue.unavailable()` pone `value=0.0`.** `confident=false` lo protege, pero `MetricSet.rawValueOf()` puede leer ese 0 — riesgo latente null→0 si un consumidor usa la vía cruda.
> - **Banco/secuencia.** Todos los generadores son procedurales por seed (no memorizables como texto), pero **sin holdout, sin probes no entrenadas por ejercicio, sin rotación de eje validada**. La única evidencia "no entrenada" vive en `ProbeBank` (baseline/evaluación), no por ejercicio de entrenamiento.

---

## E1 — Vigía (PVT / atención sostenida)

1. **Veredicto:** REVISE.
2. **Constructo:** Atención sostenida / vigilancia bajo señal rara y tiempo-en-tarea creciente (decremento de vigilancia).
3. **Señal primaria válida:** `RT_CV` (estabilidad) + `LAPSES` (conteo sobre `rawHitRts`, bien: no MAD-filtrado) — descriptivas intra-tarea. NINGUNA actualiza perfil hoy (RT no autoritativo).
4. **Señales secundarias:** `VIGILANCE_DECREMENT_SLOPE`, `RT_MEDIAN`, omisiones/comisiones separadas, `FATIGUE_INDEX`, `SUSTAINED_FOCUS_INDEX` (feedback interno).
5. **Contaminantes:** jitter de frame (onset no-vsync); fatiga confundida con decremento de vigilancia; n_targets bajo (targetRate 0.08 × bloque 40 ≈ 3 targets) hace `VIGILANCE_DECREMENT_SLOPE` ruido. **redundantWith:** E12 (re-enganche), E3 (gatillo) comparten RT/comisión.
6. **Gaming:** tap rítmico equiespaciado ignorando el estímulo infla `SUSTAINED_FOCUS_INDEX` (RT_CV bajo + lapsos bajos) — **el detector de cadencia NO existe**. Mash-tapping sube FA pero el impulsividad-gate sólo reacciona en adaptación, no invalida el bloque.
7. **Gates obligatorios:** UNTRUSTED_TIMEBASE→no perfil; jitter mínimo ≥150 ms verificado; detector de tap rítmico (CONTAMINATED si cadencia desacoplada del onset); min n_targets≥20 efectivo o slope `null`; gate de fatiga ANTES de StepUp; abandono no penaliza.
8. **Métrica/scoring:** `SUSTAINED_FOCUS_INDEX = 100·(1−RT_CV)·(1−lapseRate)` (`addFocusIndices`). n_min: RT≥5 (DEBE subir a ≥20 para slope). `confident=false`→preliminary. Agregación inter-sesión vía `sessionMetrics` (promedio ponderado por trials). Actualiza perfil: NUNCA hasta timebase autoritativa + detector cadencia.
9. **Banco:** `E1VigiaGenerator` procedural seed-determinista; sin holdout; rotación de jitter no validada como anti-ritmo. Anti-memorización OK (no hay texto). Falta: jitter≥150 ms enforced y test de cadencia.
10. **Copy permitido:** "estabilidad de tu tiempo de reacción", "lapsos de atención en esta tarea", "foto de hoy", "señal de práctica".
11. **Copy prohibido (bloquear en tests):** "concentración general", "atención en tu vida diaria", "TDAH", "déficit de atención", "mejora tu foco".
12. **Implementación KMP:** `E1VigiaGenerator`, `StimulusPayload.Vigilance`, `TrialClassifier.classify`, `DefaultMetricsEngine`, `MonotonicClock` (no frame). Falta: `TimebaseTrust` en `RecordedTrial`, `cadenceContaminated` en `BlockResult`.
13. **Tests obligatorios:** unit generador determinista (existe); **property** (jitter siempre ≥150 ms); **adversarial** tap-rítmico→CONTAMINATED (FALTA); golden-scoring lock de `SUSTAINED_FOCUS_INDEX` (FALTA); jank-feed→null (FALTA); copy-scanner (FALTA gate global).
14. **Aceptación→APPROVE:** detector de cadencia implementado+testeado con perfil sintético rítmico; jitter≥150 ms; slope `null` con n_targets<20; UNTRUSTED_TIMEBASE no actualiza perfil.
15. **Rediseño:** mantener tarea; añadir (a) `cadenceContaminated` derivado de coef. de variación de inter-tap-intervals vs inter-onset; (b) reintroducir d′/criterio c con corrección de continuidad SOLO si n_targets≥20, si no `null`; (c) jitter mínimo duro.
16. **Impacto (intra-entrenamiento):** medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** sin timebase frame-accurate, RT_CV sigue contaminado por presentación aunque se añada cadencia; el constructo "decremento" necesita bloques largos → fatiga.

---

## E2 — Filtro (Flanker / efecto de congruencia)

1. **Veredicto:** BLOCK (RT-gated sin timebase + min-n=3 + sin balanceo de lado verificado).
2. **Constructo:** Control atencional selectivo / inhibición de interferencia (efecto de congruencia).
3. **Señal primaria válida:** `CONGRUENCY_EFFECT = median(RT_incong) − median(RT_cong)` sólo sobre HIT con RT en ambas condiciones (`congruencyEffect`, `null` honesto si falta un lado). NO actualiza perfil hoy.
4. **Señales secundarias:** accuracy en incongruentes, `IES`, `RT_CV`, `LISAS`, `POST_ERROR_*`.
5. **Contaminantes:** efecto (~40 ms) < jitter de frame; sesgo motor de lado (responder siempre un lado infla accuracy); contraste mal calibrado mide agudeza, no control. **redundantWith:** E5 (Conflicto/Stroop-Simon) mide la MISMA varianza de interferencia con `CONGRUENCY_EFFECT` — fuerte solape.
6. **Gaming:** sesgo de lado fijo; "responder siempre congruente"; respuestas pre-onset (anticipación filtrada, pero el bloque no se invalida).
7. **Gates:** UNTRUSTED_TIMEBASE→null; `MIN_N_CONGRUENCY` subir de 3 a ≥20 por celda; balanceo LEFT/RIGHT correcto verificado; IES por condición; congruency `null` si falta un lado (ya); control de contraste.
8. **Métrica/scoring:** spec `score = 0.5·accIncong + 0.3·(1−norm(congruency)) + 0.2·(1−RT_CV)`. n_min: actual 3 (INSUFICIENTE). `null` si falta condición. Perfil: NUNCA hoy.
9. **Banco:** `E2FiltroGenerator` procedural; falta balanceo de lado testeado y min-n por condición.
10. **Copy permitido:** "cuánto te frena la información irrelevante en esta tarea", "control de interferencia (señal de práctica)".
11. **Copy prohibido:** "multitarea", "atención general", "rendimiento académico", "filtras distracciones en tu vida".
12. **Implementación KMP:** `FocusChoiceField`/`ChoiceParadigm.FLANKER`, `classifyChoice` (autoridad), `RecordedTrial.responseKey`. Falta min-n alto, gate timebase, balanceo verificado.
13. **Tests:** unit (existe); **property** balanceo LEFT/RIGHT; **adversarial** sesgo-de-lado→null/CONTAMINATED (FALTA); golden congruency (FALTA); jank→null (FALTA); copy-scanner (FALTA).
14. **Aceptación→APPROVE:** timebase autoritativa O perfil congelado + congruency `null` con n<20 + balanceo de lado testeado + IES por condición. Mientras `MIN_N_CONGRUENCY=3` y RT no-frame, no sube de BLOCK.
15. **Rediseño:** subir min-n por celda a 20; reportar congruency con IC (bootstrap) y banda; balanceo de lado obligatorio; merge conceptual con E5 a menos que se demuestre ortogonalidad empírica (Flanker espacial vs Stroop semántico).
16. **Impacto:** medio (si se arregla); bajo hoy.
17. **Prioridad:** P1.
18. **Riesgo residual:** aunque se suba min-n, el efecto sigue por debajo del jitter de frame en gama media → la señal puede ser hardware.

---

## E3 — Gatillo (Go/No-Go / inhibición)

1. **Veredicto:** REVISE.
2. **Constructo:** Inhibición de respuesta prepotente (comisión vs omisión).
3. **Señal primaria válida:** `FALSE_ALARMS` (comisiones) + `OMISSIONS` separadas — conteos honestos, no dependen de RT fino. Parcialmente actualizable (accuracy, no RT).
4. **Señales secundarias:** `RT_MEDIAN`, `POST_ERROR_SLOWING`, impulsividad fast-commission (`AdaptiveEngine.isImpulsive`).
5. **Contaminantes:** RT_MEDIAN no autoritativo; goRate alto crea automatismo (confunde con lapso). **redundantWith:** E4 (Freno, misma capacidad RESPONSE_INHIBITION), E1/E12.
6. **Gaming:** responder a todo (nunca inhibir) sube hit-rate; el gate impulsividad reacciona pero NO invalida el bloque, sólo ajusta dificultad.
7. **Gates:** separar comisión/omisión (ya); impulsividad fast-commission (existe, real, testeado); min-n accuracy≥20 para confianza; UNTRUSTED_TIMEBASE para RT.
8. **Métrica/scoring:** comisión/omisión como conteos; `INHIBITORY_CONTROL_INDEX` (en `MetricKey` pero no computado en `DefaultMetricsEngine` — hueco). n_min accuracy=5. Perfil: accuracy sí (con n alto), RT no.
9. **Banco:** `E3GatilloGenerator` procedural; OK anti-memorización.
10. **Copy permitido:** "control para no actuar de más en esta tarea", "señal de práctica de inhibición".
11. **Copy prohibido:** "autocontrol", "impulsividad de tu vida", "controlas tus impulsos".
12. **Implementación KMP:** `classify` (tap-or-withhold), impulsividad en `AdaptiveEngine`. Falta computar `INHIBITORY_CONTROL_INDEX`.
13. **Tests:** unit + impulsividad (existe `DefaultAdaptiveEngineTest`); **adversarial** "responder a todo"→bloque marcado (FALTA invalidación); golden (FALTA); copy-scanner (FALTA).
14. **Aceptación→APPROVE:** `INHIBITORY_CONTROL_INDEX` computado y null-honesto; "responder a todo" detectado (omisión≈0 + FA alto) → CONTAMINATED; n≥20.
15. **Rediseño:** computar índice inhibitorio = `1 − FA_rate` ponderado por RT; invalidar bloque si hit-rate≈1 con FA alto (no inhibe nunca).
16. **Impacto:** medio.
17. **Prioridad:** P2.
18. **Riesgo residual:** sin tarea de stop real (eso es E4), Go/No-Go mide inhibición prepotente más débil.

---

## E4 — Freno (Stop-Signal / SSRT Verbruggen)

1. **Veredicto:** REVISE (lo más sólido del módulo en anti-gaming).
2. **Constructo:** Inhibición reactiva / latencia del proceso de freno (SSRT).
3. **Señal primaria válida:** `SSRT` (integración Verbruggen, `SsrtCalculator`) con flag `trustworthy` (p(respond|signal)∈[0.25,0.75] y ssrt>0). `confident = trustworthy && goRts≥5`. NO actualiza perfil hoy (RT no autoritativo).
4. **Señales secundarias:** p(respond|signal), go-RT mediana, go-RT_CV, stop-failure rate.
5. **Contaminantes:** "waiting strategy" (ralentizar GO para esperar el stop) infla SSRT — `trustworthy` lo captura PARCIALMENTE vía p, **pero NO hay comparación explícita de go-RT con vs sin stop**; n_stops bajo (SSRT inestable). **redundantWith:** E3 (inhibición).
6. **Gaming:** waiting strategy; el `trustworthy` lo mitiga si p sale de banda, pero un usuario que ralentiza homogéneamente puede mantener p≈0.5 con SSRT inflado → gate incompleto.
7. **Gates:** `trustworthy=false`→null (sin excepción, ya en engine); waiting-strategy explícito (go-RT con/sin stop) — **FALTA**; n_stops mínimo para integración — el calculator no lo exige (sólo goRts≥5); stop señalado por ≥2 canales (color+forma+posición); SSD registrada por trial (ya, `stopSignalDelayMs`).
8. **Métrica/scoring:** `SSRT = nthGoRT − meanSSD`; `null` si NotComputable o `!trustworthy`. n_min: goRts≥5 (subir; n_stops sin gate explícito). Perfil: NUNCA hoy.
9. **Banco:** `E4FrenoGenerator.tracker()` (escalera online `StopSignalTracker`), procedural, testeado (`ssrtMetric_flagsGaming`). Buen anti-gaming real.
10. **Copy permitido:** "rapidez de tu freno mental en esta tarea", "freno exitoso" (refuerzo positivo).
11. **Copy prohibido:** "control de impulsos", "gambling", "autorregulación", "frenas en tu vida".
12. **Implementación KMP:** `classifyStop`, `SsrtCalculator`, `FocusFieldState.Stop` (GO→STOP dos fases). Falta: detector waiting-strategy go-RT-diff; n_stops gate.
13. **Tests:** `SsrtCalculatorTest.flagsGaming` + `FocusStopSignalAssemblerTest` + `SimulatorLoopTest.gamerOnStopSignal` (REAL, existe). Falta: golden-scoring SSRT; jank→null; copy-scanner.
14. **Aceptación→APPROVE:** waiting-strategy comparado go-RT con/sin stop implementado+testeado; n_stops mínimo enforced; stop multicanal; timebase autoritativa O perfil congelado.
15. **Rediseño:** añadir `goRtInflation = median(goRT|hasStopBlock) − median(goRT|noStopBlock)`; si supera umbral → `trustworthy=false`. Exigir n_stops≥30 para SSRT o null.
16. **Impacto:** medio-alto (mejor paradigma del módulo).
17. **Prioridad:** P1.
18. **Riesgo residual:** SSRT extremadamente sensible a timebase; sin frame-accuracy el valor absoluto no es comparable inter-sesión/inter-dispositivo.

---

## E5 — Conflicto (Stroop/Simon / interferencia)

1. **Veredicto:** MERGE→E2 (o BLOCK si se mantiene separado). Mide la misma varianza que E2.
2. **Constructo:** Control de interferencia estímulo-respuesta (efecto de congruencia).
3. **Señal primaria válida:** `CONGRUENCY_EFFECT` (idéntico cómputo a E2). No actualiza perfil hoy.
4. **Señales secundarias:** accuracy congruente/incongruente, `IES`.
5. **Contaminantes:** mismos que E2 (jitter < efecto, sesgo de lado). **redundantWith:** E2 — ambos usan `governingMetric = CONGRUENCY_EFFECT` con el mismo motor. Triple solape si se cuenta con E2 y velocidad-Flanker.
6. **Gaming:** sesgo de lado; responder a la dimensión irrelevante consistente.
7. **Gates:** igual que E2; ADEMÁS ortogonalidad empírica vs E2 antes de tratarlas como sub-señales separadas en el brain-profile.
8. **Métrica/scoring:** idéntico a E2 (`congruencyEffect`), min-n=3 insuficiente. Perfil: NUNCA hoy; y si se mantiene, NO contar dos veces la misma varianza.
9. **Banco:** `E5ConflictoGenerator` procedural.
10. **Copy permitido:** "resistencia a la interferencia en esta tarea".
11. **Copy prohibido:** "concentración", "filtras lo irrelevante en tu vida".
12. **Implementación KMP:** `classifyChoice`, `FocusChoiceField`. Sin diferenciador real frente a E2 en el motor.
13. **Tests:** unit; **falta** test de ortogonalidad E2↔E5; min-n; adversarial sesgo lado.
14. **Aceptación→APPROVE:** demostrar correlación E2↔E5 < umbral (ortogonalidad empírica) Y min-n≥20 Y balanceo. Si la varianza es la misma, MERGE.
15. **Rediseño:** diferenciar paradigmas (E2 = conflicto espacial/flanker; E5 = conflicto semántico Stroop O conflicto espacial Simon) y **validar empíricamente** que no son la misma señal; si no, retirar E5 y dejar E2 como único medidor de interferencia.
16. **Impacto:** bajo (redundante).
17. **Prioridad:** P1.
18. **Riesgo residual:** el brain-profile sobre-pondera "control de interferencia" contándolo en E2+E5 (+velocidad) — factor único disfrazado de tres.

---

## E6 — Cambio (Task-switching / SWITCH_COST)

1. **Veredicto:** REVISE.
2. **Constructo:** Flexibilidad cognitiva controlada / coste de reconfiguración de set.
3. **Señal primaria válida:** `SWITCH_COST = median(RT_switch) − median(RT_repeat)` sólo HIT-RT en ambas, `null` si falta una condición (`switchCost`). No actualiza perfil hoy.
4. **Señales secundarias:** accuracy en switch, error de perseveración, `IES` por condición.
5. **Contaminantes:** CSI largo → el usuario pre-configura la regla → mide lectura de cue, no reconfiguración; perseveración confundida con error perceptual (regla previa no registrada en métrica). **redundantWith:** parcial con E5 (selección de regla).
6. **Gaming:** ignorar el cue (responder a una regla fija → accuracy≈azar en la otra); CSI largo para precomputar.
7. **Gates:** cue inequívoco y distinto del estímulo; `SWITCH_COST` null si falta un lado (ya); separar perseveración (registrar regla previa) — **el motor no registra `previousRule`**; balanceo de lado por regla; min-n por condición≥20 (actual 3).
8. **Métrica/scoring:** `switchCost` median-diff, min-n=3 (subir). `null` honesto. Perfil: NUNCA hoy.
9. **Banco:** `E6CambioGenerator` deriva `correctResponse` de (regla, dimensión) — NO expone la respuesta (correcto, payload enriquecido `colorIsRed`/`shapeIsCircle`). Falta registro de regla previa.
10. **Copy permitido:** "coste de alternar entre reglas en esta tarea".
11. **Copy prohibido:** "multitarea", "mejor multitasking", "adaptabilidad general", "flexibilidad en tu vida".
12. **Implementación KMP:** `FocusRuleSwitchField`, `classifyChoice`. Falta `previousRule`/`isPerseveration` en `TrialResult`.
13. **Tests:** `FocusTaskSwitchTest` (existe); **falta** golden switchCost, perseveración aislada, min-n, copy-scanner.
14. **Aceptación→APPROVE:** registro de regla previa + perseveración aislada; min-n≥20; switchCost null testeado; cue dedicado verificado.
15. **Rediseño:** añadir `previousRuleId` al `RecordedTrial`; computar `perseverationRate` (error que coincide con la regla anterior); CSI corto fijo para forzar reconfiguración en línea.
16. **Impacto:** medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** switch-cost ruidoso con sesiones cortas; mide lectura de cue si el diseño visual no separa cue de estímulo.

---

## E7 — Rastreo (Búsqueda visual / SET_SIZE_SLOPE)

1. **Veredicto:** REVISE.
2. **Constructo:** Eficiencia de búsqueda visual (pendiente RT por set size).
3. **Señal primaria válida:** `SET_SIZE_SLOPE` (regresión RT vs set size sobre HIT, exige ≥2 tamaños distintos y n≥6). No actualiza perfil hoy (RT).
4. **Señales secundarias:** accuracy, `RT_MEDIAN` por set size, miss/false-absent rate.
5. **Contaminantes:** "ausente sin buscar" (RT_ausente corto aplana pendiente); items pequeños → agudeza visual; <3 set sizes o R² bajo → ruido. El motor exige ≥2 tamaños y n≥6 (insuficiente: spec pide ≥3 + R²).
6. **Gaming:** responder "ausente" rápido sin buscar; el gate anti-"ausente sin buscar" **NO existe** en `setSizeSlope`.
7. **Gates:** ≥3 set sizes con n suficiente (actual ≥2); R² mínimo de la regresión (**FALTA** — `linearSlope` no devuelve R²); pendientes presente/ausente separadas (**FALTA** — se mezclan); detección "ausente sin buscar"; itemSize sobre umbral perceptual.
8. **Métrica/scoring:** `setSizeSlope` = slope OLS; `null` si <2 tamaños o n<6. Falta R² gate y separación presente/ausente. Perfil: NUNCA hoy.
9. **Banco:** `E7RastreoGenerator` procedural; arrays seed-deterministas auditables.
10. **Copy permitido:** "eficiencia de tu búsqueda visual en esta tarea".
11. **Copy prohibido:** "atención al detalle general", "productividad", "encuentras cosas mejor en tu vida".
12. **Implementación KMP:** `FocusSearchField`, `setSizeSlope` en engine. Falta R², split presente/ausente, `setSize` con ≥3 niveles enforced.
13. **Tests:** unit; **falta** property (pendiente null con <3 sizes/R² bajo), adversarial "ausente rápido", golden.
14. **Aceptación→APPROVE:** ≥3 set sizes + R² umbral + pendientes presente/ausente separadas + gate "ausente sin buscar" implementado y testeado.
15. **Rediseño:** `linearSlope`→`linearRegression` que devuelva (slope, R²); reportar `null` si R²<0.5 o <3 sizes; computar pendiente presente y ausente por separado; invalidar si RT_ausente << RT esperado.
16. **Impacto:** medio.
17. **Prioridad:** P2.
18. **Riesgo residual:** pendiente exige varios niveles × n → sesiones largas → fatiga.

---

## E8 — Dúo (Dual-task / DUAL_TASK_COST)

1. **Veredicto:** REBUILD (la "tarea secundaria" es ficticia).
2. **Constructo declarado:** atención dividida / coste de coordinación. **Constructo real medido:** nada coherente.
3. **Señal primaria "válida":** `DUAL_TASK_COST = accuracy(single-tagged) − accuracy(dual-tagged)`. **PERO** `E8DuoGenerator` no genera ninguna segunda tarea: `DualTask(dual=bool, go=bool)` es un **flag cosmético** sobre el MISMO trial go/no-go single-tap. No hay tarea concurrente, ni accuracy secundaria, ni `taskBalance`. El "coste" compara dos subconjuntos idénticos → mide ruido.
4. **Señales secundarias:** ninguna real (no hay accuracy secundaria).
5. **Contaminantes:** el flag `dual` no cambia la demanda → el "coste" es diferencia de muestreo. **redundantWith:** E3/E1 (es go/no-go disfrazado). **redundantWith E11** (que sí tiene `duringIntrusion`).
6. **Gaming:** trivial — no hay segunda tarea que abandonar; cualquier diferencia es azar.
7. **Gates:** gate de accuracy secundaria (spec lo exige) — **IMPOSIBLE hoy: no hay secundaria**; `taskBalance` — **no existe**. Por REGLA 2 (anti-gaming diseñado≠implementado) y constructo no medible → BLOCK/REBUILD.
8. **Métrica/scoring:** `dualTaskCost` = accuracy single−dual, `null` si falta condición. n_min accuracy=5. **NO debe actualizar perfil**: la métrica no corresponde a su nombre.
9. **Banco:** `E8DuoGenerator` — sin segunda tarea real.
10. **Copy permitido (si se reconstruye):** "cuánto te cuesta repartir la atención en esta tarea (proxy, no es multitarea)".
11. **Copy prohibido:** "multitarea", "productividad", "haces dos cosas a la vez mejor".
12. **Implementación KMP:** `FocusDualTaskField` (contador concurrente en composable) existe en UI, pero el **dominio no lo evalúa**: el assembler no recibe respuestas secundarias. El runtime `presentAndAwait` es de una respuesta/trial.
13. **Tests:** `FocusAdvancedParadigmsAssemblerTest` cubre el flag, NO una segunda tarea real. Falta: secondary-accuracy gate, taskBalance, adversarial abandono.
14. **Aceptación→APPROVE:** o (a) capturar resultado de la secundaria real y computar `accSecondary`+`taskBalance`+gate, o (b) RETIRE si el runtime de una respuesta no lo soporta.
15. **Rediseño:** o reescribir runtime para capturar la respuesta secundaria (contador) al cierre del bloque y exigir `accSecondary≥umbral` (si no, `dualTaskCost=null` + bloque CONTAMINATED); o **RETIRE** y absorber la varianza de "atención dividida" en E9 (MOT) y E11 (Ancla), que sí tienen condiciones reales. Sin segunda tarea evaluada, no es defendible.
16. **Impacto:** bajo (hoy nulo).
17. **Prioridad:** P0 (vende un constructo que no mide).
18. **Riesgo residual:** incluso reconstruido, "secundaria evaluada al cierre" NO es dual-task psicofísico estricto; el copy debe declararlo o viola fronteras.

---

## E9 — Enjambre (MOT / seguimiento múltiple)

1. **Veredicto:** REVISE (cerca de BLOCK por dependencia de frame-rate no medida).
2. **Constructo:** Atención espacial dividida / indexación dinámica (capacidad de tracking).
3. **Señal primaria válida:** accuracy de sonda ("¿el sondeado era target?"). NO actualiza perfil hoy (depende de movimiento renderizado, frame-rate no medido).
4. **Señales secundarias:** `RT_MEDIAN` de sonda, accuracy por velocidad.
5. **Contaminantes:** frame-rate del `Canvas` (en 30 fps los objetos "saltan" → falla por presentación); sesgo de respuesta sí/no; una sonda por trial → umbral converge lentísimo. **redundantWith:** E11 (resistencia a distracción), E8 (carga).
6. **Gaming:** responder siempre "sí" o "no"; RT de sonda ultracorto (adivinar) — sin gate.
7. **Gates:** guardia de frame-rate OBLIGATORIA (drops→CONTAMINATED) — **NO existe medición de frames en ningún módulo**; sonda balanceada target/no-target verificada; min-n por nivel para umbral; bloques cortos (fatiga).
8. **Métrica/scoring:** accuracy de sonda como `ACCURACY`; el spec promete `trackingCapacity` (umbral 79%) — **NO se computa** (no hay staircase de capacidad en el motor de métricas). n_min accuracy=5. Perfil: NUNCA hoy.
9. **Banco:** `E9EnjambreGenerator` procedural; trayectorias seed-deterministas auditables.
10. **Copy permitido:** "cuántos objetivos puedes seguir en esta tarea".
11. **Copy prohibido:** "conducción", "deporte", "atención al volante", "reflejos en la vida real".
12. **Implementación KMP:** `FocusTrackingField` (`Canvas`+animateFloatAsState), `classify` (sonda sí/no). Falta: medición de frame-drop, `trackingCapacity`.
13. **Tests:** generadores avanzados (existe); **falta** guardia frame-rate, balanceo de sonda, adversarial sesgo respuesta, golden.
14. **Aceptación→APPROVE:** medición real de frame-drop→CONTAMINATED; sonda balanceada testeada; min-n por nivel o capacidad `null`; copy sin conducción/deporte.
15. **Rediseño:** instrumentar el `Canvas` con un contador de frames servidos vs esperados; si frameDropFraction>umbral → bloque inválido; sonda 50/50 enforced; reportar accuracy de sonda, NO "capacidad" hasta tener staircase real.
16. **Impacto:** medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** sin medición de frames real, la "capacidad" es función del hardware; el riesgo de claim conducción/deporte es alto (el spec lo menciona).

---

## E10 — Flujo (RSVP / atención temporal)

1. **Veredicto:** BLOCK (SOA nominal ≠ frame real; sin gate de jank; constructo de attentional-blink no validado).
2. **Constructo:** Atención temporal / recuperación entre estímulos (detección en ráfaga rápida).
3. **Señal primaria válida:** accuracy de detección. NO actualiza perfil hoy (la ráfaga es temporal y el SOA real depende del frame, no medido).
4. **Señales secundarias:** `RT_MEDIAN` de detección.
5. **Contaminantes:** el `LaunchedEffect` que avanza la ráfaga usa tiempo de corrutina, no vsync → el SOA presentado ≠ SOA nominal en dispositivos con jank; el constructo "recuperación de atención" (attentional blink) **no se mide** (no hay T1-T2 lag).
6. **Gaming:** responder a ritmo; adivinar (RT ultracorto).
7. **Gates:** SOA efectivo por frame (medir tiempo real entre frames) — **FALTA**; gate de jank→CONTAMINATED — **FALTA**; min-n; balanceo presente/ausente.
8. **Métrica/scoring:** sólo `ACCURACY`/`RT_MEDIAN`; el constructo RSVP (blink) no tiene métrica dedicada. Perfil: NUNCA hoy.
9. **Banco:** `E10FlujoGenerator` procedural.
10. **Copy permitido:** "detectar objetivos en un flujo rápido en esta tarea".
11. **Copy prohibido:** "procesas información más rápido en tu vida", "lees más rápido".
12. **Implementación KMP:** `FocusStreamField` (animación en composable). El SOA no se verifica contra frames reales.
13. **Tests:** generadores avanzados (existe); **falta** SOA-efectivo, jank gate, golden, copy-scanner.
14. **Aceptación→APPROVE:** medir SOA efectivo por frameTimeNanos + gate de jank; definir métrica honesta (accuracy de detección) sin invocar attentional-blink hasta tener T1-T2.
15. **Rediseño:** si no se puede medir el SOA real por frame, degradar el claim a "detección en ráfaga" pura (accuracy), sin afirmar "recuperación de atención"; instrumentar el composable para registrar el tiempo real de cada frame de la ráfaga.
16. **Impacto:** bajo-medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** sin control de frame, el SOA es nominal y la dificultad temporal es ilusoria.

---

## E11 — Ancla (Resistencia a distracción digital)

1. **Veredicto:** REVISE.
2. **Constructo:** Resistencia a la distracción (intrusiones tipo notificación).
3. **Señal primaria válida:** `DISTRACTION_RESISTANCE_INDEX = accuracy(intrusión)/accuracy(baseline)`, coerce[0,1], `null` si baseline=0 o subset pequeño (`distractionResistance`). Tiene condiciones REALES (`duringIntrusion`), a diferencia de E8. Parcial (accuracy, no RT).
4. **Señales secundarias:** accuracy baseline vs intrusión, `RT_MEDIAN`.
5. **Contaminantes:** la "intrusión" es perceptual (no una notificación real con saliencia ecológica); riesgo de copy "te distraes menos en la vida". **redundantWith:** E8 (atención dividida), E9.
6. **Gaming:** ignorar siempre el estímulo intruso si es ignorable trivialmente; sesgo de respuesta.
7. **Gates:** min-n por condición≥20 (actual accuracy=5); baseline e intrusión balanceados; copy sin transferencia digital de vida; UNTRUSTED_TIMEBASE para RT.
8. **Métrica/scoring:** índice ratio, `null` honesto. n_min accuracy=5 (subir). Perfil: accuracy con n alto; RT no.
9. **Banco:** `E11AnclaGenerator` procedural; `duringIntrusion` real.
10. **Copy permitido:** "resistencia a intrusiones en esta tarea (proxy de práctica)".
11. **Copy prohibido:** "te distraes menos con el móvil", "controlas las notificaciones en tu vida", "productividad".
12. **Implementación KMP:** `classifyChoice`/`classify`, `distractionResistance` en engine. Bien estructurado.
13. **Tests:** generadores avanzados (existe); **falta** min-n alto, balanceo, golden, copy-scanner.
14. **Aceptación→APPROVE:** min-n≥20 por condición; balanceo baseline/intrusión testeado; copy auditado.
15. **Rediseño:** subir min-n; reportar índice con IC; declarar explícitamente "proxy de práctica, no transferencia a uso de dispositivos".
16. **Impacto:** medio.
17. **Prioridad:** P2.
18. **Riesgo residual:** la "distracción digital" es perceptual, no ecológica; el copy es el punto de fuga del claim.

---

## E12 — Re-enganche (Recuperación post-error)

1. **Veredicto:** REVISE (riesgo de constructo: tedio vs reenganche).
2. **Constructo:** Recuperación tras error/lapso (post-error recovery, meta-atención).
3. **Señal primaria válida:** `POST_ERROR_ACCURACY` y `POST_ERROR_SLOWING` (`addPostErrorMetrics`, scan pairwise, min-n=3). NO actualiza perfil hoy (RT/PES depende de timing fino).
4. **Señales secundarias:** `RT_CV` (proxy de divagación), `SUSTAINED_FOCUS_INDEX`.
5. **Contaminantes:** monotonía induce mind-wandering deliberado → mide tolerancia al aburrimiento, no reenganche; `reengagementLatency` con pocos lapsos = inestable; desenganche total (racha de omisiones) confundido con lapso puntual. **redundantWith:** E1 (sostenida).
6. **Gaming:** responder a todo (nunca lapso) evita el cómputo; fatiga inducida contamina.
7. **Gates:** `reengagementLatency` null si <N lapsos — **el motor sólo da PES/PEA, no `reengagementLatency` dedicado**; separar desenganche total; probe metacognitivo de baja frecuencia; gate de fatiga REFORZADO (readiness-only, no perfil); validar que la latencia correla con recuperación de RT_CV, no con tedio.
8. **Métrica/scoring:** `POST_ERROR_SLOWING` (diff de medias), `POST_ERROR_ACCURACY`, min-n=3 (subir). `confident` requiere ≥3 post-error y ≥3 post-correct. Perfil: NUNCA hoy + nunca bajo fatiga.
9. **Banco:** `E12ReengancheGenerator` procedural.
10. **Copy permitido:** "cómo te recuperas tras un error en esta tarea (señal de práctica)".
11. **Copy prohibido:** "reduce tu divagación mental", "mindfulness", "concentración general", "TDAH".
12. **Implementación KMP:** `classify`, `addPostErrorMetrics`. Falta: `reengagementLatency` real, gate de fatiga reforzado, separación desenganche.
13. **Tests:** metrics engine (existe `DefaultMetricsEngineTest`); **falta** validación de constructo (latencia↔RT_CV), property min-lapsos, golden.
14. **Aceptación→APPROVE:** `reengagementLatency` null con <N lapsos; desenganche total separado; monotonía como eje medido; gate de fatiga readiness-only; evidencia de que la métrica no mide tedio.
15. **Rediseño:** computar `reengagementLatency` = nº trials hasta volver a RT_CV basal tras error, `null` si <N lapsos; marcar CONTAMINATED si racha de omisiones (desenganche); probe metacognitivo cada ≥30 trials, framed como readiness.
16. **Impacto:** medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** el constructo "reenganche" no está validado contra recuperación de RT_CV → puede medir tolerancia al aburrimiento; raya "dificultad artificial".

---

## E13 — Presión (Deadline pressure / PRESSURE_DELTA)

1. **Veredicto:** REVISE.
2. **Constructo:** Precisión bajo presión temporal (calma vs deadline con coste por error).
3. **Señal primaria válida:** `PRESSURE_DELTA = accuracy(calm) − accuracy(deadline)`, `null` si falta condición (`pressureDelta`). Tiene condiciones reales (`underPressure`). Parcial (accuracy, no RT).
4. **Señales secundarias:** accuracy por condición, `RT_MEDIAN`, `IES`.
5. **Contaminantes:** el deadline depende del frame (presión temporal no autoritativa); inducir presión puede degradar globalmente (no aislar presión de fatiga). **redundantWith:** combina interferencia (E2/E5) + presión.
6. **Gaming:** elegir siempre el modo calma si es opcional; sacrificar velocidad en deadline.
7. **Gates:** min-n por condición≥20 (actual 5); deadline efectivo por frame; `PRESSURE_DELTA` null si falta condición (ya); no afirmar transferencia a "rendir bajo presión en la vida".
8. **Métrica/scoring:** `pressureDelta` = diff accuracy, `null` honesto. n_min accuracy=5 (subir). Perfil: accuracy con n alto; RT no.
9. **Banco:** `E13PresionGenerator` procedural; `underPressure` real.
10. **Copy permitido:** "cuánto te afecta el deadline en esta tarea".
11. **Copy prohibido:** "rindes mejor bajo presión en tu trabajo/estudios", "manejas el estrés".
12. **Implementación KMP:** `classifyChoice`, `pressureDelta` en engine. Falta deadline-por-frame, min-n alto.
13. **Tests:** generadores avanzados (existe); **falta** min-n, deadline efectivo, golden, copy-scanner.
14. **Aceptación→APPROVE:** min-n≥20 por condición; deadline efectivo por frame; copy auditado sin transferencia laboral/estrés.
15. **Rediseño:** subir min-n; medir el deadline efectivo (tiempo real disponible) por frame; reportar delta con IC; declarar proxy.
16. **Impacto:** medio.
17. **Prioridad:** P2.
18. **Riesgo residual:** "presión" temporal depende del frame; el copy roza claims de rendimiento laboral/estrés (frontera de salud).

---

## Correcciones transversales (todo el módulo Enfoque)

**P0 (bloquean cualquier APPROVE):**
1. **Reconstruir o retirar E8 Dúo:** la tarea secundaria no existe; `DUAL_TASK_COST` no mide su nombre.
2. **Implementar el gate `device_unreliable`/UNTRUSTED_TIMEBASE en Focus** (hoy sólo modelo en speed, con frame-drop hardcodeado a 0). Sin él, NINGUNA métrica de RT/SSRT/congruency/switch-cost/slope/PES actualiza perfil.
3. **Implementar+testear el detector de tap rítmico/cadencia** (CONTAMINATED si los inter-tap-intervals están desacoplados del onset) — afecta a E1/E3/E9/E10/E12. Anti-gaming diseñado ≠ implementado = BLOCK.
4. **Subir min-n por celda:** `MIN_N_CONGRUENCY` 3→20, `MIN_N_RT` 5→20 para metricas comparativas; `null`/"—" honesto, nunca 0.

**P1:**
1. **Medición real de frame-drop/jank** que alimente el gate (E9 MOT, E10 RSVP dependen del frame; hoy es ilusión temporal).
2. **Gate anti-waiting-strategy en E4** (go-RT con vs sin stop) además de `trustworthy`.
3. **Validación de constructo de E12** (reengagementLatency↔recuperación RT_CV, no tedio) y **ortogonalidad empírica E2↔E5** antes de contarlas como sub-señales separadas.
4. **Balanceo de lado/respuesta testeado** (E2/E5/E6 LEFT/RIGHT; E9 sonda sí/no).
5. **R² + pendientes presente/ausente separadas en E7.**

**P2:**
1. **Scanner de terminología prohibida para Focus** (hoy sólo existe en arena y emotions): bloquear en build "TDAH/concentración general/multitarea/conducción/deporte/productividad/autocontrol/estrés" en strings de usuario, ni para negarlo.
2. **Reintroducir d′/criterio c en E1** con corrección de continuidad SOLO con n_targets≥20.
3. **Exponer `nEffective`/`dataQuality`/`invalidatedBy`** en cada summary (hoy sólo `n`+`confident`).
4. **Gate de fatiga reforzado** en ejercicios largos (E1/E9/E12): bajo fatiga, readiness-only, no perfil estable.
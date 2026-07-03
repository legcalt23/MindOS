# Auditoría Decisiones

## Auditoría del módulo Decisiones — Comité MindBoost

> **Ground-truth del repo (verificado en código, no en docs):** SOLO existen 3 ejercicios implementados — **E1 Bayes Arena** (`BayesArenaStateHolder`), **E2 Reversibility Triage** (`ReversibilityTriageStateHolder`) y **E4 Check-in Go/No-Go** (`DecisionCheckInStateHolder` + `DecisionSessionOrchestrator.checkIn`). **E3 Forecast, E5 Escáner, E6 Caza-Sesgos, E7 Presupuesto, E8 Ambigüedad NO tienen una sola línea de código de ejercicio** — son diseño puro.
>
> **Hallazgos transversales que arrastran a TODO el módulo:**
> 1. **RT sobre wall-clock epoch (NO autoritativo):** todo el timing se mide con `EpochClock` (`Clock.System` epoch-millis). NINGÚN ejercicio de Decisiones toca Choreographer/MotionEvent ni captura el timestamp del input event. Por REGLA 1, ninguna métrica RT-crítica puede actualizar perfil sin gate `UNTRUSTED_TIMEBASE`.
> 2. **Anti-gaming diseñado pero NEUTRALIZADO en el loop real:** `ScoringEquations.calculateAGRS` existe y está testeado, PERO los holders reales pasan `spatialEntropy=1.0` y `isDialDefault=false` HARDCODEADOS (`BayesArenaStateHolder.kt:71-72`, `ReversibilityTriageStateHolder.kt:73-74`). El término espacial y el detector dial-default del AGRS nunca reciben señal real → REGLA 2: detector ausente en producción = BLOCK/REVISE crítico.
> 3. **`null` colapsa a 0:** `meanOrZero()` (Bayes) y `accuracy = correct/size` (Reversibility) devuelven 0.0/calculan sobre n arbitrario sin min-n. No hay estado `calibrating`/`null`/`invalidated` en los summaries de los holders. VIOLA REGLA 3 y 7.
> 4. **Banco procedural seed=0 fijo, sin validación:** `generateScenarios(seed=0L)` produce SIEMPRE la misma secuencia. No hay rotación, holdout, exposure-tracking, ni validación empírica. Abstracto (no texto memorizable) pero sin IC ni discriminabilidad.
> 5. **Sin gate de exclusión priori=posterior en BUR:** la Revisión crítica del propio diseño (E1 punto 3) exige excluir trials donde `logitMath~0` del BUR; el código NO lo hace — `bayesianUpdateRatio` se promedia crudo, inflando a 1.0 trials que no miden actualización.

---

### E1 — Bayes Arena: Calibrador de Posterior Continuo

1. **Veredicto:** REVISE (implementado y jugable, pero scoring contaminado y anti-gaming neutralizado; jugable sí, profile-update NO).
2. **Constructo objetivo:** Actualización bayesiana de una creencia (P(H|E)) ante evidencia con sensibilidad/FPR conocidas; diana = base-rate neglect y over-updating.
3. **Señal primaria válida:** BUR mediano (`BayesEngine.evaluateBayesianUpdate` en espacio logit) sobre trials válidos donde `|logitMath - logitPrior| > epsilon` (actualización no trivial). Error absoluto |userPosterior - mathPosterior| como respaldo no-RT.
4. **Señales secundarias (internas):** dirección del sesgo (media firmada user-math: over vs under-updating), error absoluto medio, confianza media — solo feedback, no perfil.
5. **Contaminantes principales:** destreza motora fina del dial (confound señalado por el propio diseño, no mitigado en código — el slider no usa `dialGranularity` grueso); dependencia de lectura del escenario textual; trials priori=posterior contados como BUR=1.0 perfecto inflando la señal.
6. **Ataques de gaming:** dejar el dial en default y confirmar (el código pasa `isDialDefault=false` SIEMPRE → no se detecta); confirmar <500ms repetidamente (el holder no descarta por RT, solo `coerceAtLeast(0)`); mover el dial mínimamente igual; secuencia seed=0 fija → memorizar los 5 posteriores tras 1-2 sesiones.
7. **Gates obligatorios:** timebase (FALTA: epoch wall-clock → marcar `UNTRUSTED_TIMEBASE`, RT no actualiza perfil); min-n (FALTA: `meanOrZero` no gatea n); exclusión priori=posterior del BUR (FALTA); detección dial-default real (FALTA: hardcoded false); RT<500ms invalida trial (FALTA); calidad de banco (sin validación).
8. **Métrica y scoring:** `scoreBayes = w_bur·f(BUR) + w_cal·(1-CAGabs) + w_speed·g(IES)` con `f(BUR)=exp(-(ln BUR)²/k)`. n_min ≥ 12 trials válidos (post-exclusión priori=posterior). Invalidaciones: BUR de trial priori=posterior, RT<500ms, dial-default, FRS>0.20. null si n<12 o confident=false; calibrating hasta baseline cross-day; speed-term g(IES) SOLO si timebase confiable (hoy: omitir, marcar preliminary). Agregación inter-sesión: BUR mediano ponderado por trials. Actualiza perfil solo con timebase-OK + n≥12 + AGRS<0.75 real.
9. **Banco de items:** procedural `BayesEngine.generateScenario` (base-rate/sensibilidad/FPR jittered). Tamaño efectivo HOY = 5 fijos seed=0. Rediseño: seed por sesión (no 0L), pool ≥40 configuraciones distintas por celda de dificultad, rotación cross-sesión, exposure-tracking, HIGH_PRIOR con base-rate extremo. Sin inter-rater (numérico verificable, no requiere jueces). Anti-memorización: variar seed + jitter.
10. **UX y copy PERMITIDO:** "estimación preliminar", "señal de práctica bayesiana", "tu estimación vs el cálculo", "calibrando", "no actualizado por calidad de medición". Glyphs no numéricos para base-rate/TP/FP.
11. **UX y copy PROHIBIDO (bloquear en tests):** "mejora tu razonamiento", "decidirás mejor en la vida real", "menos sesgado", "más inteligente", cualquier far-transfer a decisiones médicas/financieras reales, "tu CI".
12. **Implementación KMP:** `BayesArenaStateHolder` existe (clock-free, screen feeds RT). Modelo `BayesTrialRecord` (diseñado, no presente — el holder usa `BayesEstimateFeedback`/`DecisionRound`). Persistencia `SecureDecisionSessionRepository` (summaries). FALTA: campo `movedFromDefault`, exclusión priori=posterior, gate min-n en `finish()`, estado null/calibrating en summary.
13. **Tests obligatorios:** unit (determinismo seed — EXISTE `BayesArenaStateHolderTest`); property (BUR monótono vs gap evidencia — FALTA); simulation (perfil anclado-a-base-rate, perfil over-updater, perfil dial-default — FALTA); adversarial (dial-default + RT<400 → trial descartado — FALTA, hoy AGRS no recibe señal); golden-scoring (lock del scoreBayes final — FALTA); copy-scanner (FALTA gate de lexemas para decisiones).
14. **Criterios de aceptación:** (a) timebase-gate `UNTRUSTED_TIMEBASE` activo y RT fuera del scoring de perfil hasta tener timebase frame-accurate; (b) exclusión priori=posterior del BUR implementada+testeada; (c) `isDialDefault` computado real + AGRS recibe entropía de recorrido del dial; (d) min-n y null/calibrating en summary; (e) seed≠0 + rotación de banco; (f) copy-scanner de decisiones en `check`.
15. **Rediseño propuesto:** mantener el ejercicio (señal bayesiana es de las mejores del catálogo) pero: dial con `dialGranularity` grueso (20 pasos) en condición de medida primaria para neutralizar confound motor; banda de acierto ≥1 paso; EXCLUIR trials priori=posterior del BUR; computar `dialDefault` y `dialPathEntropy` reales y alimentarlos al AGRS; RT solo entra al scoring con timebase confiable; min-n=12; null nunca 0.
16. **Impacto esperado:** ALTO (dentro del entrenamiento) — señal bayesiana limpia y verificable contra el mismo motor.
17. **Prioridad:** P1.
18. **Riesgo residual:** confound motor del dial si la granularidad no se hace gruesa; sin timebase frame-accurate el speed-term queda permanentemente fuera del perfil; banco abstracto sin discriminabilidad empírica medida.

---

### E2 — Triaje de Reversibilidad: Puerta de Una Vía vs Dos Vías

1. **Veredicto:** REVISE (implementado y jugable; anti-gaming neutralizado, accuracy sin min-n, lock-violation mide bien pero RT del lock es wall-clock).
2. **Constructo objetivo:** Discriminación reversible (Type-2, rápido) vs irreversible (Type-1, deliberar) + control inhibitorio de la prisa (respetar el lock en irreversibles).
3. **Señal primaria válida:** Accuracy de triaje separada por tipo (`selectedReversible == isReversible`) + tasa de violación de lock (`!validateLockDuration` en Type-1). Estas dos NO dependen de RT absoluto, solo del orden respecto al lock → válidas aun con timebase débil para la clasificación, PERO la duración del lock se compara contra RT epoch (degrada la precisión del umbral).
4. **Señales secundarias (internas):** RT mediano en reversibles (HITs), IED (penaliza demora λ=0.8 rev / 0.15 irrev), penalty de demora acumulada, omisión vs comisión separadas.
5. **Contaminantes principales:** memorización del patrón de marcadores de coste (familiaridad UI en vez de derivación de coste — sin rotación visual); banda de tipos cerca de 0.5 frustra; el RT del lock medido en epoch puede clasificar mal "antes/después del lock" en bordes.
6. **Ataques de gaming:** swipe siempre al mismo lado (entropía de respuesta baja — el AGRS lo detectaría pero `isDialDefault=false` hardcodeado y spatialEntropy=1.0 lo neutralizan); alternar mecánicamente IZQ/DER sin leer marcadores (no detectado); pre-respuesta <400ms en irreversibles (el holder no descarta); secuencia seed=0 fija de 6 cartas memorizable.
7. **Gates obligatorios:** timebase (lock comparado contra epoch → marcar la precisión del umbral como degradada); min-n por tipo (FALTA: accuracy se calcula sobre cualquier n); detección "siempre el mismo swipe" (FALTA en loop real); RT<400ms invalida (FALTA); rotación de marcadores anti-memorización (FALTA); abandono no penalizado (OK, es triaje, no emocional, pero omisión debe separarse).
8. **Métrica y scoring:** `scoreTriage = w_acc·accuracyTriage + w_imp·(1-lockViolationRate) + w_eff·meanIED_reversibles` (0.45/0.35/0.20). n_min ≥ 10 trials por tipo. Invalidaciones: respuesta antes del lock en Type-1 (cuenta como violación, no como acierto rápido), RT<400ms, swipe-constante, FRS>0.20. null si n<10/tipo; lockViolationRate nunca premia rapidez sobre cautela. La espera honesta correcta NUNCA se trata como lentitud vía IES (λ=0.15 ya lo cubre, pero la UI no debe mostrar "lento" en irreversibles). Actualiza perfil con n≥10/tipo + AGRS-swipe real.
9. **Banco de items:** procedural `ReversibilityEngine.generateScenario` (reversible/delayCost/lockMs templated). HOY = 6 fijos seed=0. Rediseño: seed por sesión, pool ≥30 escenarios/celda, ROTACIÓN VISUAL de marcadores entre sesiones (medir derivación de coste, no iconos memorizados), exposure-tracking. Sin inter-rater (clasificación objetiva por parámetros).
10. **UX y copy PERMITIDO:** "practica distinguir decisiones reversibles de irreversibles", "respetar la ventana de revisión", "señal de práctica de cautela", semáforo no culpabilizante. Lock = anillo de progreso funcional.
11. **UX y copy PROHIBIDO:** "serás más prudente en la vida real", "reduce tu impulsividad", "menos impulsivo clínicamente", "controla tus impulsos", cualquier claim de transfer a conducta real / autocontrol global.
12. **Implementación KMP:** `ReversibilityTriageStateHolder` existe (clock-free, lock UI en pantalla, `currentLockDurationMs()` no filtra respuesta). `ReversibilityEngine.validateLockDuration/calculateDelayPenalty/classifyDecision` autoridad. FALTA: `ReversibilityTrialRecord.respondedBeforeLock` explícito en summary, min-n gating, entropía de swipe real al AGRS, null/calibrating.
13. **Tests obligatorios:** unit (determinismo — EXISTE `ReversibilityTriageStateHolderTest`); property (lock-violation monótona con timePressure); simulation (perfil impulsivo rompe lock, perfil paralizado en reversibles); adversarial (swipe-constante → flag; pre-lock → violación no acierto); golden-scoring (FALTA); copy-scanner decisiones (FALTA).
14. **Criterios de aceptación:** (a) entropía de swipe computada y alimentada al AGRS (no hardcoded); (b) min-n=10/tipo + null/calibrating en summary; (c) marcadores rotan visualmente cross-sesión; (d) lock-respetado premiado, jamás tratado como lentitud; (e) seed≠0; (f) RT<400ms invalida trial.
15. **Rediseño propuesto:** conservar; añadir rotación de marcadores, AGRS-swipe real, min-n por tipo, y separar explícitamente "violación de lock" (impulsividad, penaliza fuerte) de "acierto del lado". La señal de impulsividad/cautela es la más limpia del módulo cuando el lock premia la espera correcta.
16. **Impacto esperado:** MEDIO-ALTO — señal de control inhibitorio limpia y poco dependiente de RT absoluto para la clasificación.
17. **Prioridad:** P1.
18. **Riesgo residual:** memorización de marcadores si no rotan; precisión del umbral de lock degradada por epoch-clock en bordes; banda cerca de 0.5 puede dar frustración/abandono.

---

### E4 — Check-in Cognitivo Go/No-Go: Estado de Disponibilidad

1. **Veredicto:** REVISE (implementado y wired como gate; pero CRI usa RT-CV sobre epoch wall-clock — RT-crítico sin timebase autoritativa → la métrica de consistencia es no fiable).
2. **Constructo objetivo:** Disponibilidad cognitiva (readiness) pre-sesión: accuracy Go/No-Go + variabilidad de RT + fatiga subjetiva → índice CRI. Es GATE de calibración, no entrenamiento.
3. **Señal primaria válida:** CRI (`calculateCRI`, pesos 50/30/20) COMO SEMÁFORO de readiness, no como score. La componente accuracy Go/No-Go es válida; la componente RT-CV NO es fiable sin timebase frame-accurate (RT-crítico).
4. **Señales secundarias (internas):** comisión en No-Go (impulsividad) vs omisión en Go (inatención) separadas; RT mediano Go-HITs; fatigue drift (slope RT vs trial); concordancia fatiga-subjetiva vs RT-CV objetiva.
5. **Contaminantes principales:** RT-CV contaminado por interrupciones del entorno (pausa larga) sin descarte de outliers; epoch wall-clock añade jitter de scheduler a la varianza de RT → infla RT-CV artificialmente; fatiga subjetiva auto-reportada ruidosa; bloque corto (60 trials) puede no alcanzar min-n para RT-CV.
6. **Ataques de gaming:** responder-a-todo (sin inhibir) → comisión alta detectable; pre-respuesta ritmica <400ms (anticipación); como es medida, el gaming corrompe el GATE no un leaderboard → más peligroso (auto-engaño del readiness).
7. **Gates obligatorios:** timebase (CRÍTICO: RT-CV sobre epoch → la componente RT del CRI debe marcarse no-confiable / `UNTRUSTED_TIMEBASE` hasta tener timebase real); min-n para RT-CV (FALTA definir, reportar null honestamente); descarte de outliers por pausa larga (FALTA); abandono no penalizado (OK); NO gamificar (presentar como semáforo).
8. **Métrica y scoring:** CRI directo 0..1 como readiness (NO score de habilidad). Recomendación: ≥0.7 proceder / 0.5-0.7 deload / <0.5 posponer. n_min RT-CV ≥ ~20 Go-HITs limpios; si n insuficiente → RT-CV null y CRI reportado solo con accuracy+fatiga (marcado parcial), NUNCA un CRI fabricado. Invalidaciones: outlier por pausa, RT<400ms. preliminary hasta baseline cross-day. NO actualiza "perfil de habilidad" — alimenta el gate de confianza de la sesión.
9. **Banco de items:** estímulos Go/No-Go procedurales (no banco textual). noGoRatio fijo por sesión para comparabilidad cross-día. Sin riesgo de memorización de contenido (perceptual). Anti-memorización vía interTrialJitter (evita anticipación rítmica).
10. **UX y copy PERMITIDO:** "hoy conviene una sesión ligera", "estado de disponibilidad", semáforo de estado (verde/ámbar/rojo), "no es un examen", honesto y no culpabilizante.
11. **UX y copy PROHIBIDO:** "mide tu estado mental", "detecta fatiga patológica", "diagnóstico de atención/TDAH", "tu nivel de concentración hoy es X/100" (gamificar el CRI), cualquier claim clínico de fatiga.
12. **Implementación KMP:** `DecisionCheckInStateHolder` + `DecisionSessionOrchestrator.checkIn` computan CRI. `CheckInRecord`/`CognitiveReadinessState`/`CheckInInput` existen. Wired como stage `CheckIn` del flow. FALTA: gate de timebase sobre la componente RT-CV, descarte de outliers por pausa, min-n para RT-CV con null honesto.
13. **Tests obligatorios:** unit (CRI determinista — EXISTE `DecisionCheckInStateHolderTest`); property (CRI baja monótonamente con RT-CV y fatiga); simulation (perfil fatigado, perfil responde-a-todo); adversarial (anticipación rítmica → flag); jank/timebase (RT-CV marcado no-confiable bajo epoch); copy-scanner (no "score", presentar semáforo).
14. **Criterios de aceptación:** (a) componente RT-CV del CRI marcada no-confiable hasta timebase frame-accurate; (b) descarte de outliers por pausa larga; (c) min-n RT-CV con null honesto; (d) presentado como semáforo, no como puntuación maximizable (test que bloquee copy de "score"); (e) fatiga subjetiva siempre covariable, nunca único driver.
15. **Rediseño propuesto:** conservar como GATE (es imprescindible para la validez del módulo), pero degradar honestamente la componente RT-CV mientras el timebase sea epoch: con timebase débil, basar el semáforo principalmente en accuracy Go/No-Go + fatiga + comisión/omisión, y mostrar RT-CV como "calibrando". Presentación SIEMPRE semáforo.
16. **Impacto esperado:** ALTO (transversal) — protege la validez de TODOS los demás ejercicios al evitar medir cansado; su utilidad no depende de score absoluto.
17. **Prioridad:** P0 (es el gate de calidad del módulo; su honestidad condiciona todo lo demás).
18. **Riesgo residual:** RT-CV no fiable bajo epoch limita el poder del CRI; auto-reporte de fatiga ruidoso; riesgo de que el usuario lo perciba como examen y se estrese (contamina la medida).

---

### E3 — Forecast Calibrado: Apuesta tu Confianza

1. **Veredicto:** BLOCK (diseño-only, CERO código; depende de un banco de items evidence-controlled que NO existe y es el punto más débil; el copy de diseño usa "Apuesta"/"fichas apostadas" = riesgo de léxico de apuesta).
2. **Constructo objetivo:** Calibración confianza-acierto (90% confianza ⇒ 90% acierto); diana sobreconfianza/infraconfianza + colapso al 50%.
3. **Señal primaria válida:** Brier score (`calculateBrierScore`, EXISTE en ScoringEquations) + calibration error (`calculateCalibrationError`, 5 bins) — SOLO válidos si el outcome es objetivamente verificable y la probabilidad de acierto está fijada por diseño (no por conocimiento previo).
4. **Señales secundarias (internas):** CAG (dirección del sesgo over/under), decisional courage (sd de confianzas, anti-colapso), accuracy bruta (control), RT mediano.
5. **Contaminantes principales:** **el banco de items** — si un item se acierta por conocimiento cultural/idioma, mide cultura, no calibración. Conservadurismo premiado si la dificultad no se calibra. Confundir acierto con calibración. **Redundante con E1/E8 en CAG/Brier.**
6. **Ataques de gaming:** declarar siempre 0.5 (minimiza varianza Brier — el peso courage debe contrarrestarlo suavemente sin empujar a sobreconfianza temeraria); siempre la misma opción; RT<400ms; confianza constante (sd~0, degenerado).
7. **Gates obligatorios:** timebase (RT secundario, menos crítico); min-n (calErr 5 bins necesita n≥~25 → null debajo); banco evidence-controlled VALIDADO (inter-rater o probabilidad-fijada-por-diseño); detección confianza-constante; outcome objetivamente verificable (no opinión).
8. **Métrica y scoring:** `scoreCalib = w_brier·(1-Brier) + w_calerr·(1-calErr) + w_courage·clamp(courage)` (0.50/0.35/0.15). n_min ≥ 25 para calErr fiable. Invalidaciones: confianza-constante, sd~0, item no evidence-controlled, RT<400ms. null si n<25 bins. courage peso suave (0.15), nunca dominante. preliminary siempre.
9. **Banco de items:** **NO EXISTE.** Requisito: items de evidencia controlada (probabilidad de acierto fijada por diseño, NO trivia), banco grande, holdout activo, rotación de dominios (domainRotation), inter-rater ≥80% en items textuales o formato numérico evidence-controlled. Sin esto, el ejercicio es trivia cultural. Anti-memorización: excludeRecent + pool grande.
10. **UX y copy PERMITIDO:** "calibración de confianza", "que tu seguridad coincida con tus aciertos", "estimación preliminar", "señal de práctica metacognitiva".
11. **UX y copy PROHIBIDO:** "apuesta", "fichas apostadas", "mercado de predicción" (el diseño los usa — léxico de APUESTA, bloquear); "serás mejor pronosticador en tu trabajo", "mejor decisión bajo incertidumbre real", far-transfer profesional.
12. **Implementación KMP:** NADA implementado. Diseñado: renderer choice A/B + slider de confianza (compartido con E1), `ForecastTrialRecord`, motores Brier/calErr/CAG/courage (existen). Coste alto = banco validado.
13. **Tests obligatorios:** todos FALTAN (no hay código). Críticos antes de cualquier APPROVE: validación de banco (inter-rater/evidence-controlled), property (Brier proper), adversarial (confianza-constante no puntúa alto), copy-scanner (bloquear "apuesta/fichas").
14. **Criterios de aceptación:** (a) banco evidence-controlled validado con inter-rater ≥80% o probabilidad-por-diseño; (b) outcome objetivamente verificable; (c) renombrar fuera de léxico de apuesta; (d) min-n=25 calErr con null; (e) courage peso suave.
15. **Rediseño propuesto:** SOLO viable si se construye banco evidence-controlled primero (formato numérico/perceptual para eliminar cultura, p.ej. estimar proporción de una mezcla visual donde la verdad está fijada). Renombrar a "Calibración de Confianza" sin léxico de apuesta. Si no hay banco validado defendible → RETIRE (la señal metacognitiva queda parcialmente cubierta por el CAG de E1 y E8).
16. **Impacto esperado:** MEDIO (potencialmente la mejor señal metacognitiva, pero condicionado 100% a un banco que no existe).
17. **Prioridad:** P2 (construir banco validado es caro; E1/E8 ya dan CAG).
18. **Riesgo residual:** banco como bomba cultural/idioma; Brier premia conservadurismo si la dificultad no se calibra; solapamiento de varianza metacognitiva con E1/E8.

---

### E5 — Escáner de Alternativas: Anti-Fijación en la Primera Opción

1. **Veredicto:** BLOCK (diseño-only; renderer NUEVO no existe; el scan-ratio es trivialmente inflable y el gate de RT-por-revelación es obligatorio pero ausente; riesgo de medir paciencia/perfeccionismo).
2. **Constructo objetivo:** Búsqueda exhaustiva de alternativas antes de comprometer (anti-satisficing, anti-anclaje); diana = alternativesScannedRatio + detección de la dominante no-saliente.
3. **Señal primaria válida:** Accuracy de elección dominante + alternativesScannedRatio (alimenta `ICD`, peso scan 0.15) — válida SOLO si la dominante es matemáticamente inequívoca (Pareto estricta o suma ponderada con pesos mostrados) Y el scan-ratio se descuenta cuando la revelación es <150ms (no leyó).
4. **Señales secundarias (internas):** ICD, RT primera selección vs total (impulsividad de cierre), satisficing index, resistencia al anclaje (acierto con salienceMismatch alto).
5. **Contaminantes principales:** "dominante objetiva" se vuelve subjetiva con atributos en conflicto; revelar-todo-sin-leer infla la métrica diana; mide paciencia/perfeccionismo en vez de calidad de búsqueda; riesgo idioma si los atributos son textuales. **Redundante parcial con E7 (decisión multi-atributo).**
6. **Ataques de gaming:** abrir todas las opciones mecánicamente sin leer (scan inflado falso); seleccionar <400ms tras revelar (no evaluó); patrón de exploración idéntico repetido; seed fijo memorizable.
7. **Gates obligatorios:** timebase (RT-por-revelación es la señal anti-gaming → necesita ser fiable, pero epoch puede valer para el umbral grueso 150ms); RT-por-revelación <150ms descuenta scan (OBLIGATORIO); selección <400ms invalida; dominancia inequívoca; min-n; rotación de posición de la dominante.
8. **Métrica y scoring:** `scoreScan = w_acc·accuracy + w_scan·scanRatio + w_anchor·(1-anchorErrorRate)` (0.50/0.30/0.20), limitando w_acc si scanRatio muy bajo (penaliza acierto-por-suerte). n_min ≥ 12. Invalidaciones: revelación <150ms (scan no cuenta), selección <400ms, over-scanning con coste (variante). null si n<12.
9. **Banco de items:** renderer NUEVO + banco NUEVO. Atributos NUMÉRICOS/icónicos (no textuales) para eliminar cultura; dominante por Pareto estricta o suma ponderada con pesos visibles. Rotación de optimalPosition. Pool grande, exposure-tracking. Sin esto = feedback opinable.
10. **UX y copy PERMITIDO:** "explora todas las alternativas antes de elegir", "señal de práctica de búsqueda", "evita quedarte con la primera opción aceptable" (en contexto de la tarea).
11. **UX y copy PROHIBIDO:** "tomarás mejores decisiones complejas reales", "más creativo", "menos impulsivo en la vida", far-transfer a decisiones reales / creatividad / IQ.
12. **Implementación KMP:** NADA. Renderer NUEVO (lista revelable + selección multi-tap). `ScanTrialRecord` diseñado. Motores ICD/FRS/AGRS existen.
13. **Tests obligatorios:** todos FALTAN. Críticos: gate RT-por-revelación (adversarial revelar-sin-leer), dominancia inequívoca (property), simulation satisficing vs over-scanner.
14. **Criterios de aceptación:** (a) dominante matemáticamente inequívoca con pesos visibles; (b) gate RT-por-revelación <150ms implementado+testeado; (c) atributos numéricos/icónicos; (d) over-scanning con coste penalizado en la variante; (e) min-n.
15. **Rediseño propuesto:** SOLO con atributos numéricos, dominancia Pareto/suma-ponderada explícita, y gate de revelación obligatorio. Si el scan-ratio sigue siendo trivialmente inflable o solapa con E7 → MERGE en E7 (decisión multi-atributo) o RETIRE. Diferenciar de E7: E5 mide AMPLITUD de búsqueda, E7 mide OPTIMIZACIÓN bajo restricción — si no se diferencian limpiamente, fusionar.
16. **Impacto esperado:** MEDIO — ataca un sesgo real (satisficing/anclaje) pero con métrica diana frágil.
17. **Prioridad:** P3 (renderer nuevo caro + métrica inflable + posible redundancia con E7).
18. **Riesgo residual:** scan-ratio mide engagement con la UI más que razonamiento; dominancia subjetiva con conflicto de atributos; solapamiento de varianza con E7.

---

### E6 — Detección de Sesgo: Caza-Trampas de Razonamiento

1. **Veredicto:** BLOCK (diseño-only; debiasing tiene transferencia notoriamente débil — riesgo de overclaim; banco textual = bomba cultural/idioma; sin banco validado es trivia de psicología, no entrenamiento).
2. **Constructo objetivo:** Detección del sesgo activo (anclaje, base-rate, framing, sunk-cost, confirmación) + veto de la respuesta System-1; diana = biasDetected del ICD.
3. **Señal primaria válida:** Accuracy de respuesta (resistir la trampa) + biasDetectionAccuracy (etiquetado correcto) — válidas SOLO con items donde el sesgo plantado es inequívoco (un solo sesgo en V1) y banco validado con anti-memorización estricta.
4. **Señales secundarias (internas):** ICD, RT items-trampa vs control (latencia extra = deliberación sana), perfil de vulnerabilidad por familia, jointCorrectRate (entendimiento real vs suerte).
5. **Contaminantes principales:** items dependen fuertemente de lenguaje/cultura (framing, escenarios narrativos); memorización con repetición; el "sesgo correcto" puede ser discutible en items ambiguos; debiasing no transfiere (overclaim). **Solapa con E1 (base-rate) en la familia base_rate.**
6. **Ataques de gaming:** acertar respuesta + etiquetar sesgo al azar (jointCorrectRate baja con accuracy alta lo detecta); RT<400ms (no leyó); memorizar items repetidos (requiere excludeRecent OBLIGATORIO).
7. **Gates obligatorios:** timebase (RT secundario); min-n por familia de sesgo; excludeRecent OBLIGATORIO (sin él mide memoria); sesgo inequívoco (un solo sesgo plantado en V1); banco validado inter-rater; copy honesto (no debiasing claim).
8. **Métrica y scoring:** `scoreBias = w_resp·accuracyResp + w_detect·biasDetectAcc + w_consistency·jointCorrectRate` (0.40/0.40/0.20). n_min por familia ≥ 8. Invalidaciones: RT<400ms, item visto recientemente, jointCorrect bajo con accuracy alta (suerte). null si n por familia insuficiente.
9. **Banco de items:** NUEVO, grande, con excludeRecent. Donde sea posible, formato NUMÉRICO (base-rate, anclaje) para reducir cultura; framing/sunk-cost requieren validación inter-rater ≥80%. Un solo sesgo inequívoco por item (variante compuesta diferida). HIGH_PRIOR para sesgos poco entrenados (weakness-first).
10. **UX y copy PERMITIDO:** "reconocimiento del patrón en esta tarea", "identifica la trampa de razonamiento de este ejercicio", "señal de práctica metacognitiva".
11. **UX y copy PROHIBIDO:** "te volverás menos sesgado", "debiasing", "pensarás mejor en la vida real", "menos sesgos en tus decisiones", far-transfer de debiasing (evidencia científica débil — overclaim prohibido).
12. **Implementación KMP:** NADA. Renderer choice + choice secundario (etiqueta). `BiasTrialRecord` diseñado. Banco con excludeRecent (patrón `excludeRecentWordIds` de memory).
13. **Tests obligatorios:** todos FALTAN. Críticos: excludeRecent (adversarial memorización), sesgo-inequívoco (validación banco), copy-scanner (bloquear debiasing claim), jointCorrectRate (adversarial etiquetado al azar).
14. **Criterios de aceptación:** (a) banco validado, sesgo inequívoco por item; (b) excludeRecent obligatorio implementado+testeado; (c) copy limitado a "patrón en esta tarea"; (d) formato numérico donde posible; (e) min-n por familia.
15. **Rediseño propuesto:** SOLO con banco validado, sesgo inequívoco, excludeRecent y copy honesto. Dado que (i) debiasing no transfiere, (ii) el banco textual es bomba cultural, y (iii) la familia base_rate solapa con E1 — el caso defendible es DÉBIL. Recomendación: si no se puede construir banco numérico validado → RETIRE; la metacognición de sesgos queda cubierta por biasDetected reportado honestamente en otros ejercicios.
16. **Impacto esperado:** BAJO-MEDIO — debiasing es el constructo de transferencia más débil del catálogo.
17. **Prioridad:** P3.
18. **Riesgo residual:** overclaim de debiasing casi inevitable en el copy; banco cultural; "sesgo correcto" discutible; solapamiento con E1.

---

### E7 — Presupuesto de Decisión: Asignación bajo Restricción

1. **Veredicto:** BLOCK (diseño-only; el MÁS caro de implementar; el solver del óptimo es punto único de fallo — bug → feedback miente; drag mide destreza motora; CDE describe complejidad, NO habilidad).
2. **Constructo objetivo:** Optimización multi-atributo bajo restricción manteniendo constraints en WM; diana = integración multi-atributo + respeto de restricciones sin desbordar WM.
3. **Señal primaria válida:** Optimality ratio (retorno obtenido / óptimo del solver) + constraint violation rate — válidas SOLO si el solver es correcto (knapsack/LP exacto para n pequeño) y es la MISMA fuente de verdad para feedback y score.
4. **Señales secundarias (internas):** CDE (complejidad afrontada, NO habilidad), RT total + nº reasignaciones (eficiencia), decremento de optimalidad bajo memoryLoad (WM efectiva), sunk-cost index.
5. **Contaminantes principales:** drag mide destreza UI; nItems alto satura WM perceptiva (no cognitiva); mide paciencia/perfeccionismo; fuerza bruta vía reset masivo (perseverancia, no decisión); **CDE no es métrica de habilidad**. Solapa parcialmente con E5 (multi-atributo).
6. **Ataques de gaming:** asignar-todo-al-primero o patrón fijo (baja entropía de asignación); confirmar <500ms; fuerza bruta probando combinaciones vía reset (limitar resets); seed fijo memorizable.
7. **Gates obligatorios:** timebase (RT secundario); solver EXHAUSTIVAMENTE testeado (autoridad única); steppers discretos en medida primaria (drag solo en variante); límite de resets; min-n; baja-entropía-de-asignación detectada.
8. **Métrica y scoring:** `scoreBudget = w_opt·optimalityRatio·(1-violationRate) + w_eff·efficiency` con `efficiency=1/(1+reassignments/nItems)` (0.80/0.20). Violación anula el retorno del trial. n_min ≥ 10. Invalidaciones: confirmar <500ms, baja entropía de asignación, fuerza bruta. null si n<10. NO confundir CDE alto con buena decisión.
9. **Banco de items:** procedural con solver determinista (knapsack DP exacto). Pool grande, seed por sesión, rotación. Sin inter-rater (óptimo verificable por solver). Crítico: solver correcto = no requiere validación humana pero SÍ tests exhaustivos.
10. **UX y copy PERMITIDO:** "optimiza la asignación bajo restricciones", "señal de práctica de decisión multi-atributo", "respeta las restricciones".
11. **UX y copy PROHIBIDO:** "mejor gestión financiera real", "planificación de tu vida", "gestión de tiempo real", far-transfer a finanzas/planificación/IQ.
12. **Implementación KMP:** NADA. Renderer NUEVO (drag/stepper — el más caro). Solver determinista commonMain. `BudgetTrialRecord` diseñado.
13. **Tests obligatorios:** todos FALTAN. CRÍTICO: solver exacto exhaustivo (knapsack para n pequeño, golden); adversarial (patrón fijo, fuerza bruta); simulation (WM-limitado bajo memoryLoad).
14. **Criterios de aceptación:** (a) solver exacto testeado exhaustivamente, misma fuente para feedback y score; (b) steppers en medida primaria; (c) límite de resets; (d) min-n; (e) entropía de asignación detectada.
15. **Rediseño propuesto:** SOLO con solver exacto + steppers + límite de resets. Es rico pero el más caro y solapa con E5 en multi-atributo. Si el presupuesto de implementación es limitado → diferir; priorizar E1/E2/E4 reparados. Defendible si se quiere una señal de WM bajo decisión, pero P3.
16. **Impacto esperado:** MEDIO — señal rica de decisión-bajo-restricción, pero con alto confound motor/perceptivo.
17. **Prioridad:** P3 (coste de implementación más alto del módulo).
18. **Riesgo residual:** bug del solver hace mentir todo el feedback; drag mide destreza; CDE confundido con habilidad; saturación WM perceptiva; solapamiento con E5.

---

### E8 — Resolución bajo Ambigüedad: Decidir con Información Incompleta

1. **Veredicto:** BLOCK (diseño-only; el VOI óptimo es no-trivial y un bug lo convierte en ruido; riesgo de medir aversión al riesgo/rasgo en vez de habilidad; BUR colapsa priori=posterior igual que E1).
2. **Constructo objetivo:** Trade-off recolectar-más-info vs decidir-ya; diana = sobre-recolector (parálisis) y sub-recolector (decide ciego); óptimo = umbral de valor-de-información.
3. **Señal primaria válida:** Information stopping error (|comprasdas - óptimas|) + decision accuracy — válidas SOLO si el VOI óptimo es matemáticamente correcto (incluye payoffAsymmetry) y la banda óptimo±1 es tolerante (cautela legítima no es error).
4. **Señales secundarias (internas):** BUR del posterior final (excluyendo priori=posterior), net payoff, over/under-collection rate, CAG de la confianza final.
5. **Contaminantes principales:** VOI óptimo difícil de computar (bug → stopping-error es ruido); mide aversión al riesgo (rasgo) más que habilidad; sobre-recolección vs cautela legítima difíciles de separar; **solapa con E1 (Bayes) — debe diferenciarse: E1 mide la actualización, E8 mide CUÁNDO parar**.
6. **Ataques de gaming:** "siempre compra max" o "nunca compra" (política degenerada — detectar por correlación compras-vs-incertidumbre); decidir <400ms (no integró); seed fijo memorizable.
7. **Gates obligatorios:** timebase (RT secundario); VOI óptimo correcto+testeado (incluye payoffAsymmetry); exclusión priori=posterior del BUR; banda óptimo±1 tolerante (no culpar cautela); política-degenerada detectada; min-n.
8. **Métrica y scoring:** `scoreVOI = w_stop·(1-normStoppingError) + w_acc·accuracy + w_pay·normNetPayoff` (0.45/0.35/0.20). n_min ≥ 10. Invalidaciones: decidir <400ms, política degenerada, BUR priori=posterior. null si n<10. Cautela leve con payoff simétrico NO es error (banda tolerante).
9. **Banco de items:** procedural (BayesEngine + VOI determinista). Pool grande, seed por sesión. Sin inter-rater (verificable por cálculo VOI). Crítico: VOI correcto = tests exhaustivos.
10. **UX y copy PERMITIDO:** "cuándo dejar de recolectar información", "señal de práctica de parada óptima", "estimación preliminar".
11. **UX y copy PROHIBIDO:** "mejor gestión de riesgo profesional", "mejores decisiones reales bajo incertidumbre", "rasgos de personalidad", far-transfer + claims de rasgo.
12. **Implementación KMP:** NADA. Renderer choice A/B + slider confianza (compartido E3) + botones comprar/decidir. VOI determinista commonMain. `VoiTrialRecord` diseñado. `SensorEvidence` existe.
13. **Tests obligatorios:** todos FALTAN. CRÍTICO: VOI óptimo correcto (golden, incluye payoffAsymmetry); adversarial (política degenerada); exclusión priori=posterior (property).
14. **Criterios de aceptación:** (a) VOI óptimo matemáticamente correcto y testeado con payoffAsymmetry; (b) banda óptimo±1 tolerante, copy no culpa cautela; (c) exclusión priori=posterior del BUR; (d) política degenerada detectada; (e) diferenciado limpiamente de E1; (f) min-n.
15. **Rediseño propuesto:** SOLO con VOI correcto + banda tolerante + diferenciación clara de E1. Cierra un hueco real (parada óptima) distinto de E1. Defendible si el VOI se deriva de un cálculo explícito (no heurística) y la cautela no se penaliza. Si no se puede separar de aversión-al-riesgo → la señal será un rasgo, no una habilidad entrenable → REVISE hacia RETIRE.
16. **Impacto esperado:** MEDIO — cierra un hueco real (valor de información), pero confound rasgo/aversión fuerte.
17. **Prioridad:** P2.
18. **Riesgo residual:** VOI mal computado convierte stopping-error en ruido; mide aversión al riesgo (rasgo no entrenable); solapamiento de varianza bayesiana con E1.

---

## Síntesis del comité

- **Prioridad de reparación:** E4 (P0, gate de calidad) → E1 y E2 (P1, ya implementados, reparables) → E8/E3 (P2, banco/VOI) → E5/E6/E7 (P3, renderers nuevos caros + redundancia/debiasing débil).
- **Implementar MEJORES señales, no más ejercicios:** E1+E2+E4 reparados (timebase-gate, AGRS real, min-n, null≠0, exclusión priori=posterior) dan un módulo de Decisiones honesto y defendible. E5/E6/E7 son caros, redundantes o débiles.
- **Redundancias a resolver:** E1↔E8 (actualización vs parada — diferenciar o E8 absorbe); E5↔E7 (multi-atributo — fusionar si no se diferencian); E3↔E1/E8 (CAG/Brier metacognitivo — E3 solo si banco evidence-controlled).
- **Bloqueador transversal nº1:** sin timebase frame-accurate (solo speed/Android lo tiene), ninguna métrica RT-crítica de Decisiones actualiza perfil — todo RT debe ir gated `UNTRUSTED_TIMEBASE`.
- **Bloqueador transversal nº2:** el anti-gaming AGRS está IMPLEMENTADO pero NEUTRALIZADO en el loop real (`spatialEntropy=1.0`, `isDialDefault=false` hardcoded) — computar señales reales antes de cualquier APPROVE.
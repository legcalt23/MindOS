# Auditoría Emociones

ial# Auditoría MindBoost — Módulo Emociones (rediseño quirúrgico)

> **Reconciliación crítica de fuentes.** El doc de diseño `03_7_Emociones.md` describe 8 ejercicios **EM-A…EM-H**. El **código real** (`EmotionExerciseCatalog.kt`) declara un catálogo DISTINTO de **15 ejercicios EM1…EM15**, de los cuales solo **6 tienen generador wired** (`EmotionExerciseGenerators.registry`: EM1, EM2, EM3, EM8, EM10, EM11) y esos 6 reúsan **3 paradigmas** (go/no-go afectivo, 2-alt perceptual, clasificación 4-cat). Los otros **9 (EM4, EM5, EM6, EM7, EM9, EM12, EM13, EM14, EM15)** están en el catálogo pero **NO tienen generador, ni renderer, ni path jugable**: son fichas declarativas sin implementación. Audito el **catálogo real (EM1-EM15)**, anclado en ground-truth, mapeando cada uno a su contraparte de diseño cuando existe. Ignoro la sección "Resumen de cobertura" del doc.
>
> **Hechos de ground-truth que dominan todos los veredictos:**
> 1. **Timebase NO autoritativa.** Emotions mide RT con `graph.monotonicClock.nowNanos()` (`EmotionSessionStateHolder.kt:201,126`). Es monotónico (mejor que epoch) pero **NO frame-accurate** (no Choreographer/vsync, el onset es `nowNanos()` en el hilo de la corrutina, no el frame de presentación). **No existe medición de jank/frame-drop** en ningún módulo; el gate `device_unreliable` es modelo sin datos. ⇒ **Toda métrica RT-crítica (RT_MEDIAN, RT_CV, IES, anticipaciones, post-error slowing, coste de interferencia, SSRT) NO puede actualizar perfil sin gate `UNTRUSTED_TIMEBASE`.**
> 2. **Anti-gaming implementado real en emotions = abandono (`ABANDONMENT`) + filtro de anticipación (floor 350 ms) + commission sobre denominador no-go.** NO existe: detector de cadencia de tap, sesgo posicional/de botón, colapso de respuesta por entropía (mencionado en docs, **no implementado**), SSRT/`classifyStop`, `classifyTolerance`, catch-trials, ECE.
> 3. **Banco sin validación.** `PAUSA_WORDS` (24) + `SEPARATOR_ITEMS` (40 = 10/cat, 7 train + 3 eval). **Cero inter-rater, cero holdout activo** (EM3 filtra `{!it.isEval}` → los 12 eval nunca se sirven), cero exposure tracking, cero `culturalLoad`. EM3 shuffle por seed; EM10 igual.
> 4. **Solo 3 paradigmas reales** ⇒ EM1/EM8/EM12 colapsan al mismo go/no-go; EM2/EM11/EM15/EM7 al mismo 2-alt+post-error; EM3/EM10 al mismo 4-cat. **Redundancia masiva.**
> 5. `EmotionTerminologyScanner` está enganchado a `check` y bloquea léxico clínico (whole-word, accent/hyphen-insensitive) sobre `features/mindboost/emotions/**`. **NO existe allowlist de NOMBRES DE CAPACIDAD para UI** (P0 transversal): `displayName` como "Tolerancia a la demanda" no contiene lexema prohibido pero promete un rasgo.

---

## EM1 · Pausa antes de actuar (go/no-go afectivo) — **REVISE**

1. **Veredicto:** REVISE.
2. **Constructo:** Control inhibitorio (comisión) bajo distractor afectivo leve (`PAUSE_BEFORE_ACTION`).
3. **Señal primaria válida:** `COMMISSION_RATE` (falsas alarmas / no-go) — **NO es RT-crítica**, computable sin timebase autoritativa, gate `MIN_NOGO=6`. ESTA sí puede actualizar perfil tras gates.
4. **Señales secundarias (feedback interno):** `RT_MEDIAN`, `ANTICIPATIONS`, `AFFECTIVE_INTERFERENCE_COST` — todas RT-críticas ⇒ display-only/feedback, jamás perfil sin timebase.
5. **Contaminantes:** velocidad pura si `INHIBITORY_DEMAND` bajo; jitter de frame contamina RT; congruencia confunde saliencia con control.
6. **Gaming:** "no responder nunca" → comisión 0% trivial (HOY **no hay suelo de go-accuracy que invalide**; `goAccuracy`/MISS no se enforce en `goNoGoMetrics`); tap-rítmico anticipatorio (floor 350 ms mitiga parcialmente, sin detector de cadencia).
7. **Gates obligatorios:** (a) suelo `goAccuracy>=0.6` que **invalide** el bloque ante omisión masiva (FALTA); (b) `UNTRUSTED_TIMEBASE` en toda métrica RT; (c) `MIN_NOGO` ya presente; (d) abandono no penaliza (OK).
8. **Métrica/scoring:** `commissionScore = 1 - COMMISSION_RATE`, `n_min` no-go = 6; null si `!confident`; invalida si go-accuracy < suelo; agregación inter-sesión por promedio ponderado por trials; actualiza perfil SOLO con comisión confident + go-accuracy válida. Las barras RT nunca al perfil hoy.
9. **Banco:** `PAUSA_WORDS` (24) — distractor, no determina respuesta (verificable). Falta test de paridad tono↔respuesta. No memorizable (es distractor), pero pequeño; rotación por seed OK.
10. **Copy permitido:** "pausa breve antes de responder", "respuestas impulsivas (en esta tarea)", "estimación preliminar".
11. **Copy PROHIBIDO (bloquear en tests):** "control de impulsos", "autocontrol", "impulsividad" como rasgo, "en la vida real", "menos impulsivo".
12. **KMP:** go/no-go ya wired (`AffectiveGoNoGo`, `EmotionGoNoGoField`, `classifyGoNoGo`). Falta: gate go-accuracy en `goNoGoMetrics`; flag `untrustedTimebase` en `EmotionMetricValue`.
13. **Tests:** unit comisión; property (cualquier secuencia → comisión∈[0,1]); adversarial "never-respond" → bloque inválido (FALTA); paridad tono↔respuesta; golden de scoring; copy-scanner sobre displayName.
14. **Aceptación→APPROVE:** suelo go-accuracy implementado+testeado; gate timebase; test de paridad afectiva; copy de capacidad en allowlist UI.
15. **Rediseño:** mantener como ejercicio canónico de comisión; añadir suelo go-accuracy invalidante + `UNTRUSTED_TIMEBASE`. Es el go/no-go base; EM8/EM12 deben fusionarse/diferenciarse contra él.
16. **Impacto (solo entrenamiento):** Medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** comisión es robusta sin RT; el riesgo es vender las barras RT como perfil sin timebase.

---

## EM2 · Recuperación tras error (2-alt + post-error) — **REVISE**

1. **Veredicto:** REVISE.
2. **Constructo:** Recuperación funcional post-error (`POST_ERROR_RECOVERY`), composite condicional.
3. **Señal primaria válida:** `POST_ERROR_RECOVERY` (accuracy post-error penalizando slowing SOLO si no recuperó; gate `MIN_POST_ERROR=3`). **Parcialmente RT-crítica** (el término slowing usa RT): la accuracy-recovery es robusta, el slowing requiere timebase.
4. **Secundarias:** `POST_ERROR_SLOWING` (RT-crítica), `ACCURACY`, `RT_CV`, `ABANDONMENT`.
5. **Contaminantes:** **anticipaciones contadas como "error"** abren ventanas post-error espurias (HOY `postErrorRecovery` define error = `INCORRECT` sin excluir respuestas <floor ⇒ una respuesta rápida-incorrecta cuenta como error genuino); suficiencia post-error no garantizada por seed.
6. **Gaming:** responder rápido-aleatorio infla densidad de errores "baratos"; el composite lo mitiga (impulsividad penaliza) pero no excluye anticipaciones del set de errores.
7. **Gates:** (a) error genuino = respuesta-en-ventana incorrecta **excluyendo anticipaciones** (FALTA en `postErrorRecovery`); (b) `MIN_POST_ERROR=3` presente pero **debe probarse alcanzable en peor seed** (EM11 planta más harder; EM2 base puede quedar con <3 pares); (c) `UNTRUSTED_TIMEBASE` para el término slowing.
8. **Métrica/scoring:** `recoveryScore = clamp(postErrAcc - 0.5·impulsive - penalty, 0,1)`; `n_min`=3 pares; `unavailable` si pares<3 (OK); null nunca 0 (OK); agregación inter-sesión; perfil solo con accuracy-recovery confident (el slowing al perfil exige timebase).
9. **Banco:** procedural (`TwoAltPerceptual`, harder plantado por seed). No memorizable. Sin validación de discriminabilidad psicométrica (el `discriminability` es gap perceptual, no IRT).
10. **Copy permitido:** "volver a tu rendimiento tras un error", "contra tu propio baseline", sin culpa.
11. **Copy PROHIBIDO:** "resiliencia", "tolerancia a la frustración vital", "autoestima", "no te rindas" motivacional.
12. **KMP:** EM2 wired; falta exclusión de anticipaciones en `postErrorRecovery` y test de suficiencia por seed.
13. **Tests:** unit composite (ya hay golden de stats); property (pares≥3 → score∈[0,1]); **adversarial anticipación-como-error** (FALTA); suficiencia peor-seed; copy-scanner.
14. **Aceptación→APPROVE:** error genuino excluye anticipaciones; suficiencia garantizada por seed; gate timebase en slowing.
15. **Rediseño:** mantener como canónico post-error. **EM11 y EM15 deben fusionarse aquí** (mismas métricas, mismo paradigma).
16. **Impacto:** Medio.
17. **Prioridad:** P1.
18. **Riesgo residual:** con n_pares bajo el composite es ruidoso; el `unavailable` honesto lo cubre si se enforce.

---

## EM3 · Separar emoción/pensamiento/impulso/hecho (4-cat) — **BLOCK**

1. **Veredicto:** BLOCK.
2. **Constructo:** Separación metacognitiva de 4 registros (`EMOTION_THOUGHT_SEPARATION`, `GRANULARITY`).
3. **Señal primaria válida:** Ninguna hoy. `CATEGORY_ACCURACY`/`EMOTION_THOUGHT_SEPARATION` **dependen de la "verdad" del item, que NO tiene inter-rater** ⇒ mide concordancia con el autor, no una capacidad.
4. **Secundarias:** `GRANULARITY_INDEX` (categorías distintas correctas/4 — frágil con pocos trials), `RT_MEDIAN`.
5. **Contaminantes:** ambigüedad intrínseca "pensamiento vs impulso" (¿"quiero comprobarlo otra vez" es impulso?); dependencia cultural/léxica; lectura vs concepto.
6. **Gaming:** botón-spam de una categoría (HOY **no hay detector de entropía implementado**, solo mencionado en docs); RT bajo uniforme sin lectura (sin RT-floor por longitud).
7. **Gates:** (a) **inter-rater ≥80% por item o descarte** (BLOQUEANTE, ausente); (b) holdout **activo** (HOY los 12 eval se filtran y nunca se sirven → holdout fantasma); (c) `GRANULARITY_INDEX` gated por n por categoría; (d) RT-floor proporcional a longitud.
8. **Métrica/scoring:** `separationScore=0.6·adjAcc+0.4·granularity`, `adjAcc=(etAcc-0.25)/0.75`; null si `!confident`. **Inutilizable hasta inter-rater** — sin él el score es ruido con apariencia de precisión.
9. **Banco:** `SEPARATOR_ITEMS` (40). **Sin inter-rater, sin localización, holdout reservado-no-servido, sin exposure.** 28 train memorizables tras 2-3 sesiones (shuffle por seed no repone).
10. **Copy permitido:** "distinguir estado, pensamiento, impulso y hecho", "precisión al separarlos (en esta tarea)".
11. **Copy PROHIBIDO:** "insight", "consciencia emocional", "separar pensamientos cura ansiedad", "claridad mental", cualquier beneficio terapéutico.
12. **KMP:** EM3 wired (`Classification`, `EmotionFourCategoryField`); falta capa de validación de banco, holdout real, detector de entropía, RT-floor por longitud.
13. **Tests:** unit clasificación; **inter-rater dataset** (FALTA); holdout disjunto servido; adversarial botón-spam→`GRANULARITY` degenerado; RT-floor; copy-scanner (ya cubre clínico, falta allowlist capacidad).
14. **Aceptación→desbloqueo:** inter-rater ≥80% documentado por item con descarte de no-acuerdo; holdout activo; detector de entropía implementado+testeado; RT-floor proporcional.
15. **Rediseño:** No retirar el concepto (es valioso), pero **REBUILD del banco con protocolo inter-rater** antes de exponer. Reducir a items con acuerdo ≥80%; servir holdout real; transferencia `NOT_DEMONSTRATED` (correcto hoy).
16. **Impacto:** Medio (si el banco se valida).
17. **Prioridad:** P0 (bloqueante de calidad de banco).
18. **Riesgo residual:** aun con inter-rater, "pensamiento vs impulso" puede no tener verdad estable cross-cultural.

---

## EM4 · Calibrador intensidad-rendimiento — **BLOCK (no implementado)**

1. **Veredicto:** BLOCK.
2. **Constructo:** Metacognición de calibración estado↔rendimiento (`CALIBRATION_ACCURACY`, `CALIBRATION_ERROR`).
3. **Señal primaria válida:** Ninguna. **`CALIBRATION_ERROR` está en el enum pero NO se produce** (no hay generador EM4, no hay campo `reportedConfidence`, no hay agregación ECE en el assembler).
4. **Secundarias:** `ACCURACY`.
5. **Contaminantes:** ECE con celdas vacías (n_k bajo) = ruido con precisión falsa; fatiga por bloques largos que el ECE exige.
6. **Gaming:** colapso de confianza a un nivel; reporte sin reflexión (RT confianza ~0).
7. **Gates:** ECE gated por `n_k` mínimo por nivel; feedback diferido por bloque (anti-gaming); confianza POST-respuesta; `UNTRUSTED_TIMEBASE` no aplica (ECE no es RT).
8. **Métrica/scoring:** `calibrationScore=1-ECE`, `ECE=Σ(n_k/N)·|conf_k-acc_k|`; null si algún nivel `n_k<minN`. **No computable hoy.**
9. **Banco:** reusa 2-alt (procedural) + capa confianza; sin banco nuevo.
10. **Copy permitido:** "ajusta tu confianza a tu acierto real (en esta tarea)", "estimación preliminar".
11. **Copy PROHIBIDO:** "autoconocimiento", "mejores decisiones", "conócete", "sesgos".
12. **KMP:** **Deuda real:** generador EM4, `RecordedEmotionTrial.reportedConfidence: Int?`, métrica `CALIBRATION_ERROR` en `DefaultEmotionMetricsEngine`, agregación ECE en assembler, capa de confianza en renderer 2-alt. NADA existe.
13. **Tests:** unit ECE gated por n_k; property; adversarial colapso-de-confianza; golden; copy-scanner.
14. **Aceptación→desbloqueo:** implementar la cadena completa con ECE gated por n_k.
15. **Rediseño:** Concepto valioso y reusable (mapea a EM-E del diseño). **REBUILD desde cero** con confianza post-respuesta + feedback diferido + ECE gated. **Fusionar EM14 (Calibración de confianza) aquí** — son el mismo constructo duplicado.
16. **Impacto:** Medio.
17. **Prioridad:** P2 (no urgente; nada jugable hoy).
18. **Riesgo residual:** n alto para ECE estable ⇒ fatiga; binning honesto obligatorio.

---

## EM5 · Decisiones bajo sesgo afectivo — **BLOCK (no implementado) / RETIRE**

1. **Veredicto:** BLOCK con recomendación de RETIRE.
2. **Constructo:** Calidad de decisión bajo carga afectiva (`AFFECTIVE_DECISION_QUALITY`).
3. **Señal primaria válida:** Ninguna. **`AFFECTIVE_DECISION_QUALITY` y `AFFECTIVE_INTERFERENCE_COST` catalogadas; sin generador EM5, sin métrica de "decision quality" producida.**
4. **Secundarias:** `IMPULSIVITY`.
5. **Contaminantes:** "decisión" implica banco narrativo/escenarios (NO existe en emotions); roza far-transfer a "mejores decisiones" (prohibido).
6. **Gaming:** sin paradigma definido, indefinible.
7. **Gates:** ninguno aplicable hoy.
8. **Métrica/scoring:** indefinido; `AFFECTIVE_DECISION_QUALITY` no tiene fórmula en el motor.
9. **Banco:** inexistente; requeriría escenarios validados que duplicarían el módulo Decisiones.
10. **Copy permitido:** n/a.
11. **Copy PROHIBIDO:** "mejores decisiones", "decide mejor en tu vida", "racionalidad", "debiasing".
12. **KMP:** nada implementado; el constructo "decisión bajo sesgo" es ambiguo entre conflicto perceptual (EM-G) y el módulo Decisiones.
13. **Tests:** n/a.
14. **Aceptación→desbloqueo:** definir un paradigma honesto y distinto de EM-G/Decisiones — no hay diseño defendible que no solape.
15. **Rediseño:** **RETIRE.** Solapa con EM-G (interferencia afectiva, ya cubierta por el coste de congruencia de EM1) y con el módulo Decisiones. El claim "calidad de decisión afectiva" es far-transfer encubierto.
16. **Impacto:** Bajo.
17. **Prioridad:** P3.
18. **Riesgo residual:** mantenerlo en catálogo invita a un claim prohibido; retirarlo elimina el riesgo.

---

## EM6 · Tolerancia a la demanda (Tolerance Hold) — **BLOCK (no implementado, frontera de seguridad)**

1. **Veredicto:** BLOCK (riesgo crítico de inducción + claim de rasgo).
2. **Constructo:** Persistencia segura ante demora (`FRUSTRATION_TOLERANCE`).
3. **Señal primaria válida:** Ninguna. **`FRUSTRATION_TOLERANCE_INDEX`/`SAFE_PERSISTENCE_INDEX` catalogadas, NO producidas; sin generador, sin payload `ToleranceHold`, sin `classifyTolerance`, sin catch-trials.**
4. **Secundarias:** `ABANDONMENT` (ya producido en otros, aquí sería señal de seguridad).
5. **Contaminantes:** **prolongar la espera para medir "tolerancia a la frustración" ES inducción emocional leve** (frontera de seguridad); sin baseline el índice no es interpretable.
6. **Gaming:** tap-spam (sin catch-trials no se detecta); aprender un timer fijo (sin varianza).
7. **Gates:** (a) **prueba de no-inducción** (única carga = demora, sin contenido afectivo escalado) — BLOQUEANTE; (b) abandono NUNCA penaliza ni culpa; (c) catch-trials anti tap-spam; (d) baseline previo o `NOT computable`; (e) `UNTRUSTED_TIMEBASE` (es temporal).
8. **Métrica/scoring:** `toleranceScore = matureHits/(matureHits+earlyTaps)` (abandonos excluidos del denominador); null si denominador<minN; **no computable sin baseline**.
9. **Banco:** tiempos seeded; sin contenido afectivo escalado (requisito de seguridad).
10. **Copy permitido:** "sostén la estrategia ante dificultad", "una pausa cuenta como regulación" (el displayName actual es aceptable salvo "Tolerancia"→capacidad).
11. **Copy PROHIBIDO:** "tolerancia a la frustración", "paciencia", "autocontrol", "aguante", cualquier rasgo vital (`PLACEBO_RISK`).
12. **KMP:** deuda completa: renderer timed-hold con barra+catch, payload `ToleranceHold`, `classifyTolerance`, métricas nuevas. Nada existe.
13. **Tests:** unit clasificación tolerancia; **adversarial tap-spam→catch invalida**; abandono no-penaliza (asserts); no-inducción (scanner + diseño); golden; copy-scanner.
14. **Aceptación→desbloqueo:** prueba documentada de no-inducción + abandono no-penalizado + catch-trials + baseline + copy ultra-conservador.
15. **Rediseño:** Concepto de **mayor riesgo de seguridad** del módulo. Solo procede con zona TOLERANCE estricta, abandono como señal de seguridad, catch-trials obligatorios, baseline previo. Si no se puede probar no-inducción ⇒ **RETIRE**. Renombrar capacidad UI a "persistencia bajo demora (tarea)".
16. **Impacto:** Bajo-Medio (alto riesgo/beneficio desfavorable).
17. **Prioridad:** P0 (frontera de seguridad si se intenta exponer).
18. **Riesgo residual:** incluso bien hecho, fácil de sobre-vender; usuario en distrés.

---

## EM7 · Reentrada tras pausa — **BLOCK (no implementado) / MERGE**

1. **Veredicto:** BLOCK con recomendación de MERGE.
2. **Constructo:** Estabilidad al retomar tras pausa (`REGULATION_STABILITY`).
3. **Señal primaria válida:** Ninguna. **`REGULATION_STABILITY` catalogada, NO producida** (sin generador, sin métrica de slope/estabilidad post-pausa).
4. **Secundarias:** `RT_CV` (RT-crítica).
5. **Contaminantes:** el constructo "estabilidad al volver de una pausa" tiene n minúsculo (1 pausa por bloque ⇒ 1-2 puntos); RT_CV sin timebase.
6. **Gaming:** pausa estratégica para resetear; sin estructura medible.
7. **Gates:** `UNTRUSTED_TIMEBASE`; n suficiente de eventos de pausa (improbable en 2 bloques).
8. **Métrica/scoring:** indefinido; `REGULATION_STABILITY` no tiene fórmula en el motor.
9. **Banco:** reusaría 2-alt/go-no-go; sin banco propio.
10. **Copy permitido:** "retoma la tarea con control tras una pausa (en esta tarea)".
11. **Copy PROHIBIDO:** "regulación emocional", "estabilidad emocional", "vuelve a centrarte".
12. **KMP:** nada implementado; medir estabilidad sobre 1 pausa/bloque es estadísticamente vacío.
13. **Tests:** n/a.
14. **Aceptación→desbloqueo:** no hay diseño con n suficiente; el evento "pausa" es raro por diseño de seguridad.
15. **Rediseño:** **MERGE en EM13 (Variabilidad bajo carga)** — ambos miden `RT_CV`/estabilidad. "Reentrada tras pausa" carece de n. El nombre `REGULATION_STABILITY` es el agujero de claims (regulador captura "regulación emocional" sobre un test de RT).
16. **Impacto:** Bajo.
17. **Prioridad:** P3.
18. **Riesgo residual:** el nombre de capacidad promete regulación afectiva clínica.

---

## EM8 · Anti-impulso por comisión (go/no-go) — **MERGE (en EM1)**

1. **Veredicto:** MERGE en EM1.
2. **Constructo:** Idéntico a EM1 — comisión bajo go/no-go (`PAUSE_BEFORE_ACTION`), solo con `NO_GO_RATIO` más alto (0.40 vs base EM1) y sin balanceo de congruencia.
3. **Señal primaria válida:** `COMMISSION_RATE` (misma métrica, mismo motor `goNoGoMetrics`, mismo clasificador, mismo renderer).
4. **Secundarias:** `RT_MEDIAN`, `IMPULSIVITY`.
5. **Contaminantes:** mismos que EM1; **redundancia: misma varianza que EM1** (`redundantWith` EM1, EM12).
6. **Gaming:** mismo "never-respond" trivial; mismo hueco de suelo go-accuracy.
7. **Gates:** mismos que EM1.
8. **Métrica/scoring:** idéntico a EM1; un parámetro de dificultad (no-go ratio) no justifica un ejercicio separado — es una **variante de dificultad de EM1**, no un constructo distinto.
9. **Banco:** mismo `PAUSA_WORDS`.
10. **Copy permitido:** igual que EM1.
11. **Copy PROHIBIDO:** igual que EM1.
12. **KMP:** `EM8AntiImpulsoGenerator` ya existe; al fusionar, exponerlo como **nivel de dificultad alto de EM1**, no como id de catálogo separado.
13. **Tests:** los de EM1; un golden que pruebe que EM8 = EM1 con no-go ratio mayor.
14. **Aceptación:** colapsar en EM1 como dificultad; mantener generador como parámetro.
15. **Rediseño:** **MERGE.** Tres go/no-go (EM1, EM8, EM12) miden la misma varianza de comisión. Conservar UNO (EM1) con la escalera de no-go ratio cubriendo el rango de EM8.
16. **Impacto:** Bajo (no añade señal nueva).
17. **Prioridad:** P2.
18. **Riesgo residual:** mantenerlo separado infla el catálogo y diluye la señal (10 ejercicios "limpios" > 15 redundantes).

---

## EM9 · Etiquetado funcional rápido — **BLOCK (no implementado) / MERGE**

1. **Veredicto:** BLOCK con recomendación de MERGE/REBUILD.
2. **Constructo:** Granularidad bajo presión (`GRANULARITY`) — mapea a EM-F del diseño.
3. **Señal primaria válida:** Ninguna producida (sin generador EM9, sin banco de valencia, sin IES en emotions).
4. **Secundarias:** `ACCURACY`, `RT_MEDIAN`.
5. **Contaminantes:** **sin banco de valencia disjunto + inter-rater, colapsa a EM3/EM10**; "etiquetar emoción" roza claim de inteligencia emocional (prohibido).
6. **Gaming:** botón-spam (sin detector); RT bajo sin lectura.
7. **Gates:** inter-rater de valencia ≥80% (ausente); banco disjunto de `SEPARATOR_ITEMS`; `UNTRUSTED_TIMEBASE` (IES es RT-crítico); RT-floor por longitud.
8. **Métrica/scoring:** `granularitySpeedScore=0.5·adjAcc+0.5·(1-normIES)`; **IES no computable sin timebase** ⇒ inutilizable hoy.
9. **Banco:** **inexistente** (requiere banco de valencia neutra con inter-rater).
10. **Copy permitido:** "nombra la etiqueta funcional más precisa (en esta tarea)".
11. **Copy PROHIBIDO:** "inteligencia emocional", "reconocer emociones", "empatía", "leer emociones".
12. **KMP:** nada; reusaría renderer 4-cat + banco nuevo. Deuda de banco + IES.
13. **Tests:** inter-rater dataset; adversarial spam; RT-floor; copy-scanner.
14. **Aceptación→desbloqueo:** banco de valencia validado disjunto + IES con gate de timebase, o demostrar que NO es variante trivial de EM3.
15. **Rediseño:** Si no hay banco de valencia con inter-rater, **MERGE en EM3** (mismo 4-cat). Si se construye el banco, **REBUILD** como eje valencia/activación + presión + IES (EM-F), pero sin claim de "reconocer emociones".
16. **Impacto:** Bajo (riesgo de duplicar EM3).
17. **Prioridad:** P2.
18. **Riesgo residual:** "etiquetado afectivo" arrastra el claim prohibido de inteligencia emocional.

---

## EM10 · Separación bajo presión leve (4-cat acelerado) — **MERGE (en EM3)**

1. **Veredicto:** MERGE en EM3.
2. **Constructo:** Idéntico a EM3 (`EMOTION_THOUGHT_SEPARATION`) con ventana de respuesta acortada (`PRESSURE_FACTOR=0.65`). Mismo banco, mismo motor `classificationMetrics`, mismo renderer.
3. **Señal primaria válida:** Misma que EM3 — **y heredera del mismo bloqueo: banco sin inter-rater.**
4. **Secundarias:** `RT_MEDIAN`.
5. **Contaminantes:** **redundancia con EM3** (`redundantWith` EM3); la "presión" es solo un nivel de `TEMPORAL_PRESSURE`, no un constructo distinto; además la presión temporal **sin timebase autoritativa es ruido**.
6. **Gaming:** mismo botón-spam que EM3; la ventana corta favorece respuestas sin lectura.
7. **Gates:** los de EM3 + `UNTRUSTED_TIMEBASE` (la presión hace la latencia load-bearing, agravando la dependencia de timebase).
8. **Métrica/scoring:** idéntico a EM3; la ventana corta no genera una métrica nueva — `CATEGORY_ACCURACY` bajo presión es el mismo eje.
9. **Banco:** mismo `SEPARATOR_ITEMS` (train only). Heredada toda la deuda de validación.
10. **Copy permitido:** igual que EM3 + "con una presión temporal leve".
11. **Copy PROHIBIDO:** igual que EM3.
12. **KMP:** `EM10SepPresionGenerator` ya existe (acorta la ventana); al fusionar, exponer como **nivel de presión temporal de EM3**.
13. **Tests:** los de EM3; golden que EM10 = EM3 con ventana×0.65.
14. **Aceptación:** colapsar en EM3 como variante de dificultad temporal.
15. **Rediseño:** **MERGE.** Es EM3 con `TEMPORAL_PRESSURE` alta. No añade varianza nueva; sí añade dependencia de timebase que EM3 base no tiene.
16. **Impacto:** Bajo.
17. **Prioridad:** P2.
18. **Riesgo residual:** vende "presión" como capacidad cuando es un parámetro; arrastra el bloqueo de banco de EM3.

---

## EM11 · Continuidad tras feedback negativo neutro (2-alt + post-error) — **MERGE (en EM2)**

1. **Veredicto:** MERGE en EM2.
2. **Constructo:** Idéntico a EM2 (`POST_ERROR_RECOVERY`) con más trials `harder` plantados (densidad de error mayor). Mismo motor, mismo renderer, misma métrica.
3. **Señal primaria válida:** `POST_ERROR_RECOVERY` — misma que EM2, con la ventaja de garantizar `>=MIN_POST_ERROR` pares (EM11 planta `MIN_HARD=4`/cada-3).
4. **Secundarias:** `POST_ERROR_ACCURACY`, `ABANDONMENT`.
5. **Contaminantes:** **redundancia con EM2/EM15** (`redundantWith` EM2, EM15); hereda el bug de anticipación-como-error de `postErrorRecovery`.
6. **Gaming:** igual que EM2.
7. **Gates:** los de EM2 (excluir anticipaciones; timebase para slowing).
8. **Métrica/scoring:** idéntico a EM2; la mayor densidad de harder es la **solución de suficiencia que EM2 base necesita** — por eso debe ser la configuración por defecto de EM2, no un ejercicio aparte.
9. **Banco:** procedural (TwoAltPerceptual).
10. **Copy permitido:** "continúa con control tras un aviso neutro de error".
11. **Copy PROHIBIDO:** "feedback negativo" como concepto emocional; "resiliencia"; "no te desanimes".
12. **KMP:** `EM11ContinuidadGenerator` existe; su `plantIndices` resuelve la suficiencia post-error — **portar esa lógica a EM2** y fusionar.
13. **Tests:** los de EM2; suficiencia por seed (EM11 ya la garantiza).
14. **Aceptación:** colapsar en EM2 adoptando la densidad de harder de EM11 como default.
15. **Rediseño:** **MERGE.** EM11 es la versión "con suficiencia garantizada" de EM2 → debe ser EM2. Tres ejercicios post-error (EM2, EM11, EM15) = una sola señal.
16. **Impacto:** Bajo (no añade constructo).
17. **Prioridad:** P2.
18. **Riesgo residual:** "feedback negativo neutro" puede leerse como inducción leve; el copy debe quedar neutro.

---

## EM12 · Espera activa (anticipaciones) — **MERGE (en EM1)**

1. **Veredicto:** MERGE en EM1.
2. **Constructo:** Inhibición de respuesta anticipada (`PAUSE_BEFORE_ACTION`), métrica gobernante `ANTICIPATIONS`.
3. **Señal primaria válida:** Ninguna fiable. **`ANTICIPATIONS` es RT-crítica** (RT<350 ms) ⇒ **sin timebase autoritativa, el conteo de anticipaciones es ruido cross-device** (el jitter de frame del orden de la propia ventana de anticipación). No implementado como generador propio además.
4. **Secundarias:** `RT_MEDIAN`.
5. **Contaminantes:** jitter de frame domina la señal; redundante con la `ANTICIPATIONS` secundaria de EM1.
6. **Gaming:** responder a ritmo fijo justo por encima del floor.
7. **Gates:** `UNTRUSTED_TIMEBASE` **bloqueante** — anticipaciones como métrica PRIMARIA exigen timebase frame-accurate que no existe.
8. **Métrica/scoring:** conteo de anticipaciones; **no puede ser primaria sin timebase**; degradar a secundaria/feedback.
9. **Banco:** reusa go/no-go.
10. **Copy permitido:** "espera la señal sin adelantarte (en esta tarea)".
11. **Copy PROHIBIDO:** "paciencia", "autocontrol", "impulsividad" como rasgo.
12. **KMP:** sin generador EM12; `ANTICIPATIONS` ya es secundaria de EM1.
13. **Tests:** ninguno hoy; al fusionar, los de EM1.
14. **Aceptación:** degradar `ANTICIPATIONS` a señal secundaria de EM1; no exponer como ejercicio con métrica primaria RT.
15. **Rediseño:** **MERGE en EM1.** Hacer una métrica RT-crítica la GOBERNANTE de un ejercicio sin timebase autoritativa es exactamente lo que las reglas prohíben. La anticipación vive como secundaria de EM1.
16. **Impacto:** Bajo.
17. **Prioridad:** P2.
18. **Riesgo residual:** presentar anticipaciones como capacidad medida = falsa precisión.

---

## EM13 · Variabilidad bajo carga (RT_CV) — **BLOCK (no implementado, RT-crítico) / MERGE**

1. **Veredicto:** BLOCK con recomendación de MERGE.
2. **Constructo:** Estabilidad de tiempo de respuesta (`REGULATION_STABILITY` vía `RT_CV`).
3. **Señal primaria válida:** Ninguna. **`RT_CV` es la métrica más RT-crítica del módulo** y es la GOBERNANTE aquí ⇒ sin timebase autoritativa, `RT_CV` cross-device es ruido. No implementado como generador.
4. **Secundarias:** `REGULATION_STABILITY` (catalogada, no producida).
5. **Contaminantes:** jitter de frame infla `RT_CV` artificialmente; carga afectiva confunde con variabilidad motora.
6. **Gaming:** ritmo metronómico para minimizar CV artificialmente.
7. **Gates:** `UNTRUSTED_TIMEBASE` **bloqueante**; `MIN_RT`; agregación inter-sesión.
8. **Métrica/scoring:** `stabilityScore = 1 - normRT_CV`; **no puede actualizar perfil sin timebase**; honesto solo como feedback "ritmo en esta sesión".
9. **Banco:** reusaría go/no-go o 2-alt.
10. **Copy permitido:** "mantén un ritmo estable (en esta tarea)", "variabilidad de tu tiempo de respuesta en esta sesión".
11. **Copy PROHIBIDO:** "regulación", "estabilidad emocional", "constancia mental".
12. **KMP:** sin generador; `RT_CV` ya producido como secundaria en `choiceMetrics`.
13. **Tests:** golden RT_CV; **jank/timebase** (FALTA en todo el módulo); copy-scanner.
14. **Aceptación→desbloqueo:** timebase autoritativa (Choreographer) + gate jank; o degradar a feedback no-perfil.
15. **Rediseño:** **MERGE con EM7** (ambos `REGULATION_STABILITY`). Mantener `RT_CV` solo como feedback de sesión, jamás como perfil, hasta tener timebase. El nombre de capacidad debe ser "estabilidad de ritmo (tarea)".
16. **Impacto:** Bajo.
17. **Prioridad:** P3.
18. **Riesgo residual:** `RT_CV` sin timebase presentado como "regulación" = falsa precisión + claim.

---

## EM14 · Calibración de confianza — **BLOCK (no implementado) / MERGE**

1. **Veredicto:** BLOCK con recomendación de MERGE.
2. **Constructo:** Calibración confianza-acierto (`CALIBRATION_ACCURACY`, `CALIBRATION_ERROR`) — **duplica EM4 exactamente** (misma capacity, misma governing metric).
3. **Señal primaria válida:** Ninguna producida (igual que EM4: sin `reportedConfidence`, sin ECE).
4. **Secundarias:** `ACCURACY`.
5. **Contaminantes:** ECE con celdas vacías; **redundancia total con EM4** (`redundantWith` EM4).
6. **Gaming:** colapso de confianza; reporte sin reflexión.
7. **Gates:** ECE gated por n_k; feedback diferido; confianza post-respuesta.
8. **Métrica/scoring:** idéntico a EM4 (`1-ECE`); no computable hoy.
9. **Banco:** reusa 2-alt + capa confianza.
10. **Copy permitido:** "ajusta tu confianza a tu acierto real (en esta tarea)".
11. **Copy PROHIBIDO:** "autoconocimiento", "intuición", "saber lo que sabes".
12. **KMP:** nada; sería la misma cadena que EM4.
13. **Tests:** los de EM4.
14. **Aceptación:** fusionar con EM4 en un único ejercicio de calibración.
15. **Rediseño:** **MERGE en EM4.** EM4 y EM14 son el MISMO constructo con el mismo símbolo de capacidad y métrica — un duplicado puro del catálogo. Conservar uno.
16. **Impacto:** Bajo.
17. **Prioridad:** P2.
18. **Riesgo residual:** dos ids para el mismo constructo demuestra que el catálogo de 15 se infló por cantidad, no por señal.

---

## EM15 · Recuperación encadenada (2-alt + post-error) — **MERGE (en EM2)**

1. **Veredicto:** MERGE en EM2.
2. **Constructo:** `POST_ERROR_RECOVERY` sobre cadenas de errores seguidos — **mismo constructo/métrica/motor que EM2 y EM11.**
3. **Señal primaria válida:** `POST_ERROR_RECOVERY` (idéntica a EM2). La "cadena de errores" es solo una densidad de harder aún mayor.
4. **Secundarias:** `POST_ERROR_SLOWING`.
5. **Contaminantes:** **redundancia con EM2/EM11** (`redundantWith` EM2, EM11); errores encadenados podrían cruzar a frustración si la densidad es agresiva (frontera de seguridad).
6. **Gaming:** igual que EM2.
7. **Gates:** los de EM2 + control de que la densidad de error no induce frustración (techo leve).
8. **Métrica/scoring:** idéntico a EM2; "encadenada" no genera una métrica de recuperación-en-espiral distinta en el motor actual.
9. **Banco:** procedural.
10. **Copy permitido:** "recupérate de varios errores seguidos (en esta tarea)".
11. **Copy PROHIBIDO:** "espiral", "no entres en bucle" (motivacional), "resiliencia".
12. **KMP:** sin generador EM15; reusaría EM2.
13. **Tests:** los de EM2; control de no-inducción por densidad.
14. **Aceptación:** colapsar en EM2 como nivel de densidad de error.
15. **Rediseño:** **MERGE en EM2.** Tercer clon post-error. La familia post-error (EM2+EM11+EM15) debe ser UN ejercicio con difficulty de densidad de error.
16. **Impacto:** Bajo.
17. **Prioridad:** P2.
18. **Riesgo residual:** densidad alta de error encadenado roza inducción de frustración — requiere techo leve estricto.

---

## Síntesis del panel (catálogo real EM1-EM15)

- **APPROVE:** 0/15 (igual que la auditoría previa — ningún ejercicio pasa limpio).
- **REVISE:** EM1, EM2 (jugables, paradigma sólido; bloqueante = suelo go-accuracy [EM1], exclusión de anticipaciones [EM2], gate `UNTRUSTED_TIMEBASE` en todas las barras RT).
- **BLOCK:** EM3 (banco sin inter-rater, holdout fantasma), EM4/EM5/EM6/EM7/EM9/EM13/EM14 (no implementados + deuda/claims/timebase), EM12 (métrica RT-crítica como primaria sin timebase).
- **MERGE/RETIRE:** EM8→EM1, EM10→EM3, EM11→EM2, EM12→EM1, EM14→EM4, EM15→EM2 (clones de 3 paradigmas); EM5→RETIRE (far-transfer encubierto), EM6→BLOCK/posible RETIRE (frontera de inducción).
- **Catálogo limpio defendible: ~4-5 ejercicios** (1 go/no-go comisión = EM1; 1 post-error = EM2; 1 separación 4-cat con banco validado = EM3; 1 calibración ECE = EM4; opcional 1 conflicto/interferencia afectiva = EM-G). El catálogo de 15 colapsa a **3 paradigmas reales + 2 deudas honestas**. **Menos pero limpios > 15 ruidosos.**
- **P0 transversales:** (1) **Allowlist de NOMBRES DE CAPACIDAD para UI** separada de los símbolos de código (`FRUSTRATION_TOLERANCE`, `REGULATION_STABILITY` no pueden aparecer en pantalla — el scanner whole-word no los bloquea); (2) gate `UNTRUSTED_TIMEBASE` que impida que CUALQUIER métrica RT-crítica actualice perfil (afecta a la mayoría de secundarias y a EM12/EM13 como primarias); (3) inter-rater ≥80% como precondición de release para EM3/EM9; (4) toda métrica catalogada-no-producida arranca `unavailable`/`calibrating`, jamás 0 (ya respetado por `EmotionMetricValue.unavailable`, pero EM4-EM15 ni siquiera la producen); (5) suelo go-accuracy invalidante (EM1/EM8) y exclusión de anticipaciones (EM2/EM11/EM15) — anti-gaming hoy ausentes.
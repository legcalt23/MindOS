# Auditoría Velocidad

… (see exercises array; full 18-point fichas below)

# Auditoría MindBoost — Módulo VELOCIDAD (8 ejercicios diseñados)

> **Veredicto global del módulo: REBUILD parcial / catálogo sobre-dimensionado.** El diseño promete 8 ejercicios premium con métricas avanzadas (switch-cost, congruency-effect, timing con signo, detección RSVP por frame real, calibración metacognitiva). El **código real** solo tiene 4 generadores 2-AFC delgados (E1 Destello, E3 Cifrado=code-verify, E6 Multivía=reacción simple, E7 Compuerta=regla condicional) que **no implementan ninguno de los 8 diseños**. Tres bloqueadores transversales destruyen casi todos los claims de perfil:
>
> 1. **GATE DE JANK MUERTO (BLOQUEADOR P0 GLOBAL).** `SpeedSessionStateHolder.kt:259-260` fija `frameDropFraction = 0.0` y `criticalFrameJitter = false` HARDCODED. `SessionQualityEvaluator` y `DeviceTimingRisk` existen como modelo pero **nunca reciben datos reales**. No hay medición de frames en ningún módulo, no hay `CADisplayLink` en iOS. Conclusión: NINGÚN ejercicio de Velocidad puede declarar `SessionQuality` fiable hoy → todos `preliminary=true` y ninguno actualiza perfil hasta que se mida jank de verdad.
> 2. **TIMEBASE AUTORITATIVA SOLO Android API34+.** Choreographer+MotionEvent solo en `SpeedTimebase.android.kt`; desktop/iOS caen a `FallbackSpeedTimebaseHandle` no-autoritativo (`MILLIS_DERIVED`). RT crítico fuera de Android = `UNTRUSTED_TIMEBASE` → jugable sí, profile-update NO (regla 1).
> 3. **ADAPTATIVO Y MÉTRICAS AVANZADAS = DEUDA, NO REUSO.** `MetricCalculator` NO computa switch-cost, congruency-effect, timing-con-signo, d', ni calibrationError. El propio diseño lo admite ("deuda nueva en speed"). El runtime real usa `FixedTrialEngine`; `MultiSignalStaircaseEngine` no aparece en el código de speed. Anti-gaming real en speed = anticipation filter + impulsivityIndex + quality-gate (este último con entradas muertas). NO hay detector de cadencia de tap real, ni sesgo posicional, ni stop-signal en speed (SsrtCalculator vive en focus).
>
> **Decisión de catálogo:** de 8 diseñados, los 8 son APPROVE-with-changes en el doc, pero contra el ground-truth: 0 están implementados como se diseñan. Recomiendo CONSTRUIR 4 señales limpias y ORTOGONALES (perceptiva pura, interferencia/conflicto, freno/inhibición, throughput-bajo-carga) y RETIRAR/diferir el resto por redundancia o por exigir infraestructura ausente (timing con signo, RSVP por frame real, metacognición ruidosa). Menos pero limpios.

Las 18 fichas completas por ejercicio están en el campo `exercises` (resumen estructurado) y desarrolladas a continuación.

---

## E1 — Discriminación Cromática Binaria (Tinte Crítico)
1. **Veredicto: REVISE.** Señal perceptiva pura defendible y el renderer choice ya existe (E1 Destello en código), pero el diseño (ΔE CIELAB, axisRotation, luminanceNoise) no está implementado y depende de calibración de pantalla no controlable.
2. **Constructo:** velocidad de decisión perceptiva 2-AFC pura (transducción estímulo→categoría→respuesta), casi libre de carga semántica.
3. **Señal primaria válida:** `rtMedian` de HIT-RT MAD-limpio **solo en Android autoritativo + gate de jank vivo**; sin esos dos gates → null/preliminary, no actualiza perfil.
4. **Señales secundarias:** pc (accuracy de lado), `cv` (RT-CV), pendiente RT vs deltaPerceptual (función psicométrica de coste), `anticipationRate`, `impIdx` — feedback interno, no claim absoluto.
5. **Contaminantes:** ΔE percibido varía por pantalla/daltonismo (no comparable cross-device); aprendizaje del mapeo fijo LEFT/RIGHT; latencia táctil del dispositivo. **Redundante con cualquier otro 2-AFC perceptivo si no se diferencia el eje.**
6. **Gaming:** tap rítmico anticipatorio (adivinar con ISI predecible); aprender "siempre cálido→derecha"; tap-spam con ~50% de acierto. El sintético de tap aleatorio rápido DEBE puntuar bajo (el Revisor lo exige y NO está testeado).
7. **Gates obligatorios:** timebase Android autoritativo; jank-gate REAL (hoy muerto); `MIN_N_RT`; `axisRotationDeg` activo ≥30% trials (anti-aprendizaje de mapeo); anclaje SIEMPRE al baseline del propio dispositivo (`DeviceProfile`), nunca cross-device; abandono no penaliza.
8. **Métrica/scoring:** `usefulSpeed=clamp(VCE/VCE_ref_baseline)`, `control=(1-impIdx)*pc`, `stability=(1-cv)*(1-lapseRate)`, `total=mean(...)`, techo ×0.5 si pc<0.80||cv>0.30. n_min: ≥5 HIT-RT/celda y ≥20 trials/sesión (patrón Focus). null si !confident; calibrating hasta baseline estable; agregación inter-sesión ponderada por trials. Actualiza perfil solo con jank-gate vivo + Android.
9. **Banco:** procedural seeded (`XorShiftSpeedRng`), no memorizable como contenido (es perceptual); rotar `axisRotationDeg` por sesión; sin holdout textual necesario; HIGH_PRIOR irrelevante.
10. **Copy permitido:** "señal de práctica de velocidad perceptiva", "estimación preliminar", "comparado con tu propio baseline en este dispositivo", "datos insuficientes — sigue practicando".
11. **Copy prohibido (bloquear en scanner):** "velocidad mental", "agudeza visual", "reflejos", "mejora tu rapidez en la vida diaria/conducción/lectura", "test de inteligencia/IQ".
12. **KMP:** reusa `E1DestelloScreen`/renderer choice; `TinteGenerator: SpeedPromptGenerator` (deriva correctChoice, nunca lo expone); `SpeedTrialPrompt`+deadline+RNG; `MetricCalculator.blockMetrics/sessionMetrics`; persist `MeasurementRun` en `SecureSpeedMeasurementStore`; DTO @Serializable+schemaVersion. Falta: ejes ΔE/luminanceNoise.
13. **Tests:** determinismo seed→secuencia; property (correctChoice siempre derivado del tinte); **adversarial sintético tap-aleatorio→score bajo (FALTA hoy)**; golden de scoring; jank-gate test con frameDrop real; copy-scanner.
14. **Aceptación→APPROVE:** jank-gate vivo; sintético adversario puntúa bajo y testeado; axisRotation activo; variante luminancia daltonismo-safe; anclaje device-local probado.
15. **Rediseño:** mantener como ÚNICA señal perceptiva pura del módulo (eje **luminancia** por defecto, daltonismo-safe; tono como variante), axisRotation obligatorio, banco procedural device-anchored, gate de jank antes de cualquier `usefulSpeed`. Si no se construye el jank-gate → degradar a "ejercicio de práctica" sin contribución a perfil.
16. **Impacto (solo entrenamiento):** medio.
17. **Prioridad: P1.**
18. **Riesgo residual:** sin jank-gate vivo, el RT mide GPU+pantalla, no percepción; cross-device sigue siendo ruido aunque se ancle.

---

## E2 — Sustitución Símbolo-Dígito Rápida (Cifrado Veloz)
1. **Veredicto: REVISE.** Buen constructo (throughput perceptivo-asociativo), pero a keySize≤4 con leyenda visible degenera en **búsqueda visual** (duplica E7-búsqueda) y el código actual (E3 Cifrado) es un simple match-verify de 2 vías, no DSST.
2. **Constructo:** velocidad de procesamiento con regla de mapeo símbolo→tecla mantenida activa (lectura+consulta+respuesta), no memorización.
3. **Señal primaria válida:** throughput (correctos/min) + `rtMedian` HIT MAD-limpio, **con la pendiente RT/keySize reportada para no confundir search con speed**; gates Android+jank.
4. **Señales secundarias:** pendiente RT vs keySize (coste de carga de mapeo, debe ser PRIMARIA por el riesgo de búsqueda), `cv`, `lapseRate`, `omissionRate`, `impIdx`.
5. **Contaminantes:** sacadas oculares (a keySize alto domina el RT → mide búsqueda, no velocidad); memorización de leyenda si no rota; **redundante con un ejercicio de búsqueda visual y con E1 si el mapeo es trivial.**
6. **Gaming:** memorizar la leyenda (mitigado regenerando por bloque); respuesta-por-posición si los distractores no rompen el layout; tap rítmico con keySize=2.
7. **Gates:** regenerar leyenda por bloque (anti-memoria); `legendShufflePerTrial` ante sospecha; capar `keySize`≤6 en runtime (8 solo variante de carga); `legendVisibleMs` para forzar codificación; Android+jank+MIN_N; abandono no penaliza.
8. **Métrica/scoring:** throughput escalado como modificador de `usefulSpeed`; `control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; techo estándar. n_min ≥5 HIT-RT y suficientes trials por keySize para la pendiente; null si pendiente no estimable; calibrating hasta baseline; inter-sesión ponderada.
9. **Banco:** leyenda procedural regenerada por bloque (símbolos neutros no-lingüísticos); rotación obligatoria; HIGH_PRIOR no aplica; anti-memorización = regeneración + shuffle.
10. **Copy permitido:** "throughput de codificación", "estimación preliminar", "tu ritmo vs tu baseline".
11. **Copy prohibido:** "el DSST predice inteligencia/salud cerebral", "marcador clínico", "velocidad mental general".
12. **KMP:** reusa renderer choice + slot leyenda (componente pequeño nuevo); el `CifradoGenerator` actual NO es DSST → reescribir con `legend:Map<Int,SpeedChoice>` y derivar correctChoice; `CifradoPrompt(symbolId, legend, correctChoice)`; persist `MeasurementRun`. Falta: pendiente RT/keySize en `MetricCalculator` (deuda).
13. **Tests:** determinismo; property (correctChoice = legend[symbol]); adversarial (memorización con leyenda fija debe detectarse); golden scoring; pendiente RT/keySize unit; copy-scanner.
14. **Aceptación:** pendiente RT/keySize como métrica primaria implementada; keySize capado en runtime; ventana de leyenda; jank-gate vivo.
15. **Rediseño:** mantener SOLO como "throughput-bajo-carga de mapeo" diferenciado por la pendiente RT/keySize; si no se separa search de speed → MERGE con un único ejercicio de throughput o RETIRE por redundancia con búsqueda.
16. **Impacto:** medio.
17. **Prioridad: P2.**
18. **Riesgo residual:** sin la pendiente como gate, mide búsqueda visual disfrazada de velocidad; la implementación actual (match-verify) no mide el constructo.

---

## E3 — Conmutación Perceptiva de Tarea (Multivía Veloz)
1. **Veredicto: BLOCK.** La señal primaria (SWITCH_COST) **no existe en el código de speed** (`MetricCalculator` no lo computa) y exige n por celda que una sesión corta no garantiza. Hasta tener el cómputo y el MIN_N por celda probado, no es un ejercicio medible: es diseño.
2. **Constructo:** velocidad de reconfiguración de regla (switch-cost a alta velocidad), throughput-bajo-cambio.
3. **Señal primaria válida:** `SWITCH_COST = rtMedian_switch − rtMedian_repeat` (solo HITs) **con HIT-RT en ambos lados y MIN_N por celda**; si falta un lado o n<MIN_N → null (no fabrica, no penaliza).
4. **Señales secundarias:** efecto de congruencia (separado del switch-cost para no contaminarlo), pendiente switch-cost vs CSI, `cv`, `impIdx`, `lapseRate`.
5. **Contaminantes:** efecto de congruencia se mezcla con switch-cost si no se separan; lectura del cue (CSI largo) mide lectura, no reconfiguración; n insuficiente en celda switch. **Redundante con E4(go/no-go) y E6(flanker) bajo el paraguas "control a velocidad" si no se prueba ortogonalidad.**
6. **Gaming:** responder por una dimensión fija ignorando el cue (la incongruencia + `impIdx` deben filtrarlo); tap rítmico.
7. **Gates:** **MIN_N por celda switch/repeat documentado y enforced** (4 bloques×24 con 0.4 switch ≈38 switch-HITs: límite); switch-cost null honesto; congruencia reportada aparte; Android+jank; abandono no penaliza.
8. **Métrica/scoring:** `usefulSpeed=VCE/VCE_ref` penalizado por switch-cost normalizado `*=(1-clamp(switchCost/ref)*w)`; switch-cost null NO penaliza; techo estándar. n_min por celda crítico; calibrating hasta acumular celdas inter-sesión; actualiza perfil solo con celdas suficientes + Android + jank.
9. **Banco:** secuencia switch/repeat seeded balanceada y auditable; cues no-lingüísticos (icono); rotación de mapeo.
10. **Copy permitido:** "coste de cambiar de regla (preliminar)", "señal de práctica de reconfiguración".
11. **Copy prohibido:** "flexibilidad cognitiva general", "multitarea de la vida real", "función ejecutiva".
12. **KMP:** requiere portar lógica de cue/regla (`RuleSwitch` de Focus E6) al runtime de speed + slot de cue; `MultiviaPrompt(rule,colorIsRed,shapeIsCircle,isSwitch,correctChoice)`. **El `MultiviaGenerator` actual es reacción-simple (flecha→lado), NO task-switch** → reescribir. `MetricCalculator` debe ganar SWITCH_COST (deuda real). Persist `MeasurementRun`.
13. **Tests:** determinismo; property (correctChoice derivado de rule+estímulo); **switch-cost null cuando n<MIN_N por celda (obligatorio)**; congruencia separada de switch-cost (test de no-contaminación); adversarial (responder por dimensión fija); golden scoring; jank; copy-scanner.
14. **Aceptación:** SWITCH_COST computado en speed, MIN_N por celda probado con null honesto, congruencia separada, jank-gate vivo.
15. **Rediseño:** si se construye, es la ÚNICA señal de "control secuencial/reconfiguración". Pero dado el coste (nuevo cómputo + n alto por celda + redundancia con E4/E6), recomiendo **diferir a P3** y elegir UNA sola tarea de control (ver E6) salvo que se demuestre ortogonalidad empírica. Mientras tanto: BLOCK.
16. **Impacto:** medio (alto solo si se prueba ortogonalidad).
17. **Prioridad: P3.**
18. **Riesgo residual:** alto — diseño sin cómputo; muy probable que el switch-cost quede siempre null en sesiones cortas y que sea redundante con E4/E6.

---

## E4 — Compuerta Lógica Express (Go/No-Go Veloz Asimétrico)
1. **Veredicto: REVISE.** El mejor candidato de "freno/inhibición" del módulo: go/no-go con `faRate` real (a diferencia de E1/E6). Pero el gate de comisión prioritario NO está testeado con sintético impulsivo en speed, y `faRate` necesita MIN_N de no-go (raro por diseño).
2. **Constructo:** velocidad de go-response con coste de control (equilibrio velocidad/impulsividad), curva velocidad-control.
3. **Señal primaria válida:** `rtMedian` GO-HIT MAD-limpio + pc + `faRate` (comisión NO-GO) **con MIN_N de no-go**; gates Android+jank.
4. **Señales secundarias:** `omissionRate` (separada de comisión), post-error slowing, `cv`, `anticipationRate`, `lapseRate`.
5. **Contaminantes:** noGoRate bajo → pocos no-go → `faRate` poco confident; tap-spam dispara comisión; lure mal calibrado. **Diferenciado de E3/E6 SOLO si se prueba que inhibición-de-prepotencia ≠ reconfiguración ≠ interferencia.**
6. **Gaming:** "tap a todo verde-ish" (mitigado por `lureRate`); acelerar a costa de inhibición si el gate de comisión está mal calibrado (riesgo central que el Revisor marca).
7. **Gates:** **gate de comisión (LOWER por faRate/comisión) con PRIORIDAD sobre RAISE, testeado con perfil sintético impulsivo**; MIN_N de no-go antes de `faRate`; techo incluye `faRate>0.30` además de pc/cv; Android+jank; abandono no penaliza.
8. **Métrica/scoring:** `control=(1-faRate)*pc` (comisión penaliza directamente); `usefulSpeed=VCE/VCE_ref` solo GO-HIT; `stability=(1-cv)*(1-lapseRate)`; techo ×0.5 si pc<0.80||cv>0.30||faRate>0.30; macro `FAST_IMPULSIVE` si VCE alta+faRate alta. n_min: GO-HIT≥5 y no-go suficientes; faRate null honesto bajo n; calibrating hasta baseline; actualiza perfil con gates.
9. **Banco:** procedural seeded; lure perceptivo (doble canal color+forma daltonismo-safe); rotación; sin texto.
10. **Copy permitido:** "equilibrio velocidad-control (preliminar)", "comisiones = respuestas que no frenaste", mensaje neutro sin culpa.
11. **Copy prohibido:** "control de impulsos en la vida real", "TDAH/diagnóstico" (PROHIBIDO), "autorregulación clínica".
12. **KMP:** reusa renderer single-tap (E7 Compuerta en código es regla-condicional 2-vías, NO go/no-go → necesita generador go/no-go real con noGoRate/lure); `MetricCalculator` ya soporta `faRate` donde hay no-go; `CompuertaPrompt(isGo,isLure,correctChoice?)`. Variante stop-signal reusa `SsrtCalculator`+`StopSignalTracker` de focus con `trustworthy=false` si p(respond|signal)∉[0.25,0.75].
13. **Tests:** determinismo; **adversarial sintético impulsivo: gate de comisión debe bajar dificultad, NUNCA acelerar (obligatorio)**; faRate null bajo MIN_N; techo por faRate; golden scoring; jank; copy-scanner.
14. **Aceptación:** gate comisión prioritario testeado con impulsivo; techo faRate; MIN_N no-go; jank-gate vivo.
15. **Rediseño:** **promover a la señal de inhibición canónica del módulo** (reemplaza E3 si E3 no prueba ortogonalidad). La variante stop-signal/SSRT es valiosa pero es un ejercicio distinto (E4 de Focus) → no duplicar aquí, enlazar.
16. **Impacto:** alto (si gate comisión probado).
17. **Prioridad: P1.**
18. **Riesgo residual:** si el gate de comisión no se testea, es "impulsividad recompensada" — violación directa de la regla anti-gaming.

---

## E5 — Cadencia Anticipatoria (Tiempo de Coincidencia)
1. **Veredicto: BLOCK (→ REBUILD o RETIRE).** Mide **predicción temporal, no RT**, y su señal central (error con signo, adelanto negativo) **rompe `TimebasePolicy.computeRtNanos` que LANZA con rt<0**. Requiere una `TimingPolicy` con signo NUEVA (no existe) y es el ejercicio MÁS dependiente de timebase autoritativo + latencia táctil device-anchored. Sin esa infraestructura no compila la semántica.
2. **Constructo:** precisión temporal predictiva (error temporal con signo y su variabilidad), no RT reactivo.
3. **Señal primaria válida:** `timingError_median` (con signo), `timingError_absMedian`, `tol-hit rate` — **solo con `TimingPolicy` con signo + Android autoritativo + jank-gate + anclaje device-local de latencia táctil**.
4. **Señales secundarias:** `timingVariability` (SD del error), sesgo direccional (adelanto vs retraso), pendiente error vs occlusionFraction.
5. **Contaminantes:** latencia táctil del dispositivo = sesgo CONSTANTE que invalida cross-device; sin oclusión mide reacción visual al borde, no predicción; jank. **Único en el módulo (no redundante) pero por eso mismo el más caro de hacer bien.**
6. **Gaming:** tocar sistemáticamente pronto ("tapear ya") — penalizado por error con signo; tap-a-cualquier-movimiento — penalizado por distractores.
7. **Gates:** `TimingPolicy` con signo (NUEVA, obligatoria); oclusión obligatoria (fuerza extrapolación); CONTAMINATED en fallback desktop/iOS; anclaje al baseline del MISMO dispositivo; jank-gate vivo; abandono no penaliza.
8. **Métrica/scoring:** `stability=1-clamp(timingVariability/var_ref)`; `control=1-clamp(|biasAdelanto|/bias_ref)`; `usefulSpeed=tol-hit ponderado por estrechez de tol`; techo si tol-hit<0.80||variability>cap. n_min de tol-hits; null si insuficiente; calibrating; perfil solo Android+jank+timing-policy.
9. **Banco:** trayectorias procedurales seeded; jitter de velocidad/curvatura; oclusión variable; no memorizable.
10. **Copy permitido:** "precisión de anticipación temporal (preliminar)", "+adelanto / −retraso en ms vs tu baseline en este dispositivo".
11. **Copy prohibido:** "reflejos deportivos", "tiempo de reacción", "conducción", "rapidez general".
12. **KMP:** **renderer dinámico nuevo** (animación en composable keyed por seed, patrón MOT/RSVP de Focus; una pulsación); `cos/sin`→`.toFloat()` antes de `Offset`; **`TimingPolicy` con signo (deuda crítica, `computeRtNanos` actual lanza con negativos)**; `MetricCalculator` necesita error temporal con signo (deuda); `CadenciaTrial(coincidenceUptimeNanos,tolNanos,occlusionFraction)`.
13. **Tests:** determinismo; property (signedError puede ser negativo sin lanzar); adversarial (tap-pronto sistemático → score bajo); CONTAMINATED en fallback; oclusión fuerza extrapolación (sin oclusión vs con oclusión); golden scoring; jank; copy-scanner.
14. **Aceptación:** `TimingPolicy` con signo implementada y testeada; latencia táctil cancelada por baseline device-local demostrado; oclusión obligatoria; CONTAMINATED fuera de Android.
15. **Rediseño:** si se construye, es la señal de **timing predictivo** (única, no redundante). Pero el coste (renderer nuevo + policy nueva + dependencia extrema de hardware) es alto. Recomendación: **diferir (P3); si no se prioriza la infraestructura de timing con signo en 1 release, RETIRE** — un ejercicio cuya señal central rompe el motor de RT no es defendible como "reuso".
16. **Impacto:** alto (señal genuinamente distinta) pero solo si la infra existe.
17. **Prioridad: P3.**
18. **Riesgo residual:** muy alto — sin `TimingPolicy` con signo el ejercicio no tiene semántica; el score sería ruido de hardware táctil.

---

## E6 — Resolución de Conflicto Veloz (Flanker Express)
1. **Veredicto: REVISE.** Constructo limpio (interferencia espacial-perceptiva simultánea), estímulos no-lingüísticos (flechas) evitan el problema cultural del Stroop. Pero CONGRUENCY_EFFECT **no existe en `MetricCalculator` de speed** y se **solapa con E3(switch) y E4(go/no-go)** bajo "control a velocidad" — la ortogonalidad es declarada, no probada.
2. **Constructo:** velocidad de selección bajo interferencia (efecto de congruencia = coste de filtrar flancos), eficiencia de selección perceptiva.
3. **Señal primaria válida:** `CONGRUENCY_EFFECT = rtMedian_incong − rtMedian_cong` (solo HITs) **con HIT-RT en ambos lados + MIN_N por celda**; pc; `rtMedian` global; gates Android+jank.
4. **Señales secundarias:** comisión direccional en incongruentes, post-conflicto adjustment (Gratton), `cv`, `anticipationRate`, `lapseRate`.
5. **Contaminantes:** congruency null en sesiones cortas; **redundancia con E3 (switch-cost) y E4 (comisión) — los tres "control a velocidad"**; flechas mal espaciadas → no inducen interferencia.
6. **Gaming:** responder al flanco fijo (incongruencia lo detecta); tap rítmico (anticipationRate/impIdx).
7. **Gates:** estímulos no-lingüísticos OBLIGATORIOS (flechas/formas, NO palabras-color → regla cultural); MIN_N por celda; congruency null honesto; **declarar y PROBAR ortogonalidad vs E3/E4** (el brain-profile las trata como sub-señales distintas); Android+jank; abandono no penaliza.
8. **Métrica/scoring:** `usefulSpeed=VCE/VCE_ref` penalizado `*=(1-clamp(congrEffect/ref)*w)`; congruency null NO penaliza; `control=(1-impIdx)*pc`; techo estándar. n_min por celda; null/calibrating; inter-sesión acumula celdas; perfil con gates.
9. **Banco:** procedural seeded balanceado cong/incong; flechas/formas no-lingüísticas; rotación.
10. **Copy permitido:** "coste de filtrar interferencia (preliminar)", "señal de práctica de selección perceptiva".
11. **Copy prohibido:** "atención general", "filtrado en entornos reales", "control ejecutivo", palabras-color dependientes de idioma.
12. **KMP:** reusa renderer choice; `FlankerExpressGenerator` seeded (deriva correctChoice del centro); `FlankerPrompt(centerDir,congruent,flankerCount,correctChoice)`; **CONGRUENCY_EFFECT en `MetricCalculator` = deuda (patrón E2 de Focus reusable)**.
13. **Tests:** determinismo; property (correctChoice = centro); MIN_N por celda → null; **ortogonalidad vs switch-cost/comisión (correlación baja en sintéticos)**; no-palabras (copy-scanner); golden scoring; jank.
14. **Aceptación:** CONGRUENCY_EFFECT computado; ortogonalidad probada; no-lingüístico forzado; MIN_N por celda; jank-gate vivo.
15. **Rediseño:** **elegir E6 como la ÚNICA tarea de conflicto/interferencia** y MERGE/RETIRE E3 (switch) salvo ortogonalidad empírica; el flanker es más barato (sin n por celda switch tan alto, sin cue) y no-lingüístico por construcción. Diferenciar de E4 manteniéndolo en 2-AFC sin no-go.
16. **Impacto:** medio-alto.
17. **Prioridad: P1** (como tarea de control canónica, sustituyendo E3).
18. **Riesgo residual:** sin prueba de ortogonalidad, el módulo tiene 3 ejercicios midiendo la misma varianza de "control".

---

## E7 — Ráfaga de Detección Serial (RSVP Veloz)
1. **Veredicto: BLOCK.** Su honestidad depende ENTERAMENTE de medir el **onset real de cada ítem por `frameTimeNanos`** y de un **gate de jank vivo** — y el jank-gate está MUERTO (hardcoded). Un SOA nominal de 80 ms con frames de 16.6 ms es una mentira sin medición de frame real. Mide el GPU, no la percepción.
2. **Constructo:** velocidad/umbral de detección perceptiva en flujo serial rápido (throughput perceptivo bajo presión de presentación).
3. **Señal primaria válida:** `hitRate`, `rtMedian` de detección (onset-objetivo→tap), umbral `itemDurationMs` al 79% — **solo con SOA efectivo medido por frame real + jank-gate vivo + Android**.
4. **Señales secundarias:** `faRate`, `omissionRate`, d' (sensibilidad), attentional-blink cost, `cv`.
5. **Contaminantes:** frame rate real ≠ nominal (throttling); jank; tap único no distingue a qué ítem responde (debe atribuirse al ítem visible en `tapUptime`, no al objetivo asumido). **Único como "detección serial" — no redundante, pero el más frágil a hardware.**
6. **Gaming:** tap-anticipado/spam durante la ráfaga (false alarm en ráfagas sin objetivo lo castiga; `targetProbability<1` y d' separan detección de sesgo).
7. **Gates:** **onset real por `frameTimeNanos` + SOA efectivo (no nominal)**; **gate de jank: CONTAMINATED si varianza de frame alta (HOY MUERTO)**; piso `itemDurationMs`≥2 frames del dispositivo; atribución de tap al ítem visible; Android; abandono no penaliza.
8. **Métrica/scoring:** `usefulSpeed=1-clamp(umbralItemDuration/dur_ref)`; `control=(1-faRate)*hitRate`; `stability=(1-cv)*(1-omissionRate)`; techo si hitRate<0.80||faRate>0.30. n_min de hits/ráfagas; null si insuficiente; calibrating; perfil solo con frame-real + jank-gate.
9. **Banco:** ráfagas procedurales seeded (posición objetivo jitter, distractores); ítems neutros no-lingüísticos; rotación; sin memorización.
10. **Copy permitido:** "umbral de detección rápida (preliminar)", "señal de práctica de detección en flujo".
11. **Copy prohibido:** "lectura veloz", "percepción general", "atención sostenida".
12. **KMP:** reusa renderer Stream/RSVP de Focus E10 (animación en composable, una pulsación); `RafagaGenerator` seeded; `RafagaTrial(targetSerialPos?,itemDurationMs,items)`; **onset por Choreographer `frameTimeNanos`**; `MetricCalculator` necesita hit/fa/d' serial (deuda); **medición de frame real + jank (infraestructura ausente hoy)**.
13. **Tests:** determinismo; property (atribución de tap al ítem visible); SOA efectivo vs nominal; **CONTAMINATED bajo jank real (requiere medidor de frames)**; d' separa detección de sesgo; golden scoring; copy-scanner.
14. **Aceptación:** medición de onset real por frame; jank-gate vivo con CONTAMINATED; piso de itemDuration en frames; atribución de tap correcta.
15. **Rediseño:** **NO construir hasta que exista medición de frame real + jank-gate** (es el bloqueador P0 global). Si se construye después, es la señal de detección serial (única). Mientras tanto BLOCK; si la infra de frames no se prioriza → RETIRE.
16. **Impacto:** medio (alto solo con infra de frames).
17. **Prioridad: P3** (bloqueado por P0 de jank/frames).
18. **Riesgo residual:** crítico — sin medición de frame real el `itemDurationMs` es ficción y el score mide el render pipeline.

---

## E8 — Calibración de Apuesta Temporal (Velocidad-Confianza Metacognitiva)
1. **Veredicto: BLOCK.** Mezcla DOS constructos (velocidad + metacognición temporal); la señal metacognitiva (`calibrationError`) es **ruidosa, necesita multi-bloque para confident, y es explotable** eligiendo siempre el deadline más holgado ("premiar la cobardía"). El recentrado anti-gaming y la penalización de under-commit son DISEÑO, no código. No existe `calibrationError` en `MetricCalculator`.
2. **Constructo:** calibración velocidad-exactitud (alineamiento entre deadline prometido y rendimiento real), no velocidad pura.
3. **Señal primaria válida:** `calibrationError = |deadline prometido − RT-mediano real cumpliendo accuracy|` **reportada SEPARADA del score de velocidad, multi-bloque, preliminary**; pc bajo compromiso.
4. **Señales secundarias:** `overCommitRate`, `underCommitRate`, tendencia de calibración entre bloques, `cv`, `impIdx`.
5. **Contaminantes:** under-commit trivial (deadline holgado → accuracy trivial → calibrationError bajo falso); ruido alto en sesión única; **contamina el score de speed si no se separa el modificador metacognitivo.**
6. **Gaming:** elegir siempre el deadline más lento (cobardía premiada) — requiere penalizar under-commit Y recentrar/estrechar la rejilla eliminando la opción trivialmente lenta.
7. **Gates:** penalizar under-commit explícitamente; recentrado que ESTRECHE opciones sobre el umbral real; metacog SEPARADO del score de velocidad; multi-bloque obligatorio antes de cualquier claim (null/preliminary); Android+jank; abandono no penaliza.
8. **Métrica/scoring:** `metacog=1-clamp(calibrationError/ref)` reportado como señal descriptiva separada (NO como velocidad); el score de speed sigue siendo usefulSpeed/control/stability del 2-AFC subyacente. n_min: varios bloques; null hasta entonces; calibrating; perfil: metacog NUNCA entra en la dimensión PROCESSING_SPEED.
9. **Banco:** 2-AFC subyacente procedural seeded; rejilla de deadlines recentrada por baseline; sin contenido memorizable.
10. **Copy permitido:** "calibración velocidad-exactitud (preliminar, datos insuficientes)", mensaje neutro de calibración sin culpa.
11. **Copy prohibido:** "metacognición general", "toma de decisiones de la vida real", "autoconocimiento", "rasgo".
12. **KMP:** reusa renderer choice + pantalla pre-bloque (selector de chips, `verticalScroll`); `committedDeadline` alimenta el engine; `CalibracionBlock(committedDeadlineNanos,accuracyBand)`; **`calibrationError` = deuda en `MetricCalculator`; recentrado en presentación; under-commit penalty no implementado.**
13. **Tests:** determinismo; **adversarial: usuario que elige siempre el deadline más lento → metacog NO debe premiarse (penalización under-commit obligatoria)**; metacog separado del score speed (test); multi-bloque → null en sesión única; golden scoring; copy-scanner.
14. **Aceptación:** under-commit penalizado, rejilla recentrada/estrechada, metacog separado del speed score, multi-bloque enforced.
15. **Rediseño:** **separar el constructo**: si interesa la metacognición, debe ser un ejercicio metacognitivo honesto (no de Velocidad) y NUNCA contribuir a PROCESSING_SPEED. Como "ejercicio de velocidad" no aporta señal de velocidad limpia → **RETIRE del módulo Velocidad** (reubicar en metacognición si el producto lo quiere). Mientras tanto BLOCK.
16. **Impacto (dentro de velocidad):** bajo — no mide velocidad limpia.
17. **Prioridad: P3.**
18. **Riesgo residual:** alto — dos constructos mezclados, señal explotable, ruidosa; riesgo de contaminar el perfil de velocidad con metacognición.

---

## Decisión de catálogo (anti-redundancia, regla 9/10)
- **Construir limpio (P1):** E1 (perceptiva pura, eje luminancia) · E4 (freno/inhibición go-no-go con gate comisión testeado) · E6 (conflicto/interferencia flanker no-lingüístico). Tres señales ortogonales si se prueba la ortogonalidad.
- **Diferir/condicionar (P2-P3):** E2 (throughput-bajo-carga, solo si la pendiente RT/keySize separa search de speed).
- **Bloquear por infraestructura ausente (P3):** E3 (switch-cost no computado + redundante con E6) · E5 (TimingPolicy con signo inexistente) · E7 (medición de frame real + jank-gate muertos).
- **Retirar del módulo Velocidad:** E8 (metacognición, no velocidad).
- **BLOQUEADOR P0 que precede a TODO:** construir medición real de jank/frame-drop que alimente `SessionQualityEvaluator` (hoy `frameDropFraction=0.0`/`criticalFrameJitter=false` hardcoded en `SpeedSessionStateHolder.kt:259-260`) y `CADisplayLink` iOS. Sin esto, NINGÚN ejercicio de Velocidad actualiza perfil.
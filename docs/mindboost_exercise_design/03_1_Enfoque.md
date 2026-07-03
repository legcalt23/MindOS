# Enfoque

＃ Catálogo de 8 ejercicios de calidad suprema — MindBoost Enfoque

> Contrato transversal: todos cumplen las fronteras de seguridad (perfil cognitivo funcional, no diagnóstico, no IQ), dificultad paramétrica sobre `DifficultyVector`, autoridad de clasificación en el `*SessionAssembler`, persistencia `SecureKeyValueStore` + `@Serializable`+`schemaVersion`, scoring null-si-no-confident y `preliminary=true`. Marco de reutilización/extensión de renderer indicado por ejercicio.

---

## Vigía Probabilístico (Sustained Attention con tasa de evento variable)

### Módulo
Principal: Enfoque (atención sostenida / vigilancia). Secundarios: Velocidad (RT base como covariable), Decisiones (sesgo de criterio bajo incertidumbre).

### Objetivo cognitivo
Entrenar atención sostenida y detección de señal rara bajo carga temporal prolongada, midiendo decremento de vigilancia y estabilidad del criterio de respuesta (Teoría de Detección de Señal: d′ y criterio c). Ataca el sub-componente "mantener sensibilidad perceptual cuando la señal es infrecuente y el tiempo en tarea crece".

### Hipótesis de transferencia
SÍ podría transferir: near transfer a otras tareas de vigilancia/monitoreo con señal rara y a la resistencia al decremento temporal. NO se debe afirmar: mejora de "concentración general", reducción de síntomas de TDAH, ni transferencia far a productividad/estudio. Se reporta como hipótesis no garantizada.

### Loop principal
1. Estímulo: flujo de estímulos `Vigilance` (no-target frecuentes + target raro, p. ej. una orientación/longitud crítica) con onset monotónico (`MonotonicClock.nowNanos()`).
2. Decisión: tap solo cuando aparece el target (go/withhold). Una respuesta por trial.
3. Feedback: inmediato por trial (HIT verde / FALSE_ALARM ámbar / MISS gris), breve (<250 ms), sin romper el ritmo.
4. Adaptación: tras cada bloque, `DefaultAdaptiveEngine.decide` ajusta tasa de target, ISI y similitud target/lure.
5. Siguiente trial: ISI determinista del `BlockSpec` (seed) con jitter controlado.

### Reglas
Responder solo al target; retener ante no-targets. Respuesta válida dentro de `responseWindowMs`. RT < `antFloor` = ANTICIPATION (descartado de tendencia central, contado aparte). Bloques largos (3–6) para exponer decremento de vigilancia.

### Inputs
Tap único (renderer single-tap existente, `FocusStimulusField`). Sin swipe/drag/voz.

### Variables de dificultad
1. `targetRate` 0.08–0.30 (proporción de targets; más bajo = más difícil).
2. `interStimulusDelayMs` 700–2200 ms (ISI; más largo = más decremento).
3. `targetLureSimilarity` 0.0–0.85 (similitud perceptual target↔lure; más alta = menor d′).
4. `responseWindowMs` 500–1100 ms.
5. `blockLengthTrials` 40–120 (tiempo en tarea).
6. `stimulusContrast` 0.4–1.0 (saliencia perceptual).
7. `jitterMs` 0–400 ms (irregularidad temporal del onset; rompe predicción rítmica).

### Algoritmo adaptativo
`AdaptiveInput` con d′ y accuracy como gobernantes. Banda objetivo accuracy ≈ 0.78–0.85 sobre targets. Si `hitRate` alto y `falseAlarmRate` bajo → StepUp (baja `targetRate`, sube `targetLureSimilarity`). Si `falseAlarmRate` alta (impulsividad) → `ContainImpulsivity` (sube `responseWindowMs`, baja similitud). Fatiga (slope intra-bloque de RT/accuracy vía `FatigueIndex`/`FatigueSignal`) → `Cooldown`/`StepDown`. Plateau detectado sobre serie de summaries → SwitchAxis (de `targetRate` a `jitterMs`). Baseline: `targetRate` inicial anclada a `BaselineSnapshotDto`.

### Métricas primarias
`SUSTAINED_FOCUS_INDEX`, `VIGILANCE_DECREMENT_SLOPE` (pendiente accuracy/RT por sub-bloque temporal), d′ y criterio c (derivados de HIT/FALSE_ALARM por bloque).

### Métricas secundarias
`RT_MEDIAN` (HIT), `RT_CV` (`BLOCK_STABILITY`), omission vs commission separadas, `IES`, `FATIGUE_INDEX`, `POST_ERROR_SLOWING`.

### Señales de fatiga o mala calidad
Slope de RT positivo y de accuracy negativo sostenido intra-sesión; subida de omisiones en últimos sub-bloques; `RT_CV` creciente; `SubjectiveState` adverso; n efectivo de targets < umbral → `confident=false`.

### Scoring sugerido
`score = w_d*norm(dPrime) + w_stab*(1 - RT_CV) - w_dec*max(0, decrementSlope)` con `w_d=0.5, w_stab=0.3, w_dec=0.2`; `dPrime = z(hitRate) - z(falseAlarmRate)`. `value=null` si n_targets < MIN_N(20) o `confident=false`; `preliminary=true`.

### Antitrampa / calidad de datos
Detección de tap rítmico (respuestas equiespaciadas independientes del onset) → marca bloque CONTAMINATED. ANTICIPATION excesiva → invalida. Mash-tapping (FA muy alta + RT ultracorto constante) → `ContainImpulsivity` + descuento de confianza. Seed determinista permite auditar la secuencia.

### Variantes
1. Visual-orientación (target = barra con orientación crítica).
2. Auditiva-tono (target = tono raro; requiere canal audio, secundaria).
3. Numérico-paridad (target = dígito que rompe regla; reduce dependencia perceptual fina, más cultural-neutro vía símbolos).

### UX
Pantalla limpia, estímulo central grande (>48 dp), zona de tap = pantalla completa. Feedback por color semántico (`AppColors`) + microcambio de forma (no solo hue, accesibilidad daltónica). Duración ideal 4–7 min. Microanimación funcional: pulso de confirmación de tap (no decorativa).

### Implementación KMP
Reusa renderer single-tap (`FocusStimulusField`). Generador nuevo `VigiaProbGenerator: TrialGenerator` en `FocusExerciseGenerators.registry`; `StimulusPayload.Vigilance` (existe). Clasificación `TrialClassifier.classify` (tap-or-withhold). Métricas `DefaultMetricsEngine.blockMetrics` con `primaryMetrics={SUSTAINED_FOCUS_INDEX,VIGILANCE_DECREMENT_SLOPE}`. Adaptación `DefaultAdaptiveEngine`. Persistencia: `SessionSummaryDto` + `LocalSessionHistoryRepository`, namespace Focus. Modelo: `data class VigiaSummary(schemaVersion:Int, dPrime:Double?, decrementSlope:Double?, n:Int, confident:Boolean)`. Eventos a registrar: onset/tap/outcome por trial (en memoria), solo summary persistido. Estado mínimo: `FocusFieldState.Signal/Lure/Feedback`. No requiere renderer nuevo.

### Riesgos
Decremento de vigilancia exige bloques largos → riesgo de fatiga/abandono. d′ inestable con pocos targets. Posible aprendizaje de la cadencia si jitter insuficiente.

### Revisión crítica
El Revisor exige: (1) probar que d′ se computa con `n_targets` suficiente o se reporta `null` —no fabricar c con 3 targets—; (2) jitter obligatorio ≥150 ms para que no mida ritmo motor; (3) separar omission/commission o el SUSTAINED_FOCUS_INDEX confundirá inatención con impulsividad; (4) gate de fatiga ANTES de cualquier StepUp; (5) corrección de continuidad en z() cuando hitRate=1 o FA=0 para evitar d′ infinito. Sin estas correcciones, el ejercicio mide cadencia y se rompe en extremos.

### Veredicto
Aprobado con cambios (añadir jitter mínimo, corrección de continuidad en d′, gate de fatiga pre-StepUp y reporte null con n insuficiente).

---

## Filtro de Distracción (Flanker dinámico con carga perceptual)

### Módulo
Principal: Enfoque (control atencional selectivo / inhibición de interferencia). Secundarios: Velocidad, Decisiones (control de conflicto).

### Objetivo cognitivo
Entrenar atención selectiva y supresión de distractores congruentes/incongruentes (paradigma Flanker), midiendo el efecto de congruencia (interferencia) y su modulación por carga perceptual. Sub-componente: resolver conflicto estímulo-respuesta sin sacrificar velocidad base.

### Hipótesis de transferencia
SÍ: near transfer a tareas de filtrado de distractores y control de interferencia. NO: afirmar mejora de "multitarea", atención general o rendimiento académico. Far transfer = hipótesis no garantizada.

### Loop principal
1. Estímulo: target central + flancos congruentes/incongruentes (`StimulusPayload.Flanker`).
2. Decisión: LEFT/RIGHT según el target central (dos alternativas).
3. Feedback: inmediato (correcto/incorrecto + latencia).
4. Adaptación: ajusta proporción de incongruentes, número/excentricidad de flancos, deadline.
5. Siguiente trial: orden entrelazado (interleaving) seed-determinista.

### Reglas
Responder al target central, ignorar flancos. Acierto depende del LADO (`classifyChoice`), no de responder en ventana. Incongruentes mezclados (no bloqueados) para forzar selección por trial.

### Inputs
Pantalla dividida en mitades táctiles LEFT/RIGHT (renderer `FocusChoiceField`, `ChoiceParadigm.FLANKER` — existe).

### Variables de dificultad
1. `incongruentRatio` 0.25–0.65.
2. `flankerCount` 2–8 (más flancos = más carga).
3. `flankerEccentricityDeg` 0.5–3.0 (cercanía = más interferencia cuando es pequeña).
4. `responseWindowMs` 600–1200 ms (deadline adaptativo).
5. `targetContrast` 0.4–1.0.
6. `perceptualLoadDistractors` 0–6 (distractores neutros que consumen capacidad).
7. `interStimulusDelayMs` 500–1500 ms.

### Algoritmo adaptativo
Gobernante: `CONGRUENCY_EFFECT` (RT_incong − RT_cong) y accuracy. Banda accuracy ≈ 0.80. RT bajo + congruency controlado → StepUp (sube `incongruentRatio`, `flankerCount`). Commission alta en incongruentes → `ContainImpulsivity`. Fatiga → `Cooldown`. Anti-oscilación: histéresis sobre deadline. Baseline ancla `incongruentRatio` y deadline inicial.

### Métricas primarias
`CONGRUENCY_EFFECT` (solo HIT-con-RT en ambas condiciones), accuracy en incongruentes, `IES` por condición.

### Métricas secundarias
`RT_MEDIAN`, `RT_CV`, `POST_ERROR_SLOWING`/`POST_ERROR_ACCURACY`, `LISAS`, commission rate, `RATE_CORRECT_SCORE`.

### Señales de fatiga o mala calidad
Congruency effect que colapsa por caída global de accuracy (no por mejora de control); RT_CV alto; respuestas a un solo lado (sesgo motor) → mala calidad; n incongruente insuficiente → `null`.

### Scoring sugerido
`score = w_acc*accIncong + w_ctrl*(1 - norm(congruencyEffect)) + w_stab*(1 - RT_CV)` con `w_acc=0.5, w_ctrl=0.3, w_stab=0.2`. `congruencyEffect=null` si falta RT válido en un lado → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Detección de sesgo de lado (ratio LEFT/RIGHT alejado de la mezcla real). Respuestas pre-onset = ANTICIPATION. Patrón de "responder siempre congruente" detectable por accuracy condicionada. Bloque CONTAMINATED si congruency<0 con accuracy degradada.

### Variantes
1. Flechas (clásico, símbolos no-lingüísticos).
2. Formas/colores (target = forma, flancos = formas distractoras).
3. Letras-símbolo abstracto (evita dependencia de alfabeto concreto; usar glifos neutros).

### UX
Estímulo central grande, flancos a ambos lados; mitades táctiles claras. Cue de target persistente. Feedback semántico + forma. Duración 3–5 min. Microanimación: flash de mitad pulsada.

### Implementación KMP
Reusa `FocusChoiceField` (`ChoiceParadigm.FLANKER`). `FlankerLoadGenerator` ya alineado a E2; aquí es rediseño premium con `perceptualLoad`/`eccentricity`. `RecordedTrial(responseKey,...)`; `TrialClassifier.classifyChoice` (autoridad). `DefaultMetricsEngine` con `CONGRUENCY_EFFECT`. `DefaultAdaptiveEngine`. Persistencia summary en namespace Focus. Modelo: `data class FlankerSummary(schemaVersion, congruencyEffectMs:Double?, accIncong:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo.

### Riesgos
Congruency effect ruidoso con pocos trials. Sesgo motor de lado. Carga perceptual puede medir agudeza visual más que control si contraste mal calibrado.

### Revisión crítica
El Revisor exige: (1) congruency `null` si falta un lado (no rellenar con media); (2) controlar contraste para no confundir percepción con control; (3) balancear LEFT/RIGHT correctos para que el sesgo motor no infle accuracy; (4) reportar IES por condición para detectar trade-off velocidad-error; (5) evitar bloquear incongruentes (mata el interleaving). Sin balanceo de lado y null honesto, mide impulsividad lateral, no inhibición.

### Veredicto
Aprobado con cambios (balanceo de lado obligatorio, congruency null honesto, control de contraste, IES por condición).

---

## Freno Adaptativo (Stop-Signal con escalera online)

### Módulo
Principal: Enfoque (inhibición de respuesta / control inhibitorio proactivo y reactivo). Secundarios: Velocidad (go-RT), Decisiones (frenado de impulso).

### Objetivo cognitivo
Entrenar la capacidad de cancelar una respuesta ya iniciada (inhibición reactiva), cuantificada por SSRT (Stop-Signal Reaction Time, método de integración de Verbruggen). Sub-componente: latencia del proceso de freno.

### Hipótesis de transferencia
SÍ: near transfer a control inhibitorio motor y a tareas go/stop. NO: afirmar control de impulsos en decisiones de vida, gambling o autorregulación general. Far = hipótesis.

### Loop principal
1. Estímulo: trial GO (responder por defecto); en subset, señal de stop tras SSD variable.
2. Decisión: responder rápido en GO; ABORTAR si aparece stop dentro de la SSD.
3. Feedback: inmediato (GO correcto / freno exitoso / inhibición fallida).
4. Adaptación: SSD online por escalera 1-up/1-down (`StopSignalTracker`) → p(respond|signal)≈0.5.
5. Siguiente trial.

### Reglas
GO = responder en ventana. STOP = retener. Responder en stop = FALSE_ALARM (inhibición fallida, RT retenido como go-RT). p(respond|signal) debe quedar en 0.25–0.75 o el SSRT no es fiable (`trustworthy=false`).

### Inputs
Renderer `FocusFieldState.Stop` (existe): fase GO; si no responde dentro de SSD, mitad de ventana cambia a estado STOP rojo (forma + color, no solo hue). Tap único.

### Variables de dificultad
1. `stopTrialRatio` 0.20–0.33 (proporción de stops).
2. `ssdStartMs` 150–350 ms (SSD inicial; la escalera la ajusta online).
3. `ssdStepMs` 25–75 ms (paso de la escalera).
4. `goResponseWindowMs` 700–1200 ms.
5. `goStimulusSalience` 0.4–1.0.
6. `interStimulusDelayMs` 600–1600 ms.
7. `blockLengthTrials` 48–96.

### Algoritmo adaptativo
SSD intra-bloque por `StopSignalTracker` (escalera). Inter-bloque `DefaultAdaptiveEngine`: si SSRT estable y go-RT bajo → StepUp (sube `stopTrialRatio`/baja salience). Si p(respond|signal) fuera de banda → recalibrar escalera (no premiar gaming). Fatiga → `Cooldown`. Commission en GO ≠ stop. Baseline ancla `ssdStart`.

### Métricas primarias
`SSRT` (`SsrtCalculator`, integración Verbruggen), p(respond|signal), `trustworthy`.

### Métricas secundarias
go-RT mediana, go-RT_CV, go-accuracy, stop-failure rate, `POST_ERROR_SLOWING`, `FATIGUE_INDEX`.

### Señales de fatiga o mala calidad
Estrategia de ralentizar GO para "esperar" el stop (go-RT inflado intencionalmente) → invalida SSRT; `trustworthy=false` si p sale de banda; SSD que diverge (escalera rota); go-omissions altas.

### Scoring sugerido
`score = w_ssrt*(1 - norm(SSRT)) + w_go*(1 - norm(goRT)) + w_stab*(1 - goRT_CV)` con `w_ssrt=0.55, w_go=0.2, w_stab=0.25`, SOLO si `trustworthy`. Si `!trustworthy` → `value=null`, caption "calibrando freno". `preliminary=true`.

### Antitrampa / calidad de datos
Gate clave: detectar "waiting strategy" comparando go-RT en bloques con vs sin stop (inflación sistemática). p(respond|signal) fuera 0.25–0.75 = no fiable. Anticipaciones en GO descartadas. Escalera auditada por seed/log.

### Variantes
1. Stop visual (señal = cambio de color/forma del estímulo go).
2. Stop posicional (señal = aparición de marcador en zona fija).
3. Selective-stop (frenar solo ante una clase de señal, no otra — incrementa carga de selección).

### UX
GO claro; transición a STOP inequívoca y abrupta (rojo + forma + posición). Feedback de freno exitoso reforzado positivamente (no culpa). Duración 4–6 min. Microanimación: corte visual al activarse stop.

### Implementación KMP
Reusa `FocusFieldState.Stop` + `E4FrenoGenerator.tracker()` patrón (`StopSignalTracker`). `RecordedTrial(stopSignalDelayMs=...)`; `TrialClassifier.classifyStop`. `SsrtCalculator.SsrtInput(goReactionTimes, stopTrials, stopFailures, meanStopSignalDelayMs)` → `SsrtResult`. `DefaultAdaptiveEngine` inter-bloque. Persistencia: `data class StopSummary(schemaVersion, ssrtMs:Double?, pRespond:Double?, trustworthy:Boolean, n:Int)`. Estado mínimo: GO→STOP dos fases en holder. No requiere renderer nuevo.

### Riesgos
SSRT muy sensible a estrategia de espera y a escalera mal calibrada. Requiere n de stops suficiente. Daltonismo si stop depende solo de color.

### Revisión crítica
El Revisor exige: (1) detección obligatoria de waiting strategy con go-RT comparado; (2) `trustworthy=false`→null sin excepción; (3) stop señalado por ≥2 canales (color+forma+posición); (4) n_stops mínimo para SSRT o null; (5) registrar la SSD usada por trial (no recalcular post-hoc). Sin el gate de estrategia, el SSRT premiará a quien ralentiza GO — métrica destruida.

### Veredicto
Aprobado con cambios (gate anti-waiting-strategy, null estricto si no trustworthy, señal stop multicanal).

---

## Conmutador de Reglas (Task-switching con coste de cambio)

### Módulo
Principal: Enfoque (flexibilidad cognitiva / set-shifting). Secundarios: Decisiones (selección de regla), Velocidad.

### Objetivo cognitivo
Entrenar la reconfiguración del set mental al alternar entre reglas (color vs forma), midiendo `SWITCH_COST` (RT_switch − RT_repeat). Sub-componente: coste de reconfiguración y resistencia a interferencia de tarea previa.

### Hipótesis de transferencia
SÍ: near transfer a tareas de alternancia de reglas y flexibilidad de set. NO: afirmar "mejor multitasking" real (la multitarea es mito), creatividad o adaptabilidad general. Far = hipótesis.

### Loop principal
1. Estímulo: figura multidimensional (color+forma) + cue de regla activa (`StimulusPayload.TaskSwitch` con `colorIsRed`/`shapeIsCircle`).
2. Decisión: LEFT/RIGHT derivado de aplicar la regla activa al estímulo (el usuario DERIVA, no se muestra la respuesta).
3. Feedback: inmediato.
4. Adaptación: ajusta switch ratio, CSI (cue-stimulus interval), proporción de incongruentes entre reglas.
5. Siguiente trial.

### Reglas
RULE_A=color (rojo→izq), RULE_B=forma (círculo→izq). Trials switch vs repeat entrelazados. `SWITCH_COST` solo con HIT-con-RT en ambos lados (switch y repeat).

### Inputs
`FocusFieldState.RuleSwitch` + `FocusRuleSwitchField` (existe): forma coloreada + cue de regla, mitades táctiles LEFT/RIGHT.

### Variables de dificultad
1. `switchRatio` 0.25–0.60.
2. `cueStimulusIntervalMs` 100–900 ms (CSI corto = más coste).
3. `responseWindowMs` 700–1300 ms.
4. `crossRuleIncongruentRatio` 0.2–0.6 (estímulos donde ambas reglas dan respuestas distintas).
5. `ruleCount` 2–3 (añadir tercera regla, p. ej. tamaño).
6. `interStimulusDelayMs` 500–1400 ms.
7. `cueSalience` 0.4–1.0.

### Algoritmo adaptativo
Gobernante: `SWITCH_COST` + accuracy. Banda ≈0.80. Coste bajo y estable → StepUp (sube `switchRatio`, baja CSI). Errores de perseveración (aplicar regla anterior) altos → StepDown o `Cooldown`. Fatiga → `Cooldown`. SwitchAxis si `switchRatio` satura → mover a `crossRuleIncongruentRatio`. Baseline ancla CSI.

### Métricas primarias
`SWITCH_COST` (HIT-RT switch vs repeat), accuracy en switch, error de perseveración.

### Métricas secundarias
`RT_MEDIAN`, `RT_CV`, `IES` por condición, `POST_ERROR_ACCURACY`, mixing cost (bloques mono-regla vs mixtos).

### Señales de fatiga o mala calidad
Coste de cambio que sube por colapso global de accuracy; perseveración creciente; ignorar el cue (responder a una sola regla); n switch insuficiente → `null`.

### Scoring sugerido
`score = w_acc*accSwitch + w_flex*(1 - norm(switchCost)) + w_stab*(1 - RT_CV)` con `w_acc=0.5, w_flex=0.3, w_stab=0.2`. `switchCost=null` si falta RT en una condición → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Detectar "ignorar cue" (accuracy condicionada a regla = azar en una regla). Respuestas pre-cue inválidas. CSI registrado por trial. Balanceo de lado por regla.

### Variantes
1. Color/forma (clásico).
2. Paridad/magnitud numérica (dígito: par/impar vs mayor/menor que 5 — neutro cultural con símbolos).
3. Tres reglas (color/forma/tamaño) con cue rotatorio (carga de set extra).

### UX
Cue de regla persistente y visualmente distinto del estímulo (icono dedicado). Estímulo claro, mitades táctiles. Feedback semántico. Duración 4–6 min. Microanimación: cambio de cue marcado.

### Implementación KMP
Reusa `FocusRuleSwitchField`/`E6CambioGenerator` patrón. Payload deriva `correctResponse` de (rule,dimensión) — nunca expone respuesta. `TrialClassifier.classifyChoice`. `DefaultMetricsEngine` `SWITCH_COST`. `DefaultAdaptiveEngine`. Persistencia: `data class SwitchSummary(schemaVersion, switchCostMs:Double?, accSwitch:Double?, perseverationRate:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo.

### Riesgos
Switch cost ruidoso; riesgo de medir lectura del cue más que reconfiguración si cue mal diseñado. Perseveración confundible con error perceptual.

### Revisión crítica
El Revisor exige: (1) cue inequívoco y distinto del estímulo o se mide otra cosa; (2) switch cost `null` si falta un lado; (3) separar error de perseveración de error perceptual (registrar regla previa); (4) balanceo de lado por regla; (5) prohibir afirmar "multitasking". Sin separar perseveración, el coste mezcla flexibilidad con ruido.

### Veredicto
Aprobado con cambios (cue dedicado, null honesto de switch cost, registro de regla previa para aislar perseveración).

---

## Seguimiento Múltiple (MOT — Multiple Object Tracking)

### Módulo
Principal: Enfoque (atención dividida espacial sostenida). Secundarios: Memoria (mantenimiento espacial), Velocidad (procesamiento dinámico).

### Objetivo cognitivo
Entrenar el seguimiento atencional de varios objetivos en movimiento entre distractores idénticos, midiendo la capacidad de tracking (número de objetos mantenibles) y su umbral. Sub-componente: indexación atencional dinámica (FINST).

### Hipótesis de transferencia
SÍ: near transfer a tareas de tracking dinámico y atención espacial dividida (interés en deportes/conducción documentado pero NO garantizable). NO: afirmar mejora de conducción real, deporte o atención general. Far = hipótesis explícita.

### Loop principal
1. Estímulo: N objetos; un subset se marca como targets, luego todos se vuelven idénticos y se mueven (`StimulusPayload.MultiObjectTracking`).
2. Decisión: al detenerse, el sistema sondea UN objeto; el usuario indica si era target (tap o LEFT/RIGHT sí/no).
3. Feedback: inmediato.
4. Adaptación: ajusta nº targets, velocidad, densidad, duración.
5. Siguiente trial.

### Reglas
Una respuesta por trial (juicio de sonda: "¿el sondeado era target?"). La animación corre dentro del composable keyed por `layoutSeed=stimulus.index`; el holder espera UNA pulsación.

### Inputs
`FocusFieldState.Tracking` + `FocusTrackingField` (existe, `Canvas` con `animateFloatAsState`). Tap sí/no o mitades táctiles.

### Variables de dificultad
1. `targetCount` 1–5.
2. `totalObjects` 4–12.
3. `objectSpeedDpPerS` 40–220.
4. `trackingDurationMs` 2500–9000.
5. `objectRadiusDp` 10–22 (más pequeño = más difícil de indexar).
6. `proximityThresholdDp` 12–40 (cuán cerca pasan; cerca = confusión).
7. `distractorCount` derivado = totalObjects − targetCount.

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `targetCount`/`objectSpeed` → umbral ≈79% correcto. Accuracy alta → StepUp (más targets/velocidad). Caída → StepDown. Fatiga (la tarea es exigente) → `Cooldown` con prioridad. SwitchAxis entre `speed` y `targetCount`. Baseline ancla `targetCount`. Convergencia de capacidad de tracking como umbral.

### Métricas primarias
`tracking capacity` (umbral de targets al ~79%), accuracy de sonda, `DISTRACTION_RESISTANCE_INDEX` (degradación por proximidad/densidad).

### Métricas secundarias
RT de juicio de sonda, `RT_CV`, accuracy por nivel de velocidad, slope de fatiga, sensibilidad a proximidad.

### Señales de fatiga o mala calidad
Accuracy que cae a azar (50%) sostenida = sobrecarga/desenganche; respuestas constantes "sí" o "no" (sesgo); RT de sonda ultracorto (adivinar); `SubjectiveState` alto de cansancio.

### Scoring sugerido
`score = w_cap*norm(trackingCapacity) + w_acc*probeAccuracy + w_res*DISTRACTION_RESISTANCE_INDEX` con `w_cap=0.5, w_acc=0.3, w_res=0.2`. `null` si accuracy ≈ azar o n insuficiente. `preliminary=true`.

### Antitrampa / calidad de datos
Detectar sesgo de respuesta (proporción sí/no vs base real de sonda). RT de sonda < piso = adivinar. Trayectorias seed-deterministas → auditables. Bloque CONTAMINATED si accuracy = azar.

### Variantes
1. MOT clásico (puntos idénticos).
2. MOT con identidad (MOMIT: además recordar qué target era cuál — añade carga de memoria, secundaria).
3. MOT con oclusión (objetos pasan tras barras — fricción deseable de mantenimiento).

### UX
Marcado inicial claro de targets (flash), movimiento fluido, sonda destacada al final. Zona de respuesta sí/no grande. Duración 4–6 min (exigente, bloques cortos). Microanimación: la propia tarea (funcional). `cos/sin` → `.toFloat()` en `Canvas`.

### Implementación KMP
Reusa `FocusTrackingField` (E9). Aproximación: el holder espera UNA pulsación (no captura sub-trial). `TrialClassifier.classify` (sonda = target sí/no). Métricas vía `DefaultMetricsEngine` con índice de capacidad derivado del staircase. Adaptación `DefaultAdaptiveEngine`/staircase. Persistencia: `data class MotSummary(schemaVersion, trackingCapacity:Double?, probeAccuracy:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo (reusa Tracking). NOTA: tracking continuo real (multi-respuesta) requeriría reescribir el runtime `presentAndAwait` — fuera de alcance; se queda en juicio de sonda.

### Riesgos
Carga alta → fatiga/abandono rápido. Una sonda por trial da poca info → requiere muchos trials para umbral fiable. Rendimiento del `Canvas` en dispositivos lentos puede contaminar timing.

### Revisión crítica
El Revisor exige: (1) sonda balanceada target/no-target o el sesgo de respuesta infla accuracy; (2) suficientes trials por nivel para umbral fiable o `null`; (3) verificar frame-rate del `Canvas` (drops = timing falso) y marcar CONTAMINATED; (4) bloques cortos por fatiga; (5) no afirmar transferencia a conducción/deporte. Con una sonda por trial el umbral converge lento — exigir min n explícito.

### Veredicto
Aprobado con cambios (sonda balanceada, min-n para umbral, guardia de frame-rate, copy honesto sin claims de conducción).

---

## Cazador Visual (Búsqueda visual con pendiente de set-size)

### Módulo
Principal: Enfoque (atención selectiva / búsqueda visual eficiente). Secundarios: Velocidad, Decisiones (criterio de presencia/ausencia).

### Objetivo cognitivo
Entrenar búsqueda visual y medir la pendiente de RT por tamaño de conjunto (`SET_SIZE_SLOPE`), distinguiendo búsqueda eficiente (paralela, pendiente plana) de ineficiente (serial). Sub-componente: eficiencia del despliegue atencional.

### Hipótesis de transferencia
SÍ: near transfer a tareas de búsqueda y filtrado visual. NO: afirmar "mejor atención al detalle" general o productividad. Far = hipótesis.

### Loop principal
1. Estímulo: array de N items; target presente o ausente entre distractores (`StimulusPayload.Search`).
2. Decisión: target presente (tap/LEFT) vs ausente (withhold/RIGHT).
3. Feedback: inmediato.
4. Adaptación: ajusta set size, similitud target/distractor, heterogeneidad.
5. Siguiente trial.

### Reglas
Responder "presente"/"ausente". `SET_SIZE_SLOPE` calculada sobre HIT-con-RT por set size (regresión RT vs set size). Mezcla de presente/ausente balanceada.

### Inputs
`FocusFieldState.Search` + `FocusSearchField` (existe). Tap presente / o mitades táctiles presente/ausente.

### Variables de dificultad
1. `setSizeLevels` p. ej. {4,8,16,24} (rango 4–32).
2. `targetDistractorSimilarity` 0.0–0.85 (alta = serial).
3. `distractorHeterogeneity` 0.0–1.0 (heterogéneo = más difícil).
4. `targetPresentRatio` 0.4–0.6.
5. `responseWindowMs` 900–2500 ms (búsqueda lleva más tiempo).
6. `featureConjunction` bool (feature search vs conjunction search).
7. `itemSizeDp` 16–32.

### Algoritmo adaptativo
Gobernante: accuracy + `SET_SIZE_SLOPE`. Banda accuracy ≈0.85. Pendiente plana y accuracy alta → StepUp (sube similitud → conjunción). Errores de "ausente" falsos (terminar búsqueda pronto) → ajustar deadline/`ContainImpulsivity`. Fatiga → `Cooldown`. SwitchAxis entre similitud y heterogeneidad. Baseline ancla set sizes y similitud.

### Métricas primarias
`SET_SIZE_SLOPE` (ms/item, presente y ausente por separado), accuracy, intercepto de RT.

### Métricas secundarias
`RT_MEDIAN` por set size, `RT_CV`, miss rate (no encontrar presente), false-absent rate, `IES`.

### Señales de fatiga o mala calidad
Pendiente que se aplana por adivinar (accuracy cae); RT de "ausente" sistemáticamente corto (no busca); RT_CV alto; respuestas a un solo veredicto; n por set size insuficiente → `null`.

### Scoring sugerido
`score = w_acc*accuracy + w_eff*(1 - norm(setSizeSlope)) + w_stab*(1 - RT_CV)` con `w_acc=0.5, w_eff=0.3, w_stab=0.2`. `setSizeSlope=null` si <2 set sizes con RT válido o R² de regresión bajo → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Detectar "ausente sin buscar" (RT_ausente << RT esperado por set size). Balanceo presente/ausente. Regresión con R² mínimo o pendiente no fiable. Arrays seed-deterministas auditables.

### Variantes
1. Feature search (target difiere en 1 característica — pendiente plana esperada).
2. Conjunction search (target = conjunción de 2 features — serial).
3. Búsqueda de ausencia de característica (target = el que NO tiene X — fricción deseable).

### UX
Array claro, items >16 dp con suficiente separación. Veredicto presente/ausente con dos zonas táctiles grandes. Feedback semántico. Duración 4–6 min. Microanimación: resaltado breve del target tras responder (aprendizaje).

### Implementación KMP
Reusa `FocusSearchField` (E7). `SearchSlopeGenerator: TrialGenerator`. `TrialClassifier.classify` (presente/ausente) o `classifyChoice`. `DefaultMetricsEngine` con `SET_SIZE_SLOPE` (regresión sobre HIT-RT por set size). `DefaultAdaptiveEngine`. Persistencia: `data class SearchSummary(schemaVersion, setSizeSlopePresentMs:Double?, setSizeSlopeAbsentMs:Double?, accuracy:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo.

### Riesgos
Pendiente exige varios set sizes con suficientes trials → sesiones largas. Confusión con agudeza visual si items muy pequeños. "Ausente" estratégico difícil de detectar.

### Revisión crítica
El Revisor exige: (1) ≥3 set sizes con n suficiente o pendiente `null`; (2) reportar pendiente presente/ausente por separado (la ausente es ~2× la presente en serial); (3) R² mínimo para la regresión o no afirmar pendiente; (4) detección de "ausente sin buscar"; (5) tamaño de item por encima de umbral perceptual. Sin set sizes suficientes y R², la pendiente es ruido.

### Veredicto
Aprobado con cambios (≥3 set sizes con min-n, pendientes presente/ausente separadas con R², gate anti-"ausente sin buscar").

---

## Reenganche Atencional (Sostenida con recuperación post-lapso)

### Módulo
Principal: Enfoque (control atencional dinámico / recuperación tras divagación). Secundarios: Metacognición (detección de propio lapso), Velocidad.

### Objetivo cognitivo
Entrenar la detección y recuperación rápida tras un lapso atencional (mind-wandering), midiendo la latencia de reenganche y la frecuencia de lapsos. Sub-componente: meta-atención (notar que la atención se fue) y recuperación. Inspirado en gradCPT/SART pero rediseñado para medir reenganche, no solo lapsos.

### Hipótesis de transferencia
SÍ: near transfer a tareas de atención sostenida con detección de lapso. NO: afirmar reducción de mind-wandering en vida real, mindfulness clínico o concentración general. Far = hipótesis.

### Loop principal
1. Estímulo: flujo continuo gradual (transición suave entre estímulos, `StimulusPayload.Reengagement`) con go frecuente + no-go raro.
2. Decisión: responder al go habitual, retener en no-go raro; tras un error de commission (lapso), un "probe de reenganche" mide la recuperación.
3. Feedback: inmediato + probe metacognitivo ocasional ("¿estabas enganchado?").
4. Adaptación: ajusta tasa de transición, ratio no-go, frecuencia de probe.
5. Siguiente trial.

### Reglas
Go = responder; no-go raro = retener. Commission en no-go = lapso. `reengagementLatency` = nº de trials/ms hasta recuperar RT_CV/accuracy basal tras lapso. Probe metacognitivo intermitente (`SubjectiveState`).

### Inputs
`FocusFieldState.Reengagement`/Anchor (existe `FocusAnchorField`). Tap go + probe ocasional (tap binario sí/no en mitades).

### Variables de dificultad
1. `goRate` 0.80–0.92 (más go = más automatismo = más lapsos).
2. `noGoRatio` 0.08–0.20.
3. `transitionMs` 400–1200 (transición gradual; lenta = más divagación).
4. `responseWindowMs` 600–1100 ms.
5. `probeFrequency` cada 15–60 trials.
6. `blockLengthTrials` 60–140 (largo para inducir mind-wandering).
7. `stimulusMonotony` 0.0–1.0 (mayor monotonía = más lapsos).

### Algoritmo adaptativo
Gobernante: lapse rate + `reengagementLatency`. Banda: mantener commission ≈0.10–0.20 (suficiente para medir reenganche sin frustrar). Recuperación rápida y accuracy estable → StepUp (sube `goRate`/monotonía). Lapsos excesivos → StepDown. Fatiga (clave aquí) → `Cooldown`/`StopSession`. Baseline ancla `goRate`. `MicroPauseGate` anti-precipitación para impulsivos.

### Métricas primarias
`reengagementLatency` (post-error recovery), lapse rate (commission en no-go), `POST_ERROR_SLOWING`/`POST_ERROR_ACCURACY`.

### Métricas secundarias
`RT_CV` (variabilidad = proxy de divagación), `SUSTAINED_FOCUS_INDEX`, alineamiento confianza-error (probe vs accuracy real), `VIGILANCE_DECREMENT_SLOPE`.

### Señales de fatiga o mala calidad
RT_CV creciente + lapsos en racha (desenganche total, no lapso puntual); probe metacognitivo desalineado de forma extrema; go-omissions altas; `SubjectiveState` adverso → marcar y posiblemente abortar.

### Scoring sugerido
`score = w_lapse*(1 - lapseRate) + w_recov*(1 - norm(reengagementLatency)) + w_meta*confidenceErrorAlignment` con `w_lapse=0.4, w_recov=0.35, w_meta=0.25`. `null` si n_noGo o n probes insuficiente. `preliminary=true`.

### Antitrampa / calidad de datos
Diferenciar lapso (commission aislada con RT_CV elevado alrededor) de error perceptual. Detectar "responder a todo" (no inhibe nunca) vs desenganche total (omisiones en racha). Probe honesto: comparar auto-reporte vs accuracy. Bloque CONTAMINATED si desenganche sostenido.

### Variantes
1. gradCPT-style (transición gradual de imágenes; no-go = categoría rara).
2. SART-numérico (no-go = dígito raro; neutro cultural).
3. Con probe de pensamiento (thought-probe explícito intermitente — refuerza señal metacognitiva).

### UX
Flujo suave y monótono por diseño (induce divagación medible), no aburrido-frustrante. Probe breve y no intrusivo. Feedback de recuperación positivo. Duración 5–8 min (necesita longitud). Microanimación: transición gradual (funcional, la tarea).

### Implementación KMP
Reusa `FocusAnchorField`/`Reengagement`. `ReengageGenerator: TrialGenerator`. `TrialClassifier.classify` (go/no-go) + cálculo de `reengagementLatency` en `FocusSessionAssembler` (ventana post-error). `DefaultMetricsEngine` con `POST_ERROR_*`. `DefaultAdaptiveEngine` + `MicroPauseGate`. Persistencia: `data class ReengageSummary(schemaVersion, lapseRate:Double?, reengagementLatencyMs:Double?, confidenceAlignment:Double?, n:Int, confident:Boolean)`. Probe metacognitivo via `SubjectiveState`. No requiere renderer nuevo (reusa Anchor/Reengagement).

### Riesgos
Inducir mind-wandering deliberadamente raya la línea de "dificultad artificial": debe ser monotonía controlada y medida, no aburrimiento gratuito. Reengagement latency con pocos lapsos = inestable. Probe puede sesgar la atención (acto de medir altera).

### Revisión crítica
El Revisor exige: (1) demostrar que la monotonía es una `DifficultyAxis` medida (`stimulusMonotony`), no aburrimiento sin métrica — si no, es dificultad artificial prohibida; (2) `reengagementLatency` `null` si <N lapsos; (3) separar desenganche total de lapso puntual o la métrica mezcla dos cosas; (4) probe metacognitivo no debe contaminar el flujo (frecuencia baja); (5) gate de fatiga fuerte por la longitud. El mayor riesgo: medir tolerancia al aburrimiento en vez de reenganche. Exige validar que reengagementLatency correlaciona con recuperación de RT_CV, no con tedio.

### Veredicto
Aprobado con cambios (monotonía como eje medido, null estricto de reengagement con pocos lapsos, separar desenganche total, probe de baja frecuencia, gate de fatiga reforzado).

---

## Doble Vía Conflictiva (Dual-task con coste de interferencia central)

### Módulo
Principal: Enfoque (atención dividida / cuello de botella central). Secundarios: Velocidad (RT bajo carga), Memoria (mantenimiento concurrente).

### Objetivo cognitivo
Entrenar la gestión de dos tareas concurrentes y medir `DUAL_TASK_COST` (degradación al hacer dos tareas vs una sola), exponiendo el cuello de botella de procesamiento central. Sub-componente: coste de coordinación atencional. (Honestidad: NO entrena "multitarea" como habilidad; mide y reduce el coste, que es lo único defendible.)

### Hipótesis de transferencia
SÍ: near transfer a tareas dual-task con estructura similar; reducción del coste medible. NO: afirmar capacidad de multitarea general (mito); productividad. Far = hipótesis explícita. Copy debe desmentir el mito de multitarea.

### Loop principal
1. Estímulo: tarea primaria (go/no-go) + tarea secundaria concurrente (conteo/monitoreo, `StimulusPayload.DualTask`), con bloques single y dual entrelazados.
2. Decisión: responder a la primaria; la secundaria corre en paralelo (animación en composable keyed por `layoutSeed`).
3. Feedback: inmediato sobre la primaria; resultado de la secundaria al final del trial.
4. Adaptación: ajusta carga de la secundaria, SOA, ratio dual/single.
5. Siguiente trial.

### Reglas
Bloques single-task (baseline) vs dual-task. `DUAL_TASK_COST` = (RT/accuracy_dual − RT/accuracy_single) con HIT-RT en ambas condiciones. Una respuesta primaria por trial (limitación del runtime); secundaria evaluada por resultado final, no por sub-respuestas.

### Inputs
`FocusFieldState.Dual` + `FocusDualTaskField` (existe; contador concurrente). Tap primario único; secundaria reportada al cierre del bloque.

### Variables de dificultad
1. `secondaryLoad` 0–5 (carga de la tarea concurrente).
2. `dualRatio` 0.3–0.7 (proporción de trials dual).
3. `stimulusOnsetAsyncMs` 0–600 (SOA entre tareas; corto = más interferencia).
4. `primaryResponseWindowMs` 600–1200 ms.
5. `secondaryUpdateRateMs` 600–1800 (velocidad de la secundaria).
6. `interStimulusDelayMs` 500–1400 ms.
7. `secondaryModality` (visual/auditiva — distinta modalidad reduce conflicto).

### Algoritmo adaptativo
Gobernante: `DUAL_TASK_COST` + accuracy primaria. Banda accuracy primaria ≈0.80. Coste bajo y estable → StepUp (sube `secondaryLoad`/baja SOA). Si la secundaria se abandona (accuracy secundaria ≈ azar) → no premiar (gate: ambas tareas deben tener accuracy mínima). Fatiga → `Cooldown`. Baseline ancla `secondaryLoad`.

### Métricas primarias
`DUAL_TASK_COST` (RT y accuracy, ambas condiciones con datos válidos), accuracy primaria dual, accuracy secundaria.

### Métricas secundarias
`RT_MEDIAN` single vs dual, `RT_CV`, `IES`, trade-off entre tareas (prioriza una a costa de otra), `FATIGUE_INDEX`.

### Señales de fatiga o mala calidad
Abandono de la secundaria (accuracy secundaria = azar) → dual cost falso; sacrificio extremo de una tarea; RT_CV alto en dual; n single o dual insuficiente → `null`.

### Scoring sugerido
`score = w_cost*(1 - norm(dualTaskCost)) + w_bal*taskBalance + w_acc*min(accPrimary,accSecondary)` con `w_cost=0.4, w_bal=0.25, w_acc=0.35`; `taskBalance` penaliza sacrificar una tarea. `dualTaskCost=null` si falta condición válida o accuracy secundaria=azar → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Gate central: ambas tareas con accuracy ≥ umbral, si no el "coste bajo" es por abandonar la secundaria. Detectar priorización extrema (`taskBalance` bajo). Single y dual balanceados. Bloque CONTAMINATED si secundaria ignorada.

### Variantes
1. Visual+visual (go/no-go + conteo de eventos visuales).
2. Visual+auditiva (cross-modal, menor conflicto — comparación de modalidad).
3. Go/no-go + n-back ligero (secundaria de memoria de trabajo — más cuello de botella).

### UX
Dos zonas claras (primaria central, secundaria periférica/contador). Feedback primario inmediato; secundario al cierre. Duración 4–6 min. Microanimación: actualización del contador secundario (funcional). Evitar sobrecarga visual que mida agudeza.

### Implementación KMP
Reusa `FocusDualTaskField` (E8). `DualConflictGenerator`. `TrialClassifier.classify` para primaria; secundaria evaluada por el `FocusSessionAssembler` al ensamblar el bloque (resultado agregado). `DefaultMetricsEngine` con `DUAL_TASK_COST`. `DefaultAdaptiveEngine` con gate de accuracy secundaria. Persistencia: `data class DualSummary(schemaVersion, dualTaskCostMs:Double?, accPrimaryDual:Double?, accSecondary:Double?, taskBalance:Double?, n:Int, confident:Boolean)`. LIMITACIÓN: el runtime `presentAndAwait` es de una respuesta por trial; la secundaria real multi-respuesta requeriría reescribir el runtime — aquí la secundaria se evalúa por resultado final agregado, no por captura sub-trial. No requiere renderer nuevo.

### Riesgos
El runtime de una respuesta limita la fidelidad dual-task real (la secundaria no captura sub-eventos). Riesgo de medir abandono en vez de coordinación. Tentación de afirmar "multitarea". Carga visual puede confundir con percepción.

### Revisión crítica
El Revisor es el más duro aquí: (1) sin gate de accuracy secundaria, el "coste bajo" premia abandonar la secundaria — métrica destruida; obligatorio; (2) la secundaria evaluada solo al final es una aproximación débil: documentar que NO es dual-task psicofísico estricto y NO sobre-afirmar; (3) `taskBalance` obligatorio para detectar priorización; (4) copy DEBE desmentir el mito de multitarea o viola fronteras de claims; (5) dual cost `null` si falta condición. Si no se puede garantizar engagement de la secundaria, el Revisor recomienda degradar a "Aprobado con cambios" estricto, no aprobado limpio.

### Veredicto
Aprobado con cambios (gate obligatorio de accuracy secundaria, `taskBalance`, copy anti-mito-multitarea, documentar la aproximación de secundaria-agregada y null honesto de dual cost).
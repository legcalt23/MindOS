# Decisiones

> **Fronteras de seguridad (vinculantes):** MindBoost entrena un perfil cognitivo funcional; no diagnostica, no mide CI ni promete mejora clinica. Toda salida sin soporte estadistico se reporta `null` (em-dash), nunca 0, y arranca `preliminary = true`. Motores reales conectados: `BayesEngine` (`com.mindverse.mindboost.core.mdp.bayes`), `ReversibilityEngine` (`...mdp.reversib`), `ScoringEquations` (`...scoring`), `CoachEngine` (`...coach`). Persistencia: patron `SecureKeyValueStore` + DTO `@Serializable` + `schemaVersion` + namespace `Decisiones`; renderers via registry id->renderer.

---

## E1 — Bayes Arena: Calibrador de Posterior Continuo

### Modulo
Principal: Decisiones (razonamiento probabilistico / actualizacion bayesiana). Secundarios: Metacognicion (calibracion confianza-acierto), Velocidad de procesamiento controlada (RT bajo presion sin premiar impulsividad).

### Objetivo cognitivo
Entrenar la actualizacion bayesiana correcta de una creencia ante evidencia con sensibilidad y tasa de falsos positivos conocidas: el usuario debe mover su estimacion del posterior P(H|E) hacia el valor matematico, no anclarse en la base-rate ni sobre-reaccionar a la evidencia (base-rate neglect y over-updating son los dos errores diana).

### Hipotesis de transferencia
SI podria transferir (near): mejor estimacion de posteriores en tareas isomorfas de diagnostico/test-positivo con base-rates distintas; reduccion del base-rate neglect en variantes no entrenadas. NO se debe afirmar: mejora del "razonamiento" general, del CI, ni que el usuario tomara mejores decisiones medicas/financieras reales (far transfer no garantizado; solo se reporta como hipotesis).

### Loop principal
1. **Estimulo:** escenario textual minimo + 3 sliders/indicadores de base-rate, sensibilidad (TP) y falso-positivo (FP) renderizados como barras (no numeros crudos para evitar calculo memorizado), mas un panel de evidencia.
2. **Decision:** el usuario arrastra un dial continuo [0,1] hasta su estimacion del posterior P(H|E).
3. **Feedback:** se revela el posterior matematico; el dial muestra el gap (sobre/infra-estimacion) coloreado por signo; coach inmediato si BUR fuera de banda.
4. **Adaptacion:** `DifficultyAdapter.calculateNextDifficulty` ajusta numero de evidencias encadenadas, separacion TP-FP y ruido.
5. **Siguiente trial:** nuevo `BayesScenario` seeded.

### Reglas
Una respuesta = una posicion del dial confirmada (tap "Confirmar"). El dial no muestra el valor objetivo como affordance. RT se cuenta desde render completo hasta confirmacion. Trials con confirmacion <500 ms o sin mover el dial desde el default se marcan no validos (anti-default-gaming).

### Inputs
Drag continuo del dial (primario) + tap Confirmar. Opcional: doble-tap para "no se / abstencion calibrada" (registra abstencion, no penaliza accuracy pero alimenta metacognicion).

### Variables de dificultad
1. `evidenceChainLen` 1..5 (numero de `SensorEvidence` encadenadas, likelihood ratios compuestos).
2. `baseRate` 0.02..0.50 (extremos hacen el neglect mas costoso).
3. `tpFpSeparation` 0.05..0.60 (|sensitivity - falsePositiveRate|; menor = mas dificil).
4. `noiseLevel` 0.0..0.30 (jitter del BayesEngine sobre los parametros mostrados).
5. `timeLimitMs` 4000..14000 (ventana de respuesta; deadline adaptativo).
6. `dialGranularity` 100..20 pasos (menos pasos = mas tosco al final, fuerza precision gruesa->fina).
7. `representationMode` {frecuencias_naturales, probabilidades} (frecuencias = mas facil; alterna como friccion deseable).
8. `distractorPriming` 0..2 (pistas irrelevantes que invitan al anclaje).

### Algoritmo adaptativo
Banda objetivo: |BUR| en [0.85, 1.15] (BUR=1.0 perfecto) en ~75-80% de trials. Staircase 1-up/2-down sobre `tpFpSeparation` y `evidenceChainLen` con histeresis (subir cuesta mas). Gates: si `FRS` (de `ScoringEquations.calculateFRS`) > 0.20 o `AGRS` > 0.75 -> deload (timeLimit +30%, chainLen-1). Plateau: sin mejora de BUR mediano en 7 sesiones cross-day -> `InterventionPlanner`-style switch de `representationMode`. Baseline personal ancla `baseRate`/`tpFpSeparation` iniciales.

### Metricas primarias
- **BUR (Bayesian Update Ratio):** `BayesEngine.evaluateBayesianUpdate(scenario, userPosterior)` en espacio logit, coerced [0,3]. Mediana por bloque sobre trials validos.
- **Calibracion (CAG):** `ScoringEquations.calculateCAG(avgConfidence, actualAccuracy)` usando |gap| binarizado a acierto-en-banda.

### Metricas secundarias
- Brier sobre estimaciones tratadas como predicciones (`calculateBrierScore`).
- Direccion del sesgo: media firmada de (userPosterior - mathPosterior) -> over vs under-updating.
- RT mediano de HITs (estimaciones en banda) sobre `cleanRts` post-MAD.
- IES = RT_hits / accuracy_en_banda.

### Senales de fatiga o mala calidad
RT drift intra-sesion (slope RT vs indice de trial); aumento de confirmaciones <500 ms; aumento de respuestas pegadas al default; subida de `FRS`. Cualquiera marca el bloque `confident=false`.

### Scoring sugerido
`scoreBayes = w_bur*f(BUR) + w_cal*(1-CAGabs) + w_speed*g(IES)` con `f(BUR)=exp(-((ln BUR)^2)/k)`, `w_bur=0.55, w_cal=0.30, w_speed=0.15`. Barra `null` si n<MIN_N o `confident=false`. `preliminary=true` siempre.

### Antitrampa / calidad de datos
`AGRS` (`calculateAGRS(rt, spatialEntropy, isDialDefault)`) marca dial-default y RT<400 ms; entropia espacial baja del recorrido del dial (movimientos identicos repetidos) -> gaming. Trials con confirmacion sin desplazamiento se descartan del scoring pero se loguean. Seed determinista permite auditoria.

### Variantes
1. **Cadena de evidencia secuencial:** evidencias llegan una a una; el usuario reactualiza el dial tras cada una (mide actualizacion incremental, no solo final).
2. **Frecuencias naturales vs probabilidades:** mismo escenario en ambos formatos para medir el efecto de representacion intra-sujeto.
3. **Contra-evidencia tardia:** una evidencia final contradice las previas; mide si el usuario sobre-corrige o ancla.

### UX
Pantalla: panel de evidencia arriba, dial semicircular grande abajo (zona tactil >=48 dp), barras TP/FP/base-rate como glyphs no numericos. Feedback: aguja objetivo aparece con microanimacion funcional de 180 ms mostrando el gap; color semantico por signo (no solo hue). Duracion ideal: 6-9 min (24-32 trials).

### Implementacion KMP
Renderer NUEVO: `dial` continuo (no existe; reusa patron de captura de `RecordedTrial` con `responseKey`=posicion normalizada). Modelo:
```kotlin
@Serializable data class BayesTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val scenarioSeed: Long, val userPosterior: Double,
  val mathPosterior: Double, val bur: Double,
  val latencyMs: Long, val movedFromDefault: Boolean)
```
Eventos: `bayes_trial_confirmed`, `bayes_deload`, `bayes_plateau`. Estado minimo: `BayesArenaStateHolder(generators=registry[E1])` clock-free (EpochClock). Motores: `BayesEngine.generateScenario`/`evaluateBayesianUpdate` (autoridad), `ScoringEquations.calculateCAG/calculateBrierScore/calculateFRS/calculateAGRS`, `DifficultyAdapter`, `CoachEngine.render(CoachSignals(bur=..., cag=...))`. Persistencia: `SecureDecisionSessionRepository` (solo summaries). Registry: `DecisionesExerciseGenerators.registry[E1_BAYES_DIAL]`.

### Riesgos
El dial continuo puede medir destreza motora fina en vez de razonamiento (confound). El formato textual introduce dependencia de lectura. Over-updating y base-rate neglect pueden confundirse si el escenario no separa bien ambos errores.

### Revision critica
El Revisor exige: (1) **separar destreza motora de razonamiento** -> `dialGranularity` gruesa (20 pasos) en la condicion de medida primaria para que la precision motora no domine; banda de acierto >= 1 paso. (2) **El "math posterior" mostrado debe usar el MISMO BayesEngine que clasifica**, o el feedback miente. (3) BUR via logit colapsa a 1.0 cuando logitMath~0 (priori = posterior): esos trials NO miden actualizacion -> deben excluirse del scoring, no contarse como BUR perfecto. (4) Texto = riesgo cultural/idioma; exige plantillas neutras y validacion de legibilidad. Sin estas 4 correcciones, descartado.

### Veredicto
Aprobado con cambios: excelente senal bayesiana si se neutraliza el confound motor y se excluyen los trials priori=posterior del BUR.

---

## E2 — Triaje de Reversibilidad: Puerta de Una Via vs Dos Vias

### Modulo
Principal: Decisiones (clasificacion reversible/irreversible + control inhibitorio de la prisa). Secundarios: Control inhibitorio (esperar lo correcto en irreversibles), Regulacion del impulso.

### Objetivo cognitivo
Entrenar la discriminacion rapida entre decisiones reversibles (Type-2, optimizar velocidad) e irreversibles (Type-1, optimizar deliberacion): responder rapido cuando es barato deshacer y forzar revision cuando el coste de error es permanente. Diana: el sesgo de tratar todo como urgente (impulsividad) y el opuesto (paralisis en reversibles).

### Hipotesis de transferencia
SI (near): mejor calibracion velocidad/cautela en variantes isomorfas con costes de bloqueo distintos. NO afirmar: que el usuario sera mas prudente en la vida real, ni reduccion de impulsividad clinica.

### Loop principal
1. **Estimulo:** tarjeta de decision con marcadores de coste-de-deshacer (delayCost, lockedDuration) renderizados como icono/forma + intensidad.
2. **Decision:** swipe IZQ = "actua ya" (tratar como reversible) / swipe DER = "revisa antes" (tratar como irreversible). En irreversibles correctos: aparece una ventana de revision obligatoria (lock 2000..8000 ms) que el usuario debe respetar.
3. **Feedback:** correcto si `selectedReversible == scenario.isReversible`; en irreversible respetado, recompensa por esperar; en reversible, recompensa por rapidez.
4. **Adaptacion:** ajusta `irreversibility`, `delayCostRate`, ambiguedad de marcadores.
5. **Siguiente trial.**

### Reglas
En reversibles: scoring premia RT bajo + acierto. En irreversibles: responder antes del lock = error de impulsividad (penaliza fuerte aunque el lado sea correcto). El estimulo nunca dice "irreversible"; el usuario lo deriva de los marcadores de coste.

### Inputs
Swipe horizontal (primario) + en irreversibles un tap de confirmacion solo habilitado tras el lock (drag-to-confirm). Sin teclado.

### Variables de dificultad
1. `irreversibilityProb` 0.30..0.70 (mezcla de tipos; cerca de 0.5 = max incertidumbre).
2. `markerAmbiguity` 0.0..0.40 (ruido en los marcadores de coste).
3. `delayCostRate` 0.1..1.0 (penalizacion por segundo de demora en reversibles).
4. `lockDurationMs` 2000..8000 (ventana de espera en irreversibles).
5. `timePressureMs` 1200..4000 (deadline visible que tienta a impulsar).
6. `distractorUrgencyCues` 0..3 (senuelos de "urgencia" falsa que invitan a swipe rapido).
7. `interleaveRatio` (proporcion reversible/irreversible por bloque; entrelazado fuerza seleccion de regla).

### Algoritmo adaptativo
Banda: accuracy de clasificacion 0.75-0.85. 1-up/2-down sobre `markerAmbiguity`. Gate de impulsividad: tasa de respuestas-antes-del-lock alta -> `ContainImpulsivity`-style: sube `lockDurationMs` y baja `timePressure`. `FRS`>0.20 -> deload. Plateau cross-day -> switch a variante de coste-asimetrico.

### Metricas primarias
- **Accuracy de triaje** (clasificacion correcta del tipo), separando por tipo.
- **Tasa de violacion de lock** (impulsividad en irreversibles) = respuestas antes de `validateLockDuration`.

### Metricas secundarias
- RT mediano en reversibles (HITs) sobre `cleanRts`.
- `IED` (`ScoringEquations.calculateIED(icd, rt, timeLimit, isReversible)`) que penaliza demora con lambda 0.8 reversible / 0.15 irreversible.
- Penalty de demora acumulada (`ReversibilityEngine.calculateDelayPenalty`).
- Omision (no responder) vs comision (lado erroneo) separadas.

### Senales de fatiga o mala calidad
Subida de violaciones de lock + caida de accuracy con RT estable (impulsividad por fatiga); drift de RT en reversibles; `FRS` alto. Marca bloque `confident=false`.

### Scoring sugerido
`scoreTriage = w_acc*accuracyTriage + w_imp*(1 - lockViolationRate) + w_eff*meanIED_reversibles`, `w_acc=0.45, w_imp=0.35, w_eff=0.20`. Lock-violation pesa fuerte para que la rapidez nunca compense violar la cautela. `null` si n insuficiente.

### Antitrampa / calidad de datos
`AGRS` con `isDialDefault`-analogo = "siempre el mismo swipe" (entropia de respuesta baja). RT<400 ms en irreversibles = pre-respuesta. Seed reproduce la mezcla exacta de tipos. Respuestas alternando mecanicamente IZQ/DER sin correlacion con marcadores -> flag de patron.

### Variantes
1. **Coste asimetrico:** irreversibles con coste de error variable; mide si el usuario calibra cautela al coste, no solo al tipo.
2. **Reversibilidad parcial:** decisiones "reversibles con coste"; tres salidas (actua/revisa/mitiga) -> requiere renderer de 3 zonas.
3. **Presion temporal creciente:** deadline se acorta intra-bloque; mide robustez del control inhibitorio bajo prisa.

### UX
Pantalla: tarjeta central, dos mitades tactiles laterales (IZQ/DER) cubriendo toda la pantalla; marcadores de coste como glyph+forma (no solo color). Lock = anillo de progreso que se llena (microanimacion funcional, no decorativa). Feedback: verde/forma-check vs rojo/forma-stop. Duracion: 5-7 min.

### Implementacion KMP
Renderer: reusa **choice** (LEFT/RIGHT) + estado nuevo `LockState` (analogo a `FocusFieldState.Stop`). Modelo:
```kotlin
@Serializable data class ReversibilityTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val scenarioSeed: Long, val selectedReversible: Boolean,
  val isReversible: Boolean, val respondedBeforeLock: Boolean,
  val latencyMs: Long, val delayPenalty: Double)
```
Eventos: `triage_swipe`, `lock_violation`, `lock_respected`, `triage_deload`. Motores: `ReversibilityEngine.generateScenario/validateLockDuration/calculateDelayPenalty/classifyDecision` (autoridad), `ScoringEquations.calculateIED/calculateFRS/calculateAGRS`, `CoachEngine` (template impulsivo `irreversibleReview<2000ms`). Persistencia summaries. Registry `[E2_REVERSIBILITY_TRIAGE]`. Reusa `DecisionSessionOrchestrator.reversibilityRound`.

### Riesgos
El "lock obligatorio" puede sentirse como friccion artificial si no se justifica como medida. Riesgo de que el usuario aprenda el patron de marcadores (familiaridad UI) en vez de razonar coste. La banda de tipos cerca de 0.5 puede frustrar.

### Revision critica
Revisor: (1) **El lock no debe penalizar la espera honesta**: si el usuario espera y luego acierta, la espera es la conducta correcta -> el scoring debe premiar lock-respetado, nunca tratar el tiempo de espera como "lentitud" via IES. (2) Marcadores deben **rotarse/variar visualmente** entre sesiones para que mida derivacion de coste, no memorizacion de iconos. (3) `calculateDelayPenalty` solo aplica a reversibles -> en irreversibles NO usar IES de velocidad como penalizacion (contradiccion ya cubierta por lambda 0.15, pero el UI no debe mostrar "demasiado lento" en irreversibles). Corregir 1-3 antes de aprobar.

### Veredicto
Aprobado con cambios: senal de impulsividad/cautela muy limpia si el lock premia la espera correcta y los marcadores rotan.

---

## E3 — Forecast Calibrado: Apuesta tu Confianza

### Modulo
Principal: Metacognicion / Decisiones (calibracion confianza-acierto). Secundarios: Razonamiento probabilistico, Coraje decisional (no colapsar siempre al 50%).

### Objetivo cognitivo
Entrenar la calibracion: que la confianza declarada coincida con la frecuencia real de acierto (90% de confianza => acierto 90% de las veces). Diana: sobreconfianza (confianza > acierto) e infraconfianza (lo opuesto), y la cobardia metacognitiva de declarar siempre confianza media.

### Hipotesis de transferencia
SI (near): mejor calibracion en juicios binarios isomorfos; reduccion de la brecha confianza-acierto en variantes no entrenadas. NO afirmar: que el usuario sera mejor pronosticador en su dominio profesional, ni claims clinicos.

### Loop principal
1. **Estimulo:** pregunta binaria con evidencia parcial (A vs B), resoluble pero incierta.
2. **Decision:** elige A o B + fija confianza [0.5, 1.0] con un slider de "apuesta".
3. **Feedback:** se revela el resultado; scoring proper (Brier) recompensa la honestidad de la confianza, no solo el acierto.
4. **Adaptacion:** ajusta dificultad de las preguntas para mantener accuracy en banda y forzar uso del rango completo de confianza.
5. **Siguiente trial.**

### Reglas
Confianza es obligatoria y continua [0.5,1.0]. El score usa una regla de puntuacion proper (Brier): declarar alta confianza y fallar penaliza mas que declarar confianza media. Declarar siempre 0.5 minimiza varianza pero tope el score (no se puede ganar mucho sin arriesgar calibracion). Diana metacognitiva: `calculateDecisionalCourage` (sd de confianzas) debe ser > 0.

### Inputs
Tap A/B (eleccion) + drag del slider de confianza. Opcional voz: declarar confianza hablada (diferido, no V1).

### Variables de dificultad
1. `evidenceStrength` 0.55..0.95 (probabilidad real de que la opcion "mejor" sea correcta).
2. `optionSeparation` 0.05..0.40 (diferencia de evidencia entre A y B).
3. `nQuestionsPerBlock` 12..30.
4. `confidenceGranularity` (pasos del slider 5..20).
5. `timeLimitMs` 5000..12000.
6. `distractorEvidence` 0..2 (pistas que apoyan la opcion erronea).
7. `domainRotation` (rota familias de contenido para evitar conocimiento previo).

### Algoritmo adaptativo
Banda: accuracy 0.65-0.80 (suficiente incertidumbre para que la confianza importe). Si la sd de confianzas (`calculateDecisionalCourage`) ~0 durante varios bloques -> el coach empuja a usar el rango; no se sube dificultad para no premiar el colapso al 50%. `FRS`>0.20 -> deload. Plateau de Brier cross-day -> rota dominio.

### Metricas primarias
- **Brier score** (`ScoringEquations.calculateBrierScore`) — menor = mejor, proper scoring.
- **Calibration error** (`calculateCalibrationError`, 5 bins width 0.1) — desviacion confianza vs acierto por bin.

### Metricas secundarias
- **CAG** (`calculateCAG(avgConfidence, actualAccuracy)`) — direccion del sesgo (over/under).
- **Decisional courage** (`calculateDecisionalCourage`) — sd de confianzas (anti-colapso).
- Accuracy bruta (control, no es la diana).
- RT mediano de decision.

### Senales de fatiga o mala calidad
Colapso de confianza a un valor unico (sd->0); accuracy cae a azar (0.5); RT drift; respuestas <500 ms. `FRS` alto. Marca `confident=false`.

### Scoring sugerido
`scoreCalib = w_brier*(1 - Brier) + w_calerr*(1 - calErr) + w_courage*clamp(courage)`, `w_brier=0.50, w_calerr=0.35, w_courage=0.15`. Penaliza tanto sobre como infraconfianza via calErr. `null` si n<MIN bins suficientes.

### Antitrampa / calidad de datos
Detectar confianza-constante (sd~0) como degenerado: no puntua alto. Detectar siempre-misma-opcion. `AGRS` por RT<400 ms. Las respuestas deben correlacionar confianza con dificultad real del item (si confianza no covaria con evidencia -> flag). Seed reproduce items.

### Variantes
1. **Mercado de prediccion personal:** confianza como "fichas apostadas" con payoff proper-scored; mide gestion de riesgo metacognitivo.
2. **Confianza pre y post evidencia:** declarar confianza antes y despues de ver una pista; mide actualizacion metacognitiva.
3. **Calibracion bajo presion:** deadline corto; mide si la calibracion se degrada con prisa (la sobreconfianza suele subir).

### UX
Pantalla: pregunta + dos botones A/B grandes + slider de confianza con etiqueta "% seguro" (glyph, no solo numero). Feedback: barra de Brier del trial + tendencia de calibracion diferida (curva confianza-vs-acierto). Microanimacion: la ficha de confianza "cae" en el bin (funcional). Duracion: 6-8 min.

### Implementacion KMP
Renderer: **choice** (A/B) + slider de confianza (componente nuevo reutilizable con E1). Modelo:
```kotlin
@Serializable data class ForecastTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val chosen: Int, val confidence: Double,
  val correct: Boolean, val latencyMs: Long)
```
`BrierPrediction` ya existe en `Models.kt`. Eventos: `forecast_committed`, `calibration_bin_update`, `courage_low_nudge`. Motores: `ScoringEquations.calculateBrierScore/calculateCalibrationError/calculateCAG/calculateDecisionalCourage/calculateFRS`, `CoachEngine` (templates sobreconfiado CAG>+0.15, subconfiante CAG<-0.15). Persistencia summaries + serie cross-day para calibration curve. Registry `[E3_FORECAST_CALIB]`.

### Riesgos
Las preguntas binarias dependen de un banco de items resolubles, no triviales y libres de conocimiento previo (riesgo cultural/idioma fuerte). El Brier puede premiar conservadurismo si la dificultad no se calibra. Confundir acierto con calibracion.

### Revision critica
Revisor: (1) **El banco de items es el punto debil**: si un item se puede acertar por conocimiento, mide cultura, no calibracion -> exige items de evidencia-controlada (probabilidad de acierto fijada por diseno, no por saber la respuesta). (2) **Brier solo es proper si el outcome es objetivamente verificable** -> nada de preguntas de opinion. (3) Colapso al 50% no debe puntuar alto NI penalizar tanto que empuje a sobreconfianza temeraria; el peso de `courage` debe ser suave (0.15) y nunca dominar. (4) calErr con 5 bins necesita n>=~25 para ser fiable -> reportar `null` por debajo. Sin items evidence-controlled, descartado.

### Veredicto
Aprobado con cambios: la mejor senal metacognitiva del modulo si y solo si los items son de evidencia controlada (no trivia cultural).

---

## E4 — Check-in Cognitivo Go/No-Go: Estado de Disponibilidad

### Modulo
Principal: Decisiones (readiness / CRI antes de decidir). Secundarios: Control inhibitorio (Go/No-Go base), Atencion sostenida breve.

### Objetivo cognitivo
Medir y entrenar la **disponibilidad cognitiva** (readiness) antes de una sesion de decision: combina accuracy Go/No-Go, variabilidad de RT y fatiga subjetiva en un indice CRI. No es un drill de fondo: es la puerta de calibracion que protege la validez de los demas ejercicios (no decidir cansado).

### Hipotesis de transferencia
SI (near): autoconciencia de estado pre-tarea; mejor deteccion de cuando posponer una decision. NO afirmar: medicion de "estado mental" clinico, deteccion de fatiga patologica, ni diagnostico de TDAH/atencion.

### Loop principal
1. **Estimulo:** secuencia rapida de estimulos Go (responder) / No-Go (inhibir) de alta densidad.
2. **Decision:** tap en Go, inhibir en No-Go.
3. **Feedback:** minimal por trial (para no romper el flujo); al final, CRI + recomendacion (proceder / deload / posponer).
4. **Adaptacion:** corto y de dificultad casi fija (es medida, no entrenamiento adaptativo agresivo); ajusta solo densidad para mantener carga.
5. **Siguiente:** alimenta el `CheckInInput` del flujo de Decisiones.

### Reglas
Es un bloque de **calibracion**, no de entrenamiento (mode != TRAINING, nunca adapta para subir score). Duracion fija corta (~60-90 trials). El No-Go ratio fijo para comparabilidad cross-sesion. Tras el bloque, se pregunta fatiga subjetiva (0..1) como covariable.

### Inputs
Tap unico (single-tap) en Go; abstencion en No-Go. Slider de fatiga subjetiva al final (1 vez).

### Variables de dificultad
1. `noGoRatio` 0.15..0.30 (fijo por sesion para comparabilidad; valor del baseline).
2. `stimulusOnsetMs` 600..1200 (ritmo).
3. `responseWindowMs` 400..900.
4. `blockTrials` 60..90.
5. `stimulusContrast` (legibilidad; control de carga perceptiva, no cognitiva).
6. `interTrialJitter` 0..200 ms (evita anticipacion ritmica).

### Algoritmo adaptativo
Minimo por diseno: NO sube dificultad para inflar CRI. Solo deload si fatiga subjetiva extrema (>0.8) acorta el bloque. El CRI resultante alimenta el gate del resto de ejercicios (CRI bajo -> recomienda posponer drills pesados). Baseline personal define `noGoRatio` y RT esperado de referencia.

### Metricas primarias
- **CRI** (`ScoringEquations.calculateCRI(goNoGoAccuracy, reactionTimeCV, subjectiveFatigue)`, pesos 50/30/20).
- **RT-CV** (consistencia) sobre `cleanRts` de Go-HITs.

### Metricas secundarias
- Comision en No-Go (impulsividad) vs omision en Go (inatencion), separadas.
- RT mediano Go-HITs.
- Fatigue drift (slope RT vs posicion de trial).
- Concordancia fatiga subjetiva vs RT-CV objetiva (metacognicion de estado).

### Senales de fatiga o mala calidad
RT-CV alto, drift adverso, subida de comision, fatiga subjetiva alta. El propio ejercicio ES el detector de fatiga del modulo; su salida degrada `confident` de la sesion completa de decisiones.

### Scoring sugerido
`CRI` directo (0..1) como readiness. No produce "score de habilidad" (no es entrenamiento). Recomendacion: CRI>=0.7 proceder; 0.5-0.7 deload; <0.5 posponer. Siempre `preliminary` hasta tener baseline.

### Antitrampa / calidad de datos
`AGRS` (RT<400 ms = anticipacion). Responder-a-todo (sin inhibir) detectado por comision~ratio. Patron ritmico detectado por jitter-correlacion. Como es medida, el anti-trampa protege la validez del gate, no un leaderboard.

### Variantes
1. **Sustained (SART):** Go frecuente, No-Go raro (atencion sostenida; comision = lapsus atencional).
2. **Auditivo:** estimulo sonoro Go/No-Go (reduce dependencia visual; accesibilidad).
3. **Doble-CRI pre/post sesion:** mide drift de readiness atribuible a la propia sesion de decisiones.

### UX
Pantalla: estimulo central grande, zona tactil = pantalla completa (single-tap), feedback por trial minimo (flash breve). Final: tarjeta CRI con recomendacion honesta y no culpabilizante ("hoy conviene una sesion ligera"). Duracion: 2-3 min (es breve a proposito).

### Implementacion KMP
Renderer: reusa **single-tap** (`FocusStimulusField` analogo) + `classify` Go/No-Go ya existente en patron focus. Modelo:
```kotlin
@Serializable data class CheckInRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val goNoGoAccuracy: Double, val reactionTimeCV: Double,
  val subjectiveFatigue: Double, val criValue: Double, val endedAtMs: Long)
```
`CognitiveReadinessState` y `CheckInInput` ya existen. Eventos: `checkin_started`, `checkin_cri_computed`, `checkin_recommend_postpone`. Motores: `ScoringEquations.calculateCRI` (autoridad), `DecisionSessionOrchestrator.checkIn` (ya lo computa), `CoachEngine` (template fatigado FRS>0.20). Persistencia: summary CRI por sesion (serie cross-day para readiness trend). Registry `[E4_CHECKIN_GONOGO]`. Ya wired como stage `CheckIn` del flow.

### Riesgos
Riesgo de que el usuario lo perciba como "examen" y se estrese (contamina la medida). Si se gamifica el CRI, se rompe su funcion de gate honesto. RT-CV es sensible a interrupciones externas (no atribuibles a cognicion).

### Revision critica
Revisor: (1) **No gamificar el CRI**: si se muestra como puntuacion a maximizar, el usuario lo manipula y se pierde el gate -> presentarlo como semaforo de estado, no como score. (2) RT-CV contaminado por interrupciones del entorno -> necesita deteccion de outliers (pausa larga) y descarte, no penalizacion. (3) Bloque demasiado corto (60 trials) puede dar `confident=false` para RT-CV -> definir MIN_N y reportar `null` honestamente, nunca un CRI fabricado. (4) fatiga subjetiva auto-reportada es ruidosa; debe ser covariable, jamas el unico driver. Corregir antes de aprobar.

### Veredicto
Aprobado con cambios: gate de readiness imprescindible para la validez del modulo, siempre que se presente como semaforo (no score) y descarte interrupciones.

---

## E5 — Escaner de Alternativas: Anti-Fijacion en la Primera Opcion

### Modulo
Principal: Decisiones (amplitud de busqueda / alternativesScannedRatio del ICD). Secundarios: Flexibilidad cognitiva, Control del sesgo de anclaje/satisfaccion.

### Objetivo cognitivo
Entrenar la **busqueda exhaustiva de alternativas** antes de comprometer una decision: combatir el "satisficing" prematuro (elegir la primera opcion aceptable) y el anclaje en la opcion saliente. Diana: el ratio de alternativas realmente evaluadas vs disponibles, y la deteccion de la opcion dominante cuando no es la mas obvia.

### Hipotesis de transferencia
SI (near): mayor amplitud de exploracion en problemas de eleccion multiple isomorfos; menos fijacion en la primera opcion. NO afirmar: mejor toma de decisiones complejas reales, creatividad general, ni IQ.

### Loop principal
1. **Estimulo:** set de N opciones (3..8) cada una con varios atributos (dominio multidimensional); una opcion es dominante/optima pero no necesariamente la primera ni la mas saliente.
2. **Decision:** el usuario explora (debe abrir/revelar atributos) y selecciona la mejor; el sistema registra cuantas alternativas inspecciono antes de decidir.
3. **Feedback:** correcto si eligio la dominante; muestra cuantas opciones ignoro y si la ignorada era mejor.
4. **Adaptacion:** ajusta N, numero de atributos y donde se ubica la dominante.
5. **Siguiente.**

### Reglas
La opcion correcta es la dominante por atributos (objetiva, no de gusto). El acierto exige eleccion correcta; el ICD pondera `alternativesScannedRatio` (peso 0.15). Elegir rapido la primera sin explorar = baja exploracion aunque acierte por suerte. La dominante rota de posicion (anti-aprendizaje de "siempre la 2a").

### Inputs
Tap para revelar atributos de cada opcion (seleccion multiple de exploracion) + tap final de seleccion. Opcional: long-press para "descartar" una opcion (registra poda activa).

### Variables de dificultad
1. `nOptions` 3..8.
2. `nAttributes` 2..5 (dimensiones por opcion).
3. `dominanceMargin` 0.05..0.40 (cuanto gana la optima; menor = mas hay que explorar).
4. `salienceMismatch` 0..1 (la opcion mas vistosa NO es la mejor).
5. `optimalPosition` (posicion de la dominante; rota).
6. `timeLimitMs` 6000..20000.
7. `distractorDominance` 0..1 (una opcion casi-dominante que tienta a satisficing).

### Algoritmo adaptativo
Banda: accuracy 0.70-0.85. 1-up/2-down sobre `dominanceMargin` y `nOptions`. Si `alternativesScannedRatio` cronicamente bajo con accuracy alta (suerte) -> sube `salienceMismatch` para forzar exploracion. `FRS`>0.20 -> deload. Plateau -> sube `nAttributes`.

### Metricas primarias
- **Accuracy de eleccion dominante.**
- **alternativesScannedRatio** (opciones inspeccionadas / disponibles) — feed directo al `ICD`.

### Metricas secundarias
- **ICD** (`ScoringEquations.calculateICD(objectivesAlignment, bur, biasDetected, alternativesScannedRatio)`, peso scan 0.15).
- RT hasta primera seleccion vs RT total (impulsividad de cierre).
- Tasa de "primera-opcion-aceptable" (satisficing index).
- Deteccion de la dominante cuando salienceMismatch alto (resistencia al anclaje).

### Senales de fatiga o mala calidad
Caida del scan ratio con el tiempo en tarea (exploracion se acorta por fatiga); subida de satisficing; RT drift. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreScan = w_acc*accuracy + w_scan*scanRatio + w_anchor*(1 - anchorErrorRate)`, `w_acc=0.50, w_scan=0.30, w_anchor=0.20`. Penaliza acertar-por-suerte con scan bajo (limita w_acc si scanRatio muy bajo). `null` si n insuficiente.

### Antitrampa / calidad de datos
Detectar "revelar todo mecanicamente sin leer" via RT por revelacion (<150 ms = no leyo) -> scan inflado falso, se descuenta. Seleccion <400 ms tras revelar = no evaluo. Seed reproduce el set. `AGRS` por patron de exploracion identico.

### Variantes
1. **Coste de exploracion:** revelar atributos cuesta "tiempo/fichas"; mide trade-off exploracion vs explotacion (no explorar de mas sin razon).
2. **Atributos en conflicto:** ninguna opcion domina en todo; exige integracion multi-atributo (la "mejor" es por suma ponderada).
3. **Opcion oculta superior:** la dominante esta en la ultima posicion no revelada por defecto; mide resistencia a parar pronto.

### UX
Pantalla: grid/lista de opciones con atributos colapsados; tap revela; barra de "exploradas X/N". Seleccion final con confirmacion. Feedback: resalta la dominante y las ignoradas mejores. Sin animacion decorativa. Duracion: 6-9 min.

### Implementacion KMP
Renderer: NUEVO (lista revelable + seleccion; reusa patron de captura multi-tap). Modelo:
```kotlin
@Serializable data class ScanTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val nOptions: Int, val scannedCount: Int,
  val chosenIndex: Int, val optimalIndex: Int,
  val revealLatenciesMs: List<Long>, val totalLatencyMs: Long)
```
Eventos: `option_revealed`, `option_pruned`, `scan_committed`, `scan_deload`. Motores: `ScoringEquations.calculateICD` (scanRatio es input directo), `calculateFRS`, `calculateAGRS`, `DifficultyAdapter`, `CoachEngine` (template paralisis si scan excesivo + RT enorme; impulsivo si scan~0). Persistencia summaries. Registry `[E5_ALTERNATIVE_SCAN]`. Alimenta `DecisionRound.objectivesAlignment` via dominante correcta.

### Riesgos
Definir "dominante objetiva" es dificil con atributos en conflicto (puede volverse subjetivo). El scan ratio puede inflarse abriendo todo sin leer (medido pero ruidoso). Riesgo de medir paciencia/perfeccionismo en vez de calidad de busqueda.

### Revision critica
Revisor: (1) **"Dominante" debe ser matematicamente inequivoca** (dominancia de Pareto estricta o suma ponderada con pesos mostrados), o el feedback es opinable -> en variante de conflicto, mostrar los pesos para que el optimo sea verificable. (2) Scan ratio alto NO es bueno per se: explorar de mas con coste = ineficiencia -> la variante con coste de exploracion debe penalizar over-scanning, no solo under-scanning. (3) Revelar-sin-leer infla la metrica diana -> el gate de RT-por-revelacion es OBLIGATORIO, no opcional. (4) Riesgo idioma en atributos textuales -> usar atributos numericos/iconicos comparables sin lenguaje. Sin 1-4, descartado.

### Veredicto
Aprobado con cambios: ataca un sesgo real (satisficing/anclaje) con senal clara solo si la dominancia es inequivoca y el over-scanning se penaliza.

---

## E6 — Deteccion de Sesgo: Caza-Trampas de Razonamiento

### Modulo
Principal: Decisiones (biasDetected del ICD / metacognicion de sesgos). Secundarios: Razonamiento critico, Control inhibitorio de la respuesta intuitiva.

### Objetivo cognitivo
Entrenar la **deteccion del sesgo activo** en un micro-escenario de decision: identificar rapidamente cual heuristica/sesgo esta empujando hacia la respuesta intuitiva-pero-erronea (anclaje, base-rate neglect, framing, coste hundido, confirmacion) y vetar la respuesta automatica. Diana: la respuesta System-1 rapida que cae en la trampa.

### Hipotesis de transferencia
SI (near): mejor reconocimiento del mismo sesgo en variantes isomorfas; mayor latencia ante items-trampa (deliberacion). NO afirmar: que el usuario sera "menos sesgado" globalmente, debiasing duradero, ni transferencia a decisiones reales (la literatura de debiasing es debil; solo near transfer).

### Loop principal
1. **Estimulo:** micro-escenario con una respuesta intuitiva-trampa y una correcta; el sesgo esta "plantado".
2. **Decision:** elegir la respuesta correcta Y (segunda fase) etiquetar que sesgo operaba (seleccion de un set de 5 sesgos).
3. **Feedback:** correcto/incorrecto en respuesta + en etiquetado del sesgo; explica la trampa.
4. **Adaptacion:** ajusta sutileza del sesgo y atractivo de la trampa.
5. **Siguiente.**

### Reglas
Doble juicio: (a) respuesta correcta, (b) identificacion del sesgo. `biasDetected` (boolean, peso 0.20 en ICD) = etiqueto correctamente el sesgo. La respuesta-trampa es la intuitiva; resistirla exige inhibir. Items rotan sesgo para evitar "siempre es anclaje".

### Inputs
Tap (eleccion de respuesta) + seleccion de etiqueta de sesgo (choice de 5). Sin texto libre en V1.

### Variables de dificultad
1. `biasType` {anchoring, base_rate, framing, sunk_cost, confirmation} (5 familias).
2. `trapSalience` 0.3..0.9 (cuan atractiva es la respuesta-trampa).
3. `biasSubtlety` 0.0..0.6 (cuan oculto esta el sesgo).
4. `distractorBiases` 1..4 (etiquetas senuelo plausibles).
5. `timeLimitMs` 5000..12000 (presion que favorece System-1).
6. `framingIntensity` (en items de framing, fuerza del encuadre).
7. `interleaveBias` (mezcla de tipos por bloque; entrelazado fuerza identificacion).

### Algoritmo adaptativo
Banda: accuracy de respuesta 0.70-0.85. 1-up/2-down sobre `biasSubtlety`. Si accuracy de respuesta alta pero `biasDetected` bajo (acierta sin entender) -> sube peso de la fase de etiquetado / baja `trapSalience`. `FRS`>0.20 -> deload. Plateau -> rota a familias de sesgo menos entrenadas (weakness-first).

### Metricas primarias
- **Accuracy de respuesta** (resistir la trampa).
- **biasDetectionAccuracy** (etiquetado correcto del sesgo) — feed a `ICD.biasDetected`.

### Metricas secundarias
- **ICD** (`calculateICD(..., biasDetected, ...)`).
- RT en items-trampa vs items-control (latencia extra = deliberacion sana).
- Tasa de captura por la trampa por familia de sesgo (perfil de vulnerabilidad).
- Concordancia respuesta-correcta ^ sesgo-correcto (entendimiento real vs suerte).

### Senales de fatiga o mala calidad
Subida de capturas por trampa con RT bajo (vuelta a System-1 por fatiga); etiquetado al azar; RT drift. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreBias = w_resp*accuracyResp + w_detect*biasDetectAcc + w_consistency*jointCorrectRate`, `w_resp=0.40, w_detect=0.40, w_consistency=0.20`. `jointCorrect` = respuesta Y sesgo correctos (entendimiento). `null` si n por familia insuficiente.

### Antitrampa / calidad de datos
Acertar respuesta + etiquetar sesgo al azar detectado por baja `jointCorrectRate` con accuracy alta. RT<400 ms = no leyo. Seed reproduce items. Detectar memorizacion de items repetidos (excluir items vistos recientemente, como `excludeRecentWordIds` en memory).

### Variantes
1. **Solo deteccion:** dado el escenario y la respuesta, identificar el sesgo (aisla la metacognicion).
2. **Veto temporal:** el sistema "sugiere" la respuesta-trampa y el usuario debe aceptar/vetar bajo deadline (mide inhibicion del default).
3. **Sesgo compuesto:** dos sesgos operan a la vez; identificar el dominante (dificultad alta).

### UX
Pantalla: escenario breve + 2-3 botones de respuesta + panel de etiquetas de sesgo. Feedback en dos pasos (respuesta, luego sesgo) con explicacion concisa no clinica. Sin animacion decorativa. Duracion: 7-10 min.

### Implementacion KMP
Renderer: reusa **choice** (respuesta) + **choice** secundario (etiqueta). Modelo:
```kotlin
@Serializable data class BiasTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val biasType: String,
  val respondedCorrect: Boolean, val biasLabeledCorrect: Boolean,
  val latencyMs: Long, val capturedByTrap: Boolean)
```
Eventos: `bias_response`, `bias_labeled`, `trap_captured`, `bias_deload`. Motores: `ScoringEquations.calculateICD` (biasDetected input), `calculateFRS/calculateAGRS`, `DifficultyAdapter`, `CoachEngine` (impulsivo/sobreconfiado segun captura). Persistencia summaries + perfil de vulnerabilidad por familia. Registry `[E6_BIAS_HUNTER]`. Banco de items con `excludeRecent` para anti-memorizacion.

### Riesgos
Debiasing es notoriamente dificil de transferir (riesgo de overclaim). Items dependen fuertemente de lenguaje/cultura (framing, escenarios). Memorizacion de items con repeticion. Etiquetar el sesgo "correcto" puede ser discutible en items ambiguos.

### Revision critica
Revisor: (1) **Riesgo de overclaim de debiasing**: la evidencia cientifica de transferencia de debiasing es debil -> el copy debe limitarse a "reconocimiento del patron en esta tarea", jamas "te volveras menos sesgado". (2) Items textuales = bomba cultural/idioma -> requiere validacion de items y, donde sea posible, formato numerico (base-rate, anclaje). (3) El "sesgo correcto" debe ser INEQUIVOCO por diseno (un solo sesgo plantado en V1; variante compuesta diferida) o el etiquetado es injusto. (4) Banco grande + `excludeRecent` obligatorio o mide memoria, no deteccion. (5) Sin un banco validado, esto es trivia de psicologia, no entrenamiento. Corregir 1-5.

### Veredicto
Aprobado con cambios: valioso para metacognicion de sesgos solo con copy honesto (no debiasing claim), items inequivocos y anti-memorizacion estricto.

---

## E7 — Presupuesto de Decision: Asignacion bajo Restriccion

### Modulo
Principal: Decisiones (decision multi-atributo bajo restriccion / CDE complejidad). Secundarios: Memoria de trabajo (mantener restricciones), Planificacion.

### Objetivo cognitivo
Entrenar la **decision optimizadora bajo restriccion**: asignar un recurso limitado entre opciones con retornos y costes distintos para maximizar un objetivo, manteniendo varias restricciones en memoria de trabajo simultaneamente. Diana: la integracion multi-atributo correcta y el respeto de constraints sin desbordar la WM.

### Hipotesis de transferencia
SI (near): mejor optimizacion en problemas de asignacion isomorfos; mayor respeto de restricciones bajo carga. NO afirmar: mejor gestion financiera/temporal real, planificacion de vida, ni inteligencia general.

### Loop principal
1. **Estimulo:** un presupuesto B y un set de items con coste/retorno (y restricciones: max por categoria, minimos obligatorios).
2. **Decision:** el usuario asigna (drag de unidades / steppers) hasta agotar o decidir parar; debe respetar restricciones.
3. **Feedback:** retorno obtenido vs optimo (solver), violaciones de restriccion senaladas.
4. **Adaptacion:** ajusta numero de items, restricciones y holgura del presupuesto.
5. **Siguiente.**

### Reglas
El optimo lo computa un solver determinista (knapsack/LP pequeno) -> feedback verificable. Violacion de restriccion = error duro (no cuenta el retorno). Acercarse al optimo (ratio retorno/optimo) es la senal continua. La complejidad (variables, restricciones, presion temporal, carga de memoria) se cuantifica con `CDE`.

### Inputs
Drag/steppers para asignar unidades (drag primario) + tap confirmar. Reset disponible. Sin texto.

### Variables de dificultad
1. `nItems` 3..10.
2. `nConstraints` 1..4 (restricciones simultaneas a mantener en WM).
3. `budgetSlack` 0.0..0.5 (holgura; menor = mas ajustado, mas dificil).
4. `returnVariance` (dispersion de retornos; mayor = mas importa optimizar).
5. `timePressureMs` 8000..30000 -> feed a CDE.timePressure.
6. `memoryLoad` 1..7 (restricciones ocultas que hay que recordar) -> feed a CDE.memoryLoad.
7. `reversibilityOfMoves` (si reasignar tiene coste; conecta con ReversibilityEngine).
8. `distractorHighCostItems` 0..2 (items vistosos de mala relacion retorno/coste).

### Algoritmo adaptativo
Banda: ratio retorno/optimo 0.75-0.90. 1-up/2-down sobre `nConstraints` y `budgetSlack`. Gate WM: si violaciones suben con carga -> baja `memoryLoad`. `CDE` (`calculateCDE`) modula la dificultad percibida; `FRS`>0.20 o `AGRS`>0.75 -> deload (timePressure*0.6, memoryLoad-1, igual que `DifficultyAdapter`). Plateau -> sube `returnVariance`.

### Metricas primarias
- **Optimality ratio** (retorno obtenido / retorno optimo del solver).
- **Constraint violation rate** (restricciones rotas / disponibles).

### Metricas secundarias
- **CDE** (`ScoringEquations.calculateCDE(nVar, hProb, hMax, timePressure, memoryLoad, reversibility)`) como complejidad afrontada.
- RT total y numero de reasignaciones (eficiencia de busqueda).
- Decremento de optimalidad bajo `memoryLoad` alto (capacidad WM efectiva).
- Sunk-cost index: si reasignar tiene coste, ¿se aferra a malas asignaciones previas?

### Senales de fatiga o mala calidad
Subida de violaciones con carga estable; caida de optimality ratio con RT alto (esfuerzo sin resultado); reasignaciones erraticas. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreBudget = w_opt*optimalityRatio*(1 - violationRate) + w_eff*efficiency`, con `efficiency = 1/(1+reassignments/nItems)`, `w_opt=0.80, w_eff=0.20`. Violacion anula el retorno de ese trial. `null` si n insuficiente.

### Antitrampa / calidad de datos
Asignar-todo-al-primero o patron fijo detectado por baja entropia de asignacion. Confirmar <500 ms = no penso. Seed reproduce items + optimo. Detectar "fuerza bruta" (probar todas las combinaciones via reset masivo) limitando resets. `AGRS` por patron.

### Variantes
1. **Restricciones ocultas (WM):** algunas restricciones solo se muestran al inicio y desaparecen; deben mantenerse en memoria (carga WM real).
2. **Retornos estocasticos:** cada item da retorno esperado +/- ruido; mide decision bajo incertidumbre (conecta BayesEngine).
3. **Reasignacion costosa (sunk cost):** cambiar una asignacion previa cuesta; mide resistencia al coste hundido.

### UX
Pantalla: barra de presupuesto + items con steppers/drag + panel de restricciones (visible u oculto segun variante). Feedback: barra retorno-vs-optimo + restricciones violadas resaltadas (forma+color). Sin animacion decorativa; el drag es funcional. Duracion: 7-10 min.

### Implementacion KMP
Renderer: NUEVO (**drag/stepper** de asignacion; no existe; mayor coste de UI). Modelo:
```kotlin
@Serializable data class BudgetTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val allocation: List<Int>,
  val obtainedReturn: Double, val optimalReturn: Double,
  val violations: Int, val reassignments: Int, val totalLatencyMs: Long)
```
Solver determinista en commonMain (knapsack DP / LP pequeno) = autoridad del optimo. Eventos: `budget_allocate`, `budget_violation`, `budget_committed`, `budget_deload`. Motores: `ScoringEquations.calculateCDE` (complejidad), `calculateFRS/calculateAGRS`, `DifficultyAdapter` (deload), `ReversibilityEngine` (variante reasignacion costosa), `BayesEngine` (variante estocastica), `CoachEngine`. Persistencia summaries. Registry `[E7_DECISION_BUDGET]`.

### Riesgos
Renderer drag complejo = mayor riesgo de implementacion y de medir destreza UI. El solver debe ser rapido y correcto (bug -> feedback falso). nItems alto satura WM perceptiva, no solo cognitiva. Riesgo de medir paciencia/perfeccionismo.

### Revision critica
Revisor: (1) **El solver es critico**: si el "optimo" es incorrecto, todo el feedback y scoring mienten -> el solver debe estar testeado exhaustivamente (knapsack exacto para n pequeno) y la misma fuente de verdad para feedback y score. (2) Drag mide destreza motora -> usar steppers discretos en la condicion de medida primaria (drag solo en variante avanzada). (3) `CDE` describe complejidad pero NO es la metrica de habilidad -> no confundir "afronto alta complejidad" con "decidio bien"; la diana es optimality ratio. (4) Limitar resets o el usuario hace fuerza bruta (mide perseverancia, no decision). (5) WM oculta debe verificarse que no degenere en simple memoria de listas (debe integrarse en la decision). Corregir 1-5.

### Veredicto
Aprobado con cambios: ejercicio rico de decision-bajo-restriccion, pero el mas caro de implementar; solo si el solver es correcto y se usan steppers en la medida primaria.

---

## E8 — Resolucion bajo Ambiguedad: Decidir con Informacion Incompleta

### Modulo
Principal: Decisiones (decision bajo incertidumbre / valor de informacion). Secundarios: Razonamiento bayesiano (cuando recolectar mas evidencia), Tolerancia a la ambiguedad calibrada.

### Objetivo cognitivo
Entrenar el **trade-off recolectar-mas-info vs decidir-ya**: en cada trial el usuario puede pagar (tiempo/coste) por mas evidencia que reduce incertidumbre, o decidir con lo que tiene. Diana: el sobre-recolector (paralisis, nunca decide) y el sub-recolector (decide ciego). El optimo es un umbral de valor-de-informacion.

### Hipotesis de transferencia
SI (near): mejor parada de recoleccion en problemas isomorfos; aproximacion al umbral optimo de informacion. NO afirmar: mejor toma de decisiones reales bajo incertidumbre, gestion de riesgo profesional, ni rasgos de personalidad.

### Loop principal
1. **Estimulo:** una hipotesis a decidir (A/B) con prior debil; opciones para "comprar" evidencias que actualizan el posterior (cada evidencia tiene coste y poder informativo).
2. **Decision:** en cada paso, comprar otra evidencia o comprometer la decision; al comprometer, declarar A/B (+confianza).
3. **Feedback:** resultado + comparacion con la politica optima (umbral de informacion): ¿recolecto de mas/de menos?
4. **Adaptacion:** ajusta coste de evidencia, poder informativo y prior.
5. **Siguiente.**

### Reglas
Cada evidencia comprada actualiza el posterior via `BayesEngine.calculatePosterior(baseRate, evidence)`. Decidir antes del umbral optimo = sub-recoleccion; seguir comprando tras superar el umbral = sobre-recoleccion (coste desperdiciado). El optimo se deriva del valor esperado de la informacion (VOI) determinista. El estimulo no muestra el umbral.

### Inputs
Tap "comprar evidencia" (repetible) + tap "decidir" -> choice A/B + slider confianza. Sin texto.

### Variables de dificultad
1. `evidenceCost` 0.05..0.40 (coste por evidencia; mayor = parar antes optimo).
2. `evidencePower` 0.55..0.90 (likelihood ratio por evidencia).
3. `priorStrength` 0.5..0.85 (prior inicial; cercano a 0.5 = mas info necesaria).
4. `maxEvidences` 2..8.
5. `payoffAsymmetry` (coste de error A vs B distinto; cambia el umbral optimo).
6. `timeLimitMs` 8000..25000.
7. `noiseLevel` 0.0..0.25 (jitter del BayesEngine).
8. `costVisibility` {explicito, implicito} (friccion: a veces el coste acumulado no se muestra).

### Algoritmo adaptativo
Banda: distancia al numero optimo de evidencias dentro de +-1 en 75-85% de trials. 1-up/2-down sobre `evidencePower` y `priorStrength`. Gate paralisis: si el usuario compra siempre el maximo (sobre-recoleccion cronica) -> el coach interviene y se sube `evidenceCost`. `FRS`>0.20 -> deload. Plateau -> sube `payoffAsymmetry`.

### Metricas primarias
- **Information stopping error** = |evidencesComprasdas - evidenciasOptimas| (menor = mejor).
- **Decision accuracy** (A/B correcto dado el ground truth).

### Metricas secundarias
- **BUR** del posterior final declarado (`evaluateBayesianUpdate`) — ¿integro bien la evidencia comprada?
- Net payoff = valor_acierto - coste_evidencias (eficiencia economica).
- Over vs under-collection rate (perfil: paralisis vs impulsividad).
- **CAG** sobre la confianza final (calibracion).

### Senales de fatiga o mala calidad
Subida de sobre-recoleccion (paralisis por fatiga) o de sub-recoleccion (impulsividad por fatiga); RT drift; decisiones <500 ms. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreVOI = w_stop*(1 - normStoppingError) + w_acc*accuracy + w_pay*normNetPayoff`, `w_stop=0.45, w_acc=0.35, w_pay=0.20`. Penaliza tanto comprar de mas (coste) como de menos (error). `null` si n insuficiente.

### Antitrampa / calidad de datos
Detectar "siempre compra max" o "nunca compra" (politica degenerada, no adaptada al item) via correlacion compras-vs-incertidumbre: si no covaria con `priorStrength`/`evidencePower` -> flag. Decidir <400 ms = no integro. Seed reproduce evidencias + optimo. `AGRS` por patron fijo.

### Variantes
1. **Coste creciente:** cada evidencia adicional cuesta mas; mide parada optima con coste marginal creciente.
2. **Evidencia ruidosa:** algunas evidencias son poco informativas; mide seleccion de que evidencia comprar (si hay eleccion).
3. **Payoff asimetrico:** coste de falso-negativo >> falso-positivo; el umbral optimo se desplaza; mide adaptacion al payoff.

### UX
Pantalla: medidor de posterior (barra A vs B) que se actualiza al comprar evidencia + contador de coste + botones comprar/decidir. Feedback: politica optima vs la del usuario (linea de umbral). Microanimacion funcional: la barra de posterior se mueve al integrar evidencia (180 ms). Duracion: 7-10 min.

### Implementacion KMP
Renderer: reusa **choice** (A/B) + slider confianza (compartido E3) + botones de accion (comprar/decidir). Modelo:
```kotlin
@Serializable data class VoiTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val evidencesPurchased: Int, val optimalEvidences: Int,
  val finalChoice: Int, val confidence: Double, val correct: Boolean,
  val netPayoff: Double, val finalBur: Double, val totalLatencyMs: Long)
```
VOI/optimo determinista en commonMain (autoridad). `SensorEvidence` ya existe. Eventos: `evidence_purchased`, `voi_decided`, `over_collection_nudge`, `voi_deload`. Motores: `BayesEngine.calculatePosterior(baseRate, evidence)`/`evaluateBayesianUpdate` (autoridad), `ScoringEquations.calculateCAG/calculateBUR/calculateFRS/calculateAGRS`, `DifficultyAdapter`, `CoachEngine` (paralisis si over-collection + inactivity; impulsivo si sub-collection). Persistencia summaries. Registry `[E8_DECISION_UNDER_AMBIGUITY]`.

### Riesgos
El "optimo de informacion" (VOI) es no trivial de computar correctamente (depende de payoff y coste); bug -> feedback falso. Puede medir aversion al riesgo (rasgo) mas que habilidad. Sobre-recoleccion y cautela legitima son dificiles de separar.

### Revision critica
Revisor: (1) **El VOI optimo debe ser matematicamente correcto y testeado** (incluyendo payoffAsymmetry) o el "stopping error" es ruido -> derivar el umbral de un calculo VOI explicito, no de una heuristica. (2) **No confundir aversion al riesgo con error**: si el payoff es simetrico, sobre-recolectar un poco es racionalmente defendible -> la banda de "optimo +-1" debe ser tolerante y el copy no debe culpar la cautela. (3) BUR del posterior final colapsa a 1.0 si prior=posterior (pocas evidencias) -> excluir esos del BUR. (4) Politica degenerada (siempre max/nunca) debe detectarse o el ejercicio no mide adaptacion. (5) Riesgo de solapamiento conceptual con E1 (Bayes) -> diferenciar: E1 mide la actualizacion, E8 mide CUANDO parar de recolectar (control de la busqueda). Corregir 1-5.

### Veredicto
Aprobado con cambios: cierra un hueco real (valor de la informacion / parada optima) y es distinto de E1, siempre que el VOI optimo sea correcto y la cautela no se penalice como error.
# AUDITORIA EXTREMA DE MINDBOOST — INFORME PRINCIPAL

> Unidad multidisciplinar (agentes A-O) sobre el CODIGO REAL del repo. Marco 6.1-6.8, 15 ejes de puntuacion por ejercicio, formato 1-13.
> 20 agentes, ~5.1M tokens de analisis, 8 modulos, 44+ ejercicios auditados leyendo generadores, clasificadores, motores de metricas y adaptacion.

## Indice del informe

| Seccion | Contenido | Documento |
|---|---|---|
| 1 | Veredicto ejecutivo | *(este documento)* |
| 2 | Mapa de capacidades cognitivas | *(este documento)* |
| 3 | **Auditoria ejercicio por ejercicio** | [seccion 3 completa](MINDBOOST_COGNITIVE_AUDIT_EXTREME.md) · por modulo: [focus](modules/focus.md) · [speed](modules/speed.md) · [logic](modules/logic.md) · [language](modules/language.md) · [memory](modules/memory.md) · [decisiones](modules/decisiones.md) · [emotions](modules/emotions.md) · [creativity](modules/creativity.md) |
| 4 | **Modulo de Emociones** (auditoria + diseno) | [MINDBOOST_AUDIT_04_EMOCIONES.md](MINDBOOST_AUDIT_04_EMOCIONES.md) |
| 5 | Fallos estructurales | *(este documento)* |
| 6 | Mejoras estructurales | *(este documento)* |
| 7 | Motor adaptativo recomendado | [MINDBOOST_AUDIT_07_MOTOR_ADAPTATIVO.md](MINDBOOST_AUDIT_07_MOTOR_ADAPTATIVO.md) |
| 8 | Sistema de metricas definitivo | [MINDBOOST_AUDIT_08_METRICAS.md](MINDBOOST_AUDIT_08_METRICAS.md) |
| 9 | Plan de validacion cientifica | [MINDBOOST_AUDIT_09_VALIDACION.md](MINDBOOST_AUDIT_09_VALIDACION.md) |
| 10 | Ranking de ejercicios | *(este documento)* |
| 11 | Lista P0 / P1 / P2 | *(este documento)* |
| 12 | Version ideal de MindBoost | *(este documento)* |
| 13 | Resumen final brutal | *(este documento)* |
| — | Lentes de origen (Red Team, transferencia, metacognicion, etica/claims, adherencia, evaluacion inicial, completitud) | [MINDBOOST_AUDIT_ANEXOS.md](MINDBOOST_AUDIT_ANEXOS.md) |

---

He verificado en código los 5 hallazgos que el crítico de completitud marcó como no confirmados, y las 3 contradicciones entre lentes. Los resultados cambian el veredicto en dos puntos.

---


---

# 1. Veredicto ejecutivo

**Nota global del sistema: 3.4/10.** La media de los 48 ejercicios es 4.19; el sistema puntúa por debajo de sus partes porque la capa que los une —control de práctica, adaptación cableada, línea base, perfil, metacognición— está construida, testeada y **desconectada**.

**Lo que es de verdad bueno (y hay que blindar, no tocar):**

- **La ingeniería de medición temporal.** `TimebaseClassifier`, gate `CHOREOGRAPHER_FRAME + NANOS_NATIVE` en `SpeedProfileSource`, `FrameStatsCalculator`, `SsrtCalculator` (integración de Verbruggen, correcto). Nadie en esta categoría hace esto.
- **`focus2/domain/scoring/DescriptiveScorer.kt`** — d′ + criterio por condición, corrección loglineal de Hautus incondicional, SE(d′), veredicto por límite inferior del IC95, ausencia tipada, escalera de veredicto ordenada. Es el mejor artefacto psicométrico del repo y **no lo consume nadie**.
- **`core/scoring/MinSampleGate.kt`** + invariante `null ≠ 0` + `EM_DASH` en el perfil + `compositeIndex = null` estructural. Honestidad de reporte por encima de la media del sector.
- **`speed/domain/score/GoNoGo*`** (E8): `GoNoGoQualityDiagnostics`, `TimingIntegrityLevel` escalonado, `missingOnsetTaps` contado, elegibilidad por capa (`profileEligible`/`calibrationEligible`/`rankingEligible`), `GoNoGoOfficialRunAdapter` → `CognitiveSessionRun` persistido. **Es el pipeline de referencia del producto y ninguna de las 8 auditorías lo miró** (verificado: `SpeedRootScreen.kt:149-156` expone 8 tarjetas, `speed.md` audita 4).
- **`emotions/domain/safety/`** — `PauseGovernor`, `EmotionSafetyLimits`, `SAFETY_STOP`, inducción con techo. Diseño correcto de seguridad psicológica.
- **`AICoachSafetyPolicy`** — clasificación de crisis ES+EN con prioridad sobre el resto y derivación sin fingir competencia clínica.

**Lo que es teatro (verificado, no inferido):**

1. **La adaptación de Emociones no puede subir nunca.** `EmotionSessionStateHolder.kt:871-872` fija `BLOCKS = 2`, `TRIALS_PER_BLOCK = 10`; `EmotionAdaptiveEngine.kt:156` fija `COOLDOWN_AFTER_DECISIONS = 2`, y la histéresis precede a `stepUp` en el orden de reglas. Máximo 2 decisiones/sesión con `blocksSinceChange ∈ {0,1}` ⇒ **`stepUp` estructuralmente inalcanzable**, en los 6 ejercicios, siempre. Y `MIN_NOGO = 6` (`DefaultEmotionMetricsEngine.kt:228`) sobre bloques de 10 con ratio no-go 0.28–0.55 hace que la métrica gobernante de EM1/EM8 nunca alcance confianza en el bloque que gobierna la adaptación.
2. **El bug de precedencia de Lógica está VIVO.** `ReasoningScorer.kt:47-51`, leído hoy:
   ```kotlin
   val penaltyPoints = MAX_SCORE * (
       IMPULSIVITY_WEIGHT * if (impulsive) 1.0 else 0.0 +
           CONCEPTUAL_ERROR_WEIGHT * if (conceptualError) 1.0 else 0.0
       )
   ```
   Y diez líneas más arriba, `weightedBase` **sí** tiene los paréntesis (`FINAL_ANSWER_WEIGHT * (if (isCorrect) 1.0 else 0.0)`). Es decir: el aprendizaje del 2026-06-15 se aplicó a la base y **el bloque de penalización quedó sin parchear en el mismo archivo**. Con `impulsive = true`, la penalización por distractor diagnóstico desaparece; con `false`, se pondera por 0.10 en vez de 0.15.
3. **El sesgo de posición de Lenguaje está confirmado por conteo directo**: 32 `"b"`, 3 `"a"`, 1 `"c"` sobre 36 ítems (`shared/.../language/presentation/exercises/*Bank.kt`). **88.9% pulsando siempre la segunda opción, sin leer.** Es el módulo entero de razonamiento verbal.
4. **`RealImprovementCriterion` tiene cero llamadores de producción** (solo su propio archivo y su test). `ProbeReliabilityEstimator.kt:28` usa `ReliabilitySource.EMPTY` ⇒ RCI devuelve `NotEnoughData` **siempre**. El aparato que distingue mejora de práctica existe, está testeado y no corre.
5. **`focus2/` (Focus V5) — motor completo, cero call-sites de producción** (`FocusTracerScreen.kt` solo aparece en su propio archivo y en un test de a11y). Se está construyendo un motor V5 en paralelo mientras las lentes proponen reformar el V1.

**El veredicto en una frase:** MindBoost es un producto con ingeniería de medición de primera línea, honestidad de reporte ejemplar, y **ninguna capacidad demostrable de distinguir aprendizaje de familiaridad**. Hoy no puede afirmar que mejora nada, y en cinco vectores independientes puede superarse sin ejercer la capacidad que dice entrenar. Lo grave no es el estado: es que **el arreglo de la mayoría de los fallos críticos es wiring de código ya escrito**, no investigación.

---

---

# 2. Mapa de capacidades cognitivas cubiertas

| Capacidad | Ejercicios que la entrenan | Cobertura | Calidad | Huecos | Prior. |
|---|---|---|---|---|---|
| **Atención sostenida** | Focus E1 (6.2), E12 (4.2) | Media | **Buena** — decremento de vigilancia real, MAD separado de lapsos | d′ ausente: precisión confunde sensibilidad con criterio | P1 |
| **Atención selectiva / interferencia** | Focus E2 (6.0), E5 (4.8), E7 (4.6), Speed E7 (4.5), Speed E5 [no auditado] | **Alta (nominal)** | Media | 5 instancias del mismo conflicto E-R; `MIN_N_CONGRUENCY=3` vs 20/celda del gate; adaptación de conflicto (Gratton) ausente | P1 |
| **Control inhibitorio** | Focus E3 (5.5), E4 (6.8), Speed E8 [~6.5 inf.], EM1 (3.5), EM8 (3.5), check-in Decisiones (3.4) | **Alta (nominal), 6× redundante** | Media-alta en E4/Speed-E8; baja en el resto | **Sin SDT en ningún módulo**: responder más despacio se lee como mejor inhibición. `FALSE_ALARMS` es conteo absoluto, no tasa | **P0** |
| **Flexibilidad cognitiva** | Focus E6 (5.5), Speed E4 [~5.0 inf.] | Media | Media | Banco de 8 estados S-R en Focus E6; `mixing cost` ausente; 2 implementaciones sin cruzar | P1 |
| **Memoria de trabajo** | Memoria EX1 (3.0) | **Baja** | **Rota** — `WORKING_MEMORY_SPAN = trial.hits` es un conteo de reconocimientos; sin presentación serial, sin processing-task ejecutado | El hueco más caro del producto: es la capacidad con más evidencia de transferencia. `EX2_ACTUALIZACION_SET` está **diseñado y sin construir** | **P0** |
| **Memoria episódica / asociativa** | Memoria EX3 (3.5), EX4 (3.5) | Baja | Rota — `ARC_CLUSTERING` sobre orden de estudio; `DELAYED_RECALL_ACCURACY` sin demora real (`delayMs` viene del plan) | Recall por producción; demora real de 24h | **P0** |
| **Velocidad de procesamiento** | Speed E1 (4.0), E6 (4.7), E2 [~3.5 inf.], E3 (3.4) | Media | Media-baja | **Ningún RT del producto se corrige por SRT basal** pese a computarse (`SpeedSessionStateHolder.kt:312-317`); dificultad ≡ deadline en E1/E6/E7 | **P0** |
| **Razonamiento lógico** | Lógica ×6 (3.1–4.8) | Media | Baja — banco de **6 ítems fijos**, 2 inválidos (respuesta filtrada en prompt; base-rate 100%) | Generador paramétrico; `CounterexampleValidator` existe y no se cablea; bug de precedencia vivo | **P0** |
| **Razonamiento verbal** | Lenguaje ×6 (3.0–5.5) | Media (nominal) | **Rota** — 88.9% con heurística motora | Aleatorización de posición; banco >36; recuperación por producción | **P0** |
| **Razonamiento espacial** | ninguno real (Focus E9 mide memoria de localización) | **Nula** | — | `EX5_GRID_ESPACIAL` declarado y sin construir. **Ninguna lente lo declaró hueco** | P2 |
| **Creatividad** | Creatividad EX01 (3.0), EX02 (2.2) | Baja | **Muy baja** — mide fluidez por `distinct()` de strings y "elaboración" por palabras/idea | Originalidad intrapersonal y flexibilidad **no se miden en ninguna parte** | P1 |
| **Toma de decisiones** | Bayes (4.5), Reversibilidad (1.7), Resultado (3.9) | Baja | Muy baja — Reversibilidad **mide azar** y es explotable al 100% | Banco con features reales; error en logit, no lineal | **P0** |
| **Regulación emocional para rendimiento** | EM1–EM11 (3.5–4.5) | Nominal alta, **real 1/6** | Baja — 4 de 6 tienen afecto decorativo | Sin reappraisal, sin flexibilidad de regla afectiva; holdout de 12 ítems existe y **no se administra** | P1 |
| **Metacognición** | ninguno cableado | **Nula** | — | `METACOGNITION` **no existe como `CognitiveDimension`** (9 de 11 dominios canónicos). 3 impls muertas: `calculateBrierScore`, `confidenceBias=0.0` hardcodeado, `ScoringTiming.confidence` nunca rellenado. Y el score de confianza de Lógica es **regla impropia** que premia la sobreconfianza | **P0** |
| **Estrategia de aprendizaje** | `EX3_LISTA_ESTRATEGIA` (3.5), artefactual | Nula real | — | Dominio canónico sin vehículo válido | P2 |

**Cobertura honesta: 3 capacidades de 15 con mecanismo y medición defendibles** (atención sostenida, inhibición en Focus E4/Speed E8, interferencia parcial). **~50 entradas de menú ⇒ ~12 paradigmas reales.**

---

---

# 5. Fallos estructurales del sistema

Consolidado de red team, psicometría y completitud. Sin duplicar los hallazgos por ejercicio (secciones 3 y 4). **Las tres contradicciones entre lentes van resueltas aquí con evidencia de código, no con arbitraje.**

## CRÍTICO

**C1 · Ningún control del efecto práctica se EJECUTA. El mecanismo está construido, testeado y desconectado en los 5 módulos que lo tienen.**
`ProbeReliabilityEstimator.kt:28` → `ReliabilitySource.EMPTY` ⇒ RCI nunca dispara. `RealImprovementCriterion` → **0 llamadores de producción** (verificado). `MemoryProbeBank` → nunca administrado. Los 9 valores de `BankType` (`LanguageTaxonomy.kt:90-100`) están sin usar: los 36 ítems son `TRAINING` por defecto ⇒ `E2` estructuralmente inalcanzable. Los 12 ítems `isEval=true` de Emociones están reservados y ningún generador los presenta. **Sin un solo ítem no entrenado administrado, es imposible por construcción separar aprendizaje del formato de mejora de capacidad.** [Riesgo científico]

**C2 · Bancos finitos y minúsculos son la norma; la "mejora" medida es recuerdo del banco.**
Lógica **6 ítems** (`LogicContentProvider.kt:29-36`). Lenguaje **36**, orden de array fijo. Creatividad **14 prompts**. Emociones **28** de entrenamiento. Velocidad E3 **clave de 4 pares**. Focus E6 **8 estados S-R**. Los motores adaptativos consumen esa precisión inflada y **suben la dificultad por la razón equivocada**. [Riesgo científico]

**C3 · Sesgo de posición explotable en todo el módulo verbal — VERIFICADO.**
Conteo directo sobre `language/presentation/exercises/*Bank.kt`: **32 `"b"`, 3 `"a"`, 1 `"c"`** de 36. La métrica gobernante es `accuracyPercent` (`LanguageSessionStateHolder.kt:157-162`). Pulsar siempre la segunda opción → **88.9%** y `E1_TRAINED_IMPROVEMENT`, que además es el único veredicto que el módulo puede emitir (C1). El arreglo más barato y el más vergonzoso de dejar. [Mejora crítica]

**C4 · Disonancia sistemática entre lo que el runtime PRESENTA y lo que el scorer MIDE.**
Focus E8: `FocusDualTaskField.kt:41-56`, contador decorativo, el KDoc lo admite. Focus E9: `FocusTrackingField.kt:82-97`, órbitas independientes sin cruces ⇒ memoria de localización, no MOT. Focus E10: `isTarget=true` + un tap ⇒ *attentional blink* inmedible. Memoria ×3: reconocimiento multi-select sin temporización. Creatividad EX02: constructo declarado inexistente en el código. **Tres paradigmas prestigiosos están en el nombre y no en la medición.** [Riesgo científico]

**C5 · Contenido cuya respuesta no es derivable, o está filtrada en el enunciado.**
`ReversibilityEngine.kt:18` → `isReversible = random.nextDouble() >= irreversibility`, con `description = "Reversibility triage level N"`; y `lockedDurationMs > 0` solo en irreversibles, consultado antes de la elección ⇒ **azar + fuga explotable al 100%**. `LogicContentProvider.kt:153` verbaliza la conclusión antes de preguntar. El repo ya documentó la regla exacta (CLAUDE.md 2026-06-18) y aquí se viola en las dos direcciones. [Mejora crítica]

**C6 · La adaptación está muerta en 5 de 8 módulos, y en el sexto no puede subir.**
Lógica: `nextDifficulty` **sin consumidores**; `thetaEstimate` se persiste y nadie lo lee. Decisiones: `DifficultyAdapter` con 0 call-sites. Lenguaje: `DefaultAdaptationEngine` con 0 call-sites. Creatividad: sin motor. **Emociones: cableada y estructuralmente incapaz de subir** (ver R-CONTRA-1). Focus/Memory: cableadas con la mitad de las entradas muertas (`fatigue = NONE`, `perAxisMastery = emptyMap()`, `blocksSinceChange = Int.MAX_VALUE`, `confidenceBias = 0.0` hardcodeados en el call-site) ⇒ 8 reglas de seguridad inalcanzables.

**C7 · La capa viva no consume `MinSampleGate`; ocho motores con ocho tablas de n mínimos incompatibles (3,4,5,6,8) contra una autoridad que exige 20–40.**
`DefaultMetricsEngine.kt:493-499` declara `confident = true` con `MIN_N_CONGRUENCY = 3` por celda mientras `MinSampleGate` exige 20 y todas las celdas. El sistema declara confianza donde su propia norma dice que no hay medida. [Mejora crítica]

**C8 · `RT_CV` tiene dos denominadores distintos bajo el mismo nombre.**
Focus: `SD/mediana` (`DefaultMetricsEngine.kt:65-70`, comentado explícitamente). Velocidad: `SD/media` (`MetricEdgeCasePolicy.kt:53-56`). Cualquier comparación entre módulos, cualquier compuesto que los mezcle y cualquier serie longitudinal cruzada es inválida por construcción. [Mejora crítica]

**C9 · Cuatro ejercicios VIVOS de Velocidad sin auditar — incluidos los dos mejor instrumentados del producto.**
Verificado: `SpeedRootScreen.kt:149-156` expone E1,E2,E4,E5,E6,E3,E7,E8; `speed.md` audita E1,E3,E6,E7. Lo omitido incluye `GoNoGoQualityDiagnostics` + `GoNoGoOfficialRunAdapter` + `TimingIntegrityLevel` escalonado (E8), `VisualScanQualityDiagnostics` (E5) y `RuleSwitchAnalyzer` (E4). **Contamina cinco lentes**: la afirmación "Velocidad: dificultad ≡ deadline, ese es el único eje" es falsa para E8 y E5; la propuesta de "degradar Velocidad a baseline SRT" se hizo sin saber que contiene el pipeline de referencia; y `docs/mindboost_exercise_design/EXERCISE_QUALITY_CONTRACT.md` (L1–L4, escrito a partir de E8) ya define el gate que la sección 9 reinventa. [Mejora crítica]

## ALTO

**A1 · La dificultad se sube apretando el reloj, no la carga.** Velocidad E1/E6/E7 (`MultiSignalStaircaseEngine` mueve solo el deadline), Focus E2/E3/E5/E6 (eje `PRESSURE` recorta ventana), EM10 (`PRESSURE_FACTOR=0.65`, suelo 900 ms sobre clasificación conceptual de frases). Tolerar un deadline más corto en tarea sobreaprendida no es más capacidad. **Excepción a registrar: Speed E8 y E5 sí mueven carga** (C9).

**A2 · Ningún RT del producto está corregido por latencia sensoriomotora basal.** `simpleReactionMedianMs` se computa (`SpeedSessionStateHolder.kt:312-317`) y **no se resta en ningún sitio**. Un teléfono nuevo produce "mejora cognitiva". Es la corrección más barata del repo. [Dato necesario] latencia de pantalla/táctil por dispositivo.

**A3 · Ausencia de teoría de detección de señal donde hay hits y falsas alarmas.** Focus E3 (`falseAlarms >= 2.0`, conteo absoluto sin escalar por longitud de bloque), EM1/EM8 (`COMMISSION_RATE` cruda), check-in Decisiones (funde omisión y comisión), Lógica (base-rate 100% "consistente"). **Responder más lento es la estrategia dominante y el producto la premia.**

**A4 · Potencia estadística estructuralmente insuficiente.** Emociones 20 trials/sesión con `MIN_NOGO=6` (verificado) ⇒ métrica gobernante nace no-confiable. Focus `MIN_N_POST_ERROR=3`, 1 no-go garantizado. Velocidad 12 trials/bloque alimentando `cv` con MAD (≥4 tras filtrar). Lógica 6 ítems. Y los motores adaptativos **consumen ese ruido** para mover la dificultad.

**A5 · Redundancia: ~50 entradas, ~12 paradigmas — y empeora al contar Velocidad completa.** go/no-go **×6** (Focus E3, Focus E8, **Speed E8**, EM1, EM8, check-in). Conflicto E-R ×5 (Focus E2/E5/E13, Speed E7, **Speed E5**). Task-switching **×3** (Focus E6, **Speed E4**, + el N3 propuesto). Post-error ×3. Reconocimiento multi-select ×3. La cobertura aparente es ~4× la real.

**A6 · Índices compuestos inventados, con puntos regalados y gameables.** `SUSTAINED_FOCUS_INDEX = 100·(1−rtCv)·(1−lapseRate)`. `CRI` con **20% de fatiga autorreportada** (tocar "Descansado" sube ~18 puntos sin cambio real). `ICD` con `biasDetected=false` y `alternativesScannedRatio=1.0` fijos ⇒ **35 de 100 puntos de regalo constante**. "Integridad" del polígono clavada en ~100. `LISAS` que no es LISAS. `total` de Velocidad calculado con **una barra** y presentado como tres, con acantilado discontinuo en pc=0.80.

**A7 · Bug de precedencia en la penalización de Lógica — VIVO y con reincidencia parcial documentada.** Verificado hoy: `weightedBase` tiene los paréntesis, `penaltyPoints` no. El aprendizaje del 2026-06-15 se aplicó al bloque de arriba y **dejó el de abajo, diez líneas después, en el mismo archivo**. [Mejora crítica]

**A8 · Agujero de sobre-selección: marcar todo puntúa perfecto.** `ClosedScoringEngine.kt:77-78`: solo recall, cero precisión ⇒ seleccionar todos los spans da `captured = 1.0` y `evidenceComplete = true`. Evidence Lock existe para castigar la sobre-inferencia y su scorer la premia. Mismo patrón en Memoria (salvo EX4, que sí tiene gate de discriminación).

**A9 · "Creatividad / Inteligencia" no mide creatividad ni contiene inteligencia.** `CreativityRubricScorer.kt:21-37` computa `fluency = ideas.distinct()` (igualdad exacta: "arma"/"Arma" cuentan aparte) y `elaboration` = palabras/idea. Originalidad y flexibilidad **no se miden**. Las 3 etiquetas de feedback son todas positivas: no existe etiqueta de estancamiento.

**A10 · Etiquetas que mienten sobre lo que se midió.** `DELAYED_RECALL_ACCURACY` sin demora (la clave se elige por `trial.delayMs > 0`, valor del plan). `WORKING_MEMORY_SPAN` = conteo de reconocimientos con comentario `// partial-credit unit score` que contradice al código. `ARC_CLUSTERING` sobre orden de estudio. `ASSOCIATIVE_INDEX` sin cue ⇒ `CONJUNCTION_ERROR ≈ 0` estructural, reportado como "resistencia perfecta". `GRANULARITY_INDEX` = cobertura. `EMOTION_THOUGHT_SEPARATION` sobre 2 de 4 categorías con `p_azar` de 4. **Seis constructos nombrados con literatura y medidos con otra cosa.** [Riesgo científico]

**A11 · El gate de copy no cubre las tres superficies que más claim emiten.** `mindboostCopyFeatureModules` (`shared/build.gradle.kts:446-447`) excluye `presentation/` (hub), `profile/presentation/` y `coach/`. Cuatro literales con lexema prohibido pasan hoy en verde. Y emotions corre un **lexicón distinto y más débil**, sin ningún término de transferencia lejana — justo el módulo donde más tienta prometerla.

**A12 · "Progreso frente a tu propia línea base" — no hay línea base.** `BrainProfileUiModelBuilder.kt:33`. Lo que existe es una pendiente móvil sobre las últimas N sesiones (Focus/Logic/Language/Speed) y `ProfileTrend.UNKNOWN` constante en Emociones. La copy promete un diseño pre/post que el código no implementa, sobre una pendiente que subirá por práctica (C1+C2). [Riesgo científico]

**A13 · La única detección de crisis vive en el módulo que no se envía.** `AICoachSafetyPolicy` (clasificación ES+EN, filtro de salida) está en `coach/`, no montado. Mientras tanto **sí se envían** dos superficies de texto libre sin clasificar: el diario reflexivo de Decisiones y las ideas de Creatividad. No pido moderación: pido extraer `classifyRequest` + `CRISIS_RESPONSE` a `core/safety/` (~40 líneas ya escritas y testeadas) y llamarlo en toda entrada de texto libre, **en dispositivo, sin persistir el match**.

**A14 · Inducción de presión y de fallo sin ningún gating de seguridad fuera de Emociones.** `OverloadDetector`/`PauseGovernor`/`SAFETY_STOP` existen **exclusivamente** en `emotions/domain/safety/`. Y Focus envía E13 ("bajo deadline **con coste por error**"), E12 ("continúa tras un error sin entrar en espiral" — requiere que falles) y Speed E8 (todo-o-nada), sin overload detection, junto a un indicador de **RACHA** (`FocusHomeScreen.kt:251`). El equipo sabe construir seguridad psicológica; no la ha extendido al módulo que más presión induce.

## MEDIO

**M1 · La honestidad se ha convertido en tapón permanente que oculta que la medición está rota.** `compositeIndex = null`, `ADR_SCI_006_APPROVED = false`, RCI que nunca dispara, `trend = UNKNOWN` constante. El diseño de reporte es genuinamente honesto y hay que preservarlo — pero **desde fuera, un scorer roto y uno honestamente incalculable son indistinguibles**. El `CALIBRATING` universal camufla que debajo hay métricas que miden reconocimiento, azar o ruido. [Riesgo científico]

**M2 · Fatiga sin retorno de medición.** E10 lanza un stream a 80 ms/ítem para capturar un tap; E9 corre trials de 1800 ms cuyo constructo no se mide; E8 parpadea un contador que nadie puntúa; EM2/EM11 exigen comparación de barras cerca del umbral. El `FatigueGate` de Lenguaje existe y **nunca se invoca**.

**M3 · Confounds de estado no controlados.** `SubjectiveState` (sueño/cafeína/ánimo/hora) se captura, se persiste y se sincroniza —marcado *health-adjacent* por el propio código— y **`AdaptiveInput` no tiene campo para ella**. Recolección de dato sensible sin propósito. El único autoinforme que sí se usa (fatiga en Decisiones) entra **directamente al score** con peso 20% en vez de como covariable.

**M4 · Duplicidad de fuentes de verdad en el runtime.** `LanguageSessionReducer` (con repair-retry) existe y no se usa; el holder reimplementa una versión peor. `CounterexampleValidator` definido, testeado y no cableado. `calculatePosterior` multi-fuente muerto. `calculateBrierScore`/`calculateCAG`/`calculateCalibrationError` implementados y desconectados; la certeza de Bayes se recoge y se tira. **6 de las 15 plantillas del `CoachEngine` son inalcanzables** porque el único call-site rellena 6 de 22 campos.

**M5 · Corpus de auditoría previo no reconciliado y cero trazabilidad en el ledger.** `docs/mindboost_exercise_design/` contiene `04_1..04_8_*_audit.md`, `MASTER_AUDIT_V2.md`, `AUDIT_SYNTHESIS.md`, `EXERCISE_QUALITY_CONTRACT.md`, `INFRA_P0.md`. Ninguna auditoría nueva los cita ni declara qué supersede, y ninguna registra un ID de `MINDBOOST_AUDIT_LEDGER.md`. Riesgo directo: hallazgos duplicados con IDs nuevos y hallazgos previos que desaparecen sin nota de supersesión.

**M6 · Módulos y dominios sin dueño de auditoría.** `coach/` (única detección de crisis, se vende en copy, no montado) y `profile/` (emite el claim más fuerte) no tienen lente propietaria. `metacognition` no existe como `CognitiveDimension` (9 de 11 dominios canónicos). `spatial_reasoning` y `learning_strategy` sin vehículo válido y **ninguna lente los declaró hueco**. `features/arena/` emite señal cognitiva y nadie la audita.

**M7 · 13 de 16 ejercicios de Memoria diseñados y no mencionados** — incluido `EX16_CALIBRACION_CONFIANZA`, que tres lentes independientes reinventaron sin saber que el catálogo ya lo declara. Ídem `EX2_ACTUALIZACION_SET` (el "hueco de MT" que una lente propone diseñar de cero) y `EX14_DUAL_TASK`.

## Resolución de las 3 contradicciones entre lentes

**R-CONTRA-1 · `DefaultEmotionAdaptiveEngine`: ¿espina del kernel o motor muerto? → GANA §4, con matiz.**
Verificado en código: `BLOCKS = 2` + `TRIALS_PER_BLOCK = 10` (`EmotionSessionStateHolder.kt:871-872`) contra `COOLDOWN_AFTER_DECISIONS = 2` (`EmotionAdaptiveEngine.kt:156`) ⇒ **`stepUp` es inalcanzable, demostrado por aritmética**. Y `MIN_NOGO = 6` sobre bloques de 10 con ratio ≤0.55 ⇒ la métrica gobernante de EM1/EM8 nunca alcanza confianza.
**Decisión:** se promueve el **diseño** (lista de reglas ordenada con la seguridad primero, y `engagementSufficientForStepUp`, que es la única defensa escrita contra dificultad-como-velocidad), **no el motor tal cual**. Precondición no negociable: la Ola 0 de reparación de Emociones (S1–S5) va **antes** de generalizar nada. Dueño: el módulo Emociones; el kernel unificado no arranca hasta que la Ola 0 esté verde.

**R-CONTRA-2 · RCI / `ReliabilitySource`: ¿local-first o estudio? → AMBOS TIENEN RAZÓN SOBRE COSAS DISTINTAS.**
Verificado: `ProbeReliabilityEstimator.kt:28` usa `ReliabilitySource.EMPTY` ⇒ RCI fail-closed hoy. Las dos lentes hablan de dos estimadores distintos con el mismo nombre:
- **RCI clásico (Jacobson-Truax)** necesita `SD_pre` y `r_xx` **poblacionales** ⇒ exige estudio agregado. La sección 9 tiene razón: es cuello de botella para cualquier claim normativo o comparativo.
- **RCI N-of-1 con SEM intra-individual** no necesita población: `SEM_within` = SD de k≥3 administraciones en fase de estabilización sin entrenamiento intercalado. La sección 8 tiene razón: alimenta el mismo seam sin cambiar la firma.
**Decisión:** se construye el **N-of-1** ahora (local-first, desbloquea la Evaluación Inicial y la detección de meseta), y se declara por escrito su techo: *"cambio distinguible de tu propia variabilidad"*, **nunca** "cambio fiable" en sentido normativo ni percentil. El RCI poblacional queda `NotEnoughData` hasta el estudio. Requisito añadido: `RciResult.Value` debe transportar el IC que `MinSampleGate.requiresConfidenceInterval` ya exige y hoy no puede dar; y reconciliar `MinSampleGate.RCI.nMinPerCell = 2` con `ProbeReliabilityEstimator.MIN_ADMINISTRATIONS = 3` **a 3** (el conservador), o el gate central deja de ser autoridad.

**R-CONTRA-3 · Velocidad: ¿track, instrumento, o prohibido mezclar? → LAS TRES POSTURAS SE TOMARON SOBRE UN MÓDULO INCOMPLETO (C9). REGLA POR EJERCICIO.**
- **E1 Destello → instrumento de calibración obligatorio.** No es entrenamiento (nota 4.0, `pc` trivialmente 1.0). Se administra al inicio de toda sesión que vaya a producir RT para el perfil, y su mediana **se resta** (A2). Esto obliga a añadir Velocidad a `DEFAULT_PLANNABLE` como bloque de instrumento, no como track.
- **E8 Inhibición y E5 Búsqueda → entrenamiento de primera línea con contrato de calidad**, y son el patrón que el resto del producto debe copiar (`TimingIntegrityLevel` escalonado, elegibilidad por capa, `CognitiveSessionRun` persistido).
- **E4 Cambio de Regla → fusionar con Focus E6** (dos implementaciones de task-switching, una con analizador dedicado).
- **E2, E3, E6, E7 → track de entrenamiento con la corrección de A1** (mover carga, no solo deadline).
- La prohibición "no mezclar Enfoque ↔ Velocidad" **se mantiene para E6/E7** (mismo constructo, arrastre motor) y **se levanta para E1** (es el instrumento que hace comparables los RT de todos los demás) y para E8/E5 (constructos distintos, contrato de calidad propio). Todo bloque RT-crítico en posición ≠1 debe registrar `positionInSession` y el scoring modelarlo o excluirlo.

---

---

# 6. Mejoras estructurales del sistema

Consolidado de transferencia, metacognición y adherencia. **El giro arquitectónico que precede a todo:** MindBoost debe dejar de ser una plataforma de "entrenamiento de capacidad" y convertirse en un **motor de estrategia + calibración + retención**, con la práctica de tarea como sustrato. Razón funcional: el volumen de práctica sobre tareas de laboratorio produce mejora en la tarea y transferencia cercana estrecha; lo que sí transfiere con evidencia razonable es espaciado, recuperación activa y calibración metacognitiva — y **ninguno de los tres tiene hoy dueño arquitectónico**. Todas las mejoras viven **por encima** de los módulos (`core/` o un `curriculum/` nuevo); implementadas dentro de cada módulo se replicarán ocho veces y divergirán ocho veces, exactamente como ya pasó con la adaptación.

## Arquitectura: ocho piezas transversales

**S1 · Capa de sesión (`core/curriculum/MindBoostSession`).** Hoy el hub es un `when` sobre 9 destinos exclusivos que lanza sin payload, y el runner ejecuta **un** bloque (`FocusSessionStateHolder.kt:394`, `blocks = listOf(block)`) de hasta 160 trials del mismo paradigma. `DefaultDailyPlanGenerator` produce 1–4 bloques y **`FocusHomeStateHolder.kt:70` lee solo el primero**: los bloques 2–4 se calculan y se tiran. Cada módulo expone `runBlock(spec): BlockOutcome` y conserva su runtime; el contenedor compone. Mecanismo: interferencia contextual (evidencia fuerte en aprendizaje motor/categorías, **moderada** en tareas cognitivas — se adopta por su efecto sobre retención y flexibilidad, no como promesa de transferencia).

**S2 · Motor de espaciado transversal.** Promover `DeferredRecallScheduler` (hoy privado de Memoria) a `core/` como `ReviewItem(id, moduleId, constructTag, lastSeen, outcome, intervalIndex)` con intervalos expandidos [24h, 3d, 7d, 21d]. Siete productores triviales: ítem fallado de Lenguaje + su `ErrorType` (14 tipos con repair plans, hoy sin repaso), estructura fallada de Lógica, escenario de Decisiones para re-preguntar la estimación, ítem de separación de EM3. **Mecanismo con la mejor evidencia del plan y el scheduler ya escrito.** Control interno: la mitad de los ítems fallados entra en la cola, la otra mitad no ⇒ la pendiente de olvido comparada entre ambos es el contraste decisivo, barato y ético. [Mejora crítica]

**S3 · Invariante de plataforma: recuperación por producción, no reconocimiento.** `ResponseMode { PRODUCTION, CONSTRUCTED, FORCED_CHOICE, DETECTION }`, con gate como test en `desktopTest` (que ya está en `check`), no como scanner de texto — es validación de datos estructurados, igual que `BankValidationGateTest`. Regla: un ejercicio que actualiza el perfil y declara constructo de memoria, razonamiento o lenguaje **no puede** usar `FORCED_CHOICE` como único modo. `DETECTION` sigue siendo legítimo en paradigmas de RT. Mata el sesgo de posición de Lenguaje por construcción, restaura span/ARC/binding en Memoria de una vez, y elimina el ~50% de acierto por azar en Lógica. **La accuracy bajará: esa es la señal esperada.**

**S4 · Contenido como servicio compartido.** `BankIntegrityAuthority` ya existe y es fail-closed con registro opt-in **vacío** — lo que significa que ninguna fuente tiene contenido validado hoy. Falta: registrar los bancos reales, holdout disjunto obligatorio (generalizar `TransferProbePartition`, que valida disjunción en `init`), **ledger de exposición transversal** (`SessionRecord.reproducibleIdentity` ya persiste semilla+versiones; `core/bank/ExposureTracker` existe), y generadores paramétricos donde el banco a mano es inviable (Lógica, Decisiones, Emociones, Velocidad E3). **Con banco finito, la exposición acumulada es el mejor predictor de la "mejora"**: registrarla convierte una confusión en un control.

**S5 · Currículo y periodización global.** `TrainingCycle` + `RecoveryGate` (Focus) son el artefacto correcto en el sitio equivocado, y su personalización está contenida a NEUTRAL por un flag. Promover a `core/curriculum/` con microciclo de 4 semanas: adquisición (bloqueado, 20–25% error) → consolidación (intercalado intra-módulo, 25–30%) → variación (intercalado cruzado, 30–35%) → deload + **batería de probes no entrenados**. Nota honesta: el espaciado a escala de semanas tiene evidencia fuerte; el **deload cognitivo es [Inferencia]** importada del entrenamiento físico — heurística razonable, no hallazgo establecido, y debe etiquetarse así.

**S6 · Calibración metacognitiva transversal — la mejora de mayor retorno del sistema.** Hay **tres implementaciones abandonadas del mismo constructo**, prueba de que es concern de sistema: `calculateBrierScore`/`calculateCAG` sin callers, `confidenceBias = 0.0` hardcodeado (`MemorySessionStateHolder.kt:263`, que hace `ActivateMetacognition` y 2 arquetipos inalcanzables), `ScoringTiming.confidence` nunca rellenado, y la certeza de Bayes recogida y descartada.
**Principio no negociable: la confianza NUNCA suma al score de rendimiento.** El `confidenceScore` de Lógica (`ReasoningScorer.kt:33-37`) es la **regla lineal, matemáticamente impropia**: su óptimo está siempre en un extremo ⇒ el sistema **enseña activamente a sobreconfiar** y luego penaliza el error confiado. Y `confidenceCalibrationScore` = `1 − |acierto − p|` **no mide calibración**: mezcla calibración con exactitud, premiando al más preciso. Sustituir por tres ejes **ortogonales**: Brier (regla propia, uso interno), **sesgo con signo** (`mean(p) − accuracy`, lo único que se muestra), y **resolución** (¿distingues tus aciertos de tus fallos?). `n ≥ 40` juicios ⇒ es transversal por necesidad, no un ejercicio de 20 trials. Complementaria y de alto valor aplicado: correlación entre fatiga autorreportada y `RT_CV` real — *"¿sabes leer tu propio estado?"* — medible sin banco y no memorizable.

**S7 · Batería de transfer probes de sistema.** Las tres piezas existen y ninguna está conectada extremo a extremo: `ReliableChangeIndex` (correcto, duplicado en Memoria y Focus), `RealImprovementCriterion` (gate honesto, 0 callers), `ProbeReliabilityEstimator` (fail-closed correcto, sin datos). Falta: promover a `core/measurement/`, un `ProbeSlot` en la sesión, cobertura por **constructo** (Focus tiene probes para 3 de 13 ejercicios), y **doble línea base PRE-1/PRE-2 sin entrenamiento intermedio** — hoy `untrainedDelta > 0.05` puede ser íntegramente práctica del propio probe y aun así se clasificaría `GENUINE_IMPROVEMENT`. El criterio honesto es `untrainedDelta > (PRE-2 − PRE-1) + 0.05`. **Techo real del producto: E3 de contenido.** Todos los probes son `SAME_PARADIGM_RESERVED`; E4 (`DIFFERENT_PARADIGM`) está diferido y es la mejora de mayor retorno científico: Focus ya tiene 3 paradigmas y `ProbeExerciseMapping` — basta administrar como probe uno **no entrenado** por ese usuario.

**S8 · El estado como covariable de primera clase.** `ReadinessSnapshot(selfReport, cri, timeOfDay, sessionsLast24h)` en `core/state/`. Tres usos, en orden de valor: (1) covariable de scoring para descontar varianza de estado —el menos glamuroso y el mayor—; (2) gate de elegibilidad de medición (readiness baja ⇒ válida para práctica, **no** actualiza perfil, mismo patrón fail-closed que timebase y `BankIntegrity`); (3) gate de carga. Si readiness explica ≥10% de la varianza intra-sujeto, el sistema estaba atribuyendo a capacidad algo que era estado.

## Progresión global: de 8 escaleras a una política

Cuatro motores incompatibles y tres muertos. Política única en `core/adaptive/`:
- **Objetivo por tasa de error, no por precisión.** 25–30% en consolidación, 15–20% en deload, 30–35% en variación. Hoy cada módulo tiene su banda implícita (0.85/0.65, 0.85/0.70, 0.80/0.55, 0.85/0.78…) y **ninguna está justificada por escrito**.
- **Dos ejes ortogonales obligatorios por contrato de tipo:** carga y presión temporal. Y el invariante `NO_TIME_FOR_ABILITY`: un `stepUp` solo mueve un eje temporal si el constructo **es** velocidad, la exactitud está en consigna **bajo una regla que no penaliza la tardanza**, y ya hay un eje de carga saturado. Esto obliga a partir `pc` de Velocidad en `judgementAccuracy` + `deadlineCompliance` — un cambio de una línea en la captura que desbloquea todo lo demás.
- **La adaptación se prohíbe sobre métricas no confiables.** Si la métrica gobernante tiene `confident = false` o `n < MinSampleGate`, la dificultad **no se mueve**. Hoy Velocidad adapta sobre 12 trials y Emociones sobre 20: oscilación errática que el usuario lee como progreso/regresión aleatoria y que destruye la adherencia por ruido.
- **Familias declaradas:** `ACCURACY_BAND` (targetP explícito), `PSYCHOPHYSICAL_STAIRCASE` (targetP **derivado** de la regla: 1-arriba/2-abajo → 70.7%; umbral = media de reversiones post-burn-in, `null` si no convergió), `ERROR_RATE_BAND`. **El test que hoy no existe y que define si esto funciona:** respondedor sintético con habilidad conocida debe converger a la consigna en ≤5 bloques. Ningún motor lo tiene porque **ninguno tiene consigna que recuperar**.

## Mezcla de módulos: qué se intercala con qué

| Par | ¿Mezclar? | Razón |
|---|---|---|
| Enfoque ↔ Memoria | **Sí, alto valor** | Enfoque provee la tarea distractora que Memoria necesita para inducir demora real. Intercalado + demora, dos pájaros |
| Memoria ↔ Lenguaje | Sí | El repaso espaciado de errores de Lenguaje **es** el protocolo `VOCABULARY` que Memoria declara y no ejecuta |
| Lógica ↔ Lenguaje | Sí | Comparten andamiaje de justificación y evidencia: varían superficie manteniendo constructo |
| Decisiones ↔ Emociones | **Sí, prioritario** | El par con más sentido funcional: decidir bajo estado activado. EM8 precede a Bayes |
| Enfoque ↔ Velocidad E6/E7 | No | Mismo constructo, arrastre motor |
| Enfoque ↔ Velocidad **E1** | **Obligatorio** | E1 es el instrumento que hace comparables los RT del resto (R-CONTRA-3) |
| Creatividad ↔ cualquiera | Solo al final, posición fija | Fatiga previa colapsa fluidez, su única métrica |
| RT-crítico en posición ≠1 | Solo con marca | `positionInSession` registrado y modelado, o sesgo sistemático en las medidas más frágiles [Riesgo científico] |

**Consolidar precede a mezclar.** Con 6 paradigmas duplicados, el intercalado alternaría entre variantes del mismo proceso creyendo que varía el contexto: coste de switching sin variación de constructo, el peor de los dos mundos.

## Conexión emoción ↔ cognición ↔ motivación ↔ atención ↔ metacognición

Cinco piezas en cinco sitios y **cero caminos de datos entre ellas**. Cuatro aristas, todas construibles con lo existente:

1. **Estado → Atención:** `ReadinessSnapshot` entra en `ReadinessGate` antes del primer bloque y fija volumen, presión y **si la sesión es elegible para perfil**. Piezas: `DecisionCheckInStateHolder` (calcula el CRI y lo tira), `RecoveryGate` (sabe reducir), `SessionEligibility` (sabe excluir).
2. **Error → Metacognición:** cada bloque emite `(confianza, acierto, latencia)`. El feedback no es "fallaste", es **"fallaste donde te sentías seguro"** — la única forma de feedback de error que produce cambio de estrategia.
3. **Metacognición → Motivación:** elección **calibrada** del reto. El sistema propone la banda deseable (25–30% error) y el usuario elige dentro. Autonomía dentro de un rango correcto, ni imposición ni elección libre (que lleva a elegir demasiado fácil).
4. **Motivación → Metacognición:** el feedback longitudinal debe ser **el cambio en el holdout no entrenado**, no la accuracy de la sesión. Menos halagador y lo único no fabricable. [Dato necesario] hipótesis abierta: el feedback honesto puede costar adherencia a corto plazo. Medirlo, no asumirlo en ninguna dirección.
5. **Cognición → Emoción:** el módulo de Emociones deja de ser una puerta y pasa a ser **un modo**: los bloques de separación (EM3, el único con base teórica) se insertan tras un bloque de alto error en Lógica o Decisiones — cuando la frustración es real y la regulación tiene objeto. Regular una emoción inducida por 28 frases es un simulacro; regular la frustración de haber fallado 8 de 10 problemas es el constructo. Métrica: rendimiento en el bloque **posterior**, con vs sin regulación intercalada.

## Adherencia sin sacrificar medición

**Métrica principal: Adherencia Medible 4×4** — % de usuarios con ≥4 sesiones **válidas** (que producen al menos un bloque `confident` y elegible para perfil) en cada una de 4 semanas consecutivas. No retención D30: la retención se sube con notificaciones y confeti, y ese movimiento **destruye** la métrica principal. Es la única definición que hace imposible ganar adherencia sacrificando medición.

Cambios concretos, todos cableado de lo existente:
- **Botón único "Entrenar hoy · 8 min"** que compone 3 bloques (1 ancla fijo 14 días + 2 rotativos) desde `TransversalDailyPlanner`. Hoy: 4–5 decisiones antes del primer trial.
- **Cablear `volumeMultiplier` y `capDifficulty`** (`DailyFocusPlan.kt:27-37`, **0 consumidores** en `commonMain`): hoy el gate dice "descansa" y el usuario entra igual, a volumen completo. `RestRecommended` ⇒ ruta de recuperación real.
- **`averageTrialMs` por ejercicio** (`SessionTemplate.kt:28-41` usa la constante de E1 para los 13 y el propio código lo confiesa): "10 min" no dura 10 minutos en 12 de 13 ejercicios. Romper el contrato de tiempo es el predictor de abandono más barato de arreglar.
- **La racha debe sobrevivir al descanso.** `estimateStreak` cuenta días con sesión y se pinta junto al banner "Descanso recomendado": el sistema pide descansar y castiga hacerlo. Un día `RestRecommended` cuenta como día válido — y el test discriminante obligatorio es que 6 sesiones abandonadas sin bloque completo siguen dando racha 0.
- **Cierre diario de PROCESO, nunca de capacidad.** "Hoy entrenaste atención sostenida y memoria de trabajo. Datos utilizables: 3 de 3 bloques. Tu progreso se revisa el día 30."
- **Prohibiciones de refuerzo:** nada de trofeos por subir nivel de dificultad (premia tolerancia al cronómetro), nada de "% de acierto" al cerrar (en Memoria es reconocimiento trivial), nada de índices compuestos en el cierre diario, nada de gamificar antes de tener la brecha entrenado−holdout (gamificar sobre medición falsa **escala el error**).

## Cuadro de mando de sistema

| # | Métrica | Umbral | ¿Existe hoy? |
|---|---|---|---|
| M1 | Brecha entrenado − holdout, por constructo, IC95 | mejora de holdout > 0 con IC que excluye 0 en ≥1 constructo a las 8 sem | **No** |
| M2 | Curva de retención vs log(intervalo) | se aplana en cola programada vs no programada | Solo Memoria, parcial |
| M3 | Calibración: sesgo con signo + resolución | sobreconfianza ↓ en un módulo **sin** feedback de calibración | **No** (3 impls muertas) |
| M4 | Convergencia de dificultad: distribución de tasa de error + volatilidad | moda en banda objetivo; volatilidad ↓ tras gate de confianza | Parcial |
| M5 | Exposición por ítem; correlación exposición↔accuracy | ≈0 en ítems que informan el perfil | **No** |
| M6 | Adherencia 4×4 por fase del microciclo | sin caída >20% en la fase de máxima interferencia | **No** |

**4 de 6 no existen, y las dos que más pesan llevan meses de infraestructura construida y no conectada.** El problema del sistema no es que falte ciencia: es que la ciencia construida **no tiene por dónde correr**.

---

---

# 10. Ranking de ejercicios

48 ejercicios alcanzables. Notas de `docs/audits/modules/*.md`; las 4 de Velocidad no auditadas van estimadas y marcadas.

| # | Ejercicio | Módulo | Nota | Decisión final |
|---|---|---|---|---|
| 1 | E4 Freno (stop-signal) | Enfoque | **6.8** | Mantener con cambios menores → entrenamiento avanzado / medida de inhibición de referencia |
| 2 | **E8 Inhibición Rápida** | Velocidad | **6.5** [Inferencia] | **Mantener con cambios menores.** Único con contrato de calidad completo (diagnostics + integridad escalonada + elegibilidad por capa + run persistido). **Es el patrón que el resto debe copiar.** Deuda: redundante con Focus E3/E4 y EM1/EM8; falta d′ |
| 3 | E1 Vigía | Enfoque | 6.2 | Mantener con cambios menores (alinear eje gobernante + lure discriminable) |
| 4 | E2 Filtro | Enfoque | 6.0 | Mantener con cambios menores |
| 5 | E13 Presión | Enfoque | 5.7 | Mantener con cambios menores (tarea base con conflicto + SAT) |
| 6 | E3 Gatillo | Enfoque | 5.5 | Rediseñar parcialmente (SDT + tasas) o fusionar el rol con E4 |
| 7 | E6 Cambio | Enfoque | 5.5 | Rediseñar parcialmente (romper banco finito + CSI real) |
| 8 | Evidence Lock | Lenguaje | 5.5 | Mantener con cambios mayores (ancla del módulo; arreglar sobre-selección, ampliar banco, cablear adaptación/transfer) |
| 9 | E11 Intruso | Enfoque | 5.3 | Rediseñar parcialmente (intruso con carga real + coste RT) |
| 10 | **E5 Búsqueda Visual** | Velocidad | **5.0** [Inferencia] | **Fusionar con Focus E7.** Dos pipelines de búsqueda visual con diagnósticos propios; conservar el de Velocidad (mejor instrumentado) |
| 11 | **E4 Cambio de Regla** | Velocidad | **5.0** [Inferencia] | **Fusionar con Focus E6.** `RuleSwitchAnalyzer` es el mejor de los dos; consolidar ahí |
| 12 | E5 Stroop/Simon | Enfoque | 4.8 | Rediseñar parcialmente (Stroop real) o fusionar con E2 |
| 13 | Selección (Wason) | Lógica | 4.8 | Rediseñar parcialmente. Mejor mecánica del módulo; merece ser generador completo y ancla |
| 14 | E6 Multivía | Velocidad | 4.7 | Rediseñar parcialmente (nº de alternativas/incompatibilidad + corrección por SRT) |
| 15 | Contraejemplo | Lógica | 4.6 | Rediseñar parcialmente (modo construcción + cablear `CounterexampleValidator`) |
| 16 | E7 Rastreo | Enfoque | 4.6 | Rediseñar parcialmente (conjunción + pendientes separadas) |
| 17 | Bayes Arena | Decisiones | 4.5 | Rediseñar parcialmente. Mayor ROI del módulo: activar multi-sensor, frecuencias naturales, certeza→calibración, eliminar doble conteo del ICD |
| 18 | Argument X-Ray | Lenguaje | 4.5 | Rediseñar parcialmente (distractores plausibles + clasificación + arreglar posición) |
| 19 | E7 Regla puerta | Velocidad | 4.5 | Rediseñar parcialmente (señal perceptiva + congruencia/inversión + segregación) |
| 20 | EM3 Separador | Emociones | 4.5 | Rediseñar parcialmente — **la joya del módulo**; con banco finito es pseudoprogreso. Prioridad máxima del módulo |
| 21 | Scope & Negation | Lenguaje | 4.2 | Rediseñar parcialmente (latencia, distractores plausibles, más operadores) |
| 22 | Condicionales/Auditoría | Lógica | 4.2 | Rediseñar parcialmente (ítem fijo → generador paramétrico) |
| 23 | E12 Reenganche | Enfoque | 4.2 | Rediseñar parcialmente (inducción real de errores) o fusionar con E1 |
| 24 | Source Triage | Lenguaje | 4.3 | Rediseñar parcialmente (centrar en "qué falta para fiarte", quitar marcadores-regalo) |
| 25 | Transferencia isomorfa | Lógica | 4.1 | Mantener con cambios mayores (N pares + separación temporal + unificar umbral) |
| 26 | E1 Destello | Velocidad | 4.0 | **Convertir en calentamiento/baseline** — instrumento de calibración que debe alimentar a los demás |
| 27 | EM2 Recuperación | Emociones | 4.0 | Rediseñar parcialmente (staircase + carga afectiva real) y fusionar EM11 dentro |
| 28 | EM11 Continuidad | Emociones | 4.0 | Fusionar con EM2, o implementar de verdad el feedback negativo prometido |
| 29 | Resultado + Diario | Decisiones | 3.9 | Rediseñar parcialmente (cifrar/estructurar diario; polígono a ejes no redundantes; gatear coach por validez) |
| 30 | Detección de contradicción | Lógica | 3.9 | Rediseñar parcialmente (urgente: ítems con contradicción real + balancear base-rate) |
| 31 | EM1 Pausa | Emociones | 3.5 | Rediseñar parcialmente (stop-signal afectivo) o fusionar con EM8 |
| 32 | EM8 Anti-impulso | Emociones | 3.5 | **Fusionar con EM1** (mismo paradigma, distinta ratio) |
| 33 | EM10 Sep. bajo presión | Emociones | 3.5 | **Fusionar con EM3** como modo presión. No merece slot |
| 34 | EX3 Lista+Estrategia | Memoria | 3.5 | Rediseñar parcialmente (recall por producción y orden, demora real, test diferido a 24h). Mayor retorno del módulo |
| 35 | EX4 Pares Asociados | Memoria | 3.5 | Rediseñar parcialmente (presentar cue y exigir target). En su forma actual **no mide memoria asociativa** |
| 36 | **E2 Comparación** | Velocidad | **3.5** [Inferencia] | Rediseñar parcialmente o fusionar con E1 (comparación perceptiva; constructo delgado) |
| 37 | E11 Rastreo/Búsqueda dupl. | — | — | (consolidado en #16) |
| 38 | Check-in cognitivo | Decisiones | 3.4 | **Convertir en diagnóstico** (readiness puro) y cablearlo a la dificultad. No es un ejercicio de decisiones |
| 39 | E3 Cifrado | Velocidad | 3.4 | Rediseñar completamente (digit-symbol real con banco rotativo; el actual es matching binario con banco de 4) |
| 40 | E8 Dual-task | Enfoque | 3.1 | **Rediseñar completamente (runtime de dos respuestas) o ELIMINAR** |
| 41 | Necesario/Suficiente | Lógica | 3.1 | Rediseñar completamente. La filtración de respuesta en el prompt lo **invalida como medición** |
| 42 | E9 MOT | Enfoque | 3.0 | **Rediseñar completamente o ELIMINAR** hasta que el scoring dependa del tracking |
| 43 | EX1 Span Complejo | Memoria | 3.0 | Rediseñar parcialmente; sin runtime a la altura, **eliminar del set** |
| 44 | Semantic Surgery | Lenguaje | 3.0 | Rediseñar completamente (sesgo de posición urgente; mide registro, no precisión) |
| 45 | Compression Ladder | Lenguaje | 3.0 | Rediseñar completamente (reconocimiento → producción; arreglar posición fija) |
| 46 | EX01 Divergente | Creatividad | 3.0 | Rediseñar parcialmente (alinear medición con la consigna + originalidad/flexibilidad + normalizar dedup) |
| 47 | E10 RSVP | Enfoque | 2.9 | **Rediseñar completamente (dos juicios post-stream) o ELIMINAR** |
| 48 | EX02 Rotación | Creatividad | 2.2 | Rediseñar completamente o **FUSIONAR con EX01** (sin clasificación de categorías es duplicado deshonesto) |
| 49 | **Reversibilidad/Triaje** | Decisiones | **1.7** | **Rediseñar completamente.** No entrena nada y es explotable al 100%. Sin banco real e `isReversible` derivado de features, **no funcional** — la accuracy reportada es ruido |

**Medias por módulo:** Enfoque 4.89 · Velocidad 4.58 (4 de 8 inferidos) · Lógica 4.10 · Lenguaje 4.00 · Emociones 3.83 · Memoria 3.33 · Decisiones 3.30 · Creatividad 2.50.

---

---

# 11. Lista P0 / P1 / P2

## P0 — Críticos (bloquean cualquier afirmación de progreso)

| # | Acción | Archivo | Coste |
|---|---|---|---|
| P0-1 | **Aleatorizar la posición de la respuesta correcta** en los 6 bancos (32/36 = `"b"`, verificado) | `language/presentation/exercises/*Bank.kt` | horas |
| P0-2 | **Arreglar el bug de precedencia de la penalización** — parentetizar el `if` (el `weightedBase` de 10 líneas antes ya está bien) | `logic/domain/scoring/ReasoningScorer.kt:47-51` | minutos + test numérico |
| P0-3 | **Retirar del set o quitar los números** a los ejercicios cuya métrica no es función de la conducta | Focus E8/E9/E10, Reversibilidad | 1 release |
| P0-4 | **Derivar `isReversible` de features del escenario** + banco real; hasta entonces, retirar del cómputo | `decisiones/.../ReversibilityEngine.kt:18-35` | grande |
| P0-5 | **Ola 0 de Emociones:** histéresis vs `BLOCKS`, `ACCURACY` vs `CATEGORY_ACCURACY`, `MIN_NOGO` vs trials/bloque, persistir dificultad, `confidence` ≠ `dataSufficiency` | `EmotionSessionStateHolder.kt:871-872`, `EmotionAdaptiveEngine.kt:156`, `EmotionAggregateFactory.kt:42,51` | **5 defectos de una línea cada uno** |
| P0-6 | **Cablear los call-sites adaptativos**: dejar de pasar `FatigueSignal.NONE`, `emptyMap()`, `Int.MAX_VALUE`, `0.0`. Donde no haya señal, `null` explícito ⇒ `Hold(SIGNAL_UNAVAILABLE)`. Quitar los defaults de `MemoryAdaptiveInput` para que el compilador lo impida | `FocusSessionStateHolder.kt:477-487`, `MemorySessionStateHolder.kt:264-270` | 1 día · **resucita 8 reglas de seguridad** |
| P0-7 | **`d′` + criterio `c`** puros (corrección loglineal, IC95) en `core/scoring/`, consumidos por Focus E3, Speed E8, EM1/EM8 y check-in | nuevo `core/scoring/` | ~40 líneas + tests |
| P0-8 | **Unificar `RT_CV` a `SD/mediana`** en todo el repo, renombrado `RT_CV_MEDIAN`, con test que falle si aparece `sd/mean` bajo el nombre `cv` | Focus + Velocidad | bajo |
| P0-9 | **`MetricValue.value: Double?`** + borrar el mapeo no-finito→0.0 + eliminar `impIdx ?: 0.0` y `POST_ERROR_ACCURACY = 0.0` sin errores | `DefaultMetricsEngine.kt:147,208`, `SpeedScoreEngine.kt:91` | medio |
| P0-10 | **Separar exactitud de latencia en Velocidad** (`judgementCorrect` + `withinDeadline`) — desbloquea `NO_TIME_FOR_ABILITY` | `SpeedSessionStateHolder.kt:193` | 1 día |
| P0-11 | **Meter `presentation/`, `profile/presentation/` y `coach/` en el copy-scanner** con allowlist enumerada; unificar el lexicón de emotions con `ForbiddenLexicon.ALL` + test de paridad | `shared/build.gradle.kts:446-447` | bajo |
| P0-12 | **Quitar "línea base" del subtítulo del perfil** hasta que exista baseline sellado + retest | `BrainProfileUiModelBuilder.kt:33` | minutos |
| P0-13 | **`core/safety/CrisisRouting.kt`** (extraer de `AICoachSafetyPolicy`) aplicado a diario de Decisiones y Creatividad, + red-team de ofuscación | `coach/safety/` → `core/safety/` | ~40 líneas ya escritas |
| P0-14 | **Auditar los 4 ejercicios vivos de Velocidad** (E2/E4/E5/E8) y recalcular redundancia, cobertura y motores con el módulo completo | — | bloqueante metodológico |
| P0-15 | **Test de falsificación V0** (`NULL`/`RANDOM`/`EXPERT`) para los 48 ejercicios, reutilizando `EXERCISE_QUALITY_CONTRACT.md` L1–L4 en vez de reinventarlo | `desktopTest` | mayor ROI sin usuarios |

## P1 — Alto impacto

- **P1-1 · Capa de calibración transversal** (`core/metacognition/`): sacar `CONFIDENCE_WEIGHT` del `weightedBase` de Lógica, sustituir la regla lineal impropia por Brier, exponer **sesgo con signo** y **resolución**, `METACOGNITION` como `CognitiveDimension` con productor fail-closed. Cero datos nuevos, cierra tres impls muertas y **da uso a `CALIBRATION_ERROR`, huérfana desde hace meses**.
- **P1-2 · Runner multi-bloque** (`FocusSessionStateHolder.kt:394`) + cablear `volumeMultiplier`/`capDifficulty` + `averageTrialMs` por ejercicio. Convierte dos mentiras del producto (duración, descanso) en conducta real.
- **P1-3 · Espaciado transversal**: promover `DeferredRecallScheduler` a `core/` + 7 productores de `ReviewItem`.
- **P1-4 · Banco generativo** en Lógica (plantillas paramétricas), Emociones (separador composicional ≥300 + activar los 12 ítems `isEval` ya reservados), Lenguaje (>36) y Velocidad E3 (clave rotativa).
- **P1-5 · Recall por producción** en Memoria: presentación serial temporizada + `ProcessingTask` puntuado + recall por orden. Restaura span, ARC y binding de una vez.
- **P1-6 · Registro transversal de carga** (`core/session/TrainingLoadLedger`, solo conteos y timestamps, fuera de `profile.*`) — hoy `MemoryRecoveryPolicy`, `PlateauDetector` e `InterventionPlanner` se construyen y **nunca se invocan**, y el `FatigueGate` de Lenguaje tiene 0 call-sites.
- **P1-7 · Racha que sobrevive al descanso**, con test discriminante en ambas mitades.
- **P1-8 · Promover `emotions/domain/safety/` a `core/safety/`** y aplicarlo a Focus E12/E13 y Speed E8; línea base de RT en los primeros 8 ensayos del bloque 0 (hoy se captura tarde y 4 de 6 proxies son inalcanzables).
- **P1-9 · Poblar `CoachSignals`** (6 de 22 campos hoy) **o borrar T04–T09**. Un coach metacognitivo testeado que nadie puede recibir es peor que no tenerlo.
- **P1-10 · Copy de Creatividad y Decisiones al constructo realmente medido**; corregir las 4 etiquetas de transferencia infladas (EM8, EM2, EM11, EM10) — el enum de transferencia es el único mecanismo de contención y hoy es internamente inconsistente.
- **P1-11 · Superficializar los 7 claims honestos que el producto ya puede hacer y no hace** (timebase, `null≠0`, anti-gaming, escalera de evidencia, borrado fail-closed, pausa que cuenta, progreso sesión a sesión). Diferenciadores de categoría, verificables hoy.

## P2 — Secundarios

- Índice de fatiga en Focus y Memoria (solo existen en Velocidad y Lenguaje).
- `spatial_reasoning`: construir `EX5_GRID_ESPACIAL` o declarar el dominio explícitamente no cubierto.
- Renombrar los tres sentidos de "calibración"; alinear `EM4_CALIBRADOR` con su métrica real.
- Reconciliar el corpus previo (`MASTER_AUDIT_V2.md`, `AUDIT_SYNTHESIS.md`, `EXERCISE_QUALITY_CONTRACT.md`) y registrar IDs en `MINDBOOST_AUDIT_LEDGER.md` con `SUPERSEDED` apuntando al nuevo.
- Auditar `features/arena/`: emite señal cognitiva y ninguna lente la toca.

## ELIMINAR

Focus **E8** (contador decorativo), **E9** (memoria de localización vendida como MOT), **E10** (blink con un tap) — o rediseño completo del runtime, que es más caro que su valor. **Reversibilidad** hasta tener banco con features. `com/mindverse/mindboost/core/adaptive/DifficultyAdapter.kt` (0 call-sites, bang-bang sin banda muerta). `MemoryPairsAntiGaming` ya retirado — el patrón es correcto. `SUSTAINED_FOCUS_INDEX`, `BLOCK_STABILITY`, `FATIGUE_INDEX` de Focus, `LISAS`, `VCE`, `fatIdx`, `total` de Velocidad, `averageIcr`, `transferConsistencyScore`, `elaboration`, `GRANULARITY_INDEX`, `ARC_CLUSTERING`, `CONSOLIDATION_INDEX`, ejes "Integridad" y "Reto enfrentado" del polígono.

## FUSIONAR

EM1+EM8 (inhibición, con staircase SSRT) · EM2+EM11 (post-error con staircase al 30%) · EM3+EM10 (separación, presión como modo) · Focus E3+E4 (inhibición) · Focus E2+E5 (conflicto) · Focus E1+E12 (vigilancia con error inducido) · **Focus E6 + Speed E4** (task-switching, conservar `RuleSwitchAnalyzer`) · **Focus E7 + Speed E5** (búsqueda visual, conservar `VisualScanQualityDiagnostics`) · Creatividad EX01+EX02 · Velocidad E1+E2.

## CREAR DESDE CERO

1. **Capa de calibración transversal** (P1-1) — la de mayor valor/coste del producto.
2. **Probe de span real de memoria de trabajo** — la capacidad con más evidencia de transferencia y hoy sin instrumento válido. `EX2_ACTUALIZACION_SET` ya está diseñado en el catálogo.
3. **Decisión bajo carga afectiva** (EM5), compartido con Decisiones: contraste conflicto − neutro, la métrica es el **sesgo**, no la precisión bruta.
4. **Doble línea base PRE-1/PRE-2** — es orden de administración, no infraestructura, y sin ella `untrainedDelta` no descuenta el retest del propio probe.
5. **Probes `DIFFERENT_PARADIGM`** — Focus ya tiene 3 paradigmas y `ProbeExerciseMapping`; es la mejora de mayor retorno científico del plan.
6. **Autopredicción longitudinal** (requiere arreglar `trend = UNKNOWN`) — convierte sesiones sueltas en progreso frente a la propia línea base.

---

---

# 12. Versión ideal de MindBoost

## Arquitectura de entrenamiento

```
core/
  curriculum/     MindBoostSession · SessionBlock · MixPolicy · TrainingCycle · ProbeSlot
  adaptive/       AdaptiveKernel (13 reglas ordenadas, seguridad primero) · ChallengeTarget
  scoring/        MinSampleGate · DescriptiveScorer (d′, c, SDT) · Measurement<T> sellado
  measurement/    ReliableChangeIndex · RealImprovementCriterion · PlateauDetector
  metacognition/  ConfidenceForecast · ErrorAwareness · MetacognitionMetrics
  bank/           ItemBank · BankIntegrityAuthority · HoldoutPartition · ExposureLedger
  state/          ReadinessSnapshot · ReadinessGate
  safety/         OverloadDetector · PauseGovernor · CrisisRouting
  session/        TrainingLoadLedger (solo conteos; fuera de profile.*)

módulos → exponen runBlock(spec): BlockOutcome. No poseen la sesión, no poseen la adaptación,
          no poseen la medición. Poseen el paradigma y el renderer.
```

La regla estructural que hoy falta: **`core` es neutral, los módulos aportan paradigma, y todo lo transversal vive arriba**. Hoy hay 7 motores adaptativos, 2 RCI duplicados, 3 impls de calibración y 8 tablas de n mínimos. Un solo estimador auditado con ocho consumidores es el mayor retorno de toda la reconstrucción.

## Evaluación inicial: `CALIBRACIÓN-3`

Tres sesiones cortas en días distintos (≈20 min totales), **dificultad fija**, ningún número mostrado.

- **S1 (6.5 min):** estado (20 s) → Velocidad E1 24 trials (SRT + consistencia + impulsividad) → Focus E1 75 trials con `perceptualLoad = 0.0` (≥22 señales, pasa `MinSampleGate`).
- **S2 (6 min):** E1 retest → Focus E2 60 trials con `interference = 0.50` (24/36 celdas ✓) → Focus E6 60 trials, `switchFrequency ≈ 0.45` (27/33 ✓).
- **S3 (7 min):** Focus E3 **70** trials con `inhibitoryDemand = 0.0` (21 no-go ✓ `DPRIME`) → Velocidad E6 4 bloques (curva velocidad-precisión, `FatigueIndex` viable) → 3 ítems de Lógica válidos + confianza.

**El fallo que hoy invalida el baseline existente y se arregla con tres números:** `BaselineProtocols.INITIAL` con `perceptualLoad = 0.35` da ~16 señales en E1 (gate: 20), 12 incongruentes en E2 (gate: 20/celda) y 18 no-go en E3 (gate: 20). **Los tres bloques fallan su propio gate de muestra.** Tres constantes en un archivo de datos.

**Regla dura:** S1 nunca es el ancla. Si el delta S1→S2 en SRT supera el criterio de cambio fiable ⇒ `WARMUP_DETECTED`, ancla = S2. Si además S2→S3 lo supera ⇒ `UNSTABLE` ⇒ **`SIN_PERFIL_DETERMINADO`**, que debe ser el resultado más frecuente tras S1 (test anti-sobreajuste: 500 usuarios de una única distribución ⇒ <20% de asignaciones).

**Exclusiones obligatorias del baseline:** los 2 ítems inválidos de Lógica, Reversibilidad, check-in sin ISI aleatorio, Focus E7–E10, Velocidad E3, Memoria en su forma actual, Emociones completa. Y **el baseline solo es autoritativo con `CHOREOGRAPHER_FRAME + NANOS_NATIVE`**: en desktop/iOS es jugable y no produce perfil.

## Perfiles

Diez **flags no exclusivos** con umbrales sobre índices intra-sujeto, no etiquetas. Asignables hoy tras P0-7 (d′/criterio): impulsivo, lento-pero-preciso, rápido-pero-impreciso, inconsistente, avanzado, principiante, baja tolerancia (conductual: abandono + salidas, **jamás inferencia emocional**). **Bloqueados y hay que decirlo:** "buena memoria / mala flexibilidad" (la mitad memoria no existe), "buena lógica / mala regulación" (doble bloqueo), "baja metacognición" (n=40 juicios ⇒ asignable hacia la sesión 7, no en el baseline).

El perfil consume **tres cosas y solo tres**: dificultad inicial por ejercicio, prioridad de ejercicios, y forma de sesión. **No escribe ninguna dimensión de `CognitiveProfile`**: es entrada de personalización, no afirmación cognitiva.

## Planes

| Tipo | Frec. | Dur. | Composición | Adapta | ¿Número? |
|---|---|---|---|---|---|
| **Diaria** | 5/sem | 7–9 min | 1 ancla (fijo 14 d) + 2 rotativos de módulos distintos | Sí, intra-bloque | **No** |
| **Profunda** | 2/sem | 20–25 min | 1 ejercicio, 3–4 bloques con descanso 45 s | Sí, rango completo | **No** |
| **Recuperación** | Cuando el gate dispara | 4 min | 1 bloque baja carga, dificultad congelada | **No** (cap duro) | **No**, y no elegible para perfil |
| **Semanal** | 1/sem | 6 min | **Probes no entrenados** + isomorfos | **No** | Solo "medición registrada" |
| **Mensual** | 1/30 d | 12–15 min | Batería fija, misma ventana horaria ±2h que el baseline | **No** | **Sí — único punto** |

Reglas que protegen la eficacia: el ancla no rota antes de 14 días aunque aburra (el aburrimiento se combate en los rotativos, no sacrificando la serie); semanal y mensual con dificultad congelada (una evaluación adaptativa no es comparable consigo misma); la mensual usa la misma ventana horaria que el baseline o el "cambio" mide hora del día [Riesgo científico]; **ningún módulo con banco finito o métrica no cableada entra en la mensual** — hoy eso excluye Lógica, Memoria, Focus E7–E10 y la separación emocional. Excluirlos no es cobardía: incluirlos es fabricar el número. Y la profunda no se acorta: si no hay 20 minutos, ese día no hay profunda.

## Motor adaptativo unificado

Trece reglas ordenadas, **seguridad antes que evidencia**, resueltas como tabla de datos con `firstOrNull` (no cascada de `return`, que choca con `ReturnCount`≤2):

```
0 MODE_GATE (invariante DENTRO del kernel; hoy AdaptiveModeGate existe y nadie lo llama)
1 SAFETY_STOP → 2 REDUCE_INTENSITY → 3 CHECK_IN → 4 COOLDOWN → 5 ABANDONMENT_GUARD
6 METACOGNITION (overlay, JAMÁS dificultad) → 7 CONTAIN_IMPULSIVITY (gated por familia)
8 STEP_DOWN (salta histéresis: bajar es seguridad) → 9 HYSTERESIS → 10 INSUFFICIENT_N
11 SWITCH_AXIS → 12 STEP_UP (con puerta de compromiso) → 13 HOLD
```

Cinco invariantes: bajar más rápido que subir; `INSUFFICIENT_N` bloquea la subida y no la bajada (fail-closed); la metacognición nunca mueve dificultad (un usuario sobreconfiado tiene fallo de calibración, no de capacidad); una regla cuyo constructo no existe en la tarea devuelve `NOT_APPLICABLE_TO_FAMILY`, no "no disparó"; y `NO_TIME_FOR_ABILITY` — el eje temporal solo sube si el constructo es velocidad, la exactitud está en consigna bajo regla que no penaliza tardanza, y hay un eje de carga saturado.

**Evento `adaptive_decision` con `inputs_missing[]`**, una fila por bloque. Los hallazgos "cuatro reglas muertas en Focus, cuatro en Memory" habrían aparecido el primer día como `fatigue`/`perAxisMastery`/`blocksSinceChange`/`confidenceBias` ausentes en el 100% de las decisiones.

## Capa metacognitiva y emocional transversal

- **Metacognición:** confianza muestreada al 25–30% de trials (nunca por trial en RT-crítico: 1–2 s destruyen la medida) + una predicción por bloque. Tres ejes ortogonales. Feedback que **enseña cómo**, copiando la forma del único motor que lo hace bien (`DefaultLogicFeedbackEngine`: patrón → diagnóstico → siguiente movimiento, sin revelar la solución): *"tu certeza va por delante de tu acierto — baja un nivel por defecto y comprueba si el desajuste se cierra"*.
- **Emoción:** deja de ser una puerta y pasa a ser un modo. Los bloques de separación se insertan tras alto error real. `ReadinessSnapshot` gatea volumen y elegibilidad de medición. El aparato de seguridad (`PauseGovernor`, `SAFETY_STOP`, techo de intensidad 3, pausa que nunca penaliza) sube a `core/` y cubre todo ejercicio con `governingAxis == PRESSURE`.
- **Regla de honestidad final:** el claim máximo defendible tras la reconstrucción es *"entrena estrategias, calibración y retención, y mide con rigor tu progreso contra tu propia línea base en estas tareas"*. Cualquier arquitectura que necesite prometer más para ser vendible está mal diseñada, no mal comunicada.

---

---

# 13. Resumen final brutal

Diez acciones, en orden de impacto. Las cinco primeras no requieren investigación: son wiring y aritmética.

1. **Arregla los tres defectos de minutos que invalidan tres módulos enteros.** Aleatoriza la posición correcta en `language/presentation/exercises/*Bank.kt` (32/36 = `"b"`, verificado). Parentetiza el `if` de `ReasoningScorer.kt:47-51` (el `weightedBase` de arriba ya está bien: el fix de 2026-06-15 se quedó a diez líneas). Quita "línea base" de `BrainProfileUiModelBuilder.kt:33`.
2. **Ejecuta la Ola 0 de Emociones — cinco defectos de una línea cada uno** (`BLOCKS`/`COOLDOWN`, `ACCURACY` vs `CATEGORY_ACCURACY`, `MIN_NOGO` vs trials/bloque, persistir dificultad, `confidence` ≠ `dataSufficiency`). Sin esto, generalizar ese motor a los otros 7 módulos propaga un `stepUp` **probado inalcanzable**.
3. **Cablea los call-sites adaptativos de Focus y Memory** (deja de pasar `NONE`/`emptyMap()`/`Int.MAX_VALUE`/`0.0`) y quita los defaults de `MemoryAdaptiveInput`. Un día de trabajo, **resucita 8 reglas de seguridad**.
4. **Audita los 4 ejercicios vivos de Velocidad (E2/E4/E5/E8) antes de tomar cualquier decisión de arquitectura sobre ese módulo.** E8 es el pipeline de referencia del producto y cinco lentes diseñaron sobre un módulo que creían de 4 ejercicios y tiene 8. Recalcula redundancia, cobertura y motores con el módulo completo.
5. **Corre el test de falsificación V0** (`NULL`/`RANDOM`/`EXPERT`) sobre los 48 ejercicios, reutilizando el `EXERCISE_QUALITY_CONTRACT.md` L1–L4 que ya existe. Sin usuarios, sin backend, en `desktopTest`. Elimina lo inválido antes de gastar un experimento en ello.
6. **Retira los números de E8/E9/E10 de Enfoque y de Reversibilidad.** Cuatro ejercicios cuyas métricas no son función de la conducta del usuario, uno de ellos explotable al 100% sobre un ground truth que es una moneda.
7. **Construye la capa de calibración transversal.** Coste mínimo, riesgo cero, cero datos nuevos, cierra tres implementaciones muertas, y es el único constructo comparable entre los 8 módulos — el candidato honesto a índice global el día que quieras uno, muy por delante del compuesto ponderado que `ADR_SCI_006_APPROVED = false` bloquea con razón.
8. **Decide la disposición de `focus2/` (Focus V5) por escrito antes de tocar `focus/`.** Motor completo, cero call-sites de producción, con el mejor scorer psicométrico del repo dentro y un plan S5 activo en esta rama. Reformar el V1 mientras un V5 puede sustituirlo es trabajo tirado. [Dato necesario]
9. **Cablea el ciclo de evaluación honesta antes de escalar la adquisición**: doble línea base PRE-1/PRE-2, holdout servido (copia el patrón de `EmotionHoldoutProbe`, el único que reserva **y administra** y quema el ítem), y el RCI N-of-1 con SEM intra-individual — declarando su techo por escrito ("distinguible de tu propia variabilidad", nunca percentil). **Un producto con adherencia perfecta sobre medición falsa escala el error.**
10. **Registra todo esto en `MINDBOOST_AUDIT_LEDGER.md` desde la sesión principal.** Como subagente reporto y no escribo el ledger, pero **ninguna de las 8 auditorías ni ninguna lente registró un ID**, y ninguna declara qué supersede del corpus previo (`MASTER_AUDIT_V2.md`, `AUDIT_SYNTHESIS.md`, `INFRA_P0.md`). Sin eso, este trabajo se duplicará con IDs nuevos y los hallazgos anteriores desaparecerán sin nota de supersesión — que es exactamente lo que el ledger existe para impedir.

**Lo que no verifiqué, declarado:** no releí las 8 auditorías completas (skim dirigido + greps); las notas por eje las tomo como dadas. No verifiqué `simpleReactionMedianMs` contra los mappers de E8/E5. No audité `features/arena/`. Las 4 notas de Velocidad marcadas [Inferencia] son estimaciones mías desde la infraestructura leída, no auditorías.
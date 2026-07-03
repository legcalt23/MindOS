# Ground-truth del repo real (scouts)

### Timing / Timebase infrastructure across MindBoost modules (KMP)
Hay **dos** familias de timebase, NO una autoritativa única:

1. **Speed (Velocidad)** es el ÚNICO módulo con una timebase autoritativa real, y solo en **Android**: `TimebasePolicy` + `SpeedTimebase.android.kt` capturan el onset del estímulo con `Choreographer.frameTimeNanos` y la respuesta con `MotionEvent.eventTimeNanos` (API 34+) / `eventTime*1e6` (<34), ambos en la misma base monotónica (`ANDROID_UI_UPTIME_NANOS`), prohibiendo explícitamente `elapsedRealtimeNanos()`. Desktop/iOS de speed caen a un **fallback NO-autoritativo** (`FallbackSpeedTimebaseHandle` con `TimeSource.Monotonic`, marcado `MILLIS_DERIVED` / `SYSTEM_CLOCK_MONOTONIC`).

2. **Focus (Enfoque)** mide RT con `MonotonicClock` (`TimeSource.Monotonic`, commonMain, sin expect/actual) — NO usa Choreographer/MotionEvent; el onset es `nowNanos()` en el hilo de la corrutina, no el frame de vsync. Es honesto (delta monotónico) pero **no autoritativo a nivel de frame**.

**Gate `device_unreliable`: existe el MODELO pero NO la medición.** `TimebasePolicy.deviceTimingRisk()` + `SessionQualityEvaluator` clasifican riesgo a partir de `criticalFrameJitter` y `frameDropFraction`, pero en `SpeedSessionStateHolder.kt:259-260` **ambos están hardcodeados a `false`/`0.0`**. No hay ninguna medición real de jank/frame-drop en ningún módulo. iOS NO tiene `CADisplayLink`.

**Los demás módulos miden RT/tiempo sobre un contador NO autoritativo de wall-clock:** logic, decisiones, emotions usan `EpochClock` (`Clock.System` epoch-millis); creativity usa `MonotonicMillis`; language no mide RT físico. Ninguno toca el frame de vsync ni captura el timestamp del input event.
- [partial] Timebase autoritativa frame-accurate (Choreographer + MotionEvent, misma base monotonica) — Solo en speed/Android: shared/src/androidMain/.../speed/presentation/SpeedTimebase.android.kt:41 (Choreographer.postFrameCallback), :59-69 (MotionEvent.eventTimeNanos); domain en shared/src/commonMain/.../speed/domain/TimebasePolicy.kt:54 (rtNanos = response - stimulus). Desktop/iOS de speed son fallback no-autoritativo.
- [present] Politica que prohibe wall-clock/elapsedRealtime para RT — shared/src/commonMain/.../speed/domain/TimebasePolicy.kt:11-14 (prohibe elapsedRealtimeNanos para RT); TimestampProvider.kt y Timebase enum ANDROID_UI_UPTIME_NANOS
- [present] Fallback monotonico no-autoritativo desktop/iOS (speed) — shared/src/commonMain/.../speed/presentation/SpeedTimebaseFallback.kt:27 (TimeSource.Monotonic.markNow), :29-30 (marcado MILLIS_DERIVED/SYSTEM_CLOCK_MONOTONIC); desktop e ios delegan en SpeedTimebase.{desktop,ios}.kt:14
- [partial] RT monotonico en focus (sin Choreographer) — shared/src/commonMain/.../focus/presentation/exercise/MonotonicClock.kt:13-21 (TimeSource.Monotonic, commonMain); FocusFixedBlockRunner.kt:83 (onsetNanos = monotonicClock.nowNanos()), :86 (latencyMs = response - onset). Onset NO es frame de vsync.
- [present] Gate device_unreliable / DeviceTimingRisk (modelo) — shared/src/commonMain/.../speed/domain/TimebasePolicy.kt:66-73 (deviceTimingRisk); speed/domain/quality/SessionQualityEvaluator.kt:46-69 (clasifica CONTAMINATED/LOW/HIGH/ACCEPTABLE con DeviceTimingRisk)
- [absent] Medicion REAL de jank / frame-drop que alimente el gate — shared/src/commonMain/.../speed/presentation/SpeedSessionStateHolder.kt:259-260 (frameDropFraction = 0.0, criticalFrameJitter = false HARDCODEADOS). No hay medidor de frames en ningun modulo.
- [absent] Timebase autoritativa iOS (CADisplayLink) — SpeedTimebase.ios.kt:14 delega en fallback monotonico; no hay CADisplayLink/displayLink en ningun .kt (grep solo en docs)
- [present] RT de logic/decisiones/emotions sobre wall-clock epoch (NO autoritativo) — core/time/EpochClock.kt:31-33 (Clock.System epoch-millis); logic/presentation/LogicStateHolder.kt:105,113 (timeSpentMs = now - itemStartedAt via epochClock); emotions usa epochClock.nowMillis() (EmotionSessionStateHolder.kt:140,173,338)
- [partial] RT de emotions en monotonic-nanos (mejor que epoch, no frame) — shared/src/commonMain/.../emotions/presentation/EmotionSessionStateHolder.kt:201 (onset = graph.monotonicClock.nowNanos()), :126 (response nowNanos). Usa MonotonicClock comun, no Choreographer.
- [partial] RT de creativity en monotonic-millis (anti-cheat, no frame) — shared/src/commonMain/.../creativity/presentation/CreativityMonotonicClock.kt:14-17 (MonotonicMillis, TimeSource.Monotonic millis); CreativitySessionStateHolder.kt:47,57 (elapsed = now - startMs)
- [absent] Medicion de RT fisico en language — grep en language/presentation y domain/scoring no muestra captura de RT por reloj; ClosedScoringEngine/ScoringEngine.kt puntuan correccion, no latencia. memory tampoco mide RT por reloj monotonico de input.

### MindBoost scoring &amp; estado (Focus/Speed/Logic/Decisiones/Memory + perfil global)
No existe UN `ScoreResult` comun: cada modulo tiene su propio tipo de score, todos honestos por construccion pero con convenciones distintas.

**Patrones de "estado de score" (4 estilos):**
1. **Banda + confident** (Focus): `FocusScore(value: Double?, lowerBound?, upperBound?, confident: Boolean)` — null cuando faltan datos; banda ancha con poca evidencia.
2. **`preliminary=true` + barras nullables** (Speed): `CognitiveProfileScore(usefulSpeed/control/stability/total: Double?, macroCategory, preliminary=true SIEMPRE)` mientras el gate fisico §15 siga abierto. Barra null = "no computable", nunca 0.
3. **Sealed gated result** (Memory): `RciResult.{NotEnoughData|Value(reliable)}`, `PlateauStatus`, `ImprovementPattern` — gating explicito hasta tener fiabilidad/probes.
4. **Maquina de estados de madurez** (Perfil global): `DimensionStatus.{CALIBRATING|EMERGING|ESTABLISHED}` con invariante `(CALIBRATING) == (value==null)`.

**null vs 0 HOY:** se respeta sistematicamente. `MetricValue.confident=false` + `valueOf()` devuelve default solo bajo demanda; `MetricValue.unavailable()` marca no-computable; barras/valores nullables; el aggregator deja `CALIBRATING` con value null (nunca 0 silencioso). `ScoringEquations` (Decisiones) es la excepcion: usa 0.0 como sentinela (sesion invalida AGRS>0.75 → PHD=0.0).

**Min-n por metrica:** SI, enforced. Focus `DefaultMetricsEngine` (MIN_N_ACCURACY=5, MIN_N_RT=5, MIN_N_POST_ERROR=3, MIN_N_SLOPE=6, MIN_N_CONGRUENCY=3); `DefaultFocusScoreCalculator` (MIN_COMPONENTS=2, MIN_N=20, MATURE_N=60). Speed via SessionMetrics. Memory via reliability gate (≥2-3 probes).

**Agregacion inter-sesion:** SI en 3 capas — `DefaultMetricsEngine.sessionMetrics` (promedio ponderado por trials entre bloques + fatiga); Memory `EvaluationComparator`/RCI (pre/post vs baseline); `MindBoostProfileAggregator` (pondera contribuciones por confidence·dataSufficiency, composite solo con ≥3 ESTABLISHED).
- [absent] ScoreResult comun unificado entre modulos — Cada modulo define su propio tipo: FocusScore.kt, SpeedScoreEngine.kt (CognitiveProfileScore), ReasoningScorer.kt (ReasoningScore/ScoringResult), ScoringEquations.kt (Double crudo), MindBoostProfile.kt (DimensionState). No hay supertipo/interfaz comun.
- [present] Focus composite score con banda de incertidumbre — FocusScore.kt:11-22 data class FocusScore(value:Double?,lowerBound?,upperBound?,confident,caption); DefaultFocusScoreCalculator.compute promedia componentes confident, banda=MIN_MARGIN+MAX_EXTRA_MARGIN*(1-maturity)
- [present] MetricValue con value/n/confident por metrica — MetricKey.kt:57-69 data class MetricValue(key,value,n,confident,unit) + unavailable(); valueOf() devuelve default si !confident (l.75-78)
- [present] MAD outlier rejection + anticipacion filtering en RT — DefaultMetricsEngine.kt:37-40 rawHitRts filtra anticipationFloorMs, cleanRts=RtStatistics.rejectOutliers (MAD); lapsos sobre rawHitRts NO MAD-filtrado (l.72-79)
- [present] min-n enforced por metrica — DefaultMetricsEngine.kt:461-467 MIN_N_ACCURACY=5,MIN_N_RT=5,MIN_N_POST_ERROR=3,MIN_N_SLOPE=6,MIN_N_CONGRUENCY=3; FocusScore.kt:76-77 MIN_COMPONENTS=2,MIN_N=20
- [present] Speed CognitiveProfileScore con preliminary=true y barras null — SpeedScoreEngine.kt:35-44 CognitiveProfileScore(usefulSpeed/control/stability/total:Double?, preliminary:Boolean=true); score() devuelve null si metricas faltan, total con techo ×0.5 (l.89-109)
- [present] SpeedScoreEngine techo/ceiling penalty (no calibrado) — SpeedScoreEngine.kt:97-99 penalize si pc<0.80 o cv>0.30 → total*0.50; usefulSpeedReferenceVce auto-referenciado, no percentil poblacional (l.50-66)
- [present] SsrtCalculator con flag trustworthy (anti-gaming) — SsrtCalculator.kt:39-53 metodo integracion Verbruggen; trustworthy = pRespond in 0.25..0.75 && ssrt>0; SsrtResult.{NotComputable|Value}. Usado en DefaultMetricsEngine.kt:407-414 confident=trustworthy && goRts>=MIN_N_RT
- [present] ReasoningScorer ICR (proceso-primero, speed solo resta) — ReasoningScorer.kt:42-52 weightedBase(finalAnswer .35+rule .20+premise .15+justif .15+confidence .10+independence .05) - penaltyPoints(impulsividad .10+conceptual .15); totalIcr coerceIn(0,100)
- [present] ScoringEquations Brier/BUR/calibracion (Decisiones) — ScoringEquations.kt:114-182 calculateBrierScore, calculateCalibrationError (5 bins), calculateBUR (logit ratio user/math, max 3.0), calculateCDE/PHD/ICD/IED/CRI/FRS/AGRS
- [present] Memory ReliableChangeIndex gated por fiabilidad — ReliableChangeIndex.kt:29-44 RciResult.{NotEnoughData|Value(reliable)}; reliable=|RCI|>1.96; NotEnoughData hasta tener ReliabilityEstimate (≥2-3 probes)
- [present] RealImprovementCriterion (untrained probe + effect + sustained) — RealImprovementCriterion.kt:42-68 isGenuine = untrainedDelta>0.05 && effectSize>=0.30 && sustainedWeeks>=2; classify() ImprovementPattern (GENUINE/PRACTICE_EFFECT/STAGNATION/...)
- [present] PlateauDetector (meseta + false-improvement) — PlateauDetector.kt:46-68 detect() → PlateauStatus.{None|Plateau|FalseImprovement}; FalseImprovement si trained>0.15 && untrained<0.05; InterventionPlanner regla de oro 2 semanas
- [present] MindBoostProfileAggregator con CALIBRATING/EMERGING/ESTABLISHED — MindBoostProfileAggregator.kt:48-103 + MindBoostProfile.kt:4-13 DimensionStatus; gating minSufficiency=0.5,minConfidence=0.4,established=0.7; composite solo con >=3 ESTABLISHED
- [present] null != 0 enforced (nunca 0 silencioso) — DimensionState.kt:35 require((CALIBRATING)==(value==null)); ModuleContribution.kt:25 normalizedScore null o 0..1; MetricValue.unavailable; CognitiveProfileScore barras Double?. Excepcion: ScoringEquations usa 0.0 sentinela (PHD=0.0 si AGRS invalido)
- [present] Agregacion inter-sesion / inter-bloque — DefaultMetricsEngine.kt:117-141 sessionMetrics promedio ponderado por trials + FATIGUE_INDEX; weightedAverage (l.431-442) solo metricas confident; EvaluationComparator.kt:37-54 pre/post baseline; aggregator pondera por confidence*dataSufficiency
- [partial] Cross-module common score interface (ModuleContribution one-way) — ModuleContribution.kt es el unico contrato comun: cada modulo expone normalizedScore(0..1|null)+dataSufficiency+confidence+preliminary al perfil global. NO es un ScoreResult de calculo, es un DTO de agregacion read-only.

### Anti-gaming / trial-quality / integrity detection across MindBoost (Focus, Speed, Decisiones, Creativity, Emotions) and Arena
El anti-gaming en MindOS está **realmente implementado y testeado** en sus mecanismos centrales de RT — no es vaporware de docs. Existen de verdad: el **filtro de anticipación** (RT < floor) en Focus y Speed; el **guard RT<400ms (AGRS)** en Decisiones; la **detección de waiting-strategy / inflación de go-RT en stop-signal** vía SSRT con flag `trustworthy` cuando p(respond|signal) se sale de [0.25,0.75]; la **detección de impulsividad / fast-commission** en el AdaptiveEngine de Focus; el **gate de calidad de sesión** de Speed (anticipationRate/CV/omission/frame-jitter→LOW/CONTAMINATED); los **detectores de fraude de Arena** (smurf, boosting por cadence-mismatch+device-change, account-sharing, collusion, lag-switch) con cadence-cooldowns; y la **persistencia de abandono** en Emotions.

PERO hay **huecos reales**: (1) la **cadencia/ritmo de tap** y el **mismatch de input-cadence** existen como detector SOLO en Arena (simulación offline con features sintéticas), NO en los ejercicios reales de MindBoost — ningún ejercicio mide la cadencia de toques del usuario real. (2) **Respuesta por posición/longitud** NO existe en ningún sitio (los comentarios de Decisiones dicen explícitamente "no spatial grid → no spatial-gaming signal" y meten `spatialEntropy=1.0` fijo, neutralizando ese término del AGRS). (3) **Keyword-stuffing**: NO existe; el validador de Creativity solo chequea timing monotónico, no-vacío y minIdeas — la capa de "Metric Integrity / anti-gaming" de Creativity está BLOQUEADA por diseño (comentario `BLOCKED`). (4) **Arena fraude** es 100% simulación sobre features sintéticas, sin pipeline que las derive de telemetría real.

**Fracción brutal:** de las ~8 capacidades pedidas, **~4 están IMPLEMENTADAS+TESTEADAS de verdad sobre datos reales** (anticipación, RT<400/AGRS, stop-signal waiting/SSRT, impulsividad-fast-commission + quality-gate + abandono), **~2 existen solo como simulación offline** (cadence/fraude Arena), y **~2 no existen** (posición/longitud, keyword-stuffing). El núcleo psicométrico de RT es sólido; el anti-gaming "conductual avanzado" (cadencia real, sesgo posicional, contenido) es diseño/placeholder.
- [present] Filtro de anticipación (RT < floor → ANTICIPATION) en Focus — shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/exercise/TrialClassifier.kt:33 (classify), :72 (classifyChoice); testeado en focus/domain/exercise/TrialClassifierTest.kt:42
- [present] Filtro de anticipación en Speed (umbral 100ms, anticipationRate) — speed/domain/metrics/MetricCalculator.kt:130-147 (anticipations filtradas; anticipationRate); TrialEngineTest.kt:35; MetricCalculatorTest.kt:48
- [present] Guard RT<400ms / AGRS (anti-gaming risk score) en Decisiones — mindverse/mindboost/core/scoring/ScoringEquations.kt:199-210 calculateAGRS (AGRS_FAST_RESPONSE_MS=400L:268, rtFactor si RT<400); umbral invalida sesión (AGRS_INVALID_SESSION_THRESHOLD=0.75:239,:41); test DecisionSessionOrchestratorTest.kt:186 + DecisionResultStateHolderTest.kt:59
- [present] Detección de waiting-strategy / inflación de go-RT en stop-signal (SSRT trustworthy) — focus/domain/metrics/SsrtCalculator.kt:39-53 — flag trustworthy=false cuando p(respond|signal) fuera de [0.25,0.75] (slowing estratégico); StopSignalTracker en E4FrenoGenerator.kt:41; tests SsrtCalculator/FocusStopSignalAssemblerTest.kt:106 + SimulatorLoopTest.kt:94 gamerOnStopSignal_breaksSsrtEstimate
- [present] Detección de impulsividad / fast-commission (errores rápidos) — focus/domain/adaptive/AdaptiveEngine.kt:52,116-121 (fastErrors = FALSE_ALARM con RT<impulsiveRtFloorMs=250) ; speed MetricEdgeCasePolicy.impulsivityIndexNoNoGo (MetricCalculator.kt:152). Testeado DefaultAdaptiveEngineTest.kt:80
- [present] Gate de calidad de sesión (anticipationRate/CV/omission/jitter → LOW/CONTAMINATED) — speed/domain/quality/SessionQualityEvaluator.kt:46-104 (HIGH_ANTICIPATION_RATE, HIGH_CV, FRAME_JITTER → CONTAMINATED); SessionQualityEvaluatorTest.kt:51
- [partial] Detección de cadencia/ritmo de tap en ejercicios reales — SOLO en Arena como feature sintética: integrity/engine/FraudSignalDetectors.kt:22-24 (inputCadenceMismatch) + trials/AscensionTrials.kt:108 TrialCadence cooldowns. NINGÚN ejercicio MindBoost mide cadencia de toques del usuario real
- [absent] Detección de respuesta por posición / longitud (sesgo espacial) — Neutralizado por diseño: decisiones/presentation/BayesArenaStateHolder.kt:71 y ReversibilityTriageStateHolder.kt:73 fijan spatialEntropy=1.0 con comentario 'no spatial grid: no spatial-gaming signal'. AGRS tiene término entropyFactor (ScoringEquations.kt:205) pero nunca recibe entropía real
- [absent] Detección de keyword-stuffing / contenido de baja calidad — creativity/domain/validation/CreativityResponseValidator.kt:18-26 solo valida timing monotónico, no-vacío, minIdeas. La Metric Integrity Layer está BLOQUEADA: creativity/ai/CreativityAiLayers.kt:44 'would score anti-gaming signals. BLOCKED'
- [present] Detección de abandono / sesión incompleta — emotions/presentation/EmotionSessionStateHolder.kt:159-163,349 (persist abandoned); EmotionMetricKey.ABANDONMENT + DefaultEmotionMetricsEngine.kt:79-88 (cuenta NO_RESPONSE); SafetyEvent.kt:12
- [partial] Detector de fraude multi-señal (smurf/boosting/collusion/lag-switch) Arena — arena/integrity/engine/FraudSignalDetectors.kt:13-58 implementado y testeado (FraudSignalEvaluatorTest.kt:36) PERO es 'Offline, deterministic... pure predicate over synthetic features' — sin pipeline desde telemetría real; HARD STOP antes de Fase 08

### MindBoost item banks (Logic, Language, Decisiones, Emotions, Creativity)
Todos los bancos de contenido **autorados** del repo son **pequeños, fijos y sin ninguna validación psicométrica** (sin discriminability real, sin inter-rater, sin holdout efectivo, sin exposure tracking). Inventario real:

- **Logic**: 6 ítems autorados fijos (`DefaultLogicContentProvider`); cada sesión sirve los **mismos 6** en orden fijo. Memorizable tras 1-2 sesiones.
- **Language**: 6 bancos × 6 ítems = **36 ítems autorados fijos** (Evidence Lock, Semantic Surgery, Compression Ladder, Argument X-Ray, Scope & Negation, Source Triage) + un `LanguageDemoBank` de 7. Sin barajado ni reposición. Muy memorizables.
- **Decisiones**: **NO hay banco autorado**. Bayes Arena y Reversibility son **procedurales numéricos** (`generateScenarios(seed)` → `BayesEngine/ReversibilityEngine.generateScenario`). Escenarios abstractos (base-rate/sensibilidad/FPR; reversible/irreversible). No memorizables como texto, pero sin contenido narrativo ni validación.
- **Emotions**: bancos autorados **PAUSA_WORDS (24)** y **SEPARATOR_ITEMS (40 = 10/categoría)**; resto de generadores (EM2/8/10/11) procedurales perceptuales. 15 ejercicios en catálogo, 6 generadores reales.
- **Creativity**: **14 prompts** estáticos (8 objetos EX01 + 6 temas EX02). Sin generación IA (bloqueada).

Campos por ítem: difficulty/level SÍ (entero 1-5); constructTag SÍ (TraitCode/LogicSkill/EmotionCategory); **discriminability** sólo existe en emotions como *parámetro perceptual de dificultad* (gap entre estímulos), NO como discriminación psicométrica IRT; **culturalLoad / interRater / exposure NO existen en ningún ítem**. El `isEval`/holdout de emotions existe pero está **reservado para "Ola-4" no implementada** (los eval items nunca se sirven). **Cero validación empírica, cero inter-rater, cero holdout activo en TODO el sistema.**

**Memorizables/pequeños/sin validar HOY**: Logic (6), Language (36), Emotions PAUSA(24)+SEPARATOR(40), Creativity(14). Decisiones es procedural (no memorizable) pero igualmente sin validación.
- [present] Logic item bank (authored) — LogicContentProvider.kt:29-36 — sessionItems() devuelve listOf(6 items fijos); mismos 6 cada sesion (logic-e1-audit-01, e3-access-01, e4-publish-01, e5-reservation-01, e23-chef-01, e3-library-iso-01)
- [present] Logic bank size = small/fixed — LogicModels.kt:267 sessionItemCount=6; LogicContentProvider.kt:29 lista fija sin shuffle ni reposicion → memorizable
- [absent] Logic procedural generation — LogicContentProvider.kt:23-27 KDoc: 'authored pack ... declarative; a future generative provider can implement the same contract' — generador procedural NO existe
- [present] Language banks (6 banks x 6 items) — LanguageExerciseCatalog.kt:15-20 (6 exercises); EvidenceLockBank.kt:42-161 (6 items), SourceTriageBank.kt:39-115 (6 items); conteo: cada *Bank.kt = 6 items numerados
- [present] Language LanguageDemoBank — LanguageDemoBank.kt:37-128 — 7 demo items fijos (single/evidenceSelection/ordering), KDoc:34 'Provisional offline bank ... Replace with the Content OS'
- [absent] Decisiones authored scenario bank — BayesArenaStateHolder.kt:generateScenarios → List(count){BayesEngine.generateScenario(...)}; ReversibilityTriageStateHolder igual. No hay listOf de escenarios autorados
- [present] Decisiones procedural generation — BayesEngine.kt generateScenario(): jittered base-rate/sensitivity/falsePositiveRate; ReversibilityEngine.kt: parametrico reversible/delayCost/lockMs; description='Reversibility triage level N' (templated)
- [present] Emotions authored banks — EmotionContent.kt:16-41 PAUSA_WORDS (24 AffectiveWord); :50-95 SEPARATOR_ITEMS (40 items, 10/categoria, comentario '7 training + 3 held-out eval each')
- [present] Emotions procedural generators — 6 generadores: EM1Pausa/EM2Recuperacion/EM3Separador/EM8AntiImpulso/EM10SepPresion/EM11Continuidad; EM2/8/10/11 perceptuales procedurales; catalogo lista 15 ejercicios (EmotionExerciseCatalog.kt cuenta 15 ids)
- [present] Creativity prompt bank — CreativityPromptCatalog.kt:12-30 — ex01Suppress (8 strings) + ex02Rotation (6 strings) = 14 prompts estaticos; KDoc:5-8 'ONLY source of prompts in the MVP (no AI generation, no network)'
- [present] Per-item difficulty/level field — LogicModels.kt:49-58 LogicDifficulty(1..5); DemoItem.level Int (LanguageDemoBank.kt:31); EvidenceLockBank items level=1..3
- [present] Per-item constructTag/skill field — LogicItem.targetSkills:List<LogicSkill> (LogicModels.kt:158); DemoItem.primaryTrait:TraitCode (LanguageDemoBank.kt:27); SeparatorItem.category:EmotionCategory
- [absent] Per-item discriminability (psychometric IRT) — Solo existe 'discriminability' como gap perceptual de dificultad en EmotionStimulus.kt:29 / EM2RecuperacionGenerator.kt:29 (EASY_DISC-DISC_SPAN*load), NO discriminacion psicometrica. Ningun item de Logic/Language/Creativity lo tiene
- [absent] Per-item culturalLoad field — grep culturalLoad/cultural_load en features/mindboost = 0 resultados
- [absent] Per-item interRater field — grep interRater/inter-rater en features/mindboost = 0 resultados; scorers son rule-based deterministas (ClosedScoringEngine, ReasoningScorer)
- [partial] Holdout / eval pool (active) — EmotionContent.kt:44 SeparatorItem.isEval reserva 12/40 para holdout, PERO EM3SeparadorGenerator.kt:27 filtra {!it.isEval} y KDoc:13 'reserved for the Ola-4 holdout probe' (Ola-4 NO implementada → eval nunca se sirve). Ningun otro modulo tiene holdout
- [absent] Per-item exposure tracking — grep exposure/itemExposure en features/mindboost = 0; bancos servidos en orden fijo (Logic) o shuffle por seed (Emotions EM3) sin contador de exposicion
- [absent] Empirical validation / calibration of items — 'calibrated' en KDocs (EvidenceLockBank.kt:1, SourceTriageBank) es claim de autor, no validacion empirica. No hay datos de pilotaje, IRT params, ni inter-rater. Focus/Memory ProbeBank son para baseline (3 y 4 probes) no entrenamiento

### COPY/CLAIMS safety and terminology scanners across MindBoost modules
**Hay un copy-safety-scanner real, pero NO global.** Existen DOS gates de terminología prohibida, cada uno acotado por carpeta de módulo:

1. **`arenaTerminologyScanner`** (+ variantes CommonTest/DesktopTest) en `shared/build.gradle.kts:198-272`. Bloquea lexemas de ranking competitivo / clínico / **apuesta**: `ELO/Elo/elo, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out, gana dinero, cerebro élite, debilidad mental, inteligencia general` (`:206`). Solo corre sobre `features/arena/**` (commonMain + ambos test dirs). Match whole-word case-insensitive, **cruza guiones** (`\b` trata `-` como borde).
2. **`emotionsTerminologyScanner`** (+ test variants) en `:278-355`. Bloquea léxico **clínico**: `diagnostico, terapia, tratamiento, cura, depresion, ansiedad, trauma, trastorno, patologia, enfermedad mental, crisis, panico, tdah, toc, bipolar, estres, suicidio, autolesion` (`:296-301`). Folding de acentos español + whole-word + cruza guiones. Solo `features/mindboost/emotions/**`. `requireDir=true` en commonMain (un typo no puede desactivar el gate).

Ambos están enganchados a `check`. **Contrapartes en código** (puras, mismas reglas): `arena/config/TerminologyScanner.kt` y `emotions/domain/safety/EmotionTerminologyScanner.kt`.

**Cobertura cero** para focus, memory, speed, logic, **decisiones**, language, creativity y el global `AppStrings.kt`. Su honestidad es por convención/tests, no por gate de lexemas.

**Copy de riesgo HOY: prácticamente ninguno.** El copy real es defensivo/disclaimers ("no es un test de inteligencia", "NO demuestra mejora de inteligencia ni de la atención en la vida diaria", "Entrenamiento funcional"). El módulo "Decisiones"/"Bayes Arena" NO usa léxico de apuesta (sin fichas/mercado/payoff/apuesta — 0 matches); usa estimación probabilística honesta. No encontré claims de far-transfer afirmativos (vida real / multitarea / fake news / regula tus emociones) en strings de usuario.

**TransferLabel** (`emotions/domain/catalog/EmotionCatalogIds.kt:41`): `enum { TASK_ONLY, PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }` — mecanismo de honestidad (1 por ejercicio, pinneado por test), renderizado siempre junto al feedback de 4 partes (`FourPartFeedback.kt`). **macroCategory** es de Velocidad (`SpeedMacroCategory`, clasificación de patrón RT), no de transfer-claims.
- [present] Gate de terminología prohibida para módulo Arena (ELO/IQ/clinical/diagnostic + léxico de apuesta gambling/stake/bet/casino/bankroll/odds/cash-out) — shared/build.gradle.kts:206 (lista), :238-272 (tasks + check). Counterpart: shared/src/commonMain/kotlin/com/mindos/app/features/arena/config/TerminologyScanner.kt
- [present] Gate de terminología clínica para módulo Emociones (diagnostico/terapia/depresion/ansiedad/tdah/etc.) con folding de acentos — shared/build.gradle.kts:296-301 (lista), :328-355 (tasks). Counterpart: shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/safety/EmotionTerminologyScanner.kt:21-45
- [absent] Copy-safety-scanner GLOBAL para TODOS los módulos mindboost (focus/memory/speed/logic/decisiones/language/creativity) y AppStrings — shared/build.gradle.kts: solo registra tasks arena* y emotions*; sourceDir acotado a features/arena y features/mindboost/emotions (:241,:256,:262,:331,:338,:345). Resto de módulos sin gate.
- [absent] Léxico de apuesta (fichas/mercado/payoff) en el módulo Decisiones / Bayes Arena — Grep apuesta|ficha|bote|mercado|payoff|cuota sobre features/mindboost/decisiones/**: No matches. Copy real probabilístico: BayesArenaScreen.kt:97-98,140,174,178
- [absent] Copy afirmativo de far-transfer (inteligencia, vida real, multitarea, fake news, regular emociones) en strings de usuario — Hits de 'inteligencia'/'vida diaria'/'vida real' son disclaimers negativos: focus/domain/reporting/TransferReport.kt:54, focus/.../FocusHomeScreen.kt:204, MindBoostHubScreen.kt:201, creativity/.../CreativityStrings.kt:12
- [present] TransferLabel como mecanismo de honestidad de transfer-claim (emociones) — emotions/domain/catalog/EmotionCatalogIds.kt:41 (enum TASK_ONLY/PLAUSIBLE/NOT_DEMONSTRATED/PLACEBO_RISK); render obligatorio en emotions/domain/feedback/FourPartFeedback.kt:5-21
- [absent] macroCategory ligado a transfer-claims — macroCategory es SpeedMacroCategory (clasificación de patrón RT: FAST_IMPULSIVE/SLOW_PRECISE/VARIABLE/BALANCED/CALIBRATING) en speed/domain/score/SpeedScoreEngine.kt:11,111-119; no es transfer
- [partial] Honestidad de claims por nivel de evidencia en Lenguaje (ClaimEngine) — language/domain/claim/ClaimEngine.kt:31 (ROBUST_GENERALIZATION → 'evidencia fuerte de transferencia amplia') y DefaultClaimEngine.kt:40; es runtime claim-leveling, NO un gate de lexemas prohibidos en build

### Cobertura de tests del codigo real de MindBoost (commonTest/desktopTest) — generadores deterministas, scorers, classifiers, metrics engines, regresiones, y presencia/ausencia de tests adversariales/anti-gaming/property/jank/timebase/golden
El codigo real de MindBoost tiene **alta cobertura de unidad/integracion deterministica** por modulo (focus ~61, memory ~49, language ~25, speed ~18, emotions ~19, creativity ~13, decisiones ~7, logic ~7; ~210 archivos de test mindboost). Lo bien cubierto: **generadores deterministas** (mismo seed→misma secuencia, seeds distintos→secuencias distintas) en focus E1–E13, speed prompt generators; **scorers** (ReasoningScorer, ClosedScoringEngine, SpeedScoreEngine, FocusScoreCalculator); **classifiers** (TrialClassifier go/no-go/choice/stop); **metrics engines** con **regresion golden** real (`MetricsEngineRegressionTest` bloquea cambios de output) y **parity golden** (`EmotionRtStatisticsParityTest`); **edge-cases** numericos (0 trials, 1 acierto, todas anticipaciones); **simulacion sintetica** por perfil de usuario en speed (impulsive/slow-precise/fatigable) que ejercita el bucle staircase. **Anti-gaming PARCIAL pero real**: `SsrtCalculator.flagsGaming` (focus), `E4FrenoGenerator.ssrtMetric_flagsGaming`, `DecisionSessionOrchestrator`/AlphaCore AGRS gamingScore (decisiones), y el modulo **arena** tiene una suite adversarial dedicada (`RedTeamAdversarialTest`, boosting/collusion/sandbagging/queue-sniping). **timebase**: `TimebasePolicyTest` cubre precision nanos/millis, RT, riesgo de timing. **jank**: solo data-fed (`SessionQualityEvaluator` con frameDrop/jitter como entradas), NO mide jank/frames reales. \n\n**Faltan por completo**: (1) **property-based testing** (Kotest `checkAll`/`forAll`/`Arb`) en mindboost — cero; (2) **fuzz/monkey** de inputs UI; (3) **golden de scoring end-to-end** (existe golden de metrics engine y de stats robustas, pero no un golden lock del score final por modulo en speed/logic/language/memory); (4) **adversarial/anti-gaming sistematico** en speed/logic/language/memory/emotions/creativity (solo focus+decisiones lo tocan); (5) **jank/timebase real con dispositivo** (todo es sin-device por diseno KMP).
- [present] Generadores deterministas (seed→secuencia) — shared/src/commonTest/.../focus/domain/exercise/E1VigiaGeneratorTest.kt:18-29 (generation_isDeterministicForSameSeed, differentSeeds_produceDifferentSequences); + E2/E3/E4/E5 + AdvancedExerciseGeneratorsTest; speed SpeedPromptGeneratorsTest.kt
- [present] Scorers cubiertos — logic/domain/scoring/ReasoningScorerTest.kt; language/domain/scoring/ClosedScoringEngineTest.kt; speed/domain/score/SpeedScoreEngineTest.kt; focus/domain/reporting/DefaultFocusScoreCalculatorTest.kt
- [present] Classifiers (trial outcome) — focus/domain/exercise/TrialClassifierTest.kt; focus/domain/session/FocusSessionAssemblerChoiceTest.kt y FocusStopSignalAssemblerTest.kt
- [present] Metrics engine + regresion — focus/domain/metrics/DefaultMetricsEngineTest.kt + MetricsEngineRegressionTest.kt:11 ('Snapshot regression: locks the engine output for a fixed dataset'); ParadigmMetricsTest, SsrtCalculatorTest, speed MetricCalculatorTest + MetricEdgeCasePolicyTest
- [partial] Golden de scoring / parity golden — emotions/domain/metrics/EmotionRtStatisticsParityTest.kt:10 ('Golden-value parity for robust statistics'); focus MetricsEngineRegressionTest (golden de metrics). NO hay golden del score final por modulo en speed/logic/language/memory
- [partial] Tests adversariales / anti-gaming — focus/domain/metrics/SsrtCalculatorTest.kt:30 (flagsGaming_whenProbabilityFarFromHalf); focus/.../E4FrenoGeneratorTest.kt:61 (ssrtMetric_flagsGaming_whenStaircaseBrokeDown); core/tests/AlphaCoreTest.kt:213 (agrsFlagsRealGamingPattern) + DecisionSessionOrchestratorTest gamingScore; arena/RedTeamAdversarialTest.kt:33 (suite dedicada, fuera de cognitive-training). AUSENTE en speed/logic/language/memory/emotions/creativity como suite sistematica
- [absent] Property-based testing (Arb/checkAll/forAll) — grep checkAll|forAll|io.kotest.property|Arb. en mindboost: 0 hits (los 2 hits totales del repo son social/profile, no mindboost)
- [partial] Jank / frame-drop real — speed/domain/quality/SessionQualityEvaluatorTest.kt:31-37 (frameDropOverThreshold_isContaminated) — frameDrop/jitter son ENTRADAS QualitySignals, no medicion real de frames/jank
- [present] Timebase policy — speed/domain/TimebasePolicyTest.kt:11-90 (precision nanos/millis por API, RT, deviceTimingRisk, choreographer source) — sin dispositivo
- [present] Simulacion sintetica end-to-end (perfiles de usuario) — speed/simulation/Mvp0SimulationTests.kt:42-122 (impulsive/slow-precise/fatigable + edge cases + persistencia); StaircaseSimulationTests.kt:19; Mvp3ScoringSimulationTests.kt
- [absent] Fuzz / monkey / random-input UI — grep fuzz|monkey|random.?input en mindboost test: 0 hits relevantes

---


## Timing / Timebase infrastructure across MindBoost modules (KMP)

## Mapa de timebase REAL hoy (con evidencia)

### 1. NO existe una timebase autoritativa unica compartida
Hay **tres niveles** de fidelidad, fragmentados por modulo:

**(A) Autoritativa frame-accurate — solo speed/Android.**
- `TimebasePolicy` (`shared/src/commonMain/.../speed/domain/TimebasePolicy.kt`) es el contrato puro: base unica `ANDROID_UI_UPTIME_NANOS`, `rtNanos = responseUptimeNanos - stimulusUptimeNanos` (:54), resolucion de precision NANOS_NATIVE (API>=34) vs MILLIS_DERIVED (:31-36), y prohibicion explicita de `elapsedRealtimeNanos()` para RT (:11-14).
- El `actual` real: `SpeedTimebase.android.kt` — estimulo via `Choreographer.getInstance().postFrameCallback { frameTimeNanos }` (:41) y respuesta via `MotionEvent.eventTimeNanos` en API 34+ o `eventTime*1e6` por debajo (:59-69), delegando aritmetica/clasificacion a `TimebasePolicy`. `nowMonotonicNanos()=System.nanoTime()` solo para duraciones (:38).
- Desktop e iOS de speed **delegan en fallback** (`SpeedTimebase.desktop.kt:14`, `SpeedTimebase.ios.kt:14`).

**(B) Fallback monotonico NO-autoritativo — desktop/iOS de speed.**
- `SpeedTimebaseFallback.kt`: `FallbackSpeedTimebaseHandle` usa `TimeSource.Monotonic.markNow()` (:27), se auto-marca `MILLIS_DERIVED` (:29) y `SYSTEM_CLOCK_MONOTONIC` (:30); `requestStimulusFrame` entrega `nowMonotonicNanos()` de inmediato (:37) — sin vsync. El tap usa `detectTapGestures` + `handle.nowMonotonicNanos()` (:53), no el timestamp del MotionEvent.

**(C) Monotonico de corrutina (no frame) — focus, emotions, creativity.**
- Focus: `MonotonicClock` (`focus/presentation/exercise/MonotonicClock.kt`) sobre `TimeSource.Monotonic`, commonMain, sin expect/actual. `FocusFixedBlockRunner.kt:83` toma `onsetNanos = monotonicClock.nowNanos()` en el hilo de la corrutina (no el frame de presentacion del estimulo) y `latencyMs = (response - onset)` (:86). Honesto (delta monotonico, evita wall-clock — comentado como "risk A1") pero el onset NO es el vsync en que el pixel se mostro.
- Emotions: usa el mismo patron `MonotonicClock.nowNanos()` para onset/response del trial (`EmotionSessionStateHolder.kt:201,126`), pero `EpochClock.nowMillis()` para timestamps de sesion/safety (:140,173,338).
- Creativity: `MonotonicMillis` (`CreativityMonotonicClock.kt:14`) — millis monotonicos como base anti-cheat, no frame.

### 2. RT sobre contador NO autoritativo (wall-clock epoch)
- **Logic**: mide `timeSpentMs = epochClock.nowMillis() - itemStartedAt` (`LogicStateHolder.kt:105,113,164`). `EpochClock` = `Clock.System` epoch-millis (`core/time/EpochClock.kt:31-33`). Es wall-clock, sujeto a saltos de reloj y NUNCA alineado a frame. Adecuado para "tiempo de razonamiento" (segundos), inadecuado para RT fino.
- **Decisiones**: usa `EpochClock` para timing de check-in/triage (no RT perceptivo fino; es deliberacion).
- **Language / Memory**: no se encontro captura de RT por reloj de input. `language/domain/scoring/*` puntua correccion/estructura, no latencia. Memory puntua recall/precision, sin cronometro de input monotonico.

### 3. Gate `device_unreliable`: modelo SI, medicion NO
- El modelo de riesgo existe y esta testeado: `TimebasePolicy.deviceTimingRisk(precision, criticalFrameJitter)` (:66-73) -> HIGH si jitter critico, LOW si nanos nativos, MODERATE si millis-derived. `SessionQualityEvaluator.evaluate()` (`speed/domain/quality/SessionQualityEvaluator.kt:46-69`) combina esto con CV/anticipacion/omision/frame-drop para emitir CONTAMINATED/LOW/HIGH/ACCEPTABLE con razones auditables.
- **PERO la entrada de jank esta cableada a constantes**: `SpeedSessionStateHolder.kt:259-260` pasa `frameDropFraction = 0.0` y `criticalFrameJitter = false` literalmente. Es decir, el gate JAMAS marcara un dispositivo como contaminado por jitter en produccion: la rama HIGH-risk / CONTAMINATED-por-jitter es codigo muerto hoy. No hay ningun colector de `frameTimeNanos` consecutivos que calcule drops/jitter, ni en speed ni en focus ni en ningun otro modulo.

### 4. iOS
- No hay `CADisplayLink` en codigo Kotlin/Swift (grep solo arroja menciones en docs de diseno). Speed-iOS = fallback monotonico (`SpeedTimebase.ios.kt`). El resto de modulos no tienen ruta iOS de timing especifica (comparten commonMain `TimeSource.Monotonic`).

### Conclusion para el scout
- Timebase autoritativa real: SI, pero **aislada en speed/Android** (Choreographer+MotionEvent, misma base uptime-nanos, prohibicion de elapsedRealtime). 
- Focus mide honesto-monotonico pero **a nivel de corrutina, no de frame**; emotions/creativity igual.
- logic/decisiones miden sobre **wall-clock epoch (no autoritativo)**; apropiado para sus escalas (deliberacion), no para RT perceptivo.
- language/memory **no cronometran RT fisico**.
- Deteccion de jank/frame-drop: **inexistente como medicion**; solo el esqueleto de clasificacion existe y recibe ceros hardcodeados, dejando el gate de fiabilidad de dispositivo efectivamente inoperante.

---

## MindBoost scoring &amp; estado (Focus/Speed/Logic/Decisiones/Memory + perfil global)

# Scout SCORING y ESTADO — codigo real MindOS

## 1. No hay un `ScoreResult` comun
Cada modulo define su propio tipo de salida de scoring. No existe interfaz/supertipo compartido para "un score":
- **Focus** → `FocusScore` (`focus/domain/reporting/FocusScore.kt`)
- **Speed** → `CognitiveProfileScore` (`speed/domain/score/SpeedScoreEngine.kt`)
- **Logic** → `ScoringResult`/`ReasoningScore` (`logic/domain/scoring/ReasoningScorer.kt`)
- **Decisiones** → `Double` crudo 0–100 por ecuacion (`com.mindverse.mindboost.core.scoring.ScoringEquations`)
- **Perfil global** → `DimensionState`/`MindBoostProfile` (`profile/domain/model/MindBoostProfile.kt`)

El UNICO contrato cross-module es `ModuleContribution` (DTO one-way de agregacion, no de calculo).

## 2. Manejo de null vs 0 (HOY)
La regla "null ≠ 0, nunca 0 silencioso" se aplica de forma consistente, con 4 mecanismos:

| Mecanismo | Donde | Evidencia |
|---|---|---|
| `Double?` nullable | FocusScore.value/bounds; CognitiveProfileScore.usefulSpeed/control/stability/total; DimensionState.value; ModuleContribution.normalizedScore | barra/valor null = "no computable" |
| `confident: Boolean` + `MetricValue.unavailable()` | `MetricKey.kt:57-69`; `DefaultMetricsEngine` | `valueOf()` devuelve default SOLO si `!confident`; `rawValueOf()` lo ignora |
| Sealed gated result | `RciResult.NotEnoughData`, `PlateauStatus`, `SsrtResult.NotComputable` | gating tipado explicito |
| Invariante de estado | `DimensionState.kt:35` `require((CALIBRATING)==(value==null))` | imposible CALIBRATING con value≠null |

**Excepcion notable:** `ScoringEquations` (Decisiones) usa `0.0` como sentinela legitimo, no como null. Ej.: `calculatePHD` devuelve `0.0` si `agrsRolling>0.75` (sesion invalida por gaming), `calculateIED` devuelve `0.0` si respuesta impulsiva (`<500ms`). Aqui 0 SIGNIFICA algo (penalizacion/invalidacion), no "sin datos".

## 3. Estados de score que existen
- **Focus:** binario `confident` + banda [lowerBound, upperBound] que se estrecha con `maturity = minN/MATURE_N(60)`. Caption: "preliminar" / "estable" / "margen amplio".
- **Speed:** `SpeedMacroCategory.{CALIBRATING, FAST_IMPULSIVE, SLOW_PRECISE, VARIABLE, BALANCED}` + `preliminary=true` SIEMPRE (gate fisico §15 abierto) + `ceilingPenaltyApplied`.
- **Memory:** `RciResult.{NotEnoughData|Value(reliable)}`, `ImprovementPattern.{GENUINE_IMPROVEMENT, PRACTICE_EFFECT, STAGNATION, EXCEPTIONAL_TRANSFER, POOR_CONSOLIDATION}`, `PlateauStatus.{None|Plateau|FalseImprovement}`.
- **Perfil global:** `DimensionStatus.{CALIBRATING|EMERGING|ESTABLISHED}` (maquina de madurez). `OverallSummary.compositeIndex` null hasta ≥3 ESTABLISHED.

## 4. min-n enforced por metrica
SI, en multiples capas:
- `DefaultMetricsEngine` companion (l.461-467): `MIN_N_ACCURACY=5, MIN_N_RT=5, MIN_N_POST_ERROR=3, MIN_N_SLOPE=6, MIN_N_CONGRUENCY=3`. Cada `MetricValue.confident = n >= minN && value.isFinite()` (helper `metric()` l.145-157).
- `DefaultFocusScoreCalculator`: `confident = components.size >= MIN_COMPONENTS(2) && minN >= MIN_N(20)`.
- `SsrtCalculator`: `trustworthy` ademas exige `pRespond in 0.25..0.75`; el engine combina `confident = result.trustworthy && goRts.size >= MIN_N_RT`.
- Memory: gate por fiabilidad (`ReliabilityEstimate`, ≥2-3 administraciones de probe) en vez de n directo.
- Aggregator: `minSufficiency=0.5`, `minConfidence=0.4`, `establishedConfidence=0.7`, `minEstablishedForComposite=3`.

## 5. Agregacion inter-sesion
SI, en 3 niveles distintos:
1. **Intra-sesion (bloque→sesion):** `DefaultMetricsEngine.sessionMetrics` (l.117-141) — promedio ponderado por nº de trials de ACCURACY/RT_MEDIAN/RT_CV (solo metricas `confident`, via `weightedAverage` l.431-442), suma de LAPSES, y `FATIGUE_INDEX` (tendencia RT-CV primer-tercio vs ultimo-tercio).
2. **Pre/post (sesion vs baseline):** `EvaluationComparator.compare` (`EvaluationResult.kt:37-54`) genera `MetricChange` por metrica con RCI; `hasReliableChange` true solo si algun RCI `reliable`. Solo compara metricas con baseline + retest confident + ReliabilityEstimate.
3. **Cross-module (perfil global):** `MindBoostProfileAggregator.aggregate` agrupa `ModuleContribution` por dimension, filtra las que pasan gating, y pondera `value = Σ(score·confidence·dataSufficiency)/Σ(confidence·dataSufficiency)`. `compositeIndex` = media de dimensiones ESTABLISHED (≥3).

Decisiones (`ScoringEquations`) usa medias "rolling" (icdRolling, ceRolling, etc.) como entrada a `calculatePHD`, lo que implica agregacion inter-sesion aguas arriba, aunque la ecuacion en si es puntual.

## Archivos clave
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/reporting/FocusScore.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/MetricKey.kt` (MetricValue)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/SsrtCalculator.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/evaluation/{EvaluationResult,ReliableChangeIndex}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/score/SpeedScoreEngine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/scoring/ReasoningScorer.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/scoring/ScoringEquations.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/evaluation/{ReliableChangeIndex,RealImprovementCriterion}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/antiplateau/PlateauDetector.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/MindBoostProfileAggregator.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/{MindBoostProfile,ModuleContribution}.kt`

## Observacion para una posible unificacion
Existen DOS convenciones de "preliminar/no-calibrado" en paralelo: `preliminary: Boolean` (Speed CognitiveProfileScore, ModuleContribution, DimensionState) vs `confident: Boolean` (Focus MetricValue/FocusScore). Y DOS escalas de score: 0..1 (Speed, perfil, ModuleContribution) vs 0..100 (Focus indices, ICR, ScoringEquations). Cualquier `ScoreResult` comun futuro tendria que reconciliar ambas convenciones de bandera y ambas escalas.

---

## Anti-gaming / trial-quality / integrity detection across MindBoost (Focus, Speed, Decisiones, Creativity, Emotions) and Arena

## Detalle por categoría (todo verificado en código fuente real)

### 1. Filtro de anticipación — IMPLEMENTADO + TESTEADO (real)
- **Focus**: `TrialClassifier.classify` y `classifyChoice` clasifican como `ANTICIPATION` cualquier respuesta con `responseLatencyMs < anticipationFloorMs` (TrialClassifier.kt:33,72). El stop-signal (`classifyStop`:93) conserva el go-RT del fallo para el SSRT. Tests: `TrialClassifierTest.kt:42` (responseBelowFloor_isAnticipation_regardlessOfTarget).
- **Speed**: `MetricCalculator.computeCore` separa `anticipations` y las excluye de PC/RT; `anticipationRate = anticipations/total` (MetricCalculator.kt:130,147). Umbral 100ms documentado (SyntheticUserRunner.kt:180). Tests `MetricCalculatorTest.kt:48,95`, `TrialEngineTest.kt:35`.

### 2. Guard RT<400ms / AGRS — IMPLEMENTADO + TESTEADO (real, Decisiones)
`ScoringEquations.calculateAGRS(reactionTimeMs, spatialEntropy, isDialDefault)` (ScoringEquations.kt:199): `rtFactor = if (rt < AGRS_FAST_RESPONSE_MS=400) 1.0 else 0.0`, pesos 0.4/0.3/0.3. AGRS rolling > 0.75 → sesión inválida (score 0.0, ScoringEquations.kt:41) y deload (DifficultyAdapter.kt:12, AGRS_DELOAD_THRESHOLD=0.75) y coach T02 (CoachEngine.kt:120). El orchestrator agrega AGRS por ronda → `gamingScore` (DecisionSessionOrchestrator.kt:155,204) que rebaja el eje Integridad (DecisionResultStateHolder.kt:78). Tests: `DecisionSessionOrchestratorTest.kt:107,186`, `DecisionResultStateHolderTest.kt:59` (highGamingZeroesIntegrity).
**Caveat brutal:** el término `entropyFactor` del AGRS (peso 0.3) está MUERTO en producción: los dos holders reales pasan `spatialEntropy=1.0` fijo (BayesArenaStateHolder.kt:71, ReversibilityTriageStateHolder.kt:73), así que AGRS real solo depende de RT<400 y dial-default. El componente espacial existe en la fórmula pero nunca se alimenta.

### 3. Waiting-strategy / inflación go-RT en stop-signal — IMPLEMENTADO + TESTEADO (real)
`SsrtCalculator.compute` (SsrtCalculator.kt:39): método de integración Verbruggen 2019; marca `trustworthy=false` cuando `p(respond|signal)` sale de [0.25,0.75] — exactamente el patrón de "el usuario ralentiza estratégicamente para acertar todos los stops", que rompe el staircase. Comentario explícito de "gaming" (SsrtCalculator.kt:6-12). El `StopSignalTracker` (E4FrenoGenerator.kt:41) ajusta el SSD online (1-up/1-down). Tests reales del gaming: `SimulatorLoopTest.kt:94 gamerOnStopSignal_breaksSsrtEstimate`, `E4FrenoGeneratorTest.kt:61 ssrtMetric_flagsGaming_whenStaircaseBrokeDown`, `FocusStopSignalAssemblerTest.kt:106`.

### 4. Impulsividad / fast-commission — IMPLEMENTADO + TESTEADO (real)
`AdaptiveEngine` contiene impulsividad: `fastErrors = recentTrials.count { FALSE_ALARM && RT < impulsiveRtFloorMs(250) }`, dispara contención si `fastErrors>=2 && falseAlarms>=2` (AdaptiveEngine.kt:52,116-121). Speed: `impulsivityIndexNoNoGo(anticipationRate, fastErrorShare)` (MetricCalculator.kt:152). Tests `DefaultAdaptiveEngineTest.kt:80`, `MetricCalculatorTest.kt:85`.

### 5. Gate de calidad — IMPLEMENTADO + TESTEADO (real, Speed)
`SessionQualityEvaluator` (SessionQualityEvaluator.kt:46): precedencia CONTAMINATED>LOW; señales HIGH_ANTICIPATION_RATE, HIGH_CV, HIGH_OMISSION_RATE, FRAME_JITTER_DETECTED con razones auditables. CONTAMINATED se excluye de adaptación/perfil. Test `SessionQualityEvaluatorTest.kt:51`.

### 6. Cadencia/ritmo de tap — SOLO SIMULACIÓN ARENA (no en ejercicios reales)
`BoostingDetector` usa `inputCadenceMismatch >= 0.60 && deviceIdChanged` (FraudSignalDetectors.kt:22-24). `TrialCadence` impone cooldowns por tier (AscensionTrials.kt:108). PERO el header del archivo dice "pure predicate over synthetic features. No clock, randomness, or I/O" — son features SINTÉTICAS de un simulador offline (`IntegritySignalFeatures`). Ningún ejercicio MindBoost real (tap rítmico, cadencia de toques) computa esto desde la telemetría del usuario.

### 7. Respuesta por posición/longitud — NO EXISTE
No hay detector de "siempre toca izquierda" / "elige la opción más larga". El único gancho (entropyFactor del AGRS) está deshabilitado con `spatialEntropy=1.0` fijo y comentarios "no spatial grid → no spatial-gaming signal" (BayesArenaStateHolder.kt:71, ReversibilityTriageStateHolder.kt:73). Búsqueda de positionBias/lengthBias/alwaysLeft: 0 resultados en código de producción.

### 8. Keyword-stuffing — NO EXISTE (bloqueado por diseño)
`CreativityResponseValidator.validate` (CreativityResponseValidator.kt:18-26) solo: timing monotónico (anti-cheat de reloj), respuesta no vacía, `distinctIdeaCount >= minIdeas`. Nada de keyword-stuffing, repetición, plantillas. La capa que lo haría está explícitamente BLOQUEADA: `CreativityAiLayers.kt:44 "Metric Integrity Layer boundary: would score anti-gaming signals. BLOCKED"`.

### 9. Abandono — IMPLEMENTADO (real, Emotions)
`EmotionSessionStateHolder` persiste sesión abandonada (best-effort) al salir por la barra de seguridad (EmotionSessionStateHolder.kt:159-163,349); `EmotionMetricKey.ABANDONMENT` cuenta NO_RESPONSE (DefaultEmotionMetricsEngine.kt:79-88); `SafetyEvent.ABANDONMENT`. DTO persiste `abandoned:Boolean` (EmotionDtos.kt:63).

### 10. Fraude multi-señal Arena — IMPLEMENTADO+TESTEADO pero SIMULACIÓN
`FraudSignalEvaluator` corre 5 detectores (smurf/boosting/accountSharing/collusion/lagSwitch) (FraudSignalDetectors.kt). Tests `FraudSignalEvaluatorTest.kt:36`. Gating de rating exige `IntegrityStateEnum.VERIFIED`. Hay `DataSanitizer`/`SyntheticIdMapper`/`SafeArenaException` (arena/sanitization/). PERO todo es offline-only con HARD STOP antes de Fase 08 (sin Firebase/telemetría real que alimente las features). Es un motor correcto sin fuente de datos de producción.

## Conclusión brutal
El **anti-gaming de RT (psicométrico) es real y está testeado**: anticipación, RT<400/AGRS, stop-signal waiting-strategy, impulsividad, quality-gate, abandono — todos con tests que ejercitan el caso de trampa. El **anti-gaming conductual avanzado es diseño/placeholder**: la cadencia real de tap no se mide en ejercicios, el sesgo posicional/longitud no existe (y su gancho está deshabilitado con entropía=1.0 constante), el keyword-stuffing está deliberadamente BLOCKED, y los detectores de fraude de Arena son un simulador sin pipeline de datos reales. Aproximadamente **mitad implementado+testeado sobre datos reales, un cuarto solo simulación, un cuarto inexistente.**

---

## MindBoost item banks (Logic, Language, Decisiones, Emotions, Creativity)

## Inventario real de bancos de ítems (código de producción, commonMain)

### 1. LOGIC — 6 ítems autorados FIJOS
`shared/.../mindboost/logic/domain/content/LogicContentProvider.kt:29-36`
`sessionItems()` retorna un `listOf` de exactamente 6 ítems hardcoded (`deductionItem`, `conditionalItem`, `necessarySufficientItem`, `consistencyItem`, `counterexampleItem`, `conditionalIsomorphItem`). Se sirven **los mismos 6 en orden fijo cada sesión** (`LogicConfiguration.sessionItemCount=6`, LogicModels.kt:267). No hay shuffle, ni reposición, ni pool más grande del que muestrear.
- **Generación**: autorada fija. El KDoc (líneas 23-27) admite explícitamente: *"Initial authored pack... a future generative provider can implement the same contract"* — el provider procedural **no existe**.
- **Campos por ítem** (LogicModels.kt:155-176): `difficultyLevel` (LogicDifficulty 1-5), `targetSkills` (constructo), `transferLevel` (TRAINED/T1_ISOMORPHIC), `commonErrorTags`, `isIsomorphicProbe`. **NO**: discriminability, culturalLoad, interRater, holdout, exposure.
- **Memorizable**: SÍ, trivialmente — 6 ítems con respuesta correcta fija (`correctAnswerId`), recordable tras 1-2 pasadas.

### 2. LANGUAGE — 36 ítems autorados FIJOS (6 bancos × 6) + demo de 7
`LanguageExerciseCatalog.kt:15-20` wirea 6 bancos: EvidenceLock, SemanticSurgery, CompressionLadder, ArgumentXRay, ScopeNegation, SourceTriage. Cada `*Bank.kt` contiene exactamente **6 ítems** (verificado por conteo de `id = "..._N"`). Más `LanguageDemoBank.kt` con 7 ítems demo.
- **Generación**: autorada fija. KDoc de LanguageDemoBank.kt:34: *"Provisional offline bank... Replace with the Content OS"* y EvidenceLockBank.kt:1 *"Dense calibrated content bank"* (claim de autor, no validación).
- **Campos por ítem** (`DemoItem`, LanguageDemoBank.kt:20-32): `level` (1-3), `primaryTrait` (TraitCode), `operation` (OperationType), `bankType` (TRAINING/—), `key` con `distractorErrors` mapeados a `ErrorType`. **NO**: discriminability, culturalLoad, interRater, holdout, exposure.
- **Memorizable**: SÍ — texto fijo con `correct` y `requiredEvidence` fijos; 6 ítems por ejercicio se memorizan rápido.

### 3. DECISIONES — SIN banco autorado; 100% PROCEDURAL numérico
`BayesArenaStateHolder.generateScenarios(count, seed)` → `List(count){ BayesEngine.generateScenario(difficulty, random) }`. Igual `ReversibilityTriageStateHolder`. Las pantallas usan `remember(seed){ generateScenarios(seed=...) }` (BayesArenaScreen.kt:44-47).
- `BayesEngine.generateScenario` produce tripletas numéricas jittered (baseRate, sensitivity, falsePositiveRate, expectedPosterior) — escenarios **abstractos sin narrativa**.
- `ReversibilityEngine.generateScenario` produce cartas paramétricas (isReversible, delayCostRate, lockedDurationMs) con `description` templated *"Reversibility triage level N"*.
- **Validación**: ninguna. No memorizable como contenido (varía por seed), pero tampoco hay banco de escenarios verbales ni ítems calibrados; es matemática procedural.

### 4. EMOTIONS — bancos autorados pequeños + generadores procedurales
`EmotionContent.kt`:
- `PAUSA_WORDS` = **24 palabras** autoradas (12 negativas / 12 neutras), línea 16-41 — distractores de EM1.
- `SEPARATOR_ITEMS` = **40 frases** autoradas (10 por categoría INTERNAL_STATE/THOUGHT/IMPULSE/FACT), línea 50-95. El KDoc dice "7 training + 3 held-out eval each" y el target es 80 ("v1 ships ≥30").
- Generadores: 6 reales (EM1/2/3/8/10/11). EM2/8/10/11 son perceptuales **procedurales** (`discriminability` = gap perceptual, no IRT). Catálogo declara **15 ejercicios** (EmotionExerciseCatalog.kt) → la mayoría sin generador.
- **Holdout**: `SeparatorItem.isEval` marca 12/40 como eval, pero `EM3SeparadorGenerator.kt:27` filtra `{!it.isEval}` y reserva el eval para *"Ola-4 holdout probe"* — **Ola-4 no implementada**, así que el holdout **nunca se ejerce**. Holdout nominal, no activo.
- **Campos**: `discriminability` (solo perceptual), `isEval`. **NO**: interRater, culturalLoad, exposure.
- **Memorizable**: SÍ — 40 frases con categoría fija; 24 palabras fijas.

### 5. CREATIVITY — 14 prompts estáticos
`CreativityPromptCatalog.kt:12-30`: `ex01Suppress` (8 objetos: brick, paperclip...) + `ex02Rotation` (6 temas: "uses for water"...). KDoc: *"Static, local prompt pools... the ONLY source of prompts in the MVP (no AI generation, no network)"*. Solo 2 ejercicios (EX01/EX02). Scoring por rubrica local (`CreativityRubricScorer`), sin IA (bloqueada por charter). Sin discriminability/interRater/holdout/exposure.

## Campos psicométricos — barrido global
`grep discriminab|interRater|holdout|exposure|culturalLoad|calibrat` en features/mindboost + mindverse.core:
- `discriminability`: SOLO emotions, y es **dificultad perceptual** (EmotionStimulus.kt:29, gap entre estímulos), NO discriminación de ítem.
- `interRater`, `culturalLoad`, `exposure`: **0 resultados** en todo el árbol.
- `holdout`/`isEval`: solo emotions, reservado a Ola-4 no implementada.
- `calibrated`/`calibration`: en Logic/Language es claim verbal de autor en KDoc; en Focus es calibración de baseline del *usuario* (no de ítems).

## Validación / inter-rater / holdout existentes HOY
**Ninguna real.** No hay: pilotaje empírico, parámetros IRT estimados, jueces inter-rater, ni holdout servido. Los scorers (`ClosedScoringEngine`, `ReasoningScorer`, `CreativityRubricScorer`) son deterministas rule-based contra claves autoradas. Los `ProbeBank` de focus (3 probes) y memory (4 probes) son para evaluación de *baseline*, no bancos de entrenamiento, y tampoco están validados.

## Conclusión — bancos memorizables/pequeños/sin validar HOY
1. **Logic**: 6 ítems fijos servidos en orden fijo → **altamente memorizable**.
2. **Language**: 36 ítems fijos (6×6) + 7 demo → **altamente memorizable**.
3. **Emotions**: PAUSA(24) + SEPARATOR(40) autorados fijos → **memorizable**; holdout nominal inactivo.
4. **Creativity**: 14 prompts → **muy pequeño**.
5. **Decisiones**: procedural numérico → no memorizable como texto, pero **sin contenido autorado ni validación**.

**Todos sin discriminability psicométrica, sin inter-rater, sin culturalLoad, sin exposure, sin holdout activo, sin validación empírica.**

---

## COPY/CLAIMS safety and terminology scanners across MindBoost modules

## Scanners de terminología (los dos gates reales)

### 1. Arena — `arenaTerminologyScanner`
- Definición: `shared/build.gradle.kts:198-216` (clase `ArenaTerminologyScannerTask`).
- Lexemas bloqueados (`:206`): `ELO, Elo, elo, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out, gana dinero, cerebro élite, debilidad mental, inteligencia general`. Incluye explícitamente **léxico de apuesta** (gambling/stake/bet/casino/bankroll/odds/cash-out).
- Match: `Regex("\\b$term\\b")` (whole-word; `-` actúa como borde, así que detecta el término dentro de compuestos con guion — ver aprendizaje CLAUDE.md 2026-06-23 sobre `non-clinical`).
- Alcance: `features/arena` en commonMain (`:241`), commonTest (`:256`) y desktopTest (`:262`). Enganchado a `check` (`:265-272`).
- También existe `arenaProductionGuard` (`:218-236`/`:244`) que bloquea `Firebase.firestore`, `Firebase.functions`, `firestore.collection`, `mindos-production` (gate de "no-Firebase", no de copy).
- Contraparte pura en código: `arena/config/TerminologyScanner.kt` (lista la aporta el caller; el archivo no contiene literales prohibidos para no auto-tripear).

### 2. Emociones — `emotionsTerminologyScanner`
- Definición: `shared/build.gradle.kts:278-326`.
- Lexemas (`:296-301`): `diagnostico, diagnosticar, diagnostica, terapia, terapeutico, tratamiento, cura, curar, depresion, ansiedad, trauma, trastorno, patologia, enfermedad mental, crisis, panico, tdah, toc, bipolar, estres, suicidio, autolesion`.
- Match: fold de acentos español (á→a…ñ→n) + lowercase + `\b...\b` + cruza guiones. Mirror exacto en `emotions/domain/safety/EmotionTerminologyScanner.kt:21-45`.
- `requireDir=true` en commonMain (`:332`) → un dir mal escrito hace fallar el build (no se puede desactivar silenciosamente el gate); test dirs `requireDir=false` (checkout-safe). Enganchado a `check` (`:349-354`).

### Cobertura: NO hay scanner de copy global para mindboost
`build.gradle.kts` solo registra tasks `arena*` y `emotions*`. Los módulos **focus, memory, speed, logic, decisiones, language, creativity** y el `AppStrings.kt` global NO tienen gate de lexemas prohibidos. Su seguridad de copy descansa en convención + tests de honestidad (p.ej. `FocusSessionAssemblerTest.kt:75`, `TransferReportBuilderTest.kt:47` afirman presencia de disclaimers), no en un escaneo de build.

## TransferLabel / macroCategory
- `TransferLabel` (`emotions/domain/catalog/EmotionCatalogIds.kt:41`): enum de honestidad `{ TASK_ONLY, PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }`, exactamente 1 por definición de ejercicio (pinneado por test), y SIEMPRE renderizado junto con el feedback de 4 partes (`FourPartFeedback.kt:5-21`, doc dice "the product never promises broad transfer; this enum is the single source of that honesty"). Es el mecanismo afirmativo más limpio de calibración de transfer-claim en el repo.
- `macroCategory` NO es transfer: es `SpeedMacroCategory` (Velocidad), clasificación de patrón de RT (`SpeedScoreEngine.kt:11,111-119`): CALIBRATING/FAST_IMPULSIVE/SLOW_PRECISE/VARIABLE/BALANCED. Renderiza a `SpeedStrings.PATTERN_*` (`SpeedDashboardModel.kt:42-47`).

## Copy de far-transfer / apuesta que existe HOY
**Riesgo material: muy bajo.** El copy de usuario es defensivo:
- `focus/domain/reporting/TransferReport.kt:54`: "...NO demuestra mejora de inteligencia ni de la atención en la vida diaria." (disclaimer).
- `focus/.../FocusHomeScreen.kt:204`: "...Aquí no se promete inteligencia ni..." (disclaimer).
- `MindBoostHubScreen.kt:201`: Creatividad "Solo práctica, no es un test de inteligencia."; `:191` Emociones "Entrenamiento funcional." (no "regula tus emociones en la vida real").
- `creativity/.../CreativityStrings.kt:12`: "Solo práctica · No es un diagnóstico · No es un test de inteligencia".
- Language `ClaimEngine.kt:31`: `ROBUST_GENERALIZATION → "Hay evidencia fuerte de transferencia amplia."` — el ÚNICO claim afirmativo fuerte de transfer; está gated tras un nivel de evidencia E5 (runtime), pero NO está cubierto por ningún terminology-scanner de build. Es el candidato más sensible a revisar.

**Léxico de apuesta en Decisiones / "Bayes Arena": inexistente.** Grep `apuesta|ficha|bote|mercado|payoff|cuota` sobre `features/mindboost/decisiones/**` → 0 matches. El copy es estimación probabilística honesta: "Estima la probabilidad real y di cuánta certeza tienes. Medimos cómo actualizas con la evidencia, no si aciertas un resultado." (`BayesArenaScreen.kt:97-98`), "Probabilidad real" (`:140`), "Acierta positivos" (`:174`). El nombre "Arena" aquí NO arrastra léxico de apuesta — y nótese que `bet/stake/casino` solo se bloquean en el módulo `arena` (juego competitivo), no en `decisiones`.

## Gap notable
El léxico de apuesta (gambling/stake/bet/casino/bankroll/odds/cash-out) y el de inteligencia (`IQ`, `inteligencia general`) SOLO se bloquean en `features/arena`. El módulo `decisiones` (que conceptualmente roza apuesta/probabilidad) y `language` (que emite "transferencia amplia") quedan FUERA de cualquier gate de lexemas. Hoy su copy está limpio, pero nada en el build lo impone.

---

## Cobertura de tests del codigo real de MindBoost (commonTest/desktopTest) — generadores deterministas, scorers, classifiers, metrics engines, regresiones, y presencia/ausencia de tests adversariales/anti-gaming/property/jank/timebase/golden

## Inventario por modulo (archivos de test commonTest+desktopTest bajo *mindboost*)

Conteo aproximado (por subcarpeta): **focus 61, memory 49, language 25, emotions 19, speed 18, creativity 13, decisiones 7, logic 7, core 3, storage 1**. Total ~203 archivos mindboost (mas arena, que es feature competitiva separada).

## Que se testea (con evidencia)

**Generadores deterministas** — patron consistente seed→secuencia:
- `focus/domain/exercise/E1VigiaGeneratorTest.kt:18` `generation_isDeterministicForSameSeed`; `:25` `differentSeeds_produceDifferentSequences`; `:32` `producesRequestedCount_andAtLeastOneSignal`; `:38` carga perceptual. Equivalentes para E2/E3/E4/E5 (`E2FiltroGeneratorTest`, `E3GatilloGeneratorTest`, `E4FrenoGeneratorTest`, `E5ConflictoGeneratorTest`) y `AdvancedExerciseGeneratorsTest.kt`.
- `speed/domain/exercise/SpeedPromptGeneratorsTest.kt` (uno de los 2 unicos test files de mindboost que usa `Random` controlado).

**Scorers**: `ReasoningScorerTest` (ICR logic), `ClosedScoringEngineTest` (language), `SpeedScoreEngineTest`, `DefaultFocusScoreCalculatorTest`.

**Classifiers**: `focus/domain/exercise/TrialClassifierTest.kt`; ensambladores por paradigma `FocusSessionAssemblerChoiceTest`, `FocusStopSignalAssemblerTest`, `FocusTaskSwitchTest`, `FocusAdvancedParadigmsAssemblerTest`.

**Metrics engine**: `focus/domain/metrics/DefaultMetricsEngineTest.kt` + **`MetricsEngineRegressionTest.kt:11`** que explicitamente *"locks the engine's output for a fixed dataset so any change..."* — esto es un **golden/snapshot regression real** del motor de metricas. Tambien `ParadigmMetricsTest`, `SsrtCalculatorTest`, `SsrtCalculator`. En speed: `MetricCalculatorTest` + `MetricEdgeCasePolicyTest`.

**Regresiones de dominio**: `logic/domain/adaptive/LogicDifficultyAdapterTest` (regresion impulsividad, ver CLAUDE.md 2026-06-15), `memory/domain/antiplateau/PlateauDetectorTest.kt:83` `goldenRule_overridesWithGoalReplanning` (golden *rule*, no golden de valor).

**Edge cases numericos** (muy completos en speed): `Mvp0SimulationTests.kt:79-122` — 0 trials (bloque descartado), 0 aciertos (pc=0/rt=null), 1 acierto (cv=null), todas-anticipaciones tratadas como 0 correctas.

**Simulacion sintetica end-to-end**: `speed/simulation/Mvp0SimulationTests.kt` corre un `SyntheticUserRunner` con perfiles (`IMPULSIVE`, `SLOW_PRECISE`, `FATIGABLE`) y verifica metricas+calidad+persistencia round-trip; `StaircaseSimulationTests.kt:19` alimenta `BlockMetrics` sinteticas al motor staircase y verifica reaccion de dificultad; `Mvp3ScoringSimulationTests.kt`. Es lo mas cercano a "stress" que tiene el codigo cognitivo real, pero es **deterministico por seed fijo**, no property-based.

## Anti-gaming / adversarial (PARCIAL)

Existe, pero disperso y solo en 2 modulos cognitivos + arena:
- **focus**: `SsrtCalculatorTest.kt:30` `flagsGaming_whenProbabilityFarFromHalf` (detecta staircase breakdown / strategic slowing); `E4FrenoGeneratorTest.kt:61` `ssrtMetric_flagsGaming_whenStaircaseBrokeDown`.
- **decisiones/core**: `AlphaCoreTest.kt:105` `phdInvalidatesWhenGamingRiskIsHigh`, `:202` `agrsDoesNotFlagAnalyticalControlAsGaming`, `:213` `agrsFlagsRealGamingPattern`; `DecisionSessionOrchestratorTest.kt:186` `assembleSessionSurfacesGamingCoachMessageWhenAgrsIsHigh`.
- **arena** (feature competitiva, no cognitive-training): suite adversarial *dedicada* `desktopTest/.../arena/RedTeamAdversarialTest.kt:33` con casos rt02..rt27 (boosting→HOLD, collusion/win-trading→INVALIDATED, queue-sniping, sandbagging/one-block→elite bloqueado, appeal-abuse rate-limit, rollback-abuse). Tambien `IntegrationValidationTest.kt:116` user-copy-safe/no-anti-cheat-reveal y `UserSafeExplanationMapperTest` (no fuga de thresholds/anti-cheat).

## timebase / jank

- **timebase**: `speed/domain/TimebasePolicyTest.kt` cubre precision NANOS_NATIVE/MILLIS_DERIVED por API, resolveResponseUptimeNanos con fallback, computeRtNanos, isValidRt(negativo invalido), `deviceTimingRisk` (LOW/MODERATE/HIGH) y `TRIAL_TIMESTAMP_SOURCE == CHOREOGRAPHER_FRAME`. Solido **pero sin dispositivo**: valida la *politica*, no la captura real.
- **jank**: `speed/domain/quality/SessionQualityEvaluatorTest.kt:31` `frameDropOverThreshold_isContaminatedWithReason` — pero `frameDrop`/`jitter` son **campos de entrada de `QualitySignals`** (linea 24-26), construidos en el test. No hay test que mida jank/frames reales ni que verifique la captura Choreographer en androidMain.

## Que FALTA por completo

1. **Property-based testing**: `grep checkAll|forAll|io.kotest.property|Arb\.` sobre tests mindboost = **0 hits**. Los unicos 2 hits del repo entero estan en `social/.../ProfileStateHolderSaveTest.kt` y `speed/.../SpeedPromptGeneratorsTest.kt` (este ultimo NO usa Kotest property — es match incidental con `Random`). No hay invariantes verificadas sobre rangos de entrada generados (ej: "para todo RT valido, score in [0,100]", "para toda secuencia de trials, metricas monotonas").
2. **Adversarial/anti-gaming sistematico** en **speed, logic, language, memory, emotions, creativity**: no existe una suite tipo "el usuario intenta inflar score" (tap-spamming, respuestas alternadas, latencia minima constante, repeticion de patrones). Solo focus (SSRT) y decisiones (AGRS) tienen senales anti-gaming testeadas; arena las tiene como feature aparte.
3. **Golden de scoring final por modulo**: hay golden del *metrics engine* de focus (`MetricsEngineRegressionTest`) y parity golden de stats robustas en emotions, pero **no** un lock del score/perfil final de cada modulo (speed score, logic ICR, language genome, memory) que rompa el build ante drift de scoring.
4. **Fuzz / monkey / random-input UI**: 0. Ningun test lanza inputs aleatorios/malformados contra los state holders de sesion.
5. **jank/timebase real con dispositivo**: por diseno KMP todo es commonTest/desktopTest sin device; no hay instrumented test (androidInstrumentedTest) que valide Choreographer/MotionEvent ni frame pacing real — esto esta documentado como gate fisico abierto (PHYSICAL_VALIDATION_GATE).

## Conclusion
La cobertura real es **fuerte en correccion deterministica** (generadores, scorers, classifiers, metrics + 1 golden de engine + simulacion sintetica por perfiles + edge cases numericos). Los **tipos de test que faltan casi por completo** son: **property-based** (cero), **anti-gaming sistematico** fuera de focus/decisiones/arena, **golden de scoring end-to-end** por modulo, **fuzz/monkey UI**, y **jank/timebase real on-device** (diferido a gate fisico).

---

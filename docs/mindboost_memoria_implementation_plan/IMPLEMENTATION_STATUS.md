# MindBoost · Memoria — Estado de Implementación

Seguimiento por fases del plan en [`00_master_plan.md`](00_master_plan.md).

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Andamiaje + decisiones de alcance | ✅ Decisiones V1 confirmadas (M1+M3+M4; rostros procedurales; TTS+texto) |
| **1** | **Modelo de dominio** (`domain/model/`, `domain/metrics/MemoryMetricKey`) | ✅ **Completa** |
| **2** | **Catálogo de 16 ejercicios** (`domain/catalog/`) | ✅ **Completa** |
| **3** | **Contenido + generación procedural** (`domain/content/`) | ✅ **Completa** |
| **4** | **Generadores de trials** (`domain/exercise/`, oleada V1: EX1/EX3/EX4) | ✅ **Completa** |
| **5** | **Motor de métricas** (`domain/metrics/`) | ✅ **Completa** |
| **6** | **Motor adaptativo (CME)** (`domain/adaptive/`) | ✅ **Completa** |
| **7** | **Sesiones + recall diferido** (`domain/session/`, `domain/deferred/`) | ✅ **Completa** |
| **8** | **Evaluación + transferencia** (`domain/evaluation/`, `domain/transfer/`) | ✅ **Completa** |
| **9** | **Personalización / anti-meseta / reporting** (`personalization/`, `antiplateau/`, `reporting/`) | ✅ **Completa** |
| **10** | **Persistencia local cifrada** (`data/`) | ✅ **Completa** |
| **11** | **DI (`MemoryGraph`)** (`di/`) | ✅ **Completa** |
| **12** | **Presentación + wiring boostMind** (`presentation/`, hub MindBoost) | ✅ **Completa** |

## Fase 1 — entregado

**Producción** (`shared/src/commonMain/.../features/mindboost/memory/domain/`):
- `model/MemoryTaxonomy.kt` — enums: `MemorySubcapacity`(12), `MemoryModule`(6), `MemoryExerciseId`(16), `MemoryParadigm`(16), `Mode`(3), `MemoryTrialOutcome`(10).
- `model/MemoryValueObjects.kt` — `Proportion`, `ReactionTimeMs`, `AxisLevel`, `SerialPosition`, `ConfidenceScale`, `ConfidenceJudgment`, `SessionId`/`BlockId`/`TrialId`.
- `model/MemoryDifficultyVector.kt` — `MemoryDifficultyAxis`(8: L,I,D,S,P,R,C,K), `CmeSensitivity`(α..ε), `MemoryDifficultyVector` con fórmula CME (doc §5).
- `model/MemoryExerciseDefinition.kt` — ficha declarativa + invariantes + `PracticeEffectRisk`.
- `metrics/MemoryMetricKey.kt` — `MemoryMetricKey`(28, doc §6), `MemoryMetricUnit`, `MemoryMetricValue`, `MemoryMetricSet` + accesores honestos (sample size + confidence flag).

**Tests** (`shared/src/commonTest/.../memory/domain/model/`): 27 tests, 0 fallos en `:shared:desktopTest`.
- `MemoryValueObjectsTest`(10), `MemoryDifficultyVectorTest`(8), `MemoryTaxonomyTest`(5), `MemoryExerciseDefinitionTest`(4).

**Verificación:** `:shared:desktopTest` verde · `:shared:detekt` limpio.

## Notas de diseño aplicadas
- Backbone inmutable, 100% `commonMain`, sin Compose/Firebase/plataforma (replica el patrón de `focus`).
- CME fiel a doc §5: `loadFactor·(1+α·I)·(1+β·D)·(1+γ·S)·P_w·R_w·(1+δ·C)·(1+ε·K)`; ejes normalizados 0..1, decodificados por los generadores (Fase 4).
- Métricas honestas por construcción: cada valor lleva `n` y `confident` (principio anti-engaño doc §6/§13).
- Feature autocontenida (value objects propios, sin acoplar con `focus`).

## Fase 2 — entregado

**Producción:** `domain/catalog/MemoryExerciseCatalog.kt` — las 16 fichas declarativas (doc §4/§15) como única fuente de verdad, con accesores `all`, `definition(id)`, `byModule`, `bySubcapacity`, e `implemented = [EX1, EX3, EX4]` (oleada V1 confirmada).

**Tests:** `domain/catalog/MemoryExerciseCatalogTest.kt` — 8 tests (exhaustividad 16, ids únicos, invariantes por ficha, cobertura de los 6 módulos, mapeo módulo↔tabla maestra, `implemented`).

**Verificación:** `:shared:desktopTest` (memory) → 35 tests, 0 fallos · `:shared:detekt` limpio.

**Decisiones de mapeo documentadas** (3 ejercicios ambiguos): EX7 Secuencias → M4 (Corsi/orden serial espacial); EX9 Instrucciones → M1 (paradigma WM/ejecutivo); EX13 Reconstrucción → M6 (constructivo + metamemoria, con confianza por respuesta).

## Fase 3 — entregado

**Producción** (`domain/content/`):
- `model/Language.kt`, `model/WordItem.kt`, `model/PairItem.kt` — modelo de contenido tagueado (categoría/concretud/frecuencia/longitud; pares sin auto-par).
- `pool/WordPool.kt`, `pool/PairPool.kt` — pools inmutables, versionados, con umbral mínimo (`require`, sin cap silencioso) + `qualityReport()` (honestidad de volumen: `belowScientificIdeal`, ideal 2000/600).
- `sampling/SamplingFilters.kt`, `sampling/DeterministicSampler.kt` — muestreo sembrado determinista, exclusión anti-repetición, **fallo ruidoso** (no trunca/duplica).
- `sampling/RecentItemRegistry.kt` — registro puro copy-on-write para no-repetición inter-sesión; tiempo inyectado (sin `Clock.System`).
- `partition/TransferProbePartition.kt` — partición disjunta training/reserva (transfer-probes) validada en `init`, determinista y exhaustiva.
- `data/SpanishMemoryContentData.kt` — datos ES (142 palabras, 9 categorías; 56 pares); único archivo con literales (`@file:Suppress(MagicNumber)`).
- `MemoryContent.kt` — fachada (expone solo pools de entrenamiento; reserva excluida; scope V1 = EX1/EX3/EX4; semilla de partición fija).

**Tests:** 59 nuevos (modelo, pools, sampler, partición, registro, datos ES, fachada).

**Decisiones documentadas:** RNG `Random` sembrado NO-cripto (correcto para reproducibilidad); umbrales V1 100/40 con honestidad de volumen vía `qualityReport()`; identidad por id string (no índice); anti-engaño por construcción (la fachada nunca expone la reserva).

**Verificación:** `:shared:desktopTest` (memory) → **94 tests, 0 fallos** · `:shared:detekt` limpio. Revisión adversarial (workflow) **inconclusa por tooling** (subagentes no emitieron StructuredOutput); suplida por verificación inline de las 4 dimensiones → 0 hallazgos confirmados.

## Riesgos pendientes (Fase 3)
- **Volumen ES por debajo del ideal científico** (>2000): tras reserva 20% el pool de entrenamiento baja a ~114 palabras / ~45 pares → con no-repetición de 14 días puede entrar en repetición tras ~pocas sesiones. Expuesto por `qualityReport().belowScientificIdeal`. Mitigar ampliando el corpus ES antes de producción.
- **Calidad fina del contenido**: `associationStrength` de los pares ES se asignó por rotación (no curada); categorías autoreadas a mano pueden tener sesgo. Requiere revisión humana.
- **Revisión adversarial automatizada inconclusa**: el workflow de review falló por tooling; la verificación fue inline. Considerar re-ejecutar con configuración robusta (sin agentType custom).

## Fase 4 — entregado

**Producción** (`domain/exercise/`):
- `MemoryTrialModels.kt` — planes sellados `MemoryTrialPlan` (`ComplexSpanTrial`, `RecallListTrial`, `PairedAssociatesTrial`) + `RecallType`/`RecallDirection`/`SpanStep`/`CuedRecallProbe`, con invariantes (clave de scoring embebida).
- `MemoryTrialGenerator.kt` — interfaz determinista + `DifficultyDecode` (lerp/decodeCount/decodeMillis).
- `Ex1SpanGenerator.kt` — memoranda intercalados con tareas de procesamiento; recall forward/backward.
- `Ex3ListGenerator.kt` — lista de estudio; recall libre/cued; demora; tiempo de estudio por profundidad.
- `Ex4PairsGenerator.kt` — pares + probes uni/bidireccionales; demora; fuerza asociativa por similitud.

**Tests:** 28 nuevos (decoder, modelos, EX1, EX3, EX4): determinismo, escala por eje, tipo de recall por eje, integridad de la clave de scoring, exclusión y **no fuga de transfer-probes**.

**Decisiones documentadas:** generadores deterministas (`Random(seed)`, espejo de focus); decodifican un subconjunto de ejes relevantes en V1 (similitud/interferencia de EX3 y fan-effect de EX4 **diferidos** hasta ampliar el pool / Fase 7); `pairCount` de EX4 capado a 10 por tamaño del pool V1 (límite documentado, no cap silencioso); "similitud" de EX1 aproximada por misma-categoría (sin tags fonológicos en V1).

**Verificación:** `:shared:desktopTest` (memory) → **122 tests, 0 fallos** · `:shared:detekt` limpio.

## Riesgos pendientes (Fase 4)
- **Ejes relevantes parcialmente mapeados**: EX3 no controla aún SIMILARITY/INTERFERENCE por contenido; EX4 no controla fan-effect. Ambos atados al tamaño del pool y a la interferencia de nivel-sesión (Fase 7). Documentado en los mapeos.
- **Caps por pool pequeño** (EX4 ≤10 pares, EX1/EX3 longitudes acotadas): si la dificultad pide más de lo disponible tras exclusión, el sampler falla ruidoso — el motor adaptativo (Fase 6) deberá respetar los topes.
- **Aproximaciones de contenido** (similitud por categoría, fuerza asociativa por rotación) mejoran al ampliar/curar el pool ES.

## Fase 5 — entregado

**Producción** (`domain/metrics/`):
- `MemoryScoring.kt` — `RecalledWord`/`ScoredResponse`/`ScoredTrial` + `MemoryScorer` (scoreComplexSpan/scoreList/scorePairs): puntúa la recuperación del usuario contra la clave embebida en el `MemoryTrialPlan`, clasificando HIT/ORDER_ERROR/INTRUSION/CONJUNCTION_ERROR/OMISSION; `slotAligned` distingue omisión por slot (span/pares) de no-recall en recall libre.
- `MemoryStatistics.kt` — mediana/MAD/`rejectOutliers` (auto-contenido; central tendency filtra outliers, eventos extremos NO).
- `ArcClustering.kt` — índice ARC de Roenker (1.0 clustering perfecto, 0 azar, null indefinido).
- `MemoryMetricsEngine.kt` — interfaz + `DefaultMemoryMetricsEngine.blockMetrics`: accuracy inmediata/diferida (por demora), tasa de intrusión/omisión, RT median (MAD), span parcial (EX1), ARC (EX3 free), índice asociativo + tasa de conjunción (EX4) + helpers pairwise `consolidationIndex`/`dualTaskCost`. Métricas honestas (`n`+`confident`).

**Tests:** 20 nuevos (scoring, estadística, ARC, motor). Total memory: **142**.

**Decisiones documentadas:** alcance V1 = métricas que declaran EX1/EX3/EX4 (d-prime/criterio/K-Cowan diferidos a EX6/EX12; Brier/gamma/calibración a EX16 overlay); clave de scoring embebida en los planes; consolidación/dual-task como funciones pairwise (las consumirá la agregación de sesión, Fase 7); confianza por umbral de muestra (`MIN_N=4`, `MIN_ARC_N=5`).

**Verificación:** `:shared:desktopTest` (memory) → **142 tests, 0 fallos** · `:shared:detekt` limpio.

## Riesgos pendientes (Fase 5)
- **Cobertura de métricas parcial por alcance V1**: d-prime/criterio, K de Cowan, Brier/gamma/calibración y resolución metacognitiva NO implementados (pertenecen a EX6/EX12/EX16, fuera de la oleada). El `MemoryMetricKey` ya los enumera; se implementarán al entrar esos ejercicios.
- **Métricas pairwise sin orquestación**: `consolidationIndex`/`dualTaskCost` existen y están testeadas pero nadie las invoca aún (su cableado es Fase 7, agregación de sesión).
- **Calibración no computada**: la confianza se captura en `ScoredResponse` pero su cómputo (calibración/Brier) se difiere al overlay EX16.

## Fase 6 — entregado

**Producción** (`domain/adaptive/`):
- `MemoryAdaptiveModels.kt` — `MemoryZone`, `MemoryAdaptiveDecision` (sealed: StepUp/StepDown/Hold/WorkSpeed/ActivateMetacognition/ProposeRest/StopSession), `MemoryAdaptiveConfig` (pesos+umbrales+pasos, doc §5), `MemoryAdaptiveInput`, `MemoryFatigueSignal`, `MemoryPerformance` (fórmula §5) y `zoneOf`.
- `MemoryAdaptiveEngine.kt` — interfaz + `DefaultMemoryAdaptiveEngine`: decisión priorizada **seguridad → progresión**, ajusta **un solo eje relevante** por decisión; selección de dimensión (§5.3: diferida-lag→DELAY, intrusiones→INTERFERENCE, lento→trabajar velocidad, calibración→metacognición); anti-oscilación (histéresis + pasos asimétricos; step-down salta la histéresis como seguridad).

**Tests:** 17 nuevos (performance/zonas + 12 de decisión por zona/señal/selección de eje). Total memory: **159**.

**Decisiones documentadas:** Performance ponderado con caídas por intrusión y sesgo de confianza²; delayed→fallback a immediate y velocidad neutral (0.5) sin RT; opera en espacio normalizado 0..1 (los generadores de Fase 4 aplican los topes de pool al decodificar); `confidenceBias` entra como parámetro (0.0 hasta que EX16 lo cablee).

**Verificación:** `:shared:desktopTest` (memory) → **159 tests, 0 fallos** · `:shared:detekt` limpio. Durante P6.3 se corrigieron findings de detekt (LongMethod/LongParameterList/ReturnCount/MaxLineLength) **refactorizando** (partición en `safetyDecision`/`progressionDecision`, builder local, imports de subtipos), sin tocar el baseline.

## Riesgos pendientes (Fase 6)
- **Sin orquestación de bloques aún**: el motor decide por bloque pero el bucle que lo invoca, mantiene `blocksSinceChange`/`consecutiveFrustrationBlocks`/`lastRaisedAxis` y persiste el vector es de Fase 7 (sesiones) / Fase 10 (persistencia).
- **`confidenceBias` y fatiga llegan como entrada**: su cálculo/derivación real (calibración EX16; fatiga desde caída intra-sesión) se cableará en Fases 7/posteriores; hoy el motor los consume pero nadie los computa.
- **Estancamiento/falsa-mejora (doc §10) no implementados aquí**: son el sistema anti-meseta de la Fase 9.

## Fase 7 — entregado

**Producción** (`domain/session/` + `domain/deferred/`):
- `MemorySessionTemplate.kt` — plantillas 5/10/20/30 min (doc §7).
- `MemorySessionBlueprint.kt` — `MemorySessionPhase` (warmup→codif→inmediata→interferencia→diferida→transfer→cooldown), blueprint con **suma exacta** validada + `MemorySessionBlueprints` por plantilla (micro sin interferencia/diferida; transfer solo en 20/30).
- `MemorySessionAssembler.kt` — `MemorySessionPlan`/`MemorySessionBlock` + ensamblador que mapea fases activas a bloques con dificultad ajustada (warmup más fácil; inmediata DELAY=0; diferida DELAY alto + `deferred`); **sin bloques TRANSFER** (Fase 8).
- `MemoryTrainingCycle.kt` — ciclo 8 semanas con descargas obligatorias en 4 y 8 (doc §8); énfasis en EX1/EX3/EX4.
- `MemoryRecoveryPolicy.kt` — gate de descanso (4h entre mismo módulo, día de descanso semanal, tendencia a la baja → reducir) + `shouldRestIntraSession` (caída >25%, doc §7).
- `deferred/DeferredRecall.kt` — `DeferredRecallItem`/`DeferredRecallConfig`/`DeferredRecallStatus` + `DeferredRecallScheduler` (puro, tiempo inyectado): clasifica PENDING/DUE/EXPIRED por ventana 24h (banda 20–48h) y maneja "no abrió a tiempo" (doc §9/Q13).

**Tests:** 33 nuevos (blueprint 7, assembler 7, ciclo 5, descanso 6, deferred 8). Total memory: **192**.

**Decisiones documentadas:** TRANSFER queda solo en el blueprint (timing) sin generar bloque en V1 (protocolos funcionales = Fase 8/11); tiempo siempre inyectado (sin `Clock.System`); el scheduler es stateless y opera sobre una lista provista (persistencia = Fase 10).

**Verificación:** `:shared:desktopTest` (memory) → **192 tests, 0 fallos** · `:shared:detekt` limpio. Se corrigieron en el camino: 2 tests con `studiedAtEpoch` negativo (bug de test, no de impl) y findings de detekt (LongParameterList/MaxLineLength/MagicNumber) por refactor/wrapping/suppress idiomático.

## Riesgos pendientes (Fase 7)
- **Orquestación viva pendiente**: el assembler produce un plan y el scheduler clasifica, pero el bucle que ejecuta bloques, persiste el `DeferredRecallItem`, mantiene contadores del adaptativo y agrega `SessionResult` es Fase 10 (persistencia) + Fase 12 (runtime/UI).
- **Agregación de sesión (consolidación/dual-task)**: las funciones pairwise existen (Fase 5) pero su invocación a nivel de sesión (immediate vs delayed del mismo material; single vs dual) aún no está cableada — entra al cerrar el runtime.
- **`MemorySessionResult`/historial no modelado aún**: la entidad de resultado de sesión persistible se define en Fase 10.
- **Heredados**: volumen ES bajo el ideal; cobertura de métricas parcial por oleada; revisión adversarial automatizada inconclusa por tooling.

## Fase 8 — entregado

**Producción** (`domain/evaluation/` + `domain/transfer/`):
- `evaluation/MemoryProbe.kt` — `MemoryProbe`/`ProbeFormat` + `MemoryProbeBank` (WM/episódica/asociativa); probes **reservadas** por construcción (contenido nunca entrenado), formato V1 = `SAME_PARADIGM_RESERVED` (transfer de contenido; el de paradigma distinto se difiere).
- `evaluation/MemoryBaselineProtocol.kt` — `MemoryBaselineBlock`/`MemoryBaselineProtocol` + `MemoryBaselineProtocols.INITIAL` (doc §9): calibración por subcapacidad + ≥1 probe reservada, dificultad fija, nunca TRAINING.
- `evaluation/ReliableChangeIndex.kt` — RCI de Jacobson-Truax con gate de fiabilidad (no computa con datos insuficientes).
- `evaluation/RealImprovementCriterion.kt` — `ImprovementPattern` (genuina/práctica/estancamiento/transfer/consolidación, doc §9.4) + `isGenuine` (umbral doc Q12: ganancia en NO entrenado + tamaño de efecto ≥0.30 + ≥2 semanas).
- `transfer/TransferProtocol.kt` — los 10 microprotocolos funcionales (doc §11) declarativos, con `linkedSubcapacities` y `v1Available`.

**Tests:** 28 nuevos (probes 6, baseline 7, RCI 3, mejora real 6, transferencia 6). Total memory: **220**.

**Decisiones documentadas:** el muestreo vivo de contenido reservado se difiere al runtime (la reserva de Fase 3 es menor que `MIN_POOL_SIZE`, no forma un `WordPool`; el probe solo declara `useReservedContent`); probes de paradigma distinto y microprotocolos que requieren ejercicios fuera de V1 (rostros/audio/instrucciones/prospectiva) marcados como no disponibles (solo `study_efficient` y `language_vocabulary` en V1); RCI nunca afirma cambio sin fiabilidad estimada.

**Verificación:** `:shared:desktopTest` (memory) → **220 tests, 0 fallos** · `:shared:detekt` limpio. Se corrigieron en el camino findings de detekt (MagicNumber→consts, LongParameterList→base.copy, MaxLineLength→wrapping), sin tocar el baseline.

## Riesgos pendientes (Fase 8)
- **Administración viva de probes pendiente**: el dominio define qué/cómo medir, pero ejecutar la probe contra el contenido reservado necesita un sampler sobre la reserva (Fase 3) — wiring de runtime (Fase 12). El pool reservado actual (~28 palabras) es pequeño; crecerá al ampliar el corpus ES.
- **Fiabilidad (RCI) sin estimar aún**: requiere ≥2-3 administraciones de probe acumuladas (persistencia Fase 10 + historial).
- **Transfer de paradigma distinto diferido**: V1 solo hace transfer de contenido (misma tarea, material reservado); el transfer de formato (p.ej. Span Operaciones→Lectura) llega con más ejercicios.
- **Heredados**: volumen ES bajo el ideal; cobertura de métricas parcial por oleada; orquestación viva (runtime) pendiente.

## Fase 9 — entregado

**Producción** (`personalization/` + `antiplateau/` + `reporting/`):
- `personalization/MemoryArchetype.kt` — 11 arquetipos (doc §12); BEGINNER y DEMANDING_STUDENT son declarados por el usuario, el resto inferibles.
- `personalization/PersonalizationProfile.kt` — `MemoryScoringModifiers` + `PersonalizationProfile` + `MemoryPersonalizationProfiles` (pesos/bias/scoring/plantilla por arquetipo).
- `personalization/ArchetypeInference.kt` — `MemoryArchetypeSignals` + inferencia por score+confianza (olvida rápido, confunde, over/under, lento-preciso, rápido-impreciso, low-WM, advanced, elite).
- `antiplateau/PlateauDetector.kt` — detección de meseta y mejora falsa (doc §10) + 9 `InterventionProtocol` + `InterventionPlanner` con regla de oro.
- `reporting/CompositeIndices.kt` — Learning Velocity, Retention Quality, Resilience, Calibration Quality (doc §6), con flag de confianza.
- `reporting/MemoryFitnessScore.kt` — `MemoryFitnessCalculator` con banda honesta + reporte **por subcapacidad** (doc §13.15).

**Tests:** 36 nuevos (perfiles 7, inferencia 10, anti-meseta 8, índices 6, fitness 5). Total memory: **256**.

**Decisiones documentadas:** la personalización se infiere del patrón real (no por preferencia, doc §12.9); el reporte nunca es unitario (siempre por subcapacidad); índices y score capan confianza por tamaño de muestra; la regla de oro (2+ semanas) redirige a GOAL_REPLANNING.

**Verificación:** `:shared:desktopTest` (memory) → **256 tests, 0 fallos** · `:shared:detekt` limpio (corregidos MagicNumber/MaxLineLength/LongParameterList por refactor, sin baseline).

## Riesgos pendientes (Fase 9)
- **Señales de entrada sin cableado real**: la inferencia y el anti-meseta consumen señales (consolidación, d-prime, bias, tendencias) que hoy nadie computa de forma agregada — su derivación desde historial es Fase 10 (persistencia) + Fase 12 (runtime).
- **DEMANDING_STUDENT no se cubre por inferencia** (declarado por el usuario en onboarding, pendiente UI Fase 12).
- **Heredados**: volumen ES bajo el ideal; cobertura de métricas parcial por oleada (d-prime/K-Cowan/Brier llegan con EX6/EX12/EX16); orquestación viva (runtime) pendiente.

## Estado: lógica de dominio COMPLETA (Fases 1–9)
Todo el dominio de MindBoost · Memoria (modelo, catálogo, contenido, generadores, métricas, adaptativo, sesiones/recall diferido, evaluación/transferencia, personalización/anti-meseta/reporting) está implementado y testeado en `commonMain` puro: **256 tests verdes, detekt limpio**. Faltan las capas de integración: persistencia, DI, UI/runtime.

## Fase 10 — entregado

**Producción** (`data/` + dominio persistible):
- `domain/model/MemoryProfileState.kt` — estado evolutivo persistible (arquetipo, sensibilidad CME, dificultad por ejercicio, semana de ciclo) con `initial()` y copy-on-write.
- `domain/model/MemorySessionSummary.kt` — resumen de sesión (sin trials crudos).
- `domain/repository/MemoryRepositories.kt` — interfaces (profile, session history, deferred recall, recent registry).
- `data/dto/MemoryDtos.kt` — DTOs `@Serializable` con `schemaVersion` (baseline v1).
- `data/local/MemoryJson.kt` — Json (`ignoreUnknownKeys`/`encodeDefaults`).
- `data/mapper/MemoryPersistenceMappers.kt` + `MemoryContentMappers.kt` — DTO↔dominio (clamp en lectura; enums por nombre).
- `data/local/LocalMemoryRepositories.kt` — repos cifrados sobre `SecureKeyValueStore` (reusado de focus); corrupto→null.
- `data/InMemoryMemoryRepositories.kt` — fakes in-memory para tests de capas superiores.

**Migraciones:** baseline de esquema establecido (`schemaVersion = 1` en cada DTO). Sin código de migración aún (primera versión); el gancho `schemaVersion` queda para futuras migraciones.

**Tests:** 21 nuevos (estado 6, mappers/JSON round-trip 6, repos cifrados 9). Total memory: **277**.

**Decisión de seguridad (condición 5, documentada — sin STOP):** se **reutiliza** el `SecureKeyValueStore` de focus (AES-256-GCM + Android Keystore; in-memory en desktop/iOS) en lugar de duplicar cripto (aprendizaje 2026-05-28). Solo se persisten **resúmenes/estado**, nunca trials crudos (aprendizaje 2026-06-05). Las prefs cifradas del nuevo namespace quedan cubiertas por la exclusión de backups existente (`backup_rules.xml` con `sharedpref path="."`). Acoplamiento `memory→focus` en esta utilidad de plataforma anotado como deuda (extraer a `core/` en el futuro).

**Verificación:** `:shared:desktopTest` (memory) → **277 tests, 0 fallos** · `:shared:detekt` limpio (corregidos `TooManyFunctions` dividiendo mappers en 2 archivos y `MaxLineLength`, sin baseline).

## Riesgos pendientes (Fase 10)
- **Acoplamiento memory→focus** por el `SecureKeyValueStore` compartido: deuda controlada; extraer la utilidad a `core/` cuando convenga (no bloquea).
- **`rememberSecureKeyValueStore` (factory @Composable)**: el cableado de producción del store por namespace es Fase 11 (DI); hoy los repos reciben el store inyectado.
- **Sin migraciones reales todavía**: solo el baseline v1; al evolucionar el esquema habrá que escribir migraciones por `schemaVersion`.
- **Heredados**: volumen ES bajo el ideal; orquestación viva (runtime) pendiente; cobertura de métricas parcial por oleada.

## Fase 11 — entregado

**Producción** (`di/`):
- `di/MemoryGraph.kt` — `@Stable data class MemoryGraph(...)` que agrupa todo el wiring del feature (content, generadores EX1/EX3/EX4, metrics/adaptive engines, assembler, recovery policy, deferred scheduler, archetype inference, plateau detector + planner, fitness calculator, los 4 repos, `EpochClock`), con `generatorFor(id)`. `rememberMemoryGraph()` construye producción (engines stateless + repos `Local*` sobre `rememberSecureKeyValueStore("mindboost_memory")`).

**Tests:** 5 nuevos (`MemoryGraphTest`): construcción con fakes, cobertura de generadores V1, `generatorFor` (presente/null), coherencia generadores↔`content.supportedExercises()`, clock legible. Total memory: **282**.

**Decisiones documentadas:** interfaces donde existen (metrics/adaptive/recovery/archetype) y concretos para los puros sin interfaz (assembler/scheduler/detector/planner/fitness), igual que `FocusGraph`; los tests construyen el grafo directo con fakes (nunca `rememberMemoryGraph`, que es `@Composable`). Reutiliza `rememberSecureKeyValueStore` de focus (mismo acoplamiento de plataforma ya anotado).

**Verificación:** `:shared:desktopTest` (memory) → **282 tests, 0 fallos** · `:shared:detekt` limpio (sin findings, sin correcciones necesarias).

## Riesgos pendientes (Fase 11)
- **Acoplamiento memory→focus** (store/factory compartidos): deuda ya anotada (extraer a `core/`).

## Fase 12 — entregado

**Producción** (`presentation/` + hub):
- `presentation/state/MemoryUiState.kt` — estados home (`Loading`/`Ready`) y sesión (`Intro`/`Study`/`Recall`/`Finished`/`Aborted`).
- `presentation/MemoryHomeStateHolder.kt` — carga perfil/arquetipo/semana, ejercicios implementados y nº de recall diferido **due** ahora.
- `presentation/MemorySessionStateHolder.kt` — **orquestación viva** turn-based: genera trial (graph) → estudio → recall por selección → scoring → métricas → decisión adaptativa → persiste (perfil, resumen de sesión, recent-items, registro de recall diferido 24h). Cierra el bucle que arrastraban las fases previas.
- `presentation/RememberMemoryStateHolders.kt` — factories `@Composable`.
- `presentation/ui/{MemoryHomeScreen,MemorySessionScreen,MemoryRootScreen}.kt` — Compose funcional (sin animaciones), paleta `AppColors`.
- `presentation/ui/MindBoostHubScreen.kt` (`features/mindboost/presentation/ui/`) — **hub** selector Enfoque/Memoria que monta cada root.
- `HomeScreen.kt` — `HomeDestination.MindBoost` ahora monta `MindBoostHubScreen` (antes `FocusRootScreen`).

**Tests:** 7 nuevos (home holder 3, session holder 3, smoke de navegación 1). Total memory: **289**.

**Decisiones documentadas:** recall V1 por **reconocimiento** uniforme (los scorers de dominio siguen siendo fieles por paradigma); presentación temporizada, recall tecleado/serial fiel, TTS (EX15) y animaciones **diferidos** (memoria de feedback: animaciones solo a petición). Reutiliza paleta MindBoost de `AppColors` (sin literales locales).

**Verificación:** `:shared:desktopTest` (memory) → **289 tests, 0 fallos** · `:shared:detekt` limpio · **`:androidApp:assembleDebug` BUILD SUCCESSFUL** (Memoria accesible end-to-end desde boostMind).

## Riesgos pendientes (Fase 12)
- **Fidelidad de paradigma en recall**: V1 usa reconocimiento; la entrada serial (EX1) y cued tecleada (EX4) son refinamiento de UI. El dominio ya las soporta y está testeado.
- **Sesión = bloque principal**: el runtime ejecuta el ejercicio foco; warmup/interferencia/diferido intra-sesión del blueprint y la administración viva de probes/baseline quedan como siguiente iteración (assembler/scheduler/baseline existen y están testeados).
- **TTS (EX15) y animaciones**: diferidos.
- **Heredados**: volumen ES bajo el ideal; acoplamiento memory→focus (store).

## 🎉 Estado: MindBoost · Memoria COMPLETO (Fases 1–12)
Feature end-to-end implementado y verificado: dominio + contenido + generadores + métricas + adaptativo + sesiones/recall diferido + evaluación/transferencia + personalización/anti-meseta/reporting + persistencia cifrada + DI + presentación + wiring en boostMind. **289 tests de memory verdes, detekt limpio, `assembleDebug` OK.** Accesible en la app: Home → MindBoost → **Memoria**.

### Próximas iteraciones (post-V1, fuera del plan de 12 fases)
- Ampliar corpus ES a 2000+ palabras / 600+ pares (qualityReport lo expone).
- Oleadas de ejercicios EX2/EX5–EX16 (dominio listo para EX1/EX3/EX4; resto por construir).
- Recall fiel por paradigma + presentación temporizada + TTS (EX15) + animaciones.
- Sesiones multi-bloque completas (warmup/interferencia/diferido) y administración viva de baseline/probes.
- Extraer `SecureKeyValueStore` a `core/` (romper acoplamiento memory→focus).
- Métricas avanzadas (d-prime/K-Cowan/Brier) al entrar EX6/EX12/EX16.

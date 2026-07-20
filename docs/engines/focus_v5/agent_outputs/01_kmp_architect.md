# Orden 1 — kmp-architect (2026-07-19) — output íntegro

> Read-only sobre HEAD 27849450. Integrado en: plan §4/§5, REQUIREMENTS_MATRIX_V5.csv (familias PLAT/DATA/ADP),
> ADR-FOCUS-SESSION-STATE, GATE_CATALOG_V5. Hallazgos NUEVOS respecto a V4: SKV-transversal (39 consumers),
> AdaptiveModeGate muerto, 3ª superficie de score (chip FOCO en home), corrección MonotonicClock, estándar de
> 16 eventos localizado (04_ §18), Focus emite 0 eventos hoy, FrameTimingProbe iOS = stub, 4ª+5ª ruta arquetipo.

# AUDIT — Focus v1 Architecture, Baseline & Parity (read-only, order-1 kmp-architect)

Base: HEAD `27849450345ef57f96b02f1900afe1251b456292`, branch `product/mindboost-wip-consolidation-20260717`. Zero repo mutations. Where I independently re-verified a V4 map claim it is marked `REPO_VERIFIED` with fresh file:line; where I only relied on V4/docs without re-reading the file it is marked `REPORTED`; V4 claims I could not touch are `NOT_VERIFIED`. One V4 claim is explicitly upgraded/corrected below (MonotonicClock).

## A) Inventario Focus v1

`shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/**` = **116 files**. Transversal `core/` usado por Focus = 71 files (`core/session`, `core/scoring`, `core/timing`, `core/bank`, `core/antigaming`, `core/runtime`, `core/canonical`, `core/copy`).

| Componente | Path (file:line) | Evidencia | Consumers externos | Disposición |
|---|---|---|---|---|
| FocusGraph (DI) | `focus/di/FocusGraph.kt:68-233` | REPO_VERIFIED; 2 registries (legacy + versioned); `fixedBlockForm` (:131-140) seed wall-clock no persistida | `FocusRootScreen.kt:41`, `RememberFocusStateHolders.kt` | MIGRATE (patrón DI) |
| FocusSessionStateHolder | `focus/presentation/FocusSessionStateHolder.kt:1-645` | REPO_VERIFIED; god-object, 2 rutas entrelazadas (98-105); `decideAdaptation` (397-419) | `RememberFocusStateHolders.kt:71-93` → `FocusRootScreen.kt:118-123` | REPLACE |
| FocusSessionAssembler | `focus/domain/session/FocusSessionAssembler.kt:1-186` | REPO_VERIFIED; puro; despacho por paradigma (57-80) | Holder:334,345,376; FocusGraph:148 | KEEP |
| TrialClassifier | `focus/domain/exercise/TrialClassifier.kt:1-101` | REPO_VERIFIED; puro | Assembler:60,64,74; Holder:494-509 | MIGRATE |
| FocusEngine / DefaultFocusEngine | `focus/domain/engine/` | REPO_VERIFIED; createSession mint→resolve→generate→digest (42-60); restoreSession fail-closed 9 razones (76-136); `measurementEligible`/`officialScoringEligible` MUERTOS (solo escritor :233-234 + 1 test) | FocusGraph:107, Holder:299 | KEEP y PROMOVER |
| E1-E13 | `focus/domain/exercise/E*Generator.kt` | REPO_VERIFIED; versioned={E2}, legacy={12} (`FocusExerciseGenerators.kt:35-59`, init-check 64-73); `E2FiltroGenerator.kt:70` `isTarget=true` incondicional → CORRECT_REJECTION imposible | FocusGraph; Holder:98,105 | E2 MIGRATE; 12 legacy KEEP(práctica)/RETIRE(evidencia) |
| FocusScore | `focus/domain/reporting/FocusScore.kt:11-79` | REPO_VERIFIED; compuesto 0..100 (55-61); MIN_N=20, MIN_COMPONENTS=2 | ProgressStateHolder → `FocusProgressScreen.kt:68,83-113` | RETIRE como medida |
| PersonalizedSessionScorer | `focus/domain/personalization/PersonalizedSessionScorer.kt:19-115` | REPO_VERIFIED; ponderado por ScoringModifiers (archetype) | Holder:374-376 → `FocusSummaryScreen.kt:94-99,170-210` | RETIRE |
| ArchetypeInference/Personalization | `focus/domain/personalization/` | REPO_VERIFIED (§C) | Holder:198,203-205,365-369 | RETIRE / QUARANTINED |
| AdaptiveEngine + FocusAdaptiveRulePolicy | `focus/domain/adaptive/` | REPO_VERIFIED; tabla fatiga→impulsividad→rendimiento (76-77); ruta E2 FIXED bypassa motor (Holder:401-406) | Holder:407-418 (solo legacy) | REPLACE |
| **AdaptiveModeGate** | `focus/domain/evaluation/AdaptiveModeGate.kt:20-31` | REPO_VERIFIED; **CÓDIGO MUERTO: 0 call-sites de producción** (grep: archivo + test + 2 docs) | ninguno | RETIRE o cablear explícito |
| Repos Focus | `focus/domain/repository/FocusRepositories.kt:13-48` | REPO_VERIFIED; `recent(limit)` obligatorio | LocalFocusRepositories; FocusGraph:158-166 | KEEP v1; v2 stores propios |
| BaselineProtocol | `focus/domain/evaluation/BaselineProtocol.kt:58-126` | REPO_VERIFIED; 4 bloques fijos + cuestionario; `require(mode != TRAINING)` | `FocusRootScreen.kt:70,80` | MIGRATE (conceptos) |
| ProbeExerciseMapping | `focus/domain/evaluation/ProbeExerciseMapping.kt:21-41` | REPO_VERIFIED; PVT/MACKWORTH→E1, FLANKER→E2, GO_NO_GO→E3 | BaselineAssembler | MIGRATE (→ forms HELD_OUT) |
| FocusEvaluationService / RCI | `focus/domain/evaluation/` | REPO_VERIFIED; RCI Jacobson&Truax gateado por ProbeReliabilityEstimator; `NotEnoughData` default (33-43) | ProgressStateHolder → `FocusProgressScreen.kt:149-184` | MIGRATE (precedente honesto más valioso) |
| SessionRecorder / RecoveryScanner | `core/session/` (transversal, NO focus) | REPO_VERIFIED; write-ahead total (22-49); onStart 1 fila (91-111); recover() solo `Stored` (38-47), grace 120s | FocusGraph:206-232; RememberFocusStateHolders:79-92; FocusRootScreen:52-54 | KEEP (transversal 7+ features) |
| **SecureKeyValueStore** | `focus/data/local/` | REPO_VERIFIED; **HALLAZGO: importado por 39 archivos FUERA de focus/** (core/session, AppGraph, AccountDeletionCoordinator, DI de Memory/Speed/Logic/Creativity/Emotions/Decisiones/Coach/Language) | 39 archivos | **KEEP — imposible retirar con focus/; extraer a core/ en slice previa dedicada o excepción explícita de frontera** |
| MonotonicClock | `focus/presentation/exercise/MonotonicClock.kt:1-22` | REPO_VERIFIED — **corrección a V4**: `fun interface` sobre `kotlin.time.TimeSource.Monotonic`, 100% commonMain, sin fricción de plataforma | Holder (via graph) | KEEP tal cual |
| MicroPauseGate | `focus/presentation/exercise/MicroPauseGate.kt:12-25` | REPO_VERIFIED; solo TRAINING vía scoringModifiers.prePressureMicroPause | Holder:203-205,163 | REPLACE |

## B) Score v1 visible — TRES superficies + ocultación

1. **PersonalizedScore por-sesión**: Holder:374-376 → `FocusReport.personalizedScore` → `FocusSummaryScreen.kt:94-99` → `PersonalizedScoreCard` (170-210) "RENDIMIENTO PERSONALIZADO" 0-100. Ruta: `MindBoostHubScreen.kt:117` → FocusRootScreen → Session → `FocusSessionScreen.kt:82` Finished → Summary.
2. **FocusScore compuesto**: ProgressStateHolder → `FocusProgressScreen.kt:83-113` (`FocusScoreCard`), banda de incertidumbre. Ruta: `FocusRootScreen.kt:106-115` ← `FocusHomeScreen.kt:184-189` ("Ver progreso →", SIN gate).
3. **Chip "FOCO" en home**: `FocusHomeScreen.kt:246-253` `StatsRow` = `lastSustainedFocusIndex` bruto de la última sesión, visible sin navegar.

**Ocultación reversible (mecanismo real `core/config/FeatureFlags.kt:20-127`):** flag nuevo (p.ej. `focusScoreSurfacesEnabled`) añadido a las TRES impls (DefaultFeatureFlags/StaticFeatureFlags/RemoteConfigFeatureFlags — learning 2026-06-22). Gatea: StatChip FOCO (249-253), link "Ver progreso →" + FocusProgressScreen/FocusScoreCard, PersonalizedScoreCard (94-99). El resto de FocusSummaryScreen (métricas por-sesión con caption) NO se toca. **Persistencia intacta**: `mindboost_focus` (AccountBound, `MindBoostStorageNamespaces.kt:5,81-84`) sigue escribiendo `SessionResult`/`UserCognitiveProfile`; los scores son derivados on-read puros, no persistidos → apagar no pierde nada; reencender muestra el histórico. Ninguna de las 12 flags existentes sirve (`mindboostGlobalScoreEnabled` es del brain-profile global, semántica distinta).

## C) Ruta arquetipo (MB-ENG-0008) — CINCO rutas de influencia inter-sesión

1. `ArchetypeInference.kt:61` — `IMPULSIVE to mean(faScore, faScore, rtFast)` — faScore DOBLE-ponderado. VERIFIED.
2. `ArchetypeSignalsFactory.from` — rtCv :114, lapseRate :116. VERIFIED.
3. `ArchetypePersonalization.startingDifficulty` (:21-22) ← Holder:198; `biasedDefault` (24-29) solo si no hay dificultad persistida; sobre `relevantAxes`.
4. `MicroPauseGate` ← Holder:203-205 (solo TRAINING).
5. `PersonalizationProfiles.forArchetype().scoringModifiers` → `DefaultPersonalizedSessionScorer.score` (39-62: errorPenalty/speedIncentive/lapsePenalty) — **4ª ruta: el arquetipo dobla el SCORE**, no solo dificultad.
6. Persistencia del sesgo: `finishSession` (358-370) escribe `startingDifficulty[...]` → una vez escrito, el sesgo queda CONGELADO para ese ejercicio.
Rutas totales sesión→sesión: (a) startingDifficulty persistido, (b) sesgo arquetipo si no persistido, (c) micro-pausa, (d) ponderación del score, (e) AdaptiveEngine.decide rama legacy. `FocusAdaptationPolicy.FIXED` (Holder:401-406) SOLO guarda la rama engine (E2).
Contención: (i) QUARANTINE — no escribir biased-default; arrancar EASIEST real hasta protocolo de calibración v2; (ii) `UserArchetype` es conductual (no demográfico) — no viola DNB directamente, pero es heurística no validada.

## D) SessionStatus global

`enum SessionStatus { STARTED, CHECKPOINT, FINALIZED, ABANDONED }` — `core/session/SessionRecord.kt:17`. `SessionRecord` (34-54) motor-agnóstico, minimizado, opcional `reproducibleIdentity`. Consumers: 32 archivos — producción: core/session/*, core/session/cognitive/{WriteAheadResolver,WriteAheadEvaluator}, emotions/EmotionTerminalSemantics, language/{LanguageSessionReducer,SessionModels}; resto tests de 6+ features. **Sin PAUSED hoy.** Extender el global = actualizar todos los `when` exhaustivos transversales (learning 2026-06-16, más caro por transversal). Recomendación: estado de runtime Focus-LOCAL siguiendo el patrón `core.session.cognitive` (namespace+tipo nuevo), NO extender el enum global; ADR explícito requerido.

## E) Eventos — estándar de 16 localizado

**`docs/context_pack/04_EXERCISE_QUALITY_STANDARD.md` §18 "Taxonomía de eventos" (líneas 697-718), exactamente 16:**
ExerciseOpened, TutorialStarted, TutorialCompleted, SessionStarted, TrialPresented, ResponseStarted, ResponseSubmitted, TrialResolved, DifficultyChanged, SessionPaused, SessionResumed, SessionInterrupted, SessionCompleted, SessionInvalidated, ResultPersisted, ResultSynced.
Mecanismo real: `core/analytics/MindBoostAnalytics.kt:1-58` — `MindBoostAnalyticsEvent(name, params: Map<String,String>)` genérico, `MindBoostAnalyticsHub.log()` fail-safe, NoOp sink por defecto, `MindBoostAnalyticsPolicy.requireRealSinkInRelease`. Consumers hoy: coach + emotions (`EmotionAnalyticsBridge` = precedente). **Focus emite 0 eventos (grep = 0 hits).** Cablear 16 = wiring puro, sin tocar el mecanismo.

## F) Paridad iOS/Desktop

| expect | Android | iOS | Desktop | Riesgo v2 |
|---|---|---|---|---|
| rememberSecureKeyValueStore | AES-256-GCM+Keystore | **REAL** (`IosKeychainBackend`: SecItem*, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly) | in-memory | Bajo impl; alto verificación (NOT_VERIFIED ejecución) |
| rememberFrameTimingProbe (`core/timing/FrameTimingProbe.kt:46`) | Real (Choreographer API≥34; solo speed/ lo usa — Focus NO) | **STUB** (`FrameTimingProbe.ios.kt:10` = NoOp; "no CADisplayLink capture wired yet") | no-op | **CRÍTICO si v2 adopta timebase autoritativo: iOS estructuralmente T0 hasta portar CADisplayLink** |
| MonotonicClock | — | — | — | Ninguno (100% commonMain) |
| PlatformBackHandler (FocusRootScreen:63,81,95,109,132) | Real | existe (contenido no releído) | no-op | Bajo |

`iosApp/`: Podfile `platform :ios, '15.0'`; sin xcodeproj versionado (por diseño); TODO Focus iOS NOT_VERIFIED en este host.

## G) Frontera v2 basada en grafo

**Legacy prohibido**: FocusSessionStateHolder, FocusGraph, DefaultMetricsEngine/MetricsEngine (**accuracy sin chance-floor confirmado `DefaultMetricsEngine.kt:43-57`**: correct=HIT+CORRECT_REJECTION sobre validTrials.size sin resta de azar), FocusScore/DefaultFocusScoreCalculator, PersonalizedSessionScorer, ArchetypeInference/ArchetypePersonalization/PersonalizationProfile, AdaptiveEngine/FocusAdaptiveRulePolicy, 12 generadores legacy, AdaptiveModeGate.
**Transversales legítimos**: core/scoring/*, core/session/*, core/timing/*, core/bank/*, core/antigaming/*, core/runtime/{DeterministicRng,SessionSeed,RngVersion}, core/canonical/*, storage/MindBoostStorageNamespaces, core/analytics/*, y **`focus/data/local/SecureKeyValueStore` como excepción explícita** (o extracción previa a core/).
**Forma del test de frontera**: (a) FQN prohibidos como constantes Kotlin (no strings — lección anti-negación del copy-scanner); (b) escaneo del árbol focus2/ (comentarios incluidos) por import O referencia calificada; (c) test de FIRMA por reflexión: ningún tipo público de focus2 asignable a/desde tipo legacy (cubre herencia estructural). **Limitación heredada** (`shared/build.gradle.kts:633`): tokens no bloquean RE-IMPLEMENTACIÓN equivalente → mitigación real = golden tests de comportamiento ("accuracy a 50% azar puro ⇒ null/low-confidence"), no solo ausencia de símbolo.
`Focus2DependencyBoundaryTest`: grep "Focus2" = 0 hits en HEAD (solo existió en el candidato rechazado).

## H) Toolchain

agp=9.1.0 (toml:2), compileSdk/targetSdk=36 (:3,:5), **minSdk=29 (:4)**, kotlin=2.3.10 (:16), composeMP=1.10.3 (:10), iOS 15.0 (Podfile:11).
**CONTRADICTED confirmado también en canon técnico**: `05_TECHNICAL_CURRENT_STATE.md` §3 (línea 51) y §4.1 (línea 80) = "Android 8/API 26 e iOS 15" CEO_APPROVED vs repo minSdk=29. Opciones: (a) bajar a 26 (ADR con coste: verificar AGP/Compose/Kotlin en API 26; cobertura Choreographer-nanos por API level → riesgo parity timing); (b) actualizar canon a API 29 (decisión CEO per 06_ §13: "no arreglar el documento para justificar el código"). Decide el CEO.
Source-sets test: desktopTest=JVM; commonTest=KMP (iOS NOT_VERIFIED aquí). Tareas reales: `:shared:compileAndroidMain`, `:shared:compileKotlinDesktop`, `:shared:desktopTest`, `:shared:detekt`, `:androidApp:compileDebugKotlin`.

## Resumen de disposición (delta vs V4)

- KEEP confirmados: FocusEngine/DefaultFocusEngine/ReproducibleSessionIdentity, SessionRecorder/RecoveryScanner, MonotonicClock (corrección: sin migración), FocusEvaluationService/RCI, SessionMeasurementPurpose (8v en `ProfileMeasurementEligibility.kt:11-18`), FocusProfileSource D1.
- KEEP con matiz NUEVO: SecureKeyValueStore (39 consumers externos → extracción a core/ como slice previa o excepción de frontera).
- REPLACE: FocusSessionStateHolder, DefaultMetricsEngine, AdaptiveEngine, prescripción por arquetipo.
- RETIRE (evidencia): FocusScore/PersonalizedSessionScorer + superficies UI (3), generadores legacy (como evidencia), AdaptiveModeGate (muerto).
- MIGRATE: catálogo (13/13 wired), ProbeBank/ProbeExerciseMapping, BaselineProtocol (conceptos), TrialClassifier, patrón FocusGraph.

# 13 · Prompts de Ejecución para Claude Code

> Cada prompt es **pequeño, independiente y verificable**. Pégalo tal cual en una
> sesión futura. Todos comparten estas **restricciones globales** (no las repito
> en cada uno):
>
> - Paquete: `com.mindos.app.features.mindboost.focus`, módulo `:shared`.
> - Kotlin puro en dominio (sin Compose/Firebase/plataforma) salvo que se indique.
> - Inmutable; value objects con `require`. Reloj inyectado, nunca `Clock.System`.
> - Patrón StateHolder + `rememberX` + Composable tonto para UI.
> - Tests con `kotlin-test` + `runTest`; fakes; `cancelChildren()` al final.
> - Cero dependencias nuevas. Respetar ratchet de Detekt. No tocar otras features.
> - Seguir la doc indicada de `/docs/mindboost_enfoque_implementation_plan`.

---

## Prompt 1 — Dominio (Fase 1)
**Objetivo:** crear el modelo de dominio puro.
**Lee:** `02_domain_model_plan.md`.
**Archivos esperados:** `domain/model/{Capacity,ExerciseId,Paradigm,Mode,
TrialOutcome,DifficultyVector,DifficultyAxis,ExerciseDefinition,Stimulus,Response,
TrialResult,BlockResult,SessionResult,SubjectiveState,UserCognitiveProfile}.kt`,
`domain/model/valueobject/{ReactionTimeMs,Proportion,AxisLevel,SessionId,BlockId,
TrialId}.kt`, `domain/repository/{FocusProfileRepository,SessionHistoryRepository,
BaselineRepository,ProbeBankRepository}.kt`.
**Restricciones:** sin lógica de cálculo; solo modelo + contratos.
**Tests requeridos:** `DifficultyVectorTest` (adjust/level por eje),
`ValueObjectsTest` (invariantes), construcción de cada result.
**Done:** `:shared:compileKotlinDesktop` y `:shared:desktopTest` verdes; cero
imports de Compose/Firebase en el paquete.

## Prompt 2 — Motor de métricas (Fase 2)
**Objetivo:** implementar `MetricsEngine` y todas las métricas.
**Lee:** `03_metrics_engine_plan.md`.
**Archivos esperados:** `domain/metrics/{MetricKey,MetricValue,MetricSet,
MetricsEngine,DefaultMetricsEngine,RtCleaning,SsrtCalculator,EfficiencyScores,
ParadigmEffects,FocusIndices,Rci}.kt`.
**Restricciones:** RT mediano/RT-CV primarias; MAD para outliers; SSRT por
integración; RCI gated; todo `MetricValue` con `n`/`confident`.
**Tests requeridos:** vectores conocidos por métrica; SSRT replicando el paper;
edge cases doc 03 §8; property checks (rangos).
**Done:** tests verdes; ningún cálculo usa media como primaria; cobertura alta.

## Prompt 3 — Motor adaptativo (Fase 3)
**Objetivo:** `AdaptiveEngine.decide` con todas las reglas.
**Lee:** `04_adaptive_engine_plan.md`.
**Archivos esperados:** `domain/adaptive/{AdaptiveDecision,AdaptiveResult,
AdaptiveInput,AdaptiveConfig,AdaptiveEngine,DefaultAdaptiveEngine,FatigueSignal,
FatigueDetector,AxisMastery,ImpulsivityDetector}.kt`.
**Restricciones:** un eje por decisión; seguridad>progresión; histéresis;
solo TRAINING; nunca afirma mejora real.
**Tests requeridos:** una prueba por rama de la tabla de decisión; anti-oscilación;
impulsividad→ContainImpulsivity; fatiga→Cooldown/StopSession; saturación→SwitchAxis.
**Done:** tests verdes; secuencia adversarial no oscila.

## Prompt 4 — Baseline y evaluación (Fase 4)
**Objetivo:** modos, baseline, calibración, probes, RCI.
**Lee:** `06_evaluation_and_baseline_plan.md`.
**Archivos esperados:** `domain/evaluation/{BaselineProtocol,BaselineSnapshot,
StateQuestionnaire,CalibrationEngine,DefaultCalibrationEngine,ProbeDefinition,
SeedPolicy,EvaluationResult}.kt` + `data/InMemoryProbeBankRepository.kt`.
**Restricciones:** banco de probes disjunto de training; dificultad fija en
EVALUATION/CALIBRATION; RCI gated.
**Tests requeridos:** test que falla si se adapta en EVALUATION; intersección
vacía probes∩training; calibración produce umbrales; RCI gated.
**Done:** tests verdes; separación de modos enforced.

## Prompt 5 — E1 Vigía (Fase 5, vertical slice — parte ejercicio)
**Objetivo:** primer ejercicio completo (generación + runtime + mapping).
**Lee:** `05_exercise_engine_plan.md` (E1) + `01` (timing).
**Archivos esperados:** `domain/exercise/{TrialGenerator,BlockSpec,StimulusPayload,
DifficultyMapping}.kt`, `domain/exercise/generators/E1VigiaGenerator.kt`,
`domain/exercise/mapping/E1DifficultyMapping.kt`, `presentation/exercise/
{ExerciseRuntime,TrialClock}.kt`, `{android,desktop,ios}Main/.../exercise/
MonotonicTimeSource.<plat>.kt`.
**Restricciones:** generador determinista (seed); RT con reloj monótono; runtime
no calcula métricas ni adapta.
**Tests requeridos:** determinismo de generación; clasificación de outcomes;
timing RT = delta monótono con `FakeMonotonicClock`.
**Done:** se puede correr un bloque E1 y obtener `List<TrialResult>` correctos.

## Prompt 6 — E2 Filtro (Fase 6)
**Objetivo:** flanker (selectiva + interferencia).
**Lee:** `05` (E2) + `03` (congruencyEffect).
**Archivos esperados:** `generators/E2FiltroGenerator.kt`,
`mapping/E2DifficultyMapping.kt`, `presentation/exercise/renderers/E2Renderer.kt`.
**Restricciones:** reutiliza `ExerciseRuntime`; proporción incongruente =
`interference`.
**Tests requeridos:** generación determinista; efecto de congruencia calculable
sobre la salida; mapping solo lee ejes relevantes.
**Done:** métricas de E2 (congruencyEffect) salen del lazo con datos sintéticos.

## Prompt 7 — E3 Gatillo (Fase 6)
**Objetivo:** go/no-go (inhibición proactiva).
**Lee:** `05` (E3) + `03` (falseAlarms, PES).
**Archivos esperados:** `generators/E3GatilloGenerator.kt`,
`mapping/E3DifficultyMapping.kt`, `renderers/E3Renderer.kt`.
**Restricciones:** ratio go:no-go = parámetro; detectar comisiones.
**Tests requeridos:** generación; clasificación FA/omisión/hit; PES sobre serie.
**Done:** comisiones y PES correctos en datos sintéticos.

## Prompt 8 — E4 Freno (Fase 6)
**Objetivo:** stop-signal con SSD adaptativo y SSRT.
**Lee:** `05` (E4) + `03` (SSRT por integración + gaming).
**Archivos esperados:** `generators/E4FrenoGenerator.kt` (tracking SSD 1-up/1-down),
`mapping/E4DifficultyMapping.kt`, `renderers/E4Renderer.kt`.
**Restricciones:** SSD converge a p(stop)≈0.5; SSRT por integración; detectar
ralentización estratégica.
**Tests requeridos:** convergencia SSD; SSRT vs ejemplo del paper; flag de gaming.
**Done:** SSRT calculado y marcado no fiable cuando hay gaming.

## Prompt 9 — Sesiones y periodización (Fase 7)
**Objetivo:** plantillas, ciclo 8 semanas, deload, plan diario.
**Lee:** `07_session_periodization_plan.md`.
**Archivos esperados:** `domain/session/{SessionTemplate,SessionPhase,
SessionBlueprint,SessionBlueprints,TrainingCycle,WeekPlan,RecoveryPolicy,
DailyFocusPlan,DailyPlanGenerator,DefaultDailyPlanGenerator}.kt`.
**Restricciones:** plantillas suman duración exacta; deload en 4 y 8; reloj inyectado.
**Tests requeridos:** suma de duraciones; deload reduce volumen/capa dificultad;
plan determinista respeta semana/disponibilidad/recovery.
**Done:** tests verdes; plan generable para cualquier semana.

## Prompt 10 — Persistencia local (Fase 5/10)
**Objetivo:** `FocusLocalStore` cifrado (patrón del repo) + repos.
**Lee:** `10_data_persistence_plan.md`.
**Archivos esperados:** `presentation/data/FocusLocalStore.kt` (expect),
`{android,desktop,ios}Main/.../FocusLocalStore.<plat>.kt` (actual; Android cifrado
reutilizando el helper de `PublisherPlatformStore.android.kt`), `data/local/
LocalFocusProfileRepository.kt`, `data/InMemoryFocusRepositories.kt`,
`data/dto/*Dto.kt` + `data/mapper/*.kt` (kotlinx-serialization, `schemaVersion`).
**Restricciones:** trials crudos no a Firestore; DTO versionados; cero deps nuevas.
**Tests requeridos:** round-trip DTO↔dominio; InMemory cumple contrato; recientes
respeta `limit`.
**Done:** perfil/baseline/sesión persisten y recargan; Android cifrado.

## Prompt 11 — UI inicial (Fase 5)
**Objetivo:** `FocusHomeScreen` + runner + resumen, wiring en HomeDestination.
**Lee:** `09_ui_ux_integration_plan.md`.
**Archivos esperados:** `presentation/{FocusHomeStateHolder,
RememberFocusHomeStateHolder,SessionRunnerStateHolder}.kt`, `presentation/state/
{FocusHomeUiState,SessionUiState}.kt`, `presentation/ui/{FocusHomeScreen,
ExerciseScreen,SessionSummaryScreen}.kt`, `core/di/FocusGraph.kt` +
`rememberFocusGraph()`, edición mínima de `HomeScreen.kt` (nuevo
`HomeDestination.MindBoost`). Tokens nuevos en `design/AppColors.kt`.
**Restricciones:** un solo punto de wiring; Composables tontos; toda métrica con
incertidumbre; back en ejercicio confirma abort; sin animación decorativa.
**Tests requeridos:** `FocusHomeStateHolderTest`, `SessionRunnerStateHolderTest`
(fakes, `cancelChildren()`).
**Done:** sesión de 5 min de E1 ejecutable en desktop con resumen honesto persistido.

## Prompt 12 — Testing y simuladores (Fase 11)
**Objetivo:** simuladores de usuario y suite de regresión.
**Lee:** `11_testing_validation_plan.md`.
**Archivos esperados:** `commonTest/.../sim/{UserSimulator,Simulators}.kt`,
`MetricsEngineRegressionTest`, `AdaptiveEngineDecisionTableTest`,
`AntiOscillationTest`, `ProbeContaminationTest`, `TimingTest`.
**Restricciones:** 5 perfiles (impulsivo, lento-preciso, fatigado, avanzado,
gamer-E4) con asserts del comportamiento del sistema.
**Tests requeridos:** los listados; ≥80% en dominio puro.
**Done:** simuladores verdes; lazo end-to-end validado sin device.

---

## Orden recomendado de ejecución
1 → 2 → 3 → 4 → 5 → 10 → 11 (cierra vertical slice F5) → 6 → 7 → 8 → 9 → 12, y
luego avanzados (E5–E13) replicando la plantilla de los prompts 6–8.

> Para cada nuevo ejercicio avanzado, clona el formato del Prompt 6 cambiando
> ejercicio, paradigma, ejes y métrica gobernante (ver tabla doc 05 §5).

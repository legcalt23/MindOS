# Estado de Implementación — MindBoost · Enfoque

> Última actualización: 2026-06-04. Rama: `feat/mindboost-enfoque`.

## Resumen

Implementado el **vertical slice completo** (Fases 1–5 del roadmap) más la
**UI/UX avanzada**: dominio puro, motor de métricas honesto, motor adaptativo,
ejercicio E1 Vigía de extremo a extremo, persistencia in-memory, DI, y UI
animada premium integrada en la app por un único punto de wiring.

**Verificación:** `:shared:compileKotlinDesktop` ✅ · `:androidApp:compileDebugKotlin`
✅ · `:shared:detekt` ✅ (ratchet limpio) · **402 tests, 0 fallos** (7 suites nuevas).

## Qué está hecho (por fase)

| Fase | Estado | Entregable |
|------|--------|-----------|
| 1 · Dominio | ✅ | Value objects, `DifficultyVector` (9 ejes), `ExerciseDefinition`, catálogo de los 13 ejercicios, `Trial/Block/SessionResult`, `UserCognitiveProfile`, repos. |
| 2 · Métricas | ✅ | `DefaultMetricsEngine`: accuracy, RT mediano, RT-CV, lapsos (sobre RT crudos), FA/omisiones/anticipaciones, PES, IES/RCS/LISAS, vigilance decrement, índices. MAD para outliers. `SsrtCalculator` (integración + gaming). |
| 3 · Adaptación | ✅ | `DefaultAdaptiveEngine`: SUBIR/MANTENER/BAJAR/CAMBIAR_EJE/COOLDOWN/STOP/CONTAIN_IMPULSIVITY, histéresis anti-oscilación, fatiga, impulsividad. Solo TRAINING. |
| 4 · Evaluación | ✅ | `AdaptiveModeGate` (adaptación solo en TRAINING), probes reservados disjuntos (`ProbeBank`/`ProbeSeedSpace`/`InMemoryProbeBankRepository`), `BaselineProtocol` (15–20 min, cuestionario de estado), `CalibrationEngine`/`DefaultCalibrationEngine` (umbrales individuales + dificultad inicial + snapshot), `ReliableChangeIndex` (RCI gated) + `EvaluationComparator`. |
| 5 · Vertical slice E1 | ✅ | `E1VigiaGenerator` determinista, `TrialClassifier`, `ExerciseRuntime`/`MonotonicClock`, `FocusSessionStateHolder` (loop tiempo real con RT monótono), `FocusSessionAssembler`, persistencia in-memory, UI completa, wiring `HomeDestination.MindBoost`. |
| 6 · Ejercicios núcleo (dominio) | ✅ | E2 Filtro, E3 Gatillo, E4 Freno (SSD adaptativo + SSRT), E5 Conflicto: generadores deterministas + mappings + payloads + clasificación + métricas de paradigma (efecto de congruencia, SSRT con detección de gaming). **Renderers/UI jugable diferidos a la fase de integración.** |
| 7 · Sesiones y periodización | ✅ | `SessionBlueprints` (5/10/20/30, fases que suman exacto), `TrainingCycles.STANDARD_8_WEEK` (deload en 4 y 8), `RecoveryPolicy` (fatiga + degradación RT-CV), `DailyPlanGenerator` determinista (semana/disponibilidad/recovery, deload reduce volumen+capa dificultad). |
| 8 · Personalización | ✅ | `ArchetypeInference` (arquetipo + confianza + secondGuess desde métricas), `PersonalizationProfiles` (pesos + `ScoringModifiers`: anti-velocidad para impulsivo, micro-pausa, penalización de errores/lapsos), `ProfileEvolution` (histéresis + transición suave sin saltar rango), `OvertrainingGuard` (fatiga crónica + tendencia a la baja → Caution/ForceRest). |
| 9 · Ejercicios avanzados (dominio) | ✅ | E6 Cambio, E7 Rastreo, E8 Dúo, E9 Enjambre (MOT), E10 Flujo (RSVP), E11 Ancla, E12 Re-enganche, E13 Presión: generadores deterministas + mappings + payloads + 5 métricas nuevas (switch cost, set-size slope, dual-task cost, resistencia, pressure delta). `TrialMarkers` bundle. **Renderers/UI jugable diferidos a integración.** |
| 10 · Reportes y transferencia (dominio) | ✅ | `FocusScore` (compuesto 0–100 con banda de incertidumbre), `TrendAnalyzer` (dirección según "más es mejor" por métrica), `TransferReportBuilder` (probes RCI = evidencia fuerte; intra-ejercicio secundario; cero transferencia lejana), `FunctionalProtocols` (microprotocolos etiquetados WEAK_SUBJECTIVE + disclaimer). |
| 11 · Testing fuerte y validación | ✅ | Simuladores (`UserSimulator` + 5 perfiles + `SimulatedLoop`) que cierran el lazo end-to-end; `SimulatorLoopTest` (impulsivo→ContainImpulsivity, avanzado→StepUp, lento-preciso, fatigado→lapsos, gamer-E4→SSRT no fiable); `MetricsEngineRegressionTest`, `AntiOscillationTest`, `ProbeContaminationTest`, `TimingTest`. |

## UI/UX avanzada entregada

- `FocusAtmosphere` — fondo ambiental con auroras que respiran (animación infinita).
- `FocusStimulusField` — campo de estímulo en canvas con transiciones suaves
  (idle/señal/lure/feedback), halos y núcleos animados.
- `FocusGauge` — medidor circular con estado de incertidumbre (muestra "—" si no fiable).
- `FocusHomeScreen` — hub premium: stats, tarjeta de ejercicio, selector de duración 5/10/20/30, CTA con gradiente.
- `FocusSessionScreen` — sesión en vivo tappable, HUD calmado, hints contextuales.
- `FocusSummaryScreen` — resumen honesto con revelado escalonado y nota de incertidumbre.
- Tokens en `design/AppColors.kt` (sección MindBoost). Cero `Color(0xFF…)` fuera del sistema.

## Honestidad científica respetada

- RT **mediano** y **RT-CV** como primarias (nunca media).
- Lapsos contados sobre RT crudos (no sobre los filtrados por MAD) — bug corregido y documentado.
- Toda `MetricValue` lleva `n` + `confident`; la UI nunca muestra una estimación sin potencia como hecho.
- La nota del resumen **nunca** promete transferencia lejana.
- El motor adaptativo **no** afirma "mejora real" (eso quedará a la evaluación con probes).

## Cómo probarlo

1. Compila y abre la app (Android o desktop).
2. Home → tarjeta **"MINDBOOST · ENFOQUE"**.
3. Elige duración → "Empezar" → "Estoy listo" → toca el destello verde, ignora el violeta.
4. Al terminar, ve el resumen honesto. El perfil/historial quedan persistidos (in-memory por ahora).

## Qué sigue (orden recomendado)

> **Las 11 fases numeradas del roadmap (doc 12) están completas a nivel de dominio.**
> Lo que resta es la **integración** (hacer jugable/visible el dominio) y la verificación en device.

> **Integración COMPLETA (2026-06-18): los 13 ejercicios de Enfoque son jugables en el menú.**
> Single-tap (E1 Vigía, E3 Gatillo, E7 Rastreo/búsqueda, E8 Dúo/dual-task, E9 Enjambre/MOT, E10
> Flujo/RSVP, E12 Re-enganche), elección LEFT/RIGHT (E2 Filtro, E5 Conflicto, E6 Cambio/task-switch,
> E11 Ancla/distracción, E13 Presión/deadline) y stop-signal (E4 Freno, SSD online + SSRT). El runtime
> captura PRIMARY/LEFT/RIGHT, presenta GO→STOP de dos fases (E4), aplica el deadline como ventana
> (E13) y anima MOT/RSVP/dual dentro de la ventana de respuesta; el ensamblador despacha
> `classifyStop`/`classifyChoice`/`classify` y propaga SSD + markers a `TrialResult`.
> `FocusExerciseGenerators.registry` (13) == `ExerciseCatalog.implemented` (13), con
> `pendingUiIntegration` = vacío; `ExerciseCatalogConsistencyTest` rompe el build si se desincronizan.
> Animaciones de E9/E10/E8 son **funcionales** (el movimiento/ráfaga ES la tarea), no decorativas.

1. **Integración — runner multi-ejercicio + UI** ✅ **COMPLETO (13/13)**: runner que soporta todas las
   modalidades (elección LEFT/RIGHT ✅, go/withhold ✅, stop-signal con SSD online ✅, set/stream para
   MOT/RSVP ✅), renderers Compose por ejercicio ✅, y propagación de marcadores en vivo ✅.
2. **Cableado de subsistemas al flujo** (4/4 — COMPLETO 2026-06-20):
   - ✅ `ProfileEvolution` re-infiere el arquetipo tras cada sesión (en `finishSession`); +
     `OvertrainingGuard` surge como banner Caution/ForceRest en el home.
   - ✅ `DailyPlanGenerator` dirige la recomendación del home (semana × disponibilidad × recovery),
     con nota de descanso/carga reducida.
   - ✅ `ProgressScreen` (FocusScore con banda de incertidumbre + tendencias por métrica; ruta
     Home→Progreso).
   - ✅ **F2-4: baseline/calibración + probe en `Mode.EVALUATION` + RCI (near-transfer).** Flujo de
     baseline (`FocusBaselineStateHolder`/`FocusBaselineScreen`) que corre el `BaselineProtocol` a
     dificultad fija vía el runner compartido `FocusFixedBlockRunner` y lo pliega con el pure
     `BaselineAssembler` (umbrales + dificultad inicial + `BaselineSnapshot`); retest de probe
     (`FocusEvaluationStateHolder`/`FocusEvaluationScreen`) en EVALUATION comparado por
     `FocusEvaluationService` → `EvaluationResult` + `TransferReport` con **RCI gated**
     (`ProbeReliabilityEstimator`); near-transfer en `ProgressScreen`. Repos `Local*` cifrados
     (`baseline`/`probe`) y `calibrationEngine` cableados al `FocusGraph`.
   - ✅ **Personalización completa por arquetipo — Fase 8 cerrada (2026-06-20):**
     `DefaultPersonalizedSessionScorer` (puro) puntúa la sesión doblando precisión/velocidad/estabilidad por
     `speedIncentive`/`errorPenalty`/`lapsePenalty`; `finishSession` lo adjunta a
     `FocusReport.personalizedScore` (mostrado en el resumen). `prePressureMicroPause` (impulsivo) traga
     taps precipitados vía `MicroPauseGate` **solo en TRAINING**. `exerciseWeights` → selección "debilidad
     primero" en `DefaultDailyPlanGenerator` (sort estable: neutro conserva el énfasis de la semana).
     `axisBias` → `ArchetypePersonalization.startingDifficulty` sesga la dificultad inicial en los ejes
     relevantes, solo como default (el valor calibrado/adaptado siempre gana).
3. **Verificación en device (riesgo A1)**: caracterizar la latencia de RT real en
   Android/iOS; verificar iOS (actuals) en macOS; persistencia cifrada en device.
4. **Gamificación honesta** (post-medición) y estudio interno de fiabilidad para activar RCI real.

## Fase 9 (Ejercicios avanzados, dominio) — componentes entregados (domain/exercise + domain/metrics)

| Componente | Rol |
|------------|-----|
| `TrialMarkers` (bundle) + accesores en `TrialResult` + `Stimulus.toMarkers()` | Marcadores de paradigma por trial (isSwitch, setSize, dualTask, underPressure, duringIntrusion); el ensamblador los propaga. |
| Payloads `TaskSwitch/Search/DualTask/MultiObjectTracking/RsvpStream/AnchorTask/Reengagement/Pressure` | Contenido lógico de los 8 paradigmas (render diferido). |
| `E6CambioGenerator` … `E13PresionGenerator` (+ mappings) | 8 generadores deterministas; cada uno lee solo sus ejes relevantes. |
| `DefaultMetricsEngine` (ampliado) | 5 métricas gobernantes nuevas (gated por `primaryMetrics`): switch cost (E6), set-size slope (E7), dual-task cost (E8), distraction-resistance (E11), pressure delta (E13). |
| E10 RSVP | Genera streams con T1/T2 a lag corto al subir la incertidumbre temporal (régimen del attentional blink). |

## Fase 10 (Reportes y transferencia, dominio) — componentes entregados (domain/reporting)

| Componente | Rol |
|------------|-----|
| `FocusScore` · `FocusScoreCalculator` · `DefaultFocusScoreCalculator` | Score compuesto 0–100 desde métricas honestas, con **banda de incertidumbre** que se estrecha con más datos; no `confident` con pocos datos. |
| `TrendDirection` · `BetterWhen` · `MetricDirection` · `TrendAnalyzer` · `DefaultTrendAnalyzer` | Tendencia por métrica interpretada en su dirección de mejora (accuracy↑ bien; RT-CV↑ mal). |
| `TransferLine` · `TransferReport` · `TransferReportBuilder` | Construye el reporte de near-transfer sobre `EvaluationResult`: probes con RCI fiable = **evidencia fuerte**; resto preliminar; nota honesta **sin transferencia lejana**. |
| `FunctionalContext` · `TransferStrength` · `FunctionalProtocol` · `FunctionalProtocols` | Microprotocolos funcionales (estudio, trabajo profundo, competición, lectura, decisión, distracción digital) **siempre WEAK_SUBJECTIVE + disclaimer**. |

## Fase 8 (Personalización) — componentes entregados (domain/personalization)

| Componente | Rol |
|------------|-----|
| `ArchetypeSignals` · `ArchetypeInference` · `DefaultArchetypeInference` · `ArchetypeSignalsFactory` | Infiere arquetipo (impulsivo, lento-preciso, baja concentración, avanzado, élite…) por scoring de señales normalizadas + confianza × madurez de datos. |
| `ScoringModifiers` · `PersonalizationProfile` · `PersonalizationProfiles` | Por arquetipo: pesos de ejercicio, `axisBias`, scoring (impulsivo NO premia velocidad + micro-pausa; rápido-impreciso penaliza errores; baja-concentración penaliza lapsos) y plantilla preferida. |
| `ProfileEvolution` · `DefaultProfileEvolution` | Re-inferencia con **histéresis** (umbral de confianza para cambiar) y **transición suave** (nunca salta de rango: BEGINNER→ELITE pasa por ADVANCED); reloj inyectado. |
| `OvertrainingVerdict` · `OvertrainingGuard` · `DefaultOvertrainingGuard` | Anti-sobreentrenamiento: fatiga crónica o variabilidad↑ + precisión↓ → `ForceRest`; tendencia a la baja → `Caution`. |

## Fase 7 (Sesiones y periodización) — componentes entregados (domain/session)

| Componente | Rol |
|------------|-----|
| `SessionPhase` · `PhaseSlot` · `SessionBlueprint` · `SessionBlueprints` | Desglose de fases por plantilla; invariante: suman **exacto** la duración. |
| `Volume` · `EvaluationRequirement` · `WeekPlan` · `TrainingCycle` · `TrainingCycles.STANDARD_8_WEEK` | Ciclo de 8 semanas con deload en 4 y 8 (retest FULL). |
| `RecoveryGate` · `RecoveryPolicy` · `DefaultRecoveryPolicy` | Anti-sobreentrenamiento: fatiga sostenida + degradación de RT-CV vs baseline → reducir/descansar. |
| `PlannedBlock` · `DailyFocusPlan` · `DailyPlanGenerator` · `DefaultDailyPlanGenerator` | Plan diario determinista: semana × disponibilidad × recovery; deload reduce volumen y capa dificultad. |
| `SessionTemplate.forMinutes(...)` | Selección de plantilla por minutos disponibles. |

## Fase 6 (Ejercicios núcleo, dominio) — componentes entregados (domain/exercise + domain/metrics)

| Componente | Rol |
|------------|-----|
| `StimulusPayload.{Flanker,GoNoGo,StopSignal,Conflict}` + `Stimulus.congruentFlag()` | Contenido lógico de los 4 paradigmas; congruencia expuesta al motor de métricas. |
| `TrialClassifier.classifyChoice(...)` | Clasificación de tareas de elección (E2/E5): clave correcta→HIT, incorrecta→FALSE_ALARM. |
| `E2FiltroGenerator` + `E2DifficultyMapping` | Flanker; proporción incongruente ← INTERFERENCE. |
| `E3GatilloGenerator` + `E3DifficultyMapping` | Go/no-go; prepotencia (goProportion) ← INHIBITORY_DEMAND. |
| `E4FrenoGenerator` + `E4DifficultyMapping` + `StopSignalTracker` | Stop-signal; **SSD 1-up/1-down online** convergiendo a p(stop)≈0.5. |
| `E5ConflictoGenerator` + `E5DifficultyMapping` | Stroop/Simon; proporción incongruente ← INTERFERENCE. |
| `DefaultMetricsEngine` (ampliado) | Efecto de congruencia (E2/E5) y SSRT por integración (E4, con flag de gaming), **gated** por `definition.primaryMetrics`. |
| `TrialResult.congruent` (campo nuevo) + `FocusSessionAssembler` paradigm-aware | El ensamblador propaga la congruencia desde el estímulo. |

## Fase 10 (Persistencia) — componentes entregados (data/local, data/dto, data/mapper)

| Componente | Rol |
|------------|-----|
| `SecureKeyValueStore` (expect) + `InMemorySecureKeyValueStore` | Superficie cripto/plataforma mínima; in-memory para tests/desktop/iOS. |
| `SecureKeyValueStore.android.kt` | **AES-256-GCM + Android Keystore** (clave device-bound); cifrado en reposo. |
| `FocusDtos.kt` (`@Serializable`, `schemaVersion`) + `FocusPersistenceMappers.kt` | DTOs versionados ⇄ dominio; sesiones = **resumen** (sin trials crudos). |
| `FocusJson` | `Json { ignoreUnknownKeys; encodeDefaults }` — forward-compat. |
| `LocalFocusProfileRepository` · `LocalSessionHistoryRepository` · `LocalBaselineRepository` · `LocalProbeBankRepository` | Implementan los contratos de dominio sobre el store cifrado; probes separados del historial. |
| `rememberFocusGraph()` | Producción ahora usa los repos `Local*` cifrados (perfil + historial). |

## Fase 4 — componentes entregados (domain/evaluation)

| Componente | Rol |
|------------|-----|
| `AdaptiveModeGate` | Enforce: la adaptación SOLO ocurre en `Mode.TRAINING`; EVALUATION/CALIBRATION devuelven dificultad fija. |
| `ProbeDefinition` · `ProbeSeedSpace` · `SeedPolicy` | Probes reservados con formas paralelas en banda de semillas disjunta del entrenamiento. |
| `ProbeBank` · `InMemoryProbeBankRepository` | Banco curado (vigilancia/selectiva/inhibición) + scheduler con rotación de formas y almacenamiento separado del historial. |
| `BaselineProtocol` · `BaselineProtocols.INITIAL` · `StateQuestionnaire` | Baseline fijo 15–20 min: 3 bloques de calibración + 1 probe reservado + cuestionario de estado. |
| `CalibrationEngine` · `DefaultCalibrationEngine` | Umbral de lapso individualizado, RT-CV basal, dificultad inicial (~0.80 accuracy), `BaselineSnapshot`. |
| `ReliableChangeIndex` · `RciResult` · `EvaluationComparator` | RCI (Jacobson & Truax) **gated** por fiabilidad; comparación baseline↔retest. |

## Notas técnicas / decisiones del slice

- Timing RT: `kotlin.time.TimeSource.Monotonic` vía `MonotonicClock` inyectable
  (sin expect/actual, multiplataforma). Verificación de precisión real en device pendiente (riesgo A1).
- El warmup/cooldown del slice usan transiciones cortas (4s/2.5s) en vez de los
  30s nominales del diseño, para una UX ágil; el núcleo respeta la duración por trials.
- `FocusSessionStateHolder` recibe el `FocusGraph` (no 8 deps sueltas).
- Persistencia: **local-first cifrada**. Android = AES-256-GCM con clave en Android
  Keystore (device-bound); desktop/iOS = in-memory (no escriben datos sensibles a
  disco; iOS Keychain diferido). Solo se persisten resúmenes de sesión, nunca trials crudos.
- Dependencia añadida (justificada): `kotlinx-serialization-json` 1.9.0 (el `-core`
  presente no produce formato string). Sin más dependencias.

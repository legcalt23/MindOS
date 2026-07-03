# Handoff — MindBoost · Enfoque (continúa el plan aquí)

> Punto de retoma para un chat nuevo. Rama: **`feat/mindboost-enfoque-integration`**.
> Lee primero `IMPLEMENTATION_STATUS.md` (§"Qué sigue") y `13_claude_code_execution_prompts.md`
> en esta misma carpeta, y los aprendizajes de `CLAUDE.md` (sección "Aprendizajes acumulados").

## 1. Dónde estás
El plan de **MindBoost · Enfoque** (`shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus`)
tiene las 11 fases de dominio completas. El trabajo reciente fue la **integración a presentación**:

- **Fase de Integración (runner + renderers): COMPLETA — los 13 ejercicios (E1–E13) son jugables.**
  - Single-tap (E1, E3, E7 búsqueda, E8 dual-task, E9 MOT, E10 RSVP, E12), elección LEFT/RIGHT
    (E2, E5, E6 task-switch, E11 ancla, E13 presión), stop-signal (E4, SSD online + GO→STOP).
  - Animaciones de E9/E10/E8 son **funcionales** (movimiento/ráfaga/carga = la tarea), no decorativas.
- **Fase 2 (cableado de subsistemas): 4/4 hecho — COMPLETA.**
  - ✅ F2-1 `ProfileEvolution` (re-infiere arquetipo tras cada sesión) + `OvertrainingGuard` (banner).
  - ✅ F2-2 `DailyPlanGenerator` dirige la recomendación del home.
  - ✅ F2-3 `ProgressScreen` (FocusScore con banda + tendencias; ruta Home→Progreso).
  - ✅ **F2-4 (2026-06-20):** flujo de **baseline** (calibración + probe reservado) → umbrales +
    dificultad inicial + `BaselineSnapshot`; **retest de probe en `Mode.EVALUATION`** (dificultad fija)
    → `EvaluationResult` + `TransferReport` con **RCI gated**; near-transfer en `ProgressScreen`.

Estado de tests tras F2-4: `:shared:desktopTest` = **1253 testcases, 0 failures** (+26 vs handoff;
228 en el feature de Enfoque); `:shared:detekt` limpio; `:shared:compileAndroidMain` OK.

## 2. F2-4 — COMPLETADO (Baseline/EVALUATION + probe + RCI)
Lo entregado (la pieza mayor de la Fase 2):
1. ✅ Cableado al `FocusGraph`: `baselineRepository`/`probeBankRepository` (encriptados `Local*`),
   `calibrationEngine`, `baselineProtocol`, `baselineAssembler`, `evaluationService`.
2. ✅ Flujo de **baseline** (`FocusBaselineStateHolder` + `FocusBaselineScreen`): corre los bloques del
   `BaselineProtocol` a dificultad fija (calibración en CALIBRATION, probe en EVALUATION) vía el runner
   compartido `FocusFixedBlockRunner`, y el **pure** `BaselineAssembler` deriva umbrales individuales +
   dificultad inicial por ejercicio + `BaselineSnapshot`; persiste perfil/baseline y registra el probe
   aparte del historial. Home rutea a baseline cuando `needsBaseline`.
3. ✅ **Retest de probe en `Mode.EVALUATION`** (`FocusEvaluationStateHolder` + `FocusEvaluationScreen`):
   reserva-probe a dificultad fija (nunca adapta), comparado contra el baseline por `FocusEvaluationService`
   → `EvaluationResult` + `TransferReport`. **RCI gated** vía `ProbeReliabilityEstimator` (fuente vacía por
   defecto → nunca afirma "cambio fiable" sin un estudio de fiabilidad; sólo deltas preliminares).
4. ✅ **Near-transfer** en `ProgressScreen` desde el último retest posterior al baseline.

APIs reutilizadas (no reinventadas): `domain/evaluation/{BaselineProtocol,CalibrationEngine,
DefaultCalibrationEngine,ProbeBank,ProbeDefinition,EvaluationResult,EvaluationComparator,
ReliableChangeIndex,AdaptiveModeGate}.kt`. Nuevo dominio puro: `BaselineAssembler`,
`ProbeReliabilityEstimator`/`ReliabilitySource`, `FocusEvaluationService`, `ProbeExerciseMapping`.

Notas/limitaciones honestas de F2-4 (no bloqueantes):
- El retest mide el **constructo de vigilancia** (probe week-1), igual que el baseline, para que pre/post
  comparen el mismo constructo. Retests multi-capacidad (selectiva/inhibición) quedan como follow-up.
- El **cuestionario de estado** (`StateQuestionnaire`: sueño/cafeína/ánimo/hora) está modelado en dominio
  pero su captura UI se difiere (se pasa `SubjectiveState? = null`; covariable, no afecta al RCI).
- `deviceFingerprint` se pasa `null` (riesgo A1: RT sólo comparable en el mismo device — pendiente).
- RCI permanece **gated** por diseño hasta el estudio interno de fiabilidad.

## 2.bis — ScoringModifiers por arquetipo (2026-06-20) — HECHO
Cerrado el gap que estaba pendiente: los `ScoringModifiers` de `PersonalizationProfiles` ya **se aplican**
al flujo real.
- `DefaultPersonalizedSessionScorer` (puro): puntúa la sesión (0–100) combinando precisión + velocidad
  (RT mediano) + estabilidad (RT-CV) como media ponderada **doblada por los modificadores** (impulsivo
  `speedIncentive 0.6` → premia menos la prisa; `errorPenalty>1` → los errores pesan más; `lapsePenalty>1`
  → resta más por lapsos). Gated por confianza; caption no diagnóstico.
- `FocusSessionStateHolder.finishSession` calcula el score con el arquetipo activo de la sesión y lo
  adjunta a `FocusReport.personalizedScore`; `FocusSummaryScreen` lo muestra como "Rendimiento
  personalizado".
- `prePressureMicroPause` (impulsivo): `MicroPauseGate` traga los taps anteriores a una ventana breve
  (~200 ms) en cada onset — **solo en TRAINING**; baseline/evaluación (medición honesta) no lo aplican.

## 2.ter — Fase 8 completa: axisBias + exerciseWeights (2026-06-20)
Aplicadas las dos palancas restantes de personalización, cerrando la **Fase 8**:
- **`exerciseWeights` (selección "debilidad primero"):** `DefaultDailyPlanGenerator` re-rankea el pool de la
  semana por los pesos del arquetipo con un **sort estable** → pesos iguales conservan el orden de énfasis
  de la periodización (un perfil neutro planifica exactamente el orden de la semana, sin regresión), y un
  impulsivo coloca E3/E4 primero manteniendo variedad mínima (round-robin sobre el ranking).
- **`axisBias` (sesgo de dificultad inicial):** `ArchetypePersonalization.startingDifficulty` aplica el
  sesgo del arquetipo a los ejes **relevantes** del ejercicio, pero **solo como default** (cuando no hay
  valor calibrado/adaptado guardado; el valor empírico siempre gana — no se apila heurística sobre medición).
  Usado en `FocusSessionStateHolder` (dificultad efectiva) y en el `PlannedBlock` del plan diario.
Tests: **1269 (+16 desde F2-4), 0 fallos**; detekt limpio; Android compila. La **Fase 8 queda cerrada**:
arquetipo, evolución, sobreentrenamiento, scoring, micro-pausa, axisBias y exerciseWeights, todos aplicados.

## 3. Cómo se trabaja este plan (patrones obligatorios)
- **Una fuente de verdad de ejercicios expuestos:** `FocusExerciseGenerators.registry` debe ser igual
  a `ExerciseCatalog.implemented`; `ExerciseCatalogConsistencyTest` rompe el build si se desincronizan
  o si `pendingUiIntegration` no cuadra. NUNCA exponer un id sin generador + renderer + métrica.
- **Runtime de una sola respuesta:** el holder presenta un campo y espera UNA respuesta. Paradigmas
  temporales/dinámicos (MOT/RSVP/dual) meten su animación ENTERAMENTE en el composable (keyed por
  `layoutSeed = stimulus.index`); el holder sigue capturando una pulsación. Clasificación en
  `TrialClassifier` (classify / classifyChoice / classifyStop) y el **ensamblador** es la autoridad.
- **Honestidad:** nunca mostrar la respuesta ni datos fabricados; si un paradigma exige derivar la
  respuesta de un estímulo (task-switch), enriquecer el payload de dominio. No animaciones decorativas.
- **Disciplina de ejecución (la pidió el usuario):** ejecutar prompts en orden, test tras cada uno,
  detenerse y corregir si algo falla, no avanzar de fase, no hacer trabajo de fases futuras.
- **Detekt ratchet:** no regenerar baseline. Arreglar findings (envolver líneas largas; `ReturnCount`
  ≤2 partiendo funciones; pantallas Compose con muchos composables usan `@file:Suppress("…",
  "TooManyFunctions")` como las hermanas). `cos/sin` de `kotlin.math` devuelven `Double` → `.toFloat()`.

## 4. Verificación (Windows: el daemon de Gradle es inestable — usar daemonless)
```
./gradlew :shared:compileKotlinDesktop --no-daemon --no-build-cache
./gradlew :shared:desktopTest --tests "com.mindos.app.features.mindboost.focus.*" --no-daemon --no-build-cache
./gradlew :shared:detekt :shared:compileAndroidMain --no-daemon --no-build-cache
# Conteo total fiable (el daemon "fija" filtros --tests entre runs):
./gradlew --stop; ./gradlew :shared:cleanDesktopTest :shared:desktopTest --no-daemon --no-configuration-cache --no-build-cache
```

## 5. Aviso sobre el árbol de trabajo
Esta rama hereda ~150 cambios sin commitear **ajenos a este plan** (command-router de voz,
`functions/`, `mindos/`, docs de seguridad) que venían de `codex/auditoria-command-router`. NO forman
parte del plan de Enfoque; este commit incluye **solo** `features/mindboost/focus/**`, sus tests,
`CLAUDE.md` y `docs/mindboost_enfoque_implementation_plan/**`.

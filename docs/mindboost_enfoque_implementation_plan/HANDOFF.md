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
- **Fase 2 (cableado de subsistemas): 3/4 hecho.**
  - ✅ F2-1 `ProfileEvolution` (re-infiere arquetipo tras cada sesión) + `OvertrainingGuard` (banner).
  - ✅ F2-2 `DailyPlanGenerator` dirige la recomendación del home.
  - ✅ F2-3 `ProgressScreen` (FocusScore con banda + tendencias; ruta Home→Progreso).
  - ⏳ **F2-4 (PENDIENTE, próximo):** baseline/calibración + probe en modo EVALUATION + RCI.

Estado de tests al hacer el handoff: `:shared:desktopTest` = **1227 testcases, 0 failures**;
`:shared:detekt` limpio; `:shared:compileAndroidMain` OK.

## 2. Próximo prompt — F2-4 (Baseline/EVALUATION + probe + RCI)
Es la pieza mayor de la Fase 2 (nuevo modo + flujo). Objetivo:
1. Cablear al `FocusGraph`: `BaselineRepository`, `ProbeBankRepository`, `CalibrationEngine`
   (`DefaultCalibrationEngine`), `BaselineProtocol` (ya existen en `domain/evaluation` + `data/`).
2. Cuando `profile.baseline == null` (el home ya expone `needsBaseline`), un flujo de **baseline**:
   corre `BaselineProtocol` (bloques de calibración + un probe reservado), `CalibrationEngine` deriva
   umbrales individuales + dificultad inicial + `BaselineSnapshot`, y se persiste.
3. Sesiones de **probe en `Mode.EVALUATION`**: `AdaptiveModeGate` mantiene la dificultad fija; producen
   `EvaluationResult`; comparar pre/post con `ReliableChangeIndex` (RCI **gated**: nunca afirmar mejora
   sin evidencia).
4. Mostrar **near-transfer** en `ProgressScreen` (hoy dice explícitamente que requiere probes).

APIs ya existentes (no reinventar): `domain/evaluation/{BaselineProtocol,CalibrationEngine,
DefaultCalibrationEngine,ProbeBank,ProbeDefinition,EvaluationResult,ReliableChangeIndex,
AdaptiveModeGate}.kt`. Repos: `data/InMemoryProbeBankRepository` / `data/local/*`.

Sub-tarea adicional detectada (no bloqueante): los `ScoringModifiers` por arquetipo
(`PersonalizationProfiles`) **aún no se aplican** al scoring de la sesión (F2-1 sólo cableó la
evolución del arquetipo + sobreentrenamiento). Es wiring más profundo (toca métricas/sesión).

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

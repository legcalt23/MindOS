# 06 · Plan de Evaluación y Baseline

> **Fase 4.** La columna vertebral científica honesta. Separa **entrenar** de
> **medir mejora**. La mejora real solo se afirma con **probes no entrenados** en
> dificultad fija, vía RCI. Esta separación es una restricción dura del producto.

## 0. Los tres modos (separación crítica)

```kotlin
enum class Mode { TRAINING, EVALUATION, CALIBRATION }
```

| Modo | Dificultad | Adaptación | Objetivo | Datos que alimenta |
|------|-----------|------------|----------|--------------------|
| **CALIBRATION** | barrido controlado | no (busca umbral) | hallar baseline individual, fijar umbrales y dificultad inicial | `IndividualThresholds`, `startingDifficulty` |
| **TRAINING** | adaptativa (vector) | **sí** (doc 04) | aprendizaje / mejora de control | historial, perfil vivo |
| **EVALUATION** | **fija** | **no** | medir cambio real con probes reservados | `BaselineSnapshot`, retests, RCI |

**Invariante de código (testeable):** `AdaptiveEngine.decide` solo se invoca si
`mode == TRAINING`. En EVALUATION/CALIBRATION la dificultad es constante en todo
el bloque. Añadir un test que falle si se adapta en EVALUATION.

## 1. Probes reservados (anti-contaminación)

Un **probe** es una tarea de evaluación que **NUNCA se usa para entrenar**. Si se
entrenara, mejorar en él reflejaría aprendizaje de la tarea, no transferencia.

```kotlin
data class ProbeDefinition(
    val probeId: String,
    val capacity: Capacity,
    val paradigm: Paradigm,
    val fixedDifficulty: DifficultyVector,   // SIEMPRE la misma para comparar pre/post
    val fixedSeedPolicy: SeedPolicy,         // semillas rotadas de un banco reservado
    val isReserved: Boolean = true,          // jamás aparece en TRAINING
)

enum class SeedPolicy { FIXED, ROTATED_RESERVED }  // rotar entre forms paralelas equivalentes
```

**Reglas anti-contaminación (obligatorias):**
1. El banco de probes (`ProbeBankRepository`) es **disjunto** del banco de
   ejercicios de entrenamiento. Test: intersección vacía de IDs/seeds.
2. Probes usan **formas paralelas** (mismo nivel, distinto contenido) rotadas, no
   el mismo trial repetido (evita memorización ítem-específica).
3. Un probe administrado se marca "usado" para esa ventana; se rota la siguiente.
4. El motor adaptativo **no ve** resultados de probe para decidir dificultad de
   training (canales separados).

## 2. Baseline inicial (15–20 min)

Secuencia fija, dificultad fija, **sin adaptación**:

```kotlin
data class BaselineProtocol(
    val blocks: List<BaselineBlock> = listOf(
        BaselineBlock(Capacity.SUSTAINED_VIGILANCE, ExerciseId.E1_VIGIA, fixed),   // ~4-5 min
        BaselineBlock(Capacity.SELECTIVE_FILTERING, ExerciseId.E2_FILTRO, fixed),  // selectiva/interferencia
        BaselineBlock(Capacity.RESPONSE_INHIBITION, ExerciseId.E3_GATILLO, fixed), // inhibición
        BaselineBlock(/* probe NO entrenado */, reservedProbe = true),             // transferencia
    ),
    val stateQuestionnaire: StateQuestionnaire,  // antes de empezar
)

data class BaselineSnapshot(
    val userId: String,
    val perMetric: MetricSet,                 // valores basales por métrica clave
    val capturedAtMs: Long,
    val state: SubjectiveState,               // sueño, cafeína, ánimo, hora
    val deviceFingerprint: String,            // mismo device para comparar RT (riesgo A1)
)
```

### Cuestionario de estado (covariables, no diagnóstico)
```kotlin
data class StateQuestionnaire(
    val askSleepHours: Boolean = true,
    val askCaffeine: Boolean = true,
    val askMood: Boolean = true,
    val captureLocalHour: Boolean = true,     // circadiano
)
```
Estos datos contextualizan la varianza (no afirmar causalidad). Útiles para
interpretar un mal día sin penalizar el perfil.

## 3. Calibración (umbrales individuales)

Antes/durante el baseline, un barrido tipo escalera (staircase) ajusta:
- `lapseRtMs` individual (no usar 500 ms fijo si el RT basal del usuario es muy
  distinto): p. ej. `lapseRtMs = max(500, mediana_basal + k·MAD)`.
- `startingDifficulty[exerciseId]`: nivel donde el usuario ronda accuracy ~0.80
  (zona de aprendizaje productiva, no frustrante ni trivial).
- `baselineRtCv`: para detectar subidas relativas en adaptación/fatiga.

```kotlin
interface CalibrationEngine {
    fun calibrate(samples: List<TrialResult>): IndividualThresholds
    fun initialDifficulty(exercise: ExerciseDefinition, samples: List<TrialResult>): DifficultyVector
}
```

## 4. Retests

| Tipo | Cuándo | Duración | Contenido |
|------|--------|----------|-----------|
| Ligero | semanal | 3–4 min | 1–2 probes rotados; señal de tendencia |
| Completo | semana 4 y 8 (deload) | 15–20 min | protocolo baseline completo + probes |

Resultado de retest → `EvaluationResult` → comparación con baseline → (cuando hay
fiabilidad) RCI.

## 5. RCI (Reliable Change Index) — preparado, no prematuro

```kotlin
data class ReliabilityEstimate(val metric: MetricKey, val reliability: Double, val sdPre: Double)

fun rci(pre: Double, post: Double, r: ReliabilityEstimate): RciResult {
    if (r.reliability <= 0.0) return RciResult.NotEnoughData
    val sem = r.sdPre * sqrt(1.0 - r.reliability)
    val seDiff = sqrt(2.0) * sem
    val rci = (post - pre) / seDiff
    return RciResult.Value(rci, reliable = abs(rci) > 1.96)
}
sealed interface RciResult { data object NotEnoughData : RciResult
    data class Value(val rci: Double, val reliable: Boolean) : RciResult }
```
**Gate:** no mostrar "mejora fiable" hasta tener `reliability` estimada (≥2–3
administraciones de probe). Antes: mostrar delta crudo con etiqueta de
incertidumbre ("tendencia preliminar, aún no concluyente").

## 6. Qué se puede y qué NO se puede afirmar

| Afirmación | Permitida |
|-----------|-----------|
| "Subiste de nivel en E1" | ✅ (es un hecho del entrenamiento) |
| "Tu RT-CV en el probe bajó de forma fiable (RCI>1.96)" | ✅ (con fiabilidad) |
| "Mejoraste tu control atencional" (transferencia cercana, con probes) | ⚠️ con cautela, banda de incertidumbre |
| "Mejoró tu CI / inteligencia / atención en la vida diaria" | ❌ NUNCA (transferencia lejana no probada) |

## 7. Criterios de aceptación (Fase 4)

- [ ] `Mode` enforced: test que falla si se adapta en EVALUATION/CALIBRATION.
- [ ] Banco de probes disjunto del de training (test de intersección vacía).
- [ ] Baseline protocol fijo de 15–20 min con cuestionario de estado.
- [ ] `CalibrationEngine` produce umbrales y dificultad inicial individuales.
- [ ] RCI implementado pero **gated** por disponibilidad de `reliability`.
- [ ] Ninguna ruta de código afirma transferencia lejana.

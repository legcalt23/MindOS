# 03 · Plan del Motor de Métricas

> **Fase 2.** Kotlin puro en `domain/metrics/`. Determinista, sin tiempo real,
> sin plataforma. La honestidad científica vive aquí: **RT mediano (no media),
> RT-CV, lapsos con umbral individual, separación de tipos de error**.

## 0. Contrato general

```kotlin
package com.mindos.app.features.mindboost.focus.domain.metrics

enum class MetricKey {
    ACCURACY, RT_MEDIAN, RT_CV, LAPSES, FALSE_ALARMS, OMISSIONS,
    POST_ERROR_SLOWING, POST_ERROR_ACCURACY, BLOCK_STABILITY, FATIGUE_INDEX,
    INHIBITORY_CONTROL_INDEX, SUSTAINED_FOCUS_INDEX, NEAR_TRANSFER_INDEX,
    IES, RATE_CORRECT_SCORE, LISAS, SSRT, CONGRUENCY_EFFECT, SWITCH_COST,
    SET_SIZE_SLOPE, DUAL_TASK_COST, DISTRACTION_RESISTANCE_INDEX,
    PRESSURE_DELTA, VIGILANCE_DECREMENT_SLOPE, ANTICIPATIONS,
}

/** Una métrica calculada: valor + metadatos de confianza. */
data class MetricValue(
    val key: MetricKey,
    val value: Double,
    val n: Int,                 // tamaño muestral que la sustenta
    val confident: Boolean,     // false si n insuficiente → no mostrar como hecho
    val unit: MetricUnit,
)
enum class MetricUnit { PROPORTION, MILLISECONDS, RATIO, COUNT, SLOPE, Z_SCORE, INDEX }

typealias MetricSet = Map<MetricKey, MetricValue>

/** Punto de entrada del motor. Sin estado, función pura. */
interface MetricsEngine {
    fun blockMetrics(trials: List<TrialResult>, def: ExerciseDefinition,
                     thresholds: IndividualThresholds): MetricSet
    fun sessionMetrics(blocks: List<BlockResult>): MetricSet
}
```

### Umbrales individuales

```kotlin
/** Umbrales calibrados por usuario (doc 06). Con defaults poblacionales seguros. */
data class IndividualThresholds(
    val lapseRtMs: Long = 500,            // RT por encima del cual cuenta lapso (PVT clásico)
    val anticipationFloorMs: Long = 150,  // RT por debajo = anticipación (no fisiológico)
    val baselineRtCv: Double? = null,     // RT-CV basal del usuario (para detectar subidas)
)
```

## 1. Pre-procesado: limpieza de RT y outliers (CRÍTICO)

Antes de cualquier métrica de RT, aplicar en orden:

1. **Filtro de anticipaciones:** `RT < anticipationFloorMs` (150 ms) → outcome
   `ANTICIPATION`, NO cuenta como hit válido. Se reporta aparte.
2. **Filtro de techo:** `RT > windowMs` → `TIMEOUT`/`MISS`.
3. **Outliers estadísticos:** método **MAD (Median Absolute Deviation)**, no SD
   (robusto a colas). Excluir RT con `|RT − mediana| / (1.4826·MAD) > 3.0`.
   - Documentar cuántos se excluyeron (`n` refleja la muestra limpia).
4. **Solo RT de aciertos** (`HIT`) entran en RT mediano/CV. Errores y omisiones
   tienen su propia contabilidad.

```kotlin
internal fun cleanReactionTimes(
    trials: List<TrialResult>, t: IndividualThresholds,
): List<Long> {
    val hits = trials.filter { it.outcome == TrialOutcome.HIT && it.reactionTime != null }
        .map { it.reactionTime!!.value }
        .filter { it in t.anticipationFloorMs..Long.MAX_VALUE }
    if (hits.size < 4) return hits           // n bajo: no filtrar por MAD (no fiable)
    val med = median(hits.map { it.toDouble() })
    val mad = median(hits.map { kotlin.math.abs(it - med) }) * 1.4826
    if (mad == 0.0) return hits
    return hits.filter { kotlin.math.abs(it - med) / mad <= 3.0 }
}
```

## 2. Métricas base

### Precisión (Accuracy)
`HIT + CORRECT_REJECTION` / total trials válidos (excluye anticipaciones).
```kotlin
accuracy = (hits + correctRejections).toDouble() / validTrials
```
Para tareas con base rate desigual (go/no-go), reportar **también** sensibilidad
no paramétrica (ver §Inhibición: SDT `A'`/`d'`).

### RT mediano (métrica primaria de velocidad — **NO media**)
```kotlin
rtMedian = median(cleanReactionTimes(...))  // robusto a colas largas típicas de atención
```
La media está sesgada por lapsos; la mediana es la métrica honesta.

### RT-CV (coeficiente de variación = estabilidad atencional)
```kotlin
rtCv = standardDeviation(cleanRts) / median(cleanRts)   // SD/mediana, no SD/media
```
RT-CV ↑ = atención más errática. **Métrica clave de fatiga/dispersión.** Nunca
ignorar (restricción del producto).

### Lapsos
```kotlin
lapses = cleanRts.count { it > thresholds.lapseRtMs }   // umbral individual
lapseRate = lapses.toDouble() / hits
```

### Falsas alarmas / Omisiones / Anticipaciones
```kotlin
falseAlarms = trials.count { it.outcome == FALSE_ALARM }   // comisiones
omissions   = trials.count { it.outcome == MISS }
anticipations = trials.count { it.outcome == ANTICIPATION }
```

## 3. Métricas de error secuencial

### Post-error slowing (PES) — método robust
```kotlin
// Emparejar: trial posterior a error vs trial posterior a acierto (robust PES, Dutilh 2012)
pes = meanRt(postErrorCorrectTrials) - meanRt(postCorrectCorrectTrials)
```
PES > 0 esperado (adaptación). PES muy alto + accuracy baja = espiral de error
(relevante para E12, doc 04).

### Post-error accuracy
```kotlin
postErrorAccuracy = accuracyOf(trials immediately after an error)
```

## 4. Métricas compuestas de eficiencia

Combinan velocidad y precisión. Usar la apropiada según el ejercicio.

### IES — Inverse Efficiency Score
```kotlin
ies = rtMedian / accuracy          // ms por unidad de acierto; menor = mejor
// Edge: accuracy == 0 → IES indefinido → MetricValue(confident=false)
```

### RCS — Rate-Correct Score
```kotlin
rcs = correctResponses.toDouble() / sumOfRtSecondsAcrossAllTrials  // aciertos/seg
```

### LISAS — Linear Integrated Speed-Accuracy Score
```kotlin
// LISAS = RT_mean + (SD_RT / SD_PE) * PE   (Vandierendonck 2017)
// PE = proportion of errors; SD_PE = SD de la tasa de error del participante
lisas = rtMean + (sdRt / sdPe) * proportionErrors
```
LISAS pondera RT y errores en la misma escala; preferido cuando hay trade-off
velocidad-precisión (E13).

## 5. Métricas específicas por paradigma

### SSRT — Stop-Signal Reaction Time (E4, inhibición reactiva)
Método de **integración** (Verbruggen et al. 2019, consenso):
```kotlin
// 1. Ordenar TODOS los go-RT (incluye fallos de parada como go-RT).
// 2. p(respond|signal) = nStopFailures / nStopTrials
// 3. nth = round(p(respond|signal) * nGoRTs); el RT en esa posición = nthRT
// 4. SSRT = nthRT - meanSSD
ssrt = goRtSorted[(pRespond * goRtSorted.size).roundToInt() - 1] - meanSsd
```
**Validez:** si el usuario ralentiza a propósito (p(respond|signal) ≈ 0.5
esperado; si se aleja mucho de 0.5, o go-RT crece con el bloque → estrategia de
gaming). Marcar `confident=false` y señalar a adaptación (doc 04 "contener
impulsividad / detectar gaming").

### Efecto de congruencia (E2, E5 — flanker/Stroop/Simon)
```kotlin
congruencyEffect = rtMedian(incongruent) - rtMedian(congruent)  // ms
// también en accuracy: accDelta = acc(congruent) - acc(incongruent)
```

### Switch cost (E6 — task switching)
```kotlin
switchCost = rtMedian(switchTrials) - rtMedian(repeatTrials)
mixingCost = rtMedian(repeatTrialsInMixedBlock) - rtMedian(pureBlock)
```

### Set-size slope (E7 — visual search)
```kotlin
// Regresión lineal RT ~ setSize. La pendiente (ms/ítem) = eficiencia de búsqueda.
setSizeSlope = linearRegressionSlope(points = setSizes zip rtMediansBySetSize)
```

### Dual-task cost (E8)
```kotlin
dualTaskCost = performanceSingle - performanceDual   // por tarea; balance entre ambas
```

### Vigilance decrement slope (E1 — PVT/Mackworth)
```kotlin
// Pendiente del rendimiento (accuracy o 1/RT) a lo largo del tiempo-en-tarea.
vigilanceDecrementSlope = linearRegressionSlope(timeBins zip performanceByBin)
```

### Distraction-resistance index (E11)
```kotlin
resistanceIndex = 1.0 - (performanceDropDuringIntrusion / baselinePerformance)
intrusionInteractions = count(taps on intruder elements)
```

### Pressure delta (E13)
```kotlin
pressureDelta = accuracy(calm) - accuracy(deadline)   // cuánto cae bajo presión
```

## 6. Índices de alto nivel (para reporte al usuario)

Compuestos normalizados 0..100, **siempre con banda de incertidumbre**:

- **SustainedFocusIndex** ← f(lapseRate↓, rtCv↓, vigilanceDecrementSlope→0).
- **InhibitoryControlIndex** ← f(falseAlarms↓, SSRT↓, PES razonable).
- **FatigueIndex** ← f(rtCv↑ intra-sesión, lapses↑ por bloque, decremento).
- **NearTransferIndex** ← cambio en **probes no entrenados** (doc 06) — la única
  evidencia honesta de mejora real.

```kotlin
data class FocusReport(
    val indices: Map<MetricKey, MetricValue>,   // cada uno con confident + n
    val uncertaintyNote: String,                // texto honesto mostrado al usuario
)
```

## 7. Métricas por nivel (block / session / cycle)

- **Block:** todas las base + las del paradigma del ejercicio.
- **Session:** agregación ponderada por trials; `FatigueIndex` intra-sesión
  (comparar primer tercio vs último tercio de bloques).
- **Cycle (8 semanas):** tendencias; **RCI** (Reliable Change Index) cuando exista
  fiabilidad test-retest suficiente.

### RCI — Reliable Change Index (doc 06)
```kotlin
// RCI = (scorePost - scorePre) / SEdiff ;  SEdiff = sqrt(2) * SEM ; SEM = SD_pre * sqrt(1 - reliability)
// |RCI| > 1.96 → cambio fiable (p<.05).  Requiere `reliability` estimada del probe.
rci = (post - pre) / (sqrt(2.0) * sdPre * sqrt(1.0 - reliability))
```
**No calcular RCI hasta tener `reliability` del probe** (mínimo 2-3 administraciones).
Mientras tanto: `confident=false` y reportar solo delta crudo con cautela.

## 8. Edge cases (obligatorio cubrir en tests)

| Caso | Tratamiento |
|------|-------------|
| Bloque con 0 hits | RT mediano/CV = `confident=false`, value NaN-safe (0.0). |
| accuracy = 0 | IES/LISAS indefinidos → `confident=false`. |
| n < 4 RT | No aplicar filtro MAD; marcar baja confianza. |
| MAD = 0 (todos iguales) | No filtrar; CV = 0. |
| SSD nunca presentado (E4 mal configurado) | SSRT = no calculable. |
| p(respond|signal) fuera de [0.25,0.75] | SSRT `confident=false` + flag gaming. |
| reliability ausente | RCI no se calcula. |
| división por SD=0 en LISAS | fallback a IES. |

## 9. Estrategia de test (resumen; detalle doc 11)

- **Vectores conocidos:** datasets sintéticos con métricas calculadas a mano.
- **Property-based:** accuracy ∈ [0,1]; RT-CV ≥ 0; mediana ≤ máximo.
- **Robustez:** inyectar outliers extremos y verificar que MAD los excluye.
- **SSRT:** replicar ejemplo del paper de Verbruggen y comprobar igualdad.

## 10. Criterios de aceptación (Fase 2)

- [ ] Cada `MetricKey` tiene función pura + test con vector conocido.
- [ ] RT mediano y RT-CV usados como primarias; media nunca como primaria.
- [ ] Filtro MAD + anticipaciones implementado y testeado.
- [ ] SSRT por método de integración, con detección de gaming.
- [ ] Todo `MetricValue` reporta `n` y `confident`.
- [ ] Edge cases de la tabla §8 cubiertos.
- [ ] Cero dependencias nuevas (math con `kotlin.math`).

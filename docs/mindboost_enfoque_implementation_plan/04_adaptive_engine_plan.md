# 04 · Plan del Motor Adaptativo

> **Fase 3.** Kotlin puro en `domain/adaptive/`. Función pura: dado el estado
> reciente (métricas + historial corto), devuelve una **decisión** y un nuevo
> `DifficultyVector`. Sin tiempo real, sin plataforma, determinista y testeable.
> **Solo se aplica en `Mode.TRAINING`.** En `EVALUATION`/`CALIBRATION` la
> dificultad es fija (doc 06).

## 0. Principios

1. **La dificultad es un vector, no un nivel.** Se mueve **un eje por decisión**.
2. **Anti-oscilación:** histéresis + cooldown + paso pequeño.
3. **Honestidad:** subir velocidad NO es la única vía. Si el usuario satura un
   eje, se trabaja otro (CAMBIAR_EJE), no se machaca el mismo.
4. **Seguridad sobre rendimiento:** fatiga/impulsividad mandan sobre progresión.

## 1. Contrato

```kotlin
package com.mindos.app.features.mindboost.focus.domain.adaptive

sealed interface AdaptiveDecision {
    data class StepUp(val axis: DifficultyAxis) : AdaptiveDecision
    data object Hold : AdaptiveDecision
    data class StepDown(val axis: DifficultyAxis) : AdaptiveDecision
    data class SwitchAxis(val from: DifficultyAxis, val to: DifficultyAxis) : AdaptiveDecision
    data object Cooldown : AdaptiveDecision        // micro-pausa / bajar carga global
    data object StopSession : AdaptiveDecision     // fatiga alta sostenida
    data class ContainImpulsivity(val axis: DifficultyAxis) : AdaptiveDecision
}

data class AdaptiveResult(
    val decision: AdaptiveDecision,
    val nextDifficulty: DifficultyVector,
    val rationale: String,          // explicación legible (telemetría + debug, no PII)
)

interface AdaptiveEngine {
    fun decide(input: AdaptiveInput): AdaptiveResult
}

/** Todo lo que la decisión necesita. Inmutable. */
data class AdaptiveInput(
    val exercise: ExerciseDefinition,
    val current: DifficultyVector,
    val recentBlock: MetricSet,                 // métricas del bloque recién terminado
    val recentTrials: List<TrialResult>,        // ventana corta (p. ej. últimos 8-12)
    val perAxisMastery: Map<DifficultyAxis, AxisMastery>, // saturación por eje
    val fatigue: FatigueSignal,
    val config: AdaptiveConfig,
)
```

## 2. Parámetros (constantes nombradas — nada de magic numbers sueltos)

```kotlin
data class AdaptiveConfig(
    val accUpThreshold: Double = 0.85,     // SUBIR si accuracy >= 0.85
    val accHoldLow: Double = 0.72,         // MANTENER en [0.72, 0.85)
    val accDownThreshold: Double = 0.65,   // BAJAR si accuracy < 0.65
    val consecutiveErrorsDown: Int = 3,    // BAJAR si 3+ errores consecutivos
    val rtCvSpikeFactor: Double = 1.30,    // BAJAR si RT-CV > 1.30 * baseline
    val stepUp: Double = 0.08,             // paso de subida por eje (pequeño)
    val stepDown: Double = 0.12,           // bajada más agresiva que subida (seguridad)
    val cooldownAfterDecisions: Int = 1,   // nº de bloques de histéresis tras un cambio
    val impulsiveRtFloorMs: Long = 250,    // RT muy corto + comisión = precipitación
    val masterySaturation: Double = 0.92,  // eje saturado si nivel>=0.92 y accuracy alta
)
```

## 3. Árbol de decisión (orden de evaluación = prioridad)

Se evalúa **en este orden**; la primera condición que dispara gana. Seguridad
primero, progresión al final.

```
1. STOP_SESSION   si fatiga ALTA sostenida (>=2 bloques) → terminar con dignidad.
2. COOLDOWN        si señales de fatiga emergente (RT-CV subiendo + lapsos crecientes
                   + decremento) pero no crítica → micro-pausa / bajar timeOnTask.
3. CONTAIN_IMPULSIVITY si comisiones altas con RT < impulsiveRtFloorMs
                   → subir inhibitoryDemand penalización + reducir incentivo de velocidad.
4. STEP_DOWN       si accuracy < accDownThreshold
                   O 3+ errores consecutivos
                   O RT-CV > rtCvSpikeFactor * baseline.
5. HOLD            si en histéresis (cambio reciente) — esperar a estabilizar.
6. SWITCH_AXIS     si el eje gobernante está saturado (mastery alto) PERO otro eje
                   relevante está flojo → mover trabajo al eje débil.
7. STEP_UP         si accuracy >= accUpThreshold Y RT-CV estable/bajando
                   Y sin racha de error reciente Y fatiga baja.
8. HOLD            en cualquier otro caso (zona [0.72,0.85)).
```

### Pseudocódigo

```kotlin
override fun decide(i: AdaptiveInput): AdaptiveResult {
    val acc = i.recentBlock.value(MetricKey.ACCURACY)
    val rtCv = i.recentBlock.value(MetricKey.RT_CV)
    val baselineCv = i.config.baselineRtCv(i)         // del perfil; fallback poblacional
    val consecErrors = trailingConsecutiveErrors(i.recentTrials)
    val gov = i.exercise.governingAxis()              // eje primario del ejercicio

    return when {
        i.fatigue.level == FatigueLevel.HIGH && i.fatigue.sustained ->
            result(StopSession, i.current, "fatiga alta sostenida")

        i.fatigue.emerging ->
            result(Cooldown, i.current.adjust(TIME_ON_TASK, down), "fatiga emergente")

        isImpulsive(i) ->
            result(ContainImpulsivity(INHIBITORY_DEMAND),
                   i.current.adjust(INHIBITORY_DEMAND, up), "comisiones con RT bajo")

        acc < cfg.accDownThreshold || consecErrors >= cfg.consecutiveErrorsDown
            || rtCv > cfg.rtCvSpikeFactor * baselineCv ->
            stepDown(i, gov)

        inHysteresis(i) -> result(Hold, i.current, "histéresis post-cambio")

        masterySaturated(i, gov) && weakerAxis(i) != null ->
            switchAxis(i, gov, weakerAxis(i)!!)

        acc >= cfg.accUpThreshold && rtCvStableOrFalling(i) && consecErrors == 0
            && i.fatigue.level == FatigueLevel.LOW ->
            stepUp(i, gov)

        else -> result(Hold, i.current, "zona de mantenimiento")
    }
}
```

## 4. Anti-oscilación (evitar el "ping-pong" de dificultad)

Tres mecanismos combinados:

1. **Histéresis (cooldown):** tras un STEP_UP/STEP_DOWN/SWITCH, forzar `HOLD`
   durante `cooldownAfterDecisions` bloques para medir el efecto antes de
   re-decidir. Estado: `decisionsSinceChange` en el holder de sesión.
2. **Asimetría de paso:** `stepDown (0.12) > stepUp (0.08)`. Bajar es rápido
   (seguridad), subir es prudente (consolidar).
3. **Banda muerta (dead zone):** la zona `[0.72, 0.85)` es HOLD explícito; no se
   toca dificultad ahí. Evita micro-ajustes nerviosos.
4. **Suavizado de métricas:** decidir sobre métricas de **bloque** (no de trial
   individual) o sobre media móvil exponencial de los últimos K bloques.

## 5. Cambio de eje (CAMBIAR_EJE)

```kotlin
data class AxisMastery(
    val axis: DifficultyAxis,
    val level: AxisLevel,
    val accuracyAtLevel: Double,
)
fun masterySaturated(i, axis) =
    i.perAxisMastery[axis]?.let { it.level.value >= cfg.masterySaturation
        && it.accuracyAtLevel >= cfg.accUpThreshold } ?: false

/** Eje relevante con menor mastery → candidato a trabajar. */
fun weakerAxis(i): DifficultyAxis? = i.exercise.relevantAxes
    .filter { it != i.exercise.governingAxis() }
    .minByOrNull { i.perAxisMastery[it]?.let { m -> m.accuracyAtLevel } ?: 1.0 }
```

Casos del producto:
- **Domina velocidad pero falla interferencia** → de `PRESSURE`/`PERCEPTUAL_LOAD`
  hacia `INTERFERENCE`.
- **Domina precisión pero falla presión** → de `PERCEPTUAL_LOAD`/`INTERFERENCE`
  hacia `PRESSURE`.
- **Satura un eje** → mover a otro eje relevante no saturado.

## 6. Contener impulsividad

```kotlin
fun isImpulsive(i): Boolean {
    val falseAlarms = i.recentBlock.value(MetricKey.FALSE_ALARMS)
    val fastErrors = i.recentTrials.count {
        it.outcome == TrialOutcome.FALSE_ALARM &&
        (it.reactionTime?.value ?: Long.MAX_VALUE) < cfg.impulsiveRtFloorMs
    }
    return fastErrors >= 2 && falseAlarms > expectedFalseAlarms(i)
}
```
Respuesta del sistema (no solo dificultad):
- Subir `inhibitoryDemand`.
- **Bajar el incentivo de velocidad** en el scoring (doc 09/10): no premiar RT
  corto cuando hay comisiones.
- Introducir **micro-pausa pre-respuesta** opcional (gate de 200-300 ms antes de
  aceptar input) — configurable por perfil impulsivo (doc 08).
- En E4: detectar ralentización estratégica (SSRT gaming, doc 03) y NO premiarla.

## 7. Detección de fatiga

```kotlin
data class FatigueSignal(
    val level: FatigueLevel,     // LOW / MODERATE / HIGH
    val emerging: Boolean,       // tendencia al alza aún no crítica
    val sustained: Boolean,      // alta durante >=2 bloques
)
enum class FatigueLevel { LOW, MODERATE, HIGH }

/** Fatiga = combinación de señales intra-sesión (no una sola). */
fun computeFatigue(session: List<BlockResult>, subj: SubjectiveState?): FatigueSignal {
    // 1. RT-CV creciente bloque a bloque (pendiente > 0).
    // 2. Lapsos crecientes por bloque.
    // 3. Vigilance decrement slope negativo y acelerando.
    // 4. Accuracy cayendo en el último tercio vs primero.
    // 5. (opcional) subjetivo: sueño bajo / hora tardía agrava el umbral.
}
```
La fatiga **rebaja umbrales**: con fatiga MODERATE, ser más conservador para
subir y más rápido para Cooldown.

## 8. Detección de mejora falsa (anti-meseta / anti-ilusión)

La mejora dentro de un ejercicio entrenado puede ser **aprendizaje de la tarea
específica**, no del control atencional. Señales de "mejora falsa":

- Sube accuracy pero **RT-CV no baja** (no es más estable, solo memoriza patrón).
- Mejora en el ejercicio entrenado pero **no en el probe no entrenado** (doc 06).
- Subida de dificultad sin descenso de errores tras consolidar.

→ El motor adaptativo **no afirma mejora**. La afirmación de mejora real la hace
el subsistema de evaluación con **probes reservados** (doc 06) vía RCI. El motor
adaptativo solo regula carga. Mantener esta separación es una restricción dura.

## 9. Tests obligatorios (detalle doc 11)

- Tabla de decisión: para cada rama, input que la dispara → decisión esperada.
- Anti-oscilación: secuencia alternante de accuracy alta/baja NO produce
  ping-pong (histéresis lo amortigua).
- Impulsividad: comisiones rápidas → `ContainImpulsivity`, no `StepUp`.
- Fatiga: serie con RT-CV creciente → `Cooldown` luego `StopSession`.
- Cambio de eje: eje gobernante saturado + eje débil → `SwitchAxis`.
- Simuladores de usuario (doc 11) cierran el lazo end-to-end.

## 10. Criterios de aceptación (Fase 3)

- [ ] `AdaptiveEngine.decide` es función pura, determinista, sin I/O.
- [ ] Un solo eje cambia por decisión.
- [ ] Seguridad (stop/cooldown/impulsividad) tiene prioridad sobre progresión.
- [ ] Histéresis implementada; no hay oscilación en tests adversariales.
- [ ] El motor NUNCA afirma "mejora real" (eso es de evaluación).
- [ ] Solo opera en `Mode.TRAINING`.
- [ ] Cero dependencias nuevas.

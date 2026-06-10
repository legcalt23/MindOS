# 02 · Plan del Modelo de Dominio

> **Fase 1.** Kotlin puro en `commonMain`, sin Compose, sin Firebase, sin
> plataforma. 100% testeable. Inmutable (`data class`, `val`, copias).

## 0. Paquetes propuestos

```
features/mindboost/focus/domain/
  model/        # entidades, value objects, enums
    Capacity.kt
    ExerciseId.kt
    ExerciseDefinition.kt
    DifficultyVector.kt
    Stimulus.kt
    Response.kt
    TrialResult.kt
    BlockResult.kt
    SessionResult.kt
    Mode.kt
    UserCognitiveProfile.kt
  metrics/      # (doc 03)
  adaptive/     # (doc 04)
  exercise/     # (doc 05)
  session/      # (doc 07)
```

## 1. Enums y categorías cognitivas

```kotlin
/** Subcapacidad atencional entrenada. Eje taxonómico estable. */
enum class Capacity {
    SUSTAINED_VIGILANCE,       // atención sostenida / vigilancia
    SELECTIVE_FILTERING,       // atención selectiva / filtrado
    RESPONSE_INHIBITION,       // inhibición de respuesta
    INTERFERENCE_CONTROL,      // control de interferencia
    CONTROLLED_FLEXIBILITY,    // flexibilidad controlada
    TEMPORAL_ATTENTION,        // atención temporal / re-enganche
    DUAL_TASK_INTEGRATION,     // integración / doble tarea
    DISTRACTION_RESISTANCE,    // resistencia a distracción
    PRECISION_UNDER_PRESSURE,  // precisión bajo presión
}

/** Módulo cognitivo (agrupa ejercicios). A..F del diseño funcional. */
enum class CognitiveModule { A_VIGILANCE, B_SELECTIVE, C_INHIBITION,
    D_INTERFERENCE, E_RECOVERY, F_INTEGRATION }

/** Identidad estable de cada ejercicio del catálogo (E1..E13). */
enum class ExerciseId { E1_VIGIA, E2_FILTRO, E3_GATILLO, E4_FRENO, E5_CONFLICTO,
    E6_CAMBIO, E7_RASTREO, E8_DUO, E9_ENJAMBRE, E10_FLUJO, E11_ANCLA,
    E12_REENGANCHE, E13_PRESION }

/** Familia de paradigma psicométrico (para documentar linaje científico). */
enum class Paradigm { PVT, MACKWORTH_CLOCK, FLANKER, GO_NO_GO, STOP_SIGNAL,
    STROOP, SIMON, TASK_SWITCHING, VISUAL_SEARCH, MOT, RSVP, DUAL_TASK,
    DIGITAL_DISTRACTION, DEADLINE_PRESSURE }

/** Modo de operación — separación crítica training/evaluation/calibration. */
enum class Mode { TRAINING, EVALUATION, CALIBRATION }

/** Resultado de un trial individual. */
enum class TrialOutcome {
    HIT,                 // respuesta correcta a objetivo
    CORRECT_REJECTION,   // no responder cuando no había objetivo (no-go correcto)
    MISS,                // omisión: había objetivo, no se respondió
    FALSE_ALARM,         // comisión: se respondió a no-objetivo / no-go
    ANTICIPATION,        // respuesta antes del estímulo o RT < piso fisiológico
    TIMEOUT,             // venció la ventana sin respuesta (puede ser MISS)
}
```

## 2. Value objects

Inmutables, auto-validantes, sin lógica de plataforma. Siguen el estilo de
`features/social/domain/valueobject/*` (Handle, UserId…).

```kotlin
/** Milisegundos de tiempo de reacción medido (monótono). Nunca negativo. */
@JvmInline value class ReactionTimeMs(val value: Long) {
    init { require(value >= 0) { "RT no puede ser negativo" } }
}

/** Proporción 0.0..1.0 (precisión, accuracy, ratios). */
@JvmInline value class Proportion(val value: Double) {
    init { require(value in 0.0..1.0) }
    companion object { val ZERO = Proportion(0.0); val ONE = Proportion(1.0) }
}

/** Nivel de un eje de dificultad, normalizado 0.0 (mínimo) .. 1.0 (máximo). */
@JvmInline value class AxisLevel(val value: Double) {
    init { require(value in 0.0..1.0) }
    fun stepUp(delta: Double) = AxisLevel((value + delta).coerceIn(0.0, 1.0))
    fun stepDown(delta: Double) = AxisLevel((value - delta).coerceIn(0.0, 1.0))
}

/** Identidad de sesión / bloque / trial — IDs estables generados por reloj inyectado. */
@JvmInline value class SessionId(val value: String)
@JvmInline value class BlockId(val value: String)
@JvmInline value class TrialId(val value: String)
```

## 3. DifficultyVector (eje central de adaptación)

La dificultad NO es un nivel escalar. Es un **vector multieje**. Detalle de
reglas en doc 04.

```kotlin
/**
 * Vector de dificultad multieje. Cada eje 0.0..1.0. La adaptación mueve
 * UN eje por vez (doc 04). Inmutable: toda modificación devuelve copia.
 */
data class DifficultyVector(
    val perceptualLoad: AxisLevel = AxisLevel(0.0),       // carga perceptiva
    val interference: AxisLevel = AxisLevel(0.0),         // interferencia
    val temporalUncertainty: AxisLevel = AxisLevel(0.0),  // incertidumbre temporal
    val inhibitoryDemand: AxisLevel = AxisLevel(0.0),     // demanda inhibitoria
    val dualTaskLoad: AxisLevel = AxisLevel(0.0),         // carga de doble tarea
    val timeOnTask: AxisLevel = AxisLevel(0.0),           // tiempo-en-tarea
    val pressure: AxisLevel = AxisLevel(0.0),             // presión temporal
    val distractorSalience: AxisLevel = AxisLevel(0.0),   // saliencia distractores
    val familiarityPenalty: AxisLevel = AxisLevel(0.0),   // novedad / anti-familiaridad
) {
    fun adjust(axis: DifficultyAxis, newLevel: AxisLevel): DifficultyVector = when (axis) {
        DifficultyAxis.PERCEPTUAL_LOAD -> copy(perceptualLoad = newLevel)
        DifficultyAxis.INTERFERENCE -> copy(interference = newLevel)
        // ... resto
    }
    fun level(axis: DifficultyAxis): AxisLevel = when (axis) { /* ... */ }
}

enum class DifficultyAxis { PERCEPTUAL_LOAD, INTERFERENCE, TEMPORAL_UNCERTAINTY,
    INHIBITORY_DEMAND, DUAL_TASK_LOAD, TIME_ON_TASK, PRESSURE,
    DISTRACTOR_SALIENCE, FAMILIARITY_PENALTY }
```

Cada ejercicio declara **qué ejes son relevantes** para él (no todos los ejes
aplican a todos los ejercicios — doc 05 tabla de mapeo).

## 4. ExerciseDefinition (configuración declarativa)

```kotlin
/**
 * Definición declarativa, inmutable, de un ejercicio. NO contiene lógica de
 * render ni de generación de estímulos (eso vive en el TrialGenerator del
 * exercise/, doc 05). Es la "ficha técnica".
 */
data class ExerciseDefinition(
    val id: ExerciseId,
    val capacity: Capacity,
    val module: CognitiveModule,
    val paradigm: Paradigm,
    /** Ejes que este ejercicio puede manipular. El motor adaptativo solo toca estos. */
    val relevantAxes: Set<DifficultyAxis>,
    /** Métricas primarias que define este ejercicio (para reporte y adaptación). */
    val primaryMetrics: Set<MetricKey>,
    /** Métrica que gobierna la decisión adaptativa principal (p. ej. accuracy o SSRT). */
    val governingMetric: MetricKey,
    /** Etiqueta honesta de qué entrena, mostrada al usuario (sin pseudociencia). */
    val plainDescription: String,
)
```

## 5. Estímulo y respuesta (genéricos, agnósticos de render)

```kotlin
/** Estímulo presentado en un trial. Render lo resuelve la capa UI. */
data class Stimulus(
    val index: Int,                 // posición en la secuencia del bloque
    val isTarget: Boolean,          // ¿requiere respuesta? (go) vs no-objetivo (no-go)
    val requiresStop: Boolean = false,   // stop-signal (E4)
    val correctResponseKey: ResponseKey?, // respuesta esperada (null si no-go)
    val onsetMonotonicNs: Long?,    // sellado al presentarse (lo rellena el runtime)
    val payload: StimulusPayload,   // datos de render (sealed por familia, doc 05)
)

enum class ResponseKey { PRIMARY, SECONDARY, LEFT, RIGHT, NONE }

/** Respuesta del usuario capturada por el runtime con timestamp monótono. */
data class Response(
    val key: ResponseKey,
    val responseMonotonicNs: Long,  // TimeSource.Monotonic
)
```

## 6. Resultados: Trial → Block → Session (agregación)

```kotlin
data class TrialResult(
    val trialId: TrialId,
    val exerciseId: ExerciseId,
    val mode: Mode,
    val stimulusIsTarget: Boolean,
    val requiredStop: Boolean,
    val outcome: TrialOutcome,
    val reactionTime: ReactionTimeMs?,  // null si MISS/TIMEOUT/CORRECT_REJECTION
    val difficulty: DifficultyVector,   // dificultad vigente en este trial
    val stopSignalDelayMs: Long?,       // SSD para E4
    val timestampMs: Long,              // reloj de pared (EpochClock) para orden/persistencia
)

data class BlockResult(
    val blockId: BlockId,
    val exerciseId: ExerciseId,
    val mode: Mode,
    val trials: List<TrialResult>,
    val difficultyStart: DifficultyVector,
    val difficultyEnd: DifficultyVector,
    val metrics: MetricSet,             // métricas calculadas (doc 03)
)

data class SessionResult(
    val sessionId: SessionId,
    val mode: Mode,
    val template: SessionTemplate,      // 5/10/20/30 (doc 07)
    val blocks: List<BlockResult>,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val subjectiveState: SubjectiveState?,  // cuestionario breve (sueño, cafeína, ánimo, hora)
    val sessionMetrics: MetricSet,      // agregado de sesión
)

data class SubjectiveState(
    val sleepHours: Double?,
    val caffeine: Boolean?,
    val mood1to5: Int?,
    val localHour: Int?,                // 0..23, para correlación circadiana
)
```

## 7. UserCognitiveProfile (perfil vivo)

```kotlin
/**
 * Perfil cognitivo persistente y vivo. Se actualiza tras cada sesión/baseline.
 * Local-first (doc 10). Dato sensible: nunca se comparte (doc 14).
 */
data class UserCognitiveProfile(
    val userId: String,
    val archetype: UserArchetype,                 // inferido (doc 08)
    val baseline: BaselineSnapshot?,              // baseline inicial fijo (doc 06)
    /** Umbrales individuales por métrica (p. ej. piso de lapso, RT-CV basal). */
    val individualThresholds: Map<MetricKey, Double>,
    /** Dificultad de partida por ejercicio (calibrada). */
    val startingDifficulty: Map<ExerciseId, DifficultyVector>,
    /** Estado de periodización: semana actual del ciclo de 8. */
    val cycleWeek: Int,
    val updatedAtMs: Long,
)

enum class UserArchetype { BEGINNER, LOW_CONCENTRATION, IMPULSIVE,
    SLOW_BUT_ACCURATE, FAST_BUT_INACCURATE, ADVANCED, ELITE }
```

## 8. Interfaces de dominio (contratos, sin impl)

```kotlin
// domain/repository/
interface FocusProfileRepository {
    suspend fun load(userId: String): UserCognitiveProfile?
    suspend fun save(profile: UserCognitiveProfile)
}
interface SessionHistoryRepository {
    suspend fun append(result: SessionResult)
    suspend fun recent(userId: String, limit: Int): List<SessionResult> // ⚠ limit obligatorio
}
interface BaselineRepository {
    suspend fun load(userId: String): BaselineSnapshot?
    suspend fun save(userId: String, snapshot: BaselineSnapshot)
}
interface ProbeBankRepository {                 // probes reservados (doc 06)
    suspend fun nextProbe(userId: String, week: Int): ProbeDefinition
    suspend fun recordProbe(userId: String, result: SessionResult)
}
```

## 9. Relaciones (mapa mental)

```
ExerciseDefinition ──declara──▶ Capacity, Module, Paradigm, relevantAxes, metrics
        │
        ▼ (runtime, doc 05)
   TrialGenerator(ExerciseDefinition, DifficultyVector) ──▶ List<Stimulus>
        │ + Response (runtime)
        ▼
   TrialResult ──agrega──▶ BlockResult ──agrega──▶ SessionResult
        │                      │                        │
        ▼                      ▼                        ▼
   MetricsEngine (doc 03) calcula MetricSet a cada nivel
        │
        ▼
   AdaptiveEngine (doc 04) lee MetricSet ──▶ nuevo DifficultyVector
        │
        ▼
   UserCognitiveProfile (doc 08) se actualiza ──▶ FocusProfileRepository (doc 10)
```

## 10. Criterios de aceptación (Fase 1)

- [ ] Todo el dominio compila en `commonMain` sin imports de Compose/Firebase/plataforma.
- [ ] Todos los value objects validan sus invariantes (`require`) con tests.
- [ ] `DifficultyVector.adjust/level` cubierto al 100% por tests.
- [ ] `data class` inmutables; cero `var` mutable público.
- [ ] Interfaces de repo declaran `limit` en lecturas de lista.
- [ ] Cero dependencias nuevas en `libs.versions.toml`.

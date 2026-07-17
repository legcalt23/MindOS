package com.mindos.app.features.mindboost.focus.domain.catalog

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.Capacity
import com.mindos.app.features.mindboost.focus.domain.model.CognitiveModule
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm

/**
 * The canonical, immutable catalogue of the 13 focus exercises. These are
 * declarative tech-sheets only (no render logic). They are the single source of
 * truth that metrics, adaptation, periodization and reporting read from.
 *
 * Mirrors the master table in 05_exercise_engine_plan.md §5.
 */
object ExerciseCatalog {

    val E1_VIGIA = ExerciseDefinition(
        id = ExerciseId.E1_VIGIA,
        capacity = Capacity.SUSTAINED_VIGILANCE,
        module = CognitiveModule.A_VIGILANCE,
        paradigm = Paradigm.PVT,
        relevantAxes = setOf(
            DifficultyAxis.TIME_ON_TASK,
            DifficultyAxis.PERCEPTUAL_LOAD,
            DifficultyAxis.TEMPORAL_UNCERTAINTY,
            DifficultyAxis.DISTRACTOR_SALIENCE,
        ),
        governingAxis = DifficultyAxis.TIME_ON_TASK,
        governingMetric = MetricKey.RT_CV,
        primaryMetrics = setOf(
            MetricKey.RT_CV,
            MetricKey.LAPSES,
            MetricKey.VIGILANCE_DECREMENT_SLOPE,
            MetricKey.FALSE_ALARMS,
            MetricKey.ANTICIPATIONS,
            MetricKey.RT_MEDIAN,
        ),
        displayName = "Vigía",
        plainDescription = "Entrena tu capacidad de mantener la atención cuando casi " +
            "no pasa nada. Mide tus lapsos y cuán estable es tu tiempo de reacción.",
    )

    val E2_FILTRO = ExerciseDefinition(
        id = ExerciseId.E2_FILTRO,
        capacity = Capacity.SELECTIVE_FILTERING,
        module = CognitiveModule.B_SELECTIVE,
        paradigm = Paradigm.FLANKER,
        relevantAxes = setOf(
            DifficultyAxis.PERCEPTUAL_LOAD,
            DifficultyAxis.INTERFERENCE,
            DifficultyAxis.DISTRACTOR_SALIENCE,
            DifficultyAxis.PRESSURE,
        ),
        governingAxis = DifficultyAxis.INTERFERENCE,
        governingMetric = MetricKey.CONGRUENCY_EFFECT,
        primaryMetrics = setOf(MetricKey.CONGRUENCY_EFFECT, MetricKey.ACCURACY, MetricKey.RT_MEDIAN),
        displayName = "Filtro",
        plainDescription = "Responde al objetivo ignorando lo que lo rodea. Mide cuánto " +
            "te frena la información irrelevante.",
    )

    val E3_GATILLO = ExerciseDefinition(
        id = ExerciseId.E3_GATILLO,
        capacity = Capacity.RESPONSE_INHIBITION,
        module = CognitiveModule.C_INHIBITION,
        paradigm = Paradigm.GO_NO_GO,
        relevantAxes = setOf(
            DifficultyAxis.INHIBITORY_DEMAND,
            DifficultyAxis.PERCEPTUAL_LOAD,
            DifficultyAxis.PRESSURE,
        ),
        governingAxis = DifficultyAxis.INHIBITORY_DEMAND,
        governingMetric = MetricKey.FALSE_ALARMS,
        primaryMetrics = setOf(
            MetricKey.FALSE_ALARMS,
            MetricKey.OMISSIONS,
            MetricKey.RT_MEDIAN,
            MetricKey.POST_ERROR_SLOWING,
        ),
        displayName = "Gatillo",
        plainDescription = "Responde rápido cuando toca y retén el impulso cuando no. " +
            "Mide tu control para no actuar de más.",
    )

    val E4_FRENO = ExerciseDefinition(
        id = ExerciseId.E4_FRENO,
        capacity = Capacity.RESPONSE_INHIBITION,
        module = CognitiveModule.C_INHIBITION,
        paradigm = Paradigm.STOP_SIGNAL,
        relevantAxes = setOf(DifficultyAxis.INHIBITORY_DEMAND, DifficultyAxis.PRESSURE),
        governingAxis = DifficultyAxis.INHIBITORY_DEMAND,
        governingMetric = MetricKey.SSRT,
        primaryMetrics = setOf(MetricKey.SSRT, MetricKey.RT_MEDIAN),
        displayName = "Freno",
        plainDescription = "Responde por defecto, pero aborta si aparece la señal de " +
            "freno. Mide la rapidez de tu freno mental.",
    )

    val E5_CONFLICTO = ExerciseDefinition(
        id = ExerciseId.E5_CONFLICTO,
        capacity = Capacity.INTERFERENCE_CONTROL,
        module = CognitiveModule.D_INTERFERENCE,
        paradigm = Paradigm.STROOP,
        relevantAxes = setOf(
            DifficultyAxis.INTERFERENCE,
            DifficultyAxis.PRESSURE,
            DifficultyAxis.PERCEPTUAL_LOAD,
        ),
        governingAxis = DifficultyAxis.INTERFERENCE,
        governingMetric = MetricKey.CONGRUENCY_EFFECT,
        primaryMetrics = setOf(MetricKey.CONGRUENCY_EFFECT, MetricKey.ACCURACY),
        displayName = "Conflicto",
        plainDescription = "Responde a la dimensión relevante e ignora la que entra en " +
            "conflicto. Mide tu resistencia a la interferencia.",
    )

    val E6_CAMBIO = ExerciseDefinition(
        id = ExerciseId.E6_CAMBIO,
        capacity = Capacity.CONTROLLED_FLEXIBILITY,
        module = CognitiveModule.D_INTERFERENCE,
        paradigm = Paradigm.TASK_SWITCHING,
        relevantAxes = setOf(
            DifficultyAxis.INTERFERENCE,
            DifficultyAxis.TEMPORAL_UNCERTAINTY,
            DifficultyAxis.PRESSURE,
        ),
        governingAxis = DifficultyAxis.INTERFERENCE,
        governingMetric = MetricKey.SWITCH_COST,
        primaryMetrics = setOf(MetricKey.SWITCH_COST, MetricKey.ACCURACY),
        displayName = "Cambio",
        plainDescription = "Aplica la regla activa y cámbiala cuando la situación lo pide. " +
            "Mide el coste de alternar entre tareas.",
    )

    val E7_RASTREO = ExerciseDefinition(
        id = ExerciseId.E7_RASTREO,
        capacity = Capacity.SELECTIVE_FILTERING,
        module = CognitiveModule.B_SELECTIVE,
        paradigm = Paradigm.VISUAL_SEARCH,
        relevantAxes = setOf(
            DifficultyAxis.PERCEPTUAL_LOAD,
            DifficultyAxis.DISTRACTOR_SALIENCE,
            DifficultyAxis.PRESSURE,
        ),
        governingAxis = DifficultyAxis.PERCEPTUAL_LOAD,
        governingMetric = MetricKey.SET_SIZE_SLOPE,
        primaryMetrics = setOf(MetricKey.SET_SIZE_SLOPE, MetricKey.ACCURACY, MetricKey.RT_MEDIAN),
        displayName = "Rastreo",
        plainDescription = "Localiza el objetivo entre distractores. Mide cuán eficiente " +
            "es tu búsqueda visual.",
    )

    val E8_DUO = ExerciseDefinition(
        id = ExerciseId.E8_DUO,
        capacity = Capacity.DUAL_TASK_INTEGRATION,
        module = CognitiveModule.F_INTEGRATION,
        paradigm = Paradigm.DUAL_TASK,
        relevantAxes = setOf(DifficultyAxis.DUAL_TASK_LOAD, DifficultyAxis.PERCEPTUAL_LOAD),
        governingAxis = DifficultyAxis.DUAL_TASK_LOAD,
        governingMetric = MetricKey.DUAL_TASK_COST,
        primaryMetrics = setOf(MetricKey.DUAL_TASK_COST, MetricKey.ACCURACY),
        displayName = "Dúo",
        plainDescription = "Mantén dos tareas a la vez respetando la prioridad. Mide cuánto " +
            "te cuesta repartir la atención.",
    )

    val E9_ENJAMBRE = ExerciseDefinition(
        id = ExerciseId.E9_ENJAMBRE,
        capacity = Capacity.SELECTIVE_FILTERING,
        module = CognitiveModule.B_SELECTIVE,
        paradigm = Paradigm.MOT,
        relevantAxes = setOf(
            DifficultyAxis.PERCEPTUAL_LOAD,
            DifficultyAxis.DUAL_TASK_LOAD,
            DifficultyAxis.DISTRACTOR_SALIENCE,
        ),
        governingAxis = DifficultyAxis.PERCEPTUAL_LOAD,
        governingMetric = MetricKey.ACCURACY,
        primaryMetrics = setOf(MetricKey.ACCURACY, MetricKey.RT_MEDIAN),
        displayName = "Enjambre",
        plainDescription = "Sigue varios objetivos entre objetos idénticos en movimiento. " +
            "Mide cuántos puedes seguir a la vez.",
    )

    val E10_FLUJO = ExerciseDefinition(
        id = ExerciseId.E10_FLUJO,
        capacity = Capacity.TEMPORAL_ATTENTION,
        module = CognitiveModule.E_RECOVERY,
        paradigm = Paradigm.RSVP,
        relevantAxes = setOf(DifficultyAxis.TEMPORAL_UNCERTAINTY, DifficultyAxis.PERCEPTUAL_LOAD),
        governingAxis = DifficultyAxis.TEMPORAL_UNCERTAINTY,
        governingMetric = MetricKey.ACCURACY,
        primaryMetrics = setOf(MetricKey.ACCURACY, MetricKey.RT_MEDIAN),
        displayName = "Flujo",
        plainDescription = "Detecta objetivos en un flujo rápido de información. Mide tu " +
            "recuperación de atención entre estímulos.",
    )

    val E11_ANCLA = ExerciseDefinition(
        id = ExerciseId.E11_ANCLA,
        capacity = Capacity.DISTRACTION_RESISTANCE,
        module = CognitiveModule.F_INTEGRATION,
        paradigm = Paradigm.DIGITAL_DISTRACTION,
        relevantAxes = setOf(
            DifficultyAxis.DISTRACTOR_SALIENCE,
            DifficultyAxis.DUAL_TASK_LOAD,
            DifficultyAxis.PRESSURE,
        ),
        governingAxis = DifficultyAxis.DISTRACTOR_SALIENCE,
        governingMetric = MetricKey.DISTRACTION_RESISTANCE_INDEX,
        primaryMetrics = setOf(MetricKey.DISTRACTION_RESISTANCE_INDEX, MetricKey.ACCURACY),
        displayName = "Ancla",
        plainDescription = "Mantén la tarea principal ignorando intrusos tipo notificación. " +
            "Mide tu resistencia a la distracción digital.",
    )

    val E12_REENGANCHE = ExerciseDefinition(
        id = ExerciseId.E12_REENGANCHE,
        capacity = Capacity.TEMPORAL_ATTENTION,
        module = CognitiveModule.E_RECOVERY,
        paradigm = Paradigm.PVT,
        relevantAxes = setOf(
            DifficultyAxis.TEMPORAL_UNCERTAINTY,
            DifficultyAxis.PRESSURE,
            DifficultyAxis.INHIBITORY_DEMAND,
        ),
        governingAxis = DifficultyAxis.PRESSURE,
        governingMetric = MetricKey.POST_ERROR_ACCURACY,
        primaryMetrics = setOf(MetricKey.POST_ERROR_ACCURACY, MetricKey.POST_ERROR_SLOWING),
        displayName = "Re-enganche",
        plainDescription = "Continúa tras un error sin entrar en espiral. Mide cómo te " +
            "recuperas después de equivocarte.",
    )

    val E13_PRESION = ExerciseDefinition(
        id = ExerciseId.E13_PRESION,
        capacity = Capacity.PRECISION_UNDER_PRESSURE,
        module = CognitiveModule.F_INTEGRATION,
        paradigm = Paradigm.DEADLINE_PRESSURE,
        relevantAxes = setOf(
            DifficultyAxis.PRESSURE,
            DifficultyAxis.PERCEPTUAL_LOAD,
            DifficultyAxis.INTERFERENCE,
        ),
        governingAxis = DifficultyAxis.PRESSURE,
        governingMetric = MetricKey.PRESSURE_DELTA,
        primaryMetrics = setOf(MetricKey.PRESSURE_DELTA, MetricKey.ACCURACY),
        displayName = "Presión",
        plainDescription = "Compara tu precisión con calma y bajo deadline con coste por " +
            "error. Mide cuánto te afecta la presión.",
    )

    /** All 13 definitions, indexed by id. */
    val all: Map<ExerciseId, ExerciseDefinition> = listOf(
        E1_VIGIA, E2_FILTRO, E3_GATILLO, E4_FRENO, E5_CONFLICTO, E6_CAMBIO,
        E7_RASTREO, E8_DUO, E9_ENJAMBRE, E10_FLUJO, E11_ANCLA, E12_REENGANCHE, E13_PRESION,
    ).associateBy { it.id }

    /** Definition for [id]; throws if somehow missing (catalogue is exhaustive). */
    fun definition(id: ExerciseId): ExerciseDefinition =
        requireNotNull(all[id]) { "No catalogue definition for $id." }

    /**
     * Exercises wired end-to-end (generator + real-time render + metrics) and selectable today.
     * MUST equal the keys of `FocusExerciseGenerators.wiredExerciseIds`; the consistency test enforces it.
     * Los 13 ejercicios del catálogo están integrados end-to-end (generador + renderer + métricas):
     * single-tap (E1, E3, E7 búsqueda, E8 dual-task, E9 MOT, E10 RSVP, E12), elección LEFT/RIGHT
     * (E2, E5, E6 task-switch, E11 ancla, E13 presión) y stop-signal (E4). Debe coincidir con las
     * claves de `FocusExerciseGenerators.wiredExerciseIds` (lo enforce el test de consistencia).
     */
    val implemented: List<ExerciseId> = listOf(
        ExerciseId.E1_VIGIA,
        ExerciseId.E2_FILTRO,
        ExerciseId.E3_GATILLO,
        ExerciseId.E4_FRENO,
        ExerciseId.E5_CONFLICTO,
        ExerciseId.E6_CAMBIO,
        ExerciseId.E7_RASTREO,
        ExerciseId.E8_DUO,
        ExerciseId.E9_ENJAMBRE,
        ExerciseId.E10_FLUJO,
        ExerciseId.E11_ANCLA,
        ExerciseId.E12_REENGANCHE,
        ExerciseId.E13_PRESION,
    )

    /** Exercises whose domain TrialGenerator exists and is unit-tested (≠ necessarily UI-reachable). */
    val domainReady: List<ExerciseId> = ExerciseId.entries.toList()

    /**
     * Domain-ready exercises still pending UI/render integration: choice/spatial/stream paradigms
     * (E2/E4–E13) need their own renderer + response capture before they can be exposed. Must shrink
     * to empty as paradigms are integrated. The consistency test pins this delta so finishing a
     * generator without exposing it — or exposing an id without a generator/renderer — fails CI.
     */
    val pendingUiIntegration: List<ExerciseId> = domainReady - implemented.toSet()
}

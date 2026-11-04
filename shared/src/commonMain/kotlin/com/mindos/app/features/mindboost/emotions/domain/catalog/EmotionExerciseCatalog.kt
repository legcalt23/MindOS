package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey

/**
 * The canonical, immutable catalogue of the 15 emotion exercises. Declarative tech-sheets only (no
 * render logic) — the single source of truth metrics, adaptation and reporting read from. Mirrors the
 * focus `ExerciseCatalog` pattern.
 *
 * All displayName/plainDescription strings are scanner-clean functional language (no clinical lexemes,
 * nothing in the forbidden list). [implemented] MUST equal the keys of [EmotionExerciseGenerators.registry] — the
 * consistency test enforces it, so a finished generator can never be silently forgotten nor an id
 * exposed without a runnable path. In Ola 0 both are empty; Ola 1 step 1 adds EM1/EM2/EM3 to both in
 * the same change.
 */
object EmotionExerciseCatalog {

    /**
     * Ola 3 · R1 — the fused inhibition exercise (EM1 + EM8 + EM12). See `EM1PausaGenerator` for why a stop
     * signal replaced the no-go, and note the deliberate split between the PRIMARY and the GOVERNING metric:
     *
     *  - [EmotionMetricKey.SSRT] is what the exercise measures and reports — how long the brake takes.
     *  - [EmotionMetricKey.HIT_RATE] is what moves difficulty, and it has to be, for two independent reasons.
     *    SSRT is a latency, so the zone machinery cannot compare it against a threshold in [0,1] goodness. And
     *    the metric that used to govern here, `COMMISSION_RATE`, is pinned near 0.5 BY CONSTRUCTION once a
     *    staircase is running: keeping it as the governing metric would have made every converged session read
     *    as failing its floor and step down forever. Go-trial engagement is the honest remaining question — the
     *    stop share may rise only while the user is still reliably going.
     */
    val EM1_PAUSA = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM1_PAUSA,
        capacity = EmotionCapacity.PAUSE_BEFORE_ACTION,
        governingMetric = EmotionMetricKey.HIT_RATE,
        primaryMetrics = setOf(EmotionMetricKey.SSRT, EmotionMetricKey.HIT_RATE),
        secondaryMetrics = setOf(
            EmotionMetricKey.AFFECTIVE_SSRT_DELTA,
            EmotionMetricKey.COMMISSION_RATE,
            EmotionMetricKey.RT_MEDIAN,
            EmotionMetricKey.ANTICIPATIONS,
        ),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.TASK_ONLY,
        displayName = "Frenar una respuesta ya empezada",
        plainDescription = "Responde rápido cuando el marco está abierto y aborta si se cierra a mitad. " +
            "Mide cuánto tarda tu freno.",
    )

    /**
     * Ola 3 · R2 — the fusion of EM2, EM11 and EM15 (see `EM2RecuperacionGenerator`).
     *
     * Governs on `POST_ERROR_ACCURACY`, not on the `POST_ERROR_RECOVERY` composite it used to. Two reasons, and
     * the first is the one that matters: post-error accuracy is a plain PROPORTION, so the zone machinery can
     * rank it and the block score can map it, while the composite mixes an accuracy, a normalised slowing and an
     * impulsivity penalty into an index whose 0.7 means nothing anybody can state out loud. The second is that
     * the composite is the number the audit asked us to stop leading with. It is kept as a secondary summary.
     *
     * The transfer label drops from `PLAUSIBLE` to `NOT_DEMONSTRATED`. Post-error slowing and post-error accuracy
     * adjustment are real, replicated constructs of cognitive control; that they carry over to recovering from
     * consequential mistakes in a life is not demonstrated, and the old label claimed the second from evidence for
     * the first.
     */
    val EM2_RECUPERACION = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM2_RECUPERACION,
        capacity = EmotionCapacity.POST_ERROR_RECOVERY,
        governingMetric = EmotionMetricKey.POST_ERROR_ACCURACY,
        primaryMetrics = setOf(EmotionMetricKey.POST_ERROR_ACCURACY, EmotionMetricKey.POST_ERROR_SLOWING),
        secondaryMetrics = setOf(
            EmotionMetricKey.POST_CORRECT_ACCURACY,
            EmotionMetricKey.STRATEGY_PERSISTENCE,
            EmotionMetricKey.POST_ERROR_ABANDONMENT,
            EmotionMetricKey.POST_ERROR_RECOVERY,
            EmotionMetricKey.ACCURACY,
            EmotionMetricKey.RT_CV,
            EmotionMetricKey.ABANDONMENT,
        ),
        adaptiveZone = AdaptiveZone.TOLERANCE,
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Recuperación tras error",
        plainDescription = "La dificultad se ajusta sola para que falles de vez en cuando. " +
            "Practica seguir con la misma precisión en el intento siguiente.",
    )

    val EM3_SEPARADOR = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM3_SEPARADOR,
        capacity = EmotionCapacity.CATEGORY_SEPARATION,
        governingMetric = EmotionMetricKey.CATEGORY_ACCURACY,
        primaryMetrics = setOf(EmotionMetricKey.CATEGORY_ACCURACY, EmotionMetricKey.CATEGORY_DPRIME_MIN),
        secondaryMetrics = setOf(
            EmotionMetricKey.STATE_THOUGHT_ACCURACY,
            EmotionMetricKey.CATEGORY_COVERAGE,
            EmotionMetricKey.RT_MEDIAN,
        ),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Separar emoción, pensamiento, impulso y hecho",
        plainDescription = "Distingue estado interno, pensamiento, impulso y hecho observable. " +
            "Mide tu precisión al separarlos.",
    )

    val EM4_CALIBRADOR = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM4_CALIBRADOR,
        capacity = EmotionCapacity.CALIBRATION_ACCURACY,
        governingMetric = EmotionMetricKey.CALIBRATION_ERROR,
        primaryMetrics = setOf(EmotionMetricKey.CALIBRATION_ERROR),
        secondaryMetrics = setOf(EmotionMetricKey.ACCURACY),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Calibrador intensidad-rendimiento",
        plainDescription = "Estima cómo tu estado afecta tu rendimiento y compáralo con el dato real.",
    )

    val EM5_SESGO_AFECTIVO = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM5_SESGO_AFECTIVO,
        capacity = EmotionCapacity.AFFECTIVE_DECISION_QUALITY,
        governingMetric = EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
        primaryMetrics = setOf(
            EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
            EmotionMetricKey.AFFECTIVE_DECISION_QUALITY,
        ),
        secondaryMetrics = setOf(EmotionMetricKey.IMPULSIVITY),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.PLAUSIBLE,
        displayName = "Decisiones bajo sesgo afectivo",
        plainDescription = "Decide con evidencia cuando una opción resulta más atractiva por carga afectiva.",
    )

    val EM6_TOLERANCIA = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM6_TOLERANCIA,
        capacity = EmotionCapacity.FRUSTRATION_TOLERANCE,
        governingMetric = EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX,
        primaryMetrics = setOf(EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX),
        secondaryMetrics = setOf(EmotionMetricKey.ABANDONMENT, EmotionMetricKey.SAFE_PERSISTENCE_INDEX),
        adaptiveZone = AdaptiveZone.TOLERANCE,
        transferLabel = TransferLabel.PLACEBO_RISK,
        displayName = "Tolerancia a la demanda",
        plainDescription = "Sostén la estrategia ante dificultad creciente sin sobreexigirte. " +
            "Una pausa cuenta como regulación.",
    )

    val EM7_REENTRADA = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM7_REENTRADA,
        capacity = EmotionCapacity.REGULATION_STABILITY,
        governingMetric = EmotionMetricKey.REGULATION_STABILITY,
        primaryMetrics = setOf(EmotionMetricKey.REGULATION_STABILITY),
        secondaryMetrics = setOf(EmotionMetricKey.RT_CV),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.PLAUSIBLE,
        displayName = "Reentrada tras pausa",
        plainDescription = "Retoma la tarea con control después de una pausa. Mide la estabilidad al volver.",
    )

    val EM8_ANTIIMPULSO = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM8_ANTIIMPULSO,
        capacity = EmotionCapacity.PAUSE_BEFORE_ACTION,
        governingMetric = EmotionMetricKey.COMMISSION_RATE,
        primaryMetrics = setOf(EmotionMetricKey.COMMISSION_RATE),
        secondaryMetrics = setOf(EmotionMetricKey.RT_MEDIAN, EmotionMetricKey.IMPULSIVITY),
        adaptiveZone = AdaptiveZone.COMMISSION,
        // Ola 3 · P4 — dropped from PLAUSIBLE to match EM1, which is the SAME go/no-go paradigm with a different
        // no-go share. Two labels for one paradigm made this enum — the module's only honesty mechanism —
        // internally inconsistent, and the optimistic one was the one without evidence behind it.
        transferLabel = TransferLabel.TASK_ONLY,
        displayName = "Anti-impulso por comisión",
        plainDescription = "Retén el impulso cuando la regla lo pide. Mide tus respuestas de más.",
    )

    val EM9_ETIQUETADO = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM9_ETIQUETADO,
        capacity = EmotionCapacity.GRANULARITY,
        governingMetric = EmotionMetricKey.CATEGORY_COVERAGE,
        primaryMetrics = setOf(EmotionMetricKey.CATEGORY_COVERAGE),
        secondaryMetrics = setOf(EmotionMetricKey.ACCURACY, EmotionMetricKey.RT_MEDIAN),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Etiquetado funcional rápido",
        plainDescription = "Nombra con rapidez la etiqueta funcional más precisa entre opciones cercanas.",
    )

    val EM10_SEP_PRESION = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM10_SEP_PRESION,
        capacity = EmotionCapacity.CATEGORY_SEPARATION,
        governingMetric = EmotionMetricKey.CATEGORY_ACCURACY,
        primaryMetrics = setOf(EmotionMetricKey.CATEGORY_ACCURACY, EmotionMetricKey.CATEGORY_DPRIME_MIN),
        secondaryMetrics = setOf(EmotionMetricKey.STATE_THOUGHT_ACCURACY, EmotionMetricKey.RT_MEDIAN),
        adaptiveZone = AdaptiveZone.GENERAL,
        // Ola 3 · P4 — dropped from PLAUSIBLE to match EM3, which is the same bank and the same judgement with the
        // time pressure removed. Adding hurry adds a confound, not evidence of transfer.
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Separación bajo presión leve",
        plainDescription = "Separa estado, pensamiento, impulso y hecho con una presión temporal leve.",
    )

    val EM11_FEEDBACK_NEG = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM11_FEEDBACK_NEG,
        capacity = EmotionCapacity.POST_ERROR_RECOVERY,
        governingMetric = EmotionMetricKey.POST_ERROR_RECOVERY,
        primaryMetrics = setOf(EmotionMetricKey.POST_ERROR_RECOVERY),
        secondaryMetrics = setOf(EmotionMetricKey.POST_ERROR_ACCURACY, EmotionMetricKey.ABANDONMENT),
        adaptiveZone = AdaptiveZone.TOLERANCE,
        // Ola 3 · P4 — dropped from PLAUSIBLE for the same reason EM2 was, and it is the same exercise: post-error
        // slowing and post-error accuracy adjustment are real, replicated constructs, but that they carry over to
        // recovering from consequential mistakes is not demonstrated. PLAUSIBLE claimed the second from evidence
        // for the first.
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Continuidad después de un error",
        plainDescription = "Continúa con control tras un aviso neutro de error. Mide tu continuidad.",
    )

    val EM12_ESPERA = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM12_ESPERA,
        capacity = EmotionCapacity.PAUSE_BEFORE_ACTION,
        governingMetric = EmotionMetricKey.ANTICIPATIONS,
        primaryMetrics = setOf(EmotionMetricKey.ANTICIPATIONS),
        secondaryMetrics = setOf(EmotionMetricKey.RT_MEDIAN),
        adaptiveZone = AdaptiveZone.COMMISSION,
        transferLabel = TransferLabel.TASK_ONLY,
        displayName = "Espera activa",
        plainDescription = "Espera la señal correcta sin adelantarte. Mide tus anticipaciones.",
    )

    val EM13_VARIABILIDAD = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM13_VARIABILIDAD,
        capacity = EmotionCapacity.REGULATION_STABILITY,
        governingMetric = EmotionMetricKey.RT_CV,
        primaryMetrics = setOf(EmotionMetricKey.RT_CV),
        secondaryMetrics = setOf(EmotionMetricKey.REGULATION_STABILITY),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.NOT_DEMONSTRATED,
        displayName = "Variabilidad bajo carga",
        plainDescription = "Mantén un ritmo estable bajo carga. Mide la variabilidad de tu tiempo de respuesta.",
    )

    val EM14_CONFIANZA = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM14_CONFIANZA,
        capacity = EmotionCapacity.CALIBRATION_ACCURACY,
        governingMetric = EmotionMetricKey.CALIBRATION_ERROR,
        primaryMetrics = setOf(EmotionMetricKey.CALIBRATION_ERROR),
        secondaryMetrics = setOf(EmotionMetricKey.ACCURACY),
        adaptiveZone = AdaptiveZone.GENERAL,
        transferLabel = TransferLabel.PLAUSIBLE,
        displayName = "Calibración de confianza",
        plainDescription = "Ajusta tu confianza a tu acierto real. Mide tu calibración.",
    )

    val EM15_RECUP_CADENA = EmotionExerciseDefinition(
        id = EmotionExerciseId.EM15_RECUP_CADENA,
        capacity = EmotionCapacity.POST_ERROR_RECOVERY,
        governingMetric = EmotionMetricKey.POST_ERROR_RECOVERY,
        primaryMetrics = setOf(EmotionMetricKey.POST_ERROR_RECOVERY),
        secondaryMetrics = setOf(EmotionMetricKey.POST_ERROR_SLOWING),
        adaptiveZone = AdaptiveZone.TOLERANCE,
        transferLabel = TransferLabel.PLAUSIBLE,
        displayName = "Recuperación encadenada",
        plainDescription = "Recupérate de varios errores seguidos sin entrar en espiral.",
    )

    /** All 15 definitions, indexed by id. */
    val all: Map<EmotionExerciseId, EmotionExerciseDefinition> = listOf(
        EM1_PAUSA, EM2_RECUPERACION, EM3_SEPARADOR, EM4_CALIBRADOR, EM5_SESGO_AFECTIVO,
        EM6_TOLERANCIA, EM7_REENTRADA, EM8_ANTIIMPULSO, EM9_ETIQUETADO, EM10_SEP_PRESION,
        EM11_FEEDBACK_NEG, EM12_ESPERA, EM13_VARIABILIDAD, EM14_CONFIANZA, EM15_RECUP_CADENA,
    ).associateBy { it.id }

    /** Definition for [id]; throws if missing (catalogue is exhaustive). */
    fun definition(id: EmotionExerciseId): EmotionExerciseDefinition =
        requireNotNull(all[id]) { "No catalogue definition for $id." }

    /**
     * Exercises wired end-to-end (generator + renderer + metrics) and selectable today. MUST equal
     * the keys of [EmotionExerciseGenerators.registry]; the consistency test enforces it. Empty in
     * Ola 0 — Ola 1 step 1 sets this to [EM1, EM2, EM3] AND fills the registry in the same change.
     */
    val implemented: List<EmotionExerciseId> = EmotionExerciseGenerators.registry.keys.toList()

    /** Exercises whose domain generator exists and is unit-tested (= [implemented] in v1). */
    val domainReady: List<EmotionExerciseId> = implemented

    /** Domain-ready exercises still pending UI integration. Empty while domainReady == implemented. */
    val pendingUiIntegration: List<EmotionExerciseId> = domainReady - implemented.toSet()
}

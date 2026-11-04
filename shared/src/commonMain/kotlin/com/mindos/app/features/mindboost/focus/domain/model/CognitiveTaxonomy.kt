package com.mindos.app.features.mindboost.focus.domain.model

/**
 * Stable taxonomy of the focus-training system. These enums are the backbone of
 * the cognitive model: every exercise declares which [Capacity] it trains, which
 * [CognitiveModule] it belongs to, and which scientific [Paradigm] it descends
 * from. Adding a value here is a deliberate, documented act — they are referenced
 * across metrics, adaptation and reporting.
 */

/** Sub-capacity of attentional control that an exercise trains. */
enum class Capacity {
    SUSTAINED_VIGILANCE,
    SELECTIVE_FILTERING,
    RESPONSE_INHIBITION,
    INTERFERENCE_CONTROL,
    CONTROLLED_FLEXIBILITY,
    TEMPORAL_ATTENTION,
    DUAL_TASK_INTEGRATION,
    DISTRACTION_RESISTANCE,
    PRECISION_UNDER_PRESSURE,
}

/** Cognitive module grouping a family of exercises (A..F of the functional design). */
enum class CognitiveModule {
    A_VIGILANCE,
    B_SELECTIVE,
    C_INHIBITION,
    D_INTERFERENCE,
    E_RECOVERY,
    F_INTEGRATION,
}

/** Catalogue identity of each exercise (E1..E13). */
enum class ExerciseId {
    E1_VIGIA,
    E2_FILTRO,
    E3_GATILLO,
    E4_FRENO,
    E5_CONFLICTO,
    E6_CAMBIO,
    E7_RASTREO,
    E8_DUO,
    E9_ENJAMBRE,
    E10_FLUJO,
    E11_ANCLA,
    E12_REENGANCHE,
    E13_PRESION,
}

/** Scientific paradigm an exercise is modelled on (documents its lineage). */
enum class Paradigm {
    PVT,
    MACKWORTH_CLOCK,
    FLANKER,
    GO_NO_GO,
    STOP_SIGNAL,
    STROOP,
    SIMON,
    TASK_SWITCHING,
    VISUAL_SEARCH,
    MOT,
    RSVP,
    DUAL_TASK,
    DIGITAL_DISTRACTION,
    DEADLINE_PRESSURE,
}

/**
 * Operating mode — the critical separation between training and measuring.
 *
 * - [TRAINING]: adaptive difficulty, learning is the goal.
 * - [EVALUATION]: fixed difficulty, reserved probes, no adaptation, measures real change.
 * - [CALIBRATION]: finds the individual baseline and initial thresholds.
 *
 * The adaptive engine MUST only run in [TRAINING].
 */
enum class Mode {
    TRAINING,
    EVALUATION,
    CALIBRATION,
}

/** Classified result of a single trial. */
enum class TrialOutcome {
    /** Correct response to a target (go). */
    HIT,

    /** Correctly withheld response when none was required (no-go / non-target). */
    CORRECT_REJECTION,

    /** A target was present but no response was made. */
    MISS,

    /** Responded to a non-target / no-go (commission error). */
    FALSE_ALARM,

    /** Responded before the stimulus or faster than the physiological floor. */
    ANTICIPATION,

    /** The response window elapsed with no response. */
    TIMEOUT,
}

/**
 * A response the user actually made (a press), whatever its correctness. Withholding — right or wrong — is not
 * a response. Exhaustive on purpose: a new outcome must decide which side it is on before it can be scored.
 * Single definition (FOC-P0-05): the metrics engine and the session gate must count responses the same way.
 */
val TrialOutcome.isResponse: Boolean
    get() = when (this) {
        TrialOutcome.HIT, TrialOutcome.FALSE_ALARM, TrialOutcome.ANTICIPATION -> true
        TrialOutcome.CORRECT_REJECTION, TrialOutcome.MISS, TrialOutcome.TIMEOUT -> false
    }

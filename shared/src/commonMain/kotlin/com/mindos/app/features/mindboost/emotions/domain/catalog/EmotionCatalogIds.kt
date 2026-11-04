package com.mindos.app.features.mindboost.emotions.domain.catalog

/**
 * The 15 emotion-training exercise ids. EM1..EM3 are the v1 playable set; the rest are
 * catalogue-only (designed, not yet wired). NOT clinical categories — functional training only.
 */
enum class EmotionExerciseId {
    EM1_PAUSA,
    EM2_RECUPERACION,
    EM3_SEPARADOR,
    EM4_CALIBRADOR,
    EM5_SESGO_AFECTIVO,
    EM6_TOLERANCIA,
    EM7_REENTRADA,
    EM8_ANTIIMPULSO,
    EM9_ETIQUETADO,
    EM10_SEP_PRESION,
    EM11_FEEDBACK_NEG,
    EM12_ESPERA,
    EM13_VARIABILIDAD,
    EM14_CONFIANZA,
    EM15_RECUP_CADENA,
}

/** Functional capacity an exercise trains. NOT a clinical or personality construct. */
enum class EmotionCapacity {
    PAUSE_BEFORE_ACTION,
    POST_ERROR_RECOVERY,

    /**
     * Telling apart the four categories a phrase can belong to: internal state, thought, urge, observable fact.
     * Renamed in Ola 2 · R3 · P3 from a two-category name that governed a four-category task — impulse and fact
     * were never in the label, and they are the pair users confuse most.
     */
    CATEGORY_SEPARATION,
    CALIBRATION_ACCURACY,
    AFFECTIVE_DECISION_QUALITY,
    FRUSTRATION_TOLERANCE,
    REGULATION_STABILITY,
    GRANULARITY,
}

/**
 * Honest transfer-claim level for an exercise. Exactly ONE per definition (a test pins this).
 * The product never promises broad transfer; this enum is the single source of that honesty.
 */
enum class TransferLabel { TASK_ONLY, PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }

/**
 * Which adaptive zone governs difficulty for an exercise (pinned on the catalogue definition so the
 * engine reads it, never guesses). GENERAL ~0.80-0.85 accuracy; TOLERANCE ~0.70-0.80 with strict
 * safety; COMMISSION targets commission rate, not accuracy.
 */
enum class AdaptiveZone { GENERAL, TOLERANCE, COMMISSION }

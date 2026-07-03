package com.mindos.app.features.mindboost.core.scoring

/**
 * Central registry of SCORING/DERIVATION formula versions — the single place that pins how a signal is
 * turned into a persisted or user-facing value, so no version string is hardcoded across scattered call
 * sites (scoring doc §17). It versions DERIVATIONS (the user-facing display mapping, and each exercise's
 * scoring/model version — those persisted in `ScoreProvenance.scoringVersion` and those in a block's own
 * pipeline; block-score engines / rank ladder in later phases), NOT the psychometric metric definitions
 * (the golden nMin table of `MinSampleGate`) nor DTO schema versions. Every derivation reads its version
 * from here; tests assert the pinned strings and per-block parity.
 */
object ScoringVersionRegistry {

    /**
     * User-facing DISPLAY-score mapping: `0..1` normalized value → `0-100` integer under the DC-2 display
     * gate. Linear and PRELIMINARY until a per-block calibrated baseline curve exists (scoring doc §7
     * fallback); the `-linear-preliminary` suffix keeps that explicit. This is a display derivation, NOT
     * a per-block psychometric scoring formula.
     */
    const val USER_DISPLAY_SCORE_MAPPER: String = "user-display-score-mapper-1.0.0-linear-preliminary"

    // --- Per-exercise scoring versions persisted in ScoreProvenance.scoringVersion (Tier A). ---

    /** Decisiones · Reversibility triage scorer. */
    const val DECISIONS_REVERSIBILITY: String = "e2-reversibility-v1"

    /** Memoria · complex-span (working memory) scorer. */
    const val MEMORY_SPAN: String = "ex1-span-v1"

    /** Memoria · associative-pairs scorer. */
    const val MEMORY_PAIRS: String = "ex4-pairs-v1"

    /** Memoria · deferred (24h) recall scorer. */
    const val MEMORY_DEFERRED: String = "ex3-deferred-v1"

    // --- Velocidad model versions (own pipeline; not yet routed through ScoreResultFactory). ---

    /** Velocidad · metric-thresholds model. */
    const val SPEED_METRICS: String = "1.0.0"

    /** Velocidad · adaptive-staircase model. */
    const val SPEED_STAIRCASE: String = "1.0.0-mvp1"

    /** Velocidad · difficulty model. */
    const val SPEED_DIFFICULTY_MODEL: String = "1.0.0"

    /** Velocidad · cognitive-profile score model (MVP-3, preliminary). */
    const val SPEED_SCORE: String = "1.0.0-mvp3-preliminary"

    // --- Lenguaje model/rubric versions (own pipeline; local, no LLM). ---

    /** Lenguaje · local stub evaluator. */
    const val LANGUAGE_EVALUATOR_STUB: String = "local-stub-1"

    /** Lenguaje · local stub rubric. */
    const val LANGUAGE_RUBRIC_STUB: String = "local-stub-rubric-1"

    /** Lenguaje · profile-projection estimator. */
    const val LANGUAGE_PROJECTION_ESTIMATOR: String = "v6-projector-1"
}

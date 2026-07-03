package com.mindos.app.features.mindboost.core.scoring

/**
 * Central registry of SCORING/DERIVATION formula versions — the single place that pins how a signal is
 * turned into a persisted or user-facing value, so no version string is hardcoded across scattered call
 * sites (scoring doc §17). It versions DERIVATIONS (the user-facing display mapping today; block-score
 * engines / adapters / rank ladder in later phases), NOT the psychometric metric definitions — those
 * are the golden nMin table of `MinSampleGate`. Every derivation reads its version from here; tests
 * assert the pinned strings.
 */
object ScoringVersionRegistry {

    /**
     * User-facing DISPLAY-score mapping: `0..1` normalized value → `0-100` integer under the DC-2 display
     * gate. Linear and PRELIMINARY until a per-block calibrated baseline curve exists (scoring doc §7
     * fallback); the `-linear-preliminary` suffix keeps that explicit. This is a display derivation, NOT
     * a per-block psychometric scoring formula.
     */
    const val USER_DISPLAY_SCORE_MAPPER: String = "user-display-score-mapper-1.0.0-linear-preliminary"
}

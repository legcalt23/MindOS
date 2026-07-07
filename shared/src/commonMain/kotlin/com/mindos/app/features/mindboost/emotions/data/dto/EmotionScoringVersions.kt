@file:Suppress("MaxLineLength") // Versioning KDoc reads best whole.

package com.mindos.app.features.mindboost.emotions.data.dto

/**
 * Versioning authority for emotion scoring/calibration and exercise mechanics (MC8u-B+ §K). A persisted
 * [EmotionSessionSummaryDto] stamps the `scoringVersion` and `exerciseVersion` that produced it, so a later change to
 * the calibration RULES or an exercise's mechanics can be applied to NEW rows without retro-invalidating or silently
 * losing progress from OLDER rows.
 *
 * Backward-compat contract: a row written before this version carries `scoringVersion == LEGACY` (0, the DTO default)
 * and is calibrated under the pre-gate rules (completed + valid identity + ≥120s), so earned progress is never lost.
 * A row at `scoringVersion >= QUALITY_GATE_MIN_VERSION` additionally enforces the quality gate (governing metric
 * confident AND no blocking anomaly) — see [EmotionSessionSummaryDto] and `EmotionTerminalSemantics.isValidForAggregate`.
 */
object EmotionScoringVersions {
    /** No version stamped (legacy row written before MC8u-B+). */
    const val LEGACY = 0

    /** Current calibration ruleset: MC8u-B+ quality-gated calibration (a low-confidence/anomalous session cannot count). */
    const val SCORING = 1

    /** Rows at `scoringVersion >= this` enforce the quality gate; legacy rows below it keep the pre-gate rules. */
    const val QUALITY_GATE_MIN_VERSION = 1

    /** Current mechanic/content version of the v1 wired exercises (bumped when a generator changes measurably). */
    const val EXERCISE = 1

    /** Per-exercise mechanic version. Flat 1 for the v1 set; kept as a function so a single exercise can diverge later. */
    @Suppress("UnusedParameter") // Flat version for the v1 set; the id enables per-exercise divergence without an API change.
    fun exerciseVersion(exerciseId: String): Int = EXERCISE
}

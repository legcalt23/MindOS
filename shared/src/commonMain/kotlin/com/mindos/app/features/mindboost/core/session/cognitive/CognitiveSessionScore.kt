package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreState

/**
 * The honest, sealed score of a cognitive session. [normalized] is the ONLY published currency: `0.0..1.0` or
 * `null` when not computable ("—"/calibrating), NEVER a fabricated 0. [category] reuses the canonical honest
 * [ScoreState] (VALID is the only profile-updating state).
 *
 * There is DELIBERATELY no raw score, no per-session delta, and no XP field: a raw/delta/XP economy is Not
 * Applicable BY CHARTER (see [CognitiveSessionContract.NOT_APPLICABLE_BY_CHARTER]) — it would re-introduce the
 * removed "loaded gun". The honest directional signal is a categorical trend computed by the read-model /
 * ingestor across runs, never stored as a mutable "current truth" inside this immutable run.
 */
data class CognitiveSessionScore(
    val normalized: Double?,
    val category: ScoreState,
    val scoringVersion: String,
    val preliminary: Boolean,
    val lowConfidence: Boolean,
) {
    init {
        require(normalized == null || normalized in 0.0..1.0) {
            "normalized must be null or in 0.0..1.0 (was $normalized)."
        }
        require(scoringVersion.isNotBlank()) { "scoringVersion must not be blank." }
        require(category != ScoreState.VALID || normalized != null) {
            "A VALID score must carry a normalized value (invariant null != 0)."
        }
    }
}

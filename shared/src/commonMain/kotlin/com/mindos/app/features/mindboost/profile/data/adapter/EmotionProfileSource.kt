package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileReadOnly
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend as EmotionTrend

/**
 * Adapter: emotions module → EMOTIONAL_REGULATION contribution. Consumes ONLY the stable read-only
 * [EmotionProfileReadOnly] surface (the emotions module evolves independently — never touch its internals).
 *
 * `normalizedScore` is ALWAYS null: the emotion snapshot exposes only a data-maturity `tier` (a
 * session-count ladder EXPLORATORY→STABLE), NOT a regulation-quality score. Mapping the tier to a 0..1
 * value would fabricate competence from "how much data we have". The aggregator surfaces a dimension VALUE
 * only for a contribution that carries a qualifying score, so EMOTIONAL_REGULATION deliberately stays
 * CALIBRATING (value "—", never 0) until the emotions module exposes a VALIDATED regulation score — a
 * deferred, claim-sensitive decision (Ola4: holdout/baseline). The sufficiency/confidence/trend are still
 * passed (for a future score-bearing version), but are not surfaced as a value while the score is null.
 *
 * Regla #8 (never penalize abandonment) holds STRUCTURALLY here: with no score, an abandoned/incomplete/
 * insufficient session — or any session outcome — can never lower this dimension; it can only stay
 * CALIBRATING. Hardened by EmotionProfileSourceTest (boundary) + MindBoostProfileSourcesTest (end-to-end).
 */
class EmotionProfileSource(
    private val readOnly: EmotionProfileReadOnly,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.EMOCIONES

    override suspend fun contribution(userId: String): ModuleContribution? {
        val snapshot = readOnly.snapshot(userId) ?: return null
        return ModuleContribution(
            moduleId = CognitiveModuleId.EMOCIONES,
            normalizedScore = null, // tier is data-maturity, not a quality score — never fabricate a value
            dataSufficiency = snapshot.dataSufficiency.coerceIn(0.0, 1.0),
            confidence = snapshot.confidence.coerceIn(0.0, 1.0),
            trend = snapshot.trend.toProfileTrend(),
            preliminary = snapshot.tier != ProfileTier.STABLE,
            updatedAtMs = 0L, // not exposed by the read-only surface
        )
    }
}

/** The emotions ProfileTrend enum differs from the profile one (STEADY vs STABLE), so map explicitly. */
private fun EmotionTrend.toProfileTrend(): ProfileTrend = when (this) {
    EmotionTrend.IMPROVING -> ProfileTrend.IMPROVING
    EmotionTrend.STEADY -> ProfileTrend.STABLE
    EmotionTrend.DECLINING -> ProfileTrend.DECLINING
    EmotionTrend.UNKNOWN -> ProfileTrend.UNKNOWN
}

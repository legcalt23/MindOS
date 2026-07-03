package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TraitEstimate
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.repository.EvidenceRepository
import com.mindos.app.features.mindboost.language.domain.repository.SessionSummaryRepository
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource

/**
 * Adapter: Lenguaje → VERBAL contribution. Consumes the existing public [EvidenceRepository] +
 * [SessionSummaryRepository].
 *
 * `normalizedScore` is the mean EVIDENCE-LADDER rank (E1..E5) of traits that cleared calibrating,
 * divided by 5 — NOT `thetaMean` (hardcoded 0.0 by the projector) and NOT raw accuracy (ungated). It is
 * null while no trait has measurable, gated evidence (→ CALIBRATING). Confidence rides the module's own
 * `reliability` (down-weighted by practice-effect risk); sufficiency blends session volume and trait
 * breadth; trend rides accuracy over time (used ONLY for trend, never the score). Always `preliminary`
 * (the module carries no calibrated ability).
 *
 * NOTE: this is strength-of-evidence-of-improvement, not an absolute ability percentile.
 */
class LanguageProfileSource(
    private val evidence: EvidenceRepository,
    private val summaries: SessionSummaryRepository,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.LENGUAJE

    override suspend fun contribution(userId: String): ModuleContribution? {
        val hash = UserIdHash(userId.ifBlank { ANON }) // must mirror the module's own keying convention
        val profile = evidence.loadProfile(hash)
        val measured = profile.traitEstimates.values
            .filter { it.evidenceState.isAtLeast(EvidenceState.E1_TRAINED_IMPROVEMENT) }
        val sessions = summaries.summaries(hash)
        if (measured.isEmpty() && sessions.isEmpty()) return null
        return ModuleContribution(
            moduleId = CognitiveModuleId.LENGUAJE,
            normalizedScore = normalizedScore(measured),
            dataSufficiency = dataSufficiency(sessions.size, measured.size),
            confidence = confidence(measured),
            trend = trend(sessions),
            preliminary = true, // the module carries no calibrated ability (thetaMean is hardcoded)
            updatedAtMs = profile.updatedAtEpochMs,
        )
    }

    private fun normalizedScore(measured: List<TraitEstimate>): Double? =
        if (measured.isEmpty()) {
            null
        } else {
            (measured.sumOf { it.evidenceState.rank.toDouble() } / measured.size / MAX_RANK).coerceIn(0.0, 1.0)
        }

    private fun dataSufficiency(sessionCount: Int, measuredCount: Int): Double {
        val volume = (sessionCount / SESSIONS_FOR_FULL).coerceIn(0.0, 1.0)
        val breadth = (measuredCount / TRAITS_FOR_FULL).coerceIn(0.0, 1.0)
        return (volume * HALF + breadth * HALF).coerceIn(0.0, 1.0)
    }

    private fun confidence(measured: List<TraitEstimate>): Double {
        if (measured.isEmpty()) return 0.0
        val meanReliability = measured.map { it.reliability.coerceIn(0.0, 1.0) }.average()
        val meanPracticeRisk = measured.map { it.practiceEffectRisk.coerceIn(0.0, 1.0) }.average()
        return (meanReliability * (1.0 - PRACTICE_PENALTY * meanPracticeRisk)).coerceIn(0.0, 1.0)
    }

    private fun trend(sessions: List<SessionSummary>): ProfileTrend {
        val sorted = sessions.sortedBy { it.completedAtEpochMs }
        val half = sorted.size / 2
        val older = sorted.take(half).mapNotNull { it.accuracy()?.value }
        val newer = sorted.drop(sorted.size - half).mapNotNull { it.accuracy()?.value }
        if (sorted.size < MIN_FOR_TREND || older.isEmpty() || newer.isEmpty()) return ProfileTrend.UNKNOWN
        val delta = newer.average() - older.average()
        return when {
            delta > TREND_EPSILON -> ProfileTrend.IMPROVING
            delta < -TREND_EPSILON -> ProfileTrend.DECLINING
            else -> ProfileTrend.STABLE
        }
    }

    private companion object {
        const val ANON = "anon"
        const val MAX_RANK = 5.0 // EvidenceState.E5 rank
        const val SESSIONS_FOR_FULL = 5.0
        const val TRAITS_FOR_FULL = 5.0
        const val HALF = 0.5
        const val PRACTICE_PENALTY = 0.5
        const val MIN_FOR_TREND = 4
        const val TREND_EPSILON = 0.05 // accuracy proportion
    }
}

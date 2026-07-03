package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TraitEstimate
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
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
 *
 * INTEGRITY GATE (LANGUAGE_EVIDENCE_INTEGRITY_HARDENING, 2026-07-03): before any of the above, this
 * source refuses to feed the OFFICIAL profile unless the backing content is trustworthy. [contentIntegrity]
 * defaults to [BankIntegrity.SMALL_UNVALIDATED] and [provenanceValidated] to `false` — the honest state of
 * Lenguaje today (a demo bank scored by local-stub evaluator/rubric/projector). With those defaults every
 * session — even 5+ completed ones — yields `null`, so VERBAL stays CALIBRATING and never reaches a score,
 * rank, or the Global Score. A future VALIDATED bank + real provenance re-enables the signal by passing
 * `contentIntegrity = BankIntegrity.OK, provenanceValidated = true`. See
 * docs/mindboost_profile_2a/LANGUAGE_EVIDENCE_INTEGRITY_HARDENING_2026-07-03.md. The gate is the single
 * authority of profile eligibility (no persisted `profileEligible` flag), so persistence stays deferred.
 */
class LanguageProfileSource(
    private val evidence: EvidenceRepository,
    private val summaries: SessionSummaryRepository,
    private val contentIntegrity: BankIntegrity = BankIntegrity.SMALL_UNVALIDATED,
    private val provenanceValidated: Boolean = false,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.LENGUAJE

    override suspend fun contribution(userId: String): ModuleContribution? =
        (evaluate(userId) as? LanguageEvidenceDecision.Eligible)?.contribution

    /**
     * Pure, testable eligibility decision. Blocking priority: unvalidated bank → stub provenance →
     * insufficient evidence → eligible. A [LanguageEvidenceDecision.Blocked] maps to a `null` contribution.
     */
    internal suspend fun evaluate(userId: String): LanguageEvidenceDecision {
        integrityBlock()?.let { return it }
        val hash = UserIdHash(userId.ifBlank { ANON }) // must mirror the module's own keying convention
        val profile = evidence.loadProfile(hash)
        val measured = profile.traitEstimates.values
            .filter { it.evidenceState.isAtLeast(EvidenceState.E1_TRAINED_IMPROVEMENT) }
        val sessions = summaries.summaries(hash)
        return if (measured.isEmpty() && sessions.isEmpty()) {
            LanguageEvidenceDecision.Blocked(LanguageEvidenceBlockReason.INSUFFICIENT_VALID_EVIDENCE)
        } else {
            LanguageEvidenceDecision.Eligible(contributionOf(profile, measured, sessions))
        }
    }

    /** Content/provenance gate: refuse the profile before touching evidence. Null = passes the gate. */
    private fun integrityBlock(): LanguageEvidenceDecision.Blocked? = when {
        contentIntegrity.blocksProfileEligibility() ->
            LanguageEvidenceDecision.Blocked(LanguageEvidenceBlockReason.SMALL_UNVALIDATED_BANK)
        !provenanceValidated ->
            LanguageEvidenceDecision.Blocked(LanguageEvidenceBlockReason.STUB_PROVENANCE)
        else -> null
    }

    private fun contributionOf(
        profile: UserLanguageProfile,
        measured: List<TraitEstimate>,
        sessions: List<SessionSummary>,
    ): ModuleContribution = ModuleContribution(
        moduleId = CognitiveModuleId.LENGUAJE,
        normalizedScore = normalizedScore(measured),
        dataSufficiency = dataSufficiency(sessions.size, measured.size),
        confidence = confidence(measured),
        trend = trend(sessions),
        preliminary = true, // the module carries no calibrated ability (thetaMean is hardcoded)
        updatedAtMs = profile.updatedAtEpochMs,
    )

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

package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.reporting.DefaultFocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.DefaultTrendAnalyzer
import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendAnalyzer
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendDirection
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.profile.domain.calibration.CalibrationPolicy
import com.mindos.app.features.mindboost.profile.domain.calibration.CalibrationSession
import com.mindos.app.features.mindboost.core.session.purpose.ProfileMeasurementEligibility
import com.mindos.app.features.mindboost.profile.domain.calibration.ProfileSessionEligibility
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource

/**
 * Adapter: Enfoque → ATTENTION contribution. Consumes existing public focus surfaces only.
 *
 * `normalizedScore` = the module's honest [com.mindos.app.features.mindboost.focus.domain.reporting.FocusScore]
 * (0..100 composite of confident components) divided by 100, or null when no confident component exists
 * (→ CALIBRATING, never a silent 0). The archetype is categorical and is deliberately NOT used as a score.
 * Confidence rides the real `archetypeConfidence` (data-driven), down-weighted while the score band is
 * not yet confident; sufficiency rides `totalSessions`. Always `preliminary` (the FocusScore is explicitly
 * a provisional estimate).
 */
class FocusProfileSource(
    private val history: SessionHistoryRepository,
    private val profiles: FocusProfileRepository,
    private val scoreCalculator: FocusScoreCalculator = DefaultFocusScoreCalculator(),
    private val trendAnalyzer: TrendAnalyzer = DefaultTrendAnalyzer(),
    private val clock: EpochClock = SystemEpochClock,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.ENFOQUE

    // ReturnCount: the D1 fail-closed gate `return null` sits on top of the retained (currently unreachable)
    // wiring-point computation below it, which the carrier-slice ADR will rebuild — kept intact, not deleted.
    @Suppress("ReturnCount")
    override suspend fun contribution(userId: String): ModuleContribution? {
        // SYSTEM-1 6R3 R3-FINAL D1 — FAIL-CLOSED: SessionResult transports no measurement purpose or versioned
        // lineage/scale-key, so the profile-eligibility gate cannot prove this data is measurement-eligible (never
        // training/practice) and DENIES → ATTENTION stays CALIBRATING (charter §7.1: TRAINING_ONLY never updates an
        // estimate; a `preliminary=true` label is NOT a gate). The eligible path (a session model carrying a sealed
        // purpose + complete lineage) is the registered purpose+lineage carrier slice, not implemented here; the code
        // below is its retained wiring point (D2 no cross-version pooling + D3 dedup-gated sufficiency are enforced by
        // THAT slice's estimator), unreachable while the gate denies.
        if (!ProfileMeasurementEligibility.measurementEligible(
                purpose = SessionMeasurementPurpose.UNKNOWN,
                lineageComplete = false,
            )
        ) {
            return null
        }
        val nowMs = clock.nowMillis()
        // De-dup by session id BEFORE counting sufficiency or deriving "latest" (SYSTEM-1 Slice 6-R3, C2.4
        // item 3): two eligible rows sharing the same id must count once, never inflate the sample size.
        val eligible = history.recent(userId, RECENT_LIMIT)
            .filter { it.eligibleForProfile(nowMs) }
            .distinctBy { it.sessionId.value }
        val latest = eligible.firstOrNull() ?: return null
        val focusScore = scoreCalculator.compute(latest.sessionMetrics)
        val profile = profiles.load(userId)
        return ModuleContribution(
            moduleId = CognitiveModuleId.ENFOQUE,
            normalizedScore = focusScore.value?.let { (it / SCORE_MAX).coerceIn(0.0, 1.0) },
            dataSufficiency = ((profile?.totalSessions ?: eligible.size) / MATURE_SESSIONS).coerceIn(0.0, 1.0),
            confidence = confidence(profile?.archetypeConfidence ?: 0.0, focusScore.confident),
            trend = trendOf(eligible),
            preliminary = true, // FocusScore is explicitly a provisional estimate
            updatedAtMs = (profile?.updatedAtMs ?: latest.endedAtMs).takeIf { it > 0L },
            sessionEvidence = sessionEvidenceOf(eligible),
        )
    }

    /**
     * Eligible as PROFILE EVIDENCE: real end time, not in the future, >= 120 s span, AND measurable evidence
     * (non-empty metrics).
     */
    private fun SessionResult.eligibleForProfile(nowMs: Long): Boolean =
        ProfileSessionEligibility.isEligible(startedAtMs, endedAtMs, sessionMetrics.isNotEmpty(), nowMs)

    /** DC-3 cross-session evidence over the already-eligible sessions (CalibrationPolicy still de-dups by id). */
    private fun sessionEvidenceOf(eligible: List<SessionResult>) = CalibrationPolicy.summarize(
        eligible.map {
            CalibrationSession(
                sessionId = it.sessionId.value,
                dayEpoch = it.endedAtMs / MILLIS_PER_DAY,
                valid = true, // eligible by construction; CalibrationPolicy de-dups by id and counts distinct days
            )
        },
    )

    private fun confidence(archetypeConfidence: Double, scoreConfident: Boolean): Double {
        val base = archetypeConfidence.coerceIn(0.0, 1.0)
        return if (scoreConfident) base else (base * UNCERTAIN_FACTOR).coerceIn(0.0, 1.0)
    }

    private fun trendOf(recent: List<SessionResult>): ProfileTrend {
        val trend = trendAnalyzer.analyze(MetricKey.SUSTAINED_FOCUS_INDEX, recent)
        return if (trend.confident) trend.direction.toProfileTrend() else ProfileTrend.UNKNOWN
    }

    private companion object {
        const val RECENT_LIMIT = 30
        const val SCORE_MAX = 100.0
        const val MATURE_SESSIONS = 8.0
        const val UNCERTAIN_FACTOR = 0.5
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

private fun TrendDirection.toProfileTrend(): ProfileTrend = when (this) {
    TrendDirection.IMPROVING -> ProfileTrend.IMPROVING
    TrendDirection.STABLE -> ProfileTrend.STABLE
    TrendDirection.DECLINING -> ProfileTrend.DECLINING
    TrendDirection.INSUFFICIENT -> ProfileTrend.UNKNOWN
}

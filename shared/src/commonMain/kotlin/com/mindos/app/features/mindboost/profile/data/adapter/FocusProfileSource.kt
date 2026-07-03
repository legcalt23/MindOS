package com.mindos.app.features.mindboost.profile.data.adapter

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
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.ENFOQUE

    override suspend fun contribution(userId: String): ModuleContribution? {
        val recent = history.recent(userId, RECENT_LIMIT)
        val latest = recent.firstOrNull() ?: return null
        val focusScore = scoreCalculator.compute(latest.sessionMetrics)
        val profile = profiles.load(userId)
        return ModuleContribution(
            moduleId = CognitiveModuleId.ENFOQUE,
            normalizedScore = focusScore.value?.let { (it / SCORE_MAX).coerceIn(0.0, 1.0) },
            dataSufficiency = ((profile?.totalSessions ?: recent.size) / MATURE_SESSIONS).coerceIn(0.0, 1.0),
            confidence = confidence(profile?.archetypeConfidence ?: 0.0, focusScore.confident),
            trend = trendOf(recent),
            preliminary = true, // FocusScore is explicitly a provisional estimate
            updatedAtMs = profile?.updatedAtMs ?: latest.endedAtMs,
            sessionEvidence = sessionEvidenceOf(recent),
        )
    }

    /** DC-3 cross-session evidence: valid (>=120s) de-duplicated sessions and the distinct days they span. */
    private fun sessionEvidenceOf(recent: List<SessionResult>) = CalibrationPolicy.summarize(
        recent.map {
            CalibrationSession(
                sessionId = it.sessionId.value,
                dayEpoch = it.endedAtMs / MILLIS_PER_DAY,
                valid = it.durationMs >= MIN_VALID_SESSION_MS,
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
        const val MIN_VALID_SESSION_MS = 120_000L // DC-3: a valid session is >= 120 s
    }
}

private fun TrendDirection.toProfileTrend(): ProfileTrend = when (this) {
    TrendDirection.IMPROVING -> ProfileTrend.IMPROVING
    TrendDirection.STABLE -> ProfileTrend.STABLE
    TrendDirection.DECLINING -> ProfileTrend.DECLINING
    TrendDirection.INSUFFICIENT -> ProfileTrend.UNKNOWN
}

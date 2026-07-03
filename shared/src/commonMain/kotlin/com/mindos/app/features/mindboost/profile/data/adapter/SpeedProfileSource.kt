package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.core.scoring.TimingCriticality
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.quality.SpeedAntiGaming
import com.mindos.app.features.mindboost.speed.domain.score.CognitiveProfileScore
import com.mindos.app.features.mindboost.speed.domain.score.SpeedScoreEngine

/**
 * Adapter: Velocidad → PROCESSING_SPEED contribution. Consumes only the existing public
 * [SpeedMeasurementStore] + [SpeedScoreEngine].
 *
 * HONESTY GATE (corrected vs the naive "use score.total"): `SpeedScoreEngine.total` is NEVER null and
 * collapses to a silent 0.0 for a degenerate run (zero correct trials → control bar 0). For the
 * PROCESSING_SPEED dimension we therefore require the actual speed bar to be computable — usefulSpeed
 * (derived from `vce`) non-null and real RT present — otherwise we return null (CALIBRATING), never a
 * fabricated 0. The score is always `preliminary` (the module's physical-validation gate is open), and
 * confidence is capped below the aggregator's ESTABLISHED threshold so VELOCIDAD can never read
 * ESTABLISHED while uncalibrated.
 *
 * TIMEBASE GATE (RT honesty): processing speed is an RT-critical metric, so a run informs the profile only
 * when its timebase is authoritative — Choreographer onset + native-nanos response (Android API 34+). Runs
 * measured with the desktop/iOS monotonic fallback (`SYSTEM_CLOCK_MONOTONIC` / `MILLIS_DERIVED`) or on
 * Android API < 34 are non-authoritative: they remain playable but are excluded here (see
 * [hasAuthoritativeTimebase]). Without this, a fallback run silently classified `ACCEPTABLE` would feed a
 * non-authoritative RT into PROCESSING_SPEED.
 *
 * ANTI-GAMING COVERAGE GATE: an RT-critical metric may feed the profile only with ADEQUATE anti-gaming
 * coverage. Velocidad's `SpeedAntiGaming` is only PARTIAL (it misses subtle fast-guessing), so
 * [contribution] withholds the score (Velocidad stays CALIBRATING) until a §15-calibrated detector exists —
 * closing the latent gap where the bespoke logic could have contributed without any anti-gaming check. The
 * computation itself lives in [eligibleContribution], gated by `SpeedAntiGaming.COVERAGE`.
 */
class SpeedProfileSource(
    private val store: SpeedMeasurementStore,
    private val scoreEngine: SpeedScoreEngine = SpeedScoreEngine(),
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.VELOCIDAD

    /**
     * Velocidad does NOT feed the profile yet. Processing speed is RT-critical, which (per the catalog
     * honesty rule the ScoreResult state machine enforces) requires ADEQUATE anti-gaming coverage — but
     * [SpeedAntiGaming] composes general detectors that miss subtle fast-guessing, so it declares
     * `SpeedAntiGaming.COVERAGE` = PARTIAL. The framework's own `adequateFor(RT_CRITICAL)` returns false for
     * PARTIAL, so the eligible contribution is withheld (Velocidad stays CALIBRATING). When a §15-calibrated
     * detector raises `SpeedAntiGaming.COVERAGE` to ADEQUATE, ALSO run `SpeedAntiGaming.evaluate` per
     * qualifying session here (withholding unclean verdicts); [eligibleContribution] then resumes unchanged.
     */
    override suspend fun contribution(userId: String): ModuleContribution? =
        if (SpeedAntiGaming.COVERAGE.adequateFor(TimingCriticality.RT_CRITICAL)) eligibleContribution(userId) else null

    /** The honest contribution a qualifying authoritative-timebase session WOULD make once coverage is adequate. */
    internal suspend fun eligibleContribution(userId: String): ModuleContribution? {
        val qualifying = qualifyingSessions(userId)
        val latest = qualifying.lastOrNull()
        val metrics = latest?.let { store.sessionMetrics(it.session.id) }
        val normalizedScore = metrics?.let { honestSpeedScore(it, scoreEngine.score(it)) }
        if (latest == null || metrics == null || normalizedScore == null) return null
        return ModuleContribution(
            moduleId = CognitiveModuleId.VELOCIDAD,
            normalizedScore = normalizedScore,
            dataSufficiency = dataSufficiency(qualifying.size, metrics.totalTrials),
            confidence = confidence(qualifying.size, latest.quality, metrics.cv),
            trend = trend(qualifying),
            preliminary = true, // CognitiveProfileScore.preliminary is always true while §15 is open
            updatedAtMs = latest.session.timestampEpochMs,
        )
    }

    /**
     * Sessions sorted oldest→newest, kept only if they may HONESTLY inform the profile: the run is
     * neither LOW nor CONTAMINATED AND its RT timebase is authoritative ([hasAuthoritativeTimebase]).
     * Non-authoritative runs (desktop/iOS fallback, Android API < 34) stay playable but never reach
     * PROCESSING_SPEED, so the count itself (data sufficiency / confidence / trend) only ever reflects
     * authoritative measurements.
     */
    private suspend fun qualifyingSessions(userId: String): List<QualifyingSession> =
        store.sessionsForUser(userId)
            .mapNotNull { session ->
                val quality = store.qualityLogs(session.id).firstOrNull()?.sessionQuality
                when {
                    quality == SessionQuality.CONTAMINATED || quality == SessionQuality.LOW -> null
                    !hasAuthoritativeTimebase(session.id) -> null
                    else -> QualifyingSession(session, quality ?: SessionQuality.ACCEPTABLE)
                }
            }
            .sortedBy { it.session.timestampEpochMs }

    /**
     * A run may inform an RT-critical processing-speed metric ONLY when its timebase is authoritative:
     * Choreographer onset + native-nanos response (Android API 34+). The desktop/iOS monotonic fallback
     * (`SYSTEM_CLOCK_MONOTONIC` / `MILLIS_DERIVED`) and Android API < 34 are non-authoritative and excluded.
     * Mirrors `TimingPolicyClassifier`'s `Authoritative` definition. Absent metrics → unverifiable → excluded.
     */
    private suspend fun hasAuthoritativeTimebase(sessionId: String): Boolean {
        val metrics = store.sessionMetrics(sessionId) ?: return false
        return metrics.timestampSource == TimestampSource.CHOREOGRAPHER_FRAME &&
            metrics.inputTimestampPrecision == InputTimestampPrecision.NANOS_NATIVE
    }

    /** Real processing-speed value or null when the speed bar is not computable (never a silent 0). */
    private fun honestSpeedScore(metrics: SessionMetrics, score: CognitiveProfileScore): Double? =
        if (score.usefulSpeed == null || metrics.pc <= 0.0 || metrics.rtMedianNanos == null) {
            null
        } else {
            score.total
        }

    private fun dataSufficiency(sessionCount: Int, totalTrials: Int): Double {
        val sessionFactor = (sessionCount / TARGET_SESSIONS).coerceIn(0.0, 1.0)
        val trialFactor = (totalTrials / TARGET_TRIALS).coerceIn(0.0, 1.0)
        return minOf(sessionFactor, trialFactor)
    }

    private fun confidence(sessionCount: Int, quality: SessionQuality, cv: Double?): Double {
        val volume = (sessionCount / CONFIDENT_SESSIONS).coerceIn(0.0, 1.0)
        val qualityFactor = when (quality) {
            SessionQuality.HIGH -> QUALITY_HIGH
            SessionQuality.ACCEPTABLE -> QUALITY_ACCEPTABLE
            SessionQuality.LOW, SessionQuality.CONTAMINATED -> QUALITY_DEGRADED
        }
        val stability = (1.0 - (cv ?: HIGH_CV_FALLBACK)).coerceIn(0.0, 1.0)
        return (volume * qualityFactor * stability).coerceIn(0.0, PRELIMINARY_CONFIDENCE_CEILING)
    }

    private suspend fun trend(qualifying: List<QualifyingSession>): ProfileTrend {
        val totals = qualifying.mapNotNull { q ->
            store.sessionMetrics(q.session.id)?.let { m -> honestSpeedScore(m, scoreEngine.score(m)) }
        }
        return if (totals.size < MIN_TREND_SESSIONS) {
            ProfileTrend.UNKNOWN
        } else {
            val delta = totals.last() - median(totals.dropLast(1))
            when {
                delta > TREND_EPSILON -> ProfileTrend.IMPROVING
                delta < -TREND_EPSILON -> ProfileTrend.DECLINING
                else -> ProfileTrend.STABLE
            }
        }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    private data class QualifyingSession(val session: SessionRecord, val quality: SessionQuality)

    private companion object {
        // Adapter-owned honest thresholds (the module exposes no calibrated population norm).
        const val TARGET_SESSIONS = 5.0
        const val TARGET_TRIALS = 50.0
        const val CONFIDENT_SESSIONS = 5.0
        const val HIGH_CV_FALLBACK = 0.30
        const val PRELIMINARY_CONFIDENCE_CEILING = 0.6 // < AggregationConfig.establishedConfidence (0.7)
        const val MIN_TREND_SESSIONS = 3
        const val TREND_EPSILON = 0.03
        const val QUALITY_HIGH = 1.0
        const val QUALITY_ACCEPTABLE = 0.7
        const val QUALITY_DEGRADED = 0.5
    }
}

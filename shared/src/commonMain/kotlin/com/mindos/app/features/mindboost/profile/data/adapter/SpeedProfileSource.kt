package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.core.scoring.TimingCriticality
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
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
 * coverage. Velocidad's `SpeedAntiGaming` is only PARTIAL (a full speed-accuracy-tradeoff reference still
 * needs §15), so [contribution] withholds the score (Velocidad stays CALIBRATING) until a §15-calibrated
 * detector exists. The computation itself lives in [eligibleContribution], gated by `SpeedAntiGaming.COVERAGE`.
 *
 * PER-SESSION ANTI-GAMING VERDICT GATE (Fase 1.5): independently of the coverage claim above,
 * [qualifyingSessions] now RUNS `SpeedAntiGaming.evaluate` over each session's real trials and excludes any
 * session whose verdict is not clean (fast-guessing, rhythm tapping, anticipation flooding, chance-level or
 * partial fast-guessing). So even the preliminary path can never be fed by a gamed session. Task-aware: only
 * confirmed 2-way choice sessions (E3/E6/E7) are tested against chance 0.5; simple-RT E1 or un-inferable
 * content keeps the chance gates idle (fail-closed toward NOT accusing, per the anti-FP charter rule).
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
     * PARTIAL, so the eligible contribution is withheld (Velocidad stays CALIBRATING). The per-session
     * `SpeedAntiGaming.evaluate` gate is ALREADY wired in [qualifyingSessions] (unclean verdicts excluded),
     * so when a §15-calibrated detector raises the COVERAGE to ADEQUATE, [eligibleContribution] resumes
     * unchanged and already detector-gated.
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
            updatedAtMs = latest.session.timestampEpochMs.takeIf { it > 0L },
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
                    !passesAntiGaming(session.id) -> null
                    else -> QualifyingSession(session, quality ?: SessionQuality.ACCEPTABLE)
                }
            }
            .sortedBy { it.session.timestampEpochMs }

    /**
     * Runs the tested [SpeedAntiGaming] battery over the session's REAL trials; an unclean verdict (rhythm
     * tapping, anticipation flooding, chance-level or partial fast-guessing) excludes the session from
     * profile eligibility. Task-aware chance level: only a session whose trials are ALL confirmed 2-way
     * choice exercises (E3/E6/E7) is tested against chance 0.5 with its real accuracy; simple-RT E1 — or any
     * content whose chance level cannot be inferred safely — keeps the chance gates idle (accuracy/chance
     * 1.0, the module's single-tap convention) so the fail-closed direction is "do not accuse", while the
     * task-agnostic detectors (cadence, anticipations) still protect it.
     */
    private suspend fun passesAntiGaming(sessionId: String): Boolean {
        val trials = store.blocksForSession(sessionId).flatMap { store.trialsForBlock(it.id) }
        val pc = store.sessionMetrics(sessionId)?.pc
        val choiceTask = trials.isNotEmpty() && trials.all { it.exerciseId in CHOICE_EXERCISE_CODES }
        val verdict = if (choiceTask && pc != null) {
            SpeedAntiGaming.evaluate(trials, pc, TWO_WAY_CHANCE)
        } else {
            SpeedAntiGaming.evaluate(trials, SINGLE_TAP_ACCURACY, NO_CHANCE_CONCEPT)
        }
        return verdict.isClean
    }

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

        // Task-aware anti-gaming inputs (Fase 1.5). E1 is simple-RT: accuracy 1.0 (any response is correct)
        // plus chanceAccuracy 0.0 ("no chance concept") STRUCTURALLY idles every chance-based gate — the
        // fail-closed direction for a non-choice task is "do not accuse", never "block".
        const val SINGLE_TAP_ACCURACY = 1.0
        const val NO_CHANCE_CONCEPT = 0.0
        const val TWO_WAY_CHANCE = 0.5
        val CHOICE_EXERCISE_CODES = setOf(
            SpeedExerciseId.E2_COMPARA.code,
            SpeedExerciseId.E4_CAMBIO.code,
            SpeedExerciseId.E3_CIFRADO.code,
            SpeedExerciseId.E6_MULTIVIA.code,
            SpeedExerciseId.E7_COMPUERTA.code,
        )
    }
}

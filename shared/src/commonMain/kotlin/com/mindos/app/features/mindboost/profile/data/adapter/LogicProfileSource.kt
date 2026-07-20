package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
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
 * Adapter: Lógica → REASONING contribution. Consumes the existing public [LogicTrainingRepository].
 *
 * `normalizedScore` = mean of the most recent sessions' `averageIcr` (a real 0..100 reasoning-practice
 * score) divided by 100. It is non-null whenever ≥1 session exists; honesty is carried by sufficiency
 * (a single session is below the aggregator gate, so REASONING stays CALIBRATING) and by a confidence
 * that blends session volume with per-skill learned precision (1 − uncertainty). Always `preliminary`:
 * the ICR scorer is heuristic and not physically validated.
 */
class LogicProfileSource(
    private val repository: LogicTrainingRepository,
    private val clock: EpochClock = SystemEpochClock,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.LOGICA

    // ReturnCount: the D1 fail-closed gate `return null` sits on top of the retained (currently unreachable)
    // wiring-point computation below it, which the carrier-slice ADR will rebuild — kept intact, not deleted.
    @Suppress("ReturnCount")
    override suspend fun contribution(userId: String): ModuleContribution? {
        // SYSTEM-1 6R3 R3-FINAL D1 — FAIL-CLOSED: LogicSession transports no measurement purpose and no versioned
        // scoringVersion/scale-key, so the profile-eligibility gate cannot prove this data is measurement-eligible
        // (never training/practice) NOR comparability-established (D2), and DENIES → REASONING stays CALIBRATING
        // (charter §7.1 + §9.3: no cross-version mean; a `preliminary=true` label is NOT a gate). The eligible path is
        // the registered purpose+lineage carrier slice, not implemented here; the code below is its retained wiring
        // point (its estimator must partition by scale-key and derive sufficiency from the eligible set), unreachable
        // while the gate denies.
        if (!ProfileMeasurementEligibility.measurementEligible(
                purpose = SessionMeasurementPurpose.UNKNOWN,
                lineageComplete = false,
            )
        ) {
            return null
        }
        val nowMs = clock.nowMillis()
        // De-dup by session id BEFORE counting sufficiency or averaging the recent score (SYSTEM-1 Slice
        // 6-R3, C2.4 item 3): two eligible rows sharing the same id must count once, never inflate the
        // sample size or skew the mean.
        val sessions = repository.recentSessions(userId, RECENT_LIMIT)
            .filter { it.eligibleForProfile(nowMs) }
            .distinctBy { it.id }
        if (sessions.isEmpty()) return null
        val skillStates = repository.loadSkillStates(userId)
        val recentScore = sessions.take(RECENCY_WINDOW).map { it.result.averageIcr }.average()
        return ModuleContribution(
            moduleId = CognitiveModuleId.LOGICA,
            normalizedScore = (recentScore / SCORE_MAX).coerceIn(0.0, 1.0),
            dataSufficiency = (sessions.size / SUFFICIENT_SESSIONS).coerceIn(0.0, 1.0),
            confidence = confidence(sessions.size, skillStates),
            trend = trend(sessions),
            preliminary = true, // the ICR scorer is heuristic / not physically validated
            updatedAtMs = sessions.first().endedAt.takeIf { it > 0L },
            sessionEvidence = sessionEvidenceOf(sessions),
        )
    }

    /**
     * Eligible as PROFILE EVIDENCE: real end time, not in the future, >= 120 s span, AND measurable evidence
     * (>= 1 attempt).
     */
    private fun LogicSession.eligibleForProfile(nowMs: Long): Boolean =
        ProfileSessionEligibility.isEligible(startedAt, endedAt, attempts.isNotEmpty(), nowMs)

    /** DC-3 cross-session evidence over the already-eligible sessions (CalibrationPolicy still de-dups by id). */
    private fun sessionEvidenceOf(sessions: List<LogicSession>) = CalibrationPolicy.summarize(
        sessions.map {
            CalibrationSession(
                sessionId = it.id,
                dayEpoch = it.endedAt / MILLIS_PER_DAY,
                valid = true, // eligible by construction; CalibrationPolicy de-dups by id and counts distinct days
            )
        },
    )

    private fun confidence(sessionCount: Int, skillStates: List<SkillState>): Double {
        val volume = (sessionCount / CONFIDENT_SESSIONS).coerceIn(0.0, 1.0)
        val precision = if (skillStates.isEmpty()) {
            0.0
        } else {
            skillStates.map { (1.0 - it.uncertainty).coerceIn(0.0, 1.0) }.average()
        }
        return ((volume + precision) / 2.0).coerceIn(0.0, 1.0)
    }

    /** Newer-half vs older-half average ICR, with a deadband; UNKNOWN until enough sessions. */
    private fun trend(sessions: List<LogicSession>): ProfileTrend {
        if (sessions.size < MIN_FOR_TREND) return ProfileTrend.UNKNOWN
        val chronological = sessions.asReversed() // oldest → newest
        val half = chronological.size / 2
        val older = chronological.take(half)
        val newer = chronological.drop(chronological.size - half)
        val delta = newer.map { it.result.averageIcr }.average() - older.map { it.result.averageIcr }.average()
        return when {
            delta > TREND_EPSILON -> ProfileTrend.IMPROVING
            delta < -TREND_EPSILON -> ProfileTrend.DECLINING
            else -> ProfileTrend.STABLE
        }
    }

    private companion object {
        const val RECENT_LIMIT = 30
        const val RECENCY_WINDOW = 5
        const val SCORE_MAX = 100.0
        const val SUFFICIENT_SESSIONS = 5.0
        const val CONFIDENT_SESSIONS = 8.0
        const val MIN_FOR_TREND = 4
        const val TREND_EPSILON = 3.0 // ICR points (0..100 scale)
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

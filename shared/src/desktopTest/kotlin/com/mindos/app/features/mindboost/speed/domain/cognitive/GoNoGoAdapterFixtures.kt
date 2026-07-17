package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.SessionCompletionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegritySnapshot
import com.mindos.app.features.mindboost.core.session.cognitive.WriteAheadVerdict
import com.mindos.app.features.mindboost.speed.domain.score.GONOGO_SCORE_VERSION
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOutcome
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoQualityDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoResponse
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScorer
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoStimulus
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType

/**
 * Builds real E8 outcomes (via [GoNoGoOutcome.resolve], the production path) and the derived
 * [GoNoGoRunArtifacts] for adapter tests, so the mapping is exercised against genuine resolved trials rather
 * than hand-forged flags.
 */
internal object GoNoGoAdapterFixtures {

    const val SESSION_ID = "sess_e8_abcdef012345"
    const val EXERCISE_ID = "e8_inhibicion"
    const val EXERCISE_VERSION = "1.0.0"
    val SCORING_VERSION = GONOGO_SCORE_VERSION

    private const val DEADLINE_MS = 600L
    private const val FOREPERIOD_MS = 400L

    private fun goStimulus(i: Int, difficulty: Int = 1) =
        GoNoGoStimulus(GoNoGoTrialType.GO, "go_$i", "fam", DEADLINE_MS, FOREPERIOD_MS, difficulty, i, "cfg-1")

    private fun noGoStimulus(i: Int, difficulty: Int = 1) =
        GoNoGoStimulus(GoNoGoTrialType.NO_GO, "nogo_$i", "fam", DEADLINE_MS, FOREPERIOD_MS, difficulty, i, "cfg-1")

    /** GO tap within the deadline ⇒ ACTIVE_CORRECT. */
    fun goHit(i: Int, rtMs: Long = 300L) = GoNoGoOutcome.resolve(goStimulus(i), GoNoGoResponse(false, rtMs))

    /** GO with no tap ⇒ OMISSION. */
    fun goOmission(i: Int) = GoNoGoOutcome.resolve(goStimulus(i), GoNoGoResponse(false, null))

    /** GO tap after the deadline ⇒ LATE_RESPONSE. */
    fun goLate(i: Int, rtMs: Long = 700L) = GoNoGoOutcome.resolve(goStimulus(i), GoNoGoResponse(false, rtMs))

    /** GO tap during the foreperiod ⇒ ANTICIPATION. */
    fun goAnticipation(i: Int) = GoNoGoOutcome.resolve(goStimulus(i), GoNoGoResponse(true, null))

    /** NO-GO with no tap ⇒ CORRECT_INHIBITION (no reaction time exists). */
    fun noGoInhibition(i: Int) = GoNoGoOutcome.resolve(noGoStimulus(i), GoNoGoResponse(false, null))

    /** NO-GO tap after onset ⇒ COMMISSION. */
    fun noGoCommission(i: Int, rtMs: Long = 200L) = GoNoGoOutcome.resolve(noGoStimulus(i), GoNoGoResponse(false, rtMs))

    /** NO-GO tap in the foreperiod AND after onset ⇒ BOTH anticipation and commission flags fire on one trial. */
    fun noGoAnticipationAndCommission(i: Int, rtMs: Long = 200L) =
        GoNoGoOutcome.resolve(noGoStimulus(i), GoNoGoResponse(true, rtMs))

    fun artifacts(outcomes: List<GoNoGoOutcome>, missingOnsetTaps: Int = 0): GoNoGoRunArtifacts {
        val summary = GoNoGoRunSummary.from(outcomes)
        val metrics = GoNoGoMetricsMapper.metrics(summary)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes, missingOnsetTaps)
        val score = GoNoGoScorer.score(metrics, diagnostics)
        return GoNoGoRunArtifacts(outcomes, summary, metrics, score, diagnostics)
    }

    /** 8 correct GO + 5 correct NO-GO ⇒ sufficient sample, computable index, Measured RTs, CLEAN timing. */
    fun sufficientCleanOutcomes(): List<GoNoGoOutcome> {
        val rts = listOf(280L, 300L, 320L, 290L, 310L, 305L, 295L, 315L)
        val go = rts.mapIndexed { i, rt -> goHit(i, rt) }
        val noGo = (0 until 5).map { noGoInhibition(100 + it) }
        return go + noGo
    }

    fun eligibleVerdict() = WriteAheadVerdict(eligible = true, anomalyFlags = emptyList())

    fun run(
        artifacts: GoNoGoRunArtifacts = artifacts(sufficientCleanOutcomes()),
        sessionId: String = SESSION_ID,
        sessionPurpose: SessionPurpose = SessionPurpose.TRAINING_FREE,
        writeAhead: WriteAheadVerdict = eligibleVerdict(),
        timing: TimingIntegritySnapshot =
            GoNoGoCognitiveSessionAdapter.timingSnapshotFor(artifacts.score.timingIntegrity),
        plannedTrials: Int = 40,
        completionStatus: SessionCompletionStatus = SessionCompletionStatus.COMPLETED,
        startedAtEpochMs: Long = 1_000L,
        completedAtEpochMs: Long? = 121_000L,
        durationActualMs: Long = 120_000L,
    ): CognitiveSessionRun = GoNoGoCognitiveSessionAdapter.toCognitiveSessionRun(
        artifacts = artifacts,
        sessionId = sessionId,
        exerciseId = EXERCISE_ID,
        exerciseVersion = EXERCISE_VERSION,
        scoringVersion = SCORING_VERSION,
        sessionPurpose = sessionPurpose,
        writeAhead = writeAhead,
        timing = timing,
        plannedTrials = plannedTrials,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        durationActualMs = durationActualMs,
        createdAtEpochMs = 121_000L,
        completionStatus = completionStatus,
    )
}

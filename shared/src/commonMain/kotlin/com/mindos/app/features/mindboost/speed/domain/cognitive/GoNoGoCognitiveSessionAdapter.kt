// A boundary adapter that assembles ONE immutable CognitiveSessionRun from the many cohesive, already-computed
// E8 artifacts + lifecycle inputs (LongParameterList) via one small pure helper per nested field
// (TooManyFunctions). That width/decomposition IS the shape of a mapping layer, not a smell.
@file:Suppress("LongParameterList", "TooManyFunctions")

package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.session.cognitive.AnswerSummary
import com.mindos.app.features.mindboost.core.session.cognitive.BackgroundObservation
import com.mindos.app.features.mindboost.core.session.cognitive.ClockSource
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionMetrics
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionScore
import com.mindos.app.features.mindboost.core.session.cognitive.DifficultyTrace
import com.mindos.app.features.mindboost.core.session.cognitive.EligibilityEvaluation
import com.mindos.app.features.mindboost.core.session.cognitive.EligibilityInputs
import com.mindos.app.features.mindboost.core.session.cognitive.EligibilityTrace
import com.mindos.app.features.mindboost.core.session.cognitive.FinalizationFailureReason
import com.mindos.app.features.mindboost.core.session.cognitive.MetricUnavailableReason
import com.mindos.app.features.mindboost.core.session.cognitive.OutcomeCount
import com.mindos.app.features.mindboost.core.session.cognitive.PlatformSnapshot
import com.mindos.app.features.mindboost.core.session.cognitive.SessionCompletionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.SessionEligibilityPolicy
import com.mindos.app.features.mindboost.core.session.cognitive.SessionMetric
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegritySnapshot
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegrityStatus
import com.mindos.app.features.mindboost.core.session.cognitive.TrialAggregate
import com.mindos.app.features.mindboost.core.session.cognitive.TrialOutcomeKind
import com.mindos.app.features.mindboost.core.session.cognitive.WriteAheadVerdict
import com.mindos.app.features.mindboost.core.session.cognitive.asMeasurementPurpose
import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetrics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOutcome
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScore
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoSignal
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import com.mindos.app.features.mindboost.speed.domain.score.TimingIntegrityLevel

/**
 * The already-computed E8 · Go/No-Go artifacts of ONE finished run, as produced by the session driver. The
 * adapter RE-USES these (it never re-scores): [score] is the honest index the scorer already sealed, and the
 * counts/RTs come from the same resolved [outcomes]. Passing them together keeps the adapter a pure transform.
 */
data class GoNoGoRunArtifacts(
    val outcomes: List<GoNoGoOutcome>,
    val summary: GoNoGoRunSummary,
    val metrics: GoNoGoMetrics,
    val score: GoNoGoScore,
    val diagnostics: GoNoGoDiagnostics,
)

/**
 * SYSTEM-1 slice 4 · PURE adapter: E8 · Go/No-Go run artifacts + lifecycle inputs → one sealed
 * [CognitiveSessionRun]. It reads NO stores, touches NO UI, and has NO side effects — the caller supplies the
 * existing `SessionRecord.sessionId`, the [WriteAheadVerdict], the already-computed [GoNoGoScore], the real
 * timestamps and versions, and the [SessionPurpose].
 *
 * Honesty guarantees enforced here:
 * - Each resolved outcome is classified into EXACTLY ONE [TrialOutcomeKind] (a NO-GO trial that is both a
 *   premature response AND a post-onset tap is counted ONCE as [TrialOutcomeKind.ANTICIPATION], never double);
 *   so `trials.completed == sum(outcome counts)`.
 * - A correct NO-GO inhibition maps to [TrialOutcomeKind.CORRECT_INHIBITION] with NO fabricated reaction time
 *   and is NEVER turned into a "correct RT tap".
 * - Lost taps (`missingOnsetTaps`) are surfaced as the run's LOST_INPUT signal — an anomaly flag plus a counted
 *   field in the exercise payload — never a fabricated trial or RT.
 * - `score.normalized` is the E8 index (0..1 or null); there is NO raw/delta/XP.
 * - Eligibility is decided by the ONE authority [SessionEligibilityPolicy], gated by the write-ahead verdict:
 *   a missing/unreadable/read-failed/abandoned/not-finalized/mismatch/incoherent write-ahead ⇒ all lanes false.
 * - E8 is fixed-count with no user-selected duration (`durationSelectedMs = null`, honest, not anomalous);
 *   it declares NO approved-protocol equivalent, so calibration requires `durationActualMs >= 120_000`.
 *
 * MEASUREMENT-QUALITY KILL-SWITCH (executable, in the RUN — not a KDoc promise). `eligibility.*Eligible` is
 * HARD-GATED so that the official lanes are false whenever ANY of: `score.preliminary`, `score.category !=
 * VALID`, `score.lowConfidence`, a non-authoritative timebase, or missing anti-gaming coverage. A future profile
 * INGESTOR therefore CANNOT consume a run built here as official even if it forgets its own gate — the run
 * itself denies the lanes and records WHY in [CognitiveSessionRun.eligibilityTrace]`.rejectionReasons` (also
 * copied into `anomalyFlags`). The GENERIC lifecycle gates (write-ahead / duration / purpose / integrity /
 * data-quality) stay individually visible in the trace, so a preliminary-but-otherwise-clean run is NOT masked.
 *
 * TODAY every E8 run is denied every official lane: the scorer keeps `preliminary = true` while the §15
 * physical-validation gate is open, [timingSnapshotFor] emits `ClockSource.UNKNOWN` (the caller must supply a
 * real authoritative [ClockSource] — the 2026-06-30 rule: `FRAME_MONOTONIC` = Choreographer + native-nanos), and
 * [antiGamingCoverageSatisfied] defaults FAIL-CLOSED to `false`. The run is still fully savable to HISTORY; it
 * simply never moves the profile/calibration/ranking until those measurement gates are genuinely satisfied.
 */
object GoNoGoCognitiveSessionAdapter {

    fun toCognitiveSessionRun(
        artifacts: GoNoGoRunArtifacts,
        sessionId: String,
        exerciseId: String,
        exerciseVersion: String,
        scoringVersion: String,
        sessionPurpose: SessionPurpose,
        writeAhead: WriteAheadVerdict,
        timing: TimingIntegritySnapshot,
        plannedTrials: Int,
        startedAtEpochMs: Long,
        completedAtEpochMs: Long?,
        durationActualMs: Long,
        createdAtEpochMs: Long,
        completionStatus: SessionCompletionStatus = SessionCompletionStatus.COMPLETED,
        finalizationFailure: FinalizationFailureReason? = null,
        platform: PlatformSnapshot? = null,
        // FAIL-CLOSED: without proof of a tested, adequate-coverage anti-gaming detector, the run cannot claim an
        // official lane. The productive caller supplies the real coverage verdict; today E8 has none (PARTIAL).
        antiGamingCoverageSatisfied: Boolean = false,
    ): CognitiveSessionRun {
        val outcomes = artifacts.outcomes
        val partition = outcomes.groupingBy { classify(it) }.eachCount()
        val eval = eligibilityOf(
            sessionPurpose, completionStatus, durationActualMs, outcomes.size,
            artifacts.score, timing, writeAhead, antiGamingCoverageSatisfied,
        )
        return CognitiveSessionRun(
            sessionId = sessionId,
            blockId = ScoreModuleId.SPEED,
            exerciseId = exerciseId,
            exerciseVersion = exerciseVersion,
            scoringVersion = scoringVersion,
            sessionPurpose = sessionPurpose,
            startedAtEpochMs = startedAtEpochMs,
            completedAtEpochMs = completedAtEpochMs,
            durationSelectedMs = null,
            durationActualMs = durationActualMs,
            completionStatus = completionStatus,
            finalizationFailure = finalizationFailure,
            difficulty = difficultyTraceOf(artifacts.summary),
            trials = trialsOf(partition, outcomes.size, plannedTrials),
            answerSummary = answersOf(partition, outcomes.size),
            metrics = metricsOf(artifacts.summary, artifacts.metrics),
            exerciseMetrics = GoNoGoInhibitionPayloadCodec.encode(payloadOf(artifacts)),
            score = scoreOf(artifacts.score, scoringVersion),
            eligibility = eval.eligibility,
            eligibilityTrace = eval.trace,
            calibrationEffect = null,
            anomalyFlags = anomalyFlagsOf(artifacts.diagnostics, writeAhead, eval.trace),
            platform = platform,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = createdAtEpochMs,
        )
    }

    /** Honest device-clock snapshot for an E8 run: the scoring integrity LEVEL is known; the per-field device
     *  clock provenance is deferred device-QA, so it stays [ClockSource.UNKNOWN] — never a fabricated CLEAN clock. */
    fun timingSnapshotFor(level: TimingIntegrityLevel): TimingIntegritySnapshot = TimingIntegritySnapshot(
        clockSource = ClockSource.UNKNOWN,
        monotonicAvailable = false,
        // FOC-P0-03: no lifecycle sensor is wired into this lane; "not watched" is never "kept the foreground".
        background = BackgroundObservation.NotObserved,
        pauseResumeCount = 0,
        missedTickFlags = 0,
        suspectedInputLatency = false,
        integrityStatus = when (level) {
            TimingIntegrityLevel.CLEAN -> TimingIntegrityStatus.CLEAN
            TimingIntegrityLevel.MINOR_ANOMALIES -> TimingIntegrityStatus.MINOR_ANOMALIES
            TimingIntegrityLevel.COMPROMISED -> TimingIntegrityStatus.COMPROMISED
        },
    )

    /** Classifies one resolved outcome into EXACTLY ONE kind. Anticipation wins first, so a NO-GO trial that is
     *  both premature and a post-onset tap is counted once (ANTICIPATION), never double. */
    private fun classify(o: GoNoGoOutcome): TrialOutcomeKind = when {
        o.anticipation -> TrialOutcomeKind.ANTICIPATION
        o.correct && o.trialType == GoNoGoTrialType.GO -> TrialOutcomeKind.ACTIVE_CORRECT
        o.correct && o.trialType == GoNoGoTrialType.NO_GO -> TrialOutcomeKind.CORRECT_INHIBITION
        o.commission -> TrialOutcomeKind.COMMISSION
        o.omission -> TrialOutcomeKind.OMISSION
        o.lateResponse -> TrialOutcomeKind.LATE_RESPONSE
        else -> TrialOutcomeKind.INVALID_RESPONSE
    }

    private fun trialsOf(partition: Map<TrialOutcomeKind, Int>, completed: Int, planned: Int): TrialAggregate =
        TrialAggregate(
            planned = planned,
            completed = completed,
            outcomes = TrialOutcomeKind.entries
                .filter { partition.containsKey(it) }
                .map { OutcomeCount(it, partition.getValue(it)) },
        )

    private fun answersOf(partition: Map<TrialOutcomeKind, Int>, total: Int): AnswerSummary {
        val correct = (partition[TrialOutcomeKind.ACTIVE_CORRECT] ?: 0) +
            (partition[TrialOutcomeKind.CORRECT_INHIBITION] ?: 0)
        val omissions = partition[TrialOutcomeKind.OMISSION] ?: 0
        return AnswerSummary(total = total, correct = correct, incorrect = total - correct - omissions,
            omissions = omissions)
    }

    /** E8 difficulty is an ORDINAL level, not a 0..1 normalization — so start/end/max stay null (never fabricated);
     *  only whether the run mixed levels is honestly derivable. */
    private fun difficultyTraceOf(summary: GoNoGoRunSummary): DifficultyTrace = DifficultyTrace(
        start = null,
        end = null,
        maxReached = null,
        interleaved = summary.byDifficulty.map { it.difficultyLevel }.toSet().size > 1,
        perBlock = emptyList(),
    )

    private fun metricsOf(summary: GoNoGoRunSummary, metrics: GoNoGoMetrics): CognitiveSessionMetrics {
        val rts = summary.correctGoRtsMs
        return CognitiveSessionMetrics(
            accuracy = metrics.inhibitoryControlIndex.toSessionMetric(),
            reactionTimeMedianMs = metrics.correctGoSpeedMs.toSessionMetric(),
            reactionTimeMeanMs = rtStat(rts, SpeedRtStatistics.meanOrNull(rts)),
            reactionTimeMinMs = rtStat(rts, rts.minOrNull()),
            reactionTimeMaxMs = rtStat(rts, rts.maxOrNull()),
            reactionTimeVariabilityCv = metrics.correctGoRtVariability.toSessionMetric(),
        )
    }

    /** An RT stat is Measured only with a sufficient correct-GO sample (same gate as the telemetry summary);
     *  otherwise Unavailable — never a fabricated 0. */
    private fun rtStat(rts: List<Double>, value: Double?): SessionMetric =
        if (rts.size >= GoNoGoMetricsMapper.MIN_CORRECT_GO && value != null) {
            SessionMetric.Measured(value)
        } else {
            SessionMetric.Unavailable(MetricUnavailableReason.INSUFFICIENT_SAMPLE)
        }

    private fun GoNoGoSignal.toSessionMetric(): SessionMetric = when (this) {
        is GoNoGoSignal.Measured -> SessionMetric.Measured(value)
        is GoNoGoSignal.Unavailable -> SessionMetric.Unavailable(MetricUnavailableReason.INSUFFICIENT_SAMPLE)
    }

    private fun scoreOf(score: GoNoGoScore, scoringVersion: String): CognitiveSessionScore {
        val category = scoreStateOf(score)
        return CognitiveSessionScore(
            // Charter (null != 0): ONLY a VALID score publishes a number. A DEVICE_UNRELIABLE run's capped index
            // is not trustworthy, so it renders "—" (null) here — the raw capped value never leaves the E8 layer.
            normalized = score.index.takeIf { category == ScoreState.VALID },
            category = category,
            scoringVersion = scoringVersion,
            preliminary = score.preliminary,
            lowConfidence = score.lowConfidence,
        )
    }

    /** Honest state mapping (safety verdict wins over evidence verdict, matching the ScoreState cascade):
     *  compromised timing ⇒ DEVICE_UNRELIABLE; no computable index ⇒ CALIBRATING; otherwise VALID. */
    private fun scoreStateOf(score: GoNoGoScore): ScoreState = when {
        score.dataQualityCompromised -> ScoreState.DEVICE_UNRELIABLE
        score.index == null -> ScoreState.CALIBRATING
        else -> ScoreState.VALID
    }

    private fun eligibilityOf(
        purpose: SessionPurpose,
        completionStatus: SessionCompletionStatus,
        durationActualMs: Long,
        stimuliShown: Int,
        score: GoNoGoScore,
        timing: TimingIntegritySnapshot,
        writeAhead: WriteAheadVerdict,
        antiGamingCoverageSatisfied: Boolean,
    ): EligibilityEvaluation = SessionEligibilityPolicy.evaluate(
        EligibilityInputs(
            purpose = purpose.asMeasurementPurpose(),
            // E8 carries no versioned lineage, so it declares that honestly and is denied the PROFILE lane
            // (Focus V5 · S1 · ADR-FOCUS-PURPOSE); calibration/ranking are unaffected. Not a regression: before
            // S1 the retired predicate answered `true` here for a TRAINING_FREE run with no lineage at all,
            // which is exactly the over-permissive claim the ADR removed. No DECISION consumed it: the evidence
            // layer re-derives its own purpose gate, and the one reader (CognitiveSessionHistory's
            // `mayCountTowardProfile`) is a display field. The value was already false in practice, because E8
            // stays `preliminary` while the §15 physical gate is open. When E8 gains a sealed measurement
            // purpose + lineage, THIS is the line that changes.
            lineageComplete = false,
            completionStatus = completionStatus,
            durationActualMs = durationActualMs,
            // E8 declares no validated fixed-count protocol yet ⇒ only the 120s wall-clock rule passes calibration.
            approvedProtocol = null,
            completedTrials = stimuliShown,
            stimuliShown = stimuliShown,
            dataQualityCompromised = score.dataQualityCompromised,
            timing = timing,
            writeAheadPresent = writeAhead.eligible,
            // Measurement-quality kill-switch inputs (all fail-closed): the official lanes stay false until E8 is
            // final (not preliminary), VALID, confident, on an authoritative timebase, and anti-gaming-covered.
            measurementPreliminary = score.preliminary,
            scoreCategoryValid = scoreStateOf(score) == ScoreState.VALID,
            lowConfidence = score.lowConfidence,
            authoritativeTimebaseSatisfied = isAuthoritativeTimebase(timing),
            antiGamingCoverageSatisfied = antiGamingCoverageSatisfied,
        ),
    )

    /** The 2026-06-30 rule for an RT-critical signal: only a frame-monotonic clock (Choreographer + native nanos),
     *  monotonic and not COMPROMISED, is authoritative. E8's [timingSnapshotFor] emits UNKNOWN ⇒ not authoritative. */
    private fun isAuthoritativeTimebase(timing: TimingIntegritySnapshot): Boolean =
        timing.clockSource == ClockSource.FRAME_MONOTONIC &&
            timing.monotonicAvailable &&
            timing.integrityStatus != TimingIntegrityStatus.COMPROMISED

    /** Anomaly flags = measurement anomalies + write-ahead anomalies + the eligibility rejection reasons (so a
     *  consumer scanning `anomalyFlags` alone still sees WHY the official lanes were denied). */
    private fun anomalyFlagsOf(
        diagnostics: GoNoGoDiagnostics,
        writeAhead: WriteAheadVerdict,
        trace: EligibilityTrace,
    ): List<String> = (diagnostics.activeFlags() + writeAhead.anomalyFlags + trace.rejectionReasons).distinct()

    private fun payloadOf(artifacts: GoNoGoRunArtifacts): GoNoGoInhibitionPayload {
        val s = artifacts.summary
        val score = artifacts.score
        return GoNoGoInhibitionPayload(
            goTrials = s.goTrials,
            noGoTrials = s.noGoTrials,
            goCorrect = s.goCorrect,
            noGoCorrect = s.noGoCorrect,
            commissions = s.commissions,
            omissions = s.omissions,
            anticipations = s.anticipations,
            lateResponses = s.lateResponses,
            fastCommissions = s.fastCommissions,
            lostInputTaps = artifacts.diagnostics.missingOnsetTaps,
            category = score.category.name,
            timingIntegrityLevel = score.timingIntegrity.name,
            commissionCapApplied = score.commissionCapApplied,
            goAccuracyCapApplied = score.goAccuracyCapApplied,
            anticipationCapApplied = score.anticipationCapApplied,
            lowConfidence = score.lowConfidence,
        )
    }
}

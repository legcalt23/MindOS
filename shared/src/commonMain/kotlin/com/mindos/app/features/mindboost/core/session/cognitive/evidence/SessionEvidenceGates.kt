package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.session.cognitive.ClockSource
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.SessionCompletionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegrityStatus

/**
 * SYSTEM-1 6R3 · Per-run classification for the evidence extractor. It RE-DERIVES every condition it can from the
 * run's RAW facts (completion, data quality, timing integrity, VALID/preliminary/confident, duration, purpose)
 * and — critically for P1-06 — does NOT trust the DERIVED eligibility booleans
 * (`profileEligible`/`calibrationEligible`/`rankingEligible`), which are a forgeable plain-Boolean data class. The
 * RT-criticality gate is re-derived from `clockSource` and a per-exercise [RtCriticalityRegistry] lookup (§18 C4),
 * never a trusted flag — see [timebaseExclusionFor]. Purpose is re-derived from the run's own [SessionPurpose]
 * value directly (6R3-P1-06), not the shared, forgeable `mayContributeToProfile()` derivation, because a full
 * TRAINING_FREE session must be excluded from THIS evidence-only contract even though that shared method returns
 * true for it. The remaining upstream condition (anti-gaming coverage) is NOT a fact on the run and therefore NOT
 * independently verifiable here; because this contract makes NO official-eligibility claim (evidence-only), it
 * does not need it — a future official bridge must add the anti-gaming fact (W-precondition).
 */
internal object SessionEvidenceGates {

    private const val MILLIS_PER_DAY: Long = 86_400_000L

    /** null ⇒ the run qualifies for an evidence group; otherwise the FIRST reason it does not (re-derived).
     *  [registry] defaults to the fail-closed PRODUCTION [RtCriticalityRegistry]; a caller (test only, today —
     *  the extractor's own default does the same) may inject a different one. */
    fun qualifyingExclusion(
        run: CognitiveSessionRun,
        registry: RtCriticalityRegistry = RtCriticalityRegistry(),
    ): ExclusionReason? {
        val timingCompromised =
            run.eligibility.timingIntegrity.integrityStatus == TimingIntegrityStatus.COMPROMISED
        val criticality = registry.criticalityOf(run.exerciseId, run.exerciseVersion)
        val timebaseFailure = timebaseExclusionFor(criticality, run)
        return when {
            run.completionStatus != SessionCompletionStatus.COMPLETED -> ExclusionReason.NOT_COMPLETED
            run.eligibility.dataQualityCompromised -> ExclusionReason.DATA_QUALITY
            timingCompromised -> ExclusionReason.TIMING_COMPROMISED
            timebaseFailure != null -> timebaseFailure
            run.score.category != ScoreState.VALID -> ExclusionReason.NOT_VALID
            run.score.preliminary -> ExclusionReason.PRELIMINARY
            run.score.lowConfidence -> ExclusionReason.LOW_CONFIDENCE
            !purposeQualifies(run) -> ExclusionReason.PURPOSE_EXCLUDED
            else -> null
        }
    }

    /**
     * 6R3-P1-06 · Re-derived purpose gate, independent of the shared (and forgeable-by-drift) `mayContributeToProfile`
     * method: [SessionPurpose.PRACTICE] / [SessionPurpose.DEBUG] never qualify (unchanged), and a full
     * [SessionPurpose.TRAINING_FREE] session ALSO never qualifies here — this contract has no sealed,
     * exposure-controlled probe-identity + within-dose fact on the run that would make a TRAINING_FREE session
     * evidence-bearing (03 §7.1), so it stays TRAINING_ONLY (fail-closed) regardless of what the shared method
     * returns for the live profile route.
     */
    private fun purposeQualifies(run: CognitiveSessionRun): Boolean = when (run.sessionPurpose) {
        SessionPurpose.PRACTICE, SessionPurpose.DEBUG, SessionPurpose.TRAINING_FREE -> false
        SessionPurpose.INITIAL_ASSESSMENT, SessionPurpose.TRAINING_RECOMMENDED, SessionPurpose.CALIBRATION -> true
    }

    /**
     * §18 C4 (stricter than the retired mechanism): [RtCriticality.UNKNOWN] excludes UNCONDITIONALLY — an
     * exercise with no registered timing-sensitivity dossier entry is not merely "requires a good clock", its
     * measurement contract is itself unproven, so a good clock cannot rescue it. [RtCriticality.RT_CRITICAL]
     * excludes ONLY when the run's own clock is not frame-locked. [RtCriticality.RT_NON_CRITICAL] never excludes.
     */
    private fun timebaseExclusionFor(criticality: RtCriticality, run: CognitiveSessionRun): ExclusionReason? =
        when (criticality) {
            RtCriticality.UNKNOWN -> ExclusionReason.RT_CRITICALITY_UNKNOWN
            RtCriticality.RT_CRITICAL ->
                ExclusionReason.TIMEBASE_NON_AUTHORITATIVE.takeUnless {
                    run.eligibility.timingIntegrity.clockSource == ClockSource.FRAME_MONOTONIC
                }
            RtCriticality.RT_NON_CRITICAL -> null
        }

    /**
     * A run whose OWN timestamps are impossible — a completion later than its last write — is intrinsically
     * inconsistent and QUARANTINED permanently (never re-admitted as the clock advances, unlike a merely-future
     * stamp relative to "now").
     */
    fun isTimeInconsistent(run: CognitiveSessionRun): Boolean {
        val completed = run.completedAtEpochMs ?: return false
        return completed > run.updatedAtEpochMs
    }

    /** Real event time of the run (a COMPLETED run always has completedAt); falls back to start defensively. */
    fun orderKey(run: CognitiveSessionRun): Long = run.completedAtEpochMs ?: run.startedAtEpochMs

    /**
     * Coarse distinct-day key = UTC epoch-day. An APPROXIMATION (sessions astride UTC midnight may over/under-
     * count) used ONLY for the evidence-quantity fact ([ExerciseScaleEvidence.distinctDayCount]); this contract
     * ships no rank/ranking vocabulary at all today, so this key is never trusted for anything beyond that raw
     * fact until a run captures a real IANA timezone.
     */
    fun dayEpoch(run: CognitiveSessionRun): Long = orderKey(run) / MILLIS_PER_DAY

    /** The score comparability key (all fields present and non-blank on the run). */
    fun scaleKey(run: CognitiveSessionRun): ScoreScaleKey =
        ScoreScaleKey(run.blockId, run.exerciseId, run.exerciseVersion, run.scoringVersion)
}

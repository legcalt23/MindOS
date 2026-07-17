// EligibilityInputs bundles the boundary inputs of the pure eligibility policy; its many fields are cohesive
// (they are the exact signals the policy needs), so LongParameterList is suppressed for this file.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.core.session.cognitive

/** The device clock backing a session's timing. Coarse and PII-free. */
enum class ClockSource {
    FRAME_MONOTONIC,
    SYSTEM_MONOTONIC,
    WALL_CLOCK_DERIVED,
    UNKNOWN,
}

/** Overall integrity verdict of a session's timing, derived from the snapshot's signals. */
enum class TimingIntegrityStatus {
    CLEAN,
    MINOR_ANOMALIES,
    COMPROMISED,
    UNKNOWN,
}

/**
 * A snapshot of what a session's (pure) holder KNOWS about timing integrity. Real background/lifecycle
 * detection is device-QA (deferred); fields the holder cannot yet populate stay at honest defaults via
 * [UNKNOWN] — this NEVER fabricates a CLEAN verdict.
 */
data class TimingIntegritySnapshot(
    val clockSource: ClockSource,
    val monotonicAvailable: Boolean,
    val backgroundedDuringSession: Boolean,
    val pauseResumeCount: Int,
    val missedTickFlags: Int,
    val suspectedInputLatency: Boolean,
    val integrityStatus: TimingIntegrityStatus,
) {
    init {
        require(pauseResumeCount >= 0) { "pauseResumeCount must be >= 0 (was $pauseResumeCount)." }
        require(missedTickFlags >= 0) { "missedTickFlags must be >= 0 (was $missedTickFlags)." }
    }

    companion object {
        /** Honest "nothing measured yet" snapshot — never a fabricated CLEAN verdict. */
        val UNKNOWN: TimingIntegritySnapshot = TimingIntegritySnapshot(
            clockSource = ClockSource.UNKNOWN,
            monotonicAvailable = false,
            backgroundedDuringSession = false,
            pauseResumeCount = 0,
            missedTickFlags = 0,
            suspectedInputLatency = false,
            integrityStatus = TimingIntegrityStatus.UNKNOWN,
        )
    }
}

/** Whether a run may inform the profile, calibration, and ranking. Each flag is quality-AND-gated. */
data class SessionEligibility(
    val profileEligible: Boolean,
    val calibrationEligible: Boolean,
    val rankingEligible: Boolean,
    val timingIntegrity: TimingIntegritySnapshot,
    val dataQualityCompromised: Boolean,
)

/**
 * The per-gate diagnostic BEHIND a [SessionEligibility] decision. Its purpose is anti-masking: when the
 * measurement-quality kill-switch ([officialUseEligible]) denies the official lanes, the GENERIC lifecycle gates
 * ([writeAheadEligible] / [durationEligible] / [purposeEligible] / [integrityEligible] / [dataQualityEligible])
 * stay INDIVIDUALLY OBSERVABLE — each reflects its OWN result, never ANDed with the kill-switch — so a reader can
 * see, e.g., that a clean, on-time, valid-purpose run was denied ONLY because the score was preliminary.
 * [rejectionReasons] carries one stable machine string per failed gate (the measurement strings are canonical).
 */
data class EligibilityTrace(
    val integrityEligible: Boolean,
    val writeAheadEligible: Boolean,
    val durationEligible: Boolean,
    val purposeEligible: Boolean,
    val dataQualityEligible: Boolean,
    val officialUseEligible: Boolean,
    val rejectionReasons: List<String>,
)

/** The full result of [SessionEligibilityPolicy.evaluate]: the [eligibility] decision plus its diagnostic [trace]. */
data class EligibilityEvaluation(
    val eligibility: SessionEligibility,
    val trace: EligibilityTrace,
)

/**
 * An exercise-declared APPROVED equivalent to the 120s minimum (P0-2): a fixed-count / fixed-stimuli paradigm
 * may count toward calibration WITHOUT a 120s wall-clock IF it declares — and meets — a validated protocol.
 * Absence means only the wall-clock rule applies.
 */
data class ApprovedProtocol(
    val minTrials: Int,
    val minStimuli: Int,
    val minEffectiveDurationMs: Long,
    val scoringValidated: Boolean,
)

/**
 * The boundary inputs the pure [SessionEligibilityPolicy] needs to decide a run's eligibility. The last five
 * fields are the MEASUREMENT-QUALITY kill-switch inputs (all fail-closed by their honest defaults elsewhere): an
 * RT-critical run may touch the official lanes ONLY if the score is final, VALID, confident, on an authoritative
 * timebase, and anti-gaming-covered.
 */
data class EligibilityInputs(
    val purpose: SessionPurpose,
    val completionStatus: SessionCompletionStatus,
    val durationActualMs: Long,
    val approvedProtocol: ApprovedProtocol?,
    val completedTrials: Int,
    val stimuliShown: Int,
    val dataQualityCompromised: Boolean,
    val timing: TimingIntegritySnapshot,
    val writeAheadPresent: Boolean,
    val measurementPreliminary: Boolean,
    val scoreCategoryValid: Boolean,
    val lowConfidence: Boolean,
    val authoritativeTimebaseSatisfied: Boolean,
    val antiGamingCoverageSatisfied: Boolean,
)

/**
 * Pure eligibility authority (P0-1 purpose + P0-2 duration/protocol + the measurement-quality KILL-SWITCH).
 * Profile/calibration/ranking are each a necessary AND: the run must be CLEAN (completed, write-ahead present,
 * quality not compromised, timing not COMPROMISED) AND the purpose must allow that lane AND — for
 * calibration/ranking — the official duration/protocol threshold must be met AND the measurement kill-switch
 * ([officialMeasurementOk]) must pass. The kill-switch is a HARD, EXECUTABLE gate (not a KDoc): a preliminary,
 * non-VALID, low-confidence, non-authoritative-timebase, or anti-gaming-uncovered run is denied EVERY official
 * lane, so a downstream ingestor CANNOT consume it as official even if it forgets its own gate. A user-chosen
 * [SessionPurpose.TRAINING_FREE] that is valid is NOT blocked; only PRACTICE/DEBUG are structurally excluded, and
 * INITIAL_ASSESSMENT stays provisional (no ranking).
 */
object SessionEligibilityPolicy {
    /** PRD minimum valid-session wall-clock: 120 seconds. */
    const val MIN_CALIBRATION_DURATION_MS: Long = 120_000L

    // Stable machine reason strings. The five MEASUREMENT (kill-switch) strings are the canonical wire names and
    // must not change without a coordinated consumer update; the generic strings explain the lifecycle gates.
    const val REASON_NOT_COMPLETED: String = "session_not_completed"
    const val REASON_WRITE_AHEAD: String = "write_ahead_ineligible"
    const val REASON_DATA_QUALITY: String = "data_quality_compromised"
    const val REASON_TIMING_INTEGRITY: String = "timing_integrity_compromised"
    const val REASON_DURATION: String = "duration_below_official_threshold"
    const val REASON_PURPOSE: String = "purpose_not_eligible"
    const val REASON_PRELIMINARY: String = "score_preliminary"
    const val REASON_NOT_VALID: String = "score_not_valid"
    const val REASON_LOW_CONFIDENCE: String = "score_low_confidence"
    const val REASON_TIMEBASE: String = "timebase_not_authoritative"
    const val REASON_ANTI_GAMING: String = "anti_gaming_coverage_missing"

    fun evaluate(inputs: EligibilityInputs): EligibilityEvaluation {
        val integrityOk = inputs.timing.integrityStatus != TimingIntegrityStatus.COMPROMISED
        val writeAheadOk = inputs.writeAheadPresent
        val dataQualityOk = !inputs.dataQualityCompromised
        val completed = inputs.completionStatus == SessionCompletionStatus.COMPLETED
        val durationOk = meetsOfficialThreshold(inputs)
        val purposeOk = inputs.purpose.mayContributeToProfile()
        // The five MEASUREMENT-QUALITY conditions, derived ONCE: both officialUseOk (the kill-switch) and the
        // reason strings below read these same locals, so the executable gate and its explanation cannot desync.
        val notPreliminary = !inputs.measurementPreliminary
        val categoryValid = inputs.scoreCategoryValid
        val confident = !inputs.lowConfidence
        val timebaseOk = inputs.authoritativeTimebaseSatisfied
        val antiGamingOk = inputs.antiGamingCoverageSatisfied
        val officialUseOk = listOf(notPreliminary, categoryValid, confident, timebaseOk, antiGamingOk).all { it }
        val clean = listOf(completed, writeAheadOk, dataQualityOk, integrityOk).all { it }
        val baseline = clean && officialUseOk
        val eligibility = SessionEligibility(
            profileEligible = baseline && purposeOk,
            calibrationEligible = baseline && durationOk && inputs.purpose.mayContributeToCalibration(),
            rankingEligible = baseline && durationOk && inputs.purpose.mayContributeToRanking(),
            timingIntegrity = inputs.timing,
            dataQualityCompromised = inputs.dataQualityCompromised,
        )
        val trace = EligibilityTrace(
            integrityEligible = integrityOk,
            writeAheadEligible = writeAheadOk,
            durationEligible = durationOk,
            purposeEligible = purposeOk,
            dataQualityEligible = dataQualityOk,
            officialUseEligible = officialUseOk,
            rejectionReasons = rejectionReasons(
                completed, writeAheadOk, dataQualityOk, integrityOk, durationOk, purposeOk,
                notPreliminary, categoryValid, confident, timebaseOk, antiGamingOk,
            ),
        )
        return EligibilityEvaluation(eligibility, trace)
    }

    private fun meetsOfficialThreshold(inputs: EligibilityInputs): Boolean {
        val byDuration = inputs.durationActualMs >= MIN_CALIBRATION_DURATION_MS
        val byProtocol = inputs.approvedProtocol?.let { protocol ->
            protocol.scoringValidated &&
                inputs.completedTrials >= protocol.minTrials &&
                inputs.stimuliShown >= protocol.minStimuli &&
                inputs.durationActualMs >= protocol.minEffectiveDurationMs
        } ?: false
        return byDuration || byProtocol
    }

    private fun rejectionReasons(
        completed: Boolean,
        writeAheadOk: Boolean,
        dataQualityOk: Boolean,
        integrityOk: Boolean,
        durationOk: Boolean,
        purposeOk: Boolean,
        notPreliminary: Boolean,
        categoryValid: Boolean,
        confident: Boolean,
        timebaseOk: Boolean,
        antiGamingOk: Boolean,
    ): List<String> = buildList {
        if (!completed) add(REASON_NOT_COMPLETED)
        if (!writeAheadOk) add(REASON_WRITE_AHEAD)
        if (!dataQualityOk) add(REASON_DATA_QUALITY)
        if (!integrityOk) add(REASON_TIMING_INTEGRITY)
        if (!durationOk) add(REASON_DURATION)
        if (!purposeOk) add(REASON_PURPOSE)
        if (!notPreliminary) add(REASON_PRELIMINARY)
        if (!categoryValid) add(REASON_NOT_VALID)
        if (!confident) add(REASON_LOW_CONFIDENCE)
        if (!timebaseOk) add(REASON_TIMEBASE)
        if (!antiGamingOk) add(REASON_ANTI_GAMING)
    }
}

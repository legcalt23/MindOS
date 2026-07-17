// CognitiveSessionRun is a flat sealed record; its many cohesive fields ARE the model, not a code smell.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId

/** Difficulty of one exercise block within a session. start/end/maxReached are 0..1 normalized, or null. */
data class DifficultyBlock(
    val blockIndex: Int,
    val start: Double?,
    val end: Double?,
    val maxReached: Double?,
)

/** The difficulty trajectory of a session: overall start/end/max plus optional per-block detail. */
data class DifficultyTrace(
    val start: Double?,
    val end: Double?,
    val maxReached: Double?,
    val interleaved: Boolean,
    val perBlock: List<DifficultyBlock>,
)

/** A count of trials that resolved to one [TrialOutcomeKind]. A List (not a Map) for stable serialization. */
data class OutcomeCount(
    val kind: TrialOutcomeKind,
    val count: Int,
) {
    init {
        require(count >= 0) { "count must be >= 0 (was $count)." }
    }
}

/** Aggregate trial counts. [outcomes] partitions the [completed] trials by kind with no repeats. */
data class TrialAggregate(
    val planned: Int,
    val completed: Int,
    val outcomes: List<OutcomeCount>,
) {
    init {
        require(planned >= 0) { "planned must be >= 0 (was $planned)." }
        require(completed >= 0) { "completed must be >= 0 (was $completed)." }
        require(completed <= planned) { "completed ($completed) must be <= planned ($planned)." }
        val outcomeTotal = outcomes.sumOf { it.count }
        require(outcomeTotal == completed) {
            "outcome counts ($outcomeTotal) must sum to completed ($completed)."
        }
        require(outcomes.map { it.kind }.toSet().size == outcomes.size) {
            "outcomes must not repeat a TrialOutcomeKind."
        }
    }
}

/** Coarse answer tally. [total] accounts for [correct] + [incorrect] + [omissions] exactly. */
data class AnswerSummary(
    val total: Int,
    val correct: Int,
    val incorrect: Int,
    val omissions: Int,
) {
    init {
        require(total >= 0 && correct >= 0 && incorrect >= 0 && omissions >= 0) { "answer counts must be >= 0." }
        require(correct + incorrect + omissions == total) {
            "correct + incorrect + omissions (${correct + incorrect + omissions}) must equal total ($total)."
        }
    }
}

/**
 * A historical SNAPSHOT of a session's calibration effect at seal time — never the live truth (deletions,
 * anomalies, or a rule-version change can change the current state; the live valid-session count lives in the
 * ingestor/read-model). [calibrationRuleVersion] pins which rule produced this snapshot.
 */
data class CalibrationEffect(
    val countedTowardCalibration: Boolean,
    val validSessionIndexAtCompletion: Int?,
    val calibrationRuleVersion: String,
)

/** Coarse, PII-free device/app snapshot. Nullable overall: absent when not captured (never fabricated). */
data class PlatformSnapshot(
    val osName: String,
    val osVersion: String,
    val appVersion: String,
    val deviceTier: String?,
)

/**
 * SYSTEM-1 · The sealed, immutable, per-session COGNITIVE-OUTCOME record — the unified honest cognitive-history
 * authority. Sealed ONCE at finalization; references the lifecycle `SessionRecord` by [sessionId] (which
 * remains the write-ahead / recovery authority). It carries NO sync state (that is the `SyncQueue`'s job) and
 * NO userId (identity is implicit in the per-user, account-bound namespace, exactly like `SessionRecord`; the
 * userId is attached only when building the Firestore DTO). It NEVER carries raw trials or per-item content.
 *
 * [blockId] reuses [ScoreModuleId] (the ingestor maps it to the profile's dimension taxonomy). Timestamps are
 * epoch-millis; a terminal [completionStatus] (COMPLETED/ABANDONED/FINALIZATION_FAILED) requires
 * [completedAtEpochMs] (P1-9). [durationSelectedMs] is null for paradigms with no user-selected duration.
 */
data class CognitiveSessionRun(
    val sessionId: String,
    val blockId: ScoreModuleId,
    val exerciseId: String,
    val exerciseVersion: String,
    val scoringVersion: String,
    val sessionPurpose: SessionPurpose,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val durationSelectedMs: Long?,
    val durationActualMs: Long,
    val completionStatus: SessionCompletionStatus,
    val finalizationFailure: FinalizationFailureReason?,
    val difficulty: DifficultyTrace,
    val trials: TrialAggregate,
    val answerSummary: AnswerSummary,
    val metrics: CognitiveSessionMetrics,
    val exerciseMetrics: ExerciseSpecificMetricPayload?,
    val score: CognitiveSessionScore,
    val eligibility: SessionEligibility,
    val eligibilityTrace: EligibilityTrace,
    val calibrationEffect: CalibrationEffect?,
    val anomalyFlags: List<String>,
    val platform: PlatformSnapshot?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    init {
        require(sessionId.isNotBlank()) { "sessionId must not be blank." }
        require(exerciseId.isNotBlank()) { "exerciseId must not be blank." }
        require(exerciseVersion.isNotBlank()) { "exerciseVersion must not be blank." }
        require(startedAtEpochMs > 0L) { "startedAtEpochMs must be a real epoch (> 0)." }
        require(durationActualMs >= 0L) { "durationActualMs must be >= 0 (was $durationActualMs)." }
        require(durationSelectedMs == null || durationSelectedMs >= 0L) {
            "durationSelectedMs must be null or >= 0 (was $durationSelectedMs)."
        }
        requireTerminalTimestamp()
        requireFinalizationReasonConsistency()
    }

    private fun requireTerminalTimestamp() {
        val terminal = completionStatus == SessionCompletionStatus.COMPLETED ||
            completionStatus == SessionCompletionStatus.ABANDONED ||
            completionStatus == SessionCompletionStatus.FINALIZATION_FAILED
        require(!terminal || completedAtEpochMs != null) {
            "A terminal completionStatus ($completionStatus) requires completedAtEpochMs (P1-9)."
        }
        require(completedAtEpochMs == null || completedAtEpochMs >= startedAtEpochMs) {
            "completedAtEpochMs ($completedAtEpochMs) must be >= startedAtEpochMs ($startedAtEpochMs)."
        }
    }

    private fun requireFinalizationReasonConsistency() {
        val failed = completionStatus == SessionCompletionStatus.FINALIZATION_FAILED
        require(failed == (finalizationFailure != null)) {
            "finalizationFailure must be set iff completionStatus is FINALIZATION_FAILED."
        }
    }
}

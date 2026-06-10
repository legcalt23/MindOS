package com.mindos.app.features.mindboost.memory.domain.model

/**
 * A persistable SUMMARY of one finished session — never the raw trials (project
 * learning 2026-06-05). Carries only the headline metrics needed for history,
 * trends and the recovery/anti-plateau signals.
 *
 * [sessionId] is "<userId>:<endedAtEpoch>" so history keys are naturally user-scoped.
 */
data class MemorySessionSummary(
    val sessionId: String,
    val userId: String,
    val focusExerciseId: MemoryExerciseId,
    val endedAtEpoch: Long,
    val immediateAccuracy: Double?,
    val delayedAccuracy: Double?,
    val performance: Double,
) {
    init {
        require(sessionId.isNotBlank()) { "sessionId cannot be blank." }
        require(userId.isNotBlank()) { "userId cannot be blank." }
        require(endedAtEpoch >= 0) { "endedAtEpoch cannot be negative." }
        require(performance in 0.0..1.0) { "performance must be in 0.0..1.0 (was $performance)." }
        immediateAccuracy?.let { require(it in 0.0..1.0) { "immediateAccuracy must be in 0.0..1.0." } }
        delayedAccuracy?.let { require(it in 0.0..1.0) { "delayedAccuracy must be in 0.0..1.0." } }
    }
}

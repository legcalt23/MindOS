package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/**
 * EX3 deferred recall (P2) · One completed 24h delayed-recall test: which exercise's studied material it
 * tested, and the scored [DeferredRecallOutcome]. A later phase accumulates these toward the episodic
 * (DELAYED_RECALL_ACCURACY) profile signal.
 */
data class DeferredRecallResult(
    val exerciseId: MemoryExerciseId,
    val outcome: DeferredRecallOutcome,
)

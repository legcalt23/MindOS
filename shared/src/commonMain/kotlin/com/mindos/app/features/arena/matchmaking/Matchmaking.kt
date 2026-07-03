package com.mindos.app.features.arena.matchmaking

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.hardware.IllegalMatchException

/**
 * Phase 05 / Task 5: Offline session initialization. A [HardwareClass] is REQUIRED (non-null) so
 * Mental Speed measurement can be segmented correctly (15_jitter_device_bias_plan.md AC #1).
 * Identifiers are synthetic only.
 */
data class SessionInit(
    val syntheticSessionId: String,
    val syntheticPlayerId: String,
    val taskType: TaskMotorType,
    val hardwareClass: HardwareClass,
) {
    init {
        require(syntheticSessionId.isNotBlank()) { "syntheticSessionId must not be blank" }
        require(syntheticPlayerId.isNotBlank()) { "syntheticPlayerId must not be blank" }
    }
}

/**
 * Phase 05 / Task 5: Offline matchmaking pool invariant (NOT live matchmaking).
 *
 * For Mental Speed (Choice Reaction), two sessions may only be matched within the SAME hardware
 * class. Focus and Working Memory carry no such restriction. [requireMatch] enforces the invariant
 * by throwing [IllegalMatchException]; [canMatch] is the non-throwing predicate.
 */
data class MatchmakingPoolState(
    val taskType: TaskMotorType,
    val members: List<SessionInit> = emptyList(),
) {
    fun canMatch(requester: SessionInit, candidate: SessionInit): Boolean = when {
        requester.taskType != taskType || candidate.taskType != taskType -> false
        taskType == TaskMotorType.MENTAL_SPEED_CHOICE_REACTION ->
            requester.hardwareClass == candidate.hardwareClass
        else -> true
    }

    fun requireMatch(requester: SessionInit, candidate: SessionInit) {
        if (!canMatch(requester, candidate)) {
            throw IllegalMatchException(
                "Illegal ${taskType.name} match: " +
                    "${requester.hardwareClass.id} vs ${candidate.hardwareClass.id}",
            )
        }
    }
}

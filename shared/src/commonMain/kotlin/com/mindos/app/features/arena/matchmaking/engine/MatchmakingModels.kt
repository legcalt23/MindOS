@file:Suppress("MagicNumber") // Queue timing/window defaults are documented simulation constants.

package com.mindos.app.features.arena.matchmaking.engine

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.HardwareClass

/**
 * Phase 10 (SIMULATION ONLY): A synthetic match candidate. This is offline matchmaking domain data —
 * there is no live queue, networking, public ranked, or production surface anywhere in this package.
 */
@Suppress("LongParameterList") // Irreducible matchmaking feature record.
data class MatchCandidate(
    val syntheticPlayerId: String,
    val ratingMu: Double,
    val ratingSigma: Double,
    val motorType: TaskMotorType,
    val hardwareClass: HardwareClass,
    val inputClass: String,
    val integrityState: IntegrityStateEnum,
    val smurfSuspicion: SmurfSuspicionLevel,
    val sessionsPlayed: Int,
    val isMinor: Boolean,
    val commsActive: Boolean,
    val comparability: ComparabilityStateEnum,
)

/** Phase 10: Queue expansion phase by wait time (queue_expansion.md). */
enum class QueuePhase {
    NARROW,
    GRADUAL,
    WIDE,
    TIMEOUT,
}

/**
 * Phase 10: Queue expansion config: time bands (s) and rating windows. Only the rating window widens
 * over time; hard boundaries are never crossed.
 */
data class QueueExpansionConfig(
    val configVersion: String = "QUEUE_CONFIG_V1.0",
    val narrowMaxSeconds: Int = 15,
    val gradualMaxSeconds: Int = 45,
    val wideMaxSeconds: Int = 120,
    val narrowWindow: Double = 100.0,
    val gradualWindow: Double = 250.0,
    val wideWindow: Double = 500.0,
) {
    init {
        require(narrowMaxSeconds <= gradualMaxSeconds) { "narrow <= gradual seconds" }
        require(gradualMaxSeconds <= wideMaxSeconds) { "gradual <= wide seconds" }
        require(narrowWindow <= gradualWindow && gradualWindow <= wideWindow) { "windows must widen" }
    }

    companion object {
        val DEFAULT: QueueExpansionConfig = QueueExpansionConfig()
    }
}

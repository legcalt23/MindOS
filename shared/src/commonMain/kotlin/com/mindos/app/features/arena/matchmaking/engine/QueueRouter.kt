@file:Suppress("MagicNumber") // Provisional-session threshold default is a documented constant.

package com.mindos.app.features.arena.matchmaking.engine

import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel

/** Phase 10 (SIMULATION ONLY): The queue a candidate is routed to. */
enum class QueueType {
    STANDARD,
    PROVISIONAL,
    SMURF_CALIBRATION,
    INTEGRITY_ISOLATION,
}

/** Phase 10: Routing config. */
data class QueueRoutingConfig(
    val provisionalMaxSessions: Int = 10,
) {
    init {
        require(provisionalMaxSessions >= 0) { "provisionalMaxSessions must be >= 0" }
    }

    companion object {
        val DEFAULT: QueueRoutingConfig = QueueRoutingConfig()
    }
}

/**
 * Phase 10 (SIMULATION ONLY): Routes candidates to specialized queues:
 * - INTEGRITY_ISOLATION: REVIEW_REQUIRED / HOLD players are excluded from Ranked until resolved.
 * - SMURF_CALIBRATION: smurf-suspected players calibrate (sigma kept high by the rating engine).
 * - PROVISIONAL: new players (< threshold sessions) only meet other provisionals (no skill-mismatch trauma).
 * - STANDARD: everyone else. Candidates may only share a queue if routed to the same type.
 */
class QueueRouter(
    private val config: QueueRoutingConfig = QueueRoutingConfig.DEFAULT,
) {
    fun routeFor(candidate: MatchCandidate): QueueType = when {
        isIsolated(candidate) -> QueueType.INTEGRITY_ISOLATION
        candidate.smurfSuspicion == SmurfSuspicionLevel.HIGH -> QueueType.SMURF_CALIBRATION
        candidate.sessionsPlayed < config.provisionalMaxSessions -> QueueType.PROVISIONAL
        else -> QueueType.STANDARD
    }

    fun canShareQueue(a: MatchCandidate, b: MatchCandidate): Boolean = routeFor(a) == routeFor(b)

    private fun isIsolated(candidate: MatchCandidate): Boolean =
        candidate.integrityState == IntegrityStateEnum.REVIEW_REQUIRED ||
            candidate.integrityState == IntegrityStateEnum.HOLD
}

/** Phase 10 (SIMULATION ONLY): What to do when the queue cannot fill in time. */
enum class FallbackDecision {
    CONTINUE,
    CANCEL_FAIRNESS_PRESERVED,
}

/**
 * Phase 10 (SIMULATION ONLY): Low-population fallback (low_population_fallback.md). After the wide-band
 * timeout it is better to cancel (preserving fairness) than to force an asymmetric match.
 */
object LowPopulationFallback {
    fun decide(waitSeconds: Int, config: QueueExpansionConfig): FallbackDecision =
        if (waitSeconds > config.wideMaxSeconds) {
            FallbackDecision.CANCEL_FAIRNESS_PRESERVED
        } else {
            FallbackDecision.CONTINUE
        }
}

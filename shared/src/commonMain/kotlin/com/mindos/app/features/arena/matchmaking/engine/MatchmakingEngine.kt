package com.mindos.app.features.arena.matchmaking.engine

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.evidence.motor.CompetitiveContext
import kotlin.math.abs

/** Phase 10 (SIMULATION ONLY): Queue expansion — widen the rating window over wait time. */
object QueueExpansion {

    fun phaseFor(waitSeconds: Int, config: QueueExpansionConfig): QueuePhase = when {
        waitSeconds <= config.narrowMaxSeconds -> QueuePhase.NARROW
        waitSeconds <= config.gradualMaxSeconds -> QueuePhase.GRADUAL
        waitSeconds <= config.wideMaxSeconds -> QueuePhase.WIDE
        else -> QueuePhase.TIMEOUT
    }

    fun ratingWindowFor(phase: QueuePhase, config: QueueExpansionConfig): Double = when (phase) {
        QueuePhase.NARROW -> config.narrowWindow
        QueuePhase.GRADUAL -> config.gradualWindow
        QueuePhase.WIDE, QueuePhase.TIMEOUT -> config.wideWindow
    }
}

/**
 * Phase 10 (SIMULATION ONLY): Hard boundaries that matchmaking must NEVER cross (hard_boundaries.md):
 * device/input class in speed motors, NOT_COMPARABLE in Ranked, minors+adults when comms are active,
 * and integrity-isolated players in Ranked.
 */
object HardBoundaries {

    fun crosses(a: MatchCandidate, b: MatchCandidate, context: CompetitiveContext): Boolean = when {
        isSpeedMotor(a) && a.hardwareClass != b.hardwareClass -> true
        isSpeedMotor(a) && a.inputClass != b.inputClass -> true
        context == CompetitiveContext.RANKED && notComparable(a, b) -> true
        a.isMinor != b.isMinor && (a.commsActive || b.commsActive) -> true
        context == CompetitiveContext.RANKED && (isIsolated(a) || isIsolated(b)) -> true
        else -> false
    }

    private fun isSpeedMotor(c: MatchCandidate): Boolean =
        c.motorType == TaskMotorType.MENTAL_SPEED_CHOICE_REACTION

    private fun notComparable(a: MatchCandidate, b: MatchCandidate): Boolean =
        a.comparability == ComparabilityStateEnum.NOT_COMPARABLE ||
            b.comparability == ComparabilityStateEnum.NOT_COMPARABLE

    private fun isIsolated(c: MatchCandidate): Boolean =
        c.integrityState == IntegrityStateEnum.REVIEW_REQUIRED || c.integrityState == IntegrityStateEnum.HOLD
}

/**
 * Phase 10 (SIMULATION ONLY): Offline matchmaking core rules (matchmaking_core_rules.md). Decides
 * whether two synthetic candidates may be matched given the competitive context and current wait
 * time. Pure — no live queue, no networking, no public ranked, no production.
 */
class MatchmakingEngine(
    private val config: QueueExpansionConfig = QueueExpansionConfig.DEFAULT,
) {
    fun canMatch(
        requester: MatchCandidate,
        candidate: MatchCandidate,
        context: CompetitiveContext,
        waitSeconds: Int,
    ): Boolean {
        val window = QueueExpansion.ratingWindowFor(QueueExpansion.phaseFor(waitSeconds, config), config)
        return requester.motorType == candidate.motorType &&
            !HardBoundaries.crosses(requester, candidate, context) &&
            abs(requester.ratingMu - candidate.ratingMu) <= window
    }
}

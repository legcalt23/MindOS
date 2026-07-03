package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.evidence.motor.CompetitiveContext
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.matchmaking.engine.MatchCandidate
import com.mindos.app.features.arena.matchmaking.engine.MatchmakingEngine
import com.mindos.app.features.arena.matchmaking.engine.QueueExpansion
import com.mindos.app.features.arena.matchmaking.engine.QueueExpansionConfig
import com.mindos.app.features.arena.matchmaking.engine.QueuePhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 10 / P10.1 (SIMULATION ONLY): Matchmaking core rules, queue expansion, hard boundaries. */
class MatchmakingEngineTest {

    private val engine = MatchmakingEngine()

    @Suppress("LongParameterList")
    private fun candidate(
        mu: Double,
        motor: TaskMotorType = TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
        hw: HardwareClass = ArenaTestFixtures.ANDROID_120,
        input: String = "TOUCH",
        integrity: IntegrityStateEnum = IntegrityStateEnum.VERIFIED,
        sessions: Int = 50,
        minor: Boolean = false,
        comms: Boolean = false,
        comparability: ComparabilityStateEnum = ComparabilityStateEnum.COMPARABLE,
    ) = MatchCandidate("P", mu, 80.0, motor, hw, input, integrity, SmurfSuspicionLevel.NONE, sessions, minor, comms, comparability)

    @Test
    fun closeRatingsMatchEarlyAndFarRatingsNeedExpansion() {
        assertTrue(engine.canMatch(candidate(1500.0), candidate(1550.0), CompetitiveContext.SHADOW, waitSeconds = 5))
        assertTrue(!engine.canMatch(candidate(1500.0), candidate(1800.0), CompetitiveContext.SHADOW, waitSeconds = 5))
        assertTrue(engine.canMatch(candidate(1500.0), candidate(1800.0), CompetitiveContext.SHADOW, waitSeconds = 60))
    }

    @Test
    fun speedMotorNeverCrossesHardwareOrInputClass() {
        val a = candidate(1500.0, TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        val crossHw = candidate(1500.0, TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.IOS_120)
        val crossInput = candidate(1500.0, TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120, input = "GAMEPAD")
        assertTrue(!engine.canMatch(a, crossHw, CompetitiveContext.SHADOW, waitSeconds = 5))
        assertTrue(!engine.canMatch(a, crossInput, CompetitiveContext.SHADOW, waitSeconds = 5))
    }

    @Test
    fun rankedRejectsNotComparableAndIntegrityIsolated() {
        val a = candidate(1500.0)
        assertTrue(
            !engine.canMatch(a, candidate(1520.0, comparability = ComparabilityStateEnum.NOT_COMPARABLE), CompetitiveContext.RANKED, 5),
        )
        assertTrue(
            !engine.canMatch(a, candidate(1520.0, integrity = IntegrityStateEnum.HOLD), CompetitiveContext.RANKED, 5),
        )
    }

    @Test
    fun minorsAndAdultsAreNotMatchedWhenCommsActive() {
        val minor = candidate(1500.0, minor = true)
        val adultWithComms = candidate(1520.0, minor = false, comms = true)
        assertTrue(!engine.canMatch(minor, adultWithComms, CompetitiveContext.SHADOW, 5))
    }

    @Test
    fun differentMotorsNeverMatch() {
        assertTrue(
            !engine.canMatch(
                candidate(1500.0, TaskMotorType.FOCUS_INTERFERENCE_CONTROL),
                candidate(1500.0, TaskMotorType.WORKING_MEMORY_SEQUENCE_UPDATING),
                CompetitiveContext.SHADOW, 5,
            ),
        )
    }

    @Test
    fun queuePhasesTransitionByWaitTime() {
        val c = QueueExpansionConfig.DEFAULT
        assertEquals(QueuePhase.NARROW, QueueExpansion.phaseFor(5, c))
        assertEquals(QueuePhase.GRADUAL, QueueExpansion.phaseFor(30, c))
        assertEquals(QueuePhase.WIDE, QueueExpansion.phaseFor(100, c))
        assertEquals(QueuePhase.TIMEOUT, QueueExpansion.phaseFor(200, c))
    }
}

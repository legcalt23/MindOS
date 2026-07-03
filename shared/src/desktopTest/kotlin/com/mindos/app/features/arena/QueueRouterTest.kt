package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.matchmaking.engine.FallbackDecision
import com.mindos.app.features.arena.matchmaking.engine.LowPopulationFallback
import com.mindos.app.features.arena.matchmaking.engine.MatchCandidate
import com.mindos.app.features.arena.matchmaking.engine.QueueExpansionConfig
import com.mindos.app.features.arena.matchmaking.engine.QueueRouter
import com.mindos.app.features.arena.matchmaking.engine.QueueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 10 / P10.2 (SIMULATION ONLY): Specialized queue routing + low-population fallback. */
class QueueRouterTest {

    private val router = QueueRouter()

    private fun candidate(
        integrity: IntegrityStateEnum = IntegrityStateEnum.VERIFIED,
        smurf: SmurfSuspicionLevel = SmurfSuspicionLevel.NONE,
        sessions: Int = 50,
    ) = MatchCandidate(
        "P", 1500.0, 80.0, TaskMotorType.FOCUS_INTERFERENCE_CONTROL, ArenaTestFixtures.ANDROID_120,
        "TOUCH", integrity, smurf, sessions, isMinor = false, commsActive = false, ComparabilityStateEnum.COMPARABLE,
    )

    @Test
    fun routesByPriorityIsolationThenSmurfThenProvisionalThenStandard() {
        assertEquals(QueueType.INTEGRITY_ISOLATION, router.routeFor(candidate(integrity = IntegrityStateEnum.HOLD)))
        assertEquals(QueueType.SMURF_CALIBRATION, router.routeFor(candidate(smurf = SmurfSuspicionLevel.HIGH)))
        assertEquals(QueueType.PROVISIONAL, router.routeFor(candidate(sessions = 3)))
        assertEquals(QueueType.STANDARD, router.routeFor(candidate()))
    }

    @Test
    fun isolationOutranksSmurfSuspicion() {
        assertEquals(
            QueueType.INTEGRITY_ISOLATION,
            router.routeFor(candidate(integrity = IntegrityStateEnum.REVIEW_REQUIRED, smurf = SmurfSuspicionLevel.HIGH)),
        )
    }

    @Test
    fun onlySameQueueTypesCanShareAQueue() {
        assertTrue(router.canShareQueue(candidate(sessions = 3), candidate(sessions = 5)))
        assertTrue(!router.canShareQueue(candidate(sessions = 3), candidate(sessions = 50)))
    }

    @Test
    fun lowPopulationCancelsAfterTimeoutInsteadOfAsymmetricMatch() {
        val config = QueueExpansionConfig.DEFAULT
        assertEquals(FallbackDecision.CONTINUE, LowPopulationFallback.decide(60, config))
        assertEquals(FallbackDecision.CANCEL_FAIRNESS_PRESERVED, LowPopulationFallback.decide(200, config))
    }
}

package com.mindverse.mindboost.core.tests

import com.mindverse.mindboost.core.coach.CoachEngine
import com.mindverse.mindboost.core.domain.BayesScenario
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionSessionOrchestrator
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecisionSessionOrchestratorTest {
    private val orchestrator = DecisionSessionOrchestrator()

    @Test
    fun checkInComputesCriFromGoNoGoSignals() {
        val readiness = orchestrator.checkIn(
            CheckInInput(
                goNoGoAccuracy = 0.80,
                reactionTimeCV = 0.20,
                subjectiveFatigue = 0.50,
                reactionTimeBaselineMs = 1_000L,
            ),
        )

        assertEquals(74.0, readiness.criValue, 0.0001)
        assertEquals(0.80, readiness.goNoGoAccuracy, 0.0001)
        assertEquals(0.20, readiness.reactionTimeCV, 0.0001)
        assertEquals(0.50, readiness.subjectiveFatigue, 0.0001)
    }

    @Test
    fun checkInClampsOutOfRangeSignals() {
        val readiness = orchestrator.checkIn(
            CheckInInput(
                goNoGoAccuracy = 1.50,
                reactionTimeCV = -0.30,
                subjectiveFatigue = 2.00,
                reactionTimeBaselineMs = 1_000L,
            ),
        )

        assertEquals(1.0, readiness.goNoGoAccuracy, 0.0001)
        assertEquals(0.0, readiness.reactionTimeCV, 0.0001)
        assertEquals(1.0, readiness.subjectiveFatigue, 0.0001)
        assertEquals(100.0, readiness.criValue, 0.0001)
    }

    @Test
    fun bayesRoundDerivesNeutralUpdateWhenUserMatchesMathematicalPosterior() {
        val round = orchestrator.bayesRound(
            difficulty = baseDifficulty(),
            scenario = bayesScenario(expectedPosterior = 0.5),
            userPosterior = 0.5,
            objectivesAlignment = 1.0,
            biasDetected = false,
            alternativesScannedRatio = 1.0,
            reactionTimeMs = 4_000L,
            timeLimitMs = 10_000L,
            spatialEntropy = 0.9,
            isDialDefault = false,
        )

        assertEquals(1.0, round.bayesianUpdateRatio, 0.0001)
        assertEquals(false, round.isReversible)
    }

    @Test
    fun assembleSessionScoresAPerfectBayesRound() {
        val round = orchestrator.bayesRound(
            difficulty = baseDifficulty(),
            scenario = bayesScenario(expectedPosterior = 0.5),
            userPosterior = 0.5,
            objectivesAlignment = 1.0,
            biasDetected = false,
            alternativesScannedRatio = 1.0,
            reactionTimeMs = 4_000L,
            timeLimitMs = 10_000L,
            spatialEntropy = 0.9,
            isDialDefault = false,
        )

        val result = orchestrator.assembleSession(
            sessionId = "session-1",
            timestampMs = 42L,
            checkIn = CheckInInput(
                goNoGoAccuracy = 0.80,
                reactionTimeCV = 0.20,
                subjectiveFatigue = 0.50,
                reactionTimeBaselineMs = 4_000L,
            ),
            rounds = listOf(round),
        )

        // delayRatio = 0.4, irreversible lambda = 0.15 -> IED = 100 * e^(-0.15 * 0.4^2)
        val expectedIed = 100.0 * exp(-0.15 * 0.4 * 0.4)
        assertEquals("session-1", result.session.sessionId)
        assertEquals(42L, result.session.timestampMs)
        assertEquals(1, result.rounds.size)
        assertEquals(100.0, result.session.icd, 0.0001)
        assertEquals(expectedIed, result.session.ied, 0.001)
        assertEquals(42.5, result.session.cde, 0.0001)
        assertEquals(0.03, result.session.gamingScore, 0.0001)
        assertEquals(0.0, result.session.fatigueScore, 0.0001)
        assertNull(result.coachMessage)
    }

    @Test
    fun reversibilityRoundRewardsCorrectTriageAndPenalizesWrongTriage() {
        val scenario = reversibilityScenario(isReversible = true)
        val checkIn = CheckInInput(
            goNoGoAccuracy = 0.80,
            reactionTimeCV = 0.20,
            subjectiveFatigue = 0.50,
            reactionTimeBaselineMs = 3_000L,
        )

        val correct = orchestrator.assembleSession(
            sessionId = "correct",
            timestampMs = 1L,
            checkIn = checkIn,
            rounds = listOf(
                orchestrator.reversibilityRound(
                    difficulty = baseDifficulty(),
                    scenario = scenario,
                    selectedReversible = true,
                    alternativesScannedRatio = 1.0,
                    reactionTimeMs = 3_000L,
                    timeLimitMs = 10_000L,
                    spatialEntropy = 0.9,
                    isDialDefault = false,
                ),
            ),
        )

        val wrong = orchestrator.assembleSession(
            sessionId = "wrong",
            timestampMs = 1L,
            checkIn = checkIn,
            rounds = listOf(
                orchestrator.reversibilityRound(
                    difficulty = baseDifficulty(),
                    scenario = scenario,
                    selectedReversible = false,
                    alternativesScannedRatio = 1.0,
                    reactionTimeMs = 3_000L,
                    timeLimitMs = 10_000L,
                    spatialEntropy = 0.9,
                    isDialDefault = false,
                ),
            ),
        )

        assertEquals(100.0, correct.session.icd, 0.0001)
        assertEquals(70.0, wrong.session.icd, 0.0001)
    }

    @Test
    fun assembleSessionWithNoRoundsYieldsZeroedSessionAndNoCoachMessage() {
        val result = orchestrator.assembleSession(
            sessionId = "empty",
            timestampMs = 7L,
            checkIn = CheckInInput(
                goNoGoAccuracy = 0.80,
                reactionTimeCV = 0.20,
                subjectiveFatigue = 0.50,
                reactionTimeBaselineMs = 1_000L,
            ),
            rounds = emptyList(),
        )

        assertEquals(0.0, result.session.icd, 0.0001)
        assertEquals(0.0, result.session.ied, 0.0001)
        assertEquals(0.0, result.session.cde, 0.0001)
        assertEquals(0.0, result.session.gamingScore, 0.0001)
        assertEquals(0.0, result.session.fatigueScore, 0.0001)
        assertTrue(result.rounds.isEmpty())
        assertNull(result.coachMessage)
    }

    @Test
    fun assembleSessionSurfacesGamingCoachMessageWhenAgrsIsHigh() {
        val gamingRound = orchestrator.bayesRound(
            difficulty = baseDifficulty(),
            scenario = bayesScenario(expectedPosterior = 0.5),
            userPosterior = 0.5,
            objectivesAlignment = 1.0,
            biasDetected = false,
            alternativesScannedRatio = 1.0,
            reactionTimeMs = 250L,
            timeLimitMs = 10_000L,
            spatialEntropy = 0.0,
            isDialDefault = true,
        )

        val result = orchestrator.assembleSession(
            sessionId = "gaming",
            timestampMs = 1L,
            checkIn = CheckInInput(
                goNoGoAccuracy = 0.80,
                reactionTimeCV = 0.20,
                subjectiveFatigue = 0.50,
                reactionTimeBaselineMs = 250L,
            ),
            rounds = listOf(gamingRound),
        )

        val t02 = CoachEngine.templates.first { it.id == "T02" }.messageText
        assertEquals(1.0, result.session.gamingScore, 0.0001)
        assertEquals(t02, result.coachMessage)
    }

    @Test
    fun assembleSessionRejectsBlankSessionId() {
        assertFailsWith<IllegalArgumentException> {
            orchestrator.assembleSession(
                sessionId = "   ",
                timestampMs = 1L,
                checkIn = CheckInInput(
                    goNoGoAccuracy = 0.80,
                    reactionTimeCV = 0.20,
                    subjectiveFatigue = 0.50,
                    reactionTimeBaselineMs = 1_000L,
                ),
                rounds = emptyList(),
            )
        }
    }

    private fun baseDifficulty(): DifficultyVector = DifficultyVector(
        numberOfVariables = 4,
        numberOfAlternatives = 3,
        timePressure = 0.5,
        noiseLevel = 0.5,
        ambiguityLevel = 0.5,
        irreversibility = 0.5,
        informationCost = 0.5,
        evidenceContradiction = 0.5,
        memoryLoad = 3,
        justificationDemand = 0.5,
    )

    private fun bayesScenario(expectedPosterior: Double): BayesScenario = BayesScenario(
        baseRate = 0.20,
        sensitivity = 0.90,
        falsePositiveRate = 0.05,
        expectedPosterior = expectedPosterior,
    )

    private fun reversibilityScenario(isReversible: Boolean): ReversibilityScenario = ReversibilityScenario(
        id = "rev",
        description = "triage",
        isReversible = isReversible,
        delayCostRate = if (isReversible) 5.0 else 0.0,
        lockedDurationMs = if (isReversible) 0L else 3_000L,
    )
}

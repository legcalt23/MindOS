package com.mindverse.mindboost.core.tests

import com.mindverse.mindboost.core.coach.CoachEngine
import com.mindverse.mindboost.core.domain.BrierPrediction
import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.domain.PredictionEvent
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import com.mindverse.mindboost.core.mdp.bayes.BayesEngine
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityDecisionType
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityEngine
import com.mindverse.mindboost.core.scoring.ScoringEquations
import com.mindverse.mindboost.core.storage.DebugEphemeralKeyProvider
import com.mindverse.mindboost.core.storage.InMemoryDiaryRepository
import com.mindverse.mindboost.core.storage.InMemorySessionRepository
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class AlphaCoreTest {
    @Test
    fun bayesPosteriorMatchesKnownEx07Value() {
        val posterior = BayesEngine.calculatePosterior(
            baseRate = 0.01,
            sensitivity = 0.90,
            falsePositiveRate = 0.05,
        )

        assertEquals(0.1538, posterior, 0.0001)
    }

    @Test
    fun brierMixedCertainAndChanceForecastsScoreOneEighth() {
        val predictions = listOf(1.0 to 1.0, 0.5 to 0.0)

        assertEquals(0.125, ScoringEquations.calculateBrierScore(predictions), 0.0001)
    }

    @Test
    fun brierPerfectForecastsScoreZero() {
        val predictions = listOf(1.0 to 1.0, 1.0 to 1.0)

        assertEquals(0.0, ScoringEquations.calculateBrierScore(predictions), 0.0001)
    }

    @Test
    fun brierChanceForecastsScoreQuarterAcrossOutcomes() {
        val predictions = listOf(0.5 to 1.0, 0.5 to 0.0)

        assertEquals(0.25, ScoringEquations.calculateBrierScore(predictions), 0.0001)
    }

    @Test
    fun brierPredictionDataClassUsesSameScoringPath() {
        val predictions = listOf(BrierPrediction(forecast = 1.0, outcome = 1.0))

        assertEquals(0.0, ScoringEquations.calculateBrierScore(predictions), 0.0001)
    }

    @Test
    fun burIsExactWhenUserMatchesMathematicalUpdate() {
        val bur = ScoringEquations.calculateBUR(
            pPriori = 0.20,
            pPostUser = 0.50,
            pPostMath = 0.50,
        )

        assertEquals(1.0, bur, 0.0001)
    }

    @Test
    fun cdeNormalizesToZeroAtMinimumLoad() {
        val cde = ScoringEquations.calculateCDE(
            nVar = 1,
            hProb = 0.0,
            hMax = 1.0,
            timePressure = 0.0,
            memoryLoad = 1,
            reversibility = 1.0,
        )

        assertEquals(0.0, cde, 0.0001)
    }

    @Test
    fun cdeNormalizesToOneHundredAtMaxLoad() {
        val cde = ScoringEquations.calculateCDE(
            nVar = 10,
            hProb = 1.0,
            hMax = 1.0,
            timePressure = 1.0,
            memoryLoad = 7,
            reversibility = 0.0,
        )

        assertEquals(100.0, cde, 0.0001)
    }

    @Test
    fun phdInvalidatesWhenGamingRiskIsHigh() {
        val phd = ScoringEquations.calculatePHD(
            icdRolling = 95.0,
            ceRolling = 0.0,
            iedRolling = 95.0,
            agrsRolling = 0.751,
            frsRolling = 0.0,
        )

        assertEquals(0.0, phd, 0.0001)
    }

    @Test
    fun phdAppliesCompensatoryLimits() {
        val phd = ScoringEquations.calculatePHD(
            icdRolling = 50.0,
            ceRolling = 0.05,
            iedRolling = 90.0,
            agrsRolling = 0.0,
            frsRolling = 0.0,
        )

        assertTrue(phd <= 70.0)
    }

    @Test
    fun iedReturnsZeroForImpulsiveReactionTime() {
        val ied = ScoringEquations.calculateIED(
            icd = 80.0,
            reactionTimeMs = 499L,
            timeLimitMs = 10_000L,
            isReversible = false,
        )

        assertEquals(0.0, ied, 0.0001)
    }

    @Test
    fun iedPenalizesTypeTwoDelayMoreThanTypeOneDelay() {
        val typeOneIrreversible = ScoringEquations.calculateIED(
            icd = 80.0,
            reactionTimeMs = 5_000L,
            timeLimitMs = 10_000L,
            isReversible = false,
        )
        val typeTwoReversible = ScoringEquations.calculateIED(
            icd = 80.0,
            reactionTimeMs = 5_000L,
            timeLimitMs = 10_000L,
            isReversible = true,
        )

        assertTrue(typeOneIrreversible > typeTwoReversible)
    }

    @Test
    fun burHandlesNearZeroMathematicalUpdateWithoutNonFiniteResult() {
        val bur = ScoringEquations.calculateBUR(
            pPriori = 0.50,
            pPostUser = 0.90,
            pPostMath = 0.500000001,
        )

        assertFalse(bur.isNaN())
        assertFalse(bur == Double.POSITIVE_INFINITY)
        assertFalse(bur == Double.NEGATIVE_INFINITY)
        assertEquals(1.0, bur, 0.0001)
    }

    @Test
    fun criDoesNotRequireWearables() {
        val cri = ScoringEquations.calculateCRI(
            goNoGoAccuracy = 0.80,
            reactionTimeCV = 0.20,
            subjectiveFatigue = 0.50,
        )

        assertEquals(74.0, cri, 0.0001)
    }

    @Test
    fun calibrationErrorEmptyListIsStable() {
        assertEquals(0.0, ScoringEquations.calculateCalibrationError(emptyList<Any>()), 0.0001)
    }

    @Test
    fun frsDetectsFatigueFromReactionSlowdownAndErrors() {
        val frs = ScoringEquations.calculateFRS(
            reactionTimeActualMs = 1_500L,
            reactionTimeMediaMs = 1_000L,
            goNoGoErrorRate = 0.60,
        )

        assertEquals(0.30, frs, 0.0001)
    }

    @Test
    fun agrsDoesNotFlagAnalyticalControlAsGaming() {
        val agrs = ScoringEquations.calculateAGRS(
            reactionTimeMs = 2_000L,
            spatialEntropy = 0.90,
            isDialDefault = false,
        )

        assertTrue(agrs < 0.30)
    }

    @Test
    fun agrsFlagsRealGamingPattern() {
        val agrs = ScoringEquations.calculateAGRS(
            reactionTimeMs = 250L,
            spatialEntropy = 0.0,
            isDialDefault = true,
        )

        assertTrue(agrs > 0.75)
    }

    @Test
    fun bayesScenarioGenerationIsRepeatableWithSeededRandom() {
        val first = BayesEngine.generateScenario(baseDifficulty(), random = Random(42))
        val second = BayesEngine.generateScenario(baseDifficulty(), random = Random(42))

        assertEquals(first, second)
        assertEquals(BayesEngine.calculatePosterior(first), first.expectedPosterior, 0.0001)
    }

    @Test
    fun reversibilityScenarioGenerationIsRepeatableWithSeededRandom() {
        val difficulty = baseDifficulty().copy(irreversibility = 0.65)

        val first = ReversibilityEngine.generateScenario(difficulty, random = Random(23))
        val second = ReversibilityEngine.generateScenario(difficulty, random = Random(23))

        assertEquals(first, second)
    }

    @Test
    fun reversibilityLockBlocksIrreversibleDecisionUntilElapsed() {
        val scenario = ReversibilityScenario(
            id = "lock",
            description = "Irreversible lock",
            isReversible = false,
            delayCostRate = 0.0,
            lockedDurationMs = 3_000L,
        )

        assertEquals(ReversibilityDecisionType.TYPE_1_IRREVERSIBLE, ReversibilityEngine.classifyDecision(scenario))
        assertTrue(ReversibilityEngine.validateLockDuration(scenario))
        assertFalse(ReversibilityEngine.validateLockDuration(scenario, elapsedMs = 2_999L))
        assertTrue(ReversibilityEngine.validateLockDuration(scenario, elapsedMs = 3_000L))
    }

    @Test
    fun generatedLowIrreversibilityScenarioIsTypeTwoWithoutLock() {
        val scenario = ReversibilityEngine.generateScenario(baseDifficulty().copy(irreversibility = 0.0))

        assertEquals(ReversibilityDecisionType.TYPE_2_REVERSIBLE, ReversibilityEngine.classifyDecision(scenario))
        assertEquals(0L, scenario.lockedDurationMs)
        assertTrue(ReversibilityEngine.validateLockDuration(scenario))
    }

    @Test
    fun debugEphemeralKeyProviderReturnsDefensiveCopies() {
        val provider = DebugEphemeralKeyProvider(random = Random(11))

        val first = provider.getDatabasePassphrase()
        val second = provider.getDatabasePassphrase()
        first[0] = (first[0] + 1).toByte()

        assertNotSame(first, second)
        assertFalse(first.contentEquals(provider.getDatabasePassphrase()))
        assertContentEquals(second, provider.getDatabasePassphrase())
    }

    @Test
    fun debugEphemeralKeyProvidersDoNotShareRuntimeKeyState() {
        val first = DebugEphemeralKeyProvider(random = Random(11)).getDatabasePassphrase()
        val second = DebugEphemeralKeyProvider(random = Random(12)).getDatabasePassphrase()

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun coachCatalogStaysTemplateBounded() {
        assertEquals(15, CoachEngine.templates.size)
        assertTrue(CoachEngine.templates.all { it.messageText.isNotBlank() })
    }

    @Test
    fun inMemoryDiaryRepositoryCopiesPayloads() {
        val repository = InMemoryDiaryRepository()
        val content = byteArrayOf(1, 2, 3)

        repository.saveDiaryEntry(
            DiaryEntry(
                id = "entry",
                timestampMs = 1L,
                encryptedContent = content,
                safetyFlag = false,
            ),
        )
        content[0] = 9

        val stored = repository.getDiaryEntry("entry")

        assertTrue(stored != null)
        assertContentEquals(byteArrayOf(1, 2, 3), stored.encryptedContent)
        assertNotSame(content, stored.encryptedContent)
    }

    @Test
    fun inMemorySessionRepositoryReturnsRecentSessionsByTimestampDescending() {
        val repository = InMemorySessionRepository()
        val older = decisionSession(sessionId = "older", timestampMs = 1_000L)
        val newest = decisionSession(sessionId = "newest", timestampMs = 3_000L)
        val middle = decisionSession(sessionId = "middle", timestampMs = 2_000L)

        repository.saveSession(older)
        repository.saveSession(newest)
        repository.saveSession(middle)

        assertEquals(listOf(newest, middle, older), repository.getRecentSessions(limit = 10))
    }

    @Test
    fun diaryEntryEqualityUsesByteContent() {
        val first = DiaryEntry(
            id = "entry",
            timestampMs = 1L,
            encryptedContent = byteArrayOf(1, 2, 3),
            safetyFlag = false,
        )
        val second = DiaryEntry(
            id = "entry",
            timestampMs = 1L,
            encryptedContent = byteArrayOf(1, 2, 3),
            safetyFlag = false,
        )

        assertTrue(first.encryptedContent.contentEquals(second.encryptedContent))
        assertEquals(first, second)
    }

    @Test
    fun predictionEventEqualityUsesByteContent() {
        val first = PredictionEvent(
            predictionId = "prediction",
            timestampMs = 1L,
            predictedProbability = 0.75,
            encryptedTextPayload = byteArrayOf(4, 5, 6),
            predictionHash = "hash",
            isVerified = true,
            outcomeValue = 1,
        )
        val second = PredictionEvent(
            predictionId = "prediction",
            timestampMs = 1L,
            predictedProbability = 0.75,
            encryptedTextPayload = byteArrayOf(4, 5, 6),
            predictionHash = "hash",
            isVerified = true,
            outcomeValue = 1,
        )

        assertTrue(first.encryptedTextPayload.contentEquals(second.encryptedTextPayload))
        assertEquals(first, second)
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

    private fun decisionSession(sessionId: String, timestampMs: Long) =
        DecisionSession(
            sessionId = sessionId,
            timestampMs = timestampMs,
            icd = 80.0,
            ied = 70.0,
            cde = 60.0,
            fatigueScore = 0.20,
            gamingScore = 0.10,
        )
}

package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.presentation.state.PolygonAxis
import com.mindverse.mindboost.core.coach.CoachEngine
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionRound
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecisionResultStateHolderTest {
    @Test
    fun assemblesSessionAndBuildsFiveHonestAxes() {
        val holder = DecisionResultStateHolder(
            sessionId = "session-1",
            timestampMs = 42L,
            checkIn = checkIn(baselineMs = 4_000L),
            rounds = listOf(perfectRound()),
        )

        val state = holder.uiState
        assertEquals(5, state.axes.size)
        assertEquals(100.0, axis(state.axes, "Calidad"), 0.0001)
        assertEquals(100.0 * exp(-0.15 * 0.4 * 0.4), axis(state.axes, "Efectividad"), 0.001)
        assertEquals(74.0, axis(state.axes, "Preparación"), 0.0001) // CRI from check-in
        assertEquals(97.0, axis(state.axes, "Integridad"), 0.0001) // (1 - 0.03 gaming) * 100
        assertEquals(100.0, axis(state.axes, "Frescura"), 0.0001) // (1 - 0 fatigue) * 100
        assertEquals(42.5, state.complexity, 0.0001)
        assertNull(state.coachMessage)
        assertEquals("session-1", holder.session.sessionId)
        assertEquals(42L, holder.session.timestampMs)
    }

    @Test
    fun emptyRoundsStillAssembleToAValidResult() {
        val holder = DecisionResultStateHolder(
            sessionId = "empty",
            timestampMs = 1L,
            checkIn = checkIn(baselineMs = 4_000L),
            rounds = emptyList(),
        )

        val state = holder.uiState
        assertEquals(0.0, axis(state.axes, "Calidad"), 0.0001)
        assertEquals(0.0, axis(state.axes, "Efectividad"), 0.0001)
        assertEquals(74.0, axis(state.axes, "Preparación"), 0.0001)
        assertEquals(100.0, axis(state.axes, "Integridad"), 0.0001)
        assertEquals(100.0, axis(state.axes, "Frescura"), 0.0001)
        assertEquals(0.0, state.complexity, 0.0001)
        assertTrue(state.axes.all { it.value in 0.0..100.0 })
    }

    @Test
    fun highGamingZeroesIntegrityAndSurfacesCoach() {
        val holder = DecisionResultStateHolder(
            sessionId = "gaming",
            timestampMs = 1L,
            checkIn = checkIn(baselineMs = 250L),
            rounds = listOf(gamingRound()),
        )

        val state = holder.uiState
        assertEquals(0.0, axis(state.axes, "Integridad"), 0.0001) // gaming score 1.0
        val t02 = CoachEngine.templates.first { it.id == "T02" }.messageText
        assertEquals(t02, state.coachMessage)
    }

    @Test
    fun diaryStartsEmptyAndUnsealed() {
        val holder = diaryHolder()

        assertEquals("", holder.diaryText)
        assertFalse(holder.sealed)
    }

    @Test
    fun diaryTextIsCapturedAndCapped() {
        val holder = diaryHolder()

        holder.onDiaryTextChange("Hoy decidí con calma.")
        assertEquals("Hoy decidí con calma.", holder.diaryText)

        holder.onDiaryTextChange("a".repeat(5_000))
        assertEquals(2_000, holder.diaryText.length)
    }

    @Test
    fun buildDiaryEntryCarriesSessionIdentityAndText() {
        val holder = diaryHolder(sessionId = "s-1", timestampMs = 99L)

        holder.onDiaryTextChange("reflexión")
        val entry = holder.buildDiaryEntry()

        assertEquals("s-1", entry.id)
        assertEquals(99L, entry.timestampMs)
        assertContentEquals("reflexión".encodeToByteArray(), entry.encryptedContent)
        assertFalse(entry.safetyFlag)
    }

    @Test
    fun sealLocksEditingAndReturnsEntry() {
        val holder = diaryHolder()

        holder.onDiaryTextChange("antes")
        val entry = holder.seal()

        assertTrue(holder.sealed)
        assertContentEquals("antes".encodeToByteArray(), entry.encryptedContent)

        holder.onDiaryTextChange("después")
        assertEquals("antes", holder.diaryText)
    }

    private fun diaryHolder(sessionId: String = "session", timestampMs: Long = 1L) =
        DecisionResultStateHolder(
            sessionId = sessionId,
            timestampMs = timestampMs,
            checkIn = checkIn(baselineMs = 1_000L),
            rounds = emptyList(),
        )

    private fun axis(axes: List<PolygonAxis>, label: String): Double =
        axes.first { it.label == label }.value

    private fun checkIn(baselineMs: Long) = CheckInInput(
        goNoGoAccuracy = 0.8,
        reactionTimeCV = 0.2,
        subjectiveFatigue = 0.5,
        reactionTimeBaselineMs = baselineMs,
    )

    private fun perfectRound() = DecisionRound(
        difficulty = baseDifficulty(),
        objectivesAlignment = 1.0,
        bayesianUpdateRatio = 1.0,
        biasDetected = false,
        alternativesScannedRatio = 1.0,
        reactionTimeMs = 4_000L,
        timeLimitMs = 10_000L,
        isReversible = false,
        spatialEntropy = 0.9,
        isDialDefault = false,
    )

    private fun gamingRound() = DecisionRound(
        difficulty = baseDifficulty(),
        objectivesAlignment = 1.0,
        bayesianUpdateRatio = 1.0,
        biasDetected = false,
        alternativesScannedRatio = 1.0,
        reactionTimeMs = 250L,
        timeLimitMs = 10_000L,
        isReversible = false,
        spatialEntropy = 0.0,
        isDialDefault = true,
    )

    private fun baseDifficulty() = DifficultyVector(
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
}

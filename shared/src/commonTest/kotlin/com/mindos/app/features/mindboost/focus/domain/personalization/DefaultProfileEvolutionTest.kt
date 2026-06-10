package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultProfileEvolutionTest {

    private val clock = EpochClock { FIXED_NOW }

    private class FixedInference(private val result: ArchetypeInferenceResult) : ArchetypeInference {
        override fun infer(signals: ArchetypeSignals): ArchetypeInferenceResult = result
    }

    private fun evolution(archetype: UserArchetype, confidence: Double) = DefaultProfileEvolution(
        inference = FixedInference(ArchetypeInferenceResult(archetype, confidence, secondGuess = null)),
        clock = clock,
    )

    private fun profile(archetype: UserArchetype, confidence: Double = 0.5) = UserCognitiveProfile(
        userId = "user",
        archetype = archetype,
        archetypeConfidence = confidence,
        totalSessions = 10,
    )

    @Test
    fun sameArchetype_consolidatesConfidence() {
        val before = profile(UserArchetype.IMPULSIVE, confidence = 0.5)
        val after = evolution(UserArchetype.IMPULSIVE, confidence = 0.4).evolve(before, emptyList(), retest = null)
        assertEquals(UserArchetype.IMPULSIVE, after.archetype)
        assertTrue(after.archetypeConfidence > before.archetypeConfidence)
    }

    @Test
    fun lowConfidenceDifferentInference_doesNotChangeArchetype() {
        val before = profile(UserArchetype.IMPULSIVE, confidence = 0.6)
        val after = evolution(UserArchetype.ADVANCED, confidence = 0.1).evolve(before, emptyList(), retest = null)
        assertEquals(UserArchetype.IMPULSIVE, after.archetype) // hysteresis holds
        assertTrue(after.archetypeConfidence < before.archetypeConfidence) // decays
    }

    @Test
    fun beginnerInferredElite_stepsOnlyToAdvanced_notElite() {
        val after = evolution(UserArchetype.ELITE, confidence = 0.9)
            .evolve(profile(UserArchetype.BEGINNER), emptyList(), retest = null)
        assertEquals(UserArchetype.ADVANCED, after.archetype)
    }

    @Test
    fun styleArchetypeInferredElite_promotesDirectly() {
        // IMPULSIVE (rank 1) → ELITE (rank 2) is a single rank step, allowed directly.
        val after = evolution(UserArchetype.ELITE, confidence = 0.9)
            .evolve(profile(UserArchetype.IMPULSIVE), emptyList(), retest = null)
        assertEquals(UserArchetype.ELITE, after.archetype)
    }

    @Test
    fun evolve_stampsUpdatedAtFromInjectedClock() {
        val after = evolution(UserArchetype.IMPULSIVE, 0.5).evolve(profile(UserArchetype.IMPULSIVE), emptyList(), null)
        assertEquals(FIXED_NOW, after.updatedAtMs)
    }

    private companion object {
        const val FIXED_NOW = 12_345L
    }
}

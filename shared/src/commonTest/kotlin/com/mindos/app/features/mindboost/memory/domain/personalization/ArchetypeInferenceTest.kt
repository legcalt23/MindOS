package com.mindos.app.features.mindboost.memory.domain.personalization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchetypeInferenceTest {

    private val inference = DefaultArchetypeInference()

    /** Neutral baseline that does not strongly favour any archetype; tests push one axis. */
    private val base = MemoryArchetypeSignals(
        accuracy = 0.6,
        rtMedianMs = 2_000.0,
        consolidationRatio = 0.75,
        discriminationDPrime = 2.0,
        intrusionRate = 0.05,
        confidenceBias = 0.0,
        workingMemorySpanNorm = 0.5,
        totalSessions = 8,
    )

    private fun archetypeOf(s: MemoryArchetypeSignals) = inference.infer(s).archetype

    @Test
    fun fewSessions_isBeginner() {
        assertEquals(MemoryArchetype.BEGINNER, archetypeOf(base.copy(totalSessions = 2)))
    }

    @Test
    fun lowConsolidation_isFastForgetter() {
        assertEquals(MemoryArchetype.FAST_FORGETTER, archetypeOf(base.copy(consolidationRatio = 0.1)))
    }

    @Test
    fun lowDiscriminationHighIntrusion_isConfusesSimilar() {
        assertEquals(
            MemoryArchetype.CONFUSES_SIMILAR,
            archetypeOf(base.copy(discriminationDPrime = 0.2, intrusionRate = 0.4)),
        )
    }

    @Test
    fun negativeBias_isUnderconfident() {
        assertEquals(MemoryArchetype.UNDERCONFIDENT, archetypeOf(base.copy(confidenceBias = -0.4)))
    }

    @Test
    fun positiveBias_isOverconfident() {
        assertEquals(MemoryArchetype.OVERCONFIDENT, archetypeOf(base.copy(confidenceBias = 0.4)))
    }

    @Test
    fun lowSpan_isLowWorkingMemory() {
        assertEquals(MemoryArchetype.LOW_WORKING_MEMORY, archetypeOf(base.copy(workingMemorySpanNorm = 0.0)))
    }

    @Test
    fun accurateAndSlow_isSlowButAccurate() {
        assertEquals(
            MemoryArchetype.SLOW_BUT_ACCURATE,
            archetypeOf(base.copy(accuracy = 0.95, rtMedianMs = 2_900.0)),
        )
    }

    @Test
    fun inaccurateAndFast_isFastButInaccurate() {
        assertEquals(
            MemoryArchetype.FAST_BUT_INACCURATE,
            archetypeOf(base.copy(accuracy = 0.3, rtMedianMs = 900.0)),
        )
    }

    @Test
    fun strongAcrossTheBoard_isAdvancedOrElite() {
        val advanced = archetypeOf(
            base.copy(accuracy = 0.9, consolidationRatio = 0.9, intrusionRate = 0.02, workingMemorySpanNorm = 0.5),
        )
        assertEquals(MemoryArchetype.ADVANCED, advanced)

        val elite = archetypeOf(
            base.copy(accuracy = 0.95, consolidationRatio = 0.95, intrusionRate = 0.02, workingMemorySpanNorm = 0.95),
        )
        assertEquals(MemoryArchetype.ELITE, elite)
    }

    @Test
    fun confidenceGrowsWithMargin() {
        val r = inference.infer(base.copy(confidenceBias = -0.4))
        assertTrue(r.confidence > 0.0)
        assertTrue(r.secondGuess != null)
    }
}

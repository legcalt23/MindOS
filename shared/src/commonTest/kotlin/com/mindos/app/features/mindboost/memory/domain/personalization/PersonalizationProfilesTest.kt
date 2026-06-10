package com.mindos.app.features.mindboost.memory.domain.personalization

import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.session.MemorySessionTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalizationProfilesTest {

    @Test
    fun elevenArchetypes() {
        assertEquals(11, MemoryArchetype.entries.size)
    }

    @Test
    fun aProfileExistsForEveryArchetype() {
        assertEquals(MemoryArchetype.entries.size, MemoryPersonalizationProfiles.all.size)
        MemoryArchetype.entries.forEach {
            assertEquals(it, MemoryPersonalizationProfiles.forArchetype(it).archetype)
        }
    }

    @Test
    fun fastForgetter_raisesDelayAndEpisodicWeight() {
        val p = MemoryPersonalizationProfiles.forArchetype(MemoryArchetype.FAST_FORGETTER)
        assertTrue(p.biasFor(MemoryDifficultyAxis.DELAY) > 0.0)
        assertTrue(p.weightFor(MemoryExerciseId.EX3_LISTA_ESTRATEGIA) > 1.0)
    }

    @Test
    fun overconfident_forcesConfidenceAndHoldsDifficulty() {
        val p = MemoryPersonalizationProfiles.forArchetype(MemoryArchetype.OVERCONFIDENT)
        assertTrue(p.scoringModifiers.forcedConfidencePrompt)
        assertTrue(p.scoringModifiers.holdDifficultyWhenConfidenceInflated)
    }

    @Test
    fun fastButInaccurate_addsMicroPauseAndErrorPenalty() {
        val p = MemoryPersonalizationProfiles.forArchetype(MemoryArchetype.FAST_BUT_INACCURATE)
        assertTrue(p.scoringModifiers.prePressureMicroPause)
        assertTrue(p.scoringModifiers.errorPenalty > 1.0)
    }

    @Test
    fun elite_prefersIntensiveTemplate() {
        assertEquals(
            MemorySessionTemplate.INTENSIVE_30,
            MemoryPersonalizationProfiles.forArchetype(MemoryArchetype.ELITE).preferredTemplate,
        )
    }

    @Test
    fun neutralLookupsFallBack() {
        val beginner = MemoryPersonalizationProfiles.forArchetype(MemoryArchetype.BEGINNER)
        assertEquals(1.0, beginner.weightFor(MemoryExerciseId.EX4_PARES_ASOCIADOS))
        assertEquals(0.0, beginner.biasFor(MemoryDifficultyAxis.INTERFERENCE))
    }
}

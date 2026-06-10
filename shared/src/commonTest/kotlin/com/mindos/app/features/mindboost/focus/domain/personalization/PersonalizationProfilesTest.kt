package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalizationProfilesTest {

    @Test
    fun everyArchetype_hasAProfile() {
        assertEquals(UserArchetype.entries.size, PersonalizationProfiles.all.size)
        UserArchetype.entries.forEach { assertEquals(it, PersonalizationProfiles.forArchetype(it).archetype) }
    }

    @Test
    fun impulsive_doesNotRewardSpeed_andAddsMicroPause() {
        val modifiers = PersonalizationProfiles.forArchetype(UserArchetype.IMPULSIVE).scoringModifiers
        assertTrue(modifiers.speedIncentive < 1.0)
        assertTrue(modifiers.prePressureMicroPause)
    }

    @Test
    fun fastButInaccurate_penalizesErrorsStrongly() {
        val modifiers = PersonalizationProfiles.forArchetype(UserArchetype.FAST_BUT_INACCURATE).scoringModifiers
        assertTrue(modifiers.errorPenalty > 1.0)
    }

    @Test
    fun lowConcentration_penalizesLapses() {
        val modifiers = PersonalizationProfiles.forArchetype(UserArchetype.LOW_CONCENTRATION).scoringModifiers
        assertTrue(modifiers.lapsePenalty > 1.0)
    }

    @Test
    fun impulsive_biasesTowardInhibitionExercises() {
        val profile = PersonalizationProfiles.forArchetype(UserArchetype.IMPULSIVE)
        assertTrue(profile.weightFor(ExerciseId.E3_GATILLO) > 1.0)
        assertTrue(profile.weightFor(ExerciseId.E4_FRENO) > 1.0)
        // An un-emphasized exercise keeps the neutral weight.
        assertEquals(1.0, profile.weightFor(ExerciseId.E9_ENJAMBRE))
    }

    @Test
    fun preferredTemplates_matchExperienceLevel() {
        assertEquals(
            SessionTemplate.MAINTENANCE_5,
            PersonalizationProfiles.forArchetype(UserArchetype.BEGINNER).preferredTemplate,
        )
        assertEquals(
            SessionTemplate.ELITE_30,
            PersonalizationProfiles.forArchetype(UserArchetype.ELITE).preferredTemplate,
        )
    }
}

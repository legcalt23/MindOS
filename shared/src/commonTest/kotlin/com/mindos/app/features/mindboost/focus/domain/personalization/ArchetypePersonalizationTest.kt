package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * axisBias (08_ §2) nudges the INITIAL difficulty per archetype, but only: (a) when there is no stored
 * value yet, (b) on the exercise's relevant axes. An impulsive user starts an inhibition exercise with
 * inhibitoryDemand+; a neutral user — or any exercise that does not use the biased axis — is unchanged.
 */
class ArchetypePersonalizationTest {

    private val e3 = ExerciseCatalog.definition(ExerciseId.E3_GATILLO) // relevant axes include INHIBITORY_DEMAND
    private val e1 = ExerciseCatalog.definition(ExerciseId.E1_VIGIA)   // relevant axes do NOT include it

    @Test
    fun impulsive_startsInhibitionExercise_withInhibitoryDemandBias() {
        val profile = UserCognitiveProfile(userId = "u1", archetype = UserArchetype.IMPULSIVE)
        val difficulty = ArchetypePersonalization.startingDifficulty(profile, e3)
        assertTrue(
            difficulty.level(DifficultyAxis.INHIBITORY_DEMAND).value > 0.0,
            "Impulsive must start an inhibition exercise above the floor on the inhibitory axis.",
        )
    }

    @Test
    fun bias_doesNotTouchAxesTheExerciseDoesNotUse() {
        val profile = UserCognitiveProfile(userId = "u1", archetype = UserArchetype.IMPULSIVE)
        // E1 does not train inhibition → the impulsive inhibitory bias must not apply.
        assertEquals(DifficultyVector.EASIEST, ArchetypePersonalization.startingDifficulty(profile, e1))
    }

    @Test
    fun neutralArchetype_isUnbiased() {
        val profile = UserCognitiveProfile(userId = "u1", archetype = UserArchetype.BEGINNER)
        assertEquals(DifficultyVector.EASIEST, ArchetypePersonalization.startingDifficulty(profile, e3))
    }

    @Test
    fun storedDifficulty_winsOverHeuristicBias() {
        val stored = DifficultyVector.EASIEST.stepUp(DifficultyAxis.PERCEPTUAL_LOAD, 0.5)
        val profile = UserCognitiveProfile(
            userId = "u1",
            archetype = UserArchetype.IMPULSIVE,
            startingDifficulty = mapOf(ExerciseId.E3_GATILLO to stored),
        )
        assertEquals(stored, ArchetypePersonalization.startingDifficulty(profile, e3))
    }
}

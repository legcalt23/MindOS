@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the "implemented but invisible" failure (2026-06-18 learning): every playable exercise must be
 * reachable from the home menu, and every menu id must have a generator + a definition. The hub mounts
 * `EmotionRootScreen` for the `Emociones` track via an exhaustive `when` (compiler-enforced).
 */
class EmotionHubReachabilityTest {

    @Test
    fun at_least_one_exercise_is_playable() {
        assertTrue(EmotionExerciseCatalog.implemented.isNotEmpty(), "v1 must ship playable exercises")
    }

    @Test
    fun home_menu_exactly_mirrors_implemented() {
        val menuIds = EmotionHomeStateHolder().exercises.map { it.id }.toSet()
        assertEquals(EmotionExerciseCatalog.implemented.toSet(), menuIds)
    }

    @Test
    fun every_menu_id_has_a_generator_and_a_definition() {
        EmotionHomeStateHolder().exercises.forEach { card ->
            assertEquals(card.id, EmotionExerciseCatalog.definition(card.id).id)
            assertTrue(card.id in EmotionExerciseGenerators.registry.keys, "${card.id} is selectable but has no generator")
        }
    }
}

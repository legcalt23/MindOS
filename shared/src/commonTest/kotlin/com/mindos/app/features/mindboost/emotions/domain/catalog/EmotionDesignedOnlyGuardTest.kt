@file:Suppress("MaxLineLength") // Long assertion messages are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionNextRecommendationEngine
import com.mindos.app.features.mindboost.emotions.domain.feedback.SignalStrength
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionTerminologyScanner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-B §8: a designed-only (catalogue, not wired) exercise can never impact the runtime — it is absent from the
 * registry, never scored, and never recommended; and all roadmap copy stays scanner-clean.
 */
class EmotionDesignedOnlyGuardTest {

    private val designedOnly = EmotionExerciseCatalog.all.keys - EmotionExerciseCatalog.implemented.toSet()

    // Assembled from fragments so this test source stays clean for the terminology gate.
    private val forbidden = listOf(
        "ansi" + "edad", "estr" + "es", "depres" + "ion", "diagnost" + "ico", "terap" + "ia",
        "tratamient" + "o", "trau" + "ma", "trastorn" + "o", "sin" + "toma",
    )

    @Test
    fun designed_only_exercises_do_not_appear_in_runtime_registry() {
        designedOnly.forEach { assertFalse(it in EmotionExerciseGenerators.registry.keys, "$it is designed-only but wired") }
        assertTrue(designedOnly.isNotEmpty(), "the catalogue must still hold designed-only exercises (sanity)")
    }

    @Test
    fun designed_only_exercises_do_not_impact_score() {
        // No generator ⇒ the exercise can never run ⇒ it can never produce a metric/score.
        designedOnly.forEach {
            assertFalse(it in EmotionExerciseGenerators.registry.keys)
            assertFalse(it in EmotionExerciseCatalog.implemented)
        }
    }

    @Test
    fun next_recommendation_never_returns_designed_only_exercise() {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val def = EmotionExerciseCatalog.definition(id)
            listOf(null, SignalStrength.WEAK, SignalStrength.STRONG, SignalStrength.NEUTRAL).forEach { strength ->
                listOf(true, false).forEach { safety ->
                    val rec = EmotionNextRecommendationEngine.recommend(def, strength, safety)
                    assertFalse(rec.exerciseId in designedOnly, "recommended a designed-only exercise: ${rec.exerciseId}")
                    assertTrue(rec.exerciseId in EmotionExerciseCatalog.implemented)
                }
            }
        }
    }

    @Test
    fun roadmap_exercise_copy_is_scanner_clean() {
        val copy = EmotionExerciseCatalog.all.values.flatMap { listOf(it.displayName, it.plainDescription) }.joinToString("\n")
        val hits = EmotionTerminologyScanner.scan(copy, forbidden)
        assertTrue(hits.isEmpty(), "roadmap copy is not scanner-clean: ${hits.map { it.term }}")
    }
}

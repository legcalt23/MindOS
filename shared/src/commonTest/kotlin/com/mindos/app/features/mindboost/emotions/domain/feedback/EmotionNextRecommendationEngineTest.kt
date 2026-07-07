@file:Suppress("MaxLineLength") // Long assertion messages are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u P1 retention: the pure next-exercise recommendation must be outcome-driven, safe and non-clinical. */
class EmotionNextRecommendationEngineTest {

    private val engine = EmotionNextRecommendationEngine

    private val strengths = listOf(null, SignalStrength.WEAK, SignalStrength.STRONG, SignalStrength.NEUTRAL)

    /** Every recommendation across implemented exercises × strengths × safety flag. */
    private fun allRecs(): List<EmotionRecommendation> = buildList {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val def = EmotionExerciseCatalog.definition(id)
            strengths.forEach { s -> add(engine.recommend(def, s, stoppedForSafety = false)); add(engine.recommend(def, s, stoppedForSafety = true)) }
        }
    }

    @Test
    fun next_recommendation_changes_by_governing_metric() {
        val commission = engine.recommend(EmotionExerciseCatalog.EM1_PAUSA, SignalStrength.WEAK, stoppedForSafety = false)
        val separation = engine.recommend(EmotionExerciseCatalog.EM3_SEPARADOR, SignalStrength.WEAK, stoppedForSafety = false)
        assertTrue(commission.exerciseId != separation.exerciseId, "weak inhibition and weak separation must recommend different exercises")
    }

    @Test
    fun next_recommendation_after_safety_stop_is_lower_intensity_or_pause() {
        val rec = engine.recommend(EmotionExerciseCatalog.EM3_SEPARADOR, SignalStrength.STRONG, stoppedForSafety = true)
        assertEquals(EmotionExerciseId.EM1_PAUSA, rec.exerciseId, "after a safety stop, recommend the base task, never a push")
    }

    @Test
    fun next_recommendation_low_confidence_recommends_repeat_or_longer_session() {
        val rec = engine.recommend(EmotionExerciseCatalog.EM2_RECUPERACION, strength = null, stoppedForSafety = false)
        assertEquals(EmotionExerciseCatalog.EM2_RECUPERACION.id, rec.exerciseId, "insufficient sample → repeat the same exercise")
        assertTrue(rec.reason.contains("Repite", ignoreCase = true))
    }

    @Test
    fun next_recommendation_does_not_use_clinical_language() {
        val bad = listOf("diagn" + "óstico", "tera" + "pia", "ansied" + "ad", "tras" + "torno", "eres ")
        val reasons = allRecs().map { it.reason }
        reasons.forEach { reason ->
            bad.forEach { term -> assertFalse(reason.contains(term, ignoreCase = true), "clinical term '$term' in reason: $reason") }
        }
    }

    @Test
    fun recommendation_is_always_an_implemented_exercise() {
        allRecs().forEach { rec -> assertTrue(rec.exerciseId in EmotionExerciseCatalog.implemented, "recommended a non-implemented exercise: ${rec.exerciseId}") }
    }
}

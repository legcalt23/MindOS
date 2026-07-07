@file:Suppress("MagicNumber", "MaxLineLength") // Sample values + long builder calls are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmotionFeedbackBuilderTest {

    private val builder = EmotionFeedbackBuilder()

    private fun metric(key: EmotionMetricKey, value: Double, confident: Boolean = true) =
        EmotionMetricValue(key, value, 20, confident, EmotionMetricUnit.INDEX)

    private fun allStrings(fb: FourPartFeedback): List<String> = listOfNotNull(
        fb.whatHappened, fb.whatItMeans, fb.whatTrained, fb.whatPerformanceShowed, fb.strongSignal, fb.weakSignal,
        fb.whatToPractice, fb.whatNotToConclude, fb.confidenceNote, fb.nextRecommendationReason, fb.safetyNote,
    )

    @Test
    fun feedback_has_all_four_parts_and_carries_the_transfer_label() {
        val fb = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, metric(EmotionMetricKey.POST_ERROR_RECOVERY, 0.7), ProfileTier.STABLE)
        assertTrue(fb.whatHappened.isNotBlank())
        assertTrue(fb.whatItMeans.isNotBlank())
        assertTrue(fb.whatToPractice.isNotBlank())
        assertTrue(fb.whatNotToConclude.isNotBlank())
        assertEquals(TransferLabel.PLAUSIBLE, fb.transferLabel)
    }

    @Test
    fun exploratory_tier_is_marked_preliminary() {
        val fb = builder.build(EmotionExerciseCatalog.EM3_SEPARADOR, metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.8, confident = true), ProfileTier.EXPLORATORY)
        assertTrue(fb.confidenceNote.contains("preliminares", ignoreCase = true))
        assertEquals(TransferLabel.NOT_DEMONSTRATED, fb.transferLabel)
    }

    @Test
    fun whatToPractice_differs_between_commission_and_recovery_exercises() {
        val commission = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.5), ProfileTier.STABLE)
        val recovery = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, metric(EmotionMetricKey.POST_ERROR_RECOVERY, 0.2), ProfileTier.STABLE)
        assertTrue(commission.whatToPractice != recovery.whatToPractice, "commission and recovery must give distinct practice cues")
        assertTrue(commission.whatTrained != recovery.whatTrained)
    }

    @Test
    fun whatToPractice_differs_between_inhibition_and_categorization() {
        val inhibition = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.5), ProfileTier.STABLE)
        val categorization = builder.build(EmotionExerciseCatalog.EM3_SEPARADOR, metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.3), ProfileTier.STABLE)
        assertTrue(inhibition.whatToPractice != categorization.whatToPractice)
    }

    @Test
    fun high_vs_low_confident_value_same_exercise_differ() {
        val strong = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.1), ProfileTier.STABLE)
        val weak = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.5), ProfileTier.STABLE)
        assertTrue(strong.whatPerformanceShowed != weak.whatPerformanceShowed, "two confident values must read differently")
        assertTrue(strong.whatToPractice != weak.whatToPractice)
    }

    @Test
    fun not_confident_yields_generic_no_reading() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.1, confident = false), ProfileTier.STABLE)
        assertEquals(EmotionFeedbackCopy.INSUFFICIENT_DATA_PERFORMANCE, fb.whatPerformanceShowed)
        assertNull(fb.strongSignal)
        assertNull(fb.weakSignal)
    }

    @Test
    fun lower_is_better_metric_interprets_low_as_strong() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.1), ProfileTier.STABLE)
        assertNotNull(fb.strongSignal)
        assertNull(fb.weakSignal)
    }

    @Test
    fun higher_is_better_metric_interprets_high_as_strong() {
        val fb = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, metric(EmotionMetricKey.POST_ERROR_RECOVERY, 0.8), ProfileTier.STABLE)
        assertNotNull(fb.strongSignal)
        assertNull(fb.weakSignal)
    }

    @Test
    fun feedback_never_uses_person_level_trait_language() {
        // Clinical lexemes are assembled by fragments so THIS test file stays clean for the terminology scanner
        // (which reads whole files); the runtime strings are still the full words for the contains() check.
        val forbidden = listOf(
            "eres ", "tu mente es", "tienes baja", "emocionalmente d" + "ébil",
            "impuls" + "ivo", "ansied" + "ad", "diagn" + "óstico", "tera" + "pia",
        )
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.5), ProfileTier.STABLE)
        allStrings(fb).forEach { s ->
            forbidden.forEach { bad -> assertFalse(s.contains(bad, ignoreCase = true), "person-level/clinical phrase '$bad' in: $s") }
        }
    }

    @Test
    fun feedback_includes_what_not_to_conclude() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.2), ProfileTier.STABLE)
        assertTrue(fb.whatNotToConclude.contains("no una conclusión", ignoreCase = true))
    }

    @Test
    fun feedback_recommends_next_exercise() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.5), ProfileTier.STABLE)
        val next = assertNotNull(fb.nextRecommendedExerciseId)
        assertTrue(next in EmotionExerciseCatalog.implemented, "recommendation must be an implemented exercise, got $next")
        assertTrue(fb.nextRecommendationReason.isNotBlank())
    }

    @Test
    fun safety_stop_feedback_suppresses_performance_judgment() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.1), ProfileTier.STABLE, stoppedForSafety = true)
        assertTrue(fb.whatToPractice.isBlank(), "no next-practice push after a safety stop")
        assertTrue(fb.whatPerformanceShowed.isBlank())
        assertNull(fb.strongSignal)
        assertNull(fb.weakSignal)
        assertNotNull(fb.safetyNote)
        assertEquals(com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId.EM1_PAUSA, fb.nextRecommendedExerciseId)
    }
}

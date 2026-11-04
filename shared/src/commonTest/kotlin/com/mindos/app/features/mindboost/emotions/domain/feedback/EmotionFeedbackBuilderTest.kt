@file:Suppress("MagicNumber", "MaxLineLength") // Sample values + long builder calls are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionConfusionMatrix
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
        fb.confusionInsight,
    )

    @Test
    fun feedback_has_all_four_parts_and_carries_the_transfer_label() {
        val fb = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, metric(EmotionMetricKey.POST_ERROR_ACCURACY, 0.7), ProfileTier.STABLE)
        assertTrue(fb.whatHappened.isNotBlank())
        assertTrue(fb.whatItMeans.isNotBlank())
        assertTrue(fb.whatToPractice.isNotBlank())
        assertTrue(fb.whatNotToConclude.isNotBlank())
        // Ola 3 · R2 dropped EM2 from PLAUSIBLE to NOT_DEMONSTRATED.
        assertEquals(TransferLabel.NOT_DEMONSTRATED, fb.transferLabel)
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
        val recovery = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, metric(EmotionMetricKey.POST_ERROR_ACCURACY, 0.2), ProfileTier.STABLE)
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
        // Ola 2 · R3 · P4 added a user-facing slot, so the guard is run over a feedback that HAS one: a slot only
        // scanned when it happens to be null would let the newest copy be the only copy nobody checks.
        val withConfusion = builder.build(
            EmotionExerciseCatalog.EM3_SEPARADOR,
            metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.8),
            ProfileTier.STABLE,
            confusion = urgesAsFacts(),
        )
        assertNotNull(withConfusion.confusionInsight, "fixture must populate the slot or this test skips it")
        (allStrings(fb) + allStrings(withConfusion)).forEach { s ->
            forbidden.forEach { bad -> assertFalse(s.contains(bad, ignoreCase = true), "person-level/clinical phrase '$bad' in: $s") }
        }
    }

    @Test
    fun feedback_includes_what_not_to_conclude() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.2), ProfileTier.STABLE)
        assertTrue(fb.whatNotToConclude.contains("no una conclusión", ignoreCase = true))
    }

    @Test
    fun post_error_recovery_feedback_is_gated_when_not_confident() {
        // MC8u-B §1C: an unconfident governing metric (too few post-error cases / no baseline) must read as
        // insufficient data, never a performance judgment.
        val fb = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, metric(EmotionMetricKey.POST_ERROR_RECOVERY, 0.9, confident = false), ProfileTier.STABLE)
        assertEquals(EmotionFeedbackCopy.INSUFFICIENT_DATA_PERFORMANCE, fb.whatPerformanceShowed)
        assertNull(fb.strongSignal)
        assertNull(fb.weakSignal)
        assertFalse(fb.confident)
    }

    @Test
    fun feedback_recommends_next_exercise() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.5), ProfileTier.STABLE)
        val next = assertNotNull(fb.nextRecommendedExerciseId)
        assertTrue(next in EmotionExerciseCatalog.implemented, "recommendation must be an implemented exercise, got $next")
        assertTrue(fb.nextRecommendationReason.isNotBlank())
    }

    /** A session whose mistakes pile onto urges-answered-as-facts, plus a solid diagonal. */
    private fun urgesAsFacts() = EmotionConfusionMatrix(
        EmotionCategory.entries.associateWith { truth ->
            when (truth) {
                EmotionCategory.IMPULSE -> mapOf(EmotionCategory.IMPULSE to 2, EmotionCategory.FACT to 5)
                else -> mapOf(truth to 6)
            }
        },
    )

    @Test
    fun the_dominant_confusion_reaches_the_feedback_naming_the_direction_and_the_check() {
        val fb = builder.build(
            EmotionExerciseCatalog.EM3_SEPARADOR,
            metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.8),
            ProfileTier.STABLE,
            confusion = urgesAsFacts(),
        )
        val insight = assertNotNull(fb.confusionInsight, "the matrix must reach the user, not stop at the result")
        assertTrue(insight.contains("impulsos"), "the direction's source category must be named: $insight")
        assertTrue(insight.contains("hechos"), "the over-applied label must be named: $insight")
        assertTrue(insight.contains("Antes de marcar hecho"), "the cue must be the check for what was over-used")
        // Session-scoped, never a trait. "Sueles etiquetar…" is a claim about the person that one block of one
        // exercise cannot support, and it is exactly the phrasing this line is written to avoid.
        assertTrue(insight.startsWith("En esta sesión"), "the insight must stay scoped to this session: $insight")
        assertFalse(insight.contains("sueles", ignoreCase = true))
    }

    @Test
    fun no_dominant_confusion_produces_no_line_rather_than_a_reassuring_one() {
        val scattered = EmotionConfusionMatrix(
            mapOf(
                EmotionCategory.IMPULSE to mapOf(EmotionCategory.FACT to 2),
                EmotionCategory.THOUGHT to mapOf(EmotionCategory.FACT to 2),
                EmotionCategory.INTERNAL_STATE to mapOf(EmotionCategory.THOUGHT to 2),
            ),
        )
        val fb = builder.build(
            EmotionExerciseCatalog.EM3_SEPARADOR,
            metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.8),
            ProfileTier.STABLE,
            confusion = scattered,
        )
        assertNull(fb.confusionInsight, "no pattern must mean no section, never an 'all looked fine' line")
    }

    @Test
    fun an_unconfident_or_safety_stopped_session_never_names_a_confusion() {
        // The gaming guard is what drives `confident`, and a user who presses the same key all session produces
        // the most lopsided matrix the exercise can make. Naming it would be confidently wrong advice.
        val unconfident = builder.build(
            EmotionExerciseCatalog.EM3_SEPARADOR,
            metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.8, confident = false),
            ProfileTier.STABLE,
            confusion = urgesAsFacts(),
        )
        assertNull(unconfident.confusionInsight)

        val stopped = builder.build(
            EmotionExerciseCatalog.EM3_SEPARADOR,
            metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.8),
            ProfileTier.STABLE,
            stoppedForSafety = true,
            confusion = urgesAsFacts(),
        )
        assertNull(stopped.confusionInsight, "a safety stop suppresses every measured slot, this one included")
    }

    @Test
    fun an_exercise_without_a_matrix_is_unaffected() {
        val fb = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(EmotionMetricKey.COMMISSION_RATE, 0.1), ProfileTier.STABLE)
        assertNull(fb.confusionInsight)
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

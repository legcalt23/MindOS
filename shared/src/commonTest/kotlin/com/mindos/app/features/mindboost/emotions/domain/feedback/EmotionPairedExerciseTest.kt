@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values + long assertions are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u-B §7: paired exercises (EM1/EM8, EM2/EM11, EM3/EM10) must not read as clones — their instructions, feedback,
 * and next-recommendation reasons must differ so the user perceives distinct training.
 */
class EmotionPairedExerciseTest {

    private val builder = EmotionFeedbackBuilder()

    private val pairs = listOf(
        EmotionExerciseCatalog.EM1_PAUSA to EmotionExerciseCatalog.EM8_ANTIIMPULSO,
        EmotionExerciseCatalog.EM2_RECUPERACION to EmotionExerciseCatalog.EM11_FEEDBACK_NEG,
        EmotionExerciseCatalog.EM3_SEPARADOR to EmotionExerciseCatalog.EM10_SEP_PRESION,
    )

    private fun metric(def: EmotionExerciseDefinition) =
        EmotionMetricValue(def.governingMetric, 0.5, 20, true, EmotionMetricUnit.INDEX)

    private fun assertDistinctInstructions(a: EmotionExerciseDefinition, b: EmotionExerciseDefinition) {
        assertTrue(a.displayName != b.displayName, "${a.id}/${b.id} share a display name")
        assertTrue(a.plainDescription != b.plainDescription, "${a.id}/${b.id} share a description")
    }

    @Test
    fun paired_exercises_have_distinct_user_facing_instructions() {
        pairs.forEach { (a, b) -> assertDistinctInstructions(a, b) }
    }

    @Test
    fun paired_exercises_have_distinct_feedback_copy() {
        pairs.forEach { (a, b) ->
            val fa = builder.build(a, metric(a), ProfileTier.STABLE)
            val fb = builder.build(b, metric(b), ProfileTier.STABLE)
            assertTrue(fa.whatHappened != fb.whatHappened, "${a.id}/${b.id} share identical feedback")
        }
    }

    @Test
    fun paired_exercises_have_distinct_recommendation_reason() {
        pairs.forEach { (a, b) ->
            val weakA = EmotionNextRecommendationEngine.recommend(a, SignalStrength.WEAK, false).reason
            val weakB = EmotionNextRecommendationEngine.recommend(b, SignalStrength.WEAK, false).reason
            assertTrue(weakA != weakB, "${a.id}/${b.id} share a weak-signal recommendation reason")
            val strongA = EmotionNextRecommendationEngine.recommend(a, SignalStrength.STRONG, false).reason
            val strongB = EmotionNextRecommendationEngine.recommend(b, SignalStrength.STRONG, false).reason
            assertTrue(strongA != strongB, "${a.id}/${b.id} share a strong-signal recommendation reason")
        }
    }

    @Test
    fun em1_em8_do_not_share_identical_instruction_copy() =
        assertDistinctInstructions(EmotionExerciseCatalog.EM1_PAUSA, EmotionExerciseCatalog.EM8_ANTIIMPULSO)

    @Test
    fun em2_em11_do_not_share_identical_instruction_copy() =
        assertDistinctInstructions(EmotionExerciseCatalog.EM2_RECUPERACION, EmotionExerciseCatalog.EM11_FEEDBACK_NEG)

    @Test
    fun em3_em10_do_not_share_identical_instruction_copy() =
        assertDistinctInstructions(EmotionExerciseCatalog.EM3_SEPARADOR, EmotionExerciseCatalog.EM10_SEP_PRESION)
}

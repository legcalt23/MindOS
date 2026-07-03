@file:Suppress("MagicNumber") // Sample values are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmotionFeedbackBuilderTest {

    private val builder = EmotionFeedbackBuilder()

    @Test
    fun feedback_has_all_four_parts_and_carries_the_transfer_label() {
        val primary = EmotionMetricValue(EmotionMetricKey.POST_ERROR_RECOVERY, 0.7, 12, true, EmotionMetricUnit.INDEX)
        val feedback = builder.build(EmotionExerciseCatalog.EM2_RECUPERACION, primary, ProfileTier.STABLE)
        assertTrue(feedback.whatHappened.isNotBlank())
        assertTrue(feedback.whatItMeans.isNotBlank())
        assertTrue(feedback.whatToPractice.isNotBlank())
        assertTrue(feedback.whatNotToConclude.isNotBlank())
        assertEquals(TransferLabel.PLAUSIBLE, feedback.transferLabel)
    }

    @Test
    fun exploratory_tier_is_marked_preliminary() {
        val primary = EmotionMetricValue(EmotionMetricKey.CATEGORY_ACCURACY, 0.8, 3, true, EmotionMetricUnit.PROPORTION)
        val feedback = builder.build(EmotionExerciseCatalog.EM3_SEPARADOR, primary, ProfileTier.EXPLORATORY)
        assertTrue(feedback.confidenceNote.contains("preliminares", ignoreCase = true))
        assertEquals(TransferLabel.NOT_DEMONSTRATED, feedback.transferLabel)
    }
}

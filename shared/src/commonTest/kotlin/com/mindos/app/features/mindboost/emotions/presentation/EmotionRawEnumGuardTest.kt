@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values + broad copy aggregation are the data under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionNextRecommendationEngine
import com.mindos.app.features.mindboost.emotions.domain.feedback.SignalStrength
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionLabel
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEventType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-B §6: NO raw enum token (category, label, tier, safety-event type, transfer confidence, recommendation reason,
 * exercise id, metric key) may reach the user, and every user-facing enum value must have a real string mapping.
 */
class EmotionRawEnumGuardTest {

    private val builder = EmotionFeedbackBuilder()
    private val rawEnumRegex = Regex("[A-Z]{2,}_[A-Z]")

    /** Every user-facing string the module can render — feedback, recommendations, result notes and enum mappings. */
    private fun visibleCopy(): List<String> = buildList {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val def = EmotionExerciseCatalog.definition(id)
            ProfileTier.entries.forEach { tier ->
                listOf(true, false).forEach { confident ->
                    listOf(true, false).forEach { safety ->
                        val metric = EmotionMetricValue(def.governingMetric, 0.7, 20, confident, EmotionMetricUnit.INDEX)
                        val fb = builder.build(def, metric, tier, stoppedForSafety = safety)
                        addAll(listOfNotNull(fb.whatHappened, fb.whatItMeans, fb.whatTrained, fb.whatPerformanceShowed, fb.strongSignal, fb.weakSignal, fb.whatToPractice, fb.whatNotToConclude, fb.confidenceNote, fb.nextRecommendationReason, fb.safetyNote))
                    }
                }
            }
        }
        EmotionExerciseCatalog.all.values.forEach { add(it.displayName); add(it.plainDescription) }
        EmotionCategory.entries.forEach { add(EmotionStrings.categoryLabel(it)) }
        EmotionLabel.entries.forEach { add(EmotionStrings.emotionLabel(it)) }
        TransferLabel.entries.forEach { add(EmotionStrings.transferLabel(it)) }
        ProfileTier.entries.forEach { add(EmotionStrings.tierLabel(it)) }
        addAll(listOf(EmotionStrings.RESULT_WHAT_TRAINED, EmotionStrings.RESULT_PERFORMANCE, EmotionStrings.RESULT_PRACTICE, EmotionStrings.RESULT_LIMITS, EmotionStrings.RESULT_NEXT, EmotionStrings.RESULT_SAFETY_TITLE, EmotionStrings.RESULT_SESSION_NOT_COUNTED, EmotionStrings.RESULT_INTENSITY_REDUCED, EmotionStrings.RESULT_LOW_CONFIDENCE, EmotionStrings.RESULT_CALIBRATING, EmotionStrings.validSessionsProgress(3), EmotionStrings.HISTORY_TITLE, EmotionStrings.HISTORY_VALID_LABEL, EmotionStrings.HISTORY_PRACTICE_LABEL))
    }

    @Test
    fun no_raw_enum_visible_for_category_label_tier_safety_transfer() {
        val copy = visibleCopy()
        val enumNames = EmotionCategory.entries.map { it.name } + ProfileTier.entries.map { it.name } +
            TransferLabel.entries.map { it.name } + SafetyEventType.entries.map { it.name } + EmotionLabel.entries.map { it.name }
        copy.forEach { s ->
            enumNames.forEach { name -> assertFalse(s.contains(name), "raw enum '$name' leaked to user: $s") }
        }
    }

    @Test
    fun no_raw_enum_visible_for_recommendation_reason() {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val def = EmotionExerciseCatalog.definition(id)
            listOf(null, SignalStrength.WEAK, SignalStrength.STRONG, SignalStrength.NEUTRAL).forEach { s ->
                val reason = EmotionNextRecommendationEngine.recommend(def, s, stoppedForSafety = false).reason
                assertFalse(rawEnumRegex.containsMatchIn(reason), "raw enum token in recommendation reason: $reason")
            }
        }
    }

    @Test
    fun no_raw_enum_visible_for_metric_key() {
        val copy = visibleCopy()
        EmotionMetricKey.entries.forEach { key ->
            copy.forEach { s -> assertFalse(s.contains(key.name), "raw metric key '${key.name}' leaked to user: $s") }
        }
    }

    @Test
    fun no_raw_enum_visible_for_exercise_id() {
        val copy = visibleCopy()
        EmotionExerciseId.entries.forEach { id ->
            copy.forEach { s -> assertFalse(s.contains(id.name), "raw exercise id '${id.name}' leaked to user: $s") }
        }
    }

    @Test
    fun all_user_facing_enum_values_have_string_mapping() {
        EmotionCategory.entries.forEach { assertTrue(EmotionStrings.categoryLabel(it).isNotBlank()) }
        EmotionLabel.entries.forEach { assertTrue(EmotionStrings.emotionLabel(it).isNotBlank()) }
        TransferLabel.entries.forEach { assertTrue(EmotionStrings.transferLabel(it).isNotBlank()) }
        ProfileTier.entries.forEach { assertTrue(EmotionStrings.tierLabel(it).isNotBlank()) }
    }
}

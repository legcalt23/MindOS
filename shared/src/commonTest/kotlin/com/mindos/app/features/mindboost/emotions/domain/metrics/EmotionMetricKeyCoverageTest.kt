@file:Suppress("MagicNumber") // Sample values/sizes are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins that the emotions module owns a complete [EmotionMetricKey] enum (the focus `MetricKey` is a
 * closed enum with no emotion keys) and that the confidence-safe accessors behave. The "no import of
 * focus MetricKey" boundary is enforced separately by EmotionMetricKeyImportGuardTest (desktopTest).
 */
class EmotionMetricKeyCoverageTest {

    private fun acc(value: Double, n: Int, confident: Boolean) =
        EmotionMetricValue(EmotionMetricKey.ACCURACY, value, n, confident, EmotionMetricUnit.PROPORTION)

    @Test
    fun contains_the_headline_and_core_emotion_keys() {
        val keys = EmotionMetricKey.entries.toSet()
        val required = setOf(
            EmotionMetricKey.POST_ERROR_RECOVERY,
            EmotionMetricKey.COMMISSION_RATE,
            EmotionMetricKey.CATEGORY_ACCURACY,
            EmotionMetricKey.EMOTION_THOUGHT_SEPARATION,
            EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
            EmotionMetricKey.CALIBRATION_ERROR,
            EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX,
            EmotionMetricKey.SAFE_PERSISTENCE_INDEX,
            EmotionMetricKey.REGULATION_STABILITY,
            EmotionMetricKey.GRANULARITY_INDEX,
        )
        assertTrue(keys.containsAll(required), "Missing keys: ${required - keys}")
    }

    @Test
    fun unavailable_is_not_confident_and_zero() {
        val value = EmotionMetricValue.unavailable(EmotionMetricKey.POST_ERROR_RECOVERY, EmotionMetricUnit.INDEX)
        assertEquals(0.0, value.value)
        assertTrue(!value.confident)
    }

    @Test
    fun value_if_confident_gates_on_confidence_and_presence() {
        val confident: EmotionMetricSet = mapOf(EmotionMetricKey.ACCURACY to acc(0.9, 20, true))
        val unconfident: EmotionMetricSet = mapOf(EmotionMetricKey.ACCURACY to acc(0.9, 2, false))
        assertEquals(0.9, confident.valueIfConfident(EmotionMetricKey.ACCURACY))
        assertNull(unconfident.valueIfConfident(EmotionMetricKey.ACCURACY))
        assertNull(emptyMap<EmotionMetricKey, EmotionMetricValue>().valueIfConfident(EmotionMetricKey.ACCURACY))
    }
}

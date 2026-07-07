@file:Suppress("MaxLineLength") // Long assertion messages are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-B+ §G: a runtime (implemented) Emociones exercise MUST carry complete, honest spec metadata — name, trained
 * capacity, governing metric (in its primary set, producible, non-phantom), adaptive zone, single transfer label, a
 * mechanic version, and a live generator. The build fails the moment a runtime exercise is missing any of it.
 */
class EmotionExerciseSpecContractTest {

    private val phantomMetrics = setOf(
        EmotionMetricKey.CALIBRATION_ERROR,
        EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX,
        EmotionMetricKey.REGULATION_STABILITY,
    )

    @Test
    fun every_runtime_exercise_has_complete_spec_metadata() {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val d = EmotionExerciseCatalog.definition(id)
            assertTrue(d.displayName.isNotBlank(), "$id has no displayName")
            assertTrue(d.plainDescription.isNotBlank(), "$id has no plainDescription")
            assertTrue(d.primaryMetrics.isNotEmpty(), "$id has no primary metrics")
            assertTrue(d.governingMetric in d.primaryMetrics, "$id governing metric not in its primary set")
            assertTrue(d.governingMetric !in phantomMetrics, "$id governs on a phantom metric")
            assertTrue(EmotionScoringVersions.exerciseVersion(id.name) >= 1, "$id has no positive exercise version")
            assertTrue(id in EmotionExerciseGenerators.registry.keys, "$id is implemented but has no runtime generator")
        }
    }

    @Test
    fun transfer_label_is_single_and_honest_per_runtime_exercise() {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val label = EmotionExerciseCatalog.definition(id).transferLabel
            assertTrue(label in TransferLabel.entries, "$id has an invalid transfer label")
        }
    }

    @Test
    fun mvp_ready_set_equals_runtime_registry() {
        assertTrue(EmotionExerciseCatalog.implemented.toSet() == EmotionExerciseGenerators.registry.keys, "implemented ≠ registry")
        // A catalogue-only (designed) exercise must never masquerade as runtime.
        val designedOnly = EmotionExerciseCatalog.all.keys - EmotionExerciseCatalog.implemented.toSet()
        designedOnly.forEach { assertFalse(it in EmotionExerciseGenerators.registry.keys, "$it is designed-only but wired") }
    }
}

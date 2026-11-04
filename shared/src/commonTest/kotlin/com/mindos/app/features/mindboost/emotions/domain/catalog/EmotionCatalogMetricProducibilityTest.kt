@file:Suppress("MagicNumber", "MaxLineLength") // Trial counts/RTs are the synthetic fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.metrics.ClassifiedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u P0-2: the catalog must never advertise a governing metric the engine cannot compute. [PRODUCIBLE] is
 * derived by RUNNING [DefaultEmotionMetricsEngine] over a synthetic block of each paradigm (go/no-go, 2-alt,
 * classification) and collecting the keys it actually emits — so it can never drift from the engine. Every
 * IMPLEMENTED exercise's governingMetric must be in that set; the phantom metrics (CALIBRATION_ERROR /
 * FRUSTRATION_TOLERANCE_INDEX / REGULATION_STABILITY) must not; and the exercises that govern on them
 * (EM4/EM6/EM7/EM14) must stay out of the runtime registry until their metrics exist.
 */
class EmotionCatalogMetricProducibilityTest {

    private val engine = DefaultEmotionMetricsEngine()

    private fun goNoGo(outcome: EmotionTrialOutcome, rt: Long?, frameOpen: Boolean): ClassifiedEmotionTrial {
        val key = if (outcome == EmotionTrialOutcome.HIT || outcome == EmotionTrialOutcome.FALSE_ALARM) {
            EmotionResponseKey.PRIMARY
        } else {
            EmotionResponseKey.NONE
        }
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.AffectiveGoNoGo(frameOpen, "espera", true), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    private fun choice(outcome: EmotionTrialOutcome, rt: Long?): ClassifiedEmotionTrial {
        val key = when (outcome) {
            EmotionTrialOutcome.CORRECT -> EmotionResponseKey.LEFT
            EmotionTrialOutcome.INCORRECT -> EmotionResponseKey.RIGHT
            else -> EmotionResponseKey.NONE
        }
        val stim = EmotionStimulus(0, EmotionResponseKey.LEFT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.LEFT, 0.6), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    private fun classif(truth: EmotionCategory, chosen: EmotionCategory?): ClassifiedEmotionTrial {
        val outcome = when (chosen) {
            null -> EmotionTrialOutcome.NO_RESPONSE
            truth -> EmotionTrialOutcome.CORRECT
            else -> EmotionTrialOutcome.INCORRECT
        }
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", truth, false), 0, 1000)
        val respKey = if (chosen == null) EmotionResponseKey.NONE else EmotionResponseKey.CATEGORY_A
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, 600, respKey, chosen), outcome)
    }

    private fun producibleMetrics(): Set<EmotionMetricKey> {
        val goNoGoBlock = List(2) { goNoGo(EmotionTrialOutcome.FALSE_ALARM, 300, false) } +
            List(6) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, false) } +
            List(12) { goNoGo(EmotionTrialOutcome.HIT, 500, true) }
        val choiceBlock = buildList {
            repeat(4) {
                add(choice(EmotionTrialOutcome.CORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 500))
                add(choice(EmotionTrialOutcome.CORRECT, 700))
            }
        }
        val classifBlock = List(5) { classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE) } +
            List(5) { classif(EmotionCategory.THOUGHT, EmotionCategory.THOUGHT) }
        return engine.blockMetrics(goNoGoBlock).keys + engine.blockMetrics(choiceBlock).keys + engine.blockMetrics(classifBlock).keys
    }

    @Test
    fun implemented_emotion_exercises_have_producible_governing_metric() {
        val producible = producibleMetrics()
        EmotionExerciseCatalog.implemented.forEach { id ->
            val gov = EmotionExerciseCatalog.definition(id).governingMetric
            assertTrue(gov in producible, "$id governs on $gov which the engine does not produce (producible=$producible)")
        }
    }

    @Test
    fun registry_cannot_wire_exercise_with_unproducible_metric() {
        val producible = producibleMetrics()
        EmotionExerciseGenerators.registry.keys.forEach { id ->
            assertTrue(EmotionExerciseCatalog.definition(id).governingMetric in producible, "$id wired but its governing metric is not producible")
        }
    }

    @Test
    fun phantom_metrics_are_not_allowed_for_implemented_exercises() {
        val producible = producibleMetrics()
        val phantom = setOf(EmotionMetricKey.CALIBRATION_ERROR, EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX, EmotionMetricKey.REGULATION_STABILITY)
        phantom.forEach { assertTrue(it !in producible, "$it is not actually produced; it must not govern a shipped exercise") }
        EmotionExerciseCatalog.implemented.forEach { id ->
            assertTrue(EmotionExerciseCatalog.definition(id).governingMetric !in phantom, "$id governs on a phantom metric")
        }
    }

    @Test
    fun em4_em6_em7_em14_remain_not_wired_until_metrics_exist() {
        val phantomGoverned = setOf(
            EmotionExerciseId.EM4_CALIBRADOR, EmotionExerciseId.EM6_TOLERANCIA,
            EmotionExerciseId.EM7_REENTRADA, EmotionExerciseId.EM14_CONFIANZA,
        )
        phantomGoverned.forEach { assertTrue(it !in EmotionExerciseCatalog.implemented, "$it must not be wired: its metric is phantom") }
    }

    @Test
    fun catalog_designed_only_exercises_are_not_runtime_wired() {
        val designedOnly = EmotionExerciseCatalog.all.keys - EmotionExerciseCatalog.implemented.toSet()
        designedOnly.forEach { assertTrue(it !in EmotionExerciseGenerators.registry.keys, "$it is designed-only but appears in the runtime registry") }
    }

    /**
     * Ola 3 removed EM8, EM10 and EM11 from this pin, taking the menu from six entries to three.
     *
     * EM8 and EM10 went for one reason between them: each was another exercise`s host with a constant changed
     * (a higher no-go share, a fixed time-pressure floor), so it charged the user a second session to measure
     * the same thing less well. The R1 staircase makes a fixed no-go share strictly less adaptive than what it
     * copies, and the audit is explicit that adding hurry adds no evidence.
     *
     * EM11 went for a sharper reason. Its own governing metric could not be made confident by its own
     * 20-trial block (EmotionBlockSizeSufficiencyTest caught it), and the denser post-error practice it existed
     * for is what the fused exercise`s staircase now delivers to every user instead of only to those who
     * happened to err. The definition stays in the catalog; only the runnable path is gone.
     */
    @Test
    fun implementedEmotionExerciseIds_are_expected_mvp_set() {
        val expected = setOf(
            EmotionExerciseId.EM1_PAUSA, EmotionExerciseId.EM2_RECUPERACION, EmotionExerciseId.EM3_SEPARADOR,
        )
        assertTrue(EmotionExerciseCatalog.implemented.toSet() == expected, "MVP set changed without a CEO decision: ${EmotionExerciseCatalog.implemented}")
    }
}

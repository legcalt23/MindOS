@file:Suppress("MagicNumber", "MaxLineLength") // Difficulty levels and synthetic response timings are the fixtures.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.metrics.ClassifiedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionTrialClassifier
import com.mindos.app.features.mindboost.emotions.presentation.emotionTrialsPerBlock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ola 0 · S3 — a block must be long enough for the exercise's GOVERNING metric to be a confident reading.
 *
 * `EmotionCatalogMetricProducibilityTest` already proves the metrics engine CAN emit each governing key, but it
 * proves it over hand-built synthetic blocks. That is a strictly weaker claim than the one adaptation depends on:
 * a key the engine can emit in principle is useless if the block a real session actually generates never carries
 * enough of the right trials to make it confident. The go/no-go exercises were exactly that case — their no-go
 * share is a ratio of the block size, so a short block produced too few no-go trials at EVERY difficulty level and
 * their governing metric was permanently unconfident, which silently disabled the whole commission zone.
 *
 * So this test runs the REAL generator at the REAL production block size and the real metrics engine over it.
 */
class EmotionBlockSizeSufficiencyTest {

    private val engine = DefaultEmotionMetricsEngine()

    /** Difficulty extremes plus the default start: the sample floor has to be cleared across the whole staircase. */
    private val levels = listOf(0.0, 0.3, 0.5, 1.0)

    /**
     * A competent but human responder: correct except every fourth trial. The errors are not decoration — the
     * tolerance exercises govern on post-error recovery, which does not exist at all in a block without errors.
     */
    private fun respond(stimulus: EmotionStimulus, correct: Boolean): RecordedEmotionTrial =
        when (val payload = stimulus.payload) {
            is EmotionStimulusPayload.AffectiveGoNoGo -> {
                val presses = if (correct) payload.frameOpen else !payload.frameOpen
                RecordedEmotionTrial(
                    stimulus,
                    if (presses) RESPONSE_MS else null,
                    if (presses) EmotionResponseKey.PRIMARY else EmotionResponseKey.NONE,
                )
            }
            is EmotionStimulusPayload.TwoAltPerceptual -> {
                val chosen = if (correct) payload.correct else other(payload.correct)
                RecordedEmotionTrial(stimulus, RESPONSE_MS, chosen)
            }
            is EmotionStimulusPayload.Classification -> {
                val chosen = if (correct) payload.truth else EmotionCategory.entries.first { it != payload.truth }
                // The response KEY has to follow the chosen category, exactly as the session holder maps it. A
                // fixture that answered every classification trial with the same key would be flagged by the
                // anti-gaming guard, and the metric would come back unconfident for a reason that has nothing to
                // do with the block size this test is about.
                RecordedEmotionTrial(stimulus, RESPONSE_MS, keyFor(chosen), chosen)
            }
        }

    private fun keyFor(category: EmotionCategory): EmotionResponseKey = when (category) {
        EmotionCategory.INTERNAL_STATE -> EmotionResponseKey.CATEGORY_A
        EmotionCategory.THOUGHT -> EmotionResponseKey.CATEGORY_B
        EmotionCategory.IMPULSE -> EmotionResponseKey.CATEGORY_C
        EmotionCategory.FACT -> EmotionResponseKey.CATEGORY_D
    }

    private fun other(key: EmotionResponseKey): EmotionResponseKey =
        if (key == EmotionResponseKey.LEFT) EmotionResponseKey.RIGHT else EmotionResponseKey.LEFT

    /** Generates one real block of [id] at [level] with [trials] trials and returns the engine's metric set. */
    private fun metricsFor(id: EmotionExerciseId, level: Double, trials: Int) =
        engine.blockMetrics(
            EmotionExerciseGenerators.registry.getValue(id)
                .generate(
                    EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = level),
                    EmotionBlockSpec(targetTrialCount = trials, responseWindowMs = 2_500L, recentPhrases = emptySet()),
                    seed = 42L,
                )
                .mapIndexed { index, stimulus ->
                    val trial = respond(stimulus, correct = index % 4 != 1)
                    ClassifiedEmotionTrial(trial, EmotionTrialClassifier.classify(trial))
                },
        )

    @Test
    fun every_implemented_exercise_produces_a_confident_governing_metric_at_every_difficulty_level() {
        EmotionExerciseCatalog.implemented.forEach { id ->
            val governing = EmotionExerciseCatalog.definition(id).governingMetric
            val trials = emotionTrialsPerBlock(id, governing)
            levels.forEach { level ->
                val value = metricsFor(id, level, trials)[governing]
                assertTrue(
                    value != null && value.confident,
                    "$id adapts on $governing, but a real $trials-trial block at difficulty $level does not make it " +
                        "a confident reading (got $value) — its zone cannot adapt at all",
                )
            }
        }
    }

    /**
     * The discriminating half. Without it, the test above would keep passing if the block sizes were reverted and
     * something else happened to make the metrics confident — and it would never show that the block size is the
     * thing doing the work. Every implemented exercise must FAIL to reach confidence at the old base size.
     *
     * This used to name COMMISSION_RATE specifically, and its own vacuity guard is what retired that form: after
     * Ola 3 no implemented exercise governs on commission (EM1 moved to SSRT with R1, and EM8 — the other
     * commission entry — left the menu), so the guard went red exactly as it was built to. The assertion was
     * INVERTED rather than deleted, and it took two red runs to find the honest general form:
     *
     *  - it is scoped to the exercises whose block was ENLARGED, derived from the sizing rule itself rather than
     *    listed here. An exercise still at the base size is not being checked against a counterfactual — the base
     *    IS its size — so asserting it starves there would contradict the test above, which asserts the very same
     *    exercise reads confidently at the very same 10 trials. That contradiction is what EM3 surfaced;
     *  - one enlarged exercise is checked against something other than its governing metric. R1 sizes the
     *    inhibition exercise off the SMALLEST cell it must fill — the two tone cells of the affective SSRT delta —
     *    not off `HIT_RATE`, which 10 trials already makes confident. Asserting the governing metric there would
     *    have claimed a justification the block size never had. [SIZE_DRIVERS] names the reading each size was
     *    actually derived from, defaulting to the governing metric where they are the same thing.
     *
     * Reverting a size does not open a hole: the exercise drops out of this set, and the test above goes red for
     * it instead.
     *
     * `EXTENDED_TRIALS_PER_BLOCK` is deliberately left in `emotionTrialsPerBlock` even though no implemented
     * exercise reaches it today. It is a sizing RULE keyed by governing metric, not a symbol kept for a phase
     * that never came: deleting it would mean the next exercise to govern on commission silently gets the
     * 10-trial block that starved this zone in the first place.
     */
    @Test
    fun the_base_block_would_have_starved_every_exercise_whose_block_was_enlarged() {
        val enlarged = EmotionExerciseCatalog.implemented.filter { id ->
            emotionTrialsPerBlock(id, EmotionExerciseCatalog.definition(id).governingMetric) >
                BASE_TRIALS_THAT_WERE_TOO_SHORT
        }
        assertTrue(
            enlarged.isNotEmpty(),
            "no implemented exercise is sized above the base any more, so nothing here is a counterfactual and " +
                "this test is now vacuous",
        )

        enlarged.forEach { id ->
            val driver = SIZE_DRIVERS[id] ?: EmotionExerciseCatalog.definition(id).governingMetric
            val value = metricsFor(id, level = 0.3, trials = BASE_TRIALS_THAT_WERE_TOO_SHORT)[driver]
            assertFalse(
                value != null && value.confident,
                "$id reaches a confident $driver at only $BASE_TRIALS_THAT_WERE_TOO_SHORT trials, so the " +
                    "longer block this wave gave it is not what makes its zone work and the reason recorded " +
                    "for it is wrong",
            )
        }
    }

    private companion object {
        const val RESPONSE_MS = 600L

        /** The block size every exercise used before this wave — kept here only as the counterfactual. */
        const val BASE_TRIALS_THAT_WERE_TOO_SHORT = 10

        /**
         * The reading each block size was actually derived from, where that is NOT the governing metric.
         *
         * EM1 governs on `HIT_RATE`, but R1 sized its block off the affective SSRT delta, whose two tone cells
         * are the smallest thing it has to fill. `HIT_RATE` is confident at 10 trials, so checking the governing
         * metric there would assert a justification the block size never claimed.
         */
        val SIZE_DRIVERS = mapOf(
            EmotionExerciseId.EM1_PAUSA to EmotionMetricKey.AFFECTIVE_SSRT_DELTA,
        )
    }
}

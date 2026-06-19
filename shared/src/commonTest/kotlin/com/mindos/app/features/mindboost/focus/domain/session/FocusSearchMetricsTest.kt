package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler.RecordedTrial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * E7 Rastreo is a single-tap visual search (respond if the target is present, withhold if absent), so
 * it routes through the go/no-go `classify` path. This verifies the integration: the `setSize` marker
 * reaches the assembled trials and SET_SIZE_SLOPE (RT vs set size over hits) is computed — and is
 * positive when search RT grows with set size.
 */
class FocusSearchMetricsTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun searchStim(index: Int, setSize: Int, present: Boolean) = Stimulus(
        index = index,
        isTarget = present,
        correctResponseKey = if (present) ResponseKey.PRIMARY else null,
        payload = StimulusPayload.Search(targetPresent = present, setSize = setSize),
        interStimulusDelayMs = 600,
        responseWindowMs = 2000,
    )

    /** A present trial answered in [rtMs] → HIT carrying its set size. */
    private fun hit(index: Int, setSize: Int, rtMs: Long) =
        RecordedTrial(searchStim(index, setSize, present = true), rtMs, ResponseKey.PRIMARY)

    @Test
    fun setSizeSlope_isPositive_whenSearchSlowsWithMoreItems() {
        // RT grows with set size (≈ +25 ms/item): 4→300, 8→400, 16→600. Three hits each (9 points).
        val recorded = buildList {
            listOf(4 to 300L, 8 to 400L, 16 to 600L).forEach { (size, rt) ->
                repeat(3) { add(hit(size + size, size, rt)) }
            }
            // A couple of absent trials answered correctly (withheld) → CORRECT_REJECTION.
            repeat(2) { add(RecordedTrial(searchStim(900 + it, 8, present = false), null, null)) }
        }

        val block = assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E7_RASTREO,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

        // Single-tap scoring: present + responded = HIT, absent + withheld = CORRECT_REJECTION.
        assertTrue(block.trials.any { it.outcome == TrialOutcome.HIT })
        assertTrue(block.trials.any { it.outcome == TrialOutcome.CORRECT_REJECTION })

        val slope = block.metrics[MetricKey.SET_SIZE_SLOPE]
        assertNotNull(slope, "SET_SIZE_SLOPE must be computed for E7.")
        assertTrue(slope.confident, "9 hits across 3 set sizes clears the floor.")
        assertTrue(slope.value > 0.0, "Search RT grows with set size → positive slope (was ${slope.value}).")
    }
}

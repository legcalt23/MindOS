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
 * E11 Ancla is a LEFT/RIGHT main task with intruder trials to ignore, so it routes through
 * `classifyChoice`. This verifies the integration: the `duringIntrusion` marker reaches the assembled
 * trials and DISTRACTION_RESISTANCE_INDEX = accuracy(intruded) / accuracy(baseline).
 */
class FocusAnchorMetricsTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun anchorStim(index: Int, intruder: Boolean) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = ResponseKey.LEFT,
        payload = StimulusPayload.AnchorTask(mainResponse = ResponseKey.LEFT, intruderPresent = intruder),
        interStimulusDelayMs = 700,
        responseWindowMs = 1400,
    )

    /** Target is always LEFT; [key]=LEFT is a hit, [key]=RIGHT a wrong-key commission. */
    private fun rec(index: Int, intruder: Boolean, key: ResponseKey) =
        RecordedTrial(anchorStim(index, intruder), 500, key)

    @Test
    fun distractionResistance_isIntrudedAccuracyOverBaseline() {
        val recorded = buildList {
            repeat(6) { add(rec(size, intruder = false, key = ResponseKey.LEFT)) } // baseline → acc 1.0
            repeat(3) { add(rec(size, intruder = true, key = ResponseKey.LEFT)) } // intruded hit
            repeat(3) { add(rec(size, intruder = true, key = ResponseKey.RIGHT)) } // intruded wrong → 0.5
        }

        val block = assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E11_ANCLA,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

        // Choice scoring + the intrusion marker survives into the trials.
        assertEquals(TrialOutcome.HIT, block.trials.first { it.duringIntrusion == false }.outcome)
        assertTrue(block.trials.any { it.duringIntrusion == true && it.outcome == TrialOutcome.FALSE_ALARM })

        val resistance = block.metrics[MetricKey.DISTRACTION_RESISTANCE_INDEX]
        assertNotNull(resistance, "DISTRACTION_RESISTANCE_INDEX must be computed for E11.")
        assertTrue(resistance.confident)
        // accuracy(intruded) 0.5 / accuracy(baseline) 1.0 = 0.5 (the intruder hurts accuracy).
        assertEquals(0.5, resistance.value, absoluteTolerance = 1e-9)
    }
}

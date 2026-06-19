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
 * E13 Presión is a two-choice task scored by key (so it routes through `classifyChoice`), with calm
 * vs deadline trials interleaved. This verifies the integration path: the `underPressure` marker
 * survives into the assembled trials and PRESSURE_DELTA = accuracy(calm) − accuracy(pressure).
 */
class FocusPressureMetricsTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun pressureStim(index: Int, dir: ResponseKey, underPressure: Boolean) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = dir,
        payload = StimulusPayload.Pressure(
            correctResponse = dir,
            underPressure = underPressure,
            deadlineMs = if (underPressure) 450 else 0,
        ),
        interStimulusDelayMs = 700,
        responseWindowMs = 1400,
    )

    /** Always-LEFT target; [key]=LEFT is a hit, [key]=RIGHT a wrong-key commission. */
    private fun rec(index: Int, underPressure: Boolean, key: ResponseKey) = RecordedTrial(
        stimulus = pressureStim(index, ResponseKey.LEFT, underPressure),
        responseLatencyMs = if (underPressure) 350 else 500,
        responseKey = key,
    )

    @Test
    fun pressureDelta_isCalmAccuracyMinusPressuredAccuracy() {
        val recorded = buildList {
            repeat(6) { add(rec(size, underPressure = false, key = ResponseKey.LEFT)) } // calm HIT → acc 1.0
            repeat(3) { add(rec(size, underPressure = true, key = ResponseKey.LEFT)) } // pressure HIT
            repeat(3) { add(rec(size, underPressure = true, key = ResponseKey.RIGHT)) } // pressure wrong → 0.5
        }

        val block = assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E13_PRESION,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

        // Routed through the choice path: correct key = HIT, wrong key = FALSE_ALARM.
        assertEquals(TrialOutcome.HIT, block.trials.first { it.underPressure == false }.outcome)
        assertTrue(block.trials.any { it.underPressure == true && it.outcome == TrialOutcome.FALSE_ALARM })

        val delta = block.metrics[MetricKey.PRESSURE_DELTA]
        assertNotNull(delta, "PRESSURE_DELTA must be computed for E13.")
        assertTrue(delta.confident)
        // accuracy(calm) 1.0 − accuracy(pressure) 0.5 = 0.5 (pressure hurts accuracy).
        assertEquals(0.5, delta.value, absoluteTolerance = 1e-9)
    }
}

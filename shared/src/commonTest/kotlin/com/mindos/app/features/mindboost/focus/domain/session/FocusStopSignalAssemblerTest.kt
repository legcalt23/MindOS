package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
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
 * Verifies the stop-signal (E4) integration path through the assembler: stop trials must be scored
 * by inhibition (responded = FALSE_ALARM, withheld = CORRECT_REJECTION), the online SSD recorded by
 * the runtime must reach `TrialResult.stopSignalDelayMs`, and SSRT must compute from the assembled
 * block. Mirrors `E4FrenoGeneratorTest` but exercises the real `assembleBlock` wiring.
 */
class FocusStopSignalAssemblerTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun goStim(index: Int) = Stimulus(
        index = index,
        isTarget = true,
        requiresStop = false,
        correctResponseKey = ResponseKey.PRIMARY,
        payload = StimulusPayload.StopSignal(isStop = false, stopSignalDelayMs = null),
        interStimulusDelayMs = 700,
        responseWindowMs = 1000,
    )

    private fun stopStim(index: Int) = Stimulus(
        index = index,
        isTarget = true,
        requiresStop = true,
        correctResponseKey = ResponseKey.PRIMARY,
        payload = StimulusPayload.StopSignal(isStop = true, stopSignalDelayMs = null),
        interStimulusDelayMs = 700,
        responseWindowMs = 1000,
    )

    /** 10 go hits + 10 stop trials (SSD 200ms), [stopFailures] of which were responded to. */
    private fun block(stopFailures: Int): BlockResult {
        val recorded = buildList {
            repeat(10) { i -> add(RecordedTrial(goStim(size), (300 + i * 10).toLong(), ResponseKey.PRIMARY)) }
            repeat(10) { i ->
                val failed = i < stopFailures
                add(
                    RecordedTrial(
                        stimulus = stopStim(size),
                        responseLatencyMs = if (failed) 250 else null,
                        responseKey = if (failed) ResponseKey.PRIMARY else null,
                        stopSignalDelayMs = 200,
                    ),
                )
            }
        }
        return assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E4_FRENO,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )
    }

    @Test
    fun stopTrials_areClassifiedByInhibition_andCarryTheirSsd() {
        val b = block(stopFailures = 5)

        assertEquals(TrialOutcome.HIT, b.trials.first { !it.requiredStop }.outcome, "Go trial answered = HIT.")

        val failed = b.trials.first { it.requiredStop && it.outcome == TrialOutcome.FALSE_ALARM }
        assertNotNull(failed.reactionTime, "A failed stop keeps its go-RT for SSRT.")
        assertEquals(200L, failed.stopSignalDelayMs, "The online SSD must reach the trial result.")

        val success = b.trials.first { it.requiredStop && it.outcome == TrialOutcome.CORRECT_REJECTION }
        assertEquals(null, success.reactionTime, "A successful stop has no response.")
        assertEquals(200L, success.stopSignalDelayMs)
    }

    @Test
    fun ssrt_isComputable_andConfident_forABalancedStaircase() {
        val ssrt = block(stopFailures = 5).metrics[MetricKey.SSRT]
        assertNotNull(ssrt)
        assertTrue(ssrt.confident, "p(respond)=0.5 is the valid regime for SSRT.")
        assertTrue(ssrt.value > 0.0)
    }

    @Test
    fun ssrt_isNotConfident_whenStaircaseBrokeDown() {
        // p(respond|signal) = 0.1 → strategic slowing; SSRT must be flagged untrustworthy.
        assertEquals(false, block(stopFailures = 1).metrics[MetricKey.SSRT]?.confident)
    }
}

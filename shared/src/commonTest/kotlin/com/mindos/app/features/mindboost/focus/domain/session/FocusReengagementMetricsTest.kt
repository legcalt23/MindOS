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
 * E12 Re-enganche is a single-tap vigilance task whose governing metric is post-error recovery.
 * This pins that the assembler scores E12 with the go/no-go path AND that POST_ERROR_ACCURACY /
 * POST_ERROR_SLOWING fall out of the trial sequence for the E12 definition — the wiring needed to
 * expose E12 honestly.
 */
class FocusReengagementMetricsTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun reengage(index: Int, signal: Boolean) = Stimulus(
        index = index,
        isTarget = signal,
        correctResponseKey = if (signal) ResponseKey.PRIMARY else null,
        payload = StimulusPayload.Reengagement(isSignal = signal, afterProvokedError = false),
        interStimulusDelayMs = 1200,
        responseWindowMs = 1300,
    )

    /** A non-signal trial answered → FALSE_ALARM (the provoked error). */
    private fun falseAlarm(index: Int) = RecordedTrial(reengage(index, signal = false), 500, ResponseKey.PRIMARY)

    /** A signal trial answered in time → HIT (a recovery). */
    private fun hit(index: Int) = RecordedTrial(reengage(index, signal = true), 450, ResponseKey.PRIMARY)

    /** A signal trial with no response → MISS (a failed recovery). */
    private fun miss(index: Int) = RecordedTrial(reengage(index, signal = true), null, null)

    @Test
    fun e12_block_yields_post_error_accuracy_from_the_sequence() {
        // FA→HIT three times (recovered) then FA→MISS once (failed). 4 post-error trials, 3 correct.
        val recorded = listOf(
            falseAlarm(0), hit(1),
            falseAlarm(2), hit(3),
            falseAlarm(4), hit(5),
            falseAlarm(6), miss(7),
        )

        val block = assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E12_REENGANCHE,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

        // Outcomes scored via the go/no-go path (single-tap), not the choice path.
        assertEquals(TrialOutcome.FALSE_ALARM, block.trials[0].outcome)
        assertEquals(TrialOutcome.HIT, block.trials[1].outcome)
        assertEquals(TrialOutcome.MISS, block.trials[7].outcome)

        val accuracy = block.metrics[MetricKey.POST_ERROR_ACCURACY]
        assertNotNull(accuracy, "E12's governing metric must be computed.")
        assertTrue(accuracy.confident, "4 post-error trials clears the confidence floor.")
        assertEquals(4, accuracy.n)
        assertEquals(0.75, accuracy.value, absoluteTolerance = 1e-9) // 3 recovered of 4

        assertNotNull(block.metrics[MetricKey.POST_ERROR_SLOWING], "Post-error slowing is reported too.")
    }
}

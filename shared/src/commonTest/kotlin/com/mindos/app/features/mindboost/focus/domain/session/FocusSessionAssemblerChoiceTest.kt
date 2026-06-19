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
 * Verifies the two-choice (E2/E5) path: the assembler must score trials by WHICH key was pressed
 * (via `classifyChoice`), not just by responding — and the congruency-effect metric must fall out.
 * Guards the wiring added when the choice response modality was integrated.
 */
class FocusSessionAssemblerChoiceTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun flanker(index: Int, dir: ResponseKey, congruent: Boolean) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = dir,
        payload = StimulusPayload.Flanker(centerDirection = dir, congruent = congruent, flankerCount = 4),
        interStimulusDelayMs = 600,
        responseWindowMs = 1500,
    )

    private fun conflict(index: Int, dir: ResponseKey, congruent: Boolean) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = dir,
        payload = StimulusPayload.Conflict(relevantResponse = dir, congruent = congruent),
        interStimulusDelayMs = 600,
        responseWindowMs = 1600,
    )

    private fun assembleE2(recorded: List<RecordedTrial>) = assembler.assembleBlock(
        blockId = BlockId("t:b0"),
        definition = ExerciseCatalog.E2_FILTRO,
        mode = Mode.TRAINING,
        difficultyStart = difficulty,
        difficultyEnd = difficulty,
        thresholds = thresholds,
        recorded = recorded,
        sessionIdPrefix = "t",
        epochMillis = 0,
    )

    @Test
    fun correctKeyChoices_areHits_andYieldACongruencyEffect() {
        val recorded = buildList {
            repeat(6) { i ->
                val dir = if (i % 2 == 0) ResponseKey.LEFT else ResponseKey.RIGHT
                add(RecordedTrial(flanker(size, dir, congruent = true), 400, dir)) // congruent, fast
            }
            repeat(6) { i ->
                val dir = if (i % 2 == 0) ResponseKey.LEFT else ResponseKey.RIGHT
                add(RecordedTrial(flanker(size, dir, congruent = false), 560, dir)) // incongruent, slow
            }
        }

        val block = assembleE2(recorded)

        assertTrue(block.trials.all { it.outcome == TrialOutcome.HIT }, "All correct-key choices are hits.")
        val effect = block.metrics[MetricKey.CONGRUENCY_EFFECT]
        assertNotNull(effect, "Congruency effect must be computed for a choice paradigm.")
        assertTrue(effect.confident)
        // RT(incongruent=560) − RT(congruent=400) = 160 ms of interference.
        assertEquals(160.0, effect.value, absoluteTolerance = 1.0)
    }

    @Test
    fun wrongKeyIsFalseAlarm_andNoResponseIsMiss() {
        val recorded = listOf(
            RecordedTrial(conflict(0, ResponseKey.LEFT, congruent = true), 420, ResponseKey.LEFT), // correct → HIT
            RecordedTrial(conflict(1, ResponseKey.LEFT, congruent = false), 480, ResponseKey.RIGHT), // wrong → FA
            RecordedTrial(conflict(2, ResponseKey.RIGHT, congruent = true), null, null), // no response → MISS
        )

        val block = assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E5_CONFLICTO,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

        assertEquals(TrialOutcome.HIT, block.trials[0].outcome)
        assertEquals(TrialOutcome.FALSE_ALARM, block.trials[1].outcome)
        assertEquals(TrialOutcome.MISS, block.trials[2].outcome)
    }
}

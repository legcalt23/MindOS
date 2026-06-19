package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E6CambioGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TaskRule
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler.RecordedTrial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * E6 Cambio is a real task switch: the correct side is the active rule applied to the stimulus, and
 * SWITCH_COST = RT(switch) − RT(repeat). This pins both the generator's derivation (no "show the
 * answer" shortcut) and the integration path through the assembler.
 */
class FocusTaskSwitchTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    @Test
    fun generator_derivesCorrectSideFromActiveRule() {
        val stimuli = E6CambioGenerator().generate(difficulty, BlockSpec(60, Mode.TRAINING), seed = 11)
        stimuli.forEach { stimulus ->
            val p = stimulus.payload as StimulusPayload.TaskSwitch
            val expected = when (p.rule) {
                TaskRule.RULE_A -> if (p.colorIsRed) ResponseKey.LEFT else ResponseKey.RIGHT
                TaskRule.RULE_B -> if (p.shapeIsCircle) ResponseKey.LEFT else ResponseKey.RIGHT
            }
            assertEquals(expected, p.correctResponse, "Response must follow the ACTIVE rule.")
            assertEquals(expected, stimulus.correctResponseKey)
        }
    }

    private fun taskStim(index: Int, isSwitch: Boolean, dir: ResponseKey) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = dir,
        payload = StimulusPayload.TaskSwitch(
            rule = TaskRule.RULE_A,
            isSwitch = isSwitch,
            correctResponse = dir,
            colorIsRed = dir == ResponseKey.LEFT,
            shapeIsCircle = true,
        ),
        interStimulusDelayMs = 600,
        responseWindowMs = 1500,
    )

    /** A correct-key response to a switch/repeat trial → HIT carrying its RT and isSwitch marker. */
    private fun hit(index: Int, isSwitch: Boolean, dir: ResponseKey, rt: Long) =
        RecordedTrial(taskStim(index, isSwitch, dir), rt, dir)

    @Test
    fun switchCost_isSwitchRtMinusRepeatRt() {
        val recorded = buildList {
            repeat(4) { add(hit(size, isSwitch = true, ResponseKey.LEFT, 600)) } // switch HITs → median 600
            repeat(4) { add(hit(size, isSwitch = false, ResponseKey.RIGHT, 450)) } // repeat HITs → median 450
        }

        val block = assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = ExerciseCatalog.E6_CAMBIO,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

        val cost = block.metrics[MetricKey.SWITCH_COST]
        assertNotNull(cost, "SWITCH_COST must be computed for E6.")
        assertTrue(cost.confident)
        // median switch RT (600) − median repeat RT (450) = 150 ms.
        assertEquals(150.0, cost.value, absoluteTolerance = 1e-9)
    }
}

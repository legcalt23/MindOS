package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
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
 * Integration checks for the three movement/stream paradigms (E8 dual-task, E9 MOT, E10 RSVP). They
 * all route through the single-tap `classify` path; this verifies their markers/outcomes reach the
 * assembled block: E8's dual-task cost, E9's probe classification, E10's detection.
 */
class FocusAdvancedParadigmsAssemblerTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val thresholds = UserCognitiveProfile(userId = "t").thresholds
    private val difficulty = DifficultyVector.EASIEST

    private fun block(definition: ExerciseDefinition, recorded: List<RecordedTrial>) =
        assembler.assembleBlock(
            blockId = BlockId("t:b0"),
            definition = definition,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "t",
            epochMillis = 0,
        )

    // ── E8 Dúo ───────────────────────────────────────────────────────────────
    private fun dualStim(index: Int, dual: Boolean) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = ResponseKey.PRIMARY,
        payload = StimulusPayload.DualTask(dual = dual, go = true),
        interStimulusDelayMs = 700,
        responseWindowMs = 1100,
    )

    private fun dualRec(index: Int, dual: Boolean, responded: Boolean) = RecordedTrial(
        stimulus = dualStim(index, dual),
        responseLatencyMs = if (responded) 400L else null,
        responseKey = if (responded) ResponseKey.PRIMARY else null,
    )

    @Test
    fun e8_dualTaskCost_isSingleMinusDualAccuracy() {
        val recorded = buildList {
            repeat(6) { add(dualRec(size, dual = false, responded = true)) } // single → acc 1.0
            repeat(3) { add(dualRec(size, dual = true, responded = true)) } // dual hit
            repeat(3) { add(dualRec(size, dual = true, responded = false)) } // dual miss → acc 0.5
        }
        val cost = block(ExerciseCatalog.E8_DUO, recorded).metrics[MetricKey.DUAL_TASK_COST]
        assertNotNull(cost, "DUAL_TASK_COST must be computed for E8.")
        assertTrue(cost.confident)
        assertEquals(0.5, cost.value, absoluteTolerance = 1e-9) // 1.0 single − 0.5 dual
    }

    // ── E9 Enjambre (MOT) ──────────────────────────────────────────────────────
    private fun motStim(index: Int, probedIsTarget: Boolean) = Stimulus(
        index = index,
        isTarget = probedIsTarget,
        correctResponseKey = if (probedIsTarget) ResponseKey.PRIMARY else null,
        payload = StimulusPayload.MultiObjectTracking(
            objectCount = 8,
            targetCount = 3,
            targetIndices = setOf(0, 1, 2),
            speed = 1.5,
        ),
        interStimulusDelayMs = 900,
        responseWindowMs = 2000,
    )

    @Test
    fun e9_probeIsScoredAsGoNoGo() {
        val trials = block(
            ExerciseCatalog.E9_ENJAMBRE,
            listOf(
                RecordedTrial(motStim(0, probedIsTarget = true), 500L, ResponseKey.PRIMARY), // HIT
                RecordedTrial(motStim(1, probedIsTarget = false), 500L, ResponseKey.PRIMARY), // FALSE_ALARM
                RecordedTrial(motStim(2, probedIsTarget = false), null, null), // CORRECT_REJECTION
            ),
        ).trials
        assertEquals(TrialOutcome.HIT, trials[0].outcome)
        assertEquals(TrialOutcome.FALSE_ALARM, trials[1].outcome)
        assertEquals(TrialOutcome.CORRECT_REJECTION, trials[2].outcome)
    }

    // ── E10 Flujo (RSVP) ───────────────────────────────────────────────────────
    private fun rsvpStim(index: Int) = Stimulus(
        index = index,
        isTarget = true,
        correctResponseKey = ResponseKey.PRIMARY,
        payload = StimulusPayload.RsvpStream(length = 16, t1Index = 4, t2Index = 7),
        interStimulusDelayMs = 800,
        responseWindowMs = 2000,
    )

    @Test
    fun e10_detectionIsHitOnResponseMissOtherwise() {
        val trials = block(
            ExerciseCatalog.E10_FLUJO,
            listOf(
                RecordedTrial(rsvpStim(0), 600L, ResponseKey.PRIMARY), // detected → HIT
                RecordedTrial(rsvpStim(1), null, null), // missed → MISS
            ),
        ).trials
        assertEquals(TrialOutcome.HIT, trials[0].outcome)
        assertEquals(TrialOutcome.MISS, trials[1].outcome)
    }
}

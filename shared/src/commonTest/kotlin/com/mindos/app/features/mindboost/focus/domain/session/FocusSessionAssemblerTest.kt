package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E1VigiaGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.valueOf
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FocusSessionAssemblerTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val e1 = ExerciseCatalog.E1_VIGIA

    /** A perfect player: responds at 300ms to every target, ignores every non-target. */
    private fun perfectBlock(seed: Long, count: Int = 40) = run {
        val stimuli = E1VigiaGenerator().generate(
            DifficultyVector.EASIEST,
            BlockSpec(targetTrialCount = count, mode = Mode.TRAINING),
            seed,
        )
        val recorded = stimuli.map {
            FocusSessionAssembler.RecordedTrial(it, if (it.isTarget) 300L else null)
        }
        assembler.assembleBlock(
            blockId = BlockId("b0"),
            definition = e1,
            mode = Mode.TRAINING,
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            thresholds = IndividualThresholds(),
            recorded = recorded,
            sessionIdPrefix = "user",
            epochMillis = 5_000,
        )
    }

    @Test
    fun perfectPlay_yieldsFullAccuracy_andClassifiesOutcomes() {
        val block = perfectBlock(seed = 11)
        assertEquals(1.0, block.metrics.valueOf(MetricKey.ACCURACY), absoluteTolerance = 1e-9)
        val targets = block.trials.count { it.stimulusIsTarget }
        assertEquals(targets, block.trials.count { it.outcome == TrialOutcome.HIT })
        assertTrue(block.trials.none { it.outcome == TrialOutcome.FALSE_ALARM })
    }

    @Test
    fun assembleSession_buildsSessionMetricsAndHonestReport() {
        val block = perfectBlock(seed = 3, count = 50)
        val session = assembler.assembleSession(
            sessionId = SessionId("user:5000"),
            mode = Mode.TRAINING,
            definition = e1,
            blocks = listOf(block),
            startedAtMs = 0,
            endedAtMs = 5_000,
            subjectiveState = null,
        )
        assertTrue(session.sessionMetrics.containsKey(MetricKey.ACCURACY))

        val report = assembler.buildReport(session)
        assertTrue(report.headline.isNotEmpty())
        assertTrue(report.honestNote.isNotBlank())
        // The honest note must never claim far transfer.
        val lower = report.honestNote.lowercase()
        assertTrue(("intelig" + "encia") in lower || "no demuestra" in lower || "no una medida" in lower)
    }

    @Test
    fun shortSession_reportIsFlaggedAsInsufficient() {
        // An empty/near-empty block has no confident metrics → the report must
        // explicitly say the session is too short to conclude anything.
        val rebuilt = assembler.assembleBlock(
            blockId = BlockId("b0"),
            definition = e1,
            mode = Mode.TRAINING,
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            thresholds = IndividualThresholds(),
            recorded = emptyList(),
            sessionIdPrefix = "user",
            epochMillis = 5_000,
        )
        val session = assembler.assembleSession(
            sessionId = SessionId("user:6000"),
            mode = Mode.TRAINING,
            definition = e1,
            blocks = listOf(rebuilt),
            startedAtMs = 0,
            endedAtMs = 6_000,
            subjectiveState = null,
        )
        val report = assembler.buildReport(session)
        assertTrue(report.honestNote.contains("demasiado corta"))
    }
}

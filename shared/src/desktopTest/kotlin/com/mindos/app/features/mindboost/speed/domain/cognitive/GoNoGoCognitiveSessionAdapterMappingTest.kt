package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.SessionMetric
import com.mindos.app.features.mindboost.core.session.cognitive.TrialOutcomeKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4 · The E8 · Go/No-Go → CognitiveSessionRun mapping table + honesty invariants: every resolved
 * outcome maps to exactly one honest kind, a correct inhibition NEVER becomes a fabricated RT tap, the counts
 * partition the completed trials, and a lost tap is a flagged/counted signal — never an invented trial.
 */
class GoNoGoCognitiveSessionAdapterMappingTest {

    private fun partitionOf(run: CognitiveSessionRun): Map<TrialOutcomeKind, Int> =
        run.trials.outcomes.associate { it.kind to it.count }

    @Test
    fun `GO hit maps to ACTIVE_CORRECT`() {
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goHit(0))))
        assertEquals(1, partitionOf(run)[TrialOutcomeKind.ACTIVE_CORRECT])
    }

    @Test
    fun `GO omission maps to OMISSION`() {
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goOmission(0))))
        assertEquals(1, partitionOf(run)[TrialOutcomeKind.OMISSION])
    }

    @Test
    fun `NO-GO correct inhibition maps to CORRECT_INHIBITION`() {
        val run = GoNoGoAdapterFixtures.run(
            GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.noGoInhibition(0))),
        )
        assertEquals(1, partitionOf(run)[TrialOutcomeKind.CORRECT_INHIBITION])
        assertNull(partitionOf(run)[TrialOutcomeKind.ACTIVE_CORRECT], "inhibition is not an active correct tap")
    }

    @Test
    fun `NO-GO commission maps to COMMISSION`() {
        val run = GoNoGoAdapterFixtures.run(
            GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.noGoCommission(0))),
        )
        assertEquals(1, partitionOf(run)[TrialOutcomeKind.COMMISSION])
    }

    @Test
    fun `an anticipation maps to ANTICIPATION`() {
        val run = GoNoGoAdapterFixtures.run(
            GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goAnticipation(0))),
        )
        assertEquals(1, partitionOf(run)[TrialOutcomeKind.ANTICIPATION])
    }

    @Test
    fun `a late response maps to LATE_RESPONSE`() {
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goLate(0))))
        assertEquals(1, partitionOf(run)[TrialOutcomeKind.LATE_RESPONSE])
    }

    @Test
    fun `a NO-GO trial that is both premature and a post-onset tap is counted once as ANTICIPATION`() {
        val run = GoNoGoAdapterFixtures.run(
            GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.noGoAnticipationAndCommission(0))),
        )
        val partition = partitionOf(run)
        assertEquals(1, partition[TrialOutcomeKind.ANTICIPATION])
        assertNull(partition[TrialOutcomeKind.COMMISSION], "the overlapping trial must NOT be double-counted")
        assertEquals(1, run.trials.completed)
        assertEquals(1, run.trials.outcomes.sumOf { it.count })
    }

    @Test
    fun `the outcome counts partition the completed trials with no overlap`() {
        val outcomes = listOf(
            GoNoGoAdapterFixtures.goHit(0),
            GoNoGoAdapterFixtures.goOmission(1),
            GoNoGoAdapterFixtures.goAnticipation(2),
            GoNoGoAdapterFixtures.goLate(3),
            GoNoGoAdapterFixtures.noGoInhibition(4),
            GoNoGoAdapterFixtures.noGoCommission(5),
        )
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(outcomes))
        assertEquals(6, run.trials.completed)
        assertEquals(run.trials.completed, run.trials.outcomes.sumOf { it.count })
        assertEquals(
            mapOf(
                TrialOutcomeKind.ACTIVE_CORRECT to 1,
                TrialOutcomeKind.OMISSION to 1,
                TrialOutcomeKind.ANTICIPATION to 1,
                TrialOutcomeKind.LATE_RESPONSE to 1,
                TrialOutcomeKind.CORRECT_INHIBITION to 1,
                TrialOutcomeKind.COMMISSION to 1,
            ),
            partitionOf(run),
        )
    }

    @Test
    fun `answerSummary is consistent with the outcome partition`() {
        val outcomes = listOf(
            GoNoGoAdapterFixtures.goHit(0),
            GoNoGoAdapterFixtures.goOmission(1),
            GoNoGoAdapterFixtures.goAnticipation(2),
            GoNoGoAdapterFixtures.goLate(3),
            GoNoGoAdapterFixtures.noGoInhibition(4),
            GoNoGoAdapterFixtures.noGoCommission(5),
        )
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(outcomes))
        val answers = run.answerSummary
        assertEquals(6, answers.total)
        assertEquals(2, answers.correct, "ACTIVE_CORRECT + CORRECT_INHIBITION")
        assertEquals(1, answers.omissions)
        assertEquals(3, answers.incorrect, "ANTICIPATION + LATE_RESPONSE + COMMISSION")
        assertEquals(answers.total, answers.correct + answers.incorrect + answers.omissions)
    }

    @Test
    fun `a correct inhibition never fabricates a reaction time and never becomes an active correct tap`() {
        // A run of only NO-GO inhibitions + GO omissions: there is NO correct-GO response, so no RT can exist.
        val outcomes = (0 until 6).map { GoNoGoAdapterFixtures.noGoInhibition(it) } +
            (0 until 6).map { GoNoGoAdapterFixtures.goOmission(100 + it) }
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(outcomes))
        val partition = partitionOf(run)
        assertEquals(6, partition[TrialOutcomeKind.CORRECT_INHIBITION])
        assertNull(partition[TrialOutcomeKind.ACTIVE_CORRECT], "inhibition must not be turned into an active correct")
        val m = run.metrics
        assertTrue(m.reactionTimeMedianMs is SessionMetric.Unavailable, "no correct-GO RT ⇒ RT is Unavailable")
        assertTrue(m.reactionTimeMeanMs is SessionMetric.Unavailable)
        assertTrue(m.reactionTimeMinMs is SessionMetric.Unavailable)
        assertTrue(m.reactionTimeMaxMs is SessionMetric.Unavailable)
    }

    @Test
    fun `a lost onset tap is a flagged, counted signal - never an invented trial`() {
        // CONTRACT (do not regress into double-counting): a `missingOnsetTaps` tap has NO onset, so it is NOT a
        // member of the trial universe — it is counted ONLY as an anomaly flag + the payload `lostInputTaps`
        // field, and MUST NOT also appear as a `LOST_INPUT` OutcomeCount. A `LOST_INPUT` partition entry is
        // reserved for a future paradigm where a lost input genuinely belongs to a real trial.
        val artifacts = GoNoGoAdapterFixtures.artifacts(GoNoGoAdapterFixtures.sufficientCleanOutcomes(), missingOnsetTaps = 1)
        val run = GoNoGoAdapterFixtures.run(artifacts)
        assertTrue("missingOnsetTaps" in run.anomalyFlags, "a lost tap must raise an anomaly flag")
        assertNull(partitionOf(run)[TrialOutcomeKind.LOST_INPUT], "a lost tap is NOT a completed trial")
        assertEquals(artifacts.outcomes.size, run.trials.completed, "completed counts only resolved trials")
        val payload = GoNoGoInhibitionPayloadCodec.decode(assertNotNull(run.exerciseMetrics))
        assertEquals(1, payload.lostInputTaps)
    }

    @Test
    fun `the score exposes only a normalized value or null, never a raw, delta or XP`() {
        val sufficient = GoNoGoAdapterFixtures.run()
        assertNotNull(sufficient.score.normalized)
        assertTrue(sufficient.score.normalized!! in 0.0..1.0)

        val insufficient = GoNoGoAdapterFixtures.run(
            GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goHit(0))),
        )
        assertNull(insufficient.score.normalized, "an insufficient sample is null, never a fabricated 0")
    }

    @Test
    fun `the exercise payload validates and decodes through its codec`() {
        val run = GoNoGoAdapterFixtures.run()
        val payload = assertNotNull(run.exerciseMetrics)
        assertTrue(GoNoGoInhibitionPayloadCodec.validate(payload))
        val decoded = GoNoGoInhibitionPayloadCodec.decode(payload)
        assertEquals(8, decoded.goTrials)
        assertEquals(5, decoded.noGoTrials)
        assertEquals(5, decoded.noGoCorrect)
    }

    @Test
    fun `the run is a SPEED-block, fixed-count session with an honest null selected duration`() {
        val run = GoNoGoAdapterFixtures.run()
        assertEquals(ScoreModuleId.SPEED, run.blockId)
        assertNull(run.durationSelectedMs, "E8 has no user-selected duration; null is honest, not anomalous")
    }
}

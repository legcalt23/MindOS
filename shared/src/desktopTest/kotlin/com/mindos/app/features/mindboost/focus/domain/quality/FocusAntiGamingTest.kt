package com.mindos.app.features.mindboost.focus.domain.quality

import com.mindos.app.features.mindboost.core.antigaming.GamingAction
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.exercise.E10FlujoGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * R15 · red-team for [FocusAntiGaming]. A detector counts only if THREE things hold, so all three are here:
 * it has a live call-site in the gate it claims to protect (`FocusSessionAssembler.admit`, pinned by
 * `FocusSessionAdmissionTest`), a synthetic adversary fires it, and a realistic honest player fires NOTHING.
 *
 * The honest control is the load-bearing half: a gate that fires on everyone is worse than no gate, because
 * its verdict withholds real sessions while reading as protection.
 */
class FocusAntiGamingTest {

    // ── Fixtures: an E1-shaped vigilance schedule, targets rare, built trial by trial ────────────────────

    /** [hitRate] of the targets answered, [falseAlarms] responses on non-targets, [rtMs] on every response. */
    private fun vigilanceBlock(
        trials: Int = TRIALS,
        targetEvery: Int = TARGET_EVERY,
        hitRate: Double = 1.0,
        falseAlarms: Int = 0,
        rtMs: Long = HONEST_RT_MS,
        anticipations: Int = 0,
    ): List<TrialResult> {
        val targets = (0 until trials).filter { it % targetEvery == 0 }
        val answered = targets.take((targets.size * hitRate).toInt()).toSet()
        val nonTargets = (0 until trials).filterNot { it % targetEvery == 0 }
        val commissions = nonTargets.take(falseAlarms).toSet()
        val premature = nonTargets.takeLast(anticipations).toSet()
        return (0 until trials).map { index ->
            val isTarget = index % targetEvery == 0
            val outcome = when {
                index in premature -> TrialOutcome.ANTICIPATION
                isTarget && index in answered -> TrialOutcome.HIT
                isTarget -> TrialOutcome.MISS
                index in commissions -> TrialOutcome.FALSE_ALARM
                else -> TrialOutcome.CORRECT_REJECTION
            }
            val rt = if (outcome == TrialOutcome.HIT || outcome == TrialOutcome.FALSE_ALARM) rtMs else null
            FocusTestFixtures.trial(index = index, outcome = outcome, rtMs = rt, isTarget = isTarget)
        }
    }

    // ── The honest control ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun a_realistic_honest_player_fires_no_detector_at_all() {
        // Not a perfect player: one target missed and one commission, at an ordinary reaction time.
        val honest = vigilanceBlock(hitRate = 0.9, falseAlarms = 1)

        val verdict = FocusAntiGaming.evaluate(honest)

        assertEquals(GamingAction.NONE, verdict.action, "fired: ${verdict.firedDetectors}")
        assertTrue(verdict.firedDetectors.isEmpty(), "an honest block must fire nothing")
        // Anti-vacuity: the block really is above the blind ceiling, so the gate had something to decide.
        val telemetry = FocusAntiGaming.telemetryFrom(honest)
        assertTrue(
            telemetry.accuracy > telemetry.chanceAccuracy,
            "fixture is not above the blind ceiling, so this control proves nothing",
        )
    }

    // ── Adversaries, one per wired detector ─────────────────────────────────────────────────────────────

    @Test
    fun a_blind_responder_on_a_target_rare_schedule_cannot_be_read_as_a_measure() {
        // Answers almost nothing: every non-target scores as a correct rejection, which is the blind ceiling.
        val blind = vigilanceBlock(hitRate = 0.0, falseAlarms = 2)

        val verdict = FocusAntiGaming.evaluate(blind)

        assertEquals(GamingAction.INVALIDATE, verdict.action)
        assertTrue(
            BlindStrategyCeilingDetector.ID in verdict.firedDetectors,
            "fired: ${verdict.firedDetectors}",
        )
    }

    @Test
    fun anticipation_spam_withholds_the_session() {
        // Above the physiological floor rate, and deliberately ABOVE the blind ceiling so the discrimination
        // gate stays idle: this asserts D4 on its own, not a verdict two detectors could explain.
        val spam = vigilanceBlock(anticipations = (TRIALS * ANTICIPATION_SHARE).toInt())

        val verdict = FocusAntiGaming.evaluate(spam)

        assertEquals(GamingAction.WITHHOLD_PROFILE, verdict.action)
        assertEquals(listOf(D4_TOO_FAST), verdict.firedDetectors, "only the anticipation gate should fire")
    }

    @Test
    fun answering_at_the_blind_ceiling_faster_than_the_chance_floor_also_trips_the_speed_gate() {
        val fastBlind = vigilanceBlock(hitRate = 0.0, falseAlarms = 2, rtMs = FAST_RT_MS)

        val verdict = FocusAntiGaming.evaluate(fastBlind)

        assertEquals(GamingAction.INVALIDATE, verdict.action)
        assertTrue(D3_SPEED_HACK in verdict.firedDetectors, "fired: ${verdict.firedDetectors}")
    }

    // ── The two ways this gate refuses to accuse ────────────────────────────────────────────────────────

    @Test
    fun below_the_minimum_trial_count_even_the_worst_pattern_is_clean() {
        val tiny = vigilanceBlock(trials = FocusAntiGaming.MIN_TRIALS_FOR_DETECTION - 1, hitRate = 0.0)

        val verdict = FocusAntiGaming.evaluate(tiny)

        assertEquals(GamingAction.NONE, verdict.action, "a tiny sample is not evidence of anything")
    }

    /**
     * The declared ceiling, asked of the REAL producer rather than a fixture shaped to agree with it: E10
     * serves no non-target trial (`MB-FOCUS-0138`), so its blind ceiling is degenerate and the discrimination
     * gate must IDLE. Tapping every trial there stays clean — this gate does not close H-07, and only the
     * paradigm redesign can. If E10 ever gains non-target trials this test goes RED and the claim must be
     * re-measured instead of quietly surviving.
     */
    @Test
    fun a_degenerate_schedule_idles_instead_of_accusing_everyone() {
        val spec = BlockSpec(targetTrialCount = TRIALS, mode = Mode.TRAINING)
        val stimuli = E10FlujoGenerator().generate(DifficultyVector.EASIEST, spec, SEED)
        assertTrue(stimuli.isNotEmpty(), "anti-vacuity: the producer served no trial")
        val tapEverything = stimuli.mapIndexed { index, stimulus ->
            FocusTestFixtures.trial(
                index = index,
                outcome = TrialOutcome.HIT,
                rtMs = HONEST_RT_MS,
                isTarget = stimulus.isTarget,
                exerciseId = ExerciseId.E10_FLUJO,
            )
        }

        assertEquals(
            FocusAntiGaming.DEGENERATE_CEILING,
            FocusAntiGaming.blindCeiling(tapEverything),
            "E10 stopped being degenerate: re-measure the declared ceiling",
        )
        assertEquals(GamingAction.NONE, FocusAntiGaming.evaluate(tapEverything).action)
    }

    private companion object {
        const val TRIALS = 55
        const val TARGET_EVERY = 5 // a 20% target rate, so the blind ceiling is 0.8 and there IS a contrast
        const val HONEST_RT_MS = 420L
        const val FAST_RT_MS = 200L // above the 150 ms anticipation floor, below the 250 ms chance floor
        const val ANTICIPATION_SHARE = 0.4 // above the 30% physiological floor rate
        const val SEED = 4242L
        const val D3_SPEED_HACK = "D3_SPEED_ACCURACY_HACK"
        const val D4_TOO_FAST = "D4_TOO_FAST"
    }
}

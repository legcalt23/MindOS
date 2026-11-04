package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveConfig
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TrialMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * R14 (60d bucket) - pins, against the REAL chain, what a user who does not perform the task scores on
 * E9, E10 and E8. Every number here is asked of the producer and then pushed through
 * [DefaultMetricsEngine], never computed by hand: a guard that hands its own ratio to a threshold
 * asserts nothing about the system (the 2026-09-06 lesson).
 *
 * E9 draws its probe per trial, so the score of a non-performer has a TAIL, not a ceiling: at 55 trials
 * the spread is wide enough that a single seed would be a knife-edge pin. It is therefore pinned by
 * DISTRIBUTION - median plus the share of seeds that cross - following the 2026-08-19 precedent. E8 and
 * E10 need no such handling: E8 draws its two cells from an exact quota and E10 has no second class of
 * trial at all, so both are deterministic.
 *
 * These are pins of a LIMITATION, not of a desired behaviour. Each carries a message saying which way it
 * breaks when the limitation is lifted: on that day the assertion must be INVERTED with the new
 * evidence, never deleted. That is the discipline the 2026-08-19 entry sets out.
 *
 * Owners: MB-FOCUS-0137 (E9), MB-FOCUS-0138 (E10), MB-FOCUS-0141 (E8).
 */
class FocusBlindStrategyTest {

    private val engine = DefaultMetricsEngine()
    private val thresholds = IndividualThresholds()
    private val config = AdaptiveConfig()

    /** The smallest block a real session serves, so the pins sit on the honest worst case. */
    private val spec = BlockSpec(targetTrialCount = 55, mode = Mode.TRAINING)

    private fun accuracy(trials: List<TrialResult>, definition: ExerciseDefinition): MetricValue {
        val metrics = engine.blockMetrics(trials, definition, thresholds)
        return assertNotNull(metrics[MetricKey.ACCURACY], "the engine published no ACCURACY at all")
    }

    private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

    /**
     * E9 with a user who never tracks anything: withholds on every probe except the first [taps] targets.
     * `taps = 0` is total withholding; `taps = 1` is the token response.
     */
    private fun e9Block(perceptualLoad: Double, taps: Int, seed: Long): List<TrialResult> {
        val difficulty = DifficultyVector.EASIEST
            .adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel(perceptualLoad))
        val stimuli = E9EnjambreGenerator().generate(difficulty, spec, seed)
        assertEquals(spec.targetTrialCount, stimuli.size, "anti-vacuity: the producer served no block")
        assertTrue(stimuli.any { it.isTarget }, "anti-vacuity: no probed target in the block")
        assertTrue(stimuli.any { !it.isTarget }, "anti-vacuity: no probed non-target in the block")
        var spent = 0
        return stimuli.map { stimulus ->
            val tap = stimulus.isTarget && spent < taps
            if (tap) spent++
            val outcome = when {
                tap -> TrialOutcome.HIT
                stimulus.isTarget -> TrialOutcome.MISS
                else -> TrialOutcome.CORRECT_REJECTION
            }
            FocusTestFixtures.trial(
                index = stimulus.index,
                outcome = outcome,
                rtMs = if (tap) TAP_RT_MS else null,
                isTarget = stimulus.isTarget,
                exerciseId = ExerciseId.E9_ENJAMBRE,
            )
        }
    }

    private fun e9TokenScores(perceptualLoad: Double): List<Double> =
        SEEDS.map { accuracy(e9Block(perceptualLoad, taps = 1, seed = it), ExerciseCatalog.E9_ENJAMBRE).value }

    @Test
    fun e9_aSingleTokenResponseBuysTheMaintenanceBandWithoutTrackingAnything() {
        // Total withholding is NOT a score: it is a typed absence, and it has been since MB-FOCUS-0091.
        // This half is what makes the next one meaningful - the exploit strictly needs the token response.
        for (seed in SEEDS) {
            val silent = accuracy(e9Block(0.0, taps = 0, seed = seed), ExerciseCatalog.E9_ENJAMBRE)
            assertTrue(
                silent.absence != null && !silent.confident,
                "a user who never responds must reach the engine as an absence, never as a number; was $silent",
            )
            val token = accuracy(e9Block(0.0, taps = 1, seed = seed), ExerciseCatalog.E9_ENJAMBRE)
            assertTrue(token.confident && token.absence == null, "the token response must produce a usable number")
        }

        // One tap clears the zero-response gate, and everything the user withheld is then counted as a
        // correct rejection. Nothing about the moving objects was ever tracked.
        val scores = e9TokenScores(perceptualLoad = 0.0)
        val inBand = scores.count { it >= config.accDownThreshold && it < config.accUpThreshold }
        val mid = median(scores)
        assertTrue(
            mid >= config.accDownThreshold && mid < config.accUpThreshold,
            "E9 token response scored a median of $mid over ${scores.size} seeds: expected inside the " +
                "maintenance band [${config.accDownThreshold}, ${config.accUpThreshold}). Scores: $scores. If " +
                "this now sits OUTSIDE the band, E9 gained structure that makes withholding fail (H-06 fixed) " +
                "- INVERT this pin, do not delete it.",
        )
        assertTrue(
            inBand * 2 > scores.size,
            "the band must hold for most seeds, not just the median: $inBand of ${scores.size}. Scores: $scores",
        )
    }

    @Test
    fun e9_theOnlyLeverTheLoopCanPullPaysTheNonPerformerMore() {
        // PERCEPTUAL_LOAD is E9's governingAxis, so it is the ONLY axis StepUp/StepDown moves for it.
        // Raising it raises objectCount while targetCount stays put, which thins the chance that the probed
        // object was a target - and every withheld trial then scores as a correct rejection.
        val atStart = e9TokenScores(perceptualLoad = 0.0)
        val atCeiling = e9TokenScores(perceptualLoad = 1.0)
        assertTrue(
            median(atCeiling) > median(atStart),
            "raising E9's governing axis must currently PAY the non-performer: ${median(atStart)} -> " +
                "${median(atCeiling)} (start: $atStart, ceiling: $atCeiling)",
        )

        val crossing = atCeiling.count { it >= config.accUpThreshold }
        assertTrue(
            median(atCeiling) >= config.accUpThreshold && crossing * 2 > atCeiling.size,
            "at maximum PERCEPTUAL_LOAD the token response reaches a median of ${median(atCeiling)} with " +
                "$crossing of ${atCeiling.size} seeds at or above the step-up threshold " +
                "${config.accUpThreshold}: the loop's own lever carries a non-performer into step-up " +
                "territory. INVERT this pin the day E9 stops rewarding withholding. Scores: $atCeiling",
        )
    }

    @Test
    fun e10_servesNoNonTargetTrialSoTappingCannotBeWrong() {
        var blocks = 0
        var trials = 0
        var nonTargets = 0
        var nonPrimary = 0
        for (temporal in SWEEP) {
            for (perceptual in SWEEP) {
                val difficulty = DifficultyVector.EASIEST
                    .adjust(DifficultyAxis.TEMPORAL_UNCERTAINTY, AxisLevel(temporal))
                    .adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel(perceptual))
                for (seed in SEEDS) {
                    val stimuli = E10FlujoGenerator().generate(difficulty, spec, seed)
                    blocks++
                    trials += stimuli.size
                    nonTargets += stimuli.count { !it.isTarget }
                    nonPrimary += stimuli.count { it.correctResponseKey != ResponseKey.PRIMARY }
                    assertTrue(
                        stimuli.any { (it.payload as StimulusPayload.RsvpStream).t2Index != null },
                        "anti-vacuity: the producer emitted no second target at all, so this proves nothing",
                    )
                }
            }
        }
        assertTrue(blocks > 0 && trials > 0, "anti-vacuity: the sweep visited nothing")
        assertEquals(
            0,
            nonTargets,
            "E10 served $nonTargets non-target trials over $trials. Any number above zero means H-07 was " +
                "fixed and catch trials now exist - INVERT this pin, do not delete it.",
        )
        assertEquals(0, nonPrimary, "every E10 trial still asks for the same single PRIMARY response")
    }

    @Test
    fun e10_tappingEveryTrialIsAConfidentPerfectScoreWhileSilenceIsAnHonestAbsence() {
        val stimuli = E10FlujoGenerator().generate(DifficultyVector.EASIEST, spec, seed = 914)
        fun block(tapping: Boolean) = stimuli.map { stimulus ->
            FocusTestFixtures.trial(
                index = stimulus.index,
                outcome = if (tapping) TrialOutcome.HIT else TrialOutcome.MISS,
                rtMs = if (tapping) TAP_RT_MS else null,
                isTarget = true,
                exerciseId = ExerciseId.E10_FLUJO,
            )
        }

        val blindTapper = accuracy(block(tapping = true), ExerciseCatalog.E10_FLUJO)
        assertTrue(blindTapper.confident && blindTapper.absence == null, "the blind tapper must get a number")
        assertEquals(
            expected = 1.0,
            actual = blindTapper.value,
            absoluteTolerance = TOLERANCE,
            message = "with no non-target trial a user who taps on everything cannot be wrong. The day this " +
                "stops being 1.0, E10 gained the structure H-07 asks for - INVERT this pin.",
        )

        // The asymmetry is the point: the one who taps blindly gets a confident 1.0 that E10's own governing
        // metric is made of, while the one who does nothing gets a typed absence, not a zero.
        val silent = accuracy(block(tapping = false), ExerciseCatalog.E10_FLUJO)
        assertTrue(
            silent.absence != null && !silent.confident,
            "silence must stay an absence, never a fabricated 0.0 (an unknown is not a zero); was $silent",
        )
    }

    @Test
    fun e8_ignoringTheSecondDemandScoresTheBestPossibleCost() {
        val stimuli = E8DuoGenerator().generate(DifficultyVector.EASIEST, spec, seed = 908)
        assertTrue(
            stimuli.any { (it.payload as StimulusPayload.DualTask).dual },
            "anti-vacuity: the producer served no dual trial",
        )
        assertTrue(
            stimuli.any { !(it.payload as StimulusPayload.DualTask).dual },
            "anti-vacuity: the producer served no single trial",
        )

        // `failEveryNthDual = 0` is the user who never looks at the counter, so the second demand costs him
        // nothing and his dual trials go exactly like his single ones.
        fun cost(failEveryNthDual: Int): MetricValue {
            var seen = 0
            val trials = stimuli.map { stimulus ->
                val payload = stimulus.payload as StimulusPayload.DualTask
                val fails = payload.dual && failEveryNthDual > 0 && (seen++ % failEveryNthDual) == 0
                val outcome = when {
                    fails && stimulus.isTarget -> TrialOutcome.MISS
                    fails -> TrialOutcome.FALSE_ALARM
                    stimulus.isTarget -> TrialOutcome.HIT
                    else -> TrialOutcome.CORRECT_REJECTION
                }
                val responded = outcome == TrialOutcome.HIT || outcome == TrialOutcome.FALSE_ALARM
                FocusTestFixtures.trial(
                    index = stimulus.index,
                    outcome = outcome,
                    rtMs = if (responded) TAP_RT_MS else null,
                    isTarget = stimulus.isTarget,
                    markers = TrialMarkers(dualTask = payload.dual),
                    exerciseId = ExerciseId.E8_DUO,
                )
            }
            val metrics = engine.blockMetrics(trials, ExerciseCatalog.E8_DUO, thresholds)
            return assertNotNull(metrics[MetricKey.DUAL_TASK_COST], "the engine published no DUAL_TASK_COST")
        }

        val ignoresTheCounter = cost(failEveryNthDual = 0)
        val actuallyDivides = cost(failEveryNthDual = 3)
        assertEquals(
            expected = 0.0,
            actual = ignoresTheCounter.value,
            absoluteTolerance = TOLERANCE,
            message = "a user who never monitors the counter must currently score a cost of exactly 0.0. The " +
                "metric reads positive as a cost, so 0.0 is its best value: the reading rewards not doing the " +
                "very thing it is named after. INVERT this pin the day the second demand is scored.",
        )
        assertTrue(
            actuallyDivides.value > ignoresTheCounter.value,
            "a user whose accuracy really drops on dual trials scores WORSE (${actuallyDivides.value}) than the " +
                "one who ignored the second demand (${ignoresTheCounter.value})",
        )

        // Correction to the finding as written: the value is NOT unsealed. It carries the second seal,
        // derived from the central floor over the SMALLER cell, so no measurement claim rests on it.
        assertNotNull(
            ignoresTheCounter.measurementSufficient,
            "DUAL_TASK_COST must carry its measurement seal; the audit implied it did not",
        )
    }

    private companion object {
        const val TAP_RT_MS = 620L
        const val TOLERANCE = 1e-9
        val SWEEP = listOf(0.0, 0.5, 1.0)
        val SEEDS = listOf(11L, 97L, 313L, 907L, 1291L, 4242L, 8675L)
    }
}

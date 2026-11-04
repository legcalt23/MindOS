package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.metrics.MeasurementReachability
import com.mindos.app.features.mindboost.focus.domain.metrics.MeasurementSufficiency
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the baseline protocol and the reserved probe bank can actually MEASURE — asked of
 * [MeasurementReachability] before anyone plays a single trial.
 *
 * Why this lives in a test and not in a production symbol: nothing in the runtime needs to ask the
 * question, and a production symbol with no consumer is the anti-pattern this repo already paid for
 * (kept "for the next phase", then read by later phases as an existing capability). The barrier that
 * matters is that these numbers are DERIVED from `BaselineProtocols` / `ProbeBank` / `ExerciseCatalog`
 * and pinned, so editing a block size, the calibration difficulty or `fixedProbeDifficulty` turns this
 * file RED and forces the change to be a decision instead of a side effect.
 *
 * NOT a duplicate of `RciMeasurementSufficiencyTest.theBaselineE2BlockCannotSampleACongruencyComparison`,
 * and the split is deliberate: that guard DRAWS stimuli from `E2FiltroGenerator` over 200 seeds and counts
 * them, so it owns "the generator really emits 12". This file asks the reachability authority about the
 * PROTOCOL's declared shape, so it owns "what the baseline and the probe bank can claim at all" — which is
 * what catches a block being added, a probe being swapped, or `fixedProbeDifficulty` being edited.
 *
 * See `docs/audits/ADR_PROBE_REACHABILITY_2026-08-26.md`.
 */
class ProbeMeasurementReachabilityTest {

    /** The real production wiring, so form questions are asked of the producer and not of a fixture. */
    private val graph = FocusTestFixtures.focusGraph()

    /** The reserved-probe block length in [BaselineProtocols.INITIAL] — derived, never hand-written. */
    private val probeTrialCount: Int =
        BaselineProtocols.INITIAL.blocks.single { it.isProbe }.targetTrialCount

    /**
     * Renders one block/probe as `<exercise>@<n> <verdict>`. A `null` verdict is NOT one fact: it is
     * either "this exercise's comparative cell is behavioural, so no design parameter can size it"
     * (E3's post-error, E12) or "this exercise declares no quota-sized comparative at all". Collapsing
     * the two would hide exactly the case that cannot be fixed by choosing a better number.
     */
    private fun row(exerciseId: ExerciseId?, trialCount: Int, difficulty: DifficultyVector): String {
        if (exerciseId == null) return "unrenderable"
        val verdict = MeasurementReachability.verdictFor(exerciseId, trialCount, difficulty)
            ?: return "${exerciseId.name}@$trialCount " +
                (if (exerciseId in MeasurementReachability.behaviouralCells) "behavioural" else "unsized")
        return "${exerciseId.name}@$trialCount ${verdict.metric.name} " +
            "${verdict.smallestCell}/${verdict.floor} " +
            (if (verdict.reachable) "REACHABLE" else "UNREACHABLE")
    }

    @Test
    fun `the baseline protocol's measurement surface is pinned`() {
        val surface = BaselineProtocols.INITIAL.blocks.map { block ->
            val exerciseId = block.exerciseId ?: block.probe?.let { ProbeExerciseMapping.exerciseFor(it) }
            "${block.capacity.name}|" + row(exerciseId, block.targetTrialCount, block.fixedDifficulty)
        }

        assertEquals(
            listOf(
                "SUSTAINED_VIGILANCE|E1_VIGIA@75 unsized",
                "SELECTIVE_FILTERING|E2_FILTRO@60 CONGRUENCY_EFFECT 12/20 UNREACHABLE",
                "RESPONSE_INHIBITION|E3_GATILLO@60 behavioural",
                "SUSTAINED_VIGILANCE|E1_VIGIA@55 unsized",
            ),
            surface,
            "The baseline protocol's measurement surface changed. That is a product decision, not a refactor.",
        )
    }

    /**
     * THE FINDING. A reserved probe is the only instrument the charter admits for a capacity claim
     * (invariant #7). `SELECTIVE_PROBE` claims SELECTIVE_FILTERING, whose governing metric is
     * `CONGRUENCY_EFFECT` — a difference score judged on its SMALLER cell against a ratified floor of
     * 20. `ProbeBank.fixedProbeDifficulty` sets perceptual load and temporal uncertainty and leaves
     * `interference` at MIN, so the probe runs at `incongruentProportion = 0.20` and its smaller cell
     * is 11 of 55. The probe cannot sustain the claim it exists to make — and it is registered in
     * `ProbeBank.all`, so it is served today; only `BaselineProtocols.INITIAL` not selecting it keeps
     * the defect latent rather than live.
     */
    @Test
    fun `the selective probe cannot sustain the capacity claim it exists for`() {
        val probe = ProbeBank.SELECTIVE_PROBE
        val exerciseId = assertNotNull(ProbeExerciseMapping.exerciseFor(probe))
        val definition = ExerciseCatalog.definition(exerciseId)

        // The claim rests on the governing metric, and that metric is the unreachable one.
        assertEquals(MetricKey.CONGRUENCY_EFFECT, definition.governingMetric)
        assertEquals(
            AxisLevel.MIN,
            probe.fixedDifficulty.level(DifficultyAxis.INTERFERENCE),
            "The probe's fixed difficulty leaves interference unset; that is what pins the proportion at 0.20.",
        )

        val verdict = assertNotNull(
            MeasurementReachability.verdictFor(exerciseId, probeTrialCount, probe.fixedDifficulty),
        )
        assertEquals(MetricKey.CONGRUENCY_EFFECT, verdict.metric)
        assertTrue(verdict.comparesTwoCells, "A congruency effect is a difference score.")
        assertEquals(11, verdict.smallestCell)
        assertEquals(20, verdict.floor)
        assertTrue(!verdict.reachable, "11 < 20: the probe is unreachable before the first trial.")
    }

    /**
     * The discriminant half — and the half that used to assert the OPPOSITE of what the probe measures.
     *
     * `VIGILANCE_PROBE` is the one probe the protocol actually runs and its governing metric is `RT_CV`,
     * whose cell is the CAUGHT SIGNALS, not the trials. This guard handed the TRIAL count to the central
     * gate and concluded the probe clears its floor. It does not: a trial nobody had to respond to yields
     * no reaction time, so 55 trials at a declared signal rate near 0.20 carry about 11. That is the same
     * substitution the metrics engine was making one layer down (`MB-FOCUS-0128`), reached from the other
     * side — and a guard that commits the defect it exists to catch is worse than no guard.
     *
     * So it CONSULTS the producer instead of asserting a number. The probe serves a CLOSED population of
     * reserved forms, so every form it can ever serve is enumerable: the bound below is a hard fact about
     * the instrument, not an expectation over a draw. Signals are the UPPER bound on the cell (a missed
     * signal yields no reaction time either), which is the direction that matters — under the floor here
     * means under it always. `MeasurementReachability` cannot answer this: `RT_CV` is not a quota-sized
     * comparative, so it declines the question, and that silence reads as reassurance unless someone asks
     * the generator.
     *
     * The denial stays specific: on the SAME block and the SAME forms, ACCURACY counts every trial and
     * clears the same floor. Without that half, stamping every metric insufficient would pass.
     */
    @Test
    fun `the wired probe cannot sample the metric its claim rests on`() {
        val probe = ProbeBank.VIGILANCE_PROBE
        val exerciseId = assertNotNull(ProbeExerciseMapping.exerciseFor(probe))
        val governing = ExerciseCatalog.definition(exerciseId).governingMetric
        assertEquals(MetricKey.RT_CV, governing)

        // Not a quota-sized comparative, so the reachability authority declines the question…
        assertNull(MeasurementReachability.verdictFor(exerciseId, probeTrialCount, probe.fixedDifficulty))

        // …and the producer says why that silence is not an answer. Every servable form, not a sample.
        val floor = MinSampleGate
            .specFor(assertNotNull(MeasurementSufficiency.centralMetricFor(governing)))
            .nMinPerCell
        assertTrue(probe.reservedForms.isNotEmpty(), "No reserved form: nothing was measured.")
        for (formSeed in probe.reservedForms) {
            val cell = signalsIn(exerciseId, probe.fixedDifficulty, formSeed, probeTrialCount)
            assertEquals(
                false,
                MeasurementSufficiency.evaluate(governing, nEffective = cell, cellsPresent = true),
                "Form $formSeed carries $cell signals in $probeTrialCount trials, against a floor of $floor.",
            )
        }

        // Discriminating half: same block, same difficulty, the metric whose cell IS the trials.
        assertEquals(
            true,
            MeasurementSufficiency.evaluate(MetricKey.ACCURACY, probeTrialCount, cellsPresent = true),
            "ACCURACY counts every trial, so it clears the floor — this is not a blanket denial.",
        )
    }

    /**
     * The probe remedy surface, DERIVED the same way the E2 one is: what block length would let the
     * governing metric reach its floor on EVERY reserved form. Pinned so the size of the gap is a number
     * somebody decides about, not a paragraph. Lengthening the probe invalidates every baseline already
     * captured against the 55-trial form, so this test states the price and takes no decision
     * (`MB-FOCUS-0130`).
     */
    @Test
    fun `the probe remedy surface is pinned`() {
        val probe = ProbeBank.VIGILANCE_PROBE
        val exerciseId = assertNotNull(ProbeExerciseMapping.exerciseFor(probe))
        val floor = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE).nMinPerCell
        fun everyFormClears(trialCount: Int): Boolean = probe.reservedForms.all { formSeed ->
            signalsIn(exerciseId, probe.fixedDifficulty, formSeed, trialCount) >= floor
        }

        val minTrials = (probeTrialCount..MAX_SEARCHED_BLOCK).firstOrNull(::everyFormClears)
        assertNotNull(minTrials, "No block up to $MAX_SEARCHED_BLOCK lets every form reach $floor signals.")
        assertEquals(PROBE_TRIALS_FOR_FLOOR, minTrials, "Smallest block whose every reserved form clears.")
        assertTrue(
            !everyFormClears(minTrials - 1),
            "One trial fewer must still fail, or the boundary is not pinned.",
        )
        assertTrue(
            minTrials > probeTrialCount,
            "If the served block already cleared, the finding above would be stale.",
        )

        // And the second option E2 had does NOT exist here. E2 reached its floor without adding a trial,
        // by setting interference; the axis that governs this rate only makes signals RARER, so no setting
        // of it clears the floor at the block being served. The remedy is the block length or nothing.
        val clearingLoad = (0..100).firstOrNull { step ->
            val at = probe.fixedDifficulty.adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel(step / 100.0))
            probe.reservedForms.all { signalsIn(exerciseId, at, it, probeTrialCount) >= floor }
        }
        assertNull(clearingLoad, "No perceptual load clears the floor at $probeTrialCount trials.")
        // Anti-vacuity: the scan is real, and the axis moves the cell in the direction claimed.
        val atMin = probe.fixedDifficulty.adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel.MIN)
        val atMax = probe.fixedDifficulty.adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel.MAX)
        val easiest = probe.reservedForms.minOf { signalsIn(exerciseId, atMin, it, probeTrialCount) }
        val hardest = probe.reservedForms.minOf { signalsIn(exerciseId, atMax, it, probeTrialCount) }
        assertTrue(
            easiest in 1 until floor && hardest < easiest,
            "Perceptual load must move the cell and still land under $floor: $easiest then $hardest.",
        )
    }

    /**
     * Signals in the form production would actually serve: through `FocusGraph.fixedBlockForm`, so a
     * migrated exercise takes its versioned path exactly as the two live lanes do. Fabricating the form
     * here would let the guard survive the day the producer changes, which is the day it exists for.
     */
    private fun signalsIn(
        exerciseId: ExerciseId,
        difficulty: DifficultyVector,
        formSeed: Long,
        trialCount: Int,
    ): Int = graph.fixedBlockForm(
        id = exerciseId,
        difficulty = difficulty,
        spec = BlockSpec(targetTrialCount = trialCount, mode = Mode.EVALUATION),
        seed = SessionSeed(formSeed, RngVersion.V1),
    ).count { it.isTarget }

    /**
     * Fail-closed adjacency, pinned so nobody "fixes" it backwards. `EvaluationComparator` gates on
     * `measurementSufficient == true` (explicit, not `!= false`). A metric with no ratified floor
     * stamps `null`, and `null == true` is false — so a probe whose governing metric has no central
     * floor can never emit an RCI claim. `INHIBITION_PROBE` is that case (E3 → `FALSE_ALARMS`).
     */
    @Test
    fun `a probe whose governing metric has no ratified floor can never emit a claim`() {
        val exerciseId = assertNotNull(ProbeExerciseMapping.exerciseFor(ProbeBank.INHIBITION_PROBE))
        val governing = ExerciseCatalog.definition(exerciseId).governingMetric
        assertEquals(MetricKey.FALSE_ALARMS, governing)

        assertNull(
            MeasurementSufficiency.centralMetricFor(governing),
            "FALSE_ALARMS has no ratified floor, so the sufficiency question does not apply to it.",
        )
        // Even with an enormous sample, the verdict is `null` — which the RCI's `== true` denies.
        val verdict = MeasurementSufficiency.evaluate(key = governing, nEffective = 10_000, cellsPresent = true)
        assertNull(verdict)
        assertTrue(verdict != true, "Fail-closed: absence of a floor must never read as sufficiency.")
    }

    /**
     * The decision surface, DERIVED from the production authority rather than restated from the ADR.
     * If these disagree with `docs/audits/ADR_PROBE_REACHABILITY_2026-08-26.md` §2, the ADR is wrong
     * and gets corrected — the test is not adjusted to the number.
     */
    @Test
    fun `the remedy surface is pinned`() {
        val minInterference = DifficultyVector.EASIEST
        fun reachableAt(trialCount: Int, difficulty: DifficultyVector): Boolean =
            MeasurementReachability
                .verdictFor(ExerciseId.E2_FILTRO, trialCount, difficulty)
                ?.reachable == true

        // Option A — raise the block, leave interference alone.
        val minTrials = (24..400).first { reachableAt(it, minInterference) }
        assertEquals(98, minTrials, "Smallest block that reaches the floor at proportion 0.20.")
        assertTrue(!reachableAt(97, minInterference), "97 must still fail, or the boundary is not pinned.")

        // Option B — set interference, leave the block at the protocol's 60.
        fun atInterference(step: Int) =
            DifficultyVector.EASIEST.adjust(DifficultyAxis.INTERFERENCE, AxisLevel(step / 100.0))
        val minStep = (0..100).first { reachableAt(BASELINE_E2_TRIALS, atInterference(it)) }
        assertEquals(32, minStep, "interference >= 0.32 reaches the floor without adding a single trial.")

        // The optimum is the BALANCED split, not the maximum: this surface is not monotonic.
        val at75 = assertNotNull(
            MeasurementReachability.verdictFor(ExerciseId.E2_FILTRO, BASELINE_E2_TRIALS, atInterference(75)),
        )
        val at100 = assertNotNull(
            MeasurementReachability.verdictFor(ExerciseId.E2_FILTRO, BASELINE_E2_TRIALS, atInterference(100)),
        )
        assertEquals(30, at75.smallestCell)
        assertEquals(24, at100.smallestCell)
        assertTrue(
            at100.smallestCell < at75.smallestCell,
            "Maximum interference is NOT maximum power; pushing the axis to 1.0 shrinks the smaller cell.",
        )
    }

    private companion object {
        /** Search ceiling for the probe remedy surface; a miss is reported, never silently unbounded. */
        const val MAX_SEARCHED_BLOCK = 400

        /** Measured over the closed set of reserved forms; see `the probe remedy surface is pinned`. */
        const val PROBE_TRIALS_FOR_FLOOR = 110

        /** Derived from the protocol so the remedy surface cannot drift from the block it is about. */
        val BASELINE_E2_TRIALS: Int = BaselineProtocols.INITIAL.blocks
            .single { it.exerciseId == ExerciseId.E2_FILTRO }
            .targetTrialCount
    }
}

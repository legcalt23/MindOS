package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E2FiltroGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * R8 · `MB-CATALOG-0011` — the Reliable Change Index is a claim about the user's capacity (charter
 * invariant: capacity improvement only via a reserved probe + RCI — and the probe is a parallel form
 * of a trained paradigm, so the ladder stays at C0 regardless, `MB-FOCUS-0089`). It may rest only on a
 * sample the CENTRAL table ratifies, never on the in-session floor that exists to steer difficulty.
 *
 * These are inert today: production reliabilities come from [ReliabilitySource.EMPTY], so every RCI is
 * already `NotEnoughData`. The defect being closed is armed, not live — the day an empirical reliability
 * study populates that seam (which the design says will happen "without touching call sites") a
 * congruency effect measured on 3 trials per cell would have started supporting a reliable-change claim.
 */
class RciMeasurementSufficiencyTest {

    private val reliableRt = ReliabilityEstimate(MetricKey.RT_MEDIAN, reliability = 0.8, sdPre = 40.0)

    @Test
    fun rciRefusesAMetricWhoseSampleIsBelowTheCentralFloor() {
        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot("user", mapOf(MetricKey.RT_MEDIAN to 320.0), capturedAtMs = 0),
            retestMetrics = mapOf(MetricKey.RT_MEDIAN to rtMedian(n = 8, measurementSufficient = false)),
            reliabilities = mapOf(MetricKey.RT_MEDIAN to reliableRt),
            capturedAtMs = 10,
        )

        // A change of 70 ms with a reliability of 0.8 would be "reliable" on the old rule. It is dropped
        // entirely: an under-sampled metric produces no MetricChange at all, so nothing downstream can
        // read a delta and label it. Absent, never a number (`null != 0`).
        assertTrue(result.changes.isEmpty(), "an under-sampled metric must not reach the comparison")
        assertTrue(!result.hasReliableChange)
    }

    @Test
    fun rciStillFiresWhenTheSampleClearsTheCentralFloor_discriminantHalf() {
        // Without this half, "refuse everything" would pass the test above.
        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot("user", mapOf(MetricKey.RT_MEDIAN to 300.0), capturedAtMs = 0),
            retestMetrics = mapOf(MetricKey.RT_MEDIAN to rtMedian(n = 50, measurementSufficient = true)),
            reliabilities = mapOf(MetricKey.RT_MEDIAN to reliableRt),
            capturedAtMs = 10,
        )

        val change = result.changes.single { it.metric == MetricKey.RT_MEDIAN }
        assertIs<RciResult.Value>(change.rci)
        assertTrue(result.hasReliableChange)
    }

    @Test
    fun confidentIsNoLongerWhatDecidesAReliableChange() {
        // The exact defect: `confident` true (the in-session floor of 5 is cleared at n = 8) while the
        // measurement floor is not. Before R8 this produced a reliable-change claim.
        val underSampled = MetricValue(
            key = MetricKey.RT_MEDIAN,
            value = 250.0,
            n = 8,
            confident = true,
            unit = MetricUnit.MILLISECONDS,
            measurementSufficient = false,
        )
        assertTrue(underSampled.confident, "the fixture must reproduce the old passing condition")

        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot("user", mapOf(MetricKey.RT_MEDIAN to 320.0), capturedAtMs = 0),
            retestMetrics = mapOf(MetricKey.RT_MEDIAN to underSampled),
            reliabilities = mapOf(MetricKey.RT_MEDIAN to reliableRt),
            capturedAtMs = 10,
        )
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun aMetricOutsideTheCentralTableCannotCarryAReliableChange() {
        // VIGILANCE_DECREMENT_SLOPE has no ratified floor, so its verdict is null → fail-closed.
        val slope = MetricValue(
            key = MetricKey.VIGILANCE_DECREMENT_SLOPE,
            value = 0.4,
            n = 500,
            confident = true,
            unit = MetricUnit.SLOPE,
            measurementSufficient = null,
        )
        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot(
                "user",
                mapOf(MetricKey.VIGILANCE_DECREMENT_SLOPE to 0.1),
                capturedAtMs = 0,
            ),
            retestMetrics = mapOf(MetricKey.VIGILANCE_DECREMENT_SLOPE to slope),
            reliabilities = mapOf(
                MetricKey.VIGILANCE_DECREMENT_SLOPE to
                    ReliabilityEstimate(MetricKey.VIGILANCE_DECREMENT_SLOPE, 0.8, 0.1),
            ),
            capturedAtMs = 10,
        )
        assertTrue(result.changes.isEmpty(), "no ratified floor means no capacity claim, however big n is")
    }

    @Test
    fun theBaselineE2BlockCannotSampleACongruencyComparison() {
        // Reachability, COMPUTED rather than assumed (the R7 lesson: never adopt a threshold without checking
        // what the real call-sites can reach). `calibrationDifficulty` leaves `interference` at its MIN
        // default, so E2's incongruent share over the 60-trial baseline block is 0.20 — 12 trials against a
        // 20-per-cell floor. And this counts stimuli; only HITs actually feed the RT cells, so the real
        // sample is smaller still.
        //
        // R8 wrote this DISTRIBUTIONALLY, because the draw was then a per-trial Bernoulli: a median of ~12
        // with a thin tail that did clear 20 (measured: 4..20 over 40 seeds). That was worse than a hard
        // ceiling — whether a retest could carry a capacity claim came down to the seed. R9 turned the draw
        // into a quota (`MB-CATALOG-0014`), so the fact is now exact and the assertion can be too. The
        // deficit itself is UNCHANGED and still owned by product: raising the block or pinning `interference`
        // are its remedies, and lowering the central table is the anti-pattern R7 closed.
        val block = BaselineProtocols.INITIAL.blocks.single { it.exerciseId == ExerciseId.E2_FILTRO }
        val floor = MinSampleGate.specFor(ScoreMetricId.CONGRUENCY_EFFECT).nMinPerCell
        assertEquals(60, block.targetTrialCount, "the baseline E2 block size changed; recompute this fact")

        val incongruentPerSeed = (0 until SEEDS).map { seed ->
            E2FiltroGenerator()
                .generate(
                    difficulty = block.fixedDifficulty,
                    spec = BlockSpec(targetTrialCount = block.targetTrialCount, mode = block.mode),
                    seed = SessionSeed(seed.toLong(), RngVersion.V1),
                )
                .count { (it.payload as? StimulusPayload.Flanker)?.congruent == false }
        }.toSet()

        // Not vacuous: the cell EXISTS in every draw. The problem is its size, not its absence — otherwise
        // this test would also pass against a generator that emitted no incongruent trials at all.
        assertTrue(incongruentPerSeed.all { it > 0 }, "every draw must contain some incongruent trials")
        assertEquals(
            1,
            incongruentPerSeed.size,
            "over $SEEDS seeds the cell must be one single size; it was $incongruentPerSeed",
        )
        assertTrue(
            incongruentPerSeed.single() < floor,
            "the baseline E2 block now clears the congruency floor ($floor); the deficit fact is stale " +
                "and the ADR must be recomputed",
        )
    }

    @Test
    fun anUnderSampledMetricIsRecordedAsWithheld_notSilentlyDropped() {
        // R12 · `MB-CATALOG-0019`. Keeping the NUMBER out is R8 and stays exactly as it was; what changes
        // is that the FACT no longer vanishes with it. Fail-closed on the value, not on the knowledge that
        // the value was withheld — otherwise "we could not measure this" is indistinguishable downstream
        // from "this probe does not measure that", and only one of those is a fact about the user.
        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot("user", mapOf(MetricKey.RT_MEDIAN to 320.0), capturedAtMs = 0),
            retestMetrics = mapOf(MetricKey.RT_MEDIAN to rtMedian(n = 8, measurementSufficient = false)),
            reliabilities = mapOf(MetricKey.RT_MEDIAN to reliableRt),
            capturedAtMs = 10,
        )

        assertTrue(result.changes.isEmpty(), "the under-sampled VALUE must still not cross")
        assertEquals(listOf(MetricKey.RT_MEDIAN), result.withheldForSample)
    }

    @Test
    fun aMetricWithNoRatifiedFloorIsNotReportedAsWithheld_discriminantHalf() {
        // Abort criterion A4 of the ADR, mechanised. `measurementSufficient == null` means "no floor has
        // been ratified, so the question does not apply" — NOT "we tried and the sample was short". Without
        // this half, "record everything the gate rejects" would pass the test above while conflating the two
        // facts this change exists to separate.
        val slope = MetricValue(
            key = MetricKey.VIGILANCE_DECREMENT_SLOPE,
            value = 0.4,
            n = 500,
            confident = true,
            unit = MetricUnit.SLOPE,
            measurementSufficient = null,
        )
        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot(
                "user",
                mapOf(MetricKey.VIGILANCE_DECREMENT_SLOPE to 0.1),
                capturedAtMs = 0,
            ),
            retestMetrics = mapOf(MetricKey.VIGILANCE_DECREMENT_SLOPE to slope),
            reliabilities = emptyMap(),
            capturedAtMs = 10,
        )

        assertTrue(result.changes.isEmpty(), "no ratified floor still means no capacity claim")
        assertTrue(
            result.withheldForSample.isEmpty(),
            "an ungoverned metric was never a measurement we attempted to sample; it is not 'withheld'",
        )
    }

    @Test
    fun aSufficientMetricIsNotReportedAsWithheld_discriminantHalf() {
        // The other half: without it, "report every metric as withheld" would satisfy both tests above.
        val result = EvaluationComparator.compare(
            userId = "user",
            baseline = BaselineSnapshot("user", mapOf(MetricKey.RT_MEDIAN to 300.0), capturedAtMs = 0),
            retestMetrics = mapOf(MetricKey.RT_MEDIAN to rtMedian(n = 50, measurementSufficient = true)),
            reliabilities = mapOf(MetricKey.RT_MEDIAN to reliableRt),
            capturedAtMs = 10,
        )

        assertEquals(1, result.changes.size)
        assertTrue(result.withheldForSample.isEmpty())
    }

    private fun rtMedian(n: Int, measurementSufficient: Boolean) = MetricValue(
        key = MetricKey.RT_MEDIAN,
        value = 250.0,
        n = n,
        confident = true,
        unit = MetricUnit.MILLISECONDS,
        measurementSufficient = measurementSufficient,
    )

    private companion object {
        const val SEEDS = 200
    }
}

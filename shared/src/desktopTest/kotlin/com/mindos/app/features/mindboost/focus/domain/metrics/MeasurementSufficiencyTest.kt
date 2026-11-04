package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * R8 · `MB-CATALOG-0011` — `DefaultMetricsEngine` consumes `MinSampleGate`, as a SECOND named fact.
 *
 * `confident` answers the in-session control question (may the adaptive engine move difficulty on this,
 * may the UI caption it). `measurementSufficient` answers a different one: is the sample big enough to
 * carry a MEASUREMENT claim — the reliable-change index above all. One boolean was answering both, and
 * the two questions have legitimately different floors (5 vs 20/cell). Raising the single boolean would
 * have silenced the control loop; leaving it alone would have let a 3-trial-per-cell difference score
 * support a claim about the user's capacity.
 */
class MeasurementSufficiencyTest {

    @Test
    fun everyMetricKeyDeclaresWhetherItHasACentralFloor() {
        // Anti-vacuity + forcing function: a MetricKey added later has to be decided here, not defaulted
        // into silence. Absence from the central table is a legitimate answer; forgetting is not.
        val mapped = MetricKey.entries.associateWith { MeasurementSufficiency.centralMetricFor(it) }

        assertEquals(
            setOf(
                MetricKey.ACCURACY, MetricKey.RT_MEDIAN, MetricKey.RT_CV, MetricKey.LAPSES,
                MetricKey.IES, MetricKey.RATE_CORRECT_SCORE, MetricKey.LISAS,
                MetricKey.BLOCK_STABILITY, MetricKey.SUSTAINED_FOCUS_INDEX,
                MetricKey.CONGRUENCY_EFFECT, MetricKey.SWITCH_COST, MetricKey.DUAL_TASK_COST,
                MetricKey.PRESSURE_DELTA, MetricKey.POST_ERROR_SLOWING, MetricKey.POST_ERROR_ACCURACY,
                MetricKey.SSRT,
            ),
            mapped.filterValues { it != null }.keys,
            "the set of metrics that can carry a measurement claim changed — decide it deliberately",
        )
        assertEquals(ScoreMetricId.ACCURACY_RT_BASE, mapped[MetricKey.ACCURACY])
        assertEquals(ScoreMetricId.CONGRUENCY_EFFECT, mapped[MetricKey.CONGRUENCY_EFFECT])
        assertEquals(ScoreMetricId.POST_ERROR, mapped[MetricKey.POST_ERROR_SLOWING])
        assertEquals(ScoreMetricId.SSRT, mapped[MetricKey.SSRT])
    }

    @Test
    fun metricsOutsideTheCentralTableCannotClaimMeasurement() {
        // Fail-closed and declared: no ratified floor exists for these, so no reliable-change claim may
        // rest on them. `null` means "the question does not apply", never "yes" and never "no".
        listOf(
            MetricKey.VIGILANCE_DECREMENT_SLOPE,
            MetricKey.SET_SIZE_SLOPE,
            MetricKey.DISTRACTION_RESISTANCE_INDEX,
            MetricKey.FATIGUE_INDEX,
            MetricKey.FALSE_ALARMS,
        ).forEach { key ->
            assertNull(MeasurementSufficiency.centralMetricFor(key), "$key must have no central floor")
            assertNull(
                MeasurementSufficiency.evaluate(key, nEffective = 10_000, cellsPresent = true),
                "$key must not become measurement-sufficient just by having a huge sample",
            )
        }
    }

    @Test
    fun baseMetricsNeedTheCentralFloor_notTheInSessionOne() {
        val floor = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE).nMinPerCell
        assertEquals(20, floor, "the central floor moved; this test pins the value the decision was made on")

        assertEquals(
            false,
            MeasurementSufficiency.evaluate(MetricKey.ACCURACY, floor - 1, cellsPresent = true),
        )
        assertEquals(
            true,
            MeasurementSufficiency.evaluate(MetricKey.ACCURACY, floor, cellsPresent = true),
        )
        // Discriminant half: the in-session floor (5) must NOT be what decides a measurement claim.
        assertEquals(
            false,
            MeasurementSufficiency.evaluate(MetricKey.RT_MEDIAN, 5, cellsPresent = true),
            "5 clean RTs is enough to steer difficulty and nowhere near enough to claim a change",
        )
    }

    @Test
    fun comparativeMetricsAreJudgedOnTheirSMALLESTCell() {
        // A difference score is limited by its weaker cell: 40 congruent + 19 incongruent is 59 trials of
        // total sample and still cannot support the comparison. Passing the SUM here would be the defect.
        assertEquals(
            false,
            MeasurementSufficiency.evaluate(MetricKey.CONGRUENCY_EFFECT, 19, cellsPresent = true),
        )
        assertEquals(
            true,
            MeasurementSufficiency.evaluate(MetricKey.CONGRUENCY_EFFECT, 20, cellsPresent = true),
        )
        // A missing cell is not a small cell: with one condition absent there is no comparison at all.
        assertEquals(
            false,
            MeasurementSufficiency.evaluate(MetricKey.CONGRUENCY_EFFECT, 40, cellsPresent = false),
        )
    }

    @Test
    fun ssrtCarriesItsOwnHigherFloor() {
        val floor = MinSampleGate.specFor(ScoreMetricId.SSRT).nMinPerCell
        assertEquals(30, floor)
        assertEquals(false, MeasurementSufficiency.evaluate(MetricKey.SSRT, 29, cellsPresent = true))
        assertEquals(true, MeasurementSufficiency.evaluate(MetricKey.SSRT, 30, cellsPresent = true))
    }

    @Test
    fun theEngineStampsTheVerdictOnEveryMetricThatHasACentralFloor() {
        // The pure mapping is worthless if the engine never fills it in. This is the wiring guard.
        val trials = buildList {
            repeat(30) {
                add(flankerHit(it, rt = 320, congruent = true))
                add(flankerHit(100 + it, rt = 380, congruent = false))
            }
        }
        val metrics = DefaultMetricsEngine().blockMetrics(
            trials = trials,
            definition = ExerciseCatalog.E2_FILTRO,
            thresholds = IndividualThresholds(),
        )

        metrics.forEach { (key, value) ->
            if (MeasurementSufficiency.centralMetricFor(key) != null) {
                assertNotNull(
                    value.measurementSufficient,
                    "$key has a central floor but the engine left its measurement verdict undeclared",
                )
            } else {
                assertNull(value.measurementSufficient, "$key has no central floor and must stay null")
            }
        }
        assertTrue(metrics.isNotEmpty())
    }

    @Test
    fun theEngineJudgesACongruencyEffectOnItsSmallerCell() {
        // 30 congruent + 10 incongruent: a total of 40 clears the floor of 20, the smaller cell does not.
        // Without this case the engine could pass the SUM to the gate and every test above would still pass,
        // because the pure mapping is correct in isolation — the defect would live entirely in the wiring.
        val lopsided = buildList {
            repeat(30) { add(flankerHit(it, rt = 320, congruent = true)) }
            repeat(10) { add(flankerHit(100 + it, rt = 380, congruent = false)) }
        }
        val metrics = DefaultMetricsEngine().blockMetrics(
            trials = lopsided,
            definition = ExerciseCatalog.E2_FILTRO,
            thresholds = IndividualThresholds(),
        )
        val congruency = assertNotNull(metrics[MetricKey.CONGRUENCY_EFFECT])
        assertEquals(40, congruency.n, "n is still the total — the sample of the CLAIM is what differs")
        assertEquals(false, congruency.measurementSufficient)

        // Discriminant half: with both cells at 20 the same block does become measurement-sufficient, so the
        // rule is "the smaller cell", not "always deny a comparative".
        val balanced = buildList {
            repeat(20) { add(flankerHit(it, rt = 320, congruent = true)) }
            repeat(20) { add(flankerHit(100 + it, rt = 380, congruent = false)) }
        }
        val balancedMetrics = DefaultMetricsEngine().blockMetrics(
            trials = balanced,
            definition = ExerciseCatalog.E2_FILTRO,
            thresholds = IndividualThresholds(),
        )
        assertEquals(true, balancedMetrics[MetricKey.CONGRUENCY_EFFECT]?.measurementSufficient)
    }

    private fun flankerHit(index: Int, rt: Long, congruent: Boolean) = FocusTestFixtures.trial(
        index = index,
        outcome = TrialOutcome.HIT,
        rtMs = rt,
        markers = TrialMarkers(congruent = congruent),
        exerciseId = ExerciseId.E2_FILTRO,
    )
}

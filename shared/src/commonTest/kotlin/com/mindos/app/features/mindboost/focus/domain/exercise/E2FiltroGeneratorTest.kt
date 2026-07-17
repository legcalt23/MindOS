package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.valueOf
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class E2FiltroGeneratorTest {

    private val generator = E2FiltroGenerator()
    private val spec = BlockSpec(targetTrialCount = 60, mode = Mode.TRAINING)

    @Test
    fun generation_isDeterministicForSameSeed() {
        assertEquals(
            generator.generate(DifficultyVector.EASIEST, spec, seed = SessionSeed(5L, RngVersion.V1)),
            generator.generate(DifficultyVector.EASIEST, spec, seed = SessionSeed(5L, RngVersion.V1)),
        )
    }

    @Test
    fun higherInterference_yieldsMoreIncongruentTrials() {
        fun incongruentCount(interference: Double): Int = generator.generate(
            DifficultyVector.EASIEST.adjust(DifficultyAxis.INTERFERENCE, AxisLevel(interference)),
            spec,
            seed = SessionSeed(9L, RngVersion.V1),
        ).count { (it.payload as StimulusPayload.Flanker).congruent.not() }

        assertTrue(incongruentCount(1.0) > incongruentCount(0.0))
    }

    @Test
    fun mapping_readsOnlyRelevantAxes() {
        val base = generator.generate(DifficultyVector.EASIEST, spec, seed = SessionSeed(3L, RngVersion.V1))
        // DUAL_TASK_LOAD is irrelevant to E2 — changing it must not alter the sequence.
        val changed = generator.generate(
            DifficultyVector.EASIEST.adjust(DifficultyAxis.DUAL_TASK_LOAD, AxisLevel(1.0)),
            spec,
            seed = SessionSeed(3L, RngVersion.V1),
        )
        assertEquals(base, changed)
    }

    @Test
    fun congruencyEffect_isComputableFromTrials() {
        // Congruent responses are fast (~300ms); incongruent are slower (~380ms) → +80ms effect.
        fun hit(i: Int, rt: Long, congruent: Boolean) = FocusTestFixtures.trial(
            i, TrialOutcome.HIT, rtMs = rt, congruent = congruent, exerciseId = ExerciseId.E2_FILTRO,
        )
        val trials = buildList {
            repeat(6) { add(hit(it, 300, congruent = true)) }
            repeat(6) { add(hit(10 + it, 380, congruent = false)) }
        }
        val metrics = DefaultMetricsEngine().blockMetrics(trials, ExerciseCatalog.E2_FILTRO, IndividualThresholds())
        assertEquals(80.0, metrics.valueOf(MetricKey.CONGRUENCY_EFFECT), absoluteTolerance = 1.0)
        assertTrue(metrics[MetricKey.CONGRUENCY_EFFECT]?.confident == true)
    }
}

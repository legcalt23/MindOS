package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.congruentFlag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class E5ConflictoGeneratorTest {

    private val generator = E5ConflictoGenerator()
    private val spec = BlockSpec(targetTrialCount = 60, mode = Mode.TRAINING)

    @Test
    fun generation_isDeterministicForSameSeed() {
        assertEquals(
            generator.generate(DifficultyVector.EASIEST, spec, seed = 12),
            generator.generate(DifficultyVector.EASIEST, spec, seed = 12),
        )
    }

    @Test
    fun higherInterference_yieldsMoreIncongruentTrials() {
        fun incongruent(interference: Double) = generator.generate(
            DifficultyVector.EASIEST.adjust(DifficultyAxis.INTERFERENCE, AxisLevel(interference)),
            spec,
            seed = 14,
        ).count { (it.payload as StimulusPayload.Conflict).congruent.not() }
        assertTrue(incongruent(1.0) > incongruent(0.0))
    }

    @Test
    fun mapping_readsOnlyRelevantAxes() {
        val base = generator.generate(DifficultyVector.EASIEST, spec, seed = 15)
        val changed = generator.generate(
            DifficultyVector.EASIEST.adjust(DifficultyAxis.DUAL_TASK_LOAD, AxisLevel(1.0)),
            spec,
            seed = 15,
        )
        assertEquals(base, changed)
    }

    @Test
    fun correctKeyMatchesRelevantDimension_andCongruentFlagExposed() {
        val stimulus = generator.generate(DifficultyVector.EASIEST, spec, seed = 1).first()
        val payload = stimulus.payload as StimulusPayload.Conflict
        assertEquals(payload.relevantResponse, stimulus.correctResponseKey)
        // The shared congruency helper exposes the flag for the metrics engine.
        assertEquals(payload.congruent, stimulus.congruentFlag())
    }
}

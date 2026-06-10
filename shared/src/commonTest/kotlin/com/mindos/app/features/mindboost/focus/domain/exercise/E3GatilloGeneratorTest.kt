package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class E3GatilloGeneratorTest {

    private val generator = E3GatilloGenerator()
    private val spec = BlockSpec(targetTrialCount = 60, mode = Mode.TRAINING)

    @Test
    fun generation_isDeterministicForSameSeed() {
        assertEquals(
            generator.generate(DifficultyVector.EASIEST, spec, seed = 8),
            generator.generate(DifficultyVector.EASIEST, spec, seed = 8),
        )
    }

    @Test
    fun guaranteesAtLeastOneNoGo() {
        val stimuli = generator.generate(DifficultyVector.EASIEST, spec, seed = 4)
        assertTrue(stimuli.any { !it.isTarget })
    }

    @Test
    fun noGoStimuli_haveNoCorrectKey() {
        val noGo = generator.generate(DifficultyVector.EASIEST, spec, seed = 2).first { !it.isTarget }
        assertNull(noGo.correctResponseKey)
        assertTrue((noGo.payload as StimulusPayload.GoNoGo).go.not())
    }

    @Test
    fun higherInhibitoryDemand_increasesGoProportion() {
        fun goCount(demand: Double) = generator.generate(
            DifficultyVector.EASIEST.adjust(DifficultyAxis.INHIBITORY_DEMAND, AxisLevel(demand)),
            spec,
            seed = 7,
        ).count { it.isTarget }
        assertTrue(goCount(1.0) >= goCount(0.0))
    }

    @Test
    fun goStimulus_classifiesAsHit_andRespondedNoGo_asFalseAlarm() {
        val stimuli = generator.generate(DifficultyVector.EASIEST, spec, seed = 1)
        val go = stimuli.first { it.isTarget }
        val noGo = stimuli.first { !it.isTarget }

        val hit = TrialClassifier.classify(go.isTarget, responseLatencyMs = 300, 150, go.responseWindowMs)
        assertEquals(TrialOutcome.HIT, hit.outcome)

        val falseAlarm = TrialClassifier.classify(noGo.isTarget, responseLatencyMs = 300, 150, noGo.responseWindowMs)
        assertEquals(TrialOutcome.FALSE_ALARM, falseAlarm.outcome)

        // Withholding on the go-target produces a miss; withholding on no-go is a correct rejection.
        assertEquals(ResponseKey.PRIMARY, go.correctResponseKey)
    }
}

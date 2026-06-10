package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancedExerciseGeneratorsTest {

    private val spec = BlockSpec(targetTrialCount = 60, mode = Mode.TRAINING)
    private val easiest = DifficultyVector.EASIEST

    private fun axis(axis: DifficultyAxis, level: Double) = easiest.adjust(axis, AxisLevel(level))

    @Test
    fun allGenerators_areDeterministicForSameSeed() {
        listOf(
            E6CambioGenerator(), E7RastreoGenerator(), E8DuoGenerator(), E9EnjambreGenerator(),
            E10FlujoGenerator(), E11AnclaGenerator(), E12ReengancheGenerator(), E13PresionGenerator(),
        ).forEach { generator ->
            assertEquals(
                generator.generate(easiest, spec, seed = 21),
                generator.generate(easiest, spec, seed = 21),
                "${generator.exerciseId} must be deterministic",
            )
        }
    }

    @Test
    fun e6_moreSwitchesAtHigherInterference() {
        fun switches(level: Double) = E6CambioGenerator()
            .generate(axis(DifficultyAxis.INTERFERENCE, level), spec, seed = 3)
            .count { (it.payload as StimulusPayload.TaskSwitch).isSwitch }
        assertTrue(switches(1.0) > switches(0.0))
    }

    @Test
    fun e7_variesSetSize_andGrowsWithPerceptualLoad() {
        val stimuli = E7RastreoGenerator().generate(axis(DifficultyAxis.PERCEPTUAL_LOAD, 1.0), spec, seed = 4)
        val sizes = stimuli.map { (it.payload as StimulusPayload.Search).setSize }.toSet()
        assertTrue(sizes.size >= 2, "set size should vary so a slope can be fit")
        val maxAtHighLoad = sizes.max()
        val maxAtLowLoad = E7RastreoGenerator().generate(axis(DifficultyAxis.PERCEPTUAL_LOAD, 0.0), spec, seed = 4)
            .maxOf { (it.payload as StimulusPayload.Search).setSize }
        assertTrue(maxAtHighLoad > maxAtLowLoad)
    }

    @Test
    fun e8_moreDualTrialsAtHigherDualTaskLoad() {
        fun dualCount(level: Double) = E8DuoGenerator()
            .generate(axis(DifficultyAxis.DUAL_TASK_LOAD, level), spec, seed = 5)
            .count { (it.payload as StimulusPayload.DualTask).dual }
        assertTrue(dualCount(1.0) > dualCount(0.0))
    }

    @Test
    fun e9_producesRequestedObjectAndTargetCounts() {
        val payload = E9EnjambreGenerator().generate(easiest, spec, seed = 6).first().payload
            as StimulusPayload.MultiObjectTracking
        assertEquals(payload.targetCount, payload.targetIndices.size)
        assertTrue(payload.targetIndices.all { it < payload.objectCount })
    }

    @Test
    fun e10_t1PrecedesT2_andLagShortensWithTemporalUncertainty() {
        fun avgLag(level: Double): Double {
            val streams = E10FlujoGenerator().generate(axis(DifficultyAxis.TEMPORAL_UNCERTAINTY, level), spec, seed = 7)
                .map { it.payload as StimulusPayload.RsvpStream }
            streams.filter { it.t2Index != null }.forEach { assertTrue(it.t1Index < it.t2Index!!) }
            return streams.mapNotNull { it.t2Index?.minus(it.t1Index)?.toDouble() }.average()
        }
        assertTrue(avgLag(1.0) < avgLag(0.0), "higher temporal uncertainty → shorter T1–T2 lag (blink regime)")
    }

    @Test
    fun e11_moreIntrudersAtHigherSalience() {
        fun intruders(level: Double) = E11AnclaGenerator()
            .generate(axis(DifficultyAxis.DISTRACTOR_SALIENCE, level), spec, seed = 8)
            .count { (it.payload as StimulusPayload.AnchorTask).intruderPresent }
        assertTrue(intruders(1.0) > intruders(0.0))
    }

    @Test
    fun e12_marksSomeRecoveryTrialsAfterProvokedError() {
        val stimuli = E12ReengancheGenerator().generate(axis(DifficultyAxis.INHIBITORY_DEMAND, 1.0), spec, seed = 9)
        assertTrue(stimuli.any { (it.payload as StimulusPayload.Reengagement).afterProvokedError })
    }

    @Test
    fun e13_moreDeadlineTrialsAtHigherPressure() {
        fun pressured(level: Double) = E13PresionGenerator()
            .generate(axis(DifficultyAxis.PRESSURE, level), spec, seed = 10)
            .count { (it.payload as StimulusPayload.Pressure).underPressure }
        assertTrue(pressured(1.0) >= pressured(0.0))
    }
}

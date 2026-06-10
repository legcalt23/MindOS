package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultArchetypeInferenceTest {

    private val inference = DefaultArchetypeInference()

    @Suppress("LongParameterList") // Test builder mirroring ArchetypeSignals' fields.
    private fun signals(
        accuracy: Double = 0.8,
        rtMedianMs: Double = 400.0,
        rtCv: Double = 0.2,
        falseAlarmRate: Double = 0.02,
        lapseRate: Double = 0.02,
        pressureDelta: Double = 0.1,
        totalSessions: Int = 10,
    ) = ArchetypeSignals(accuracy, rtMedianMs, rtCv, falseAlarmRate, lapseRate, pressureDelta, totalSessions)

    private fun inferArchetype(s: ArchetypeSignals) = inference.infer(s).archetype

    @Test
    fun fewSessions_isLowConfidenceBeginner() {
        val result = inference.infer(signals(totalSessions = 1))
        assertEquals(UserArchetype.BEGINNER, result.archetype)
        assertTrue(result.confidence < 0.5)
    }

    @Test
    fun highFalseAlarmsAndFast_isImpulsive() {
        assertEquals(
            UserArchetype.IMPULSIVE,
            inferArchetype(signals(accuracy = 0.78, rtMedianMs = 270.0, falseAlarmRate = 0.30)),
        )
    }

    @Test
    fun fastAndInaccurate_withoutCommissions_isFastButInaccurate() {
        assertEquals(
            UserArchetype.FAST_BUT_INACCURATE,
            inferArchetype(signals(accuracy = 0.50, rtMedianMs = 270.0, falseAlarmRate = 0.01, rtCv = 0.35)),
        )
    }

    @Test
    fun slowAndAccurate_isSlowButAccurate() {
        assertEquals(
            UserArchetype.SLOW_BUT_ACCURATE,
            inferArchetype(signals(accuracy = 0.96, rtMedianMs = 540.0, falseAlarmRate = 0.01, rtCv = 0.20)),
        )
    }

    @Test
    fun highVariabilityAndLapses_isLowConcentration() {
        assertEquals(
            UserArchetype.LOW_CONCENTRATION,
            inferArchetype(signals(accuracy = 0.82, rtMedianMs = 420.0, rtCv = 0.55, lapseRate = 0.25)),
        )
    }

    @Test
    fun accurateStableButShakyUnderPressure_isAdvanced() {
        val s = signals(accuracy = 0.94, rtMedianMs = 360.0, rtCv = 0.12, falseAlarmRate = 0.01, pressureDelta = 0.30)
        assertEquals(UserArchetype.ADVANCED, inferArchetype(s))
    }

    @Test
    fun accurateStableAndResilientUnderPressure_isElite() {
        val s = signals(accuracy = 0.95, rtMedianMs = 340.0, rtCv = 0.10, falseAlarmRate = 0.01, pressureDelta = 0.0)
        assertEquals(UserArchetype.ELITE, inferArchetype(s))
    }

    @Test
    fun result_carriesConfidenceAndSecondGuess() {
        val result = inference.infer(signals(accuracy = 0.95, rtMedianMs = 340.0, rtCv = 0.10, pressureDelta = 0.0))
        assertTrue(result.confidence in 0.0..1.0)
        assertNotNull(result.secondGuess)
    }
}

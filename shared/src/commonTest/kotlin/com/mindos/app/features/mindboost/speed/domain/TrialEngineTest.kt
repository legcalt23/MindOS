package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrialEngineTest {

    private val ms = 1_000_000L
    private fun engine() = FixedTrialEngine(
        blockId = "b",
        exerciseId = SpeedExerciseId.E1_DESTELLO,
        context = SpeedTestFixtures.context,
    )

    @Test
    fun startTrial_returnsStimulusTimestamp() {
        val e = engine()
        assertEquals(1_000L, e.startTrial(0.5, deadlineNanos = null, stimulusUptimeNanos = 1_000L))
    }

    @Test
    fun onResponse_computesRtAsResponseMinusStimulus() {
        val e = engine()
        e.startTrial(0.5, null, stimulusUptimeNanos = 1_000_000_000L)
        val record = e.onResponse(responseUptimeNanos = 1_300 * ms, isCorrect = true)
        assertEquals(1_300 * ms - 1_000_000_000L, record.rtNanos)
        assertEquals(300 * ms, record.rtNanos)
    }

    @Test
    fun anticipation_classifiedBelowThreshold() {
        val e = engine()
        e.startTrial(0.5, null, stimulusUptimeNanos = 0L)
        // 80 ms < 100 ms umbral ⇒ anticipación.
        val record = e.onResponse(responseUptimeNanos = 80 * ms, isCorrect = true)
        assertTrue(record.isAnticipation)
    }

    @Test
    fun validResponse_notAnticipation() {
        val e = engine()
        e.startTrial(0.5, null, stimulusUptimeNanos = 0L)
        val record = e.onResponse(responseUptimeNanos = 250 * ms, isCorrect = true)
        assertFalse(record.isAnticipation)
    }

    @Test
    fun negativeRt_throws() {
        val e = engine()
        e.startTrial(0.5, null, stimulusUptimeNanos = 1_000 * ms)
        // respuesta antes del estímulo ⇒ RT negativo ⇒ inválido.
        assertFailsWith<IllegalArgumentException> {
            e.onResponse(responseUptimeNanos = 500 * ms, isCorrect = true)
        }
    }

    @Test
    fun onResponseBeforeStart_throws() {
        assertFailsWith<IllegalArgumentException> {
            engine().onResponse(responseUptimeNanos = 100 * ms, isCorrect = true)
        }
    }

    @Test
    fun history_accumulatesTrialsWithIncrementingIndex() {
        val e = engine()
        repeat(3) { i ->
            e.startTrial(0.5, null, stimulusUptimeNanos = (i * 1_000_000_000L))
            e.onResponse(responseUptimeNanos = i * 1_000_000_000L + 250 * ms, isCorrect = true)
        }
        val history = e.getTrialHistory()
        assertEquals(3, history.size)
        assertEquals(listOf(0, 1, 2), history.map { it.trialIndex })
        assertEquals("b", history.first().blockId)
    }

    @Test
    fun records_carryTimebaseMetadata() {
        val e = engine()
        e.startTrial(0.5, null, 0L)
        val record = e.onResponse(250 * ms, isCorrect = true)
        assertEquals(Timebase.ANDROID_UI_UPTIME_NANOS, record.timebase)
        assertEquals(TimestampSource.CHOREOGRAPHER_FRAME, record.timestampSource)
        assertEquals(InputTimestampPrecision.NANOS_NATIVE, record.inputTimestampPrecision)
    }
}

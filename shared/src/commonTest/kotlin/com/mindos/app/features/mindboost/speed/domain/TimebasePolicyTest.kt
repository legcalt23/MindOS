package com.mindos.app.features.mindboost.speed.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Prueba la TimebasePolicy del §4.1 sin dispositivo (acceptance #1, #2, #5, #8, #16).
 */
class TimebasePolicyTest {

    @Test
    fun precision_isNanosNativeOnApi34Plus() {
        assertEquals(InputTimestampPrecision.NANOS_NATIVE, TimebasePolicy.precisionFor(34))
        assertEquals(InputTimestampPrecision.NANOS_NATIVE, TimebasePolicy.precisionFor(36))
    }

    @Test
    fun precision_isMillisDerivedBelowApi34() {
        assertEquals(InputTimestampPrecision.MILLIS_DERIVED, TimebasePolicy.precisionFor(33))
        assertEquals(InputTimestampPrecision.MILLIS_DERIVED, TimebasePolicy.precisionFor(29))
    }

    @Test
    fun responseNanos_usesNativeNanosOnApi34Plus() {
        val resolved = TimebasePolicy.resolveResponseUptimeNanos(
            eventTimeNanos = 5_000_000_123L,
            eventTimeMillis = 5_000L,
            apiLevel = 34,
        )
        assertEquals(5_000_000_123L, resolved)
    }

    @Test
    fun responseNanos_derivesFromMillisBelowApi34() {
        val resolved = TimebasePolicy.resolveResponseUptimeNanos(
            eventTimeNanos = null,
            eventTimeMillis = 5_000L,
            apiLevel = 30,
        )
        assertEquals(5_000L * 1_000_000L, resolved)
    }

    @Test
    fun responseNanos_fallsBackToMillisWhenNativeMissingEvenOnApi34() {
        val resolved = TimebasePolicy.resolveResponseUptimeNanos(
            eventTimeNanos = null,
            eventTimeMillis = 7L,
            apiLevel = 34,
        )
        assertEquals(7L * 1_000_000L, resolved)
    }

    @Test
    fun rtNanos_isResponseMinusStimulus() {
        assertEquals(
            150_000_000L,
            TimebasePolicy.computeRtNanos(stimulusUptimeNanos = 1_000_000_000L, responseUptimeNanos = 1_150_000_000L),
        )
    }

    @Test
    fun negativeRt_isInvalid() {
        assertFalse(TimebasePolicy.isValidRt(-1L))
        assertTrue(TimebasePolicy.isValidRt(0L))
        assertTrue(TimebasePolicy.isValidRt(1L))
    }

    @Test
    fun deviceTimingRisk_separatesHardwareSignal() {
        assertEquals(
            DeviceTimingRisk.LOW,
            TimebasePolicy.deviceTimingRisk(InputTimestampPrecision.NANOS_NATIVE, criticalFrameJitter = false),
        )
        assertEquals(
            DeviceTimingRisk.MODERATE,
            TimebasePolicy.deviceTimingRisk(InputTimestampPrecision.MILLIS_DERIVED, criticalFrameJitter = false),
        )
        assertEquals(
            DeviceTimingRisk.HIGH,
            TimebasePolicy.deviceTimingRisk(InputTimestampPrecision.NANOS_NATIVE, criticalFrameJitter = true),
        )
    }

    @Test
    fun trialTimestampSource_isChoreographerFrame() {
        assertEquals(TimestampSource.CHOREOGRAPHER_FRAME, TimebasePolicy.TRIAL_TIMESTAMP_SOURCE)
    }
}

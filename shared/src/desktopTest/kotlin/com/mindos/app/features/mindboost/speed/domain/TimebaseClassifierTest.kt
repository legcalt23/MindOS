package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.core.timing.FrameStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * INFRA-02 · classifier acceptance. Proves the DEVICE_UNRELIABLE/UNTRUSTED gate fires on simulated jank,
 * non-frame sources (Focus's MonotonicClock + desktop/iOS fallback) are never authoritative, and a clock
 * discontinuity (device-swap / time jump) is device-unreliable.
 */
class TimebaseClassifierTest {

    private val thresholds = MetricThresholds.DEFAULT
    private val clean = FrameStats.UNAVAILABLE.copy(sampledFrames = 30, frameDropFraction = 0.0)

    private fun classify(
        source: TimestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
        precision: InputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
        frame: FrameStats = clean,
        clockDiscontinuity: Boolean = false,
    ) = TimingPolicyClassifier.classify(source, precision, frame, clockDiscontinuity, thresholds)

    @Test
    fun authoritative_whenChoreographerFrame_nanosNative_lowJank() {
        val g = classify()
        assertTrue(g is TimebaseGuarantee.Authoritative)
    }

    @Test
    fun nonFrameSource_isNonAuthoritative_focusAndFallback() {
        val g = classify(source = TimestampSource.SYSTEM_CLOCK_MONOTONIC)
        assertEquals(TimebaseGuarantee.NonAuthoritative(DowngradeReason.SYSTEM_CLOCK_MONOTONIC), g)
    }

    @Test
    fun millisDerivedPrecision_isNonAuthoritative() {
        val g = classify(precision = InputTimestampPrecision.MILLIS_DERIVED)
        assertEquals(TimebaseGuarantee.NonAuthoritative(DowngradeReason.MILLIS_DERIVED), g)
    }

    @Test
    fun criticalFrameJitter_isDeviceUnreliable() {
        val g = classify(frame = clean.copy(criticalFrameJitter = true))
        assertTrue(g is TimebaseGuarantee.DeviceUnreliable)
        assertEquals(UnreliableReason.CRITICAL_FRAME_JITTER, (g as TimebaseGuarantee.DeviceUnreliable).reason)
    }

    @Test
    fun excessiveFrameDrop_isDeviceUnreliable() {
        val g = classify(frame = clean.copy(frameDropFraction = 0.10))
        assertTrue(g is TimebaseGuarantee.DeviceUnreliable)
        assertEquals(UnreliableReason.EXCESSIVE_FRAME_DROP, (g as TimebaseGuarantee.DeviceUnreliable).reason)
    }

    @Test
    fun clockDiscontinuity_isDeviceUnreliable_evenWithCleanFrames() {
        val g = classify(clockDiscontinuity = true)
        assertEquals(
            TimebaseGuarantee.DeviceUnreliable(UnreliableReason.CLOCK_DISCONTINUITY, clean),
            g,
        )
    }
}

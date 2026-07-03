package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.core.timing.FrameStats
import com.mindos.app.features.mindboost.speed.domain.TimebaseGuarantee.Authoritative
import com.mindos.app.features.mindboost.speed.domain.TimebaseGuarantee.DeviceUnreliable
import com.mindos.app.features.mindboost.speed.domain.TimebaseGuarantee.NonAuthoritative

/** Why a timebase is downgraded to non-authoritative (playable, but does not update the profile). */
enum class DowngradeReason { SYSTEM_CLOCK_MONOTONIC, MILLIS_DERIVED, NO_DISPLAY_LINK }

/** Why a timebase is judged device-unreliable (contaminated by hardware/environment). */
enum class UnreliableReason { CLOCK_DISCONTINUITY, CRITICAL_FRAME_JITTER, EXCESSIVE_FRAME_DROP }

/**
 * INFRA-02 · The timebase verdict for an RT-critical metric. Only [Authoritative] may feed an
 * authoritative RT to the profile; the other two map (via the ScoreResult state machine) to
 * `DEVICE_UNRELIABLE`/`UNTRUSTED_TIMEBASE`.
 */
sealed interface TimebaseGuarantee {
    data class Authoritative(val source: TimestampSource, val frame: FrameStats) : TimebaseGuarantee
    data class NonAuthoritative(val reason: DowngradeReason) : TimebaseGuarantee
    data class DeviceUnreliable(val reason: UnreliableReason, val frame: FrameStats) : TimebaseGuarantee
}

/**
 * Pure timebase classifier (testable without a device). Decision: `criticalFrameJitter` gates ahead of
 * the aggregate drop fraction; a non-Choreographer source (e.g. Focus's `MonotonicClock`, desktop/iOS
 * fallback) is NEVER authoritative for RT-critical metrics.
 */
object TimingPolicyClassifier {
    fun classify(
        source: TimestampSource,
        precision: InputTimestampPrecision,
        frame: FrameStats,
        clockDiscontinuity: Boolean,
        thresholds: MetricThresholds,
    ): TimebaseGuarantee = when {
        clockDiscontinuity ->
            DeviceUnreliable(UnreliableReason.CLOCK_DISCONTINUITY, frame)
        source != TimestampSource.CHOREOGRAPHER_FRAME ->
            NonAuthoritative(DowngradeReason.SYSTEM_CLOCK_MONOTONIC)
        precision == InputTimestampPrecision.MILLIS_DERIVED ->
            NonAuthoritative(DowngradeReason.MILLIS_DERIVED)
        frame.criticalFrameJitter ->
            DeviceUnreliable(UnreliableReason.CRITICAL_FRAME_JITTER, frame)
        frame.frameDropFraction > thresholds.contaminatedFrameDropThreshold ->
            DeviceUnreliable(UnreliableReason.EXCESSIVE_FRAME_DROP, frame)
        else ->
            Authoritative(source, frame)
    }
}

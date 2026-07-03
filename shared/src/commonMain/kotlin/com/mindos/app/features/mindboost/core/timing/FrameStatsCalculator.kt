package com.mindos.app.features.mindboost.core.timing

import kotlin.math.abs

/**
 * INFRA-02 · Pure frame-statistics computation, so the jank gate is testable WITHOUT a device
 * (acceptance: "jank simulado > umbral ⇒ DeviceUnreliable"). The Android probe feeds it the captured
 * `Choreographer.frameTimeNanos` stream; tests feed synthetic streams.
 */
object FrameStatsCalculator {

    /** Default nominal refresh used when the real display rate is unknown (conservative 60 Hz). */
    const val DEFAULT_REFRESH_HZ: Double = 60.0

    private const val NANOS_PER_SECOND: Double = 1_000_000_000.0

    /** A frame longer than 1.5× the nominal period counts as a drop. */
    private const val DROP_FACTOR: Double = 1.5

    /** A frame longer than 2.5× the nominal period is a clearly dropped/janky frame (critical). */
    private const val CRITICAL_FACTOR: Double = 2.5

    private const val MIN_FRAMES: Int = 2

    /** Computes [FrameStats] from consecutive frame timestamps (nanos). <2 frames ⇒ UNAVAILABLE. */
    fun fromFrameTimes(frameTimesNanos: List<Long>, refreshHz: Double = DEFAULT_REFRESH_HZ): FrameStats {
        if (frameTimesNanos.size < MIN_FRAMES) return FrameStats.UNAVAILABLE
        val nominalNanos = NANOS_PER_SECOND / refreshHz
        val deltas = frameTimesNanos.zipWithNext { a, b -> b - a }
        val drops = deltas.count { it > nominalNanos * DROP_FACTOR }
        val maxJitter = deltas.maxOf { abs(it - nominalNanos.toLong()) }
        val critical = deltas.any { it > nominalNanos * CRITICAL_FACTOR }
        return FrameStats(
            sampledFrames = deltas.size,
            frameDropFraction = drops.toDouble() / deltas.size,
            maxFrameJitterNanos = maxJitter,
            criticalFrameJitter = critical,
        )
    }
}

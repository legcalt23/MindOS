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

    /**
     * Computes [FrameStats] from consecutive frame timestamps (nanos). <2 frames ⇒ UNAVAILABLE.
     *
     * `refreshHz` is MANDATORY on purpose (`MB-FOCUS-0071`). It used to default to
     * [DEFAULT_REFRESH_HZ], which let a caller fabricate a 60 Hz reference by simply omitting the
     * argument — on a 90/120 Hz panel that turns real jank into a false negative, and it does so
     * WITHOUT naming the constant, so the per-file textual honesty guard cannot see it. Every
     * production call-site already passed it explicitly; the default was a re-fabrication vector
     * waiting for the next caller.
     */
    fun fromFrameTimes(frameTimesNanos: List<Long>, refreshHz: Double): FrameStats =
        fromFrameTimes(frameTimesNanos.toLongArray(), refreshHz)

    /**
     * Primitive overload (S5 · Paso 5, observer-effect B2). Identical semantics to the [List] one,
     * which now delegates here; it exists so a windowed probe can hand over its `LongArray` ring
     * WITHOUT boxing a frame time per sample. The boxing it removes happened at the response
     * boundary — the worst possible moment to trigger a collection the probe would then classify as
     * the user's jank.
     */
    fun fromFrameTimes(frameTimesNanos: LongArray, refreshHz: Double): FrameStats {
        // Fail-closed on too few frames OR a nonsense reference cadence (S5 · Paso 5). With NaN every
        // `delta > threshold` is false, so a NaN refresh rate used to return zero drops and no critical
        // jitter: an ABSENT input fabricating a flawless stream — the exact inversion the charter
        // forbids. Zero, negative and infinite are the same class of non-answer.
        if (frameTimesNanos.size < MIN_FRAMES || !refreshHz.isFinite() || refreshHz <= 0.0) {
            return FrameStats.UNAVAILABLE
        }
        val nominalNanos = NANOS_PER_SECOND / refreshHz
        val nominalWholeNanos = nominalNanos.toLong()
        val dropCeiling = nominalNanos * DROP_FACTOR
        val criticalCeiling = nominalNanos * CRITICAL_FACTOR
        var drops = 0
        var maxJitter = 0L
        var critical = false
        for (i in 1 until frameTimesNanos.size) {
            val delta = frameTimesNanos[i] - frameTimesNanos[i - 1]
            if (delta > dropCeiling) drops += 1
            if (delta > criticalCeiling) critical = true
            val jitter = abs(delta - nominalWholeNanos)
            if (jitter > maxJitter) maxJitter = jitter
        }
        val deltas = frameTimesNanos.size - 1
        return FrameStats(
            sampledFrames = deltas,
            frameDropFraction = drops.toDouble() / deltas,
            maxFrameJitterNanos = maxJitter,
            criticalFrameJitter = critical,
        )
    }

    /**
     * The cadence the frames were ACTUALLY delivered at, from the median inter-frame delta — a
     * different fact from the cadence the display says it runs at, and the whole point of separating
     * them (S5 · Paso 5, B4/F-R3-E). One `refreshHz` doing both jobs is how a stream pacing at half
     * the panel's rate reads either as "every frame dropped" (classify against the panel) or as
     * "flawless" (classify against itself). `null` when there is no delta to take a median of.
     *
     * Median, not mean: one long stall must not move the estimate of the steady cadence.
     */
    fun detectedRefreshHz(frameTimesNanos: LongArray): Double? {
        if (frameTimesNanos.size < MIN_FRAMES) return null
        val deltas = LongArray(frameTimesNanos.size - 1) { frameTimesNanos[it + 1] - frameTimesNanos[it] }
        deltas.sort()
        val median = deltas[deltas.size / 2]
        return if (median > 0L) NANOS_PER_SECOND / median else null
    }
}

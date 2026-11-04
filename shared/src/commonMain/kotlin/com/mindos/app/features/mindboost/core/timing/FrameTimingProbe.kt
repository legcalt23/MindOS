@file:Suppress("MagicNumber") // [0,1]/0L defaults are domain invariants for the "no measurement" record.

package com.mindos.app.features.mindboost.core.timing

import androidx.compose.runtime.Composable

/**
 * INFRA-02 · Real frame-timing of a measurement window. [criticalFrameJitter] / [frameDropFraction] are
 * the signals the device-reliability gate consumes (replacing the hardcoded `0.0`/`false`).
 */
data class FrameStats(
    val sampledFrames: Int,
    val frameDropFraction: Double,
    val maxFrameJitterNanos: Long,
    val criticalFrameJitter: Boolean,
) {
    companion object {
        /** No measurement available (fallback platforms / no probe): never authoritative, but no jank. */
        val UNAVAILABLE = FrameStats(
            sampledFrames = 0,
            frameDropFraction = 0.0,
            maxFrameJitterNanos = 0L,
            criticalFrameJitter = false,
        )
    }
}

/**
 * Measures real frame drops/jitter between [start] and [stop]. Android accumulates Choreographer
 * `frameTimeNanos`; desktop/iOS have no portable vsync source → [NoOpFrameTimingProbe] returns
 * [FrameStats.UNAVAILABLE].
 *
 * **What protects an absent measurement, corrected (`MB-FOCUS-0071`).** This KDoc used to claim the
 * absence was handled by the classifier treating `UNAVAILABLE` as non-authoritative. That was false
 * twice over: `TimingPolicyClassifier` never inspects [FrameStats.sampledFrames] — with a good source
 * and precision, `UNAVAILABLE` lands on its `Authoritative` branch, because what makes desktop/iOS
 * non-authoritative is the timestamp SOURCE, not the frame stats — and that classifier has no
 * production call-site at all. The real protection is the consumer's SAMPLE GATE: read
 * [FrameStats.sampledFrames] and refuse to derive anything from frame signals when it is `0`
 * (`SessionQualityEvaluator` for Velocidad, `TemporalClassPolicy` for focus2). A caller that reads
 * [frameDropFraction] without that check fabricates perfection from absence.
 */
interface FrameTimingProbe {
    fun start()
    fun stop(): FrameStats
}

/** Fallback probe for non-Android targets / tests: measures nothing. */
object NoOpFrameTimingProbe : FrameTimingProbe {
    override fun start() = Unit
    override fun stop(): FrameStats = FrameStats.UNAVAILABLE
}

/** Provides the platform frame-timing probe. Android = real Choreographer; desktop/iOS = no-op. */
@Composable
expect fun rememberFrameTimingProbe(): FrameTimingProbe

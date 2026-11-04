@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.mindos.app.features.mindboost.core.timing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject

/**
 * S5 iOS actual (ADR-FOCUS-TEMPORAL-QUALITY §3.1 [R2-HIST]) — CADisplayLink on the main run loop, raising the
 * structural iOS ceiling T0→T1. Declared epistemic limit, by design: CADisplayLink observes the
 * SYSTEM's refresh cadence, not the frame Compose Multiplatform's Skia/Metal loop actually
 * composited (unlike Android, where the View IS the render surface). It is a valid jank proxy for
 * window integrity, but NOT a commit anchor — so [markPresent] is a no-op and every observation
 * reports a null render marker, which selects the conservative no-marker branch of the policy.
 *
 * The per-drain refresh rate is SELF-DETECTED from the median frame delta (C.1: real Hz, never the
 * panel maximum) — on a ProMotion panel pacing at 60 Hz, classifying against the 120 Hz maximum
 * would read normal pacing as drops and exclude honest sessions (a false negative the charter
 * forbids). `timestamp` is CACurrentMediaTime-based (mach_absolute_time family); only durations and
 * counts leave this adapter (§12 [R2-HIST]).
 *
 * One self-re-registering link per [start], guarded by a GENERATION counter (same discipline as the
 * Android actual): a stale generation's callback neither records nor keeps the link alive.
 * `NSRunLoopCommonModes` so frames keep arriving during touch tracking.
 */
private class IosWindowedTemporalProbe : WindowedTemporalProbe {
    override val capability: ProbeCapability = ProbeCapability.IOS_CADISPLAYLINK
    override val clockDomain: ClockDomain = ClockDomain.IOS_CADISPLAYLINK_MACH_TIME

    private val frames = FrameTimeRing()
    private var generation = 0
    private var running = false
    private var link: CADisplayLink? = null

    private inner class FrameListener(private val boundGeneration: Int) : NSObject() {
        @ObjCAction
        fun onFrame(sender: CADisplayLink) {
            if (!running || boundGeneration != generation) {
                sender.invalidate()
                return
            }
            frames.record((sender.timestamp * NANOS_PER_SECOND).toLong())
        }
    }

    override fun start() {
        generation += 1
        frames.reset()
        running = true
        link?.invalidate()
        val fresh = CADisplayLink.displayLinkWithTarget(
            target = FrameListener(generation),
            selector = NSSelectorFromString("onFrame:"),
        )
        fresh.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
        link = fresh
    }

    /** No commit anchor exists on iOS in S5 (see class KDoc): the marker stays honestly absent. */
    override fun markPresent() = Unit

    override fun drainWindow(): TemporalWindowObservation? {
        if (!running) return null
        val drained = frames.drain()
        val times = drained.frameTimesNanos
        val detectedHz = FrameStatsCalculator.detectedRefreshHz(times)
        val stats = FrameStatsCalculator.fromFrameTimes(times, detectedHz ?: FrameStatsCalculator.DEFAULT_REFRESH_HZ)
        return TemporalWindowObservation(
            sampledFrames = stats.sampledFrames,
            droppedFrames = (stats.frameDropFraction * stats.sampledFrames).toInt(),
            criticalFrameJitter = stats.criticalFrameJitter,
            presentToFirstFrameCommitNanos = null,
            bufferOverflow = drained.overflowed,
            // NO expected cadence on iOS in S5 (Paso 5, B4). CADisplayLink reports the SYSTEM's refresh
            // cadence, not what this app's Skia/Metal loop was targeting, so there is no defensible
            // statement of what SHOULD have arrived. Self-detecting the rate and classifying against
            // it — what this adapter used to do implicitly — makes a half-rate window read as flawless,
            // which is the false negative the charter forbids. Declared absent ⇒ NotCaptured.
            expectedCadenceHz = null,
            detectedCadenceHz = detectedHz,
        )
    }

    override fun stop() {
        generation += 1
        running = false
        link?.invalidate()
        link = null
        frames.reset()
    }

    private companion object {
        const val NANOS_PER_SECOND: Double = 1_000_000_000.0
    }
}

/**
 * The [DisposableEffect] is load-bearing (Paso 5, observer-effect B3): the CADisplayLink is attached
 * to the main run loop, which outlives any composition, so without a stop bound to the composition's
 * lifetime the link keeps firing for every frame after the screen is gone.
 */
@Composable
actual fun rememberWindowedTemporalProbe(): WindowedTemporalProbe {
    val probe = remember { IosWindowedTemporalProbe() }
    DisposableEffect(probe) { onDispose { probe.stop() } }
    return probe
}

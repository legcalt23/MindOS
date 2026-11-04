package com.mindos.app.features.mindboost.core.timing

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt

/**
 * S5 Android actual (ADR-FOCUS-TEMPORAL-QUALITY §3/§5 [R2-HIST]). One self-reposting Choreographer callback per
 * [start] writing into a bounded [FrameTimeRing] (fixed 8 KiB, no allocation and no boxing per frame —
 * Paso 5 blockers B1/B2), guarded by a GENERATION counter:
 * a callback from a stale generation neither records nor re-posts (REDTEAM #22 — recomposition that
 * relaunches start without the prior stop having landed).
 *
 * Render marker: `System.nanoTime()` and `Choreographer.frameTimeNanos` share the CLOCK_MONOTONIC
 * uptime base, so `firstFrameAtOrAfter(mark) - mark` is an INTRA-domain duration (§12 [R2-HIST]: no absolute
 * timestamp leaves this adapter). Drains retain the last frame time as the anchor of the next
 * interval, so no inter-drain gap is lost.
 *
 * Main-thread confinement: all members are touched only from composition / Choreographer callbacks.
 */
private class AndroidWindowedTemporalProbe(
    /**
     * The cadence the DISPLAY STATES it runs at, or `null` when the platform cannot state one.
     *
     * `null` is a fact, not a gap to paper over (S5 · Paso 7). Substituting a nominal 60 Hz here would
     * make an ABSENT platform statement classify: on a 60 Hz panel the fabricated expectation agrees
     * with the detected cadence, the window resolves, and a session with no reference cadence gets a
     * CLEAN/Excluded verdict derived from a constant. That is the same inversion `MB-FOCUS-0065` fixed
     * on iOS, which honestly declares `expectedCadenceHz = null` — the two adapters must fail the same
     * way or the platforms are not comparable.
     */
    private val refreshHzProvider: () -> Double?,
) : WindowedTemporalProbe {
    override val capability: ProbeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER
    override val clockDomain: ClockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME

    private val frames = FrameTimeRing()
    private var generation = 0
    private var running = false
    private var pendingMarkNanos: Long? = null
    private var markerDelayNanos: Long? = null
    private var activeCallback: Choreographer.FrameCallback? = null

    override fun start() {
        generation += 1
        val gen = generation
        frames.reset()
        pendingMarkNanos = null
        markerDelayNanos = null
        running = true
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running || gen != generation) return
                record(frameTimeNanos)
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        activeCallback = callback
        Choreographer.getInstance().postFrameCallback(callback)
    }

    override fun markPresent() {
        if (running) pendingMarkNanos = System.nanoTime()
    }

    override fun drainWindow(): TemporalWindowObservation? {
        if (!running) return null
        val drained = frames.drain()
        val marker = markerDelayNanos
        markerDelayNanos = null
        // The display's STATED rate is the reference a drop is defined against; what actually arrived
        // is a separate fact the policy compares it to (Paso 5, B4). With no stated rate there is no
        // definition of "dropped", so no drop count is computed at all — reporting zero drops against
        // an absent reference is how absence fabricates a flawless window.
        val times = drained.frameTimesNanos
        val expectedHz = refreshHzProvider()
        val stats = expectedHz?.let { FrameStatsCalculator.fromFrameTimes(times, it) }
        return TemporalWindowObservation(
            // Same unit either way: `sampledFrames` counts INTER-FRAME DELTAS, so the session floor
            // accumulates the same quantity whether or not the window was classifiable.
            sampledFrames = stats?.sampledFrames ?: (times.size - 1).coerceAtLeast(0),
            // fromFrameTimes derives the fraction as drops/size, so this recovers the exact count.
            droppedFrames = stats?.let { (it.frameDropFraction * it.sampledFrames).roundToInt() } ?: 0,
            criticalFrameJitter = stats?.criticalFrameJitter ?: false,
            presentToFirstFrameCommitNanos = marker,
            bufferOverflow = drained.overflowed,
            expectedCadenceHz = expectedHz,
            detectedCadenceHz = FrameStatsCalculator.detectedRefreshHz(times),
        )
    }

    override fun stop() {
        generation += 1
        running = false
        activeCallback?.let(Choreographer.getInstance()::removeFrameCallback)
        activeCallback = null
        frames.reset()
    }

    private fun record(frameTimeNanos: Long) {
        frames.record(frameTimeNanos)
        val mark = pendingMarkNanos
        if (mark != null && frameTimeNanos >= mark) {
            markerDelayNanos = frameTimeNanos - mark
            pendingMarkNanos = null
        }
    }
}

/**
 * Real display refresh per drain via the attached View (same idiom as [rememberFrameTimingProbe]).
 *
 * The [DisposableEffect] is load-bearing (Paso 5, observer-effect B3): [start] posts a callback that
 * RE-POSTS ITSELF every frame, so without a stop tied to the composition's lifetime the probe keeps
 * waking for every vsync after the screen is gone — burning battery, holding this instance alive
 * through the Choreographer, and (worst, for a measurement device) leaving a second probe running
 * alongside the next session's. Ownership of the loop must end where the composition does.
 */
@Composable
actual fun rememberWindowedTemporalProbe(): WindowedTemporalProbe {
    val view = LocalView.current
    val probe = remember(view) {
        // No fallback constant: a detached view (or a nonsense rate) means the platform stated nothing,
        // and `null` propagates that all the way to `NoObservation`. See the provider's KDoc.
        AndroidWindowedTemporalProbe {
            view.display?.refreshRate?.toDouble()?.takeIf { it.isFinite() && it > 0.0 }
        }
    }
    DisposableEffect(probe) { onDispose { probe.stop() } }
    return probe
}

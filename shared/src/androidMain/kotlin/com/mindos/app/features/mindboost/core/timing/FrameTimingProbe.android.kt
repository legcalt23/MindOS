package com.mindos.app.features.mindboost.core.timing

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * INFRA-02 Android actual. Accumulates consecutive `Choreographer.frameTimeNanos` between [start] and
 * [stop] (same monotonic base as `SpeedTimebase.android`), delegating the drop/jitter math to the pure
 * [FrameStatsCalculator] so the logic stays device-testable. Real per-display refresh-rate detection is
 * a documented refinement; the conservative 60 Hz nominal is used for now.
 */
private class AndroidFrameTimingProbe : FrameTimingProbe {
    private val frameTimesNanos = ArrayList<Long>()
    private var running = false

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            frameTimesNanos.add(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun start() {
        frameTimesNanos.clear()
        running = true
        Choreographer.getInstance().postFrameCallback(callback)
    }

    override fun stop(): FrameStats {
        running = false
        Choreographer.getInstance().removeFrameCallback(callback)
        return FrameStatsCalculator.fromFrameTimes(frameTimesNanos.toList())
    }
}

@Composable
actual fun rememberFrameTimingProbe(): FrameTimingProbe = remember { AndroidFrameTimingProbe() }

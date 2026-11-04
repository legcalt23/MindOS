package com.mindos.app.features.mindboost.core.timing

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * INFRA-02 Android actual. Accumulates consecutive `Choreographer.frameTimeNanos` between [start] and
 * [stop] (same monotonic base as `SpeedTimebase.android`), delegating the drop/jitter math to the pure
 * [FrameStatsCalculator] so the logic stays device-testable.
 *
 * S5 · task 0 (REDTEAM #19): the nominal refresh rate is read from the attached display via
 * [refreshHzProvider], not hardcoded. On a 90/120 Hz panel a 60 Hz-hardcoded nominal made real jank a
 * false negative — frames pacing at the 60 Hz period (16.67 ms) looked smooth against a 16.67 ms nominal
 * instead of dropped against the true 8.33 ms one.
 *
 * `MB-FOCUS-0071`: that fix left a FALLBACK to the same 60 Hz constant, which reintroduced the very
 * inversion it removed whenever the platform states no rate. The provider is now nullable and the
 * fallback is gone — see [refreshHzProvider].
 */
private class AndroidFrameTimingProbe(
    /**
     * The cadence the DISPLAY STATES it runs at, or `null` when the platform cannot state one.
     *
     * `null` is a fact, not a gap to paper over. The stated rate is the reference a drop is DEFINED
     * against; with no stated rate there is no definition of "dropped", so no drop count is computed at
     * all — reporting zero drops against an absent reference is how absence fabricates a flawless
     * session. Mirrors `WindowedTemporalProbe.android.kt` (`MB-FOCUS-0069`) and the iOS adapter
     * (`MB-FOCUS-0065`), which declare the absence honestly: adapters that fail differently are not
     * comparable.
     */
    private val refreshHzProvider: () -> Double?,
) : FrameTimingProbe {
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
        // No stated cadence ⇒ no reference a drop could be measured against ⇒ no measurement at all.
        // UNAVAILABLE is the honest carrier here (`sampledFrames = 0`), and the consumer's sample gate
        // is what stops it from reading as a flawless device (SessionQualityEvaluator).
        val expectedHz = refreshHzProvider() ?: return FrameStats.UNAVAILABLE
        return FrameStatsCalculator.fromFrameTimes(frameTimesNanos.toList(), expectedHz)
    }
}

/**
 * Reads the real display refresh rate from the composition's attached [android.view.View]
 * (`View.getDisplay()` is API 17+, so valid at minSdk 29). Reports `null` — never a nominal — when the
 * view is not attached or the platform reports a non-positive rate: substituting 60 Hz there is exactly
 * the false negative `MB-FOCUS-0071` closed (on a 90/120 Hz panel, real jank pacing at the 60 Hz period
 * looks smooth against a fabricated 60 Hz nominal).
 */
@Composable
actual fun rememberFrameTimingProbe(): FrameTimingProbe {
    val view = LocalView.current
    return remember(view) {
        AndroidFrameTimingProbe { view.display?.refreshRate?.toDouble()?.takeIf { it > 0.0 } }
    }
}

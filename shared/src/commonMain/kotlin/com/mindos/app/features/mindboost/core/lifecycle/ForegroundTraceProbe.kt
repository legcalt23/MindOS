package com.mindos.app.features.mindboost.core.lifecycle

/**
 * FOC-P0-01 · The pure half of the sensor: platform adapters only report foreground transitions through
 * [onForegroundChanged]; the interval arithmetic lives here so it is testable without a device (same
 * split as `FrameStatsCalculator` / `FrameTimingProbe.android`).
 *
 * Not thread-safe by design: adapters deliver lifecycle events on the main thread, and `start`/`stop`
 * are called from the session holder on that same thread. Recording from another thread is a caller
 * defect, not a supported mode.
 *
 * @param nowNanos monotonic clock shared with the adapter's event timestamps.
 */
class ForegroundTraceProbe(private val nowNanos: () -> Long) : AppLifecycleProbe {

    private var running = false
    private var hiddenSinceNanos: Long? = null
    private val losses = ArrayList<ForegroundLoss>()

    override fun start() {
        losses.clear()
        hiddenSinceNanos = null
        running = true
    }

    /**
     * Records a foreground transition at the current instant. Events outside the window and repeats of
     * the current state are ignored, so a platform that re-emits its state cannot manufacture intervals.
     */
    fun onForegroundChanged(inForeground: Boolean) {
        if (!running) return
        val since = hiddenSinceNanos
        when {
            !inForeground && since == null -> hiddenSinceNanos = nowNanos()
            inForeground && since != null -> {
                losses += ForegroundLoss(lostAtNanos = since, regainedAtNanos = nowNanos())
                hiddenSinceNanos = null
            }
            else -> Unit // already in the reported state
        }
    }

    override fun stop(): LifecycleObservation {
        if (!running) return LifecycleObservation.NotObservable(NotObservableReason.PROBE_NOT_STARTED)
        running = false
        val stopNanos = nowNanos()
        hiddenSinceNanos?.let { losses += ForegroundLoss(lostAtNanos = it, regainedAtNanos = stopNanos) }
        hiddenSinceNanos = null
        return LifecycleObservation.Observed(losses.toList())
    }
}

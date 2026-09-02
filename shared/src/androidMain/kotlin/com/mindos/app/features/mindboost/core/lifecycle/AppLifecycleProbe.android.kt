package com.mindos.app.features.mindboost.core.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * FOC-P0-01 Android actual. Adapts the hosting Activity's lifecycle to [ForegroundTraceProbe]:
 * `ON_PAUSE` = foreground lost, `ON_RESUME` = foreground regained. Timestamps come from
 * `System.nanoTime()`, the same monotonic base as the Choreographer probe.
 *
 * `ON_PAUSE` rather than `ON_STOP` on purpose — it is the stricter boundary. A paused Activity cannot
 * receive touches (permission dialog, multi-window focus loss, full-screen call UI), so a trial that
 * spans it is compromised even if the process never went "to the background". Over-reporting an
 * interruption withholds a measurement; under-reporting publishes a fabricated one. The charter picks
 * the first.
 *
 * The observer is attached for the life of the composition and reports into the probe, which ignores
 * everything outside its own `start()`..`stop()` window; on `start()` the CURRENT state is reported so
 * a session that begins while paused counts as interrupted from its first instant.
 *
 * No new dependency: `LocalLifecycleOwner` / `LifecycleEventObserver` are already used by
 * `ProfileForegroundEffect.android.kt`.
 */
@Composable
actual fun rememberAppLifecycleProbe(): AppLifecycleProbe {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val probe = remember(lifecycle) { AndroidAppLifecycleProbe(lifecycle) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> probe.trace.onForegroundChanged(inForeground = true)
                Lifecycle.Event.ON_PAUSE -> probe.trace.onForegroundChanged(inForeground = false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return probe
}

private class AndroidAppLifecycleProbe(private val lifecycle: Lifecycle) : AppLifecycleProbe {
    val trace = ForegroundTraceProbe { System.nanoTime() }

    override fun start() {
        trace.start()
        // Report the state at the start instant: a window that begins paused is interrupted from t0.
        trace.onForegroundChanged(inForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    override fun stop(): LifecycleObservation = trace.stop()
}

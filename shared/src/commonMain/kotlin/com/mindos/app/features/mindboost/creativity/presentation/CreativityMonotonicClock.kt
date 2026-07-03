package com.mindos.app.features.mindboost.creativity.presentation

import kotlin.time.TimeSource

/**
 * Monotonic millisecond source for timing a practice round (the anti-cheat base): immune to
 * wall-clock changes. Injected so the state holder is deterministically testable.
 */
fun interface MonotonicMillis {
    fun now(): Long
}

/** Real monotonic clock. The single place that touches [TimeSource.Monotonic] for this feature. */
object SystemMonotonicMillis : MonotonicMillis {
    private val origin = TimeSource.Monotonic.markNow()
    override fun now(): Long = origin.elapsedNow().inWholeMilliseconds
}

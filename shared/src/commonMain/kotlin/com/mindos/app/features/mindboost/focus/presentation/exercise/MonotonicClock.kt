package com.mindos.app.features.mindboost.focus.presentation.exercise

import kotlin.time.TimeSource

/**
 * High-resolution monotonic clock used to measure reaction time. RT must be the
 * delta between two monotonic marks — never wall-clock (risk A1, 05_/14_).
 *
 * Multiplatform: backed by `kotlin.time.TimeSource.Monotonic`, which is available
 * in commonMain, so no expect/actual is required for the timing source itself.
 * A `fun interface` keeps it injectable so tests can supply a deterministic fake.
 */
fun interface MonotonicClock {
    /** A monotonically non-decreasing timestamp in nanoseconds. */
    fun nowNanos(): Long
}

/** Production clock. The single place that reads the platform monotonic source. */
class SystemMonotonicClock : MonotonicClock {
    private val origin = TimeSource.Monotonic.markNow()
    override fun nowNanos(): Long = origin.elapsedNow().inWholeNanoseconds
}

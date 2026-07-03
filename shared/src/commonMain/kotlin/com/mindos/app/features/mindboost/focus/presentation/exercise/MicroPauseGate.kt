package com.mindos.app.features.mindboost.focus.presentation.exercise

/**
 * The pre-response micro-pause for impulsive users (08_ §2/§6): a brief window at each stimulus onset
 * during which a tap is NOT accepted, so a precipitous response cannot register and the user is nudged
 * to inhibit before answering. It is applied ONLY in TRAINING (a training aid) — never in
 * EVALUATION/CALIBRATION, where measurement must stay honest and unaltered.
 *
 * Pure and deterministic: the runtime supplies the onset and current monotonic timestamps; this object
 * only decides whether enough time has elapsed.
 */
object MicroPauseGate {

    /** Default gate length — just above the anticipation floor, long enough to interrupt precipitation. */
    const val DEFAULT_WINDOW_MS = 200L

    /** The gate window for this run: [DEFAULT_WINDOW_MS] when enabled, otherwise 0 (no gate). */
    fun windowMs(enabled: Boolean, gateMs: Long = DEFAULT_WINDOW_MS): Long = if (enabled) gateMs else 0L

    /** A response at [nowNanos] is accepted only once [windowMs] has elapsed since [onsetNanos]. */
    fun accepts(onsetNanos: Long, nowNanos: Long, windowMs: Long): Boolean =
        (nowNanos - onsetNanos) >= windowMs * NANOS_PER_MS

    private const val NANOS_PER_MS = 1_000_000L
}

package com.mindos.app.features.mindboost.core.lifecycle

import androidx.compose.runtime.Composable

/**
 * FOC-P0-01 · One interval during which the session screen could NOT receive input, in the probe's
 * monotonic nanosecond base. [regainedAtNanos] is clamped to the stop instant when the window ends
 * while still hidden, so `regainedAtNanos == stop` is the "ended in background" case.
 */
data class ForegroundLoss(val lostAtNanos: Long, val regainedAtNanos: Long)

/** Why a platform could not observe the foreground. Typed, so absence never travels as an empty list. */
enum class NotObservableReason {
    /** This target has no foreground/background source wired (desktop, iOS until ported). */
    NO_PLATFORM_SOURCE,

    /** `stop()` was called on a probe that never `start()`ed — a caller defect surfaced as data. */
    PROBE_NOT_STARTED,
}

/**
 * What the lifecycle sensor saw between `start()` and `stop()`.
 *
 * Two facts that the charter forbids collapsing (`null != 0`): [Observed] with an empty list means
 * "we watched the whole window and the screen stayed in the foreground"; [NotObservable] means "we
 * could not watch". A consumer that treats the second as the first reads a session whose middle
 * minute was a phone call as a clean one — that is `MB-FOCUS-0091`'s route into the profile.
 *
 * There is deliberately no default, no `Boolean`, and no `EMPTY` constant: the only way to obtain an
 * `Observed` is from a probe that actually ran.
 */
sealed interface LifecycleObservation {
    data class Observed(val interruptions: List<ForegroundLoss>) : LifecycleObservation

    data class NotObservable(val reason: NotObservableReason) : LifecycleObservation
}

/**
 * Watches whether the hosting screen keeps the foreground while a session runs. Mirrors the
 * `FrameTimingProbe` seam: `start()` at session start, `stop()` at session end, and the platform
 * decides what it can honestly claim.
 *
 * "Foreground" here means the screen can receive input. The Android adapter maps it to the Activity's
 * `ON_PAUSE`/`ON_RESUME`, which is the stricter boundary: a paused Activity (permission dialog,
 * multi-window focus loss, incoming-call UI) cannot deliver touches, so any trial spanning it is
 * compromised whether or not the process went "to the background" in the OS sense.
 */
interface AppLifecycleProbe {
    fun start()
    fun stop(): LifecycleObservation
}

/**
 * The probe for targets without a foreground source. It never returns [LifecycleObservation.Observed]:
 * an empty observation from a sensor that does not exist is exactly the fabricated absence this file
 * exists to make impossible.
 */
object NoOpAppLifecycleProbe : AppLifecycleProbe {
    override fun start() = Unit
    override fun stop(): LifecycleObservation =
        LifecycleObservation.NotObservable(NotObservableReason.NO_PLATFORM_SOURCE)
}

/** Provides the platform lifecycle probe. Android = Activity lifecycle; desktop/iOS = [NoOpAppLifecycleProbe]. */
@Composable
expect fun rememberAppLifecycleProbe(): AppLifecycleProbe

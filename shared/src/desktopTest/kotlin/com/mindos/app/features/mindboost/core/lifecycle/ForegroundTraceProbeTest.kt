package com.mindos.app.features.mindboost.core.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * FOC-P0-01 · The pure half of the lifecycle sensor. The platform adapter only feeds
 * [ForegroundTraceProbe.onForegroundChanged]; every interval below is computed here, so the arithmetic
 * that decides whether a session was interrupted is testable without a device.
 *
 * Invariant under test (charter, `null != 0`): a probe that RAN always answers [LifecycleObservation.Observed]
 * — an empty list means "watched the whole window, nothing happened", which is a fact. Absence of a
 * sensor is a different fact and is never encoded as an empty list (see [AppLifecycleProbeContractTest]).
 */
class ForegroundTraceProbeTest {

    private class FakeClock(var nowNanos: Long) {
        fun tick(nanos: Long) {
            nowNanos += nanos
        }
    }

    private fun probe(clock: FakeClock) = ForegroundTraceProbe { clock.nowNanos }

    @Test
    fun uninterruptedWindowIsObservedWithNoLoss() {
        val clock = FakeClock(1_000)
        val probe = probe(clock)
        probe.start()
        clock.tick(5_000)

        assertEquals(LifecycleObservation.Observed(emptyList()), probe.stop())
    }

    @Test
    fun oneLossAndRegainProducesOneIntervalWithExactBounds() {
        val clock = FakeClock(1_000)
        val probe = probe(clock)
        probe.start()
        clock.tick(2_000)
        probe.onForegroundChanged(inForeground = false) // 3_000
        clock.tick(500)
        probe.onForegroundChanged(inForeground = true) // 3_500
        clock.tick(1_000)

        assertEquals(
            LifecycleObservation.Observed(listOf(ForegroundLoss(lostAtNanos = 3_000, regainedAtNanos = 3_500))),
            probe.stop(),
        )
    }

    @Test
    fun lossStillOpenAtStopIsClampedToTheStopInstant() {
        val clock = FakeClock(1_000)
        val probe = probe(clock)
        probe.start()
        clock.tick(2_000)
        probe.onForegroundChanged(inForeground = false) // 3_000
        clock.tick(1_500) // stop at 4_500, still not in foreground

        assertEquals(
            LifecycleObservation.Observed(listOf(ForegroundLoss(lostAtNanos = 3_000, regainedAtNanos = 4_500))),
            probe.stop(),
        )
    }

    @Test
    fun startingWhileNotInForegroundCountsFromTheStartInstant() {
        // The adapter reports the initial state right after start(); a window that BEGINS hidden is an
        // interruption from its first nanosecond, not a clean window with a late event.
        val clock = FakeClock(1_000)
        val probe = probe(clock)
        probe.start()
        probe.onForegroundChanged(inForeground = false) // 1_000
        clock.tick(700)
        probe.onForegroundChanged(inForeground = true) // 1_700
        clock.tick(300)

        assertEquals(
            LifecycleObservation.Observed(listOf(ForegroundLoss(lostAtNanos = 1_000, regainedAtNanos = 1_700))),
            probe.stop(),
        )
    }

    @Test
    fun repeatedEventsOfTheSameKindAreIdempotent() {
        val clock = FakeClock(0)
        val probe = probe(clock)
        probe.start()
        clock.tick(10)
        probe.onForegroundChanged(inForeground = false) // 10
        clock.tick(10)
        probe.onForegroundChanged(inForeground = false) // 20 — duplicate, must not open a second interval
        clock.tick(10)
        probe.onForegroundChanged(inForeground = true) // 30
        clock.tick(10)
        probe.onForegroundChanged(inForeground = true) // 40 — duplicate, must not close anything twice

        assertEquals(
            LifecycleObservation.Observed(listOf(ForegroundLoss(lostAtNanos = 10, regainedAtNanos = 30))),
            probe.stop(),
        )
    }

    @Test
    fun eventsOutsideTheWindowAreIgnored() {
        val clock = FakeClock(0)
        val probe = probe(clock)
        probe.onForegroundChanged(inForeground = false) // before start(): not our window
        clock.tick(10)
        probe.start() // 10
        clock.tick(10)
        val observed = probe.stop() // 20
        probe.onForegroundChanged(inForeground = false) // after stop(): must not mutate the returned value

        assertEquals(LifecycleObservation.Observed(emptyList()), observed)
    }

    @Test
    fun stopWithoutStartIsAbsenceWithATypedReason_neverAnEmptyObservation() {
        val probe = probe(FakeClock(0))

        assertEquals(
            LifecycleObservation.NotObservable(NotObservableReason.PROBE_NOT_STARTED),
            probe.stop(),
        )
    }

    @Test
    fun aStartedProbeAlwaysObserves_discriminantHalf() {
        // Without this, an implementation that returned NotObservable for everything would pass the
        // contract test and the whole sensor would be a no-op wearing a type.
        val clock = FakeClock(0)
        val probe = probe(clock)
        probe.start()
        clock.tick(1)

        assertIs<LifecycleObservation.Observed>(probe.stop())
    }
}

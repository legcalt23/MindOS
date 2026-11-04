package com.mindos.app.features.mindboost.core.timing

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * S5 · Paso 5 (F-R3-D / observer-effect B1-B2) — the probe's buffer is bounded, keeps the newest
 * frames, and REPORTS when it had to overwrite.
 *
 * The overflow fact is the point of the whole class: an unbounded `ArrayList` never overflowed, it
 * just grew, so an incomplete sample was indistinguishable from a complete one. Every rejection test
 * here has its discriminating half (the same ring, one frame under the cap, reporting no overflow),
 * or "bounded" would just mean "always suspicious".
 */
class FrameTimeRingTest {

    @Test
    fun drainReturnsFramesInChronologicalOrder() {
        val ring = FrameTimeRing(capacity = 8)
        listOf(10L, 20L, 30L).forEach(ring::record)

        val drained = ring.drain()

        assertContentEquals(longArrayOf(10L, 20L, 30L), drained.frameTimesNanos)
        assertFalse(drained.overflowed)
    }

    @Test
    fun drainRetainsTheLastFrameAsTheAnchorOfTheNextInterval() {
        // Without the anchor, the gap BETWEEN two windows would vanish: the first delta of the next
        // interval would be measured from its own second frame, silently hiding a stall that spans
        // the boundary.
        val ring = FrameTimeRing(capacity = 8)
        listOf(10L, 20L, 30L).forEach(ring::record)
        ring.drain()

        ring.record(500L)
        val next = ring.drain()

        assertContentEquals(longArrayOf(30L, 500L), next.frameTimesNanos)
    }

    @Test
    fun aRingFilledExactlyToCapacityDoesNotReportOverflow() {
        // DISCRIMINATING HALF: the boundary is strict, so "overflowed" means data was actually lost.
        val ring = FrameTimeRing(capacity = 4)
        repeat(4) { ring.record(it.toLong()) }

        val drained = ring.drain()

        assertFalse(drained.overflowed, "exactly capacity frames fit without overwriting anything")
        assertEquals(4, drained.frameTimesNanos.size)
    }

    @Test
    fun exceedingCapacityReportsOverflowAndKeepsTheNewestFrames() {
        val ring = FrameTimeRing(capacity = 4)
        repeat(6) { ring.record(it.toLong()) }

        val drained = ring.drain()

        assertTrue(drained.overflowed, "6 frames into a 4-frame ring overwrote two")
        // Newest kept (not oldest): the anchor for the next interval must be the true last frame,
        // otherwise the ring would fabricate a stall at every boundary after an overflow.
        assertContentEquals(longArrayOf(2L, 3L, 4L, 5L), drained.frameTimesNanos)
    }

    @Test
    fun theOverflowFactBelongsToItsOwnInterval() {
        val ring = FrameTimeRing(capacity = 4)
        repeat(6) { ring.record(it.toLong()) }
        assertTrue(ring.drain().overflowed)

        ring.record(99L)
        assertFalse(ring.drain().overflowed, "a clean interval must not inherit the previous overflow")
    }

    @Test
    fun resetDropsTheAnchorAndTheOverflowFact() {
        val ring = FrameTimeRing(capacity = 4)
        repeat(6) { ring.record(it.toLong()) }

        ring.reset()
        ring.record(7L)
        val drained = ring.drain()

        assertContentEquals(longArrayOf(7L), drained.frameTimesNanos)
        assertFalse(drained.overflowed)
    }

    @Test
    fun aCapacityTooSmallToYieldADeltaIsRejected() {
        assertFailsWith<IllegalArgumentException> { FrameTimeRing(capacity = 1) }
    }

    @Test
    fun theDrainedArrayFeedsTheCalculatorWithoutChangingItsSemantics() {
        // The primitive overload exists to remove per-frame boxing (B2); it must classify a stream
        // exactly as the List overload did, or the de-boxing would have silently re-tuned the gate.
        val frameNanos60Hz = 16_666_667L
        val ring = FrameTimeRing(capacity = 64)
        repeat(30) { ring.record(it * frameNanos60Hz) }

        val viaArray = FrameStatsCalculator.fromFrameTimes(ring.drain().frameTimesNanos, refreshHz = 60.0)
        val viaList = FrameStatsCalculator.fromFrameTimes((0 until 30).map { it * frameNanos60Hz }, refreshHz = 60.0)

        assertEquals(viaList, viaArray)
    }
}

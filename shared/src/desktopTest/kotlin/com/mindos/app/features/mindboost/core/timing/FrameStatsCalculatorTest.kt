package com.mindos.app.features.mindboost.core.timing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** INFRA-02 · the jank gate is testable without a device: synthetic frame streams → FrameStats. */
class FrameStatsCalculatorTest {

    private val frameNanos60Hz = 16_666_667L // ~1/60 s

    @Test
    fun fewerThanTwoFrames_isUnavailable() {
        assertEquals(FrameStats.UNAVAILABLE, FrameStatsCalculator.fromFrameTimes(emptyList()))
        assertEquals(FrameStats.UNAVAILABLE, FrameStatsCalculator.fromFrameTimes(listOf(0L)))
    }

    @Test
    fun smoothStream_hasNoDrops_andNoCriticalJitter() {
        val frames = (0 until 30).map { it * frameNanos60Hz }
        val stats = FrameStatsCalculator.fromFrameTimes(frames)
        assertEquals(29, stats.sampledFrames)
        assertEquals(0.0, stats.frameDropFraction)
        assertFalse(stats.criticalFrameJitter)
    }

    @Test
    fun jankyStream_overThreshold_flagsDropsAndCriticalJitter() {
        // 4 deltas: three smooth, one ~3.6× nominal (60 ms) → 1 drop (0.25) + critical.
        val frames = listOf(0L, frameNanos60Hz, 2 * frameNanos60Hz, 2 * frameNanos60Hz + 60_000_000L, 3 * frameNanos60Hz + 60_000_000L)
        val stats = FrameStatsCalculator.fromFrameTimes(frames)
        assertTrue(stats.frameDropFraction > 0.05, "drop fraction ${stats.frameDropFraction} should exceed threshold")
        assertTrue(stats.criticalFrameJitter, "a 3.6x-nominal frame is critical jitter")
    }
}

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
        assertEquals(FrameStats.UNAVAILABLE, FrameStatsCalculator.fromFrameTimes(emptyList(), refreshHz = 60.0))
        assertEquals(FrameStats.UNAVAILABLE, FrameStatsCalculator.fromFrameTimes(listOf(0L), refreshHz = 60.0))
    }

    @Test
    fun smoothStream_hasNoDrops_andNoCriticalJitter() {
        val frames = (0 until 30).map { it * frameNanos60Hz }
        val stats = FrameStatsCalculator.fromFrameTimes(frames, refreshHz = 60.0)
        assertEquals(29, stats.sampledFrames)
        assertEquals(0.0, stats.frameDropFraction)
        assertFalse(stats.criticalFrameJitter)
    }

    @Test
    fun jankyStream_overThreshold_flagsDropsAndCriticalJitter() {
        // 4 deltas: three smooth, one ~3.6× nominal (60 ms) → 1 drop (0.25) + critical.
        val frames = listOf(0L, frameNanos60Hz, 2 * frameNanos60Hz, 2 * frameNanos60Hz + 60_000_000L, 3 * frameNanos60Hz + 60_000_000L)
        val stats = FrameStatsCalculator.fromFrameTimes(frames, refreshHz = 60.0)
        assertTrue(stats.frameDropFraction > 0.05, "drop fraction ${stats.frameDropFraction} should exceed threshold")
        assertTrue(stats.criticalFrameJitter, "a 3.6x-nominal frame is critical jitter")
    }

    @Test
    fun sameStream_isClassifiedDifferently_at60Hz_and120Hz() {
        // REDTEAM #19 (S5 · task 0): the display's REAL refresh rate must feed the classifier. A stream
        // pacing at the 60 Hz period (16.67 ms frames) is perfectly smooth on a 60 Hz panel, but on a
        // 120 Hz panel those same frames are each ~2x the 8.33 ms period — i.e. every one is a dropped
        // frame. Hardcoding 60 Hz would make real jank on a high-refresh display a FALSE NEGATIVE.
        val frames = (0 until 30).map { it * frameNanos60Hz }

        val at60 = FrameStatsCalculator.fromFrameTimes(frames, refreshHz = 60.0)
        val at120 = FrameStatsCalculator.fromFrameTimes(frames, refreshHz = 120.0)

        assertEquals(0.0, at60.frameDropFraction, "16.67 ms frames are smooth against a 60 Hz nominal")
        assertEquals(1.0, at120.frameDropFraction, "the same frames are all drops against a 120 Hz nominal")
        assertTrue(
            at120.maxFrameJitterNanos > at60.maxFrameJitterNanos,
            "jitter against the nominal period grows when the true rate is 120 Hz",
        )
        // The device-reliability gate consumes frameDropFraction, so the verdict itself flips with the Hz.
    }

    @Test
    fun aNonsenseRefreshRateIsUnavailable_neverAFlawlessStream() {
        // S5 · Paso 5. With NaN as the nominal period every `delta > threshold` comparison is false,
        // so the old code answered "zero drops, no critical jitter" — an ABSENT reference cadence
        // fabricating a perfect stream. Zero, negative and infinite are the same non-answer.
        val janky = listOf(0L, 60_000_000L, 200_000_000L)
        for (bad in listOf(Double.NaN, 0.0, -60.0, Double.POSITIVE_INFINITY)) {
            assertEquals(
                FrameStats.UNAVAILABLE,
                FrameStatsCalculator.fromFrameTimes(janky, refreshHz = bad),
                "refreshHz=$bad must fail closed",
            )
        }
        // DISCRIMINATING HALF: a sane rate still classifies the same stream as janky.
        assertTrue(FrameStatsCalculator.fromFrameTimes(janky, refreshHz = 60.0).criticalFrameJitter)
    }

    @Test
    fun detectedRefreshHzReportsWhatActuallyArrived_notWhatWasExpected() {
        val paced60 = LongArray(30) { it * frameNanos60Hz }
        val detected = FrameStatsCalculator.detectedRefreshHz(paced60)
        assertTrue(detected != null && kotlin.math.abs(detected - 60.0) < 0.1, "expected ~60 Hz, got $detected")

        // One long stall must not move the estimate of the steady cadence (median, not mean).
        val withStall = LongArray(30) { if (it < 29) it * frameNanos60Hz else 29 * frameNanos60Hz + 2_000_000_000L }
        val stillSixty = FrameStatsCalculator.detectedRefreshHz(withStall)
        assertTrue(stillSixty != null && kotlin.math.abs(stillSixty - 60.0) < 0.1, "expected ~60 Hz, got $stillSixty")

        assertEquals(null, FrameStatsCalculator.detectedRefreshHz(longArrayOf(42L)), "one frame yields no delta")
    }

    @Test
    fun theCriticalJitterFactorIsPinnedBehaviourally_at2_5xNominal() {
        // S5 · Paso 5 (F-R3-L / ADR §R3-18). `criticalJitterFactor` is the DECLARED NON-VERSIONED
        // EXCEPTION: it is not a field of `TemporalQualityThresholds` because it is a multiple of the
        // platform's nominal frame period and this calculator is shared with the Speed lane. Unversioned
        // does NOT mean unpinned — a silent change here would re-classify results in BOTH lanes with no
        // version bump to detect it, so the boundary is pinned by behaviour rather than by exposing the
        // constant. 2.4x nominal is a drop but not critical; 2.6x is critical.
        val justUnder = FrameStatsCalculator.fromFrameTimes(listOf(0L, (frameNanos60Hz * 2.4).toLong()), refreshHz = 60.0)
        val justOver = FrameStatsCalculator.fromFrameTimes(listOf(0L, (frameNanos60Hz * 2.6).toLong()), refreshHz = 60.0)

        assertFalse(justUnder.criticalFrameJitter, "2.4x nominal must stay below the critical factor")
        assertEquals(1.0, justUnder.frameDropFraction, "2.4x nominal is still a dropped frame")
        assertTrue(justOver.criticalFrameJitter, "2.6x nominal must cross the critical factor")
    }
}

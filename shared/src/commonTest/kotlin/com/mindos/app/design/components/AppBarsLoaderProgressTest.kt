package com.mindos.app.design.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure three-bar keyframe function [calculateBarProgress],
 * verifying it maps the reference CSS keyframe faithfully (rest 0f .. peak 1f)
 * including the per-bar delay phase-shift and the flat rest plateau.
 */
class AppBarsLoaderProgressTest {

    private val tolerance = 1e-4f

    @Test
    fun rests_at_cycle_start() {
        assertEquals(0f, calculateBarProgress(elapsedFraction = 0f, delayFraction = 0f), tolerance)
    }

    @Test
    fun peaks_at_forty_percent() {
        assertEquals(1f, calculateBarProgress(elapsedFraction = 0.4f, delayFraction = 0f), tolerance)
    }

    @Test
    fun rests_again_at_eighty_percent() {
        assertEquals(0f, calculateBarProgress(elapsedFraction = 0.8f, delayFraction = 0f), tolerance)
    }

    @Test
    fun stays_flat_through_the_rest_plateau() {
        // Everything in (0.8, 1.0) is the flat rest — no second bump.
        assertEquals(0f, calculateBarProgress(elapsedFraction = 0.9f, delayFraction = 0f), tolerance)
        assertEquals(0f, calculateBarProgress(elapsedFraction = 0.99f, delayFraction = 0f), tolerance)
    }

    @Test
    fun delay_shifts_the_peak_by_its_fraction() {
        // The center bar (delay 0.2) peaks 0.2 later than the left bar.
        assertEquals(1f, calculateBarProgress(elapsedFraction = 0.6f, delayFraction = 0.2f), tolerance)
        assertEquals(0f, calculateBarProgress(elapsedFraction = 0.2f, delayFraction = 0.2f), tolerance)
    }

    @Test
    fun delay_wraps_around_the_cycle() {
        // elapsed 0.1 with delay 0.3 -> local phase (0.1 - 0.3) mod 1 = 0.8 -> rest.
        assertEquals(0f, calculateBarProgress(elapsedFraction = 0.1f, delayFraction = 0.3f), tolerance)
    }

    @Test
    fun intensity_stays_within_unit_range_across_the_cycle() {
        var t = 0f
        while (t < 1f) {
            val v = calculateBarProgress(elapsedFraction = t, delayFraction = 0f)
            assertTrue(v in 0f..1f, "intensity out of range at t=$t -> $v")
            t += 0.01f
        }
    }

    @Test
    fun rises_monotonically_from_rest_to_peak() {
        val early = calculateBarProgress(elapsedFraction = 0.1f, delayFraction = 0f)
        val mid = calculateBarProgress(elapsedFraction = 0.2f, delayFraction = 0f)
        val late = calculateBarProgress(elapsedFraction = 0.35f, delayFraction = 0f)
        assertTrue(early < mid, "expected rise: $early < $mid")
        assertTrue(mid < late, "expected rise: $mid < $late")
    }
}

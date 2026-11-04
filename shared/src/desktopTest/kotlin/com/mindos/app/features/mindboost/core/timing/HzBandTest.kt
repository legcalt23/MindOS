package com.mindos.app.features.mindboost.core.timing

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §14 [R2-HIST] — banding golden (privacy criterio 5): nearby detections land
 * in the SAME persisted band (no float fingerprint), and non-standard cadences collapse to OTHER.
 */
class HzBandTest {

    @Test
    fun nearbyDetections_landInTheSameBand() {
        assertEquals(HzBand.HZ_120, HzBand.of(119.4))
        assertEquals(HzBand.HZ_120, HzBand.of(120.6))
        assertEquals(HzBand.HZ_60, HzBand.of(59.94))
        assertEquals(HzBand.HZ_90, HzBand.of(90.0))
        assertEquals(HzBand.HZ_144, HzBand.of(144.5))
        assertEquals(HzBand.HZ_240, HzBand.of(240.0))
    }

    @Test
    fun nonStandardCadences_collapseToOther() {
        assertEquals(HzBand.OTHER, HzBand.of(500.0))
        assertEquals(HzBand.OTHER, HzBand.of(30.0))
        assertEquals(HzBand.OTHER, HzBand.of(72.0))
        assertEquals(HzBand.OTHER, HzBand.of(0.0))
    }
}

package com.mindos.app.features.mindboost.memory.domain.reporting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositeIndicesTest {

    @Test
    fun learningVelocity_needsTrend() {
        assertNull(CompositeIndices.learningVelocity(slope = 0.5, n = 2).value)
        val ok = CompositeIndices.learningVelocity(slope = 0.5, n = 5)
        assertEquals(0.5, ok.value)
        assertTrue(ok.confident)
    }

    @Test
    fun retentionQuality_combinesRatioAndForgetting() {
        // delayed/immediate = 0.6/0.8 = 0.75; ×(1-0.2)=0.6
        val r = CompositeIndices.retentionQuality(immediate = 0.8, delayed = 0.6, forgettingRate = 0.2, n = 10)
        assertEquals(0.6, r.value!!, 1e-9)
        assertTrue(r.confident)
    }

    @Test
    fun retentionQuality_unavailableWhenNoImmediate() {
        val r = CompositeIndices.retentionQuality(immediate = 0.0, delayed = 0.5, forgettingRate = 0.1, n = 10)
        assertNull(r.value)
    }

    @Test
    fun resilience_combinesInterferenceAndPressure() {
        // (1-0.3)*0.8 = 0.56
        val r = CompositeIndices.resilience(interferenceIndex = 0.3, accuracyUnderPressure = 0.8, n = 10)
        assertEquals(0.56, r.value!!, 1e-9)
    }

    @Test
    fun calibrationQuality_combinesCorrAndBias() {
        // 0.9*(1-0.2)=0.72
        val r = CompositeIndices.calibrationQuality(confidenceAccuracyCorrelation = 0.9, bias = -0.2, n = 10)
        assertEquals(0.72, r.value!!, 1e-9)
    }

    @Test
    fun smallSample_isNotConfident() {
        assertFalse(CompositeIndices.resilience(0.2, 0.8, n = 2).confident)
    }
}

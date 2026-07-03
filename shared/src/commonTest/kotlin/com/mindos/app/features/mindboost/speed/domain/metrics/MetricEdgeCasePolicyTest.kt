package com.mindos.app.features.mindboost.speed.domain.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cubre la tabla MetricEdgeCasePolicy del §7 (acceptance #7/#8/#14). La regla maestra:
 * ninguna métrica nula se convierte silenciosamente en 0.
 */
class MetricEdgeCasePolicyTest {

    private val ms = 1_000_000.0

    @Test
    fun proportionCorrect_withZeroValid_isZeroNotNull() {
        assertEquals(0.0, MetricEdgeCasePolicy.proportionCorrect(correctCount = 0, validCount = 0))
    }

    @Test
    fun proportionCorrect_computesFraction() {
        assertEquals(0.5, MetricEdgeCasePolicy.proportionCorrect(correctCount = 2, validCount = 4))
    }

    @Test
    fun coefficientOfVariation_nullWhenSdOrMeanMissing() {
        assertNull(MetricEdgeCasePolicy.coefficientOfVariation(sd = null, mean = 100.0))
        assertNull(MetricEdgeCasePolicy.coefficientOfVariation(sd = 10.0, mean = null))
        assertNull(MetricEdgeCasePolicy.coefficientOfVariation(sd = 10.0, mean = 0.0))
    }

    @Test
    fun coefficientOfVariation_computesSdOverMean() {
        assertEquals(0.1, MetricEdgeCasePolicy.coefficientOfVariation(sd = 10.0, mean = 100.0))
    }

    @Test
    fun falseAlarmRate_nullWithoutNoGo() {
        // 0 trials no-go ⇒ null (no aplicable), NUNCA 0.0.
        assertNull(MetricEdgeCasePolicy.falseAlarmRate(falseAlarms = 0, noGoTrials = 0))
    }

    @Test
    fun falseAlarmRate_computesWhenNoGoExists() {
        assertEquals(0.25, MetricEdgeCasePolicy.falseAlarmRate(falseAlarms = 1, noGoTrials = 4))
    }

    @Test
    fun omissionRate_zeroWhenNoPresentedTrials() {
        assertEquals(0.0, MetricEdgeCasePolicy.omissionRate(omissions = 0, presentedTrials = 0))
    }

    @Test
    fun fastErrorShare_zeroWhenNoErrors() {
        // 0 errores ⇒ 0.0 (definido), aunque no haya mediana de aciertos.
        assertEquals(0.0, MetricEdgeCasePolicy.fastErrorShare(errorRtsNanos = emptyList(), hitMedianNanos = null))
    }

    @Test
    fun fastErrorShare_nullWhenErrorsButNoHitMedian() {
        assertNull(MetricEdgeCasePolicy.fastErrorShare(errorRtsNanos = listOf(200 * ms), hitMedianNanos = null))
    }

    @Test
    fun fastErrorShare_fractionBelowHitMedian() {
        val errors = listOf(150 * ms, 400 * ms, 100 * ms) // 2 de 3 por debajo de 300 ms
        assertEquals(2.0 / 3.0, MetricEdgeCasePolicy.fastErrorShare(errors, hitMedianNanos = 300 * ms))
    }

    @Test
    fun lapseRate_zeroWithoutHits() {
        assertEquals(0.0, MetricEdgeCasePolicy.lapseRate(rawHitRtsNanos = emptyList(), lapseMultiplier = 2.5))
    }

    @Test
    fun lapseRate_countsAboveMultiplierTimesMedian() {
        // mediana = 200 ms; umbral = 500 ms; un trial (600) lo supera de 5.
        val hits = listOf(180 * ms, 200 * ms, 210 * ms, 220 * ms, 600 * ms)
        assertEquals(1.0 / 5.0, MetricEdgeCasePolicy.lapseRate(hits, lapseMultiplier = 2.5))
    }

    @Test
    fun impulsivityIndexNoNoGo_nullWhenFastErrorShareNull() {
        assertNull(MetricEdgeCasePolicy.impulsivityIndexNoNoGo(anticipationRate = 0.2, fastErrorShare = null))
    }

    @Test
    fun impulsivityIndexNoNoGo_usesRenormalizedWeights() {
        // 0.6*0.2 + 0.4*0.5 = 0.12 + 0.20 = 0.32
        assertEquals(0.32, MetricEdgeCasePolicy.impulsivityIndexNoNoGo(anticipationRate = 0.2, fastErrorShare = 0.5))
    }

    @Test
    fun vce_nullWhenPcZero() {
        assertNull(MetricEdgeCasePolicy.vce(pc = 0.0, rtMedianNanos = 300 * ms, cv = 0.1))
    }

    @Test
    fun vce_nullWhenMedianNull() {
        assertNull(MetricEdgeCasePolicy.vce(pc = 0.9, rtMedianNanos = null, cv = 0.1))
    }

    @Test
    fun vce_nullWhenCvNull() {
        assertNull(MetricEdgeCasePolicy.vce(pc = 0.9, rtMedianNanos = 300 * ms, cv = null))
    }

    @Test
    fun vce_computesFormula() {
        // pc^2 / (rtSeconds * (1 + 0.7*cv)) = 0.81 / (0.3 * 1.07) = 2.5234...
        val vce = MetricEdgeCasePolicy.vce(pc = 0.9, rtMedianNanos = 300 * ms, cv = 0.1)
        assertNotNull(vce)
        assertTrue(vce in 2.52..2.53, "VCE was $vce")
    }
}

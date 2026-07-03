// El helper signals() espeja QualitySignals (7 campos) para legibilidad de los tests.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain.quality

import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.QualityReason
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionQualityEvaluatorTest {

    private val evaluator = SessionQualityEvaluator()

    private fun signals(
        totalTrials: Int = 24,
        cv: Double? = 0.12,
        anticipationRate: Double = 0.02,
        omissionRate: Double = 0.0,
        precision: InputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
        frameDrop: Double = 0.0,
        jitter: Boolean = false,
        source: TimestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
    ) = QualitySignals(totalTrials, cv, anticipationRate, omissionRate, precision, frameDrop, jitter, source)

    @Test
    fun cleanRun_onNanosNativeDevice_isHigh() {
        val decision = evaluator.evaluate(signals())
        assertEquals(SessionQuality.HIGH, decision.sessionQuality)
        assertEquals(DeviceTimingRisk.LOW, decision.deviceTimingRisk)
    }

    @Test
    fun frameDropOverThreshold_isContaminatedWithReason() {
        val decision = evaluator.evaluate(signals(frameDrop = 0.10))
        assertEquals(SessionQuality.CONTAMINATED, decision.sessionQuality)
        assertTrue(QualityReason.CONTAMINATED_SYSTEM_NOISE in decision.reasons)
    }

    @Test
    fun criticalJitter_isContaminatedAndHighDeviceRisk() {
        val decision = evaluator.evaluate(signals(jitter = true))
        assertEquals(SessionQuality.CONTAMINATED, decision.sessionQuality)
        assertEquals(DeviceTimingRisk.HIGH, decision.deviceTimingRisk)
        assertTrue(QualityReason.FRAME_JITTER_DETECTED in decision.reasons)
    }

    @Test
    fun highAnticipationRate_isLowWithReason() {
        val decision = evaluator.evaluate(signals(anticipationRate = 0.40))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.HIGH_ANTICIPATION_RATE in decision.reasons)
    }

    @Test
    fun highCv_isLowWithReason() {
        val decision = evaluator.evaluate(signals(cv = 0.50))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.HIGH_CV in decision.reasons)
    }

    @Test
    fun lowTrialCount_isLowWithReason() {
        val decision = evaluator.evaluate(signals(totalTrials = 3))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.LOW_TRIAL_COUNT in decision.reasons)
    }

    @Test
    fun millisDerived_isAcceptableNotHigh_withInformationalReason() {
        val decision = evaluator.evaluate(signals(precision = InputTimestampPrecision.MILLIS_DERIVED))
        assertEquals(SessionQuality.ACCEPTABLE, decision.sessionQuality)
        assertEquals(DeviceTimingRisk.MODERATE, decision.deviceTimingRisk)
        assertTrue(QualityReason.INPUT_PRECISION_MILLIS_DERIVED in decision.reasons)
    }

    @Test
    fun nonFrameSource_isAcceptableNotHigh_evenOnNanosNativeDevice() {
        // A non-Choreographer onset (desktop/iOS monotonic fallback) is non-authoritative for RT: even
        // native-nanos precision cannot reach HIGH — the source, not just the precision, gates HIGH.
        val decision = evaluator.evaluate(signals(source = TimestampSource.SYSTEM_CLOCK_MONOTONIC))
        assertEquals(SessionQuality.ACCEPTABLE, decision.sessionQuality)
        assertTrue(QualityReason.TIMESTAMP_SOURCE_NON_FRAME in decision.reasons)
    }
}

package com.mindos.app.features.arena

import com.mindos.app.features.arena.integrity.engine.FraudSignalEvaluator
import com.mindos.app.features.arena.integrity.model.IntegritySignal
import com.mindos.app.features.arena.integrity.model.IntegritySignalFeatures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 05 / P5.1: Fraud-signal detectors over synthetic features. */
class FraudSignalEvaluatorTest {

    private val evaluator = FraudSignalEvaluator()

    @Test
    fun cleanFeaturesTriggerNoSignals() {
        val signals = evaluator.evaluate(IntegritySignalFeatures())
        assertTrue(!signals.any)
        assertEquals(emptySet(), signals.triggered)
    }

    @Test
    fun smurfRequiresEarlyHighWinRateAndVolatility() {
        val smurf = evaluator.evaluate(
            IntegritySignalFeatures(earlyWinRate = 0.95, muVolatility = 120.0, earlySessionCount = 5),
        )
        assertTrue(smurf.smurf)
        // High win rate alone (late account, low volatility) does not trigger.
        val notSmurf = evaluator.evaluate(
            IntegritySignalFeatures(earlyWinRate = 0.95, muVolatility = 10.0, earlySessionCount = 500),
        )
        assertTrue(!notSmurf.smurf)
    }

    @Test
    fun boostingTriggersOnImpossibleTravelOrCadencePlusDeviceChange() {
        assertTrue(evaluator.evaluate(IntegritySignalFeatures(geoVelocityKmh = 2000.0)).boosting)
        assertTrue(
            evaluator.evaluate(
                IntegritySignalFeatures(inputCadenceMismatch = 0.9, deviceIdChanged = true),
            ).boosting,
        )
        assertTrue(!evaluator.evaluate(IntegritySignalFeatures(inputCadenceMismatch = 0.9)).boosting)
    }

    @Test
    fun accountSharingTriggersOnTravelOrFingerprintPlusInputShift() {
        assertTrue(evaluator.evaluate(IntegritySignalFeatures(geoVelocityKmh = 1500.0)).accountSharing)
        assertTrue(
            evaluator.evaluate(
                IntegritySignalFeatures(deviceFingerprintChanged = true, inputPatternShift = 0.8),
            ).accountSharing,
        )
    }

    @Test
    fun collusionRequiresRepeatedOpponentsAndCoordinatedTiming() {
        assertTrue(
            evaluator.evaluate(
                IntegritySignalFeatures(repeatedOpponentRatio = 0.8, coordinatedQueueTiming = true),
            ).collusion,
        )
        assertTrue(!evaluator.evaluate(IntegritySignalFeatures(repeatedOpponentRatio = 0.8)).collusion)
    }

    @Test
    fun lagSwitchTriggersOnJitterSpikeDuringLoss() {
        assertTrue(evaluator.evaluate(IntegritySignalFeatures(jitterSpikeDuringLoss = true)).lagSwitch)
    }

    @Test
    fun triggeredSetReflectsAllFiredSignals() {
        val signals = evaluator.evaluate(
            IntegritySignalFeatures(
                geoVelocityKmh = 2000.0,
                repeatedOpponentRatio = 0.9,
                coordinatedQueueTiming = true,
                jitterSpikeDuringLoss = true,
            ),
        )
        assertTrue(signals.any)
        assertEquals(
            setOf(IntegritySignal.BOOSTING, IntegritySignal.ACCOUNT_SHARING, IntegritySignal.COLLUSION, IntegritySignal.LAG_SWITCH),
            signals.triggered,
        )
    }
}

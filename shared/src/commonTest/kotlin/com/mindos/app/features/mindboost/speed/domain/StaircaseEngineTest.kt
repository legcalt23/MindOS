package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StaircaseEngineTest {

    private val ctx = SpeedTestFixtures.context

    private fun bm(pc: Double, vce: Double? = 2.0): BlockMetrics = BlockMetrics(
        blockId = "b",
        totalTrials = 20,
        pc = pc,
        rtMedianNanos = 300.0 * SpeedTestFixtures.MS,
        rtMeanNanos = 300.0 * SpeedTestFixtures.MS,
        cv = 0.1,
        lapseRate = 0.0,
        anticipationRate = 0.0,
        faRate = null,
        omissionRate = 0.0,
        impIdx = 0.1,
        fatIdx = null,
        vce = vce,
        timestampSource = ctx.timestampSource,
        inputTimestampPrecision = ctx.inputTimestampPrecision,
        timebase = ctx.timebase,
    )

    @Test
    fun highAccuracy_raisesDifficulty() {
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        val next = engine.updateDifficulty(bm(pc = 0.95))
        assertTrue(next > 0.5, "CCE debe subir con pc alta; fue $next")
    }

    @Test
    fun lowAccuracy_lowersDifficulty() {
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        val next = engine.updateDifficulty(bm(pc = 0.60))
        assertTrue(next < 0.5, "CCE debe bajar con pc baja; fue $next")
    }

    @Test
    fun midAccuracy_holdsDifficulty() {
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        val next = engine.updateDifficulty(bm(pc = 0.78))
        assertEquals(0.5, next)
    }

    @Test
    fun zeroCorrect_pcZero_lowersDifficulty_rule2() {
        // §7: 0 trials correctos ⇒ pc=0.0 ⇒ el staircase usa solo PC para BAJAR.
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        val next = engine.updateDifficulty(bm(pc = 0.0))
        assertTrue(next < 0.5)
    }

    @Test
    fun vce_isIgnored_sameePcSameDecision_acceptance6() {
        val a = DefaultStaircaseEngine(initialCce = 0.5).updateDifficulty(bm(pc = 0.95, vce = 0.1))
        val b = DefaultStaircaseEngine(initialCce = 0.5).updateDifficulty(bm(pc = 0.95, vce = 99.0))
        assertEquals(a, b, "VCE no debe influir en la decisión del staircase")
    }

    @Test
    fun cce_clampedToUpperBound() {
        val engine = DefaultStaircaseEngine(initialCce = 0.95)
        repeat(10) { engine.updateDifficulty(bm(pc = 1.0)) }
        assertEquals(1.0, engine.currentCce())
    }

    @Test
    fun cce_clampedToLowerBound() {
        val engine = DefaultStaircaseEngine(initialCce = 0.05)
        repeat(10) { engine.updateDifficulty(bm(pc = 0.0)) }
        assertEquals(0.0, engine.currentCce())
    }

    @Test
    fun deadline_isShorterAtHigherCce() {
        val engine = DefaultStaircaseEngine()
        val config = StaircaseConfig.DEFAULT
        assertEquals(config.maxDeadlineNanos, engine.getNextDeadline(0.0))
        assertEquals(config.minDeadlineNanos, engine.getNextDeadline(1.0))
        assertTrue(engine.getNextDeadline(0.8)!! < engine.getNextDeadline(0.2)!!)
    }

    @Test
    fun reset_restoresInitialCce() {
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        engine.updateDifficulty(bm(pc = 1.0))
        engine.reset()
        assertEquals(0.5, engine.currentCce())
    }

    @Test
    fun config_rejectsInvalidThresholds() {
        assertFailsWith<IllegalArgumentException> { StaircaseConfig(accuracyHigh = 0.5, accuracyLow = 0.7) }
        assertFailsWith<IllegalArgumentException> {
            StaircaseConfig(minDeadlineNanos = 2_000_000_000L, maxDeadlineNanos = 1_000_000_000L)
        }
    }
}

// bm() es un fixture con muchos campos para cubrir todas las señales del staircase.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiSignalStaircaseEngineTest {

    private val ctx = SpeedTestFixtures.context
    private val ms = SpeedTestFixtures.MS

    private fun bm(
        pc: Double,
        impIdx: Double? = 0.1,
        cv: Double? = 0.1,
        anticipationRate: Double = 0.0,
        omissionRate: Double = 0.0,
        vce: Double? = 2.0,
        rtMedianMs: Double = 300.0,
        lapseRate: Double = 0.0,
    ): BlockMetrics = BlockMetrics(
        blockId = "b",
        totalTrials = 20,
        pc = pc,
        rtMedianNanos = rtMedianMs * ms,
        rtMeanNanos = rtMedianMs * ms,
        cv = cv,
        lapseRate = lapseRate,
        anticipationRate = anticipationRate,
        faRate = null,
        omissionRate = omissionRate,
        impIdx = impIdx,
        fatIdx = null,
        vce = vce,
        timestampSource = ctx.timestampSource,
        inputTimestampPrecision = ctx.inputTimestampPrecision,
        timebase = ctx.timebase,
    )

    @Test
    fun raise_whenAccurateStableNonImpulsive() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        val decision = engine.evaluate(bm(pc = 0.95, impIdx = 0.1, cv = 0.1))
        assertEquals(StaircaseDirective.RAISE, decision.directive)
        assertTrue(decision.cce > 0.5)
    }

    @Test
    fun lower_whenImpulsiveEvenIfAccurate() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        val decision = engine.evaluate(bm(pc = 0.95, impIdx = 0.50))
        assertEquals(StaircaseDirective.LOWER, decision.directive)
        assertTrue(decision.cce < 0.5)
    }

    @Test
    fun lower_whenHighAnticipation() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        assertEquals(StaircaseDirective.LOWER, engine.evaluate(bm(pc = 0.95, anticipationRate = 0.30)).directive)
    }

    @Test
    fun lower_whenHighOmission() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        assertEquals(StaircaseDirective.LOWER, engine.evaluate(bm(pc = 0.95, omissionRate = 0.30)).directive)
    }

    @Test
    fun lower_whenLowAccuracy() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        assertEquals(StaircaseDirective.LOWER, engine.evaluate(bm(pc = 0.50)).directive)
    }

    @Test
    fun stabilize_whenAccurateButUnstable() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        val decision = engine.evaluate(bm(pc = 0.95, impIdx = 0.1, cv = 0.40))
        assertEquals(StaircaseDirective.STABILIZE, decision.directive)
        assertEquals(0.5, decision.cce, "ESTABILIZAR no sube ni baja la dificultad")
    }

    @Test
    fun hold_inMidAccuracyBand() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        val decision = engine.evaluate(bm(pc = 0.78))
        assertEquals(StaircaseDirective.HOLD, decision.directive)
        assertEquals(0.5, decision.cce)
    }

    @Test
    fun nullImpIdxAndCv_doNotTriggerTheirGates() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        // pc alta, impIdx y cv nulos ⇒ ni LOWER (impIdx) ni STABILIZE (cv) ⇒ RAISE.
        assertEquals(StaircaseDirective.RAISE, engine.evaluate(bm(pc = 0.95, impIdx = null, cv = null)).directive)
    }

    @Test
    fun cooldown_whenFatigueRisesOverThreeBlocks() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.6)
        engine.evaluate(bm(pc = 0.9, vce = 3.0, rtMedianMs = 300.0, lapseRate = 0.0))
        engine.evaluate(bm(pc = 0.9, vce = 3.0, rtMedianMs = 300.0, lapseRate = 0.0))
        val cceBeforeCooldown = engine.currentCce()
        // Tercer bloque marcadamente fatigado: VCE cae, TR sube, lapsos suben.
        val decision = engine.evaluate(bm(pc = 0.9, vce = 1.0, rtMedianMs = 900.0, lapseRate = 0.30))
        assertEquals(StaircaseDirective.COOLDOWN, decision.directive)
        assertTrue(decision.cce < cceBeforeCooldown, "COOLDOWN reduce la carga respecto al bloque previo")
    }

    @Test
    fun vce_isIgnored_acceptance6() {
        val a = MultiSignalStaircaseEngine(initialCce = 0.5).evaluate(bm(pc = 0.95, vce = 0.01))
        val b = MultiSignalStaircaseEngine(initialCce = 0.5).evaluate(bm(pc = 0.95, vce = 999.0))
        assertEquals(a.directive, b.directive)
        assertEquals(a.cce, b.cce)
    }

    @Test
    fun reset_clearsHistoryAndCce() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        repeat(3) { engine.evaluate(bm(pc = 0.9, vce = 1.0, rtMedianMs = 900.0, lapseRate = 0.3)) }
        engine.reset()
        assertEquals(0.5, engine.currentCce())
        // Tras reset, un solo bloque no puede disparar COOLDOWN (fatIdx null con <3 bloques).
        assertTrue(engine.evaluate(bm(pc = 0.9)).directive != StaircaseDirective.COOLDOWN)
    }
}

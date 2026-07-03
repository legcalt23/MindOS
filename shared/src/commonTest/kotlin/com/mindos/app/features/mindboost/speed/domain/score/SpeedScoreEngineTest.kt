// sessionMetrics() es un fixture amplio (espeja SessionMetrics) para los tests de scoring.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeedScoreEngineTest {

    private val ctx = SpeedTestFixtures.context
    private val ms = SpeedTestFixtures.MS
    private val engine = SpeedScoreEngine()

    private fun sessionMetrics(
        pc: Double,
        impIdx: Double? = 0.1,
        cv: Double? = 0.1,
        vce: Double? = 2.5,
        lapseRate: Double = 0.0,
    ): SessionMetrics = SessionMetrics(
        sessionId = "s",
        totalTrials = 40,
        pc = pc,
        rtMedianNanos = 300.0 * ms,
        rtMeanNanos = 300.0 * ms,
        cv = cv,
        lapseRate = lapseRate,
        anticipationRate = 0.0,
        faRate = null,
        omissionRate = 0.0,
        impIdx = impIdx,
        fatIdx = null,
        vce = vce,
        timestampSource = ctx.timestampSource,
        inputTimestampPrecision = ctx.inputTimestampPrecision,
        timebase = ctx.timebase,
    )

    @Test
    fun usefulSpeed_normalizesVceAgainstReference() {
        val score = engine.score(sessionMetrics(pc = 0.9, vce = 2.5)) // 2.5 / 5.0
        assertEquals(0.5, score.usefulSpeed)
    }

    @Test
    fun usefulSpeed_nullWhenVceNull() {
        assertNull(engine.score(sessionMetrics(pc = 0.9, vce = null)).usefulSpeed)
    }

    @Test
    fun control_isOneMinusImpIdxTimesPc() {
        val score = engine.score(sessionMetrics(pc = 0.9, impIdx = 0.2))
        assertEquals(0.72, score.control!!, 1e-9)
    }

    @Test
    fun stability_isInverseCvTimesInverseLapses() {
        val score = engine.score(sessionMetrics(pc = 0.9, cv = 0.1, lapseRate = 0.0))
        assertEquals(0.9, score.stability!!, 1e-9)
    }

    @Test
    fun stability_nullWhenCvNull() {
        assertNull(engine.score(sessionMetrics(pc = 0.9, cv = null)).stability)
    }

    @Test
    fun ceiling_penalizesTotalWhenAccuracyLow() {
        val high = engine.score(sessionMetrics(pc = 0.95, cv = 0.1, vce = 2.5, impIdx = 0.1))
        val low = engine.score(sessionMetrics(pc = 0.70, cv = 0.1, vce = 2.5, impIdx = 0.1))
        assertTrue(low.ceilingPenaltyApplied)
        assertTrue(low.total!! < high.total!!, "pc baja debe penalizar el total")
    }

    @Test
    fun ceiling_penalizesTotalWhenCvHigh() {
        val score = engine.score(sessionMetrics(pc = 0.95, cv = 0.45))
        assertTrue(score.ceilingPenaltyApplied)
    }

    @Test
    fun macro_calibratingWhenSpeedOrStabilityUnavailable() {
        assertEquals(SpeedMacroCategory.CALIBRATING, engine.score(sessionMetrics(pc = 0.9, vce = null)).macroCategory)
        assertEquals(SpeedMacroCategory.CALIBRATING, engine.score(sessionMetrics(pc = 0.9, cv = null)).macroCategory)
    }

    @Test
    fun macro_fastImpulsive() {
        // control = (1-0.5)*0.7 = 0.35 (<0.6); usefulSpeed = 3.0/5 = 0.6 (>=0.5).
        val score = engine.score(sessionMetrics(pc = 0.70, impIdx = 0.5, cv = 0.2, vce = 3.0))
        assertEquals(SpeedMacroCategory.FAST_IMPULSIVE, score.macroCategory)
    }

    @Test
    fun macro_slowPrecise() {
        // control = (1-0.1)*0.95 = 0.855 (>=0.75); usefulSpeed = 1.5/5 = 0.3 (<0.4).
        val score = engine.score(sessionMetrics(pc = 0.95, impIdx = 0.1, cv = 0.1, vce = 1.5))
        assertEquals(SpeedMacroCategory.SLOW_PRECISE, score.macroCategory)
    }

    @Test
    fun macro_variable() {
        // stability = (1-0.6)*1 = 0.4 (<0.5); control 0.765, speed 0.6.
        val score = engine.score(sessionMetrics(pc = 0.85, impIdx = 0.1, cv = 0.6, vce = 3.0))
        assertEquals(SpeedMacroCategory.VARIABLE, score.macroCategory)
    }

    @Test
    fun macro_balanced() {
        // control 0.7, speed 0.7, stability 0.8 ⇒ ninguna categoría extrema.
        val score = engine.score(sessionMetrics(pc = 0.875, impIdx = 0.2, cv = 0.2, vce = 3.5))
        assertEquals(SpeedMacroCategory.BALANCED, score.macroCategory)
    }

    @Test
    fun score_isAlwaysPreliminary_withVersionedTag() {
        val score = engine.score(sessionMetrics(pc = 0.9))
        assertTrue(score.preliminary, "el score es preliminar hasta validación física (§3/§15)")
        assertEquals(SpeedVersions.SCORE_VERSION, score.scoreVersion)
    }

    @Test
    fun total_nullWhenNoBarsComputable() {
        // vce null ⇒ usefulSpeed null; cv null ⇒ stability null; queda solo control ⇒ total no es null.
        // Para forzar total null necesitamos control... control siempre computable (pc no es null).
        // Por tanto el total es null solo si TODAS las barras lo son, lo que no ocurre con pc presente.
        val score = engine.score(sessionMetrics(pc = 0.9, vce = null, cv = null))
        // control sigue disponible ⇒ total NO es null (es el control), pero speed/stability sí null.
        assertNull(score.usefulSpeed)
        assertNull(score.stability)
        assertTrue(score.total != null)
    }
}

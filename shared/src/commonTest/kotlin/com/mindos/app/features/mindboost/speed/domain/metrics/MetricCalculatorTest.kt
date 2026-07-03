package com.mindos.app.features.mindboost.speed.domain.metrics

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.SpeedTestFixtures.MS
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetricCalculatorTest {

    private val calc = MetricCalculator()
    private val ctx = SpeedTestFixtures.context

    private fun hit(i: Int, ms: Long, anticipation: Boolean = false) =
        SpeedTestFixtures.trial("b", i, ms * MS, isCorrect = !anticipation, isAnticipation = anticipation)

    private fun error(i: Int, ms: Long) =
        SpeedTestFixtures.trial("b", i, ms * MS, isCorrect = false)

    @Test
    fun zeroTrials_blockDiscardedAsNull() {
        assertNull(calc.blockMetrics("b", emptyList(), ctx))
    }

    @Test
    fun zeroCorrect_pcZero_rtAndCvAndVceNull() {
        val trials = listOf(error(0, 300), error(1, 320))
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertEquals(0.0, m.pc)
        assertNull(m.rtMedianNanos)
        assertNull(m.cv)
        assertNull(m.vce)
    }

    @Test
    fun oneCorrect_rtMedianIsThatTrial_cvAndVceNull() {
        val trials = listOf(hit(0, 280), error(1, 320))
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertEquals(280.0 * MS, m.rtMedianNanos)
        assertNull(m.cv) // n=1 ⇒ no computable
        assertNull(m.vce) // cv null ⇒ vce null
    }

    @Test
    fun allAnticipations_treatedAsZeroCorrect() {
        val trials = listOf(hit(0, 60, anticipation = true), hit(1, 70, anticipation = true))
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertEquals(0.0, m.pc)
        assertNull(m.rtMedianNanos)
        assertEquals(1.0, m.anticipationRate)
    }

    @Test
    fun faRate_nullForE1E6_noNoGo() {
        val trials = (0 until 6).map { hit(it, 300) }
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertNull(m.faRate)
    }

    @Test
    fun lapsesCountedOnRawRts_centralTendencyUsesCleanRts() {
        // 600 ms es un lapso (cola lenta) y un outlier MAD: cuenta como lapso pero NO
        // contamina la mediana (CLAUDE.md 2026-06-04).
        val trials = listOf(hit(0, 200), hit(1, 210), hit(2, 205), hit(3, 195), hit(4, 600))
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertEquals(1.0 / 5.0, m.lapseRate)
        // Mediana de los 4 limpios [195,200,205,210] = 202.5 ms (600 excluido por MAD).
        assertEquals(202.5 * MS, m.rtMedianNanos)
    }

    @Test
    fun vce_isComputedAndPersistedWhenInputsValid() {
        val trials = listOf(hit(0, 300), hit(1, 310), hit(2, 295), hit(3, 305), hit(4, 300), hit(5, 290))
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertEquals(1.0, m.pc)
        assertNotNull(m.vce, "VCE debe calcularse cuando pc>0 y cv es computable")
        assertTrue(m.vce!! > 0.0)
    }

    @Test
    fun impIdx_usesNoGoRenormalization() {
        // 2 anticipaciones + 2 aciertos + 1 error rápido ⇒ impIdx computable y no nulo.
        val trials = listOf(
            hit(0, 60, anticipation = true),
            hit(1, 70, anticipation = true),
            hit(2, 300),
            hit(3, 310),
            error(4, 150),
        )
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertNotNull(m.impIdx)
        assertEquals(0.4, m.anticipationRate) // 2 de 5 trials
    }

    @Test
    fun blockMetrics_carryTimebaseMetadata() {
        val trials = listOf(hit(0, 300))
        val m = assertNotNull(calc.blockMetrics("b", trials, ctx))
        assertEquals(ctx.timestampSource, m.timestampSource)
        assertEquals(ctx.inputTimestampPrecision, m.inputTimestampPrecision)
        assertEquals(ctx.timebase, m.timebase)
    }

    @Test
    fun fatIdx_nullWithFewerThanThreeBlocks() {
        val trials = (0 until 6).map { hit(it, 300) }
        val s = calc.sessionMetrics("s", trials, orderedBlockMetrics = twoBlockSeries(), context = ctx)
        assertNull(s.fatIdx)
    }

    @Test
    fun fatIdx_computableWithThreeOrMoreBlocks() {
        val trials = (0 until 6).map { hit(it, 300) }
        val s = calc.sessionMetrics("s", trials, orderedBlockMetrics = threeBlockFatigueSeries(), context = ctx)
        assertNotNull(s.fatIdx)
    }

    private fun blockMetricStub(blockId: String, rtMs: Double, cv: Double, lapse: Double, vce: Double): BlockMetrics =
        BlockMetrics(
            blockId = blockId,
            totalTrials = 10,
            pc = 0.9,
            rtMedianNanos = rtMs * MS,
            rtMeanNanos = rtMs * MS,
            cv = cv,
            lapseRate = lapse,
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

    private fun twoBlockSeries(): List<BlockMetrics> = listOf(
        blockMetricStub("b0", 300.0, 0.1, 0.0, 3.0),
        blockMetricStub("b1", 305.0, 0.1, 0.0, 2.9),
    )

    // VCE cae, TR sube y lapsos suben a lo largo de 3 bloques ⇒ patrón de fatiga.
    private fun threeBlockFatigueSeries(): List<BlockMetrics> = listOf(
        blockMetricStub("b0", 300.0, 0.10, 0.00, 3.0),
        blockMetricStub("b1", 360.0, 0.18, 0.05, 2.4),
        blockMetricStub("b2", 470.0, 0.30, 0.18, 1.6),
    )
}

package com.mindos.action.matching

import com.mindos.testkit.BenchmarkHarness
import com.mindos.testkit.CorpusBuilder
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Benchmark + regression tracking (ZIP04 PERFORMANCE_HARNESS). Targets P50<5ms / P95<20ms /
 * P99<50ms. The committed baseline below is the regression gate: a large generous budget that
 * still catches gross (e.g. O(N)) regressions. The printed line is the real signal; update the
 * baseline deliberately when the engine legitimately changes.
 */
class BenchmarkRegressionTest {

    private companion object {
        const val BASELINE_P99_MICROS = 80.0
        const val TOLERANCE_PCT = 400.0 // budget = 400us, far above the ~10-15us we observe
    }

    @Test
    fun understandLatencyHasNotRegressed() {
        val inputs = CorpusBuilder(Reliability.grammar, Reliability.aliases).build(2000, seed = 7).map { it.text }

        val result = BenchmarkHarness.measure("understand", warmup = 3000, iterations = 6000) { i ->
            Reliability.matcher.understand(inputs[i % inputs.size])
        }
        println(result.describe())

        val regression = BenchmarkHarness.regression(result, BASELINE_P99_MICROS, TOLERANCE_PCT)
        println(regression.describe())
        assertFalse(regression.regressed, regression.describe())
    }
}

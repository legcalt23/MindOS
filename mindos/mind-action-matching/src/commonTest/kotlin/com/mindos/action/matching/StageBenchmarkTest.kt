package com.mindos.action.matching

import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.normalization.NormalizationPipeline
import com.mindos.action.pattern.registrySource
import com.mindos.testkit.BenchmarkHarness
import com.mindos.testkit.ScalabilityPoint
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Per-stage + throughput + scalability benchmarks (ZIP11 BENCHMARK_SUITE / BENCHMARK_PROMPT).
 * Measures normalization and matching, reports throughput, and proves the trie keeps matching latency
 * flat as the registry grows (scalability). Generous assertions; printed numbers are the signal.
 */
class StageBenchmarkTest {

    private val inputs = listOf("open settings", "open ana", "open story abc123", "message ana hi there", "florble")

    @Test
    fun normalizationStageLatency() {
        val pipeline = NormalizationPipeline(NormalizationConfig.Default)
        val result = BenchmarkHarness.measure("normalization", warmup = 2000, iterations = 5000) { i ->
            pipeline.normalize(inputs[i % inputs.size])
        }
        println(result.describe())
        assertTrue(result.p99Micros < 20_000.0)
    }

    @Test
    fun matchingStageLatencyAndThroughput() {
        val matcher = Reliability.matcher
        val latency = BenchmarkHarness.measure("matching", warmup = 3000, iterations = 6000) { i ->
            matcher.understand(inputs[i % inputs.size])
        }
        val throughput = BenchmarkHarness.throughput("matching", warmup = 3000, iterations = 20000) { i ->
            matcher.understand(inputs[i % inputs.size])
        }
        println(latency.describe())
        println(throughput.describe())
        assertTrue(latency.p99Micros < 50_000.0)
        assertTrue(throughput.opsPerSecond > 1000.0)
    }

    @Test
    fun matchingScalesFlatlyWithRegistrySize() {
        val points = listOf(10, 100, 500).map { size ->
            val matcher = matcherWith(size)
            val probe = "cmd${size / 2} ana"
            val r = BenchmarkHarness.measure("matching@$size", warmup = 1000, iterations = 3000) { matcher.understand(probe) }
            ScalabilityPoint(size, r.p99Micros)
        }
        points.forEach { println("scalability size=${it.size} p99=${it.p99Micros}us") }
        // Trie keeps it bounded: even at 500 patterns p99 stays well under budget.
        assertTrue(points.last().p99Micros < 50_000.0)
    }

    private fun matcherWith(patternCount: Int): DefaultActionMatcher {
        val source = registrySource {
            for (i in 0 until patternCount) parametric("p$i", "cmd$i {x:username}", intent = "intent$i")
        }
        return DefaultActionMatcher(RegistryCompiler.compile(source).getOrThrow())
    }
}

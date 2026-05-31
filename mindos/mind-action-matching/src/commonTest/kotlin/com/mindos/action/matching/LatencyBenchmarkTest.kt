package com.mindos.action.matching

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Smoke benchmark (not JMH). Measures per-request latency percentiles to keep the design
 * honest against the P50<5ms / P95<20ms / P99<50ms targets. The hard assertion is generous
 * (P99 < 50ms) to avoid CI flakiness; the printed line is the real signal.
 */
class LatencyBenchmarkTest {

    @Test
    fun latencyPercentilesAreWithinBudget() {
        val matcher = TestRegistries.matcher()
        val inputs = listOf(
            "open settings",
            "ajustes",
            "open ana",
            "open story aB3xZ9kQ",
            "message ana hola que tal",
            "florble",
        )

        repeat(3000) { matcher.understand(inputs[it % inputs.size]) } // warm up JIT

        val n = 5000
        val samples = LongArray(n)
        for (i in 0 until n) {
            val mark = TimeSource.Monotonic.markNow()
            matcher.understand(inputs[i % inputs.size])
            samples[i] = mark.elapsedNow().inWholeNanoseconds
        }
        samples.sort()
        val p50 = samples[n / 2] / 1000.0
        val p95 = samples[(n * 95) / 100] / 1000.0
        val p99 = samples[(n * 99) / 100] / 1000.0
        println("MindOS matcher latency (micros): p50=$p50 p95=$p95 p99=$p99")

        assertTrue(p99 < 50_000.0, "p99 ${p99}us exceeded the 50ms budget")
    }
}

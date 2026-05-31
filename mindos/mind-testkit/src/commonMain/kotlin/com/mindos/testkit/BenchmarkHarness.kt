package com.mindos.testkit

import kotlin.time.TimeSource

/** Latency percentiles (microseconds) for one benchmarked operation. */
data class BenchmarkResult(
    val label: String,
    val samples: Int,
    val p50Micros: Double,
    val p95Micros: Double,
    val p99Micros: Double,
) {
    fun describe(): String = "$label: p50=${p50Micros}us p95=${p95Micros}us p99=${p99Micros}us (n=$samples)"
}

/** Throughput measurement (operations per second). */
data class ThroughputResult(
    val label: String,
    val opsPerSecond: Double,
    val totalMicros: Long,
    val ops: Int,
) {
    fun describe(): String = "$label: ${opsPerSecond.toLong()} ops/s ($ops ops in ${totalMicros}us)"
}

/** One scalability data point: p99 latency at a given input/registry size. */
data class ScalabilityPoint(val size: Int, val p99Micros: Double)

/** Outcome of comparing a fresh result against a committed baseline. */
data class RegressionReport(
    val label: String,
    val currentP99Micros: Double,
    val baselineP99Micros: Double,
    val tolerancePct: Double,
) {
    val budgetMicros: Double get() = baselineP99Micros * (1.0 + tolerancePct / 100.0)
    val regressed: Boolean get() = currentP99Micros > budgetMicros

    fun describe(): String =
        "$label: p99=${currentP99Micros}us vs baseline=${baselineP99Micros}us " +
            "(+${tolerancePct}% budget=${budgetMicros}us) -> ${if (regressed) "REGRESSION" else "ok"}"
}

/**
 * Deterministic micro-benchmark harness with regression tracking (ZIP04 PERFORMANCE_HARNESS).
 * Computes p50/p95/p99 over a warmed-up run and compares against a committed baseline. Pure
 * (kotlin.time only) — persistence/CI wiring lives in the consuming test.
 */
object BenchmarkHarness {

    inline fun measure(label: String, warmup: Int, iterations: Int, block: (Int) -> Unit): BenchmarkResult {
        require(iterations > 0) { "iterations must be > 0" }
        repeat(warmup) { block(it) }
        val samples = LongArray(iterations)
        for (i in 0 until iterations) {
            val mark = TimeSource.Monotonic.markNow()
            block(i)
            samples[i] = mark.elapsedNow().inWholeNanoseconds
        }
        samples.sort()
        return BenchmarkResult(
            label = label,
            samples = iterations,
            p50Micros = percentile(samples, 50) / 1000.0,
            p95Micros = percentile(samples, 95) / 1000.0,
            p99Micros = percentile(samples, 99) / 1000.0,
        )
    }

    fun regression(result: BenchmarkResult, baselineP99Micros: Double, tolerancePct: Double): RegressionReport =
        RegressionReport(result.label, result.p99Micros, baselineP99Micros, tolerancePct)

    /** Throughput in operations/second over a warmed-up run (ZIP11 BENCHMARK_PROMPT). */
    inline fun throughput(label: String, warmup: Int, iterations: Int, block: (Int) -> Unit): ThroughputResult {
        require(iterations > 0) { "iterations must be > 0" }
        repeat(warmup) { block(it) }
        val mark = TimeSource.Monotonic.markNow()
        for (i in 0 until iterations) block(i)
        val micros = mark.elapsedNow().inWholeMicroseconds.coerceAtLeast(1)
        return ThroughputResult(label, iterations / (micros / 1_000_000.0), micros, iterations)
    }

    fun percentile(sortedNanos: LongArray, p: Int): Long {
        if (sortedNanos.isEmpty()) return 0
        val index = ((p.toLong() * (sortedNanos.size - 1)) / 100L).toInt()
        return sortedNanos[index]
    }
}

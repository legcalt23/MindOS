package com.mindos.action.execution

import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Execution-stage benchmark (ZIP11 BENCHMARK_SUITE: "measure … execution"). Measures the full
 * validate → resolve → navigate pipeline. Uses real `runBlocking` (not virtual time) for accurate
 * latency. Generous budget; the printed p99 is the signal.
 */
class ExecutionBenchmarkTest {

    @Test
    fun executionPipelineLatency() = runBlocking {
        val coordinator = FakeCoordinator()
        val executor = ExecReliability.executor(coordinator)
        val action = MindAction.OpenProfile(RequestId("bench"), "ana")

        repeat(3000) { executor.execute(action) } // warm up

        val n = 6000
        val samples = LongArray(n)
        for (i in 0 until n) {
            val mark = TimeSource.Monotonic.markNow()
            executor.execute(action)
            samples[i] = mark.elapsedNow().inWholeNanoseconds
        }
        samples.sort()
        val p50 = samples[n / 2] / 1000.0
        val p99 = samples[(n * 99) / 100] / 1000.0
        println("execution pipeline latency (micros): p50=$p50 p99=$p99")

        assertTrue(p99 < 50_000.0, "execution p99 ${p99}us exceeded 50ms budget")
    }
}

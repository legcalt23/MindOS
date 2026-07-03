package com.mindos.app.features.arena

import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SyntheticIdMapper
import com.mindos.app.features.arena.simulation.generators.PopulationConfig
import com.mindos.app.features.arena.simulation.runner.DashboardExporter
import com.mindos.app.features.arena.simulation.runner.SimulationRunConfig
import com.mindos.app.features.arena.simulation.runner.SimulationRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.measureTime

/** Phase 07 / P7.3: Simulation runner determinism, metric sanity, 100k throughput, sanitized export. */
class SimulationRunnerTest {

    private val runner = SimulationRunner()

    private fun config(seed: Long, size: Int = 500, sessions: Int = 20) =
        SimulationRunConfig(masterSeed = seed, population = PopulationConfig(size = size), sessionsPerPlayer = sessions)

    @Test
    fun reportIsDeterministicForSameSeedAndDiffersForOthers() {
        assertEquals(runner.run(config(1)), runner.run(config(1)))
        assertTrue(runner.run(config(1)).resultDigest != runner.run(config(2)).resultDigest)
    }

    @Test
    fun cleanSessionsMostlyApplyAndAbusersAreMostlyDetected() {
        val report = runner.run(config(7, size = 800, sessions = 10))
        assertTrue(report.cleanAppliedRate > 0.6, "cleanAppliedRate=${report.cleanAppliedRate}")
        assertTrue(report.abusersDetectedRate > 0.4, "abusersDetectedRate=${report.abusersDetectedRate}")
    }

    @Test
    fun processesOneHundredThousandSessionsWellUnderBudget() {
        var report = runner.run(config(3, size = 1, sessions = 1)) // warm up
        val elapsed = measureTime {
            report = runner.run(config(3, size = 1000, sessions = 100))
        }
        assertEquals(100_000, report.sessionsProcessed)
        assertTrue(elapsed < 5.minutes, "elapsed=$elapsed")
    }

    @Test
    fun dashboardExportIsSanitizedAndIdentifierFree() {
        val report = runner.run(config(9))
        val output = DashboardExporter(DataSanitizer(SyntheticIdMapper(seed = 1))).exportDashboard(report)
        assertTrue(output.content.contains("sessionsProcessed"))
        assertTrue(output.content.contains("resultDigest"))
        assertTrue(!Regex("[A-Za-z0-9]{20,}").containsMatchIn(output.content), "UID-like token leaked")
    }
}

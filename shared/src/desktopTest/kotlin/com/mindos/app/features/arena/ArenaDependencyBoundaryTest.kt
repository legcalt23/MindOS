package com.mindos.app.features.arena

import com.mindos.app.features.arena.simulation.SimulationConfig
import com.mindos.app.features.arena.simulation.SimulationHarness
import com.mindos.app.features.arena.simulation.SimulationSeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Task 11: Dependency boundaries — the arena core resolves no production infrastructure and runs offline. */
class ArenaDependencyBoundaryTest {

    private val forbiddenImportTokens = listOf("firebase", "crashlytics", "sentry", "analytics", "gitlive")

    @Test
    fun arenaMainHasNoFirebaseCrashlyticsSentryOrAnalyticsImports() {
        val files = ArenaSourceLocator.kotlinFiles(ArenaSourceLocator.mainDir())
        assertTrue(files.isNotEmpty(), "arena main source files should be locatable")
        for (file in files) {
            for (importLine in ArenaSourceLocator.importLines(file)) {
                val lower = importLine.lowercase()
                forbiddenImportTokens.forEach { token ->
                    assertFalse(lower.contains(token), "forbidden import '$token' in ${file.name}: $importLine")
                }
            }
        }
    }

    @Test
    fun arenaCoreAndSimulationRunFullyOffline() {
        // No network or Firebase is touched: engine + harness complete purely in memory.
        val result = SimulationHarness().run(
            SimulationConfig(SimulationSeed(11L), populationSize = 50, matchCount = 500),
        )
        assertEquals(500, result.matchesProcessed)
        assertEquals(0, result.ratingUpdatesRejected)
    }
}

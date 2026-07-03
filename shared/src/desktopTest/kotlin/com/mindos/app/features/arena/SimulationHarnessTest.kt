package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.UuidV4
import com.mindos.app.features.arena.simulation.SimulationConfig
import com.mindos.app.features.arena.simulation.SimulationHarness
import com.mindos.app.features.arena.simulation.SimulationSeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.measureTime

/** Phase 07: Offline synthetic simulation harness — performance, determinism, synthetic ids. */
class SimulationHarnessTest {

    private val harness = SimulationHarness()

    @Test
    fun processes100kMatchesUnderFiveMinutes() {
        val config = SimulationConfig(seed = SimulationSeed(123L), populationSize = 2_000, matchCount = 100_000)
        var processed = 0
        val elapsed = measureTime { processed = harness.run(config).matchesProcessed }
        assertEquals(100_000, processed)
        assertTrue(elapsed < 5.minutes, "100k matches took $elapsed")
    }

    @Test
    fun sameSeedProducesSameResult() {
        val config = SimulationConfig(seed = SimulationSeed(7L), populationSize = 500, matchCount = 5_000)
        assertEquals(harness.run(config), harness.run(config))
    }

    @Test
    fun differentSeedProducesDifferentResult() {
        val a = harness.run(SimulationConfig(SimulationSeed(1L), populationSize = 500, matchCount = 5_000))
        val b = harness.run(SimulationConfig(SimulationSeed(2L), populationSize = 500, matchCount = 5_000))
        assertNotEquals(a.finalPosteriorDigest, b.finalPosteriorDigest)
        assertNotEquals(a.syntheticProfileIds, b.syntheticProfileIds)
    }

    @Test
    fun outputsContainOnlySyntheticUuidV4Ids() {
        val result = harness.run(SimulationConfig(SimulationSeed(9L), populationSize = 100, matchCount = 1_000))
        assertTrue(UuidV4.isValid(result.runId.value))
        assertTrue(result.syntheticProfileIds.all { UuidV4.isValid(it) }, "all profile ids must be UUIDv4")
    }

    @Test
    fun cleanMatchesAreAllApplied() {
        val result = harness.run(SimulationConfig(SimulationSeed(3L), populationSize = 100, matchCount = 1_000))
        assertEquals(1_000, result.ratingUpdatesApplied)
        assertEquals(0, result.ratingUpdatesRejected)
    }
}

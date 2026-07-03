package com.mindos.app.features.arena

import com.mindos.app.features.arena.comparability.model.DeviceClass
import com.mindos.app.features.arena.config.UuidV4
import com.mindos.app.features.arena.simulation.generators.AbuseProfileAssigner
import com.mindos.app.features.arena.simulation.generators.AbuseProportions
import com.mindos.app.features.arena.simulation.generators.AbuseType
import com.mindos.app.features.arena.simulation.generators.LatencyJitterGenerator
import com.mindos.app.features.arena.simulation.generators.PlayerPopulationGenerator
import com.mindos.app.features.arena.simulation.generators.PopulationConfig
import com.mindos.app.features.arena.simulation.generators.SeedDeriver
import com.mindos.app.features.arena.simulation.generators.SkillGenerator
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 07 / P7.1: Seeded synthetic generators are reproducible and statistically sane. */
class SimulationGeneratorsTest {

    @Test
    fun seedDeriverIsDeterministicAndStreamDependent() {
        assertEquals(SeedDeriver.derive(42L, 1L), SeedDeriver.derive(42L, 1L))
        assertTrue(SeedDeriver.derive(42L, 1L) != SeedDeriver.derive(42L, 2L))
    }

    @Test
    fun normalSkillIsReproducibleAndCenteredOnMean() {
        assertEquals(SkillGenerator().generateSkill(Random(7)), SkillGenerator().generateSkill(Random(7)), 1e-9)
        val gen = SkillGenerator()
        val random = Random(7)
        val mean = (0 until 5000).map { gen.generateSkill(random) }.average()
        assertTrue(abs(mean - 1500.0) < 40.0, "mean=$mean")
    }

    @Test
    fun abuseProportionsAreRespectedAndReproducible() {
        val assigner = AbuseProfileAssigner()
        val random = Random(11)
        val n = 10_000
        val clean = (0 until n).count {
            assigner.assignAbuseProfile(AbuseProportions.DEFAULT, random) == AbuseType.CLEAN
        }
        assertTrue(abs(clean.toDouble() / n - 0.90) < 0.02, "cleanFraction=${clean.toDouble() / n}")
    }

    @Test
    fun latencyIsLowerOnHighEndAndLagSwitcherSpikesJitter() {
        val gen = LatencyJitterGenerator()
        val high = gen.generateLatency(DeviceClass.HIGH_END_120HZ, AbuseType.CLEAN, Random(1)).first
        val low = gen.generateLatency(DeviceClass.LOW_END_60HZ, AbuseType.CLEAN, Random(1)).first
        assertTrue(high < low, "high=$high low=$low")
        val (_, lagJitter) = gen.generateLatency(DeviceClass.HIGH_END_120HZ, AbuseType.LAG_SWITCHER, Random(1))
        assertEquals(120L, lagJitter)
    }

    @Test
    fun populationIsReproducibleForSameSeedAndDiffersForOthers() {
        val config = PopulationConfig(size = 200)
        val a = PlayerPopulationGenerator().generatePopulation(config, seed = 99)
        val b = PlayerPopulationGenerator().generatePopulation(config, seed = 99)
        val c = PlayerPopulationGenerator().generatePopulation(config, seed = 100)
        assertEquals(a, b)
        assertTrue(a != c)
        assertEquals(200, a.size)
        assertTrue(a.all { UuidV4.isValid(it.id) })
    }
}

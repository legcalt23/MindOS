@file:Suppress("MagicNumber") // Distribution / latency / proportion constants are documented synthetic defaults.

package com.mindos.app.features.arena.simulation.generators

import com.mindos.app.features.arena.comparability.model.DeviceClass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/** Phase 07: Abuse archetypes for synthetic population (integrity_abuse_generator.md). */
enum class AbuseType {
    CLEAN,
    SMURF,
    BOOSTER,
    COLLUDER,
    LAG_SWITCHER,
    SOLVER,
    ACCOUNT_SHARER,
}

/** Phase 07: Default abuse proportions (ordered for deterministic weighted selection). */
object AbuseProportions {
    val DEFAULT: Map<AbuseType, Double> = linkedMapOf(
        AbuseType.CLEAN to 0.90,
        AbuseType.SMURF to 0.03,
        AbuseType.BOOSTER to 0.02,
        AbuseType.COLLUDER to 0.02,
        AbuseType.LAG_SWITCHER to 0.01,
        AbuseType.SOLVER to 0.01,
        AbuseType.ACCOUNT_SHARER to 0.01,
    )
}

/** Phase 07: Derives independent, reproducible sub-seeds from a master seed. */
object SeedDeriver {
    private const val STREAM_MULTIPLIER = 0x9E3779B1L
    fun derive(masterSeed: Long, streamId: Long): Long = masterSeed * 31 + streamId * STREAM_MULTIPLIER
}

enum class SkillDistributionType {
    NORMAL,
    UNIFORM,
    BIMODAL,
}

/** Phase 07: Skill (true mu) distribution config. Default N(1500, 300) (skill_distribution_generator.md). */
data class SkillDistributionConfig(
    val type: SkillDistributionType = SkillDistributionType.NORMAL,
    val mean: Double = 1500.0,
    val stdDev: Double = 300.0,
    val low: Double = 1000.0,
    val high: Double = 2000.0,
    val bimodalShift: Double = 400.0,
) {
    init {
        require(stdDev >= 0.0) { "stdDev must be >= 0" }
        require(low <= high) { "low must be <= high" }
    }
}

/** Phase 07: Deterministic skill generator. */
class SkillGenerator(private val config: SkillDistributionConfig = SkillDistributionConfig()) {

    fun generateSkill(random: Random): Double = when (config.type) {
        SkillDistributionType.NORMAL -> normal(random, config.mean)
        SkillDistributionType.UNIFORM -> config.low + random.nextDouble() * (config.high - config.low)
        SkillDistributionType.BIMODAL ->
            if (random.nextBoolean()) normal(random, config.mean - config.bimodalShift)
            else normal(random, config.mean + config.bimodalShift)
    }

    private fun normal(random: Random, mean: Double): Double {
        val u1 = random.nextDouble().coerceIn(MIN_UNIFORM, 1.0)
        val u2 = random.nextDouble()
        val z = sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
        return mean + config.stdDev * z
    }

    companion object {
        private const val MIN_UNIFORM = 1e-12
    }
}

/** Phase 07: Weighted, deterministic abuse-profile assignment. */
class AbuseProfileAssigner {
    fun assignAbuseProfile(proportions: Map<AbuseType, Double>, random: Random): AbuseType {
        val roll = random.nextDouble()
        var cumulative = 0.0
        for ((type, weight) in proportions) {
            cumulative += weight
            if (roll < cumulative) return type
        }
        return AbuseType.CLEAN
    }
}

/** Phase 07: Latency/jitter config; latency is correlated with device class. */
data class LatencyJitterConfig(
    val highEndBaseMs: Long = 20,
    val midBaseMs: Long = 40,
    val lowEndBaseMs: Long = 70,
    val unknownBaseMs: Long = 80,
    val latencyRangeMs: Long = 30,
    val normalJitterMaxMs: Long = 15,
    val lagSwitchJitterMs: Long = 120,
)

/** Phase 07: Generates latency/jitter per session; lag-switchers get a jitter spike. */
class LatencyJitterGenerator(private val config: LatencyJitterConfig = LatencyJitterConfig()) {

    fun generateLatency(
        deviceClass: DeviceClass,
        abuseType: AbuseType,
        random: Random,
    ): Pair<Long, Long> {
        val latency = baseLatencyFor(deviceClass) + (random.nextDouble() * config.latencyRangeMs).toLong()
        val jitter = if (abuseType == AbuseType.LAG_SWITCHER) {
            config.lagSwitchJitterMs
        } else {
            (random.nextDouble() * config.normalJitterMaxMs).toLong()
        }
        return latency to jitter
    }

    private fun baseLatencyFor(deviceClass: DeviceClass): Long = when (deviceClass) {
        DeviceClass.HIGH_END_120HZ -> config.highEndBaseMs
        DeviceClass.MID_RANGE_90HZ -> config.midBaseMs
        DeviceClass.LOW_END_60HZ -> config.lowEndBaseMs
        DeviceClass.UNKNOWN -> config.unknownBaseMs
    }
}

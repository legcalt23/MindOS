package com.mindos.app.features.arena.simulation.generators

import com.mindos.app.features.arena.comparability.model.DeviceClass
import com.mindos.app.features.arena.config.UuidV4
import kotlin.random.Random

/** Phase 07: A 100% synthetic player with per-block true skill, device, abuse archetype, and context. */
data class SyntheticPlayer(
    val id: String,
    val skillByBlock: Map<String, Double>,
    val deviceClass: DeviceClass,
    val abuseType: AbuseType,
    val languageGroup: String,
    val accessibilityClass: String,
)

/** Phase 07: Population generation config. */
data class PopulationConfig(
    val size: Int,
    val blocks: List<String> = listOf("FOCUS", "WORKING_MEMORY", "MENTAL_SPEED"),
    val skill: SkillDistributionConfig = SkillDistributionConfig(),
    val abuseProportions: Map<AbuseType, Double> = AbuseProportions.DEFAULT,
) {
    init {
        require(size > 0) { "size must be > 0" }
        require(blocks.isNotEmpty()) { "blocks must not be empty" }
    }
}

/**
 * Phase 07: Generates a reproducible synthetic player population (player_population_generator.md).
 * Same (config, seed) always yields the same population — every id is a synthetic UUIDv4.
 */
class PlayerPopulationGenerator {

    fun generatePopulation(config: PopulationConfig, seed: Long): List<SyntheticPlayer> {
        val random = Random(seed)
        val skillGenerator = SkillGenerator(config.skill)
        val abuseAssigner = AbuseProfileAssigner()
        val deviceClasses = DeviceClass.entries
        return (0 until config.size).map {
            SyntheticPlayer(
                id = UuidV4.random(random),
                skillByBlock = config.blocks.associateWith { skillGenerator.generateSkill(random) },
                deviceClass = deviceClasses[random.nextInt(deviceClasses.size)],
                abuseType = abuseAssigner.assignAbuseProfile(config.abuseProportions, random),
                languageGroup = "EN",
                accessibilityClass = "COMPARABLE",
            )
        }
    }
}

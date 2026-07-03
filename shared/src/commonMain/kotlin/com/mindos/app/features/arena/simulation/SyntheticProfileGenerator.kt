@file:Suppress("MagicNumber") // Synthetic skill band and default-profile priors are documented constants.

package com.mindos.app.features.arena.simulation

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.evidence.ledger.SyntheticProfileId
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import kotlin.random.Random

/**
 * Phase 07: Generates 100% synthetic profiles with seeded latent skills. Deterministic given the
 * [random] source. Every id is a synthetic UUIDv4.
 */
class SyntheticProfileGenerator(private val random: Random) {

    fun generate(
        count: Int,
        motorType: TaskMotorType,
        hardwareClass: HardwareClass,
    ): List<SyntheticProfile> = (0 until count).map {
        val skill = SKILL_MIN + random.nextDouble() * (SKILL_MAX - SKILL_MIN)
        SyntheticProfile(
            id = SyntheticProfileId.random(random),
            motorType = motorType,
            hardwareClass = hardwareClass,
            trueSkill = Rating(skill),
            initialPosterior = RatingPosterior(Rating(DEFAULT_MU), Sigma(DEFAULT_SIGMA)),
        )
    }

    companion object {
        private const val SKILL_MIN = 1100.0
        private const val SKILL_MAX = 1900.0
        private const val DEFAULT_MU = 1500.0
        private const val DEFAULT_SIGMA = 350.0
    }
}

@file:Suppress("MagicNumber") // Default skill band / difficulty / hardware are documented config defaults.

package com.mindos.app.features.arena.simulation

import com.mindos.app.features.arena.config.OsFamily
import com.mindos.app.features.arena.config.RefreshRateTier
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.evidence.ledger.SimulationRunId
import com.mindos.app.features.arena.evidence.ledger.SyntheticProfileId
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.model.Rating

/** Phase 07: Seed for a reproducible simulation. Same seed -> same result. */
@JvmInline
value class SimulationSeed(val value: Long)

/**
 * Phase 07: A 100% synthetic competitor. [trueSkill] is the latent skill the rating should recover;
 * [id] is a synthetic UUIDv4. No real-user fields exist.
 */
data class SyntheticProfile(
    val id: SyntheticProfileId,
    val motorType: TaskMotorType,
    val hardwareClass: HardwareClass,
    val trueSkill: Rating,
    val initialPosterior: RatingPosterior,
)

/** Phase 07: A synthetic pairing for one match. Mental Speed pairs always share a hardware class. */
data class SyntheticMatch(
    val a: SyntheticProfile,
    val b: SyntheticProfile,
    val taskDifficulty: Rating,
)

/**
 * Phase 07: Simulation configuration. The whole simulated population shares one [hardwareClass] so
 * every Mental Speed match is legal by construction (cross-class ranking is forbidden).
 */
data class SimulationConfig(
    val seed: SimulationSeed,
    val populationSize: Int,
    val matchCount: Int,
    val motorType: TaskMotorType = TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
    val taskDifficulty: Rating = Rating(1500.0),
    val hardwareClass: HardwareClass = HardwareClass(OsFamily.ANDROID, RefreshRateTier.HZ_120),
) {
    init {
        require(populationSize > 0) { "populationSize must be > 0" }
        require(matchCount >= 0) { "matchCount must be >= 0" }
    }
}

/**
 * Phase 07: Aggregate, sanitized simulation output. Contains only synthetic UUIDv4 ids and aggregate
 * metrics — never raw identifiers or per-user rating values. Deterministic for a given seed.
 */
data class SimulationResult(
    val runId: SimulationRunId,
    val matchesProcessed: Int,
    val ratingUpdatesApplied: Int,
    val ratingUpdatesRejected: Int,
    val averageMatchDurationMs: Long,
    val finalPosteriorDigest: String,
    val syntheticProfileIds: List<String>,
)

package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.model.Capacity
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm

/** How a probe draws its content seed across administrations. */
enum class SeedPolicy {
    /** Always the exact same form (single seed). */
    FIXED,

    /** Rotates through a reserved bank of equivalent parallel forms. */
    ROTATED_RESERVED,
}

/**
 * Seed namespaces that keep probe content provably disjoint from training content
 * (anti-contamination, 06_ §1). Probe forms draw from a small reserved band; the
 * training seed source is the wall clock (epoch millis), which is always far above
 * the reserved band. The two ranges never overlap.
 */
@Suppress("MagicNumber") // The band bounds ARE the reserved-seed namespace definition.
object ProbeSeedSpace {
    /** Reserved band for probe parallel-form seeds. */
    val RESERVED: LongRange = 1L..100_000L

    /** Training seeds are epoch millis; this floor (~2001) is far above [RESERVED]. */
    const val MIN_TRAINING_SEED: Long = 1_000_000_000_000L

    fun isReserved(seed: Long): Boolean = seed in RESERVED

    fun isTrainingSeed(seed: Long): Boolean = seed >= MIN_TRAINING_SEED
}

/**
 * A reserved evaluation probe. NEVER presented in TRAINING mode. Improving on a
 * probe reflects (near-)transfer, not task-specific learning, which is the only
 * honest evidence of real change.
 *
 * @param fixedDifficulty the difficulty is constant across all administrations so
 *   pre/post are comparable.
 * @param reservedForms parallel-form seeds, all inside [ProbeSeedSpace.RESERVED].
 */
data class ProbeDefinition(
    val probeId: String,
    val capacity: Capacity,
    val paradigm: Paradigm,
    val fixedDifficulty: DifficultyVector,
    val seedPolicy: SeedPolicy,
    val reservedForms: List<Long>,
    val isReserved: Boolean = true,
) {
    init {
        require(probeId.isNotBlank()) { "probeId cannot be blank." }
        require(isReserved) { "A probe must be reserved (never trained)." }
        require(reservedForms.isNotEmpty()) { "A probe needs at least one reserved form." }
        require(reservedForms.all { ProbeSeedSpace.isReserved(it) }) {
            "All probe forms must use reserved seeds (probeId=$probeId)."
        }
    }
}

/** A scheduled administration of a probe: which probe and which parallel form to run. */
data class ProbeAdministration(
    val probe: ProbeDefinition,
    val formSeed: Long,
    val week: Int,
)

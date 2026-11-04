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
 * Seed namespaces for probe parallel forms (06_ §1). Probe forms draw from a small reserved band,
 * enforced on the probe side by the [ProbeDefinition] constructor.
 *
 * The disjointness this once claimed is NOT structural, and the correction matters. This KDoc used to
 * state that the training seed source was the wall clock (epoch millis), therefore always far above the
 * reserved band, therefore the two ranges never overlap. That producer no longer exists: the production
 * source `PlatformSeedSource` mints `Random.Default.nextLong()`, uniform over the whole `Long` range, and
 * it moved there deliberately — a wall clock is observable to the user and hands identical sequences to
 * two sessions started in the same millisecond. So a real training seed is negative about half the time
 * and may land anywhere, [RESERVED] included: an overlap is about 1e-14 per session rather than
 * impossible, and [isTrainingSeed] recognises only about half of what production actually mints.
 *
 * Read [isTrainingSeed] for what it is — a predicate over the epoch-millis band, with no production
 * call-site — and not as a classifier for the seeds this app mints today. Restoring a structural
 * guarantee would mean constraining which seed a training session receives, which changes the generated
 * content of every session; that is a product decision, registered as `MB-FOCUS-0125`, not taken here.
 *
 * What the reserved band still buys, and only this: the probe side is pinned, so a probe form seed is
 * always inside the band. It does NOT make the probe an untrained task — the paradigm is shared with the
 * catalogue — so it is not the anti-contamination a capacity claim would need. See [ProbeBank] and
 * `MB-FOCUS-0089`.
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
 * A reserved evaluation probe: fixed difficulty, reserved seeds, NEVER presented in TRAINING mode.
 * Improving on a probe is NOT by itself evidence about a capacity — the probe runs a paradigm the
 * catalogue trains, so practice on that paradigm is an alternative explanation that this design does
 * not rule out. Treat a probe delta as a same-instrument comparison against the user's own earlier
 * run, nothing wider (`MB-FOCUS-0089`; ladder frozen at C0 by `FocusClaimLevelGateTest`).
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
    /**
     * Whether [formSeed] re-serves a form this user has already been given.
     *
     * Reserved forms are a FINITE supply cycled modulo [ProbeDefinition.reservedForms], so once every form has
     * been spent the rotation hands back the first one — starting with the form the baseline itself ran. The
     * scheduler is the only place where that is knowable (it holds the burn count), and before this field
     * existed it computed the fact and dropped it, so no caller could tell a first showing from a repeat.
     *
     * MANDATORY, no default: a default would let a future scheduler stay silent about a repeat, and silence
     * here reads downstream as "this form is new" — the favourable answer, fabricated.
     *
     * Direction: this constrains what a caller MAY CLAIM; it is never an accusation. A repeat is a limit of
     * the reserved supply, not something the user did.
     */
    val repeatsEarlierForm: Boolean,
)

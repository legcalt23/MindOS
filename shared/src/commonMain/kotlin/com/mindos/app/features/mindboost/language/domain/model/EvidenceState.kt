package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Evidence ladder (v6 §5.2). This is the single most important governance type in the
 * module: it is the *honesty contract* between what the system measured and what it is
 * allowed to claim. The [ClaimEngine] (later slice) reads this ladder; it never invents a
 * higher state than the evidence supports.
 *
 * Ordering is strict: `E0 < E1 < E2 < E3 < E4 < E5`. Use [isAtLeast]/[rank] for gating;
 * never compare by `name`. [stateLabel] is a neutral Spanish description of the *state*
 * (not a motivational claim — claim copy is governed separately by the ClaimEngine).
 */
@Serializable
enum class EvidenceState(val stateLabel: String) {
    /** Insufficient data or high standard error — only "Estamos calibrando." is allowed. */
    E0_CALIBRATING("Calibrando"),

    /** Performance improves in *practiced* item families. Closed, in-template improvement. */
    E1_TRAINED_IMPROVEMENT("Mejora entrenada"),

    /** Improvement appears in unseen material of the same construct (near transfer). */
    E2_NEAR_TRANSFER("Transferencia cercana"),

    /** Skill demonstrated on an applied/simulated task (or consented own material). */
    E3_FUNCTIONAL_TRANSFER("Transferencia funcional"),

    /** Improvement held after a delay (7/30-day retest with a parallel form). */
    E4_MAINTAINED("Mantenimiento"),

    /** Robust generalization across domains, formats and cohorts; audit passed. */
    E5_ROBUST_GENERALIZATION("Generalización robusta");

    /** Strict ladder position (E0=0 .. E5=5). Derived from declaration order. */
    val rank: Int get() = ordinal

    /** True when this state is the same or stronger than [other]. */
    fun isAtLeast(other: EvidenceState): Boolean = rank >= other.rank

    /** The immediately stronger state, or `null` if already at the top of the ladder. */
    fun next(): EvidenceState? = entries.firstOrNull { it.rank == rank + 1 }

    /** True while the profile is still calibrating and uncertainty must be surfaced (§5.7). */
    val isCalibrating: Boolean get() = this == E0_CALIBRATING

    companion object {
        /** Lowest state on the ladder; the safe default before any evidence exists. */
        val LOWEST: EvidenceState = E0_CALIBRATING

        /** Resolves a state by [rank], or `null` if out of range. */
        fun ofRank(rank: Int): EvidenceState? = entries.firstOrNull { it.rank == rank }
    }
}

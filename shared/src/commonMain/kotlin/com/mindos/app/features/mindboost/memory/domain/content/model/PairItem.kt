package com.mindos.app.features.mindboost.memory.domain.content.model

/** Stable identity of an associated pair (string id, not a positional index). */
@JvmInline
value class PairId(val value: String) {
    init {
        require(value.isNotBlank()) { "PairId cannot be blank." }
    }
}

/** Pre-existing associative strength of a pair — a difficulty/depth knob for EX4. */
enum class AssociationStrength {
    WEAK,
    MODERATE,
    STRONG,
}

/**
 * An immutable word-word pair for EX4 (Pares Asociados). [cue] and [target] are
 * full [WordItem]s. Self-pairs are rejected. The fan-effect (which words are
 * shared ACROSS the pairs of a single trial) is a set-level decision owned by the
 * Phase-4 generator — this models a single pair only (doc §4 Ej.4 / §5 axis I).
 */
data class PairItem(
    val id: PairId,
    val cue: WordItem,
    val target: WordItem,
    val associationStrength: AssociationStrength,
) {
    init {
        require(cue.id != target.id) {
            "PairItem cannot be a self-pair (cue.id == target.id == ${cue.id})."
        }
    }

    /** All distinct word ids this pair touches — lets Phase 4 detect fan/overlap at set level. */
    fun wordIds(): Set<WordId> = setOf(cue.id, target.id)
}

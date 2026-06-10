package com.mindos.app.features.mindboost.memory.domain.content.pool

import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem

/**
 * Immutable, versioned pool of [PairItem]s for EX4 (Pares Asociados).
 *
 * Validates unique [PairId]s and a hard minimum size (volume honesty, same as
 * [WordPool]). It deliberately does NOT enforce global word-uniqueness across
 * pairs: the fan-effect (sharing a word between pairs) is a per-trial difficulty
 * lever owned by the Phase-4 generator (doc §4 Ej.4 / §5 axis I) — forbidding it
 * here would stop Phase 4 from modulating associative interference.
 */
data class PairPool(
    val version: Int,
    val language: Language,
    val items: List<PairItem>,
) {
    init {
        require(items.size >= MIN_POOL_SIZE) {
            "PairPool($language) has ${items.size} items; minimum is $MIN_POOL_SIZE " +
                "(no silent cap is applied)."
        }
        require(items.map { it.id }.toSet().size == items.size) {
            "PairPool($language) contains duplicate PairIds."
        }
    }

    val size: Int get() = items.size

    fun byStrength(strength: AssociationStrength): List<PairItem> =
        items.filter { it.associationStrength == strength }

    fun ids(): Set<PairId> = items.map { it.id }.toSet()

    /** A copy of the pool with [excludedIds] removed (used to reserve / exclude content). */
    fun excluding(excludedIds: Set<PairId>): PairPool =
        copy(items = items.filterNot { it.id in excludedIds })

    /** Honest volume/coverage snapshot — never hides the gap to the scientific ideal. */
    fun qualityReport(): PairPoolQualityReport = PairPoolQualityReport(
        totalItems = size,
        recommendedMinimum = SCIENTIFIC_IDEAL,
        belowScientificIdeal = size < SCIENTIFIC_IDEAL,
        perStrengthCount = AssociationStrength.entries.associateWith { byStrength(it).size },
    )

    companion object {
        /** Hard V1 floor enforced at construction. */
        const val MIN_POOL_SIZE = 40

        /** Scientific ideal for negligible practice effects on pairs. */
        const val SCIENTIFIC_IDEAL = 600
    }
}

/** Honest coverage snapshot of a [PairPool]. */
data class PairPoolQualityReport(
    val totalItems: Int,
    val recommendedMinimum: Int,
    val belowScientificIdeal: Boolean,
    val perStrengthCount: Map<AssociationStrength, Int>,
)

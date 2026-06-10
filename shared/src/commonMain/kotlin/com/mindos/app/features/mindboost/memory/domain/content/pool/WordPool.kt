package com.mindos.app.features.mindboost.memory.domain.content.pool

import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem

/**
 * Immutable, versioned, queryable pool of [WordItem]s for one [language].
 *
 * Volume honesty (doc §6/§13.4 anti-engaño): the pool enforces a hard minimum via
 * `require` — it fails loudly when under-sized rather than silently truncating or
 * padding. [qualityReport] exposes the real count and the gap to the scientific
 * ideal so callers can never mistake an under-powered pool for a sufficient one.
 *
 * Holds NO content literals (those live in the data layer) and no sampling logic
 * (that is [com.mindos.app.features.mindboost.memory.domain.content.sampling]).
 * Modelled as a `data class` (not an `object`) so tests can inject fakes.
 */
data class WordPool(
    val version: Int,
    val language: Language,
    val items: List<WordItem>,
) {
    init {
        require(items.size >= MIN_POOL_SIZE) {
            "WordPool($language) has ${items.size} items; minimum is $MIN_POOL_SIZE. " +
                "Authoring must be expanded before production (no silent cap is applied)."
        }
        require(items.map { it.id }.toSet().size == items.size) {
            "WordPool($language) contains duplicate WordIds."
        }
    }

    private val byCategoryIndex: Map<WordCategory, List<WordItem>> = items.groupBy { it.category }

    val size: Int get() = items.size

    fun byCategory(category: WordCategory): List<WordItem> = byCategoryIndex[category].orEmpty()

    fun byConcreteness(concreteness: Concreteness): List<WordItem> =
        items.filter { it.concreteness == concreteness }

    fun byFrequency(frequency: WordFrequency): List<WordItem> =
        items.filter { it.frequency == frequency }

    fun ids(): Set<WordId> = items.map { it.id }.toSet()

    /** A copy of the pool with [excludedIds] removed (used to reserve / exclude content). */
    fun excluding(excludedIds: Set<WordId>): WordPool =
        copy(items = items.filterNot { it.id in excludedIds })

    /** Honest volume/coverage snapshot — never hides the gap to the scientific ideal. */
    fun qualityReport(): PoolQualityReport = PoolQualityReport(
        totalItems = size,
        recommendedMinimum = SCIENTIFIC_IDEAL,
        belowScientificIdeal = size < SCIENTIFIC_IDEAL,
        perCategoryCount = WordCategory.entries.associateWith { byCategory(it).size },
        perFrequencyCount = WordFrequency.entries.associateWith { byFrequency(it).size },
        perConcretenessCount = Concreteness.entries.associateWith { byConcreteness(it).size },
    )

    companion object {
        /** Hard V1 floor enforced at construction. */
        const val MIN_POOL_SIZE = 100

        /** Scientific ideal (doc §4: >2000 to make practice effects negligible). */
        const val SCIENTIFIC_IDEAL = 2000
    }
}

/** Honest coverage snapshot of a [WordPool]. */
data class PoolQualityReport(
    val totalItems: Int,
    val recommendedMinimum: Int,
    val belowScientificIdeal: Boolean,
    val perCategoryCount: Map<WordCategory, Int>,
    val perFrequencyCount: Map<WordFrequency, Int>,
    val perConcretenessCount: Map<Concreteness, Int>,
)

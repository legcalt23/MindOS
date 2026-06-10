package com.mindos.app.features.mindboost.memory.domain.metrics

import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory

/**
 * Adjusted Ratio of Clustering (ARC, Roenker, Thompson & Brown 1971) — measures how
 * much free recall is organized by category beyond chance (doc §6 ARC_CLUSTERING).
 *
 * ARC = (R − E(R)) / (maxR − E(R))
 *   R     = observed category repetitions (adjacent recalled items, same category)
 *   E(R)  = expected repetitions by chance = (Σ nᵢ²)/N − 1
 *   maxR  = N − c   (c = number of distinct categories recalled)
 *
 * 1.0 = perfect categorical clustering, 0.0 = chance, negative = below chance.
 * Returns null when undefined (fewer than [MIN_ITEMS] items, or a single category
 * where maxR − E(R) = 0) — the engine then reports it as "insufficient data".
 */
internal object ArcClustering {

    const val MIN_ITEMS = 2

    fun arcIndex(recalledCategories: List<WordCategory>): Double? {
        val n = recalledCategories.size
        if (n < MIN_ITEMS) return null

        val observed = (1 until n).count { recalledCategories[it] == recalledCategories[it - 1] }
        val counts = recalledCategories.groupingBy { it }.eachCount()
        val distinctCategories = counts.size
        val sumSquares = counts.values.sumOf { it.toDouble() * it.toDouble() }

        val expected = sumSquares / n - 1.0
        val maxRepetitions = (n - distinctCategories).toDouble()
        val denominator = maxRepetitions - expected

        return if (denominator == 0.0) null else (observed - expected) / denominator
    }
}

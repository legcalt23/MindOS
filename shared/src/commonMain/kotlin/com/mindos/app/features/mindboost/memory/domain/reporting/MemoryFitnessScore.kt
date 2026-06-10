package com.mindos.app.features.mindboost.memory.domain.reporting

import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity

/** A per-subcapacity input score (0..100) with its supporting sample size. */
data class SubcapacityScore(
    val subcapacity: MemorySubcapacity,
    val value: Double,
    val n: Int,
) {
    init {
        require(value in 0.0..MAX_SCORE) { "Subcapacity score must be in 0..$MAX_SCORE (was $value)." }
        require(n >= 0) { "n cannot be negative." }
    }

    companion object {
        const val MAX_SCORE = 100.0
    }
}

/** A reported per-subcapacity line — always shown PER sub-capacity, never unitary (doc §13.15). */
data class SubcapacityLine(
    val subcapacity: MemorySubcapacity,
    val value: Double,
    val confident: Boolean,
)

/**
 * An honest composite Memory Fitness Score (0..100) with an explicit uncertainty
 * band: WIDE when little data backs it, narrowing as evidence accrues. When not
 * [confident] the UI must show it as provisional, never as a hard fact (doc §6/§13).
 */
data class MemoryFitnessScore(
    val value: Double?,
    val lowerBound: Double?,
    val upperBound: Double?,
    val confident: Boolean,
    val caption: String,
)

/** The overall fitness score plus the honest per-subcapacity breakdown. */
data class MemoryFitnessReport(
    val overall: MemoryFitnessScore,
    val perSubcapacity: List<SubcapacityLine>,
)

/**
 * Computes the [MemoryFitnessReport] from per-subcapacity scores. Reports BY
 * sub-capacity (doc §13.15: never as a single "memory improved X%") and widens the
 * overall band by the smallest supporting sample (less data → wider band).
 */
class MemoryFitnessCalculator {

    fun compute(subScores: List<SubcapacityScore>): MemoryFitnessReport {
        if (subScores.isEmpty()) {
            return MemoryFitnessReport(
                overall = MemoryFitnessScore(null, null, null, confident = false, caption = NO_DATA),
                perSubcapacity = emptyList(),
            )
        }

        val lines = subScores.map {
            SubcapacityLine(it.subcapacity, it.value, confident = it.n >= MIN_N)
        }
        val value = subScores.map { it.value }.average()
        val minN = subScores.minOf { it.n }
        val maturity = (minN.toDouble() / MATURE_N).coerceIn(0.0, 1.0)
        val margin = MIN_MARGIN + MAX_EXTRA_MARGIN * (1.0 - maturity)
        val confident = subScores.size >= MIN_COMPONENTS && minN >= MIN_N

        return MemoryFitnessReport(
            overall = MemoryFitnessScore(
                value = value,
                lowerBound = (value - margin).coerceAtLeast(0.0),
                upperBound = (value + margin).coerceAtMost(MAX_SCORE),
                confident = confident,
                caption = if (confident) "Estimación estable." else "Estimación preliminar — pocos datos todavía.",
            ),
            perSubcapacity = lines,
        )
    }

    private companion object {
        const val MAX_SCORE = 100.0
        const val MATURE_N = 60.0
        const val MIN_MARGIN = 3.0
        const val MAX_EXTRA_MARGIN = 17.0
        const val MIN_COMPONENTS = 2
        const val MIN_N = 20
        const val NO_DATA = "Datos insuficientes para un Memory Fitness Score."
    }
}

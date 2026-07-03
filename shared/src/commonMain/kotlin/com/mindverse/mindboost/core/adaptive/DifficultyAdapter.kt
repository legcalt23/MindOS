package com.mindverse.mindboost.core.adaptive

import com.mindverse.mindboost.core.domain.DifficultyVector

class DifficultyAdapter {
    fun calculateNextDifficulty(
        current: DifficultyVector,
        icd: Double,
        frs: Double,
        agrs: Double,
    ): DifficultyVector {
        if (agrs > AGRS_DELOAD_THRESHOLD || frs > FRS_DELOAD_THRESHOLD) {
            return current.copy(
                timePressure = (current.timePressure * DELOAD_TIME_PRESSURE_FACTOR).coerceAtLeast(0.0),
                memoryLoad = (current.memoryLoad - 1).coerceAtLeast(MIN_MEMORY_LOAD),
            ).coerceToSupportedRange()
        }

        val error = (icd.coerceIn(0.0, MAX_PERCENT_SCORE) / MAX_PERCENT_SCORE) - TARGET_ICD
        val nextVariables = if (error > 0.0) {
            current.numberOfVariables + 1
        } else {
            current.numberOfVariables - 1
        }

        return current.copy(numberOfVariables = nextVariables).coerceToSupportedRange()
    }

    private fun DifficultyVector.coerceToSupportedRange(): DifficultyVector = copy(
        numberOfVariables = numberOfVariables.coerceIn(MIN_VARIABLES, MAX_VARIABLES),
        numberOfAlternatives = numberOfAlternatives.coerceIn(MIN_ALTERNATIVES, MAX_ALTERNATIVES),
        timePressure = timePressure.coerceIn(0.0, 1.0),
        noiseLevel = noiseLevel.coerceIn(0.0, 1.0),
        ambiguityLevel = ambiguityLevel.coerceIn(0.0, 1.0),
        irreversibility = irreversibility.coerceIn(0.0, 1.0),
        informationCost = informationCost.coerceIn(0.0, 1.0),
        evidenceContradiction = evidenceContradiction.coerceIn(0.0, 1.0),
        memoryLoad = memoryLoad.coerceIn(MIN_MEMORY_LOAD, MAX_MEMORY_LOAD),
        justificationDemand = justificationDemand.coerceIn(0.0, 1.0),
    )

    private companion object {
        const val TARGET_ICD = 0.82
        const val MAX_PERCENT_SCORE = 100.0
        const val AGRS_DELOAD_THRESHOLD = 0.75
        const val FRS_DELOAD_THRESHOLD = 0.20
        const val DELOAD_TIME_PRESSURE_FACTOR = 0.6
        const val MIN_VARIABLES = 1
        const val MAX_VARIABLES = 10
        const val MIN_ALTERNATIVES = 2
        const val MAX_ALTERNATIVES = 15
        const val MIN_MEMORY_LOAD = 1
        const val MAX_MEMORY_LOAD = 7
    }
}

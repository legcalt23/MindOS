package com.mindos.app.features.mindboost.memory.domain.model

import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MemoryExerciseDefinitionTest {

    private fun valid(
        relevantAxes: Set<MemoryDifficultyAxis> = setOf(MemoryDifficultyAxis.LOAD, MemoryDifficultyAxis.DELAY),
        governingAxis: MemoryDifficultyAxis = MemoryDifficultyAxis.LOAD,
        governingMetric: MemoryMetricKey = MemoryMetricKey.WORKING_MEMORY_SPAN,
        primaryMetrics: Set<MemoryMetricKey> = setOf(
            MemoryMetricKey.WORKING_MEMORY_SPAN,
            MemoryMetricKey.INTRUSION_RATE,
        ),
    ) = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX1_SPAN_COMPLEJO,
        subcapacity = MemorySubcapacity.WORKING_MEMORY,
        module = MemoryModule.M1_EXECUTIVE_WM,
        paradigm = MemoryParadigm.COMPLEX_SPAN,
        relevantAxes = relevantAxes,
        governingAxis = governingAxis,
        governingMetric = governingMetric,
        primaryMetrics = primaryMetrics,
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Span Complejo",
        plainDescription = "Mantén ítems mientras resuelves otra tarea.",
    )

    @Test
    fun valid_definitionConstructs() {
        valid() // does not throw
    }

    @Test
    fun governingAxis_mustBeRelevant() {
        assertFailsWith<IllegalArgumentException> {
            valid(
                relevantAxes = setOf(MemoryDifficultyAxis.LOAD),
                governingAxis = MemoryDifficultyAxis.DELAY,
            )
        }
    }

    @Test
    fun governingMetric_mustBePrimary() {
        assertFailsWith<IllegalArgumentException> {
            valid(
                governingMetric = MemoryMetricKey.COWAN_K,
                primaryMetrics = setOf(MemoryMetricKey.WORKING_MEMORY_SPAN),
            )
        }
    }

    @Test
    fun relevantAxes_cannotBeEmpty() {
        assertFailsWith<IllegalArgumentException> {
            valid(relevantAxes = emptySet(), governingAxis = MemoryDifficultyAxis.LOAD)
        }
    }
}

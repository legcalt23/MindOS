package com.mindos.app.features.mindboost.focus.domain.model

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey

/**
 * Declarative, immutable "tech sheet" of an exercise. It carries NO render or
 * stimulus-generation logic (that lives in the exercise/ generators) — only the
 * stable description that metrics, adaptation and reporting depend on.
 *
 * @param relevantAxes the only difficulty axes this exercise manipulates. The
 *   adaptive engine restricts its choices to this set.
 * @param governingAxis the axis the adaptive engine moves first for this exercise.
 * @param governingMetric the metric that drives the primary adaptive decision.
 * @param plainDescription an honest, jargon-free sentence shown to the user.
 */
data class ExerciseDefinition(
    val id: ExerciseId,
    val capacity: Capacity,
    val module: CognitiveModule,
    val paradigm: Paradigm,
    val relevantAxes: Set<DifficultyAxis>,
    val governingAxis: DifficultyAxis,
    val governingMetric: MetricKey,
    val primaryMetrics: Set<MetricKey>,
    val displayName: String,
    val plainDescription: String,
) {
    init {
        require(governingAxis in relevantAxes) {
            "governingAxis ($governingAxis) must be one of relevantAxes for ${id.name}."
        }
        require(governingMetric in primaryMetrics) {
            "governingMetric ($governingMetric) must be one of primaryMetrics for ${id.name}."
        }
    }
}

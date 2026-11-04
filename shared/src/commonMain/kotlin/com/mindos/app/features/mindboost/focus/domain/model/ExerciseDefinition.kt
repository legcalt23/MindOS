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
 * @param governingMetric DECLARED per exercise and read by NOBODY in Focus (measured 2026-09-07,
 *   R14). `DefaultAdaptiveEngine` names its three keys directly — ACCURACY, RT_CV and FALSE_ALARMS —
 *   and never consults this field, so for E8, whose declaration says DUAL_TASK_COST, the adaptation
 *   actually runs on a metric the declaration does not name. The only readers of `.governingMetric`
 *   in the tree belong to Emotions and Memory, which are different types with their own engines. The
 *   field is kept because the `require` below constrains it against `primaryMetrics`, which IS read
 *   and decides which metrics get computed at all. Retiring it changes no behaviour, so "retire the
 *   governing metric" is not a remedy for anything. See MB-FOCUS-0140.
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

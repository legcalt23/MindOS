package com.mindos.app.features.mindboost.memory.domain.model

import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey

/**
 * Declarative, immutable "tech sheet" of a memory exercise (doc §4 / master table
 * §15). It carries NO render, stimulus-generation or scoring logic (those live in
 * content/, exercise/ and metrics/) — only the stable description that content
 * generation, metrics, adaptation, evaluation and reporting depend on.
 *
 * @param relevantAxes the only difficulty axes this exercise manipulates. The
 *   adaptive engine restricts its choices to this set.
 * @param governingAxis the axis the adaptive engine moves first for this exercise.
 * @param governingMetric the metric that drives the primary adaptive decision.
 * @param practiceEffectRisk how prone the exercise is to improvement-by-familiarity
 *   rather than real capacity (doc §4 "Riesgo de efecto práctica"); HIGH-risk
 *   exercises require larger procedural stimulus pools (doc §13.4).
 * @param plainDescription an honest, jargon-free sentence shown to the user.
 */
data class MemoryExerciseDefinition(
    val id: MemoryExerciseId,
    val subcapacity: MemorySubcapacity,
    val module: MemoryModule,
    val paradigm: MemoryParadigm,
    val relevantAxes: Set<MemoryDifficultyAxis>,
    val governingAxis: MemoryDifficultyAxis,
    val governingMetric: MemoryMetricKey,
    val primaryMetrics: Set<MemoryMetricKey>,
    val practiceEffectRisk: PracticeEffectRisk,
    val displayName: String,
    val plainDescription: String,
) {
    init {
        require(relevantAxes.isNotEmpty()) {
            "relevantAxes cannot be empty for ${id.name}."
        }
        require(governingAxis in relevantAxes) {
            "governingAxis ($governingAxis) must be one of relevantAxes for ${id.name}."
        }
        require(primaryMetrics.isNotEmpty()) {
            "primaryMetrics cannot be empty for ${id.name}."
        }
        require(governingMetric in primaryMetrics) {
            "governingMetric ($governingMetric) must be one of primaryMetrics for ${id.name}."
        }
        require(displayName.isNotBlank()) { "displayName cannot be blank for ${id.name}." }
        require(plainDescription.isNotBlank()) { "plainDescription cannot be blank for ${id.name}." }
    }
}

/**
 * Susceptibility of an exercise to practice effects (familiarity masquerading as
 * capacity gain, doc §4 / §13.4). Drives how aggressively the content layer must
 * vary stimuli and how reporting interprets improvement.
 */
enum class PracticeEffectRisk {
    LOW,
    MEDIUM,
    HIGH,
}

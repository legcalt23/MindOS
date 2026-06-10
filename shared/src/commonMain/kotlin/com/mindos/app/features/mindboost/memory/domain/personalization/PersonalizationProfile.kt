package com.mindos.app.features.mindboost.memory.domain.personalization

import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.session.MemorySessionTemplate

/**
 * How scoring is bent for an archetype (doc §12). E.g. OVERCONFIDENT forces a
 * confidence prompt and holds difficulty even on high performance (inflated
 * confidence shouldn't make the task easier); FAST_BUT_INACCURATE adds a pre-response
 * micro-pause and penalizes errors; CONFUSES_SIMILAR reduces similarity until
 * discrimination improves.
 */
data class MemoryScoringModifiers(
    val speedIncentive: Double = 1.0,
    val errorPenalty: Double = 1.0,
    val forcedConfidencePrompt: Boolean = false,
    val prePressureMicroPause: Boolean = false,
    val reduceSimilarity: Boolean = false,
    val holdDifficultyWhenConfidenceInflated: Boolean = false,
)

/**
 * The personalization layer for an archetype: which exercises to bias toward, which
 * difficulty axes to nudge, how to score, and the preferred session length.
 */
data class PersonalizationProfile(
    val archetype: MemoryArchetype,
    val exerciseWeights: Map<MemoryExerciseId, Double>,
    val axisBias: Map<MemoryDifficultyAxis, Double>,
    val scoringModifiers: MemoryScoringModifiers,
    val preferredTemplate: MemorySessionTemplate,
) {
    fun weightFor(id: MemoryExerciseId): Double = exerciseWeights[id] ?: NEUTRAL_WEIGHT
    fun biasFor(axis: MemoryDifficultyAxis): Double = axisBias[axis] ?: 0.0

    private companion object {
        const val NEUTRAL_WEIGHT = 1.0
    }
}

/**
 * Canonical per-archetype personalization (doc §12). Weights/biases are tuning data,
 * hence MagicNumber is suppressed. Exercise weights reference the V1 wave (EX1/EX3/EX4).
 */
@Suppress("MagicNumber")
object MemoryPersonalizationProfiles {

    private val WM = MemoryExerciseId.EX1_SPAN_COMPLEJO
    private val EPISODIC = MemoryExerciseId.EX3_LISTA_ESTRATEGIA
    private val ASSOC = MemoryExerciseId.EX4_PARES_ASOCIADOS

    @Suppress("CyclomaticComplexMethod") // One arm per archetype; the table must stay in one place.
    fun forArchetype(archetype: MemoryArchetype): PersonalizationProfile = when (archetype) {
        MemoryArchetype.BEGINNER -> PersonalizationProfile(
            archetype, mapOf(WM to 1.5, EPISODIC to 1.3), emptyMap(),
            MemoryScoringModifiers(), MemorySessionTemplate.MICRO_5,
        )

        MemoryArchetype.LOW_WORKING_MEMORY -> PersonalizationProfile(
            archetype, mapOf(WM to 1.6), mapOf(MemoryDifficultyAxis.LOAD to -0.05),
            MemoryScoringModifiers(), MemorySessionTemplate.STANDARD_10,
        )

        MemoryArchetype.FAST_FORGETTER -> PersonalizationProfile(
            archetype, mapOf(EPISODIC to 1.5, ASSOC to 1.3),
            mapOf(MemoryDifficultyAxis.DELAY to 0.08, MemoryDifficultyAxis.DEPTH to 0.05),
            MemoryScoringModifiers(), MemorySessionTemplate.STANDARD_10,
        )

        MemoryArchetype.CONFUSES_SIMILAR -> PersonalizationProfile(
            archetype, mapOf(ASSOC to 1.4, EPISODIC to 1.2),
            mapOf(MemoryDifficultyAxis.SIMILARITY to 0.05),
            MemoryScoringModifiers(reduceSimilarity = true), MemorySessionTemplate.STANDARD_10,
        )

        MemoryArchetype.UNDERCONFIDENT -> PersonalizationProfile(
            archetype, mapOf(EPISODIC to 1.1), emptyMap(),
            MemoryScoringModifiers(forcedConfidencePrompt = true), MemorySessionTemplate.MICRO_5,
        )

        MemoryArchetype.OVERCONFIDENT -> PersonalizationProfile(
            archetype, mapOf(ASSOC to 1.3), mapOf(MemoryDifficultyAxis.DELAY to 0.08),
            MemoryScoringModifiers(forcedConfidencePrompt = true, holdDifficultyWhenConfidenceInflated = true),
            MemorySessionTemplate.STANDARD_10,
        )

        MemoryArchetype.SLOW_BUT_ACCURATE -> PersonalizationProfile(
            archetype, mapOf(WM to 1.2), emptyMap(),
            MemoryScoringModifiers(speedIncentive = 0.8), MemorySessionTemplate.FULL_20,
        )

        MemoryArchetype.FAST_BUT_INACCURATE -> PersonalizationProfile(
            archetype, mapOf(ASSOC to 1.2), emptyMap(),
            MemoryScoringModifiers(errorPenalty = 1.6, prePressureMicroPause = true, forcedConfidencePrompt = true),
            MemorySessionTemplate.STANDARD_10,
        )

        MemoryArchetype.DEMANDING_STUDENT -> PersonalizationProfile(
            archetype, mapOf(EPISODIC to 1.4, ASSOC to 1.3), mapOf(MemoryDifficultyAxis.DEPTH to 0.05),
            MemoryScoringModifiers(), MemorySessionTemplate.FULL_20,
        )

        MemoryArchetype.ADVANCED -> PersonalizationProfile(
            archetype, mapOf(WM to 1.2, EPISODIC to 1.2, ASSOC to 1.2),
            mapOf(MemoryDifficultyAxis.LOAD to 0.05),
            MemoryScoringModifiers(), MemorySessionTemplate.FULL_20,
        )

        MemoryArchetype.ELITE -> PersonalizationProfile(
            archetype, mapOf(WM to 1.3, ASSOC to 1.3),
            mapOf(
                MemoryDifficultyAxis.INTERFERENCE to 0.10,
                MemoryDifficultyAxis.SIMILARITY to 0.10,
                MemoryDifficultyAxis.DELAY to 0.10,
            ),
            MemoryScoringModifiers(), MemorySessionTemplate.INTENSIVE_30,
        )
    }

    /** A profile exists for every archetype. */
    val all: List<PersonalizationProfile> = MemoryArchetype.entries.map { forArchetype(it) }
}

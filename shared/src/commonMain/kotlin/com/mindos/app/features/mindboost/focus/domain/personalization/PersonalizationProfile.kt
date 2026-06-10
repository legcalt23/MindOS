package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate

/**
 * How scoring is bent for an archetype. Crucially for the IMPULSIVE archetype,
 * [speedIncentive] drops below 1.0 — a fast response is NOT rewarded when it comes
 * with commissions — and [prePressureMicroPause] gates input briefly. See 08_ §2.
 */
data class ScoringModifiers(
    val speedIncentive: Double = 1.0,
    val errorPenalty: Double = 1.0,
    val lapsePenalty: Double = 1.0,
    val prePressureMicroPause: Boolean = false,
)

/**
 * The personalization layer for an archetype: which exercises to bias toward,
 * which difficulty axes to nudge, how to score, and the preferred session length.
 */
data class PersonalizationProfile(
    val archetype: UserArchetype,
    val exerciseWeights: Map<ExerciseId, Double>,
    val axisBias: Map<DifficultyAxis, Double>,
    val scoringModifiers: ScoringModifiers,
    val preferredTemplate: SessionTemplate,
) {
    fun weightFor(id: ExerciseId): Double = exerciseWeights[id] ?: NEUTRAL_WEIGHT
    fun biasFor(axis: DifficultyAxis): Double = axisBias[axis] ?: 0.0

    private companion object {
        const val NEUTRAL_WEIGHT = 1.0
    }
}

/**
 * Canonical per-archetype personalization (08_personalization_plan.md table). The
 * weights/biases are tuning data, hence MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
object PersonalizationProfiles {

    fun forArchetype(archetype: UserArchetype): PersonalizationProfile = when (archetype) {
        UserArchetype.BEGINNER -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(ExerciseId.E1_VIGIA to 1.5, ExerciseId.E2_FILTRO to 1.3),
            axisBias = emptyMap(),
            scoringModifiers = ScoringModifiers(),
            preferredTemplate = SessionTemplate.MAINTENANCE_5,
        )

        UserArchetype.LOW_CONCENTRATION -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(ExerciseId.E1_VIGIA to 1.5, ExerciseId.E12_REENGANCHE to 1.4),
            axisBias = mapOf(DifficultyAxis.TIME_ON_TASK to 0.05),
            scoringModifiers = ScoringModifiers(lapsePenalty = 1.5),
            preferredTemplate = SessionTemplate.MAINTENANCE_5,
        )

        UserArchetype.IMPULSIVE -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(ExerciseId.E3_GATILLO to 1.6, ExerciseId.E4_FRENO to 1.5),
            axisBias = mapOf(DifficultyAxis.INHIBITORY_DEMAND to 0.10),
            // Do NOT reward speed; add a pre-response micro-pause to contain precipitation.
            scoringModifiers = ScoringModifiers(speedIncentive = 0.6, errorPenalty = 1.2, prePressureMicroPause = true),
            preferredTemplate = SessionTemplate.STANDARD_10,
        )

        UserArchetype.SLOW_BUT_ACCURATE -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(ExerciseId.E13_PRESION to 1.4, ExerciseId.E6_CAMBIO to 1.3),
            axisBias = mapOf(DifficultyAxis.PRESSURE to 0.10),
            scoringModifiers = ScoringModifiers(),
            preferredTemplate = SessionTemplate.DEVELOPMENT_20,
        )

        UserArchetype.FAST_BUT_INACCURATE -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(ExerciseId.E13_PRESION to 1.4, ExerciseId.E5_CONFLICTO to 1.4),
            axisBias = emptyMap(),
            // Penalize errors strongly; lower the speed incentive so accuracy is prioritized.
            scoringModifiers = ScoringModifiers(errorPenalty = 1.6, speedIncentive = 0.8),
            preferredTemplate = SessionTemplate.STANDARD_10,
        )

        UserArchetype.ADVANCED -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(
                ExerciseId.E6_CAMBIO to 1.3,
                ExerciseId.E8_DUO to 1.3,
                ExerciseId.E9_ENJAMBRE to 1.3,
            ),
            axisBias = mapOf(DifficultyAxis.DUAL_TASK_LOAD to 0.05),
            scoringModifiers = ScoringModifiers(),
            preferredTemplate = SessionTemplate.DEVELOPMENT_20,
        )

        UserArchetype.ELITE -> PersonalizationProfile(
            archetype = archetype,
            exerciseWeights = mapOf(ExerciseId.E11_ANCLA to 1.4, ExerciseId.E13_PRESION to 1.4),
            axisBias = mapOf(DifficultyAxis.DISTRACTOR_SALIENCE to 0.10, DifficultyAxis.PRESSURE to 0.10),
            scoringModifiers = ScoringModifiers(),
            preferredTemplate = SessionTemplate.ELITE_30,
        )
    }

    /** A profile exists for every archetype. */
    val all: List<PersonalizationProfile> = UserArchetype.entries.map { forArchetype(it) }
}

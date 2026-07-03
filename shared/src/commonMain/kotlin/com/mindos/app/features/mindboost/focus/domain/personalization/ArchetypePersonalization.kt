package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

/**
 * Applies the archetype's [PersonalizationProfile.axisBias] to the *initial* difficulty of an exercise
 * (08_ §2: "sesgo de dificultad inicial por eje" — e.g. an impulsive user starts with `inhibitoryDemand+`).
 *
 * Honesty / non-stacking rule: the bias is a heuristic default, so it only applies when the user has NO
 * calibrated or adapted difficulty for the exercise yet. Once a real value exists (from calibration or a
 * prior session's adaptive decision), that empirical value wins — the heuristic never overrides it. The
 * bias is also restricted to the exercise's [ExerciseDefinition.relevantAxes], so an axis the exercise
 * does not use is never nudged.
 */
object ArchetypePersonalization {

    /** The exercise's stored (calibrated/adapted) starting difficulty, or an archetype-biased default. */
    fun startingDifficulty(profile: UserCognitiveProfile, exercise: ExerciseDefinition): DifficultyVector =
        profile.startingDifficulty[exercise.id] ?: biasedDefault(profile, exercise)

    private fun biasedDefault(profile: UserCognitiveProfile, exercise: ExerciseDefinition): DifficultyVector {
        val axisBias = PersonalizationProfiles.forArchetype(profile.archetype).axisBias
        return axisBias.entries.fold(DifficultyVector.EASIEST) { vector, (axis, delta) ->
            if (axis in exercise.relevantAxes && delta != 0.0) nudge(vector, axis, delta) else vector
        }
    }

    private fun nudge(vector: DifficultyVector, axis: DifficultyAxis, delta: Double): DifficultyVector =
        if (delta >= 0.0) vector.stepUp(axis, delta) else vector.stepDown(axis, -delta)
}

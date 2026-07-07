@file:Suppress("MaxLineLength") // Copy table: user-facing Spanish recommendation reasons read best whole.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionCapacity
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId

/** A next-exercise suggestion + its plain, non-clinical reason. [exerciseId] is always an implemented exercise. */
data class EmotionRecommendation(val exerciseId: EmotionExerciseId, val reason: String)

/**
 * Pure next-exercise recommendation (MC8u P0-1 / P1 retention). Deterministic from the session outcome:
 *  - after a SAFETY stop → a base task, never a push to more intensity;
 *  - with insufficient sample ([strength] == null) → repeat for a stable reading;
 *  - a WEAK confident signal → reinforce the same capacity with a base exercise;
 *  - a STRONG confident signal → broaden to a complementary capacity.
 * Every returned id is in the implemented set {EM1, EM2, EM3, EM8, EM10, EM11}; reasons are scanner-clean.
 */
object EmotionNextRecommendationEngine {

    fun recommend(
        current: EmotionExerciseDefinition,
        strength: SignalStrength?,
        stoppedForSafety: Boolean,
    ): EmotionRecommendation = when {
        stoppedForSafety -> EmotionRecommendation(
            EmotionExerciseId.EM1_PAUSA,
            "Cuando quieras volver, empieza con una tarea base y sin prisa.",
        )
        strength == null -> EmotionRecommendation(
            current.id,
            "Repite este ejercicio para lograr una lectura de rendimiento más estable.",
        )
        strength == SignalStrength.WEAK -> reinforce(current.capacity)
        strength == SignalStrength.STRONG -> broaden(current.capacity)
        else -> EmotionRecommendation(current.id, "Repite para consolidar el ritmo de esta capacidad.")
    }

    private fun reinforce(capacity: EmotionCapacity): EmotionRecommendation = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION ->
            EmotionRecommendation(EmotionExerciseId.EM1_PAUSA, "Practica la pausa antes de responder con una base sencilla.")
        EmotionCapacity.POST_ERROR_RECOVERY ->
            EmotionRecommendation(EmotionExerciseId.EM2_RECUPERACION, "Practica retomar el ritmo después de un error.")
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION ->
            EmotionRecommendation(EmotionExerciseId.EM3_SEPARADOR, "Practica separar estado, pensamiento, impulso y hecho.")
        else ->
            EmotionRecommendation(EmotionExerciseId.EM1_PAUSA, "Empieza por una tarea base para asentar el ritmo.")
    }

    private fun broaden(capacity: EmotionCapacity): EmotionRecommendation = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION ->
            EmotionRecommendation(EmotionExerciseId.EM3_SEPARADOR, "Amplía a separar categorías bajo una presión leve.")
        EmotionCapacity.POST_ERROR_RECOVERY ->
            EmotionRecommendation(EmotionExerciseId.EM1_PAUSA, "Amplía al control de las respuestas de más.")
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION ->
            EmotionRecommendation(EmotionExerciseId.EM2_RECUPERACION, "Amplía a retomar el ritmo tras un error.")
        else ->
            EmotionRecommendation(EmotionExerciseId.EM2_RECUPERACION, "Prueba una variante complementaria.")
    }
}

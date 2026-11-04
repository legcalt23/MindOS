@file:Suppress("MaxLineLength") // Copy table: user-facing Spanish recommendation reasons read best whole.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId

/** A next-exercise suggestion + its plain, non-clinical reason. [exerciseId] is always an implemented exercise. */
data class EmotionRecommendation(val exerciseId: EmotionExerciseId, val reason: String)

/**
 * Pure next-exercise recommendation (MC8u P0-1 / P1 retention · MC8u-B §7 paired distinctness). Deterministic from
 * the session outcome, and exercise-AWARE so paired siblings never give an identical recommendation:
 *  - after a SAFETY stop → a base task, never a push to more intensity;
 *  - with insufficient sample ([strength] == null) → repeat the SAME exercise for a stable reading;
 *  - a WEAK confident signal → reinforce by dedicating another session to the same exercise;
 *  - a STRONG confident signal → reinforce as well, because after Ola 3 there is nothing to broaden TO.
 * Every returned id is in the implemented set; every reason is scanner-clean and names the concrete exercise.
 *
 * The sibling table this object used to carry (EM1↔EM8, EM2↔EM11, EM3↔EM10) is gone with the three entries it
 * pointed at. It is deleted rather than kept empty on purpose: a map whose every value is an exercise the user
 * can no longer reach would have handed out a recommendation for an exercise that does not run, which is the
 * one thing this object promises never to do. When a second variant genuinely exists again — R3's pressure mode
 * is the candidate — the branch comes back WITH it, not before it.
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
            "Repite «${current.displayName}» para lograr una lectura de rendimiento más estable.",
        )
        strength == SignalStrength.WEAK -> reinforce(current)
        strength == SignalStrength.STRONG -> reinforce(current)
        else -> EmotionRecommendation(current.id, "Repite «${current.displayName}» para consolidar el ritmo de esta práctica.")
    }

    private fun reinforce(current: EmotionExerciseDefinition): EmotionRecommendation =
        EmotionRecommendation(
            current.id,
            "Dedica otra sesión a «${current.displayName}» para afianzar ${EmotionFeedbackCopy.focusOf(current.capacity)}.",
        )

}

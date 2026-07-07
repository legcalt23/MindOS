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
 *  - a STRONG confident signal → broaden to the complementary SIBLING exercise (EM1↔EM8, EM2↔EM11, EM3↔EM10).
 * Every returned id is in the implemented set {EM1, EM2, EM3, EM8, EM10, EM11}; every reason is scanner-clean and, by
 * naming the concrete exercise, distinct across paired exercises.
 */
object EmotionNextRecommendationEngine {

    /** Complementary exercise within the same trained capacity — used to broaden after a strong reading. */
    private val SIBLINGS: Map<EmotionExerciseId, EmotionExerciseId> = mapOf(
        EmotionExerciseId.EM1_PAUSA to EmotionExerciseId.EM8_ANTIIMPULSO,
        EmotionExerciseId.EM8_ANTIIMPULSO to EmotionExerciseId.EM1_PAUSA,
        EmotionExerciseId.EM2_RECUPERACION to EmotionExerciseId.EM11_FEEDBACK_NEG,
        EmotionExerciseId.EM11_FEEDBACK_NEG to EmotionExerciseId.EM2_RECUPERACION,
        EmotionExerciseId.EM3_SEPARADOR to EmotionExerciseId.EM10_SEP_PRESION,
        EmotionExerciseId.EM10_SEP_PRESION to EmotionExerciseId.EM3_SEPARADOR,
    )

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
        strength == SignalStrength.STRONG -> broaden(current)
        else -> EmotionRecommendation(current.id, "Repite «${current.displayName}» para consolidar el ritmo de esta práctica.")
    }

    private fun reinforce(current: EmotionExerciseDefinition): EmotionRecommendation =
        EmotionRecommendation(
            current.id,
            "Dedica otra sesión a «${current.displayName}» para afianzar ${EmotionFeedbackCopy.focusOf(current.capacity)}.",
        )

    private fun broaden(current: EmotionExerciseDefinition): EmotionRecommendation {
        val siblingId = SIBLINGS[current.id] ?: current.id
        val siblingName = EmotionExerciseCatalog.definition(siblingId).displayName
        return EmotionRecommendation(
            siblingId,
            "Ya rindes bien en «${current.displayName}»; prueba «$siblingName» para ampliar ${EmotionFeedbackCopy.focusOf(current.capacity)}.",
        )
    }
}

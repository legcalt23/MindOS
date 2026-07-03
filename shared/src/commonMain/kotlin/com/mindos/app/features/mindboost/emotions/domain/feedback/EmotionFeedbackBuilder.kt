package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier

/**
 * Builds the sober four-part feedback. Always emits all four parts plus the transfer label and a
 * confidence caveat — never over-promising, never a clinical/life claim. All copy is scanner-clean and
 * functional. The "what not to conclude" part is mandatory and constant.
 */
class EmotionFeedbackBuilder {

    fun build(
        definition: EmotionExerciseDefinition,
        primary: EmotionMetricValue?,
        tier: ProfileTier,
    ): FourPartFeedback {
        val confident = primary?.confident == true && tier != ProfileTier.EXPLORATORY
        return FourPartFeedback(
            whatHappened = "Has completado un bloque de ${definition.displayName}.",
            whatItMeans = "Refleja tu práctica de ${definition.displayName.lowercase()} dentro del entrenamiento.",
            whatToPractice = "La próxima vez, nota la señal y responde con un poco más de control.",
            whatNotToConclude = "Esto describe tu rendimiento en el ejercicio, no una conclusión sobre ti.",
            transferLabel = definition.transferLabel,
            confidenceNote = if (confident) {
                "Tendencia con datos suficientes."
            } else {
                "Datos preliminares: aún no es una tendencia estable."
            },
        )
    }
}

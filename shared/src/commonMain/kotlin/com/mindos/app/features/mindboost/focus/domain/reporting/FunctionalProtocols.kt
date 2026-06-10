package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/** A real-life context a micro-protocol maps to. */
enum class FunctionalContext {
    STUDY,
    DEEP_WORK,
    COMPETITION,
    READING,
    DECISION_UNDER_PRESSURE,
    DIGITAL_DISTRACTION,
}

/**
 * The transfer strength a protocol can claim. There is intentionally only ONE
 * value: functional transfer is, at best, weak and subjective — never proven by
 * un-trained probes. We never claim strong functional transfer (14_risks §C5/P3).
 */
enum class TransferStrength { WEAK_SUBJECTIVE }

/**
 * A functional micro-protocol: a practice analogy mapping a life context to focus
 * exercises. Carries a mandatory [disclaimer] so it is never read as a guarantee.
 */
data class FunctionalProtocol(
    val context: FunctionalContext,
    val title: String,
    val exercises: List<ExerciseId>,
    val rationale: String,
    val disclaimer: String = DEFAULT_DISCLAIMER,
    val strength: TransferStrength = TransferStrength.WEAK_SUBJECTIVE,
) {
    init {
        require(exercises.isNotEmpty()) { "A protocol must reference at least one exercise." }
        require(disclaimer.isNotBlank()) { "A protocol must carry its honesty disclaimer." }
    }

    companion object {
        const val DEFAULT_DISCLAIMER: String =
            "Transferencia funcional débil y subjetiva: NO está demostrada con pruebas no " +
                "entrenadas. Es una analogía de práctica, no una garantía de mejora en esa actividad."
    }
}

/** The catalogue of functional micro-protocols (14_risks §Transferencia funcional). */
object FunctionalProtocols {

    fun forContext(context: FunctionalContext): FunctionalProtocol = when (context) {
        FunctionalContext.STUDY -> FunctionalProtocol(
            context = context,
            title = "Bloque Ancla para estudiar",
            exercises = listOf(ExerciseId.E11_ANCLA, ExerciseId.E1_VIGIA),
            rationale = "Mantener la tarea ignorando intrusos se parece a estudiar sin caer en distracciones.",
        )
        FunctionalContext.DEEP_WORK -> FunctionalProtocol(
            context = context,
            title = "Sesión protegida de trabajo profundo",
            exercises = listOf(ExerciseId.E1_VIGIA, ExerciseId.E12_REENGANCHE),
            rationale = "Vigilancia sostenida y re-enganche tras interrupciones imitan los bloques de foco prolongado.",
        )
        FunctionalContext.COMPETITION -> FunctionalProtocol(
            context = context,
            title = "Bajo presión",
            exercises = listOf(ExerciseId.E13_PRESION),
            rationale = "Decidir con deadline y coste por error se parece a rendir bajo presión competitiva.",
        )
        FunctionalContext.READING -> FunctionalProtocol(
            context = context,
            title = "Flujo controlado para lectura",
            exercises = listOf(ExerciseId.E10_FLUJO),
            rationale = "Detectar objetivos en un flujo serial imita seguir un texto sin perder el hilo.",
        )
        FunctionalContext.DECISION_UNDER_PRESSURE -> FunctionalProtocol(
            context = context,
            title = "Freno antes de actuar",
            exercises = listOf(ExerciseId.E4_FRENO, ExerciseId.E3_GATILLO),
            rationale = "Abortar una respuesta ya iniciada practica el freno previo a decidir con prisa.",
        )
        FunctionalContext.DIGITAL_DISTRACTION -> FunctionalProtocol(
            context = context,
            title = "Ancla personalizada anti-distracción",
            exercises = listOf(ExerciseId.E11_ANCLA),
            rationale = "Ignorar intrusos tipo notificación practica resistir la distracción digital.",
        )
    }

    val all: List<FunctionalProtocol> = FunctionalContext.entries.map { forContext(it) }
}

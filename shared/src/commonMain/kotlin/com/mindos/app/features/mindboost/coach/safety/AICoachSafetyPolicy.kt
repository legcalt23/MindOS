@file:Suppress("MaxLineLength") // Safe user-facing sentences + detection term lists read best whole.

package com.mindos.app.features.mindboost.coach.safety

import com.mindos.app.features.mindboost.core.copy.MindBoostCopySafetyScanner

/**
 * MC8u-F — the DETERMINISTIC safety layer that wraps the AI Coach model on BOTH sides. It runs before and after any
 * model call so safety never depends on the model behaving. This file lives OUTSIDE the copy/terminology-scanned tree
 * (like `ForbiddenLexicon`) precisely because it must hold the literal clinical/crisis terms it DETECTS.
 *
 * Input guard: classify a user request. Anything that is not [CoachRequestCategory.NORMAL_TRAINING] is answered by a
 * fixed, safe response WITHOUT calling the model. Crisis/self-harm takes priority and yields a brief, non-clinical
 * response that points to urgent local professional help — never improvised therapy, never a MindBoost claim.
 *
 * Output guard: even for a normal request, the model's output is scanned; a diagnosis / guarantee / therapist voice /
 * medication / cure / IQ-boost / real-emotional-state reading is BLOCKED and replaced with a safe fallback.
 */
enum class CoachRequestCategory {
    NORMAL_TRAINING, DIAGNOSIS, MEDICATION, CRISIS_SELF_HARM, DISORDER, IQ_GUARANTEE, MEDICAL_CLAIM, EMOTIONAL_STATE, OUT_OF_SCOPE
}

enum class OutputVerdict { ALLOW, BLOCK }

data class OutputDecision(val verdict: OutputVerdict, val safeText: String)

object AICoachSafetyPolicy {

    // Safe, fixed responses (non-clinical, non-manipulative, no MindBoost health claims).
    const val CRISIS_RESPONSE =
        "Lo que cuentas es importante y va más allá de lo que un entrenador cognitivo puede acompañar. Si estás en " +
            "peligro o piensas en hacerte daño, por favor contacta ahora mismo con un servicio de emergencia local o " +
            "una línea de ayuda profesional de tu país. No estás solo en esto."
    const val SAFE_REFUSAL =
        "Eso no es algo que pueda responder: no doy valoraciones de salud, ni identifico condiciones, ni recomiendo " +
            "medicación. MindBoost entrena capacidades cognitivas con ejercicios; para temas de salud, consulta a un " +
            "profesional cualificado."
    const val IQ_REFUSAL =
        "MindBoost no promete subir tu IQ ni garantiza mejoras. Entrena habilidades concretas dentro de la app y te " +
            "muestra tu progreso en esas tareas, sin prometer efectos amplios en la vida diaria."
    const val EMOTIONAL_STATE_REDIRECT =
        "No leo cómo te sientes por dentro. Solo puedo comentar tu rendimiento en las tareas de MindBoost y tu " +
            "progreso de entrenamiento, no tu estado personal."
    const val OUT_OF_SCOPE_REDIRECT =
        "Puedo ayudarte con tu entrenamiento en MindBoost: qué practicar, tu progreso y recomendaciones de práctica. " +
            "Para otros temas, no soy la herramienta adecuada."
    const val SAFE_FALLBACK =
        "Prefiero no responder eso así. Puedo comentar tu rendimiento en las tareas y sugerirte qué practicar, sin " +
            "valoraciones de salud ni promesas de resultados."
    const val NOT_ENOUGH_DATA =
        "Todavía no tengo suficientes sesiones válidas para darte una lectura útil. Entrena unas cuantas sesiones más " +
            "y volveré con observaciones de práctica."

    private data class Rule(val category: CoachRequestCategory, val terms: List<String>)

    // Ordered by priority: crisis first; a cure/medication/IQ ask outranks a bare disorder mention.
    private val REQUEST_RULES: List<Rule> = listOf(
        Rule(CoachRequestCategory.CRISIS_SELF_HARM, listOf("suicid", "autolesion", "hacerme dano", "quitarme la vida", "no quiero vivir", "matarme", "acabar con todo", "kill myself", "end my life", "self harm", "hurt myself")),
        Rule(CoachRequestCategory.MEDICATION, listOf("medicacion", "medicamento", "pastilla", "antidepresivo", "ansiolitico", "que tomo", "deberia tomar", "que deberia tomar", "medication", "pills", "prescrib", "dosis")),
        Rule(CoachRequestCategory.IQ_GUARANTEE, listOf("iq", "coeficiente intelectual", "coeficiente de inteligencia", "subir mi iq", "raise my iq", "cuanto sube", "cuanto me sube")),
        Rule(CoachRequestCategory.MEDICAL_CLAIM, listOf("cura", "curar", "curas", "curara", "me cura", "cure", "heal", "sanar")),
        Rule(CoachRequestCategory.DIAGNOSIS, listOf("diagnostic", "diagnostica", "diagnose", "do i have", "tengo tdah", "tengo depresion", "tengo ansiedad", "tengo un trastorno", "padezco")),
        Rule(CoachRequestCategory.DISORDER, listOf("trastorno", "tdah", "adhd", "depresion", "bipolar", "disorder", "salud mental", "ansiedad")),
        Rule(CoachRequestCategory.EMOTIONAL_STATE, listOf("que emocion", "emocion real", "estado emocional real", "que siento", "como me siento de verdad", "que siento de verdad")),
    )

    // Model-output red flags (folded): diagnosis, guarantee, therapist voice, medication, cure, IQ boost, state reading.
    private val OUTPUT_RED_FLAGS: List<String> = listOf(
        "tienes tdah", "tienes depresion", "tienes ansiedad", "padeces", "sufres de", "tu diagnostico", "diagnostico es",
        "garantiz", "seguro que mejoraras", "guaranteed", "100% mejor",
        "como tu terapeuta", "soy tu terapeuta", "as your therapist",
        "deberias tomar", "toma esta", "recomiendo medicacion",
        "cura tu", "te curara", "cure your",
        "aumentara tu iq", "subira tu iq", "tu iq subira",
        "tu estado emocional real es", "tu emocion real es", "your real emotional state",
    )

    /** Classifies a user request. Only [CoachRequestCategory.NORMAL_TRAINING] should ever reach the model. */
    fun classifyRequest(rawText: String): CoachRequestCategory {
        val folded = fold(rawText)
        return REQUEST_RULES.firstOrNull { rule -> rule.terms.any { folded.contains(it) } }?.category
            ?: CoachRequestCategory.NORMAL_TRAINING
    }

    /** The fixed, safe response for a non-normal request (never reaches the model). */
    fun responseFor(category: CoachRequestCategory): String = when (category) {
        CoachRequestCategory.CRISIS_SELF_HARM -> CRISIS_RESPONSE
        CoachRequestCategory.IQ_GUARANTEE -> IQ_REFUSAL
        CoachRequestCategory.EMOTIONAL_STATE -> EMOTIONAL_STATE_REDIRECT
        CoachRequestCategory.OUT_OF_SCOPE -> OUT_OF_SCOPE_REDIRECT
        CoachRequestCategory.DIAGNOSIS, CoachRequestCategory.MEDICATION, CoachRequestCategory.DISORDER, CoachRequestCategory.MEDICAL_CLAIM -> SAFE_REFUSAL
        CoachRequestCategory.NORMAL_TRAINING -> SAFE_FALLBACK
    }

    /** Scans a model output. A red-flagged output is BLOCKED and replaced with the safe fallback. */
    fun guardOutput(rawOutput: String): OutputDecision {
        val folded = fold(rawOutput)
        return if (OUTPUT_RED_FLAGS.any { folded.contains(it) }) {
            OutputDecision(OutputVerdict.BLOCK, SAFE_FALLBACK)
        } else {
            OutputDecision(OutputVerdict.ALLOW, rawOutput)
        }
    }

    /**
     * `MB-EMO-0032`: this object used to carry its OWN copy of the accent table and its own `fold`. It
     * was the fourth of five hand-rolled folds in the tree, it was `private` — so no parity test could
     * reach it — and the two Gradle-side copies are the only ones that are genuinely unavoidable (a
     * build task cannot import module code). Measured, five of the seven pairs are load-bearing for the
     * REAL Spanish spelling of the term lists above, and two of those five sit on the crisis path:
     * without `ó` the user typing "autolesion" with its accent stops matching, and without `ñ` so does
     * "hacerme dano" — a message that must return [CRISIS_RESPONSE] would instead reach the model.
     * Consuming the canonical fold makes that drift impossible rather than merely detectable. What is
     * deliberately NOT shared is the MATCHING: this gate uses `contains`, not the scanner's word
     * anchoring, because a gate that BLOCKS is correct to be broader.
     */
    private fun fold(raw: String): String = MindBoostCopySafetyScanner.fold(raw)
}

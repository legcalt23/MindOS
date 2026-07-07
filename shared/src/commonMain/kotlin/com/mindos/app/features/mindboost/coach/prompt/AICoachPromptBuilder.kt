@file:Suppress("MaxLineLength") // Fixed policy/disclaimer prompt lines read best whole.

package com.mindos.app.features.mindboost.coach.prompt

import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.coach.domain.CoachContribution
import com.mindos.app.features.mindboost.coach.domain.CoachSummary

/** A built, size-bounded coach prompt. `text` contains ONLY the structured summary + fixed policy — never raw trials or PII. */
data class CoachPrompt(val text: String, val schemaVersion: Int)

/**
 * MC8u-F — builds a STRUCTURED prompt (never free-form) from the PII-free [CoachSummary]. It always injects the fixed
 * MindBoost policy/disclaimers, marks LOW-confidence data explicitly, states the allowed and forbidden claim sets, and
 * is capped at [MAX_PROMPT_CHARS]. Because the summary is coarse by construction, the prompt cannot contain raw trials,
 * self-report values, PII, or clinical data.
 */
object AICoachPromptBuilder {

    const val MAX_PROMPT_CHARS = 8_000

    const val SYSTEM_POLICY =
        "Eres el entrenador de MindBoost. Reglas no negociables: MindBoost NO diagnostica ni valora la salud. " +
            "Entrena capacidades cognitivas mediante ejercicios. Los datos describen el RENDIMIENTO del usuario en las " +
            "tareas de la app, no rasgos de la persona ni su estado emocional real. No prometas mejoras de IQ ni " +
            "transferencia amplia a la vida diaria. No des consejos médicos ni de medicación. Habla de rendimiento en " +
            "tareas, patrones de entrenamiento, calibración y confianza de medición. Si un dato es de baja confianza, " +
            "dilo. No inventes datos que no estén en el resumen estructurado."

    const val ALLOWED_CLAIMS = "rendimiento en tareas, calibración, consistencia, precisión, patrones de entrenamiento, recomendaciones de práctica, confianza de medición"
    const val FORBIDDEN_CLAIMS = "diagnóstico, condición de salud, medicación, cura, garantía de IQ, interpretación clínica, estado emocional real del usuario"

    fun build(summary: CoachSummary, userRequest: String, currentPlan: String?, candidates: List<String>): CoachPrompt {
        val body = buildString {
            appendLine("[SYSTEM]"); appendLine(SYSTEM_POLICY)
            appendLine("[ALLOWED_CLAIMS] $ALLOWED_CLAIMS")
            appendLine("[FORBIDDEN_CLAIMS] $FORBIDDEN_CLAIMS")
            appendLine("[OVERALL_CONFIDENCE] ${summary.overallConfidence}")
            appendLine("[SCHEMA_VERSION] ${summary.schemaVersion}")
            appendLine("[CONTEXT]")
            summary.contributions.forEach { c -> appendLine(renderContribution(c)) }
            currentPlan?.let { appendLine("[CURRENT_PLAN] $it") }
            if (candidates.isNotEmpty()) appendLine("[RECOMMENDATION_CANDIDATES] ${candidates.joinToString("; ")}")
            appendLine("[USER_REQUEST] $userRequest")
        }
        return CoachPrompt(text = body.take(MAX_PROMPT_CHARS), schemaVersion = summary.schemaVersion)
    }

    private fun renderContribution(c: CoachContribution): String {
        val lowConf = if (c.confidence == CoachConfidenceLevel.LOW) " (LOW_CONFIDENCE)" else ""
        return "- block=${c.blockId} calibration=${c.calibrationState} confidence=${c.confidence}$lowConf" +
            " strengths=${c.strengths.joinToString("|")} weaknesses=${c.weaknesses.joinToString("|")}" +
            " patterns=${c.patterns.joinToString("|")} cautions=${c.cautions.joinToString("|")}"
    }
}

/** The real LLM provider boundary. The app registers a concrete provider; until then the pending default is used. */
interface AICoachProvider {
    suspend fun generate(prompt: CoachPrompt): String
}

/** Thrown by [PendingAICoachProvider] — there is no real model wired yet (the only remaining real-integration piece). */
class AICoachProviderNotWired : IllegalStateException("No real AI Coach LLM provider is wired yet.")

/** Charter default: no model → generation is impossible (the orchestrator turns this into a safe fallback, never a lie). */
object PendingAICoachProvider : AICoachProvider {
    override suspend fun generate(prompt: CoachPrompt): String = throw AICoachProviderNotWired()
}

/** MC8u-F release guard: a release build must wire a real provider, not [PendingAICoachProvider]. */
object AICoachProviderPolicy {
    fun requireRealProviderInRelease(provider: AICoachProvider, isReleaseBuild: Boolean) {
        require(!(isReleaseBuild && provider === PendingAICoachProvider)) {
            "AI Coach: a release build must wire a real AICoachProvider (LLM), not PendingAICoachProvider."
        }
    }
}

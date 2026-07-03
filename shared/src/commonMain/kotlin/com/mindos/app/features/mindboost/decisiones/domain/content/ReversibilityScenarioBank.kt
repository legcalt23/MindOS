package com.mindos.app.features.mindboost.decisiones.domain.content

/** Everyday, non-sensitive life domains for reversibility scenarios (no medical, financial, legal or self-harm). */
enum class ReversibilityDomain { CONSUMER, SOCIAL, WORK_STUDY, PLANNING, HABITS, TOOLS }

/**
 * A CANDIDATE reversibility-triage scenario: a real, everyday decision the user classifies as reversible
 * (Type-2, easily undone) or irreversible (Type-1, hard or impossible to undo). [isReversible] is the
 * author's intended label — NOT yet inter-rater validated.
 */
data class CandidateReversibilityScenario(
    val id: String,
    val prompt: String,
    val isReversible: Boolean,
    val domain: ReversibilityDomain,
)

/**
 * CANDIDATE reversibility scenario bank (30, balanced 15 reversible / 15 irreversible, across six everyday
 * non-sensitive domains). This is REAL, judgeable content that replaces the degenerate generator (template
 * description + coin-flip label) which made the task unjudgeable.
 *
 * STATUS: **UNVALIDATED**. The labels are the author's; there is NO inter-rater validation (≥0.80) yet, so:
 *  - this bank is NOT registered as a profile-updating bank and is NOT wired to the live generator here;
 *  - the SMALL_UNVALIDATED block on `ReversibilityScore` stands → DECISION_MAKING stays CALIBRATING;
 *  - profile re-enablement waits on human validation (a separate, deliberate step).
 *
 * Content boundary (non-clinical product): everyday decisions only — no medical/health, financial/legal
 * advice, or self-harm. Enforced by `ReversibilityScenarioBankTest` and the copy-safety scanner.
 */
object ReversibilityScenarioBank {

    val candidates: List<CandidateReversibilityScenario> = listOf(
        // --- Reversible (Type-2): easily undone ---
        rev("rev-01", "Empezar una prueba gratuita que puedes cancelar cuando quieras.", ReversibilityDomain.CONSUMER),
        rev("rev-02", "Cambiar el fondo de pantalla del teléfono.", ReversibilityDomain.TOOLS),
        rev("rev-03", "Reordenar las carpetas de tu escritorio.", ReversibilityDomain.PLANNING),
        rev("rev-04", "Tomar té en lugar de café durante una semana.", ReversibilityDomain.HABITS),
        rev("rev-05", "Apuntarte a una clase de prueba gratuita.", ReversibilityDomain.SOCIAL),
        rev("rev-06", "Probar una técnica de estudio nueva esta semana.", ReversibilityDomain.WORK_STUDY),
        rev("rev-07", "Activar el modo oscuro de una aplicación.", ReversibilityDomain.TOOLS),
        rev("rev-08", "Mover una reunión a otra hora del mismo día.", ReversibilityDomain.PLANNING),
        rev("rev-09", "Cambiar la ruta de tu paseo de la tarde.", ReversibilityDomain.HABITS),
        rev("rev-10", "Proponer ver una película distinta a la planeada.", ReversibilityDomain.SOCIAL),
        rev("rev-11", "Reordenar las tareas de tu lista del día.", ReversibilityDomain.WORK_STUDY),
        rev("rev-12", "Renombrar un archivo de borrador.", ReversibilityDomain.TOOLS),
        rev("rev-13", "Posponer una tarea no urgente para mañana.", ReversibilityDomain.PLANNING),
        rev("rev-14", "Añadir un artículo al carrito sin finalizar la compra.", ReversibilityDomain.CONSUMER),
        rev("rev-15", "Poner la alarma diez minutos antes de lo habitual.", ReversibilityDomain.HABITS),
        // --- Irreversible (Type-1): hard or impossible to undo ---
        irr("irr-01", "Enviar un mensaje muy duro a un amigo en pleno enfado.", ReversibilityDomain.SOCIAL),
        irr("irr-02", "Borrar de forma permanente una carpeta sin copia de seguridad.", ReversibilityDomain.TOOLS),
        irr("irr-03", "Comprar un billete no reembolsable para mañana.", ReversibilityDomain.CONSUMER),
        irr("irr-04", "Entregar el examen final y salir del aula.", ReversibilityDomain.WORK_STUDY),
        irr("irr-05", "Publicar un comentario polémico con tu nombre visible.", ReversibilityDomain.SOCIAL),
        irr("irr-06", "Tirar a la basura unos papeles únicos sin copia.", ReversibilityDomain.PLANNING),
        irr("irr-07", "Sobrescribir el único original de un documento.", ReversibilityDomain.TOOLS),
        irr("irr-08", "Hacerte un tatuaje visible.", ReversibilityDomain.HABITS),
        irr("irr-09", "Contar a otros un secreto que te confiaron.", ReversibilityDomain.SOCIAL),
        irr("irr-10", "Pulsar 'enviar' en la entrega definitiva de un trabajo.", ReversibilityDomain.WORK_STUDY),
        irr("irr-11", "Vaciar la papelera de reciclaje del ordenador.", ReversibilityDomain.TOOLS),
        irr("irr-12", "Romper el precinto de un producto que solo se cambia sin abrir.", ReversibilityDomain.CONSUMER),
        irr("irr-13", "Confirmar una reserva sin opción de cancelar.", ReversibilityDomain.PLANNING),
        irr("irr-14", "Decir algo hiriente en una discusión.", ReversibilityDomain.SOCIAL),
        irr("irr-15", "Cortar por la mitad un objeto para que quepa.", ReversibilityDomain.HABITS),
    )

    private fun rev(id: String, prompt: String, domain: ReversibilityDomain) =
        CandidateReversibilityScenario(id, prompt, isReversible = true, domain = domain)

    private fun irr(id: String, prompt: String, domain: ReversibilityDomain) =
        CandidateReversibilityScenario(id, prompt, isReversible = false, domain = domain)
}

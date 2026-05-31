package com.mindos.action.understanding

import com.mindos.action.entity.DeterministicConfidence
import com.mindos.action.entity.ExtractedEntity
import com.mindos.action.observability.MatchTrace

/** A ranked intent candidate surfaced when the input is genuinely ambiguous. */
data class MindIntentCandidate(
    val intent: MindIntent,
    val entities: Map<SlotName, ExtractedEntity>,
    /** Human-readable, auditable score summary (the engine serializes its MatchScore here). */
    val scoreSummary: String,
)

/**
 * Outcome of understanding one input. This is the **stable public contract** of the
 * Action Understanding Layer. It deliberately stops at *intent + entities* — converting an
 * intent into an executable, navigable action is the resolver/executor's job (separation of
 * understanding from execution).
 *
 * [trace] is populated only when explain mode is on and never affects the decision.
 */
sealed interface ActionUnderstandingResult {
    val trace: MatchTrace?

    data class Understood(
        val intent: MindIntent,
        val entities: Map<SlotName, ExtractedEntity>,
        val confidence: DeterministicConfidence,
        override val trace: MatchTrace? = null,
    ) : ActionUnderstandingResult

    data class Ambiguous(
        val candidates: List<MindIntentCandidate>,
        override val trace: MatchTrace? = null,
    ) : ActionUnderstandingResult

    data class Rejected(
        val reason: RejectionReason,
        override val trace: MatchTrace? = null,
    ) : ActionUnderstandingResult

    data class NoMatch(
        override val trace: MatchTrace? = null,
    ) : ActionUnderstandingResult
}

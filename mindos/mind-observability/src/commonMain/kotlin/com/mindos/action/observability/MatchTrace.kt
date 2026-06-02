package com.mindos.action.observability

import kotlinx.serialization.Serializable

/** Pipeline stage reached during understanding. */
@Serializable
enum class MatchStage { NORMALIZATION, EXACT, ALIAS, PARAMETRIC, RANKING, DONE }

/** One candidate considered during ranking, with its auditable score string. */
@Serializable
data class CandidateTrace(
    val patternId: String,
    val intentId: String,
    val score: String,
    val won: Boolean,
)

/** One entity extraction attempt (extracted or rejected). */
@Serializable
data class EntityTrace(
    val slot: String,
    val type: String,
    val value: String,
    val detail: String? = null,
)

/**
 * Full explanation of a single understanding pass. Produced **only** when explain mode
 * is enabled; it never influences the decision and stays off the hot path.
 */
@Serializable
data class MatchTrace(
    val rawInput: String,
    val normalizedText: String,
    val localeTag: String,
    val stageReached: MatchStage,
    val evaluatedCandidates: List<CandidateTrace>,
    val extractedEntities: List<EntityTrace>,
    val rejectedEntities: List<EntityTrace>,
    val winnerIntentId: String?,
    val outcome: String,
)

/**
 * Mutable accumulator the engine fills while explain mode is on. Separated from the
 * immutable [MatchTrace] so the engine can build incrementally then [build] once.
 */
class MatchTraceBuilder(
    private val rawInput: String,
    private val normalizedText: String,
    private val localeTag: String,
) {
    private var stage: MatchStage = MatchStage.NORMALIZATION
    private val candidates = ArrayList<CandidateTrace>()
    private val extracted = ArrayList<EntityTrace>()
    private val rejected = ArrayList<EntityTrace>()
    private var winner: String? = null

    fun stage(stage: MatchStage) = apply { this.stage = stage }
    fun candidate(c: CandidateTrace) = apply { candidates.add(c) }
    fun extracted(e: EntityTrace) = apply { extracted.add(e.redacted()) }
    fun rejected(e: EntityTrace) = apply { rejected.add(e.redacted()) }
    fun winner(intentId: String?) = apply { winner = intentId }

    fun build(outcome: String): MatchTrace = MatchTrace(
        rawInput = TraceRedactor.redact("raw_input", rawInput),
        normalizedText = TraceRedactor.redact("normalized_text", normalizedText),
        localeTag = localeTag,
        stageReached = stage,
        evaluatedCandidates = candidates.toList(),
        extractedEntities = extracted.toList(),
        rejectedEntities = rejected.toList(),
        winnerIntentId = winner,
        outcome = outcome,
    )

    private fun EntityTrace.redacted(): EntityTrace =
        copy(value = TraceRedactor.redact(type, value))
}

package com.mindos.app.features.mindboost.creativity.ai

/**
 * Prepared-but-blocked AI-layer boundaries for the Creativity module.
 *
 * Per `10_claude_handoff.md`, the interfaces for the Evaluator Cascade, Privacy Gateway,
 * Cost Budget Gateway and Metric Integrity Layer are defined so downstream wiring has a
 * stable contract — but they are NOT implemented or activated. The only implementations
 * shipped in the local MVP are the deterministic `Blocked*` stubs in this file, which
 * perform NO LLM calls, NO embeddings, NO network I/O and NO external prompts.
 *
 * Every blocked layer returns [CreativityAiOutcome.Blocked] and never echoes or forwards
 * the input it receives, so no practice text can leak through a prepared boundary.
 */

/** Outcome of any AI-layer call. The local MVP only ever produces [Blocked]. */
sealed interface CreativityAiOutcome {
    /** The layer refused to run: AI runtime disabled / evidence gate pending. */
    data object Blocked : CreativityAiOutcome
}

/** Thrown if code ever tries to force-activate a blocked layer. */
class CreativityAiBlockedException(layer: String) :
    IllegalStateException("Creativity AI layer '$layer' is blocked until evidence gates pass.")

/** Layer 3 frontier evaluator boundary. BLOCKED — no LLM, no network. */
interface CreativityEvaluatorCascade {
    val isActive: Boolean
    fun evaluate(localResponseText: String): CreativityAiOutcome
}

/** Privacy Gateway boundary: would redact PII before any evaluator call. BLOCKED. */
interface CreativityPrivacyGateway {
    val isActive: Boolean
    fun redactForEvaluation(rawText: String): CreativityAiOutcome
}

/** Cost Budget Gateway boundary: would hard-cap per-session evaluator spend. BLOCKED. */
interface CreativityCostBudgetGateway {
    val isActive: Boolean
    fun reserveBudget(sessionId: String): CreativityAiOutcome
}

/** Metric Integrity Layer boundary: would score anti-gaming signals. BLOCKED. */
interface CreativityMetricIntegrityLayer {
    val isActive: Boolean
    fun scrutinize(localResponseText: String): CreativityAiOutcome
}

/** Deterministic blocked evaluator: ignores input, performs no work, returns [CreativityAiOutcome.Blocked]. */
object BlockedCreativityEvaluatorCascade : CreativityEvaluatorCascade {
    override val isActive: Boolean = false
    override fun evaluate(localResponseText: String): CreativityAiOutcome = CreativityAiOutcome.Blocked
}

/** Deterministic blocked privacy gateway: never returns the raw text it was given. */
object BlockedCreativityPrivacyGateway : CreativityPrivacyGateway {
    override val isActive: Boolean = false
    override fun redactForEvaluation(rawText: String): CreativityAiOutcome = CreativityAiOutcome.Blocked
}

/** Deterministic blocked cost gateway. */
object BlockedCreativityCostBudgetGateway : CreativityCostBudgetGateway {
    override val isActive: Boolean = false
    override fun reserveBudget(sessionId: String): CreativityAiOutcome = CreativityAiOutcome.Blocked
}

/** Deterministic blocked metric integrity layer. */
object BlockedCreativityMetricIntegrityLayer : CreativityMetricIntegrityLayer {
    override val isActive: Boolean = false
    override fun scrutinize(localResponseText: String): CreativityAiOutcome = CreativityAiOutcome.Blocked
}

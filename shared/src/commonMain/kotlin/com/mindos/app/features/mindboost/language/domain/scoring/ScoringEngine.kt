package com.mindos.app.features.mindboost.language.domain.scoring

import com.mindos.app.features.mindboost.language.domain.model.AttemptFlag
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ConfounderAssessment
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.FatigueState
import com.mindos.app.features.mindboost.language.domain.model.LatencyMs
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import kotlinx.serialization.Serializable

/**
 * Closed scoring contracts (v6 scoring MVP, §5.6 gates, §9.3 result shape). The engine turns
 * a response + an answer key + context into a fully-gated [ScoredResponse]. It is
 * deterministic and offline — the optional Evaluator OS handles open production separately.
 */
fun interface ScoringEngine {
    fun score(
        key: AnswerKey,
        response: ResponsePayload,
        timing: ScoringTiming,
        context: ScoringContext,
    ): ScoredResponse
}

/**
 * Deterministic answer key for a closed item (the scorable content the passport metadata does
 * not carry). [distractorErrors] maps a wrong option to the error pattern it diagnoses, so a
 * single response can surface a reparable pattern (§6.6).
 */
@Serializable
sealed interface AnswerKey {
    @Serializable
    data class SingleChoice(
        val correctOptionId: String,
        val distractorErrors: Map<String, ErrorType> = emptyMap(),
    ) : AnswerKey

    @Serializable
    data class EvidenceSelection(
        val correctOptionId: String,
        val requiredEvidenceIds: Set<String>,
        val distractorErrors: Map<String, ErrorType> = emptyMap(),
    ) : AnswerKey

    @Serializable
    data class Ordering(val correctOrder: List<String>) : AnswerKey
}

/** Response timing/metacognition captured alongside the response. */
@Serializable
data class ScoringTiming(
    val latencyMs: LatencyMs? = null,
    val confidence: Proportion? = null,
)

/**
 * Everything the gate logic needs that is NOT part of the response itself (§5.6). Closed
 * items default [evaluatorConfidence] high because scoring is deterministic.
 */
@Serializable
data class ScoringContext(
    val bankType: BankType,
    val fatigue: FatigueState = FatigueState.FRESH,
    val confounders: ConfounderAssessment = ConfounderAssessment.NONE,
    val isRepeatedItem: Boolean = false,
    val evaluatorConfidence: Double = 1.0,
    val itemInformation: Double = 1.0,
)

/**
 * The scored result (§9.3 simplified). [evidenceWeight] is 0.0 whenever any [flags] forbids a
 * strong update; [isStrongUpdateAllowed] is the explicit boolean the EvidenceEngine keys on.
 */
@Serializable
data class ScoredResponse(
    val correctness: Proportion,
    val latencyMs: LatencyMs?,
    val confidence: Proportion?,
    val errorTypes: List<ErrorType>,
    val evidenceContribution: Proportion,
    val flags: Set<AttemptFlag>,
    val gatesApplied: Set<EvidenceGate>,
    val evidenceWeight: Double,
    val isStrongUpdateAllowed: Boolean,
)

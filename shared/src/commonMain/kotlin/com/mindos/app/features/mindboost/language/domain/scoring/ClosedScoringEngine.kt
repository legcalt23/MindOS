package com.mindos.app.features.mindboost.language.domain.scoring

import com.mindos.app.features.mindboost.language.domain.model.Attempt
import com.mindos.app.features.mindboost.language.domain.model.AttemptFlag
import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TaskVersion
import com.mindos.app.features.mindboost.language.domain.model.TraitCode

/**
 * Default deterministic [ScoringEngine] for closed items. Pure, offline, total.
 *
 * It scores correctness deterministically, then applies the §5.6 gates: ANY raised
 * [AttemptFlag] zeroes the evidence weight and forbids a strong profile update (high fatigue,
 * repeated item, prior-knowledge/variant confounders, low evaluator confidence, unsupported
 * inference). Training-bank items are down-weighted (bank separation) even when clean, because
 * in-template practice is weaker evidence than unseen material.
 */
class ClosedScoringEngine : ScoringEngine {

    override fun score(
        key: AnswerKey,
        response: ResponsePayload,
        timing: ScoringTiming,
        context: ScoringContext,
    ): ScoredResponse {
        val scored = when (key) {
            is AnswerKey.SingleChoice -> scoreSingleChoice(key, response)
            is AnswerKey.EvidenceSelection -> scoreEvidenceSelection(key, response)
            is AnswerKey.Ordering -> scoreOrdering(key, response)
        }
        val flags = deriveFlags(context, scored.unsupported)
        val gates = deriveGates(context, flags)
        val strongAllowed = flags.isEmpty()
        val weight = if (strongAllowed) computeWeight(context, gates) else 0.0
        return ScoredResponse(
            correctness = scored.correctness,
            latencyMs = timing.latencyMs,
            confidence = timing.confidence,
            errorTypes = scored.errorTypes,
            evidenceContribution = scored.evidenceContribution,
            flags = flags,
            gatesApplied = gates,
            evidenceWeight = weight,
            isStrongUpdateAllowed = strongAllowed,
        )
    }

    private data class Scored(
        val correctness: Proportion,
        val errorTypes: List<ErrorType>,
        val evidenceContribution: Proportion,
        val unsupported: Boolean,
    )

    private fun scoreSingleChoice(key: AnswerKey.SingleChoice, response: ResponsePayload): Scored {
        val r = response as? ResponsePayload.MultipleChoice ?: return malformed()
        val correct = r.selectedOptionId == key.correctOptionId
        val errors = if (correct) emptyList() else listOfNotNull(key.distractorErrors[r.selectedOptionId])
        val c = if (correct) Proportion.ONE else Proportion.ZERO
        return Scored(c, errors, c, unsupported = false)
    }

    private fun scoreEvidenceSelection(key: AnswerKey.EvidenceSelection, response: ResponsePayload): Scored {
        val r = response as? ResponsePayload.EvidenceSelection ?: return malformed()
        val optionCorrect = r.selectedOptionId == key.correctOptionId
        val selected = r.selectedEvidenceIds.toSet()
        val required = key.requiredEvidenceIds
        val captured = if (required.isEmpty()) 1.0 else (required intersect selected).size.toDouble() / required.size
        val evidenceComplete = selected.containsAll(required)
        val correctness = when {
            optionCorrect && evidenceComplete -> Proportion.ONE
            optionCorrect -> Proportion(PARTIAL_UNSUPPORTED_CORRECTNESS)
            else -> Proportion.ZERO
        }
        val unsupported = optionCorrect && !evidenceComplete
        val errors = buildList {
            if (unsupported) add(ErrorType.INF_OVER) // right answer, but evidence not pinned
            if (!optionCorrect) key.distractorErrors[r.selectedOptionId]?.let { add(it) }
        }
        return Scored(correctness, errors, Proportion.clamped(captured), unsupported)
    }

    private fun scoreOrdering(key: AnswerKey.Ordering, response: ResponsePayload): Scored {
        val r = response as? ResponsePayload.Ordering ?: return malformed()
        val c = orderingCorrectness(r.orderedIds, key.correctOrder)
        return Scored(c, emptyList(), c, unsupported = false)
    }

    /**
     * Normalized Kendall-tau concordance: fraction of ordered pairs the user got in the right
     * relative order. A wrong item set (missing/extra/duplicated ids) scores zero.
     */
    private fun orderingCorrectness(ordered: List<String>, correct: List<String>): Proportion {
        if (ordered.size != correct.size || ordered.toSet() != correct.toSet()) return Proportion.ZERO
        val rank = correct.withIndex().associate { (i, id) -> id to i }
        var concordant = 0
        var total = 0
        for (i in ordered.indices) {
            for (j in i + 1 until ordered.size) {
                total++
                if (rank.getValue(ordered[i]) < rank.getValue(ordered[j])) concordant++
            }
        }
        return if (total == 0) Proportion.ONE else Proportion.clamped(concordant.toDouble() / total)
    }

    private fun malformed(): Scored =
        Scored(Proportion.ZERO, emptyList(), Proportion.ZERO, unsupported = false)

    private fun deriveFlags(context: ScoringContext, unsupported: Boolean): Set<AttemptFlag> = buildSet {
        if (context.isRepeatedItem) add(AttemptFlag.REPEATED_ITEM)
        if (context.fatigue.isHigh) add(AttemptFlag.HIGH_FATIGUE)
        if (context.confounders.hasHighPriorKnowledge) add(AttemptFlag.HIGH_PRIOR_KNOWLEDGE)
        if (context.confounders.variantBiasRisk) add(AttemptFlag.VARIANT_BIAS_RISK)
        if (context.evaluatorConfidence < LOW_EVALUATOR_CONFIDENCE) add(AttemptFlag.LOW_EVALUATOR_CONFIDENCE)
        if (unsupported) {
            add(AttemptFlag.UNSUPPORTED_INFERENCE)
            add(AttemptFlag.INSUFFICIENT_EVIDENCE)
        }
    }

    private fun deriveGates(context: ScoringContext, flags: Set<AttemptFlag>): Set<EvidenceGate> = buildSet {
        add(EvidenceGate.BASE_ITEM_INFORMATION)
        if (AttemptFlag.REPEATED_ITEM in flags) add(EvidenceGate.NOVELTY)
        if (context.bankType == BankType.TRAINING) add(EvidenceGate.BANK_SEPARATION)
        if (AttemptFlag.HIGH_FATIGUE in flags) add(EvidenceGate.FATIGUE)
        if (AttemptFlag.HIGH_PRIOR_KNOWLEDGE in flags) add(EvidenceGate.CONFOUNDER)
        if (AttemptFlag.VARIANT_BIAS_RISK in flags) add(EvidenceGate.FAIRNESS)
        if (AttemptFlag.LOW_EVALUATOR_CONFIDENCE in flags) add(EvidenceGate.EVALUATOR_CONFIDENCE)
    }

    private fun computeWeight(context: ScoringContext, gates: Set<EvidenceGate>): Double {
        val bankFactor = if (EvidenceGate.BANK_SEPARATION in gates) TRAINING_BANK_FACTOR else 1.0
        return context.itemInformation * context.evaluatorConfidence * bankFactor
    }

    companion object {
        const val LOW_EVALUATOR_CONFIDENCE = 0.6
        const val PARTIAL_UNSUPPORTED_CORRECTNESS = 0.5
        const val TRAINING_BANK_FACTOR = 0.5
    }
}

/**
 * Folds a [ScoredResponse] into a persistable [Attempt] (the §12.6 row shape). Keeps the
 * scoring engine free of identity/timestamp concerns — those are supplied by the caller.
 */
@Suppress("LongParameterList") // assembling the flat attempt record needs the full identity set.
fun ScoredResponse.toAttempt(
    attemptId: AttemptId,
    sessionId: LanguageSessionId,
    taskId: TaskId,
    taskVersion: TaskVersion,
    bankType: BankType,
    primaryTrait: TraitCode,
    operation: OperationType,
    responseMode: ResponseMode,
    response: ResponsePayload,
    createdAtEpochMs: Long,
): Attempt = Attempt(
    attemptId = attemptId,
    sessionId = sessionId,
    taskId = taskId,
    taskVersion = taskVersion,
    bankType = bankType,
    primaryTrait = primaryTrait,
    operation = operation,
    responseMode = responseMode,
    response = response,
    correctness = correctness,
    latencyMs = latencyMs,
    confidence = confidence,
    errorTypes = errorTypes,
    flags = flags,
    evidenceWeight = evidenceWeight,
    createdAtEpochMs = createdAtEpochMs,
)

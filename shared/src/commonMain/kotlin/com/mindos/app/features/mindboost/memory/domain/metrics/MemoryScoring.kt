package com.mindos.app.features.mindboost.memory.domain.metrics

import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.exercise.ComplexSpanTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.PairedAssociatesTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallListTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallType
import com.mindos.app.features.mindboost.memory.domain.model.ConfidenceJudgment
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryTrialOutcome
import com.mindos.app.features.mindboost.memory.domain.model.ReactionTimeMs

/**
 * A single item the user produced during recall, with optional timing and a
 * metacognitive confidence judgment (doc §4 Ej.16 overlay).
 */
data class RecalledWord(
    val word: WordItem,
    val rtMs: ReactionTimeMs? = null,
    val confidence: ConfidenceJudgment? = null,
)

/** One scored response: the classified [outcome] plus the data metrics consume. */
data class ScoredResponse(
    val outcome: MemoryTrialOutcome,
    val rtMs: ReactionTimeMs? = null,
    val confidence: ConfidenceJudgment? = null,
    val category: WordCategory? = null,
)

/**
 * A fully scored trial — the bridge between a Phase-4 [com.mindos.app.features
 * .mindboost.memory.domain.exercise.MemoryTrialPlan] and the metrics engine. The
 * scorer compares the user's recall against the plan's embedded answer key.
 */
data class ScoredTrial(
    val exerciseId: MemoryExerciseId,
    val recallType: RecallType,
    val delayMs: Long,
    val studiedCount: Int,
    val responses: List<ScoredResponse>,
    /**
     * True when [responses] align 1:1 to to-be-recalled slots/probes (span, pairs),
     * so a blank is an explicit OMISSION. False for free recall (a produced list),
     * where unrecalled items are inferred as omissions = studiedCount − hits.
     */
    val slotAligned: Boolean = true,
) {
    init {
        require(studiedCount >= 0) { "studiedCount cannot be negative." }
        require(delayMs >= 0) { "delayMs cannot be negative." }
    }

    fun count(outcome: MemoryTrialOutcome): Int = responses.count { it.outcome == outcome }

    val hits: Int get() = count(MemoryTrialOutcome.HIT)
}

/**
 * Scores user recall against a trial plan's answer key. Pure and deterministic.
 * Memory errors are diagnostic, so the scorer distinguishes intrusions (extra-list),
 * conjunction errors (a different studied item), order errors and omissions.
 */
object MemoryScorer {

    /**
     * Scores EX1 serial recall position-by-position. [recalled] is aligned to the
     * expected positions; a null entry is an omission at that position.
     */
    fun scoreComplexSpan(plan: ComplexSpanTrial, recalled: List<RecalledWord?>): ScoredTrial {
        val expected = plan.expectedRecall
        val studiedIds = expected.map { it.id }.toSet()
        val responses = expected.indices.map { i ->
            val r = recalled.getOrNull(i)
            val outcome = when {
                r == null -> MemoryTrialOutcome.OMISSION
                r.word.id == expected[i].id -> MemoryTrialOutcome.HIT
                r.word.id in studiedIds -> MemoryTrialOutcome.ORDER_ERROR
                else -> MemoryTrialOutcome.INTRUSION
            }
            ScoredResponse(outcome, r?.rtMs, r?.confidence, r?.word?.category)
        }
        return ScoredTrial(
            exerciseId = plan.exerciseId,
            recallType = plan.recallType,
            delayMs = 0,
            studiedCount = expected.size,
            responses = responses,
        )
    }

    /**
     * Scores EX3 list recall (order-independent). Repeated recalls of the same item
     * are deduped (first occurrence kept); items not studied are intrusions.
     */
    fun scoreList(plan: RecallListTrial, recalled: List<RecalledWord>): ScoredTrial {
        val studiedIds = plan.studyList.map { it.id }.toSet()
        val seen = mutableSetOf<String>()
        val responses = recalled.mapNotNull { r ->
            if (!seen.add(r.word.id.value)) return@mapNotNull null // dedupe repetitions
            val outcome =
                if (r.word.id in studiedIds) MemoryTrialOutcome.HIT else MemoryTrialOutcome.INTRUSION
            ScoredResponse(outcome, r.rtMs, r.confidence, r.word.category)
        }
        return ScoredTrial(
            exerciseId = plan.exerciseId,
            recallType = plan.recallType,
            delayMs = plan.delayMs,
            studiedCount = plan.studyList.size,
            responses = responses,
            slotAligned = false, // free/cued recall: produced list, not fixed slots
        )
    }

    /**
     * Scores EX4 cued recall. [answers] is aligned to the plan's probes. An answer
     * that is a studied word but not the expected target is a conjunction error.
     */
    fun scorePairs(plan: PairedAssociatesTrial, answers: List<RecalledWord?>): ScoredTrial {
        val studiedWordIds = plan.studyPairs.flatMap { listOf(it.cue.id, it.target.id) }.toSet()
        val responses = plan.probes.indices.map { i ->
            val probe = plan.probes[i]
            val a = answers.getOrNull(i)
            val outcome = when {
                a == null -> MemoryTrialOutcome.OMISSION
                a.word.id == probe.expectedTarget.id -> MemoryTrialOutcome.HIT
                a.word.id in studiedWordIds -> MemoryTrialOutcome.CONJUNCTION_ERROR
                else -> MemoryTrialOutcome.INTRUSION
            }
            ScoredResponse(outcome, a?.rtMs, a?.confidence, a?.word?.category)
        }
        return ScoredTrial(
            exerciseId = plan.exerciseId,
            recallType = RecallType.CUED,
            delayMs = plan.delayMs,
            studiedCount = plan.probes.size,
            responses = responses,
        )
    }
}

// Content factories carry the full item descriptor; their wide signatures are the model, not a smell.
@file:Suppress("LongParameterList", "MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey
import com.mindos.app.features.mindboost.language.presentation.DemoItem
import com.mindos.app.features.mindboost.language.presentation.DemoOption

/**
 * A selectable language exercise: a named, single-mechanic training task with its own calibrated
 * item bank. Each exercise targets a [primaryTrait] and maps to one elite task family (v6 §7.5).
 * The catalog ([LanguageExerciseCatalog]) groups these so the user can pick one from a menu when
 * entering the Lenguaje module. All content lives in `commonMain`; scoring reuses the closed engine.
 */
data class LanguageExercise(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryTrait: TraitCode,
    val family: String,
    val items: List<DemoItem>,
) {
    /** Distinct calibrated difficulty levels this bank spans (a real desirable-difficulty ladder). */
    val levels: Set<Int> get() = items.map { it.level }.toSet()
}

/** Elite task families (v6 §7.5) each exercise belongs to. Stable ids used in content + analytics. */
object LanguageExerciseFamily {
    const val EVIDENCE_LOCK = "evidence_lock"
    const val SEMANTIC_SURGERY = "semantic_surgery"
    const val COMPRESSION_LADDER = "compression_ladder"
    const val ARGUMENT_XRAY = "argument_xray"
    const val SCOPE_NEGATION = "scope_negation"
    const val SOURCE_TRIAGE = "source_triage"
}

/** Builds a single-choice item. [distractors] maps each wrong option id to the error it diagnoses. */
internal fun single(
    id: String,
    prompt: String,
    options: List<DemoOption>,
    correct: String,
    distractors: Map<String, ErrorType>,
    trait: TraitCode,
    operation: OperationType,
    level: Int,
    bankType: BankType = BankType.TRAINING,
): DemoItem = DemoItem(
    taskId = TaskId(id),
    prompt = prompt,
    responseMode = ResponseMode.MULTIPLE_CHOICE,
    options = options,
    key = AnswerKey.SingleChoice(correctOptionId = correct, distractorErrors = distractors),
    primaryTrait = trait,
    operation = operation,
    bankType = bankType,
    level = level,
)

/** Builds an evidence-selection item: pick the supported conclusion AND mark its evidence spans. */
internal fun evidenceSelection(
    id: String,
    prompt: String,
    options: List<DemoOption>,
    evidence: List<DemoOption>,
    correct: String,
    requiredEvidence: Set<String>,
    distractors: Map<String, ErrorType>,
    trait: TraitCode,
    operation: OperationType,
    level: Int,
    bankType: BankType = BankType.TRAINING,
): DemoItem = DemoItem(
    taskId = TaskId(id),
    prompt = prompt,
    responseMode = ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION,
    options = options,
    evidence = evidence,
    key = AnswerKey.EvidenceSelection(
        correctOptionId = correct,
        requiredEvidenceIds = requiredEvidence,
        distractorErrors = distractors,
    ),
    primaryTrait = trait,
    operation = operation,
    bankType = bankType,
    level = level,
)

/** Builds an ordering item; [ordered] is the canonical correct order (ids scored by Kendall-tau). */
internal fun ordering(
    id: String,
    prompt: String,
    ordered: List<DemoOption>,
    trait: TraitCode,
    operation: OperationType,
    level: Int,
    bankType: BankType = BankType.TRAINING,
): DemoItem = DemoItem(
    taskId = TaskId(id),
    prompt = prompt,
    responseMode = ResponseMode.ORDERING,
    options = ordered,
    key = AnswerKey.Ordering(correctOrder = ordered.map { it.id }),
    primaryTrait = trait,
    operation = operation,
    bankType = bankType,
    level = level,
)

/** Compact option builder to keep dense content banks readable. */
internal fun opt(id: String, text: String): DemoOption = DemoOption(id, text)

package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/**
 * Render-agnostic plans a Phase-4 generator produces for one trial. A plan carries
 * BOTH the study material to present AND the scoring key (the correct answers) —
 * the generator is the only component that knows ground truth, so it embeds it.
 * The UI renders the study/test phases; Phase-5 metrics will score responses
 * against the embedded key. No timing-at-presentation here (the runtime stamps that).
 */
sealed interface MemoryTrialPlan {
    val exerciseId: MemoryExerciseId
}

/** How the user must reproduce the studied material. */
enum class RecallType {
    /** Any order (EX3 free recall). */
    FREE,

    /** With a retrieval cue, e.g. a category prompt (EX3 cued recall). */
    CUED,

    /** In presented order (EX1 span forward). */
    SERIAL_FORWARD,

    /** In reverse order (EX1 span backward). */
    SERIAL_BACKWARD,
}

/** Direction of a cued-recall probe for paired associates (EX4). */
enum class RecallDirection {
    /** Cue is the pair's cue, expected answer is the target (A→B). */
    FORWARD,

    /** Cue is the pair's target, expected answer is the cue (B→A). */
    BACKWARD,
}

// ---------- EX1 · Span Complejo ----------

/** One step of a complex-span sequence: either an item to remember or a processing task. */
sealed interface SpanStep

/** An item to hold in working memory. */
data class Memorandum(val word: WordItem) : SpanStep

/**
 * A processing task interleaved between memoranda (e.g. "3 + 4 = 8" → false). Forces
 * the dual-task demand that distinguishes complex span from simple span.
 */
data class ProcessingTask(val prompt: String, val correct: Boolean) : SpanStep

/**
 * EX1 trial: memoranda interleaved with processing tasks, recalled in [recallType]
 * order. [expectedRecall] is the correct answer in the order the user must produce.
 */
data class ComplexSpanTrial(
    override val exerciseId: MemoryExerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
    val steps: List<SpanStep>,
    val recallType: RecallType,
    val expectedRecall: List<WordItem>,
) : MemoryTrialPlan {
    init {
        require(expectedRecall.isNotEmpty()) { "ComplexSpanTrial.expectedRecall cannot be empty." }
        require(recallType == RecallType.SERIAL_FORWARD || recallType == RecallType.SERIAL_BACKWARD) {
            "ComplexSpanTrial requires a serial recall type (was $recallType)."
        }
        val memoranda = steps.filterIsInstance<Memorandum>().map { it.word }
        require(memoranda.toSet() == expectedRecall.toSet()) {
            "expectedRecall must contain exactly the memoranda present in steps."
        }
    }
}

// ---------- EX3 · Lista con Estrategia ----------

/**
 * EX3 trial: a study list recalled after [delayMs]. [expected] is the scoring key
 * (the set studied; order matters only for serial variants, not modelled in V1).
 * [cueCategory] is non-null only for [RecallType.CUED].
 */
data class RecallListTrial(
    override val exerciseId: MemoryExerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
    val studyList: List<WordItem>,
    val recallType: RecallType,
    val studyMsPerItem: Long,
    val delayMs: Long,
    val cueCategory: WordCategory? = null,
    val expected: List<WordItem> = studyList,
) : MemoryTrialPlan {
    init {
        require(studyList.isNotEmpty()) { "RecallListTrial.studyList cannot be empty." }
        require(studyMsPerItem > 0) { "studyMsPerItem must be > 0 (was $studyMsPerItem)." }
        require(delayMs >= 0) { "delayMs cannot be negative (was $delayMs)." }
        require(recallType == RecallType.FREE || recallType == RecallType.CUED) {
            "RecallListTrial requires FREE or CUED recall (was $recallType)."
        }
        require((recallType == RecallType.CUED) == (cueCategory != null)) {
            "cueCategory must be set iff recallType is CUED."
        }
    }
}

// ---------- EX4 · Pares Asociados ----------

/** A cued-recall probe: present [cue], expect [expectedTarget], in [direction]. */
data class CuedRecallProbe(
    val cue: WordItem,
    val expectedTarget: WordItem,
    val direction: RecallDirection,
) {
    init {
        require(cue.id != expectedTarget.id) { "A probe cannot expect the cue itself." }
    }
}

/**
 * EX4 trial: study [studyPairs], then answer [probes] (unidirectional or
 * bidirectional) after [delayMs].
 */
data class PairedAssociatesTrial(
    override val exerciseId: MemoryExerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
    val studyPairs: List<PairItem>,
    val probes: List<CuedRecallProbe>,
    val studyMsPerPair: Long,
    val delayMs: Long,
) : MemoryTrialPlan {
    init {
        require(studyPairs.isNotEmpty()) { "PairedAssociatesTrial.studyPairs cannot be empty." }
        require(probes.isNotEmpty()) { "PairedAssociatesTrial.probes cannot be empty." }
        require(studyMsPerPair > 0) { "studyMsPerPair must be > 0 (was $studyMsPerPair)." }
        require(delayMs >= 0) { "delayMs cannot be negative (was $delayMs)." }
    }
}

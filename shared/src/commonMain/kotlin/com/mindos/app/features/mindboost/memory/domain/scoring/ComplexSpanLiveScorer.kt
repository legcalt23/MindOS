package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.memory.domain.exercise.ComplexSpanTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.ProcessingTask
import com.mindos.app.features.mindboost.memory.domain.metrics.ScoredTrial
import com.mindos.app.features.mindboost.memory.domain.model.MemoryTrialOutcome

/**
 * P1 EX1 · Bridges a LIVE span trial to the complex-span model. Combines (a) the user's processing
 * answers vs the embedded [ProcessingTask] ground truth — the dual-task validity that makes this a
 * complex span, not a simple span — with (b) the already-scored recall, into one [SpanTrial]. One live
 * session = one [SpanTrial]; these accumulate across sessions toward the block/session gates in
 * [ComplexSpanScorer].
 */
object ComplexSpanLiveScorer {

    /**
     * A trial's processing is valid iff there ARE processing tasks and the user answered EVERY one
     * correctly (their yes/no matches the task's ground-truth correctness). Standard complex-span: a
     * trial only counts toward the span when the concurrent-processing demand was met.
     */
    fun processingValid(tasks: List<ProcessingTask>, answers: List<Boolean>): Boolean =
        tasks.isNotEmpty() && answers.size == tasks.size && tasks.indices.all { answers[it] == tasks[it].correct }

    /** Folds one live span trial into a [SpanTrial] (processing validity + scored recall). */
    fun toSpanTrial(
        plan: ComplexSpanTrial,
        processingAnswers: List<Boolean>,
        scoredRecall: ScoredTrial,
    ): SpanTrial {
        val tasks = plan.steps.filterIsInstance<ProcessingTask>()
        return SpanTrial(
            sequenceLength = plan.expectedRecall.size,
            processingCorrect = processingValid(tasks, processingAnswers),
            serialCorrectItems = scoredRecall.hits,
            intrusions = scoredRecall.count(MemoryTrialOutcome.INTRUSION),
        )
    }
}

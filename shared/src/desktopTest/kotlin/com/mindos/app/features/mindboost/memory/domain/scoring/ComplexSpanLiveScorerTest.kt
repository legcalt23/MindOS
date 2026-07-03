package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.memory.domain.exercise.ProcessingTask
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P1 EX1 · processing validity per trial (the dual-task gate that distinguishes complex from simple span). */
class ComplexSpanLiveScorerTest {

    private fun task(correct: Boolean) = ProcessingTask(prompt = "a + b = c", correct = correct)

    @Test
    fun allAnswersMatchGroundTruth_isValid() {
        val tasks = listOf(task(true), task(false), task(true))
        val answers = listOf(true, false, true) // user matches truth on every task
        assertTrue(ComplexSpanLiveScorer.processingValid(tasks, answers))
    }

    @Test
    fun oneAnswerWrong_isInvalid() {
        val tasks = listOf(task(true), task(false))
        val answers = listOf(true, true) // second answer disagrees with the (false) ground truth
        assertFalse(ComplexSpanLiveScorer.processingValid(tasks, answers))
    }

    @Test
    fun answerCountMismatch_isInvalid() {
        assertFalse(ComplexSpanLiveScorer.processingValid(listOf(task(true), task(true)), listOf(true)))
    }

    @Test
    fun noProcessingTasks_isInvalid_notAComplexSpan() {
        assertFalse(ComplexSpanLiveScorer.processingValid(emptyList(), emptyList()))
    }
}

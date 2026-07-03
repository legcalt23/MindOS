package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey
import com.mindos.app.features.mindboost.language.presentation.exercises.LanguageExercise
import com.mindos.app.features.mindboost.language.presentation.exercises.LanguageExerciseCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the menu navigation added on top of the demo flow: start at the menu, pick an exercise, run it
 * to completion, and return to the menu. The legacy single-bank path (menuMode = false) is unchanged.
 */
class LanguageMenuNavigationTest {

    private fun runToDone(holder: LanguageSessionStateHolder, exercise: LanguageExercise) {
        holder.start(exercise)
        exercise.items.forEach { item ->
            assertEquals(LanguageStage.QUESTION, holder.state.value.stage)
            holder.submit(correctResponse(item))
            holder.next()
        }
    }

    @Test
    fun menuModeStartsAtTheMenu() {
        val holder = LanguageSessionStateHolder("u1", menuMode = true)
        assertEquals(LanguageStage.MENU, holder.state.value.stage)
        assertTrue(holder.menu.size >= 5, "menu should offer the exercises, got ${holder.menu.size}")
        assertEquals(LanguageExerciseCatalog.MIXED_ID, holder.menu.first().id, "mixed session listed first")
    }

    @Test
    fun startLoadsTheChosenExerciseFromItsFirstQuestion() {
        val exercise = LanguageExerciseCatalog.exercises.first()
        val holder = LanguageSessionStateHolder("u1", menuMode = true)
        holder.start(exercise)
        val s = holder.state.value
        assertEquals(LanguageStage.QUESTION, s.stage)
        assertEquals(exercise.items.first().taskId, s.item?.taskId)
        assertEquals(exercise.items.size, s.totalItems)
        assertEquals(exercise.title, s.exerciseTitle)
        assertEquals(1, s.itemNumber)
    }

    @Test
    fun completingAnExerciseReachesDoneAndBackToMenuReturns() {
        val exercise = LanguageExerciseCatalog.exercises.first()
        val holder = LanguageSessionStateHolder("u1", menuMode = true)
        runToDone(holder, exercise)
        assertEquals(LanguageStage.DONE, holder.state.value.stage)
        assertEquals(100, holder.state.value.accuracyPercent)
        assertTrue(holder.state.value.skillLines.isNotEmpty())

        holder.backToMenu()
        assertEquals(LanguageStage.MENU, holder.state.value.stage)
    }

    @Test
    fun aSecondExerciseCanBeStartedAfterReturningToTheMenu() {
        val holder = LanguageSessionStateHolder("u1", menuMode = true)
        runToDone(holder, LanguageExerciseCatalog.exercises[0])
        holder.backToMenu()
        val second = LanguageExerciseCatalog.exercises[1]
        holder.start(second)
        assertEquals(LanguageStage.QUESTION, holder.state.value.stage)
        assertEquals(second.items.first().taskId, holder.state.value.item?.taskId)
    }

    @Test
    fun mixedSessionRunsAcrossExercises() {
        val holder = LanguageSessionStateHolder("u1", menuMode = true)
        runToDone(holder, LanguageExerciseCatalog.mixedSession)
        assertEquals(LanguageStage.DONE, holder.state.value.stage)
    }

    @Test
    fun backToMenuIsANoOpInTheLegacySingleBankFlow() {
        val holder = LanguageSessionStateHolder("u1") // menuMode = false
        assertEquals(LanguageStage.QUESTION, holder.state.value.stage)
        holder.backToMenu()
        assertEquals(LanguageStage.QUESTION, holder.state.value.stage)
    }

    private fun correctResponse(item: DemoItem): ResponsePayload = when (val k = item.key) {
        is AnswerKey.SingleChoice -> ResponsePayload.MultipleChoice(k.correctOptionId)
        is AnswerKey.EvidenceSelection ->
            ResponsePayload.EvidenceSelection(k.correctOptionId, k.requiredEvidenceIds.toList())

        is AnswerKey.Ordering -> ResponsePayload.Ordering(k.correctOrder)
    }
}

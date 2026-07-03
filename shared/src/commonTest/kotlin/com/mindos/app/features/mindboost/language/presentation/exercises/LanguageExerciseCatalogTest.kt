package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey
import com.mindos.app.features.mindboost.language.domain.scoring.ClosedScoringEngine
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringContext
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringTiming
import com.mindos.app.features.mindboost.language.presentation.DemoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Content QA for the eligible-exercise catalog. Validates calibration (≥5 items spanning ≥2 levels),
 * key well-formedness, and — through the REAL [ClosedScoringEngine] — that every correct answer scores
 * 1.0 and every distractor is both wrong and diagnoses the error it claims to. This is the per-bank gate.
 */
class LanguageExerciseCatalogTest {

    private val scoring = ClosedScoringEngine()
    private val all = LanguageExerciseCatalog.exercises
    private val everyItem: List<DemoItem> = all.flatMap { it.items }

    @Test
    fun catalogOffersAtLeastFiveDistinctExercises() {
        assertTrue(all.size >= 5, "expected >=5 exercises, got ${all.size}")
        assertEquals(all.size, all.map { it.id }.toSet().size, "exercise ids must be unique")
        assertEquals(all.size, all.map { it.family }.toSet().size, "each exercise is a distinct family")
        all.forEach {
            assertTrue(it.title.isNotBlank(), "${it.id}: title")
            assertTrue(it.subtitle.isNotBlank(), "${it.id}: subtitle")
        }
    }

    @Test
    fun everyExerciseHasAtLeastFiveCalibratedItems() {
        all.forEach { ex ->
            assertTrue(ex.items.size >= 5, "${ex.id} needs >=5 items, got ${ex.items.size}")
            assertTrue(ex.levels.size >= 2, "${ex.id} must span >=2 difficulty levels, got ${ex.levels}")
        }
    }

    @Test
    fun everyItemKeyIsWellFormed() {
        everyItem.forEach { item ->
            val optionIds = item.options.map { it.id }.toSet()
            assertEquals(item.options.size, optionIds.size, "${item.taskId.value}: option ids unique")
            when (val k = item.key) {
                is AnswerKey.SingleChoice ->
                    assertTrue(k.correctOptionId in optionIds, "${item.taskId.value}: correct option exists")

                is AnswerKey.EvidenceSelection -> {
                    assertTrue(k.correctOptionId in optionIds, "${item.taskId.value}: correct option exists")
                    val evidenceIds = item.evidence.map { it.id }.toSet()
                    assertTrue(k.requiredEvidenceIds.isNotEmpty(), "${item.taskId.value}: required evidence set")
                    assertTrue(
                        evidenceIds.containsAll(k.requiredEvidenceIds),
                        "${item.taskId.value}: required evidence ids exist",
                    )
                }

                is AnswerKey.Ordering ->
                    assertEquals(optionIds, k.correctOrder.toSet(), "${item.taskId.value}: order ids match options")
            }
        }
    }

    @Test
    fun correctResponseScoresPerfectForEveryItem() {
        everyItem.forEach { item ->
            val scored = scoring.score(
                item.key,
                correctResponse(item),
                ScoringTiming(),
                ScoringContext(bankType = item.bankType),
            )
            assertEquals(1.0, scored.correctness.value, 1e-9, "${item.taskId.value}: correct answer -> 1.0")
        }
    }

    @Test
    fun everyWrongOptionIsWrongAndDiagnosesItsError() {
        everyItem.forEach { item ->
            val (correctId, distractors) = when (val k = item.key) {
                is AnswerKey.SingleChoice -> k.correctOptionId to k.distractorErrors
                is AnswerKey.EvidenceSelection -> k.correctOptionId to k.distractorErrors
                is AnswerKey.Ordering -> "" to emptyMap()
            }
            // Quality: no silent un-mapped distractor — every wrong option carries a diagnostic error.
            val wrongOptions = item.options.map { it.id }.filter { it != correctId }.toSet()
            if (item.key !is AnswerKey.Ordering) {
                assertEquals(wrongOptions, distractors.keys, "${item.taskId.value}: every wrong option mapped")
            }
            distractors.forEach { (optionId, error) ->
                assertNotEquals(correctId, optionId, "${item.taskId.value}: distractor != correct")
                val scored = scoring.score(
                    item.key,
                    wrongResponse(item, optionId),
                    ScoringTiming(),
                    ScoringContext(bankType = item.bankType),
                )
                assertTrue(scored.correctness.value < 1.0, "${item.taskId.value}: $optionId is not correct")
                assertTrue(error in scored.errorTypes, "${item.taskId.value}: $optionId diagnoses $error")
            }
        }
    }

    @Test
    fun itemIdsAreUniqueAcrossExercises() {
        val ids = everyItem.map { it.taskId.value }
        assertEquals(ids.size, ids.toSet().size, "task ids must be unique across the catalog")
    }

    @Test
    fun mixedSessionInterleavesOneItemPerExercise() {
        val mixed = LanguageExerciseCatalog.mixedSession
        assertEquals(all.size, mixed.items.size, "mixed session has one item per exercise")
        assertEquals(LanguageExerciseCatalog.MIXED_ID, LanguageExerciseCatalog.menu.first().id, "mixed listed first")
        assertEquals(mixed, LanguageExerciseCatalog.byId(LanguageExerciseCatalog.MIXED_ID), "byId resolves mixed")
        all.forEach { assertEquals(it, LanguageExerciseCatalog.byId(it.id), "byId resolves ${it.id}") }
    }

    private fun correctResponse(item: DemoItem): ResponsePayload = when (val k = item.key) {
        is AnswerKey.SingleChoice -> ResponsePayload.MultipleChoice(k.correctOptionId)
        is AnswerKey.EvidenceSelection ->
            ResponsePayload.EvidenceSelection(k.correctOptionId, k.requiredEvidenceIds.toList())

        is AnswerKey.Ordering -> ResponsePayload.Ordering(k.correctOrder)
    }

    private fun wrongResponse(item: DemoItem, optionId: String): ResponsePayload = when (val k = item.key) {
        is AnswerKey.SingleChoice -> ResponsePayload.MultipleChoice(optionId)
        is AnswerKey.EvidenceSelection ->
            ResponsePayload.EvidenceSelection(optionId, k.requiredEvidenceIds.toList())

        is AnswerKey.Ordering -> ResponsePayload.Ordering(emptyList())
    }
}

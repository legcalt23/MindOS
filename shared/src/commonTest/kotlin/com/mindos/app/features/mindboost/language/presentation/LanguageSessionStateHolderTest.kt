package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the demo flow across the three closed exercise models (single-choice, evidence-selection,
 * ordering) through the real engines, plus the honest claim-safe summary and guard rails.
 */
class LanguageSessionStateHolderTest {

    private val evidenceItem = LanguageDemoBank.items.first {
        it.responseMode == ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION
    }
    private val orderingItem = LanguageDemoBank.items.first { it.responseMode == ResponseMode.ORDERING }

    private fun correctResponse(item: DemoItem): ResponsePayload = when (val k = item.key) {
        is AnswerKey.SingleChoice -> ResponsePayload.MultipleChoice(k.correctOptionId)
        is AnswerKey.EvidenceSelection ->
            ResponsePayload.EvidenceSelection(k.correctOptionId, k.requiredEvidenceIds.toList())
        is AnswerKey.Ordering -> ResponsePayload.Ordering(k.correctOrder)
    }

    @Test
    fun startsOnTheFirstQuestion() {
        val s = LanguageSessionStateHolder("u1").state.value
        assertEquals(LanguageStage.QUESTION, s.stage)
        assertEquals(1, s.itemNumber)
        assertEquals(LanguageDemoBank.items.size, s.totalItems)
        assertEquals(LanguageDemoBank.items.first().taskId, s.item?.taskId)
    }

    @Test
    fun singleChoiceCorrectShowsPositiveFeedback() {
        val holder = LanguageSessionStateHolder("u1")
        holder.answer("b") // item 1 correct
        val s = holder.state.value
        assertEquals(LanguageStage.FEEDBACK, s.stage)
        assertEquals(true, s.lastAnswerCorrect)
        assertTrue(s.feedback.startsWith("Correcto"))
    }

    @Test
    fun evidenceSelectionFullySupportedIsCorrect() {
        val holder = LanguageSessionStateHolder("u1", items = listOf(evidenceItem))
        holder.submit(correctResponse(evidenceItem))
        assertEquals(true, holder.state.value.lastAnswerCorrect)
    }

    @Test
    fun evidenceSelectionWithoutEvidenceIsCappedAndNotCorrect() {
        val key = evidenceItem.key as AnswerKey.EvidenceSelection
        val holder = LanguageSessionStateHolder("u1", items = listOf(evidenceItem))
        holder.submit(ResponsePayload.EvidenceSelection(key.correctOptionId, emptyList()))
        val s = holder.state.value
        assertFalse(s.lastAnswerCorrect!!)
        assertTrue(s.feedback.startsWith("Revisa:")) // right option but unsupported -> repair hint
    }

    @Test
    fun orderingExactIsCorrectAndWrongOrderIsNot() {
        val key = orderingItem.key as AnswerKey.Ordering
        val ok = LanguageSessionStateHolder("u1", items = listOf(orderingItem))
        ok.submit(ResponsePayload.Ordering(key.correctOrder))
        assertEquals(true, ok.state.value.lastAnswerCorrect)

        val wrong = LanguageSessionStateHolder("u1", items = listOf(orderingItem))
        wrong.submit(ResponsePayload.Ordering(key.correctOrder.reversed()))
        assertFalse(wrong.state.value.lastAnswerCorrect!!)
    }

    @Test
    fun submitIsIgnoredOutsideTheQuestionStage() {
        val holder = LanguageSessionStateHolder("u1")
        holder.answer("b") // -> FEEDBACK
        val snapshot = holder.state.value
        holder.answer("a") // ignored
        assertEquals(snapshot, holder.state.value)
    }

    @Test
    fun completingEveryItemSummarizesEachTrainedSkill() {
        val holder = LanguageSessionStateHolder("u1")
        LanguageDemoBank.items.forEach { item ->
            assertEquals(LanguageStage.QUESTION, holder.state.value.stage)
            assertEquals(item.taskId, holder.state.value.item?.taskId)
            holder.submit(correctResponse(item))
            holder.next()
        }
        val s = holder.state.value
        assertEquals(LanguageStage.DONE, s.stage)
        assertEquals(100, s.accuracyPercent)
        // Several distinct skills were trained; each is summarized with its honest evidence state.
        assertTrue(s.skillLines.size >= 4)
        assertTrue(
            s.skillLines.any {
                it.contains(TraitCode.INF.display) && it.contains(EvidenceState.E1_TRAINED_IMPROVEMENT.stateLabel)
            },
        )
        assertTrue(s.skillLines.any { it.contains(TraitCode.LIT.display) })
        assertTrue(s.summaryNote.isNotBlank())
    }

    @Test
    fun bankCoversSeveralDistinctSkills() {
        val traits = LanguageDemoBank.items.map { it.primaryTrait }.toSet()
        assertTrue(traits.size >= 4, "expected several skills, got $traits")
    }

    @Test
    fun restartReturnsToTheFirstQuestion() {
        val holder = LanguageSessionStateHolder("u1")
        holder.answer("b")
        holder.next()
        holder.restart()
        val s = holder.state.value
        assertEquals(LanguageStage.QUESTION, s.stage)
        assertEquals(1, s.itemNumber)
        assertEquals(0, s.correctCount)
    }
}

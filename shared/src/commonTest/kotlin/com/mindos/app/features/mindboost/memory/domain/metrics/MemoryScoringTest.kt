package com.mindos.app.features.mindboost.memory.domain.metrics

import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.exercise.ComplexSpanTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.CuedRecallProbe
import com.mindos.app.features.mindboost.memory.domain.exercise.Memorandum
import com.mindos.app.features.mindboost.memory.domain.exercise.PairedAssociatesTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallDirection
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallListTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallType
import com.mindos.app.features.mindboost.memory.domain.model.MemoryTrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryScoringTest {

    private fun w(id: String, category: WordCategory = WordCategory.ANIMALS) = WordItem(
        id = WordId(id),
        text = "w$id",
        category = category,
        concreteness = Concreteness.CONCRETE,
        frequency = WordFrequency.HIGH,
        lengthChars = 3,
    )

    // ---- EX1 span ----

    @Test
    fun span_classifiesHitOrderErrorIntrusionOmission() {
        val a = w("a"); val b = w("b"); val c = w("c")
        val plan = ComplexSpanTrial(
            steps = listOf(Memorandum(a), Memorandum(b), Memorandum(c)),
            recallType = RecallType.SERIAL_FORWARD,
            expectedRecall = listOf(a, b, c),
        )
        // pos0 correct(a); pos1 wrong-but-studied(c)->ORDER_ERROR; pos2 missing->OMISSION
        val scored = MemoryScorer.scoreComplexSpan(
            plan,
            recalled = listOf(RecalledWord(a), RecalledWord(c), null),
        )
        assertEquals(
            listOf(MemoryTrialOutcome.HIT, MemoryTrialOutcome.ORDER_ERROR, MemoryTrialOutcome.OMISSION),
            scored.responses.map { it.outcome },
        )
        assertEquals(3, scored.studiedCount)
    }

    @Test
    fun span_extraListAnswerIsIntrusion() {
        val a = w("a"); val b = w("b"); val z = w("z")
        val plan = ComplexSpanTrial(
            steps = listOf(Memorandum(a), Memorandum(b)),
            recallType = RecallType.SERIAL_FORWARD,
            expectedRecall = listOf(a, b),
        )
        val scored = MemoryScorer.scoreComplexSpan(plan, listOf(RecalledWord(a), RecalledWord(z)))
        assertEquals(MemoryTrialOutcome.INTRUSION, scored.responses[1].outcome)
    }

    // ---- EX3 list ----

    @Test
    fun list_hitsIntrusionsAndDedupesRepetitions() {
        val a = w("a"); val b = w("b"); val z = w("z")
        val plan = RecallListTrial(
            studyList = listOf(a, b),
            recallType = RecallType.FREE,
            studyMsPerItem = 1000,
            delayMs = 0,
        )
        val scored = MemoryScorer.scoreList(
            plan,
            recalled = listOf(RecalledWord(a), RecalledWord(a), RecalledWord(z)), // a repeated
        )
        // a (HIT), a deduped, z (INTRUSION)
        assertEquals(listOf(MemoryTrialOutcome.HIT, MemoryTrialOutcome.INTRUSION), scored.responses.map { it.outcome })
        assertEquals(1, scored.hits)
        assertEquals(2, scored.studiedCount)
    }

    // ---- EX4 pairs ----

    @Test
    fun pairs_classifiesHitConjunctionIntrusionOmission() {
        val a1 = w("a1"); val b1 = w("b1"); val a2 = w("a2"); val b2 = w("b2"); val z = w("z")
        val pair1 = PairItem(PairId("p1"), a1, b1, AssociationStrength.MODERATE)
        val pair2 = PairItem(PairId("p2"), a2, b2, AssociationStrength.MODERATE)
        val plan = PairedAssociatesTrial(
            studyPairs = listOf(pair1, pair2),
            probes = listOf(
                CuedRecallProbe(a1, b1, RecallDirection.FORWARD), // answer b1 -> HIT
                CuedRecallProbe(a2, b2, RecallDirection.FORWARD), // answer b1 (other studied) -> CONJUNCTION
            ) + listOf(
                CuedRecallProbe(b1, a1, RecallDirection.BACKWARD), // answer z -> INTRUSION
                CuedRecallProbe(b2, a2, RecallDirection.BACKWARD), // null -> OMISSION
            ),
            studyMsPerPair = 3000,
            delayMs = 5000,
        )
        val scored = MemoryScorer.scorePairs(
            plan,
            answers = listOf(RecalledWord(b1), RecalledWord(b1), RecalledWord(z), null),
        )
        assertEquals(
            listOf(
                MemoryTrialOutcome.HIT,
                MemoryTrialOutcome.CONJUNCTION_ERROR,
                MemoryTrialOutcome.INTRUSION,
                MemoryTrialOutcome.OMISSION,
            ),
            scored.responses.map { it.outcome },
        )
        assertEquals(5000, scored.delayMs)
        assertEquals(RecallType.CUED, scored.recallType)
    }
}

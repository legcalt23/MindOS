package com.mindos.app.features.mindboost.memory.domain.content.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PairItemTest {

    private fun word(id: String, text: String) = WordItem(
        id = WordId(id),
        text = text,
        category = WordCategory.OBJECTS,
        concreteness = Concreteness.CONCRETE,
        frequency = WordFrequency.MEDIUM,
        lengthChars = text.length,
    )

    @Test
    fun selfPair_throws() {
        val w = word("w1", "mesa")
        assertFailsWith<IllegalArgumentException> {
            PairItem(PairId("p1"), cue = w, target = w, associationStrength = AssociationStrength.WEAK)
        }
    }

    @Test
    fun validPair_constructs() {
        val pair = PairItem(
            id = PairId("p1"),
            cue = word("w1", "mesa"),
            target = word("w2", "libro"),
            associationStrength = AssociationStrength.MODERATE,
        )
        assertEquals(AssociationStrength.MODERATE, pair.associationStrength)
    }

    @Test
    fun wordIds_returnsCueAndTarget() {
        val pair = PairItem(
            id = PairId("p1"),
            cue = word("w1", "mesa"),
            target = word("w2", "libro"),
            associationStrength = AssociationStrength.STRONG,
        )
        assertEquals(setOf(WordId("w1"), WordId("w2")), pair.wordIds())
    }

    @Test
    fun blankPairId_throws() {
        assertFailsWith<IllegalArgumentException> { PairId(" ") }
    }
}

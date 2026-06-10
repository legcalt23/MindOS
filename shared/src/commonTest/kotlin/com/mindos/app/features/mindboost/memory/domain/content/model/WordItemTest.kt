package com.mindos.app.features.mindboost.memory.domain.content.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WordItemTest {

    private fun word(
        text: String = "perro",
        lengthChars: Int = "perro".length,
    ) = WordItem(
        id = WordId("an1"),
        text = text,
        category = WordCategory.ANIMALS,
        concreteness = Concreteness.CONCRETE,
        frequency = WordFrequency.HIGH,
        lengthChars = lengthChars,
    )

    @Test
    fun blankText_throws() {
        assertFailsWith<IllegalArgumentException> { word(text = " ") }
    }

    @Test
    fun nonPositiveLength_throws() {
        assertFailsWith<IllegalArgumentException> { word(lengthChars = 0) }
    }

    @Test
    fun lengthAboveMax_throws() {
        assertFailsWith<IllegalArgumentException> { word(lengthChars = WordItem.MAX_WORD_LENGTH + 1) }
    }

    @Test
    fun validWord_preservesAllTags() {
        val w = word()
        assertEquals(WordCategory.ANIMALS, w.category)
        assertEquals(Concreteness.CONCRETE, w.concreteness)
        assertEquals(WordFrequency.HIGH, w.frequency)
        assertEquals("perro".length, w.lengthChars)
    }

    @Test
    fun blankWordId_throws() {
        assertFailsWith<IllegalArgumentException> { WordId("") }
    }
}

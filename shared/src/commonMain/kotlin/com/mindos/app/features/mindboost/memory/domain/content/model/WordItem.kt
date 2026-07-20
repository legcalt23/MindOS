package com.mindos.app.features.mindboost.memory.domain.content.model

import kotlin.jvm.JvmInline

/**
 * Stable identity of a word in a content pool. A string id (not a positional
 * index) so pools never couple to each other by position and ids stay stable as
 * pools grow.
 */
@JvmInline
value class WordId(val value: String) {
    init {
        require(value.isNotBlank()) { "WordId cannot be blank." }
    }
}

/**
 * Semantic category of a word. Drives categorical clustering (ARC, doc §6) and
 * categorical interference (same-category lists overlap more, doc §4 Ej.11/§5 axis I).
 */
enum class WordCategory {
    ANIMALS,
    VEHICLES,
    FOODS,
    OBJECTS,
    ACTIONS,
    EMOTIONS,
    PLACES,
    PROFESSIONS,
    CONCEPTS,
}

/** Concreteness of a word — concrete words are easier to encode (imagery), doc §4 Ej.3. */
enum class Concreteness {
    CONCRETE,
    ABSTRACT,
}

/** Lexical frequency band — low-frequency words are harder to retrieve, doc §4 Ej.3. */
enum class WordFrequency {
    HIGH,
    MEDIUM,
    LOW,
}

/**
 * An immutable, tagged word from the content pool. Carries the linguistic
 * properties the Phase-4 generators need to control similarity (S), interference
 * (I) and encoding depth (P): [category], [concreteness], [frequency] and
 * [lengthChars]. No render or difficulty logic here — this is pure content.
 */
data class WordItem(
    val id: WordId,
    val text: String,
    val category: WordCategory,
    val concreteness: Concreteness,
    val frequency: WordFrequency,
    val lengthChars: Int,
) {
    init {
        require(text.isNotBlank()) { "WordItem.text cannot be blank." }
        require(lengthChars in 1..MAX_WORD_LENGTH) {
            "WordItem.lengthChars must be in 1..$MAX_WORD_LENGTH (was $lengthChars)."
        }
    }

    companion object {
        const val MAX_WORD_LENGTH = 30
    }
}

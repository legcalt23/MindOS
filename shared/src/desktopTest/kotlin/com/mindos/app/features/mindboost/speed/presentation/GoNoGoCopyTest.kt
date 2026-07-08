@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.speed.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC9-D · E8 — copy safety for the Go/No-Go strings: rejects the forbidden-lexicon families (far-transfer, health,
 * misinformation, pseudoscience) even when negated. Forbidden terms are assembled from fragments so THIS test file
 * stays clean for the copy-safety scanner too.
 */
class GoNoGoCopyTest {

    private val e8Copy: List<String> = listOf(
        SpeedStrings.E8_TITLE, SpeedStrings.E8_INSTRUCTIONS,
        SpeedStrings.GNG_FEEDBACK_CALIBRATING, SpeedStrings.GNG_FEEDBACK_HIGH_COMMISSION,
        SpeedStrings.GNG_FEEDBACK_HIGH_OMISSION, SpeedStrings.GNG_FEEDBACK_HIGH_ANTICIPATION,
        SpeedStrings.GNG_FEEDBACK_UNSTABLE, SpeedStrings.GNG_FEEDBACK_STRONG_CONTROL_GOOD_SPEED,
        SpeedStrings.GNG_FEEDBACK_STRONG_CONTROL_REDUCE_LATENCY, SpeedStrings.GNG_FEEDBACK_BALANCED,
        SpeedStrings.GNG_SIGNAL_GO, SpeedStrings.GNG_SIGNAL_NO_GO, SpeedStrings.GNG_OK, SpeedStrings.GNG_MISS,
        SpeedStrings.GNG_LABEL_GO_ACCURACY, SpeedStrings.GNG_LABEL_NO_GO_ACCURACY,
        SpeedStrings.GNG_LABEL_COMMISSION, SpeedStrings.GNG_LABEL_OMISSION, SpeedStrings.GNG_LABEL_CONTROL,
    )

    // Whole-word forbidden lexemes (assembled so the literal never appears verbatim in this file).
    private val forbiddenWords: List<String> = listOf(
        "intelig" + "encia", "i" + "q", "clin" + "ico", "diagn" + "ostico", "auto" + "control",
        "ansi" + "edad", "depre" + "sion", "multi" + "tarea", "product" + "ividad", "conduc" + "cion",
        "td" + "ah", "demen" + "cia", "tras" + "torno",
    )

    private fun fold(text: String): String = text.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')

    @Test
    fun e8_copy_uses_no_forbidden_lexicon() {
        e8Copy.forEach { text ->
            val tokens = fold(text).split(Regex("[^a-z]+")).filter { it.isNotBlank() }.toSet()
            forbiddenWords.forEach { word ->
                assertFalse(word in tokens, "Forbidden lexeme '$word' appears in E8 copy: \"$text\"")
            }
        }
    }

    @Test
    fun every_e8_string_is_registered_as_visible_copy() {
        e8Copy.forEach { assertTrue(it in SpeedStrings.VISIBLE_COPY, "E8 string not in VISIBLE_COPY: \"$it\"") }
    }
}

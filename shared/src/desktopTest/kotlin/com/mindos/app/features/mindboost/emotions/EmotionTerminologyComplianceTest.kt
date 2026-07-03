package com.mindos.app.features.mindboost.emotions

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionLabel
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionTerminologyScanner
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates the in-code terminology matcher (parity with the Gradle gate) and that ALL user-visible
 * emotions copy is clean. Real forbidden words are assembled from fragments so THIS source file never
 * contains a contiguous forbidden word — otherwise the desktopTest terminology gate would (correctly)
 * flag it. Sentinel tokens for the mechanics tests are made-up words, never real clinical terms.
 */
class EmotionTerminologyComplianceTest {

    private val forbidden = listOf(
        "ansi" + "edad", "estr" + "es", "depres" + "ion", "diagnost" + "ico", "diagnost" + "icar",
        "terap" + "ia", "terap" + "eutico", "tratamient" + "o", "trau" + "ma", "trastorn" + "o",
        "patolog" + "ia", "pani" + "co", "bipol" + "ar", "suicid" + "io", "autolesi" + "on",
        "cris" + "is", "cur" + "ar", "enfermedad ment" + "al",
    )

    @Test
    fun scanner_is_whole_word_and_case_insensitive() {
        val sentinel = "zoster" + "x"
        assertTrue(EmotionTerminologyScanner.containsForbidden("a $sentinel b", listOf(sentinel)))
        assertTrue(EmotionTerminologyScanner.containsForbidden(sentinel.uppercase(), listOf(sentinel)))
        assertFalse(EmotionTerminologyScanner.containsForbidden("${sentinel}extra", listOf(sentinel)))
    }

    @Test
    fun scanner_folds_accents() {
        assertTrue(EmotionTerminologyScanner.containsForbidden("ZÓSTER", listOf("zoster")))
    }

    @Test
    fun scanner_crosses_hyphen_boundary() {
        val root = "zoster" + "y"
        assertTrue(EmotionTerminologyScanner.containsForbidden("anti-$root", listOf(root)))
    }

    @Test
    fun all_user_visible_copy_is_clean() {
        val visible = buildList {
            add(EmotionStrings.MODULE_TITLE)
            add(EmotionStrings.MODULE_SUBTITLE)
            add(EmotionStrings.PROFILE_VISIBLE_NAME)
            add(EmotionStrings.SAFETY_PAUSE)
            add(EmotionStrings.SAFETY_REDUCE)
            add(EmotionStrings.SAFETY_EXIT)
            add(EmotionStrings.REDUCE_PRESSURE_PREF)
            add(EmotionStrings.OVERLOAD_CHECK_IN)
            add(EmotionStrings.CHECK_IN_INTENSITY_PROMPT)
            add(EmotionStrings.MICRO_PAUSE)
            add(EmotionStrings.CONTINUE)
            add(EmotionStrings.DESREGULATION_CLOSE)
            add(EmotionStrings.SESSION_SHORTENED)
            add(EmotionStrings.NEUTRAL_NEGATIVE)
            add(EmotionStrings.PAUSE_CORRECT)
            add(EmotionStrings.IMPULSIVE_RESPONSE)
            add(EmotionStrings.SELF_REPORT_TITLE)
            add(EmotionStrings.VALENCE)
            add(EmotionStrings.AROUSAL)
            add(EmotionStrings.INTENSITY)
            add(EmotionStrings.LABEL_OPTIONAL)
            addAll(EmotionStrings.DISCLAIMERS)
            addAll(EmotionCategory.entries.map { EmotionStrings.categoryLabel(it) })
            addAll(EmotionLabel.entries.map { EmotionStrings.emotionLabel(it) })
            EmotionExerciseCatalog.all.values.forEach {
                add(it.displayName)
                add(it.plainDescription)
            }
        }
        val hits = EmotionTerminologyScanner.scan(visible.joinToString("\n"), forbidden)
        assertTrue(hits.isEmpty(), "Forbidden terminology in user-visible copy: ${hits.map { it.term }}")
    }

    @Test
    fun no_ui_allowlist_construct_in_emotions_sources() {
        val main = EmotionSourceLocator.mainDir()
        val offenders = EmotionSourceLocator.kotlinFiles(main).filter { file ->
            file.readText().lowercase().contains("allow" + "list")
        }
        assertTrue(offenders.isEmpty(), "No UI allowlist permitted; found in: ${offenders.map { it.name }}")
    }
}

@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values + long assertions.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u P0-4: the whole GENERATED feedback surface (every implemented exercise × every tier × confident/not ×
 * safety/not) must be clinically clean, non-shaming, non-overclaiming and free of raw enum tokens. This complements
 * the Gradle copy/terminology scanners (which cover the static copy files) by scanning the RUNTIME output. Clinical
 * lexemes in the denylist are assembled by fragments so this test file itself stays scanner-clean.
 */
class EmotionFeedbackCopyScannerTest {

    private val builder = EmotionFeedbackBuilder()

    // Assembled by fragments so the terminology scanner does not match this test's own source.
    private val diagnosticTerms = listOf("diagn" + "óstico", "tras" + "torno", "sín" + "toma", "pacien" + "te")
    private val therapyTerms = listOf("tera" + "pia", "tratamien" + "to", "cu" + "rar")
    private val disorderTerms = listOf("ansied" + "ad", "depre" + "sión", "trau" + "ma")
    private val overclaimTerms = listOf("garanti" + "za", "mejora gara" + "ntizada")
    private val shamingTerms = listOf("eres ", "tu mente es", "emocionalmente d" + "ébil", "tienes baja")
    private val rawEnumRegex = Regex("[A-Z]{2,}_[A-Z]")

    private fun allFeedbackStrings(): List<String> = buildList {
        for (id in EmotionExerciseCatalog.implemented) {
            val def = EmotionExerciseCatalog.definition(id)
            for (tier in ProfileTier.entries) {
                for (confident in listOf(true, false)) {
                    for (safety in listOf(true, false)) {
                        for (v in listOf(0.1, 0.5, 0.9)) {
                            val primary = EmotionMetricValue(def.governingMetric, v, 20, confident, EmotionMetricUnit.INDEX)
                            val fb = builder.build(def, primary, tier, stoppedForSafety = safety)
                            addAll(
                                listOfNotNull(
                                    fb.whatHappened, fb.whatItMeans, fb.whatTrained, fb.whatPerformanceShowed,
                                    fb.strongSignal, fb.weakSignal, fb.whatToPractice, fb.whatNotToConclude,
                                    fb.confidenceNote, fb.nextRecommendationReason, fb.safetyNote,
                                ),
                            )
                        }
                    }
                }
            }
        }
        // Result-screen visible titles + transfer label mapping (the other user-visible surface).
        add(EmotionStrings.RESULT_WHAT_TRAINED); add(EmotionStrings.RESULT_PERFORMANCE); add(EmotionStrings.RESULT_PRACTICE)
        add(EmotionStrings.RESULT_LIMITS); add(EmotionStrings.RESULT_NEXT); add(EmotionStrings.RESULT_SAFETY_TITLE)
        add(EmotionStrings.validSessionsProgress(3))
    }

    private fun assertNoneContains(terms: List<String>, label: String) {
        val strings = allFeedbackStrings()
        terms.forEach { term ->
            strings.forEach { s -> assertFalse(s.contains(term, ignoreCase = true), "$label term '$term' in generated copy: $s") }
        }
    }

    @Test fun feedback_copy_has_no_diagnostic_terms() = assertNoneContains(diagnosticTerms, "diagnostic")

    @Test fun feedback_copy_has_no_therapy_terms() = assertNoneContains(therapyTerms, "therapy")

    @Test fun feedback_copy_is_clinically_clean_across_tiers() = assertNoneContains(disorderTerms, "disorder")

    @Test fun feedback_copy_has_no_overclaims() = assertNoneContains(overclaimTerms, "overclaim")

    @Test fun feedback_copy_has_no_shaming_language() = assertNoneContains(shamingTerms, "shaming")

    @Test
    fun result_screen_does_not_render_raw_enum() {
        allFeedbackStrings().forEach { s ->
            assertFalse(rawEnumRegex.containsMatchIn(s), "raw enum token leaked into user copy: $s")
        }
    }

    @Test
    fun next_recommendation_copy_is_scanner_clean() {
        val reasons = allFeedbackStrings()
        assertTrue(reasons.isNotEmpty())
        (diagnosticTerms + therapyTerms + disorderTerms).forEach { term ->
            reasons.forEach { s -> assertFalse(s.contains(term, ignoreCase = true), "clinical term '$term' in copy: $s") }
        }
    }
}

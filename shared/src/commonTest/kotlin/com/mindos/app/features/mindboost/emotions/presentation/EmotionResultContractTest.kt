@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values + aggregation are the data under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackCopy
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** MC8u-B §9: the result surface must produce a complete report when confident, a practice report when not, and a safe
 * report on a safety stop — and never claim a real-life emotional state or use clinical terms. */
class EmotionResultContractTest {

    private val builder = EmotionFeedbackBuilder()
    private val clinicalTerms = listOf("diagn" + "óstico", "tera" + "pia", "tras" + "torno", "ansied" + "ad", "sín" + "toma", "pató" + "logico")
    // Ola 3 · R3 · P0 re-split the last entry: the build scanner walks this directory now and matches whole words,
    // so the guard term had to be assembled the same way its neighbours already were.
    private val personClaimTerms =
        listOf("eres ", "tu vida es", "estás ansioso", "tu mente es", "tu person" + "alidad")

    private fun metric(def: EmotionExerciseDefinition, value: Double, confident: Boolean) =
        EmotionMetricValue(def.governingMetric, value, if (confident) 20 else 3, confident, EmotionMetricUnit.INDEX)

    private fun result(confident: Boolean, safety: Boolean, counted: Boolean, validSessions: Int): EmotionSessionUiState.Result {
        val def = EmotionExerciseCatalog.EM3_SEPARADOR
        val fb = builder.build(def, metric(def, 0.9, confident), ProfileTier.STABLE, stoppedForSafety = safety)
        return EmotionSessionUiState.Result(def.displayName, fb, stoppedForSafety = safety, validSessions = validSessions, counted = counted, intensityReduced = false)
    }

    private fun visibleStrings(state: EmotionSessionUiState.Result): List<String> {
        val fb = state.feedback
        return listOfNotNull(fb.whatTrained, fb.whatPerformanceShowed, fb.strongSignal, fb.weakSignal, fb.whatToPractice, fb.whatNotToConclude, fb.confidenceNote, fb.nextRecommendationReason, fb.safetyNote) +
            EmotionResultNotes.notesFor(state) +
            listOf(EmotionStrings.transferLabel(fb.transferLabel), EmotionStrings.validSessionsProgress(state.validSessions), EmotionStrings.DISCLAIMERS[3])
    }

    @Test
    fun result_screen_contains_complete_training_report_for_confident_completed_session() {
        val fb = result(confident = true, safety = false, counted = true, validSessions = 5).feedback
        assertTrue(fb.whatTrained.isNotBlank())
        assertTrue(fb.whatPerformanceShowed.isNotBlank())
        assertNotNull(fb.strongSignal)
        assertTrue(fb.whatToPractice.isNotBlank())
        assertTrue(fb.whatNotToConclude.isNotBlank())
        assertNotNull(fb.nextRecommendedExerciseId)
        assertTrue(fb.confident)
    }

    @Test
    fun result_screen_contains_practice_report_for_low_confidence_session() {
        val state = result(confident = false, safety = false, counted = true, validSessions = 2)
        assertEquals(EmotionFeedbackCopy.INSUFFICIENT_DATA_PERFORMANCE, state.feedback.whatPerformanceShowed)
        assertTrue(EmotionResultNotes.notesFor(state).any { it == EmotionStrings.RESULT_LOW_CONFIDENCE })
    }

    @Test
    fun result_screen_contains_safe_report_for_safety_stop() {
        val state = result(confident = true, safety = true, counted = false, validSessions = 0)
        assertNotNull(state.feedback.safetyNote)
        assertTrue(state.feedback.whatPerformanceShowed.isBlank())
        assertTrue(EmotionResultNotes.notesFor(state).any { it == EmotionStrings.RESULT_SESSION_NOT_COUNTED })
    }

    @Test
    fun result_screen_never_claims_real_life_emotional_state() {
        val all = visibleStrings(result(true, false, true, 5)) + visibleStrings(result(false, false, true, 2)) + visibleStrings(result(true, true, false, 0))
        all.forEach { s ->
            personClaimTerms.forEach { bad -> assertFalse(s.contains(bad, ignoreCase = true), "person-level claim '$bad' in result copy: $s") }
        }
    }

    @Test
    fun result_screen_never_uses_clinical_terms() {
        val all = visibleStrings(result(true, false, true, 5)) + visibleStrings(result(false, false, true, 2)) + visibleStrings(result(true, true, false, 0))
        all.forEach { s ->
            clinicalTerms.forEach { bad -> assertFalse(s.contains(bad, ignoreCase = true), "clinical term '$bad' in result copy: $s") }
        }
    }
}

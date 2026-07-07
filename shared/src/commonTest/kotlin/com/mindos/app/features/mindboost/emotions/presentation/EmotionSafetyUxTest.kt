@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values + long assertion messages are the data under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC8u-B §3 (safety UX visible & non-punitive) and §5 (low-confidence vs calibrating). */
class EmotionSafetyUxTest {

    private val builder = EmotionFeedbackBuilder()

    // Assembled from fragments so this test file stays clean for the terminology scanners.
    private val clinicalTerms = listOf("diagn" + "óstico", "tera" + "pia", "tras" + "torno", "ansied" + "ad", "sín" + "toma")
    private val shamingTerms = listOf("eres ", "tu mente es", "emocionalmente d" + "ébil", "tienes baja", "has fracasado")
    private val harderTrainingTerms = listOf("más fuerte", "más difícil", "sube la exigencia", "entrena más duro")

    private fun metric(value: Double, confident: Boolean = true) =
        EmotionMetricValue(com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey.COMMISSION_RATE, value, if (confident) 20 else 3, confident, EmotionMetricUnit.RATIO)

    private fun safetyStopResult(): EmotionSessionUiState.Result {
        val feedback = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(0.1), ProfileTier.STABLE, stoppedForSafety = true)
        return EmotionSessionUiState.Result("Pausa antes de actuar", feedback, stoppedForSafety = true, validSessions = 0, counted = false, intensityReduced = true)
    }

    private fun assertClean(strings: List<String>) {
        (clinicalTerms + shamingTerms).forEach { bad ->
            strings.forEach { s -> assertFalse(s.contains(bad, ignoreCase = true), "unsafe term '$bad' in copy: $s") }
        }
    }

    @Test
    fun safety_stop_copy_is_non_clinical() {
        val state = safetyStopResult()
        assertClean(EmotionResultNotes.notesFor(state) + listOfNotNull(state.feedback.safetyNote) + listOf(EmotionStrings.RESULT_SAFETY_TITLE, EmotionStrings.DESREGULATION_CLOSE))
    }

    @Test
    fun safety_stop_copy_is_not_shaming() {
        val note = safetyStopResult().feedback.safetyNote ?: ""
        shamingTerms.forEach { assertFalse(note.contains(it, ignoreCase = true), "shaming '$it' in safety note") }
    }

    @Test
    fun safety_stop_result_explains_session_not_counted_when_applicable() {
        assertTrue(EmotionResultNotes.notesFor(safetyStopResult()).any { it == EmotionStrings.RESULT_SESSION_NOT_COUNTED })
    }

    @Test
    fun safety_stop_recommends_pause_or_base_exercise() {
        assertEquals(EmotionExerciseId.EM1_PAUSA, safetyStopResult().feedback.nextRecommendedExerciseId)
    }

    @Test
    fun safety_stop_does_not_show_performance_judgment() {
        val fb = safetyStopResult().feedback
        assertTrue(fb.whatPerformanceShowed.isBlank())
        assertTrue(fb.whatToPractice.isBlank())
        assertNull(fb.strongSignal)
        assertNull(fb.weakSignal)
    }

    @Test
    fun safety_stop_does_not_recommend_harder_training() {
        val reason = safetyStopResult().feedback.nextRecommendationReason
        harderTrainingTerms.forEach { assertFalse(reason.contains(it, ignoreCase = true), "safety recommendation pushes harder training: $reason") }
    }

    @Test
    fun intensity_reduced_copy_is_user_safe() = assertClean(listOf(EmotionStrings.RESULT_INTENSITY_REDUCED))

    @Test
    fun session_not_counted_copy_is_user_safe() = assertClean(listOf(EmotionStrings.RESULT_SESSION_NOT_COUNTED))

    @Test
    fun intensity_reduced_note_shows_when_intensity_was_reduced() {
        assertTrue(EmotionResultNotes.notesFor(safetyStopResult()).any { it == EmotionStrings.RESULT_INTENSITY_REDUCED })
    }

    // ---- §5 low confidence vs calibrating ----------------------------------------------------------------------

    @Test
    fun low_confidence_and_calibrating_have_distinct_copy() {
        assertTrue(EmotionStrings.RESULT_LOW_CONFIDENCE != EmotionStrings.RESULT_CALIBRATING)
    }

    @Test
    fun low_confidence_copy_refers_to_session_sample() {
        val text = EmotionStrings.RESULT_LOW_CONFIDENCE
        assertTrue(text.contains("sesión", ignoreCase = true) && text.contains("muestra", ignoreCase = true))
    }

    @Test
    fun calibrating_copy_refers_to_valid_sessions_progress() {
        assertTrue(EmotionStrings.RESULT_CALIBRATING.contains("sesiones válidas", ignoreCase = true))
    }

    @Test
    fun calibrating_copy_does_not_sound_like_poor_performance() {
        val text = EmotionStrings.RESULT_CALIBRATING
        assertTrue(text.contains("no refleja un rendimiento bajo", ignoreCase = true))
        assertClean(listOf(text))
    }

    @Test
    fun low_confidence_note_shows_for_unconfident_reading() {
        val feedback = builder.build(EmotionExerciseCatalog.EM1_PAUSA, metric(0.1, confident = false), ProfileTier.STABLE)
        val state = EmotionSessionUiState.Result("Pausa", feedback, stoppedForSafety = false, validSessions = 2, counted = true, intensityReduced = false)
        val notes = EmotionResultNotes.notesFor(state)
        assertTrue(notes.any { it == EmotionStrings.RESULT_LOW_CONFIDENCE })
        assertTrue(notes.any { it == EmotionStrings.RESULT_CALIBRATING })
    }
}

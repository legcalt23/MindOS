@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values are the data under test.

package com.mindos.app.features.mindboost.emotions.coach

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u-B+ §J: the AI-Coach summary is training-pattern only, privacy-safe, and copy-guarded (no clinical/trait/IQ/
 * guaranteed/real-life claims). */
class EmotionCoachSummaryTest {

    // Assembled from fragments so this source stays clean for the terminology scanners. MC8u-C §10 extends the list.
    private val forbidden = listOf(
        "diagn" + "óstico", "tera" + "pia", "tras" + "torno", "ansied" + "ad", "depres" + "ión",
        "salud ment" + "al", "co" + "eficiente intelectual", " i" + "q ", "mejora garanti" + "zada",
        "eres ", "tu personalidad", "tu vida", "td" + "ah", "trau" + "ma",
        "regulación emocional clí" + "nica", "tu estado emocional r" + "eal",
    )

    private fun valid(id: EmotionExerciseId, startedAt: Long, sessionId: String) = EmotionSessionSummaryDto(
        exerciseId = id.name,
        startedAtMillis = startedAt,
        durationMs = 130_000,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = sessionId,
        endReason = EmotionTerminalSemantics.COMPLETED,
        governingConfident = true,
        scoringVersion = EmotionScoringVersions.SCORING,
        exerciseVersion = EmotionScoringVersions.EXERCISE,
    )

    private fun allStrings(s: EmotionCoachSummary): List<String> =
        listOfNotNull(s.calibrationState, s.confidenceLabel, s.mostPracticed, s.interpretationLimits, s.disclaimer) + s.practicedCapacities

    @Test
    fun empty_state_is_honest() {
        val s = EmotionCoachSummaryBuilder.build(emptyList(), null)
        assertTrue(s.practicedCapacities.isEmpty())
        assertFalse(s.calibrated)
        assertTrue(s.validSessions == 0)
    }

    @Test
    fun reports_training_patterns_and_calibration_state() {
        val summaries = listOf(
            valid(EmotionExerciseId.EM1_PAUSA, 1, "sess_1"),
            valid(EmotionExerciseId.EM1_PAUSA, 2, "sess_2"),
            valid(EmotionExerciseId.EM3_SEPARADOR, 3, "sess_3"),
        )
        val s = EmotionCoachSummaryBuilder.build(summaries, null)
        assertTrue(s.practicedCapacities.isNotEmpty())
        assertTrue(s.mostPracticed != null)
        assertFalse(s.calibrated, "3 valid sessions is not yet calibrated")
        assertTrue(s.validSessions == 3)
    }

    @Test
    fun coach_summary_has_no_clinical_trait_or_overclaim_copy() {
        val summaries = listOf(valid(EmotionExerciseId.EM1_PAUSA, 1, "sess_1"), valid(EmotionExerciseId.EM3_SEPARADOR, 2, "sess_2"))
        val strings = allStrings(EmotionCoachSummaryBuilder.build(summaries, null))
        strings.forEach { str ->
            forbidden.forEach { bad -> assertFalse(str.contains(bad, ignoreCase = true), "unsafe coach copy '$bad' in: $str") }
        }
    }

    @Test
    fun coach_summary_states_limits_and_disclaimer() {
        val s = EmotionCoachSummaryBuilder.build(listOf(valid(EmotionExerciseId.EM1_PAUSA, 1, "sess_1")), null)
        assertTrue(s.interpretationLimits.contains("patrones de entrenamiento", ignoreCase = true))
        assertTrue(s.disclaimer.contains("clínica", ignoreCase = true))
    }
}

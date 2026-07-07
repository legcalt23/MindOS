@file:Suppress("MagicNumber", "MaxLineLength", "LongParameterList") // Fixture builder mirrors the DTO's fields.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.SafetyEventDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC8u-B §4: the history/progress model is honest, session-scoped and never a person-level claim. */
class EmotionHistoryCardBuilderTest {

    private val clinicalOrShaming = listOf("diagn" + "óstico", "tera" + "pia", "tras" + "torno", "ansied" + "ad", "eres ", "tu mente es")

    private fun summary(
        exerciseId: EmotionExerciseId,
        startedAt: Long,
        durationMs: Long,
        endReason: String,
        completed: Boolean,
        abandoned: Boolean,
        sessionId: String,
        safetyEvents: List<SafetyEventDto> = emptyList(),
    ) = EmotionSessionSummaryDto(
        exerciseId = exerciseId.name,
        startedAtMillis = startedAt,
        durationMs = durationMs,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = completed,
        abandoned = abandoned,
        sessionId = sessionId,
        endReason = endReason,
        safetyEvents = safetyEvents,
    )

    private fun validSession(exerciseId: EmotionExerciseId, startedAt: Long, sessionId: String) =
        summary(exerciseId, startedAt, 130_000, EmotionTerminalSemantics.COMPLETED, completed = true, abandoned = false, sessionId = sessionId)

    private fun shortSession(exerciseId: EmotionExerciseId, startedAt: Long, sessionId: String) =
        summary(exerciseId, startedAt, 60_000, EmotionTerminalSemantics.COMPLETED, completed = true, abandoned = false, sessionId = sessionId)

    private fun safetyStopSession(exerciseId: EmotionExerciseId, startedAt: Long, sessionId: String) =
        summary(exerciseId, startedAt, 90_000, EmotionTerminalSemantics.SAFETY_STOP, completed = false, abandoned = false, sessionId = sessionId, safetyEvents = listOf(SafetyEventDto("SAFETY_STOP", 1000)))

    private fun modelStrings(model: EmotionHistoryUiModel): List<String> = listOfNotNull(
        model.lastExerciseName, model.lastSessionLabel, model.capacityTrained, model.stateLabel,
        model.validSessionsProgress, model.nextSuggestion, model.safetyNote,
    )

    @Test
    fun history_card_handles_empty_state() {
        val model = EmotionHistoryCardBuilder.build(emptyList(), null)
        assertTrue(model.isEmpty)
        assertNull(model.lastExerciseName)
    }

    @Test
    fun history_card_shows_last_exercise() {
        val model = EmotionHistoryCardBuilder.build(listOf(validSession(EmotionExerciseId.EM1_PAUSA, 1000, "sess_a1")), null)
        assertEquals(EmotionExerciseCatalog.EM1_PAUSA.displayName, model.lastExerciseName)
    }

    @Test
    fun history_card_shows_valid_sessions_progress_toward_5() {
        val model = EmotionHistoryCardBuilder.build(listOf(validSession(EmotionExerciseId.EM1_PAUSA, 1000, "sess_a1")), null)
        assertTrue(model.validSessionsProgress.contains("/${EmotionStrings.VALID_SESSION_TARGET}"))
    }

    @Test
    fun history_card_distinguishes_valid_and_practice_session() {
        val valid = EmotionHistoryCardBuilder.build(listOf(validSession(EmotionExerciseId.EM1_PAUSA, 1000, "sess_v1")), null)
        assertTrue(valid.lastSessionWasValid)
        assertEquals(EmotionStrings.HISTORY_VALID_LABEL, valid.lastSessionLabel)

        val practice = EmotionHistoryCardBuilder.build(listOf(shortSession(EmotionExerciseId.EM1_PAUSA, 1000, "sess_s1")), null)
        assertFalse(practice.lastSessionWasValid)
        assertEquals(EmotionStrings.HISTORY_PRACTICE_LABEL, practice.lastSessionLabel)
    }

    @Test
    fun history_card_does_not_make_person_level_claims() {
        val model = EmotionHistoryCardBuilder.build(listOf(validSession(EmotionExerciseId.EM3_SEPARADOR, 1000, "sess_c1")), null)
        modelStrings(model).forEach { s ->
            clinicalOrShaming.forEach { bad -> assertFalse(s.contains(bad, ignoreCase = true), "person-level/clinical '$bad' in history: $s") }
        }
    }

    @Test
    fun history_card_safety_note_is_non_clinical() {
        val model = EmotionHistoryCardBuilder.build(listOf(safetyStopSession(EmotionExerciseId.EM1_PAUSA, 1000, "sess_x1")), null)
        val note = model.safetyNote
        assertTrue(note != null, "a safety-stopped last session should carry a protective note")
        clinicalOrShaming.forEach { assertFalse(note!!.contains(it, ignoreCase = true)) }
    }

    @Test
    fun history_card_does_not_show_official_rank_before_5_valid_sessions() {
        val three = listOf(
            validSession(EmotionExerciseId.EM1_PAUSA, 1000, "sess_1"),
            validSession(EmotionExerciseId.EM1_PAUSA, 2000, "sess_2"),
            validSession(EmotionExerciseId.EM1_PAUSA, 3000, "sess_3"),
        )
        assertFalse(EmotionHistoryCardBuilder.build(three, null).hasOfficialRank)
        val five = three + validSession(EmotionExerciseId.EM1_PAUSA, 4000, "sess_4") + validSession(EmotionExerciseId.EM1_PAUSA, 5000, "sess_5")
        assertTrue(EmotionHistoryCardBuilder.build(five, null).hasOfficialRank)
    }
}

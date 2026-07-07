@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.coach

import com.mindos.app.features.mindboost.coach.domain.CoachCalibrationState
import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC8u-F — Emociones feeds the coach as a coarse, PII-free contribution: patterns only, no person-level trait, with the interpretation limits attached. */
class EmotionCoachContributionSourceTest {

    private fun valid(sessionId: String, started: Long) = EmotionSessionSummaryDto(
        exerciseId = "EM3_SEPARADOR",
        startedAtMillis = started,
        durationMs = 130_000,
        difficultySummary = DifficultySummaryDto("CATEGORY_AMBIGUITY", 0.5, 2),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = sessionId,
        endReason = "COMPLETED",
        governingConfident = true,
        scoringVersion = EmotionScoringVersions.SCORING,
    )

    @Test
    fun returns_null_until_there_is_data() = runTest {
        assertNull(EmotionCoachContributionSource(LocalEmotionRepository(InMemorySecureKeyValueStore())).contribute("u"))
    }

    @Test
    fun maps_to_a_coarse_contribution_with_no_person_level_trait() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repo.saveSessionSummary("u", valid("s1", 1_000))
        repo.saveSessionSummary("u", valid("s2", 2_000))
        val c = assertNotNull(EmotionCoachContributionSource(repo).contribute("u"))
        assertEquals("EMOCIONES", c.blockId)
        assertTrue(c.strengths.isEmpty() && c.weaknesses.isEmpty(), "charter: no you-are-good-or-bad-at-X")
        assertTrue(c.patterns.isNotEmpty(), "only what was practiced becomes a pattern")
        assertTrue(c.cautions.isNotEmpty(), "the interpretation limits + disclaimer travel with the contribution")
        assertEquals(CoachCalibrationState.PROVISIONAL, c.calibrationState)
        assertEquals(CoachConfidenceLevel.LOW, c.confidence)
    }
}

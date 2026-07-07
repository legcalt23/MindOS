@file:Suppress("MagicNumber", "MaxLineLength", "LongParameterList") // Fixture builder mirrors the DTO's fields.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MC8u-B+ §B/§E/§K — calibration integrity: a completed, long-enough session counts toward the 5-valid calibration
 * ONLY when it is trustworthy (governing metric confident, no blocking anomaly) under the current ruleset. Legacy rows
 * (pre-gate) keep counting so earned progress is never lost. Boundary cases per the CEO audit.
 */
class EmotionCalibrationIntegrityTest {

    private val now = 5_000_000L

    private fun summary(
        sessionId: String,
        startedAt: Long,
        durationMs: Long = 130_000,
        endReason: String = EmotionTerminalSemantics.COMPLETED,
        completed: Boolean = true,
        abandoned: Boolean = false,
        governingConfident: Boolean = true,
        anomalyFlags: List<String> = emptyList(),
        scoringVersion: Int = EmotionScoringVersions.SCORING,
    ) = EmotionSessionSummaryDto(
        exerciseId = "EM1_PAUSA",
        startedAtMillis = startedAt,
        durationMs = durationMs,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = completed,
        abandoned = abandoned,
        sessionId = sessionId,
        endReason = endReason,
        governingConfident = governingConfident,
        anomalyFlags = anomalyFlags,
        scoringVersion = scoringVersion,
        exerciseVersion = EmotionScoringVersions.EXERCISE,
    )

    /** A legacy row: written before the quality gate (scoringVersion 0), lacking quality fields (defaults). */
    private fun legacySummary(sessionId: String, startedAt: Long) = EmotionSessionSummaryDto(
        exerciseId = "EM1_PAUSA",
        startedAtMillis = startedAt,
        durationMs = 130_000,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = sessionId,
        endReason = EmotionTerminalSemantics.COMPLETED,
        // governingConfident/anomalyFlags/scoringVersion left at DTO defaults (true / [] / LEGACY 0)
    )

    @Test
    fun four_valid_plus_one_low_confidence_calibrate_only_four() {
        val valids = (1..4).map { summary("sess_v$it", it.toLong()) }
        val lowConf = summary("sess_low", 5, governingConfident = false)
        val aggregate = EmotionAggregateFactory.fromSummaries(valids + lowConf, now)
        assertEquals(4, aggregate?.sessionsCompleted)
        assertTrue(aggregate?.tier != ProfileTier.STABLE.name, "4 valid must not reach the official STABLE tier")
    }

    @Test
    fun five_valid_sessions_reach_stable() {
        val valids = (1..5).map { summary("sess_v$it", it.toLong()) }
        val aggregate = EmotionAggregateFactory.fromSummaries(valids, now)
        assertEquals(5, aggregate?.sessionsCompleted)
        assertEquals(ProfileTier.STABLE.name, aggregate?.tier)
    }

    @Test
    fun abandoned_session_never_counts() {
        val abandoned = summary("sess_ab", 1, endReason = EmotionTerminalSemantics.ABANDONED, completed = false, abandoned = true)
        assertNull(EmotionAggregateFactory.fromSummaries(listOf(abandoned), now))
    }

    @Test
    fun anomalous_session_does_not_count() {
        val anomalous = summary("sess_an", 1, anomalyFlags = listOf(EmotionSessionQualityAnomaly.IMPOSSIBLE))
        assertNull(EmotionAggregateFactory.fromSummaries(listOf(anomalous), now))
    }

    @Test
    fun legacy_session_without_quality_fields_still_counts() {
        val legacy = (1..5).map { legacySummary("sess_l$it", it.toLong()) }
        assertEquals(5, EmotionAggregateFactory.fromSummaries(legacy, now)?.sessionsCompleted, "legacy rows keep counting — no progress loss")
    }

    @Test
    fun quality_and_version_fields_survive_a_persistence_round_trip() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val s = summary("sess_rt", 1, governingConfident = false, anomalyFlags = listOf(EmotionSessionQualityAnomaly.IMPOSSIBLE))
        repo.saveSessionSummary("u1", s)
        val read = repo.recentSummaries("u1").single()
        assertEquals(false, read.governingConfident)
        assertEquals(listOf(EmotionSessionQualityAnomaly.IMPOSSIBLE), read.anomalyFlags)
        assertEquals(EmotionScoringVersions.SCORING, read.scoringVersion)
        assertEquals(EmotionScoringVersions.EXERCISE, read.exerciseVersion)
    }
}

/** Local mirror of the evaluator's blocking-anomaly code, so this data-layer test needs no domain import. */
private object EmotionSessionQualityAnomaly {
    const val IMPOSSIBLE = "impossible_timing"
}

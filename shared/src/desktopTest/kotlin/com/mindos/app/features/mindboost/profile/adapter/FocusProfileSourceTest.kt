package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScore
import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.MetricTrend
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendAnalyzer
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendDirection
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.profile.data.adapter.FocusProfileSource
import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.model.SessionEvidence
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeHistory(private val sessions: List<SessionResult>) : SessionHistoryRepository {
    override suspend fun append(result: SessionResult) = Unit
    override suspend fun recent(userId: String, limit: Int): List<SessionResult> = sessions.take(limit)
}

private class FakeProfiles(private val profile: UserCognitiveProfile?) : FocusProfileRepository {
    override suspend fun load(userId: String): UserCognitiveProfile? = profile
    override suspend fun save(profile: UserCognitiveProfile) = Unit
}

private class FakeCalculator(private val score: FocusScore) : FocusScoreCalculator {
    override fun compute(metrics: MetricSet): FocusScore = score
}

private class FakeAnalyzer(private val trend: MetricTrend) : TrendAnalyzer {
    override fun analyze(metric: MetricKey, sessions: List<SessionResult>): MetricTrend = trend
}

/** Non-empty metrics = "the session produced measurable evidence" (the Fase 1.6 eligibility signal for Focus). */
private fun measurableMetrics(): MetricSet = mapOf(
    MetricKey.SUSTAINED_FOCUS_INDEX to MetricValue(MetricKey.SUSTAINED_FOCUS_INDEX, 70.0, 60, true, MetricUnit.INDEX),
)

private fun sessionResult(endedAtMs: Long): SessionResult = SessionResult(
    sessionId = SessionId("s1"),
    mode = Mode.TRAINING,
    exerciseFocus = ExerciseId.E1_VIGIA,
    blocks = emptyList(),
    startedAtMs = 0L,
    endedAtMs = endedAtMs,
    sessionMetrics = measurableMetrics(), // eligible: measurable evidence present (Fase 1.6)
)

/** A valid (>=120 s, evidence-bearing) session with a distinct id on the given day — DC-3 cross-session (Fase 1.8/1.6). */
private fun validSession(id: String, dayIndex: Long) = SessionResult(
    sessionId = SessionId(id),
    mode = Mode.TRAINING,
    exerciseFocus = ExerciseId.E1_VIGIA,
    blocks = emptyList(),
    startedAtMs = dayIndex * 86_400_000L,
    endedAtMs = dayIndex * 86_400_000L + 200_000L,
    sessionMetrics = measurableMetrics(),
)

private fun focusScore(value: Double?, confident: Boolean) = FocusScore(value, null, null, confident, "x")

private fun metricTrend(direction: TrendDirection, confident: Boolean) =
    MetricTrend(MetricKey.SUSTAINED_FOCUS_INDEX, direction, slope = 0.0, confident = confident, caption = "x")

class FocusProfileSourceTest {

    @Test
    fun noSessions_returnsNull() = runTest {
        val source = FocusProfileSource(
            FakeHistory(emptyList()),
            FakeProfiles(null),
            FakeCalculator(focusScore(50.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.STABLE, true)),
        )
        assertNull(source.contribution("u1"))
    }

    @Test
    fun confidentScore_mapsValueOver100_withRealConfidenceAndSufficiency() = runTest {
        val profile = UserCognitiveProfile(userId = "u1", archetypeConfidence = 0.8, totalSessions = 4, updatedAtMs = 500L)
        val source = FocusProfileSource(
            FakeHistory(listOf(sessionResult(200_000L))),
            FakeProfiles(profile),
            FakeCalculator(focusScore(60.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.IMPROVING, true)),
        )
        val contribution = source.contribution("u1")!!
        assertEquals(CognitiveModuleId.ENFOQUE, contribution.moduleId)
        assertEquals(0.6, contribution.normalizedScore!!, 1e-9)
        assertEquals(0.5, contribution.dataSufficiency, 1e-9) // 4 / 8
        assertEquals(0.8, contribution.confidence, 1e-9)
        assertEquals(ProfileTrend.IMPROVING, contribution.trend)
        assertTrue(contribution.preliminary)
        assertEquals(500L, contribution.updatedAtMs)
    }

    @Test
    fun noConfidentComponent_scoreIsNull_andTrendUnknown_andConfidenceDownWeighted() = runTest {
        val profile = UserCognitiveProfile(userId = "u1", archetypeConfidence = 0.3, totalSessions = 2)
        val source = FocusProfileSource(
            FakeHistory(listOf(sessionResult(200_000L))),
            FakeProfiles(profile),
            FakeCalculator(focusScore(null, false)),
            FakeAnalyzer(metricTrend(TrendDirection.INSUFFICIENT, false)),
        )
        val contribution = source.contribution("u1")!!
        assertNull(contribution.normalizedScore, "no confident component -> CALIBRATING, never a silent 0")
        assertEquals(ProfileTrend.UNKNOWN, contribution.trend)
        assertEquals(0.15, contribution.confidence, 1e-9) // 0.3 * 0.5 (score not confident)
    }

    @Test
    fun contributionSurfacesAttentionThroughTheAggregator_notSilentlyDropped() = runTest {
        // EX1-class regression guard: assert the contribution clears the aggregator gates
        // (dataSufficiency≥0.5, confidence≥0.4) and SURFACES ATTENTION (EMERGING/preliminary), not dropped.
        val profile = UserCognitiveProfile(userId = "u1", archetypeConfidence = 0.6, totalSessions = 4, updatedAtMs = 500L)
        val contribution = FocusProfileSource(
            FakeHistory(listOf(validSession("a", 0), validSession("b", 1))),
            FakeProfiles(profile),
            FakeCalculator(focusScore(65.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.IMPROVING, true)),
        ).contribution("u1")!!
        // Focus emits REAL cross-session evidence: 2 valid distinct sessions across 2 days (DC-3 → EMERGING allowed).
        assertEquals(SessionEvidence(validSessions = 2, distinctDays = 2), contribution.sessionEvidence)
        val aggregated = MindBoostProfileAggregator().aggregate("u1", listOf(contribution), nowMs = 1L)
        val attention = aggregated.dimensions.getValue(CognitiveDimension.ATTENTION)
        assertEquals(DimensionStatus.EMERGING, attention.status)
        assertEquals(0.65, assertNotNull(attention.value), 1e-9)
        assertTrue(attention.preliminary)
        assertEquals(listOf(CognitiveModuleId.ENFOQUE), attention.contributingModules)
    }

    @Test
    fun insufficientSessions_keepAttentionCalibrating_evenWithANonNullScore() = runTest {
        // Pins the EX1 locus for Focus: a confident 0.6 score with only 2 sessions → dataSufficiency 0.25
        // (< 0.5), so the aggregator MUST drop it — ATTENTION stays CALIBRATING with a null value.
        val profile = UserCognitiveProfile(userId = "u1", archetypeConfidence = 0.8, totalSessions = 2)
        val contribution = FocusProfileSource(
            FakeHistory(listOf(sessionResult(200_000L))),
            FakeProfiles(profile),
            FakeCalculator(focusScore(60.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.STABLE, true)),
        ).contribution("u1")!!
        assertNotNull(contribution.normalizedScore, "the isolated contribution DOES carry a value")
        val aggregated = MindBoostProfileAggregator().aggregate("u1", listOf(contribution), nowMs = 1L)
        val attention = aggregated.dimensions.getValue(CognitiveDimension.ATTENTION)
        assertEquals(DimensionStatus.CALIBRATING, attention.status)
        assertNull(attention.value)
    }

    // --- Fase 1.6: session eligibility for profile evidence ---

    @Test
    fun longSessionWithoutMeasurableMetrics_isNotCountedAsEvidence() = runTest {
        // A >= 120 s session that produced NO measurable metrics is open time, not training → excluded entirely
        // (even the score path), so the contribution is null and ATTENTION stays CALIBRATING.
        val idle = SessionResult(
            sessionId = SessionId("idle"),
            mode = Mode.TRAINING,
            exerciseFocus = ExerciseId.E1_VIGIA,
            blocks = emptyList(),
            startedAtMs = 0L,
            endedAtMs = 200_000L, // >= 120 s
            sessionMetrics = emptyMap(), // no measurable evidence
        )
        val source = FocusProfileSource(
            FakeHistory(listOf(idle)),
            FakeProfiles(null),
            FakeCalculator(focusScore(60.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.STABLE, true)),
        )
        assertNull(source.contribution("u1"), "idle >=120s session with empty metrics must not feed the profile")
    }

    @Test
    fun duplicateSessionId_countsOnceForCrossSessionEvidence() = runTest {
        // Two eligible sessions sharing the same id → DC-3 counts a single distinct session (not two).
        val contribution = FocusProfileSource(
            FakeHistory(listOf(validSession("same", 0), validSession("same", 1))),
            FakeProfiles(null),
            FakeCalculator(focusScore(60.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.STABLE, true)),
        ).contribution("u1")!!
        assertEquals(1, contribution.sessionEvidence?.validSessions, "same session id de-duplicates to one")
    }
}

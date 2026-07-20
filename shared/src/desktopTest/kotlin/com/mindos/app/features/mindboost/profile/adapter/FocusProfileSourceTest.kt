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
import com.mindos.app.features.mindboost.profile.data.adapter.FocusProfileSource as RealFocusProfileSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

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

private fun measurableMetrics(): MetricSet = mapOf(
    MetricKey.SUSTAINED_FOCUS_INDEX to MetricValue(MetricKey.SUSTAINED_FOCUS_INDEX, 70.0, 60, true, MetricUnit.INDEX),
)

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

private const val TEST_NOW_MS = 10_000_000_000L

private fun FocusProfileSource(
    history: SessionHistoryRepository,
    profiles: FocusProfileRepository,
    calculator: FocusScoreCalculator,
    analyzer: TrendAnalyzer,
) = RealFocusProfileSource(history, profiles, calculator, analyzer, clock = { TEST_NOW_MS })

/**
 * SYSTEM-1 6R3 R3-FINAL D1 — FocusProfileSource is FAIL-CLOSED: `SessionResult` transports no measurement purpose
 * and no versioned lineage/scale-key, so the source can never prove its data is measurement-eligible (never
 * training/practice) and ALWAYS returns null → ATTENTION stays CALIBRATING. These tests pin the invariant that no
 * input — not even a maximally confident, high-volume, improving history — can produce a contribution. The
 * measurement-eligibility RULE itself is unit-tested in [com.mindos.app.features.mindboost.core.session
 * .purpose.ProfileMeasurementEligibilityTest]; the eligible-path computation is the retained wiring point for
 * the purpose+lineage carrier slice.
 */
class FocusProfileSourceTest {

    @Test
    fun noSessions_failsClosed_null() = runTest {
        val source = FocusProfileSource(
            FakeHistory(emptyList()),
            FakeProfiles(null),
            FakeCalculator(focusScore(50.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.STABLE, true)),
        )
        assertNull(source.contribution("u1"))
    }

    @Test
    fun aMaximallyConfidentHighVolumeImprovingTrainingHistory_stillFailsClosed_null() = runTest {
        // The charter guarantee (§7.1): perfect-looking TRAINING is still training. Without a sealed measurement
        // purpose + complete lineage it can never feed the profile — no "preliminary" label rescues it.
        val highlyConfident = UserCognitiveProfile(userId = "u1", archetypeConfidence = 1.0, totalSessions = 100)
        val source = FocusProfileSource(
            FakeHistory(List(5) { validSession("s$it", it.toLong()) }),
            FakeProfiles(highlyConfident),
            FakeCalculator(focusScore(99.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.IMPROVING, true)),
        )
        assertNull(source.contribution("u1"))
    }

    @Test
    fun idleSessionWithoutMeasurableMetrics_failsClosed_null() = runTest {
        val idle = SessionResult(
            sessionId = SessionId("idle"),
            mode = Mode.TRAINING,
            exerciseFocus = ExerciseId.E1_VIGIA,
            blocks = emptyList(),
            startedAtMs = 0L,
            endedAtMs = 200_000L,
            sessionMetrics = emptyMap(),
        )
        val source = FocusProfileSource(
            FakeHistory(listOf(idle)),
            FakeProfiles(null),
            FakeCalculator(focusScore(60.0, true)),
            FakeAnalyzer(metricTrend(TrendDirection.STABLE, true)),
        )
        assertNull(source.contribution("u1"))
    }
}

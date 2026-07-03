package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.profile.data.adapter.SpeedProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.speed.data.persistence.DeletionReport
import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot
import com.mindos.app.features.mindboost.speed.domain.score.SpeedScoreEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSpeedStore(
    private val sessions: List<SessionRecord> = emptyList(),
    private val metrics: Map<String, SessionMetrics> = emptyMap(),
    private val quality: Map<String, SessionQuality> = emptyMap(),
) : SpeedMeasurementStore {
    override suspend fun sessionsForUser(userId: String): List<SessionRecord> = sessions.filter { it.userId == userId }
    override suspend fun sessionMetrics(sessionId: String): SessionMetrics? = metrics[sessionId]
    override suspend fun qualityLogs(sessionId: String): List<QualityDecisionLog> =
        quality[sessionId]?.let {
            listOf(QualityDecisionLog("q-$sessionId", sessionId, DeviceTimingRisk.LOW, it, emptyList(), 0L))
        } ?: emptyList()

    override suspend fun saveRun(run: MeasurementRun) = error("unused")
    override suspend fun deviceProfile(id: String): DeviceProfile? = null
    override suspend fun allDeviceProfiles(): List<DeviceProfile> = emptyList()
    override suspend fun session(sessionId: String): SessionRecord? = sessions.firstOrNull { it.id == sessionId }
    override suspend fun blocksForSession(sessionId: String): List<BlockRecord> = emptyList()
    override suspend fun trialsForBlock(blockId: String): List<TrialRecord> = emptyList()
    override suspend fun blockMetricsForSession(sessionId: String): List<BlockMetrics> = emptyList()
    override suspend fun versionSnapshot(sessionId: String): VersionSnapshot? = null
    override suspend fun loadRun(sessionId: String): MeasurementRun? = null
    override suspend fun deleteUserData(userId: String): DeletionReport = error("unused")
}

private fun session(id: String, ts: Long, userId: String = "u1") =
    SessionRecord(id = id, userId = userId, timestampEpochMs = ts, deviceProfileId = "dev", durationMs = 1_000L)

private fun metrics(
    sessionId: String,
    pc: Double = 0.9,
    cv: Double? = 0.2,
    vce: Double? = 4.0,
    rtMedian: Double? = 3.0e8,
    totalTrials: Int = 60,
    source: TimestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
    precision: InputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
) = SessionMetrics(
    sessionId = sessionId,
    totalTrials = totalTrials,
    pc = pc,
    rtMedianNanos = rtMedian,
    rtMeanNanos = 3.1e8,
    cv = cv,
    lapseRate = 0.05,
    anticipationRate = 0.0,
    faRate = 0.0,
    omissionRate = 0.0,
    impIdx = 0.1,
    fatIdx = 0.0,
    vce = vce,
    timestampSource = source,
    inputTimestampPrecision = precision,
    timebase = Timebase.ANDROID_UI_UPTIME_NANOS,
)

class SpeedProfileSourceTest {

    @Test
    fun contribution_returnsNull_whileAntiGamingCoverageIsPartial() = runTest {
        // The PUBLIC contribution gates on anti-gaming coverage: Velocidad's SpeedAntiGaming is only PARTIAL,
        // insufficient for an RT-critical metric, so even a healthy authoritative HIGH-quality run does NOT
        // feed the profile (CALIBRATING). The eligible computation itself is verified via eligibleContribution.
        val m = metrics("s1")
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.HIGH))
        assertNull(SpeedProfileSource(store).contribution("u1"))
    }

    @Test
    fun noSessions_returnsNull() = runTest {
        assertNull(SpeedProfileSource(FakeSpeedStore()).eligibleContribution("u1"))
    }

    @Test
    fun healthyRun_mapsScoreTotal_isPreliminary_confidenceCappedBelowEstablished() = runTest {
        val m = metrics("s1")
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.HIGH))
        val contribution = SpeedProfileSource(store).eligibleContribution("u1")!!
        assertEquals(CognitiveModuleId.VELOCIDAD, contribution.moduleId)
        assertEquals(SpeedScoreEngine().score(m).total!!, contribution.normalizedScore!!, 1e-9)
        assertTrue(contribution.preliminary)
        assertTrue(contribution.confidence <= 0.6 + 1e-9, "preliminary confidence must stay below ESTABLISHED")
        assertEquals(100L, contribution.updatedAtMs, "real capture time flows through (session timestampEpochMs)")
    }

    @Test
    fun runWithoutComputableSpeed_returnsNull_neverSilentZero() = runTest {
        // vce == null -> usefulSpeed == null -> processing speed not measurable -> CALIBRATING, not 0.
        val m = metrics("s1", vce = null)
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.HIGH))
        assertNull(SpeedProfileSource(store).eligibleContribution("u1"))
    }

    @Test
    fun zeroCorrectTrials_returnsNull_neverSilentZero() = runTest {
        // pc == 0 collapses SpeedScoreEngine.total to a silent 0; the adapter must return null instead.
        val m = metrics("s1", pc = 0.0, vce = 4.0)
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.HIGH))
        assertNull(SpeedProfileSource(store).eligibleContribution("u1"))
    }

    @Test
    fun onlyContaminatedRuns_returnNull() = runTest {
        val m = metrics("s1")
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.CONTAMINATED))
        assertNull(SpeedProfileSource(store).eligibleContribution("u1"))
    }

    @Test
    fun nonAuthoritativeTimebase_returnsNull_excludedFromProfile() = runTest {
        // Desktop/iOS fallback (SYSTEM_CLOCK_MONOTONIC / MILLIS_DERIVED) is non-authoritative for an
        // RT-critical metric: even a healthy, HIGH-quality run must NOT feed PROCESSING_SPEED.
        val m = metrics(
            "s1",
            source = TimestampSource.SYSTEM_CLOCK_MONOTONIC,
            precision = InputTimestampPrecision.MILLIS_DERIVED,
        )
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.HIGH))
        assertNull(SpeedProfileSource(store).eligibleContribution("u1"))
    }

    @Test
    fun millisDerivedPrecision_isNonAuthoritative_returnsNull() = runTest {
        // Android API < 34 keeps the Choreographer source but only MILLIS_DERIVED precision -> excluded.
        val m = metrics("s1", precision = InputTimestampPrecision.MILLIS_DERIVED)
        val store = FakeSpeedStore(listOf(session("s1", 100L)), mapOf("s1" to m), mapOf("s1" to SessionQuality.HIGH))
        assertNull(SpeedProfileSource(store).eligibleContribution("u1"))
    }

    @Test
    fun mixedTimebases_onlyAuthoritativeSessionsInformTheProfile() = runTest {
        // One non-authoritative (fallback) + one authoritative run: the contribution comes from the
        // authoritative run only, and the count reflects a single qualifying session (no trend yet).
        val sessions = listOf(session("s1", 100L), session("s2", 200L))
        val m = mapOf(
            "s1" to metrics(
                "s1", vce = 5.0,
                source = TimestampSource.SYSTEM_CLOCK_MONOTONIC,
                precision = InputTimestampPrecision.MILLIS_DERIVED,
            ),
            "s2" to metrics("s2", vce = 4.0),
        )
        val q = mapOf("s1" to SessionQuality.HIGH, "s2" to SessionQuality.HIGH)
        val contribution = SpeedProfileSource(FakeSpeedStore(sessions, m, q)).eligibleContribution("u1")!!
        assertEquals(SpeedScoreEngine().score(m.getValue("s2")).total!!, contribution.normalizedScore!!, 1e-9)
        assertEquals(ProfileTrend.UNKNOWN, contribution.trend) // only 1 authoritative session -> no trend
    }

    @Test
    fun increasingSpeedAcrossSessions_yieldsImprovingTrend() = runTest {
        val sessions = listOf(session("s1", 100L), session("s2", 200L), session("s3", 300L))
        val m = mapOf(
            "s1" to metrics("s1", vce = 2.0),
            "s2" to metrics("s2", vce = 3.0),
            "s3" to metrics("s3", vce = 5.0),
        )
        val q = mapOf("s1" to SessionQuality.HIGH, "s2" to SessionQuality.HIGH, "s3" to SessionQuality.HIGH)
        val contribution = SpeedProfileSource(FakeSpeedStore(sessions, m, q)).eligibleContribution("u1")!!
        assertEquals(ProfileTrend.IMPROVING, contribution.trend)
    }
}

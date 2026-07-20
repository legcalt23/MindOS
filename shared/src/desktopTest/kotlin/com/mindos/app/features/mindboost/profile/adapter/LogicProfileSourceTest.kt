package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.logic.LogicTestFixtures
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfiguration
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.LogicSessionResult
import com.mindos.app.features.mindboost.logic.domain.model.LogicSkill
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.profile.data.adapter.LogicProfileSource as RealLogicProfileSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

private class FakeLogicRepo(
    private val sessions: List<LogicSession> = emptyList(),
    private val skills: List<SkillState> = emptyList(),
) : LogicTrainingRepository {
    override suspend fun recentSessions(userId: String, limit: Int): List<LogicSession> = sessions.take(limit)
    override suspend fun loadSkillStates(userId: String): List<SkillState> = skills
    override suspend fun saveSession(session: LogicSession) = Unit
    override suspend fun saveSkillStates(userId: String, states: List<SkillState>) = Unit
    override suspend fun saveConfiguration(userId: String, configuration: LogicConfiguration) = Unit
    override suspend fun loadConfiguration(userId: String): LogicConfiguration = error("unused")
    override suspend fun deleteUserData(userId: String) = Unit
}

private fun logicSession(icr: Double, endedAt: Long = 300_000L) = LogicSession(
    id = "s$endedAt",
    userId = "u1",
    startedAt = endedAt - 200_000L,
    endedAt = endedAt,
    attempts = listOf(LogicTestFixtures.attempt()),
    result = LogicSessionResult(
        averageIcr = icr,
        inferentialAccuracy = 0.0,
        averageJustification = 0.0,
        averageConfidenceCalibration = 0.0,
        transferConsistency = null,
        topErrorTags = emptyList(),
        recommendation = "",
    ),
)

private const val TEST_NOW_MS = 10_000_000_000L

private fun LogicProfileSource(repository: LogicTrainingRepository) =
    RealLogicProfileSource(repository, clock = { TEST_NOW_MS })

/**
 * SYSTEM-1 6R3 R3-FINAL D1/D2 — LogicProfileSource is FAIL-CLOSED: `LogicSession` transports no measurement purpose
 * and no versioned scoringVersion/scale-key, so the source can neither prove measurement-eligibility (never
 * training/practice, §7.1) nor establish comparability for a cross-session mean (§9.3), and ALWAYS returns null →
 * REASONING stays CALIBRATING. These tests pin that no input can produce a contribution. The eligibility RULE is in
 * [com.mindos.app.features.mindboost.core.session.purpose.ProfileMeasurementEligibilityTest].
 */
class LogicProfileSourceTest {

    @Test
    fun noSessions_failsClosed_null() = runTest {
        assertNull(LogicProfileSource(FakeLogicRepo()).contribution("u1"))
    }

    @Test
    fun aMaximallyConfidentHighVolumeImprovingTrainingHistory_stillFailsClosed_null() = runTest {
        // The charter guarantee (§7.1 + §9.3): perfect, high-volume, fully-precise TRAINING with no scale-key can
        // never feed the profile — no cross-version mean, no "preliminary" rescue.
        val manySessions = List(8) { logicSession(95.0, endedAt = (it + 1).toLong()) }
        val fullyPrecise = listOf(SkillState(skillId = LogicSkill.NECESSARY_DEDUCTION, uncertainty = 0.0))
        assertNull(LogicProfileSource(FakeLogicRepo(manySessions, fullyPrecise)).contribution("u1"))
    }

    @Test
    fun idleSessionWithoutAttempts_failsClosed_null() = runTest {
        val idle = LogicSession(
            id = "idle",
            userId = "u1",
            startedAt = 0L,
            endedAt = 200_000L,
            attempts = emptyList(),
            result = LogicSessionResult(
                averageIcr = 72.0,
                inferentialAccuracy = 0.0,
                averageJustification = 0.0,
                averageConfidenceCalibration = 0.0,
                transferConsistency = null,
                topErrorTags = emptyList(),
                recommendation = "",
            ),
        )
        assertNull(LogicProfileSource(FakeLogicRepo(sessions = listOf(idle))).contribution("u1"))
    }
}

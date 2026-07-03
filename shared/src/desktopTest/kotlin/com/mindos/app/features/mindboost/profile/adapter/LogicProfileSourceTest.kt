package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.logic.domain.model.LogicConfiguration
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.LogicSessionResult
import com.mindos.app.features.mindboost.logic.domain.model.LogicSkill
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.profile.data.adapter.LogicProfileSource
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

private fun logicSession(icr: Double, endedAt: Long = 0L) = LogicSession(
    id = "s$endedAt",
    userId = "u1",
    startedAt = endedAt - 200_000L, // >= 120 s duration so the session counts as valid (DC-3, Fase 1.8)
    endedAt = endedAt,
    attempts = emptyList(),
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

class LogicProfileSourceTest {

    @Test
    fun noSessions_returnsNull() = runTest {
        assertNull(LogicProfileSource(FakeLogicRepo()).contribution("u1"))
    }

    @Test
    fun mapsRecentAverageIcrOver100_withVolumeBasedSufficiencyAndConfidence() = runTest {
        val source = LogicProfileSource(FakeLogicRepo(sessions = listOf(logicSession(60.0, endedAt = 900L))))
        val contribution = source.contribution("u1")!!
        assertEquals(CognitiveModuleId.LOGICA, contribution.moduleId)
        assertEquals(0.6, contribution.normalizedScore!!, 1e-9)
        assertEquals(0.2, contribution.dataSufficiency, 1e-9) // 1 / 5
        assertEquals(0.0625, contribution.confidence, 1e-9) // volume (1/8) blended with precision 0
        assertTrue(contribution.preliminary)
        assertEquals(900L, contribution.updatedAtMs)
    }

    @Test
    fun onlyTheRecentWindowFeedsTheScore() = runTest {
        // newest-first: five strong sessions then an old weak one — the old one must NOT drag the score.
        val sessions = listOf(
            logicSession(90.0, 6), logicSession(90.0, 5), logicSession(90.0, 4),
            logicSession(90.0, 3), logicSession(90.0, 2), logicSession(10.0, 1),
        )
        val contribution = LogicProfileSource(FakeLogicRepo(sessions = sessions)).contribution("u1")!!
        assertEquals(0.9, contribution.normalizedScore!!, 1e-9)
    }

    @Test
    fun confidenceBlendsSessionVolumeWithSkillPrecision() = runTest {
        val sessions = List(8) { logicSession(50.0, endedAt = it.toLong()) }
        val skills = listOf(SkillState(skillId = LogicSkill.NECESSARY_DEDUCTION, uncertainty = 0.2))
        val contribution = LogicProfileSource(FakeLogicRepo(sessions, skills)).contribution("u1")!!
        assertEquals(0.9, contribution.confidence, 1e-9) // (volume 1.0 + precision 0.8) / 2
    }

    @Test
    fun improvingAverageIcrOverTime_yieldsImprovingTrend() = runTest {
        // newest-first: recent strong, older weak.
        val sessions = listOf(
            logicSession(80.0, 4), logicSession(80.0, 3), logicSession(40.0, 2), logicSession(40.0, 1),
        )
        val contribution = LogicProfileSource(FakeLogicRepo(sessions = sessions)).contribution("u1")!!
        assertEquals(ProfileTrend.IMPROVING, contribution.trend)
    }

    @Test
    fun contributionSurfacesReasoningThroughTheAggregator_notSilentlyDropped() = runTest {
        // EX1-class regression guard: the isolated-contribution tests above don't prove the fold. Assert the
        // contribution actually clears the aggregator's dataSufficiency≥0.5 AND confidence≥0.4 gates and
        // SURFACES REASONING (EMERGING/preliminary) — a silent drop (the dataSufficiency≠confidence bug class)
        // would leave it CALIBRATING forever with green unit tests.
        val sessions = List(8) { logicSession(72.0, endedAt = it.toLong()) }
        val contribution = LogicProfileSource(FakeLogicRepo(sessions = sessions)).contribution("u1")!!
        // Logic emits REAL cross-session evidence: 8 valid distinct sessions, all on day 0.
        assertEquals(SessionEvidence(validSessions = 8, distinctDays = 1), contribution.sessionEvidence)
        val profile = MindBoostProfileAggregator().aggregate("u1", listOf(contribution), nowMs = 1L)
        val reasoning = profile.dimensions.getValue(CognitiveDimension.REASONING)
        assertEquals(DimensionStatus.EMERGING, reasoning.status)
        assertEquals(0.72, assertNotNull(reasoning.value), 1e-9)
        assertTrue(reasoning.preliminary)
        assertEquals(listOf(CognitiveModuleId.LOGICA), reasoning.contributingModules)
    }

    @Test
    fun insufficientSessions_keepReasoningCalibrating_evenWithANonNullScore() = runTest {
        // Pins the exact EX1 locus: one session yields a REAL 0.72 score but dataSufficiency 0.2 (< 0.5), so
        // the aggregator MUST drop it — REASONING stays CALIBRATING with a null value, never surfacing a
        // number off a single session.
        val contribution =
            LogicProfileSource(FakeLogicRepo(sessions = listOf(logicSession(72.0)))).contribution("u1")!!
        assertNotNull(contribution.normalizedScore, "the isolated contribution DOES carry a value")
        val profile = MindBoostProfileAggregator().aggregate("u1", listOf(contribution), nowMs = 1L)
        val reasoning = profile.dimensions.getValue(CognitiveDimension.REASONING)
        assertEquals(DimensionStatus.CALIBRATING, reasoning.status)
        assertNull(reasoning.value)
    }
}

package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend as EmotionTrend
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileReadOnly
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileSnapshot
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import com.mindos.app.features.mindboost.language.domain.repository.EvidenceRepository
import com.mindos.app.features.mindboost.language.domain.repository.SessionSummaryRepository
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfiguration
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.LogicSessionResult
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.decisiones.data.InMemoryReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryPairsResultStore
import com.mindos.app.features.mindboost.memory.data.InMemorySpanResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial
import com.mindos.app.features.mindboost.profile.data.InMemoryMindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.data.mindBoostProfileSources
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.speed.data.persistence.DeletionReport
import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class EmptyEmotion : EmotionProfileReadOnly {
    override suspend fun snapshot(userId: String): EmotionProfileSnapshot? = null
}

/** A fully-matured emotion history (STABLE tier, full sufficiency) — the strongest case for fabrication. */
private class StableEmotion : EmotionProfileReadOnly {
    override suspend fun snapshot(userId: String): EmotionProfileSnapshot? = EmotionProfileSnapshot(
        tier = ProfileTier.STABLE,
        dataSufficiency = 1.0,
        confidence = 1.0,
        trend = EmotionTrend.IMPROVING,
    )
}

private class EmptySpeedStore : SpeedMeasurementStore {
    override suspend fun saveRun(run: MeasurementRun) = Unit
    override suspend fun deviceProfile(id: String): DeviceProfile? = null
    override suspend fun allDeviceProfiles(): List<DeviceProfile> = emptyList()
    override suspend fun session(sessionId: String): SessionRecord? = null
    override suspend fun sessionsForUser(userId: String): List<SessionRecord> = emptyList()
    override suspend fun blocksForSession(sessionId: String): List<BlockRecord> = emptyList()
    override suspend fun trialsForBlock(blockId: String): List<TrialRecord> = emptyList()
    override suspend fun blockMetricsForSession(sessionId: String): List<BlockMetrics> = emptyList()
    override suspend fun sessionMetrics(sessionId: String): SessionMetrics? = null
    override suspend fun versionSnapshot(sessionId: String): VersionSnapshot? = null
    override suspend fun qualityLogs(sessionId: String): List<QualityDecisionLog> = emptyList()
    override suspend fun loadRun(sessionId: String): MeasurementRun? = null
    override suspend fun deleteUserData(userId: String): DeletionReport = error("unused")
}

private class EmptyFocusHistory : SessionHistoryRepository {
    override suspend fun append(result: SessionResult) = Unit
    override suspend fun recent(userId: String, limit: Int): List<SessionResult> = emptyList()
}

private class EmptyFocusProfiles : FocusProfileRepository {
    override suspend fun load(userId: String): UserCognitiveProfile? = null
    override suspend fun save(profile: UserCognitiveProfile) = Unit
}

private class EmptyLogic : LogicTrainingRepository {
    override suspend fun saveSession(session: LogicSession) = Unit
    override suspend fun recentSessions(userId: String, limit: Int): List<LogicSession> = emptyList()
    override suspend fun saveSkillStates(userId: String, states: List<SkillState>) = Unit
    override suspend fun loadSkillStates(userId: String): List<SkillState> = emptyList()
    override suspend fun saveConfiguration(userId: String, configuration: LogicConfiguration) = Unit
    override suspend fun loadConfiguration(userId: String): LogicConfiguration = error("unused")
    override suspend fun deleteUserData(userId: String) = Unit
}

private class EmptyEvidence : EvidenceRepository {
    override suspend fun append(packet: EvidencePacket) = Unit
    override suspend fun packets(userIdHash: UserIdHash): List<EvidencePacket> = emptyList()
    override suspend fun loadProfile(userIdHash: UserIdHash): UserLanguageProfile =
        UserLanguageProfile(userIdHash = UserIdHash("anon"), traitEstimates = emptyMap(), updatedAtEpochMs = 0L)
    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = 0
}

private class EmptySummaries : SessionSummaryRepository {
    override suspend fun save(summary: SessionSummary) = Unit
    override suspend fun summaries(userIdHash: UserIdHash): List<SessionSummary> = emptyList()
    override suspend fun get(userIdHash: UserIdHash, sessionId: LanguageSessionId): SessionSummary? = null
    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = 0
}

/** Focus history/profiles/logic repos that RETURN data — for the full-stack (real calculator) e2e tests. */
private class DataFocusHistory(private val sessions: List<SessionResult>) : SessionHistoryRepository {
    override suspend fun append(result: SessionResult) = Unit
    override suspend fun recent(userId: String, limit: Int): List<SessionResult> = sessions.take(limit)
}

private class DataFocusProfiles(private val profile: UserCognitiveProfile) : FocusProfileRepository {
    override suspend fun load(userId: String): UserCognitiveProfile? = profile
    override suspend fun save(profile: UserCognitiveProfile) = Unit
}

private class DataLogic(private val sessions: List<LogicSession>) : LogicTrainingRepository {
    override suspend fun saveSession(session: LogicSession) = Unit
    override suspend fun recentSessions(userId: String, limit: Int): List<LogicSession> = sessions.take(limit)
    override suspend fun saveSkillStates(userId: String, states: List<SkillState>) = Unit
    override suspend fun loadSkillStates(userId: String): List<SkillState> = emptyList()
    override suspend fun saveConfiguration(userId: String, configuration: LogicConfiguration) = Unit
    override suspend fun loadConfiguration(userId: String): LogicConfiguration = error("unused")
    override suspend fun deleteUserData(userId: String) = Unit
}

/** A focus session whose metrics make the REAL DefaultFocusScoreCalculator produce a confident score. */
private fun focusSessionWithConfidentMetrics(id: String, endedAtMs: Long): SessionResult = SessionResult(
    sessionId = SessionId(id),
    mode = Mode.TRAINING,
    exerciseFocus = ExerciseId.E1_VIGIA,
    blocks = emptyList(),
    startedAtMs = endedAtMs - 200_000L, // valid >= 120 s (DC-3, Fase 1.8)
    endedAtMs = endedAtMs,
    sessionMetrics = mapOf(
        // Two confident components at n=60 (≥ MIN_COMPONENTS=2, ≥ MIN_N=20): 70 + 80 (0.80×100) → 75.
        MetricKey.SUSTAINED_FOCUS_INDEX to MetricValue(MetricKey.SUSTAINED_FOCUS_INDEX, 70.0, 60, true, MetricUnit.INDEX),
        MetricKey.ACCURACY to MetricValue(MetricKey.ACCURACY, 0.80, 60, true, MetricUnit.PROPORTION),
    ),
)

private fun logicSessionWithIcr(icr: Double, endedAt: Long): LogicSession = LogicSession(
    id = "s$endedAt",
    userId = "u1",
    startedAt = endedAt - 200_000L, // valid >= 120 s (DC-3, Fase 1.8)
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

private fun allSources() = mindBoostProfileSources(
    emotionProfile = EmptyEmotion(),
    speedStore = EmptySpeedStore(),
    focusHistory = EmptyFocusHistory(),
    focusProfiles = EmptyFocusProfiles(),
    logicRepository = EmptyLogic(),
    languageEvidence = EmptyEvidence(),
    languageSummaries = EmptySummaries(),
    spanStore = InMemorySpanResultStore(),
    pairsStore = InMemoryPairsResultStore(),
    deferredStore = InMemoryDeferredResultStore(),
    reversibilityStore = InMemoryReversibilityResultStore(),
)

class MindBoostProfileSourcesTest {

    @Test
    fun wiresTheEightHonestModules_episodicMemorySplitFromWorkingMemory() {
        val ids = allSources().map { it.moduleId }.toSet()
        assertEquals(
            setOf(
                CognitiveModuleId.EMOCIONES,
                CognitiveModuleId.VELOCIDAD,
                CognitiveModuleId.ENFOQUE,
                CognitiveModuleId.LOGICA,
                CognitiveModuleId.LENGUAJE,
                CognitiveModuleId.MEMORIA,
                CognitiveModuleId.MEMORIA_EPISODICA,
                CognitiveModuleId.DECISIONES,
            ),
            ids,
        )
        assertEquals(
            2,
            allSources().count { it.moduleId == CognitiveModuleId.MEMORIA },
            "WORKING_MEMORY is fed by TWO sources: EX1 complex-span + EX4 associative",
        )
        assertEquals(
            1,
            allSources().count { it.moduleId == CognitiveModuleId.MEMORIA_EPISODICA },
            "EPISODIC_MEMORY is fed by ONE source: EX3 24h delayed recall",
        )
        assertEquals(
            1,
            allSources().count { it.moduleId == CognitiveModuleId.DECISIONES },
            "Decisiones is fed by one source: E2 reversibility",
        )
    }

    @Test
    fun absentModule_isSkipped_leavingItsDimensionUnsourced() {
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = null, // not production-wired -> skipped
            languageSummaries = null,
            spanStore = null, // not provided -> Memoria span source skipped (same skip mechanic as Lenguaje)
            pairsStore = null, // not provided -> Memoria pairs source skipped
            deferredStore = null, // not provided -> Memoria episodic source skipped
            reversibilityStore = null, // not provided -> Decisiones source skipped
        )
        val ids = sources.map { it.moduleId }.toSet()
        assertEquals(4, sources.size)
        assertFalse(CognitiveModuleId.LENGUAJE in ids, "absent Lenguaje source is skipped")
        assertFalse(CognitiveModuleId.MEMORIA in ids, "absent memory stores -> Memoria skipped")
        assertTrue(CognitiveModuleId.VELOCIDAD in ids)
    }

    @Test
    fun service_withRealSourcesButNoData_yieldsAllDimensionsCalibrating() = runTest {
        val service = MindBoostProfileService(allSources(), InMemoryMindBoostProfileRepository(), clock = { 1L })
        val profile = service.refresh("u1")
        assertTrue(
            profile.dimensions.values.all { it.status == DimensionStatus.CALIBRATING },
            "no module has data -> every dimension stays CALIBRATING (no fabricated values)",
        )
    }

    @Test
    fun service_withSufficientMemoryData_surfacesWorkingMemoryAsEmergingPreliminary() = runTest {
        val spanStore = InMemorySpanResultStore()
        repeat(20) { i ->
            spanStore.record(
                "u1",
                SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = 4),
                listOf(i % 2), // varied yes/no → anti-gaming clean
            )
        }
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = spanStore,
            pairsStore = null,
            deferredStore = null,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val workingMemory = service.refresh("u1").dimensions.getValue(CognitiveDimension.WORKING_MEMORY)

        // The honest end-to-end claim: ≥20 valid trials SURFACE the dimension (not silently dropped),
        // as a preliminary EMERGING value — never CALIBRATING-forever, never ESTABLISHED yet.
        assertEquals(DimensionStatus.EMERGING, workingMemory.status)
        assertEquals(0.5, workingMemory.value) // span 4 / MAX_SPAN 8
        assertTrue(workingMemory.preliminary, "one accumulated block → preliminary, never ESTABLISHED")
    }

    @Test
    fun service_withSufficientPairsData_surfacesWorkingMemoryFromTheAssociativeSource() = runTest {
        val pairsStore = InMemoryPairsResultStore()
        repeat(20) { pairsStore.record("u1", PairsTrial(targetCount = 5, hits = 4, distractorCount = 6, falseAlarms = 0)) }
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = null,
            pairsStore = pairsStore,
            deferredStore = null,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val workingMemory = service.refresh("u1").dimensions.getValue(CognitiveDimension.WORKING_MEMORY)

        assertEquals(DimensionStatus.EMERGING, workingMemory.status)
        assertEquals(0.8, assertNotNull(workingMemory.value), absoluteTolerance = 1e-9) // associative hit rate 4/5
        assertTrue(workingMemory.preliminary)
    }

    @Test
    fun service_blendsSpanAndPairsIntoOneWorkingMemoryDimension() = runTest {
        val spanStore = InMemorySpanResultStore()
        repeat(20) { i ->
            spanStore.record("u1", SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = 4), listOf(i % 2))
        }
        val pairsStore = InMemoryPairsResultStore()
        repeat(20) { pairsStore.record("u1", PairsTrial(targetCount = 5, hits = 4, distractorCount = 6, falseAlarms = 0)) }
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = spanStore,
            pairsStore = pairsStore,
            deferredStore = null,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val workingMemory = service.refresh("u1").dimensions.getValue(CognitiveDimension.WORKING_MEMORY)

        // Both MEMORIA sources (span 0.5, associative 0.8) blend into ONE dimension, weighted by
        // confidence·sufficiency (equal here) → 0.65 — the coarse one-dimension-per-module design.
        assertEquals(DimensionStatus.EMERGING, workingMemory.status)
        assertEquals(0.65, assertNotNull(workingMemory.value), absoluteTolerance = 1e-9)
        assertEquals(2, workingMemory.contributingModules.size, "both Memoria sources contribute")
    }

    @Test
    fun service_withSufficientDeferredData_surfacesEpisodicMemory_notWorkingMemory() = runTest {
        val deferredStore = InMemoryDeferredResultStore()
        // 20 discriminating 24h delayed-recall outcomes: hit rate 6/8 = 0.75, false-alarm rate 0.
        repeat(20) {
            deferredStore.record("u1", DeferredRecallOutcome(studiedCount = 8, hits = 6, distractorCount = 8, falseAlarms = 0))
        }
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = null,
            pairsStore = null,
            deferredStore = deferredStore,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val profile = service.refresh("u1")
        val episodic = profile.dimensions.getValue(CognitiveDimension.EPISODIC_MEMORY)

        // EX3's episodic 24h-retention signal now surfaces in its OWN dimension (no longer folded under
        // WORKING_MEMORY) as EMERGING/preliminary, never CALIBRATING-forever, never ESTABLISHED yet.
        assertEquals(DimensionStatus.EMERGING, episodic.status)
        assertEquals(0.75, assertNotNull(episodic.value), absoluteTolerance = 1e-9) // delayed hit rate 6/8
        assertTrue(episodic.preliminary, "one accumulated block → preliminary, never ESTABLISHED")
        // WORKING_MEMORY has no span/pairs data here → stays CALIBRATING (the episodic signal does NOT leak in).
        assertEquals(DimensionStatus.CALIBRATING, profile.dimensions.getValue(CognitiveDimension.WORKING_MEMORY).status)
    }

    @Test
    fun service_splitsMemoryAcrossWorkingAndEpisodicDimensions() = runTest {
        val spanStore = InMemorySpanResultStore()
        repeat(20) { i ->
            spanStore.record("u1", SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = 4), listOf(i % 2))
        }
        val pairsStore = InMemoryPairsResultStore()
        repeat(20) { pairsStore.record("u1", PairsTrial(targetCount = 5, hits = 4, distractorCount = 6, falseAlarms = 0)) }
        val deferredStore = InMemoryDeferredResultStore()
        repeat(20) {
            deferredStore.record("u1", DeferredRecallOutcome(studiedCount = 8, hits = 6, distractorCount = 8, falseAlarms = 0))
        }
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = spanStore,
            pairsStore = pairsStore,
            deferredStore = deferredStore,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val profile = service.refresh("u1")
        val workingMemory = profile.dimensions.getValue(CognitiveDimension.WORKING_MEMORY)
        val episodic = profile.dimensions.getValue(CognitiveDimension.EPISODIC_MEMORY)

        // The two WORKING-memory sources (span 0.5, associative 0.8) blend into WORKING_MEMORY (→ 0.65);
        // the EPISODIC source (delayed recall 0.75) now lands in its OWN dimension, no longer folded in.
        assertEquals(DimensionStatus.EMERGING, workingMemory.status)
        assertEquals(0.65, assertNotNull(workingMemory.value), absoluteTolerance = 1e-9)
        assertEquals(2, workingMemory.contributingModules.size, "span + associative feed WORKING_MEMORY")

        assertEquals(DimensionStatus.EMERGING, episodic.status)
        assertEquals(0.75, assertNotNull(episodic.value), absoluteTolerance = 1e-9)
        assertEquals(1, episodic.contributingModules.size, "only the delayed-recall source feeds EPISODIC_MEMORY")
        assertTrue(workingMemory.preliminary && episodic.preliminary)
    }

    @Test
    fun service_withReversibilityData_keepsDecisionMakingCalibrating_whileContentBankIsUnvalidated() = runTest {
        val reversibilityStore = InMemoryReversibilityResultStore()
        // 24 honest balanced cards (12 reversible + 12 irreversible, all correct). Even so, the live E2
        // scenario source is degenerate (template description + coin-flip label), so the fold marks the bank
        // SMALL_UNVALIDATED and DECISION_MAKING stays CALIBRATING (never a fabricated number) until a real bank.
        reversibilityStore.record(
            "u1",
            List(12) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) } +
                List(12) { ReversibilityTrial(actuallyReversible = false, selectedReversible = false) },
        )
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = null,
            pairsStore = null,
            deferredStore = null,
            reversibilityStore = reversibilityStore,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val decisionMaking = service.refresh("u1").dimensions.getValue(CognitiveDimension.DECISION_MAKING)

        assertEquals(DimensionStatus.CALIBRATING, decisionMaking.status)
        assertNull(decisionMaking.value, "degenerate content bank → no DECISION_MAKING number, never 0")
    }

    @Test
    fun service_surfacesAllWiredDimensionsTogether_withNoCrossLeak() = runTest {
        // Every wired module gets sufficient honest data IN THE SAME profile, to lock that they all surface
        // their OWN dimension simultaneously, with the right value + contributing-source count, and that
        // NOTHING leaks into the dimensions that have no wired source with data.
        val spanStore = InMemorySpanResultStore()
        repeat(20) { i ->
            spanStore.record("u1", SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = 4), listOf(i % 2))
        }
        val pairsStore = InMemoryPairsResultStore()
        repeat(20) { pairsStore.record("u1", PairsTrial(targetCount = 5, hits = 4, distractorCount = 6, falseAlarms = 0)) }
        val deferredStore = InMemoryDeferredResultStore()
        repeat(20) {
            deferredStore.record("u1", DeferredRecallOutcome(studiedCount = 8, hits = 6, distractorCount = 8, falseAlarms = 0))
        }
        val reversibilityStore = InMemoryReversibilityResultStore()
        reversibilityStore.record(
            "u1",
            List(12) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) } +
                List(12) { ReversibilityTrial(actuallyReversible = false, selectedReversible = false) },
        )
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = spanStore,
            pairsStore = pairsStore,
            deferredStore = deferredStore,
            reversibilityStore = reversibilityStore,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val profile = service.refresh("u1")

        val workingMemory = profile.dimensions.getValue(CognitiveDimension.WORKING_MEMORY)
        assertEquals(DimensionStatus.EMERGING, workingMemory.status)
        assertEquals(0.65, assertNotNull(workingMemory.value), absoluteTolerance = 1e-9) // span 0.5 + associative 0.8
        assertEquals(2, workingMemory.contributingModules.size, "span + associative feed WORKING_MEMORY")

        val episodic = profile.dimensions.getValue(CognitiveDimension.EPISODIC_MEMORY)
        assertEquals(DimensionStatus.EMERGING, episodic.status)
        assertEquals(0.75, assertNotNull(episodic.value), absoluteTolerance = 1e-9) // delayed recall 6/8
        assertEquals(1, episodic.contributingModules.size, "only delayed recall feeds EPISODIC_MEMORY")

        // DECISION_MAKING has a wired E2 source WITH sufficient data, but the live content bank is degenerate
        // (SMALL_UNVALIDATED), so it stays CALIBRATING — distinct from the no-data dimensions below.
        val decisionMaking = profile.dimensions.getValue(CognitiveDimension.DECISION_MAKING)
        assertEquals(DimensionStatus.CALIBRATING, decisionMaking.status)
        assertNull(decisionMaking.value, "degenerate content bank → DECISION_MAKING stays CALIBRATING, never a number")

        // No leak: every other dimension without a usable wired source stays CALIBRATING (value null, never a
        // fabricated/leaked number) — ATTENTION/PROCESSING_SPEED/REASONING/VERBAL/EMOTIONAL_REGULATION…
        val surfaced = setOf(
            CognitiveDimension.WORKING_MEMORY,
            CognitiveDimension.EPISODIC_MEMORY,
            CognitiveDimension.DECISION_MAKING, // wired+data but content-blocked; asserted CALIBRATING above
        )
        CognitiveDimension.entries.filter { it !in surfaced }.forEach { dim ->
            assertEquals(
                DimensionStatus.CALIBRATING,
                profile.dimensions.getValue(dim).status,
                "$dim has no usable wired source → must stay CALIBRATING (no cross-leak)",
            )
        }
    }

    @Test
    fun service_withStableEmotionData_keepsEmotionalRegulationCalibrating_neverFabricatesAScore() = runTest {
        // EM1/EM2 → EMOTIONAL_REGULATION end-to-end with a STABLE/full emotion history. By design the
        // emotion source contributes a NULL score (maturity ≠ competence), so the aggregator keeps the
        // dimension CALIBRATING with NO numeric value — never a fabricated competence, never a 0. This is
        // also Regla #8 proven end-to-end: no session outcome (incl. abandonment) can lower this dimension.
        val sources = mindBoostProfileSources(
            emotionProfile = StableEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = null,
            pairsStore = null,
            deferredStore = null,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val emotionalRegulation =
            service.refresh("u1").dimensions.getValue(CognitiveDimension.EMOTIONAL_REGULATION)

        assertEquals(DimensionStatus.CALIBRATING, emotionalRegulation.status)
        assertNull(emotionalRegulation.value, "null-score emotion contribution → no numeric value, never 0")
    }

    @Test
    fun service_withSufficientFocusData_surfacesAttentionAsEmergingPreliminary() = runTest {
        // Full-stack e2e through the REAL DefaultFocusScoreCalculator (not a fake): confident session metrics
        // + a mature-enough profile must SURFACE ATTENTION, not be silently dropped by the aggregator fold.
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = DataFocusHistory(
                listOf(
                    focusSessionWithConfidentMetrics("a", 200_000L),
                    focusSessionWithConfidentMetrics("b", 86_400_000L + 200_000L),
                ),
            ),
            focusProfiles = DataFocusProfiles(
                UserCognitiveProfile(userId = "u1", archetypeConfidence = 0.6, totalSessions = 4),
            ),
            logicRepository = EmptyLogic(),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = null,
            pairsStore = null,
            deferredStore = null,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val attention = service.refresh("u1").dimensions.getValue(CognitiveDimension.ATTENTION)

        // Real calculator: SUSTAINED 70 + ACCURACY 0.80×100=80 → 75 → 0.75; sufficiency 4/8=0.5; conf 0.6.
        assertEquals(DimensionStatus.EMERGING, attention.status)
        assertEquals(0.75, assertNotNull(attention.value), absoluteTolerance = 1e-9)
        assertTrue(attention.preliminary)
        assertEquals(listOf(CognitiveModuleId.ENFOQUE), attention.contributingModules)
    }

    @Test
    fun service_withSufficientLogicData_surfacesReasoningAsEmergingPreliminary() = runTest {
        // Full-stack e2e through the real LogicProfileSource wired by the factory: 8 sessions clear the
        // aggregator gates and SURFACE REASONING as EMERGING/preliminary.
        val sources = mindBoostProfileSources(
            emotionProfile = EmptyEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = EmptyFocusHistory(),
            focusProfiles = EmptyFocusProfiles(),
            logicRepository = DataLogic(List(8) { logicSessionWithIcr(72.0, endedAt = it.toLong()) }),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = null,
            pairsStore = null,
            deferredStore = null,
            reversibilityStore = null,
        )
        val service = MindBoostProfileService(sources, InMemoryMindBoostProfileRepository(), clock = { 1L })
        val reasoning = service.refresh("u1").dimensions.getValue(CognitiveDimension.REASONING)

        assertEquals(DimensionStatus.EMERGING, reasoning.status)
        assertEquals(0.72, assertNotNull(reasoning.value), absoluteTolerance = 1e-9) // avg ICR 72 / 100
        assertTrue(reasoning.preliminary)
        assertEquals(listOf(CognitiveModuleId.LOGICA), reasoning.contributingModules)
    }

    @Test
    fun timestampIntegrity_realSourcesCarryEventTime_unknownSourcesNull_noSourceEmitsZero() = runTest {
        // Fase 1.7 invariant: every PRODUCTION source, built WITH qualifying data, must carry either a real
        // (>0) event time or null ("unknown") — NEVER a fabricated 0L that masquerades as a 1970 timestamp.
        val spanStore = InMemorySpanResultStore()
        repeat(20) { i ->
            spanStore.record("u1", SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = 4), listOf(i % 2))
        }
        val pairsStore = InMemoryPairsResultStore()
        repeat(20) { pairsStore.record("u1", PairsTrial(targetCount = 5, hits = 4, distractorCount = 6, falseAlarms = 0)) }
        val deferredStore = InMemoryDeferredResultStore()
        repeat(20) {
            deferredStore.record("u1", DeferredRecallOutcome(studiedCount = 8, hits = 6, distractorCount = 8, falseAlarms = 0))
        }
        val sources = mindBoostProfileSources(
            emotionProfile = StableEmotion(),
            speedStore = EmptySpeedStore(),
            focusHistory = DataFocusHistory(
                listOf(
                    focusSessionWithConfidentMetrics("a", 200_000L),
                    focusSessionWithConfidentMetrics("b", 86_400_000L + 200_000L),
                ),
            ),
            focusProfiles = EmptyFocusProfiles(), // no stored profile → Focus falls back to the real latest.endedAtMs
            logicRepository = DataLogic(List(8) { logicSessionWithIcr(72.0, endedAt = 100_000L + it.toLong()) }),
            languageEvidence = EmptyEvidence(),
            languageSummaries = EmptySummaries(),
            spanStore = spanStore,
            pairsStore = pairsStore,
            deferredStore = deferredStore,
            reversibilityStore = null,
        )

        // INVARIANT: no production contribution ever carries a fabricated 0L timestamp.
        sources.forEach { s ->
            assertNotEquals(0L, s.contribution("u1")?.updatedAtMs, "${s.moduleId} must never emit a fabricated 0L timestamp")
        }

        // Unknown-time sources → null (trial-accumulation logs / read-only surface expose no single event time).
        sources.filter { it.moduleId == CognitiveModuleId.MEMORIA }.forEach {
            assertNull(it.contribution("u1")?.updatedAtMs, "Memoria (accumulated log) has no single event time → null")
        }
        val byId = sources.associateBy { it.moduleId }
        assertNull(byId.getValue(CognitiveModuleId.MEMORIA_EPISODICA).contribution("u1")?.updatedAtMs)
        assertNull(byId.getValue(CognitiveModuleId.EMOCIONES).contribution("u1")?.updatedAtMs)

        // Real-event sources → a real (>0) time.
        val focusTs = assertNotNull(byId.getValue(CognitiveModuleId.ENFOQUE).contribution("u1")?.updatedAtMs)
        assertTrue(focusTs > 0L, "Focus carries the real session end time")
        val logicTs = assertNotNull(byId.getValue(CognitiveModuleId.LOGICA).contribution("u1")?.updatedAtMs)
        assertTrue(logicTs > 0L, "Logic carries the real session end time")
    }
}

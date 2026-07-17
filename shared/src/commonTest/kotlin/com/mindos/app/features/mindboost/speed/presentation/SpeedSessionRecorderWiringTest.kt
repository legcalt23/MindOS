package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.features.mindboost.core.session.ReproducibleSessionIdentity
import com.mindos.app.features.mindboost.core.session.SessionRecordUpsertOutcome

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorderDiagnostics
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.di.speedSessionRecorder
import com.mindos.app.features.mindboost.speed.data.persistence.InMemorySpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.DefaultStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.StaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.exercise.MultiviaGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MC1 (Fase 2.2): verifies the ADDITIVE write-ahead SessionRecorder wiring in Velocidad. Drives a real
 * [SpeedSessionStateHolder] run with a fake recorder/generator and asserts the lifecycle hooks fire in order
 * (STARTED -> CHECKPOINT* -> FINALIZED), the opaque sessionId is stable and not derived from the user, and a
 * recorder storage failure never breaks the Velocidad session (scoring/persistence stay intact).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedSessionRecorderWiringTest {

    private val ms = SpeedTestFixtures.MS

    private class FakeClock(start: Long = 1_700_000_000_000L) : EpochClock {
        private var t = start
        override fun nowMillis(): Long = t.also { t += 10L }
    }

    private class FakeRecorder : SessionRecorder {
        val ops = mutableListOf<String>()
        val sessionIds = mutableListOf<String>()
        var startBlock: ScoreModuleId? = null
        var startPlanned: Int = -1
        var lastFinalizeCompleted: Int = -1

        override suspend fun onStart(
        sessionId: String,
        block: ScoreModuleId,
        startedAtMs: Long,
        plannedTrials: Int,
        identity: ReproducibleSessionIdentity?,
    ) = SessionRecordUpsertOutcome.Stored.also {
            ops += "onStart"; sessionIds += sessionId; startBlock = block; startPlanned = plannedTrials
        }

        override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
            ops += "onCheckpoint"; sessionIds += sessionId
        }

        override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
            ops += "onFinalize"; sessionIds += sessionId; lastFinalizeCompleted = completedTrials
        }

        override suspend fun onAbandon(sessionId: String) {
            ops += "onAbandon"; sessionIds += sessionId
        }
    }

    private class ThrowingSessionRecordStore : SessionRecordStore {
        override suspend fun upsert(record: SessionRecord) = error("boom")
        override suspend fun find(sessionId: String): SessionRecord? = error("boom")
        override suspend fun all(): List<SessionRecord> = error("boom")
        override suspend fun delete(sessionId: String) = error("boom")
    }

    private class RecordingDiagnostics : SessionRecorderDiagnostics {
        val ops = mutableListOf<String>()
        override fun onRecorderError(operation: String) {
            ops += operation
        }
    }

    @Suppress("LongParameterList") // Cohesive test-only constructor mirror of the real holder's config.
    private fun holder(
        store: InMemorySpeedMeasurementStore,
        scope: CoroutineScope,
        recorder: SessionRecorder,
        generator: SessionIdGenerator,
        blockCount: Int,
        trialsPerBlock: Int,
        staircase: StaircaseEngine? = DefaultStaircaseEngine(initialCce = 0.5),
    ) = SpeedSessionStateHolder(
        scope = scope,
        store = store,
        clock = FakeClock(),
        userId = "u1",
        exerciseId = SpeedExerciseId.E6_MULTIVIA,
        context = SpeedTestFixtures.context,
        apiLevel = 34,
        deviceModel = "Pixel-Test",
        osVersion = "Android 14",
        trialsPerBlock = trialsPerBlock,
        blockCount = blockCount,
        staircase = staircase,
        promptGenerator = MultiviaGenerator(),
        recorder = recorder,
        sessionIdGenerator = generator,
    )

    private fun drive(h: SpeedSessionStateHolder, rtNanos: Long) {
        h.startSession()
        var stimulus = 1_000_000_000L
        var guard = 0
        while (h.state.value.phase != SpeedPhase.Finished && guard < MAX_STEPS) {
            guard++
            when (h.state.value.phase) {
                SpeedPhase.Foreperiod -> h.onForeperiodElapsed()
                SpeedPhase.Stimulus -> {
                    h.onStimulusShown(stimulus)
                    h.onResponse(stimulus + rtNanos, h.state.value.prompt?.correctChoice)
                }
                SpeedPhase.Feedback -> {
                    h.beginNextTrial()
                    stimulus += 3_000_000_000L
                }
                else -> Unit
            }
        }
    }

    @Test
    fun startSession_callsOnStart_withSpeedBlock_andOpaqueId() = runTest {
        val rec = FakeRecorder()
        val h = holder(InMemorySpeedMeasurementStore(), this, rec, { "sess_fixed" }, blockCount = 3, trialsPerBlock = 4)
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        assertEquals("onStart", rec.ops.first())
        assertEquals(ScoreModuleId.SPEED, rec.startBlock)
        assertEquals(12, rec.startPlanned) // 3 blocks * 4 trials
        assertEquals("sess_fixed", rec.sessionIds.first())
        assertFalse(rec.sessionIds.first().contains("u1"), "sessionId must not embed the userId")
        coroutineContext.cancelChildren()
    }

    @Test
    fun multiBlock_checkpointsBetweenBlocks_finalizeOnce_stableId() = runTest {
        val rec = FakeRecorder()
        val h = holder(InMemorySpeedMeasurementStore(), this, rec, { "sess_x" }, blockCount = 3, trialsPerBlock = 4)
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        assertEquals(listOf("onStart", "onCheckpoint", "onCheckpoint", "onFinalize"), rec.ops)
        assertEquals(1, rec.ops.count { it == "onFinalize" }, "finalize exactly once")
        assertTrue(rec.sessionIds.all { it == "sess_x" }, "no new sessionId within one session")
        coroutineContext.cancelChildren()
    }

    @Test
    fun singleBlock_startThenFinalize_noCheckpoint() = runTest {
        val rec = FakeRecorder()
        val h = holder(
            InMemorySpeedMeasurementStore(), this, rec, { "sess_1" },
            blockCount = 1, trialsPerBlock = 4, staircase = null,
        )
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        assertEquals(listOf("onStart", "onFinalize"), rec.ops)
        coroutineContext.cancelChildren()
    }

    @Test
    fun finalize_reportsCompletedTrialCount() = runTest {
        val rec = FakeRecorder()
        val h = holder(InMemorySpeedMeasurementStore(), this, rec, { "s" }, blockCount = 2, trialsPerBlock = 5)
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        assertEquals(10, rec.lastFinalizeCompleted) // 2 blocks * 5 trials
        coroutineContext.cancelChildren()
    }

    @Test
    fun recorderStorageFailure_doesNotBreakSession_orScoring() = runTest {
        val store = InMemorySpeedMeasurementStore()
        val diag = RecordingDiagnostics()
        val recorder = SecureSessionRecorder(ThrowingSessionRecordStore(), EpochClock { 1L }, diag)
        val h = holder(store, this, recorder, { "s" }, blockCount = 2, trialsPerBlock = 4)
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        // Velocidad session still completes + persists (scoring path untouched):
        assertEquals(SpeedPhase.Finished, h.state.value.phase)
        assertTrue(h.state.value.sessionPersisted)
        val run = assertNotNull(h.state.value.lastSession)
        assertEquals(8, run.trials.size)
        // The recorder failure was OBSERVED (diagnosed), not thrown:
        assertTrue(diag.ops.isNotEmpty(), "storage failure must be diagnosed, not swallowed silently")
        coroutineContext.cancelChildren()
    }

    @Test
    fun recorderWiring_leavesScoringAndPersistenceIntact() = runTest {
        val store = InMemorySpeedMeasurementStore()
        val h = holder(store, this, FakeRecorder(), { "s" }, blockCount = 3, trialsPerBlock = 4)
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        val run = assertNotNull(h.state.value.lastSession)
        assertEquals(3, run.blocks.size)
        assertEquals(12, run.trials.size)
        assertTrue(run.trials.all { it.isCorrect }, "fast correct responses unchanged by the recorder")
        assertNotNull(store.loadRun(run.session.id))
        coroutineContext.cancelChildren()
    }

    @Test
    fun productionFactory_persistsFinalizedSessionRecord_toSessionsStore() = runTest {
        // Exercises the REAL production wiring: speedSessionRecorder (the factory the screen injects) over a
        // real SecureSessionRecordStore backed by an in-memory secure store (the mindboost_sessions namespace).
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = speedSessionRecorder(
            sessionsBacking,
            InMemorySecureKeyValueStore(),
            EpochClock { 1_000L },
            subject = "u1",
        )
        val h = holder(
            InMemorySpeedMeasurementStore(), this, recorder, RandomSessionIdGenerator(),
            blockCount = 2, trialsPerBlock = 4,
        )
        drive(h, rtNanos = 200 * ms)
        advanceUntilIdle()
        val recorded = testSessionRecordStore(sessionsBacking, subject = "u1").all()
        assertEquals(1, recorded.size, "exactly one SessionRecord persisted")
        val record = recorded.single()
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.SPEED, record.block)
        assertEquals(8, record.completedTrials) // 2 blocks * 4 trials
        coroutineContext.cancelChildren()
    }

    private companion object {
        const val MAX_STEPS = 2000
    }
}

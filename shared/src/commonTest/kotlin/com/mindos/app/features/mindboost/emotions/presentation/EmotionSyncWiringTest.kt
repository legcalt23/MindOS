@file:Suppress("MagicNumber", "MaxLineLength") // Fixtures + dense graph wiring under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalytics
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalyticsEvent
import com.mindos.app.features.mindboost.emotions.analytics.NoOpEmotionAnalytics
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionCloudSyncSink
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSyncStatus
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MC8u-E §3/§6/§7 — the cloud-sync seam is now WIRED (not dangling): with a real EmotionCloudSyncSink the durable
 * summary is pushed and its syncStatus flips from the result; a failed push emits `emotion_sync_failed` and NEVER
 * deletes local data; the charter default no-op never pushes (stays LOCAL_ONLY).
 */
class EmotionSyncWiringTest {

    private class Recording : EmotionAnalytics {
        val events = mutableListOf<EmotionAnalyticsEvent>()
        override fun log(event: EmotionAnalyticsEvent) { events += event }
    }

    private class FakeCloudSync(private val result: String?, private val throwing: Boolean = false) : EmotionCloudSyncSink {
        var pushCount = 0
        override suspend fun push(userId: String, summary: EmotionSessionSummaryDto): String {
            pushCount++
            if (throwing) error("network down")
            return result ?: EmotionSyncStatus.SYNC_FAILED
        }
    }

    private fun graph(repo: LocalEmotionRepository, cloudSync: EmotionCloudSyncSink, analytics: EmotionAnalytics = NoOpEmotionAnalytics): EmotionGraph {
        var nowMs = 1_000L
        val clock = EpochClock { nowMs += 50; nowMs }
        return EmotionGraph(
            generators = EmotionExerciseGenerators.registry,
            metricsEngine = DefaultEmotionMetricsEngine(),
            assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
            adaptiveEngine = DefaultEmotionAdaptiveEngine(),
            overloadDetector = DefaultOverloadDetector(),
            pauseGovernor = PauseGovernor(),
            feedbackBuilder = EmotionFeedbackBuilder(),
            repository = repo,
            epochClock = clock,
            monotonicClock = SystemMonotonicClock(),
            analytics = analytics,
            cloudSync = cloudSync,
        )
    }

    private fun TestScope.driveToTerminal(holder: EmotionSessionStateHolder) {
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))
        repeat(12) {
            advanceUntilIdle()
            when (holder.uiState) {
                is EmotionSessionUiState.MidCheckIn -> holder.submitCheckInIntensity(2)
                is EmotionSessionUiState.MicroPause -> holder.onResumeFromMicroPause()
                is EmotionSessionUiState.SelfReportAfter -> holder.submitSelfReport(SelfReport(3, 3, 2))
                else -> Unit
            }
        }
        advanceUntilIdle()
    }

    @Test
    fun successful_push_flips_persisted_syncStatus_to_synced() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val sink = FakeCloudSync(EmotionSyncStatus.SYNCED)
        val holder = EmotionSessionStateHolder(graph(repo, sink), EmotionExerciseId.EM1_PAUSA, "u1", scope = this, seed = 1L)
        driveToTerminal(holder)

        val saved = repo.recentSummaries("u1")
        assertTrue(saved.isNotEmpty(), "the session must persist")
        assertTrue(sink.pushCount >= 1, "a durable summary must be pushed")
        assertEquals(EmotionSyncStatus.SYNCED, saved.first().syncStatus, "syncStatus flips from the push result")
        coroutineContext.cancelChildren()
    }

    @Test
    fun failed_push_emits_sync_failed_and_never_deletes_local() = runTest {
        val analytics = Recording()
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val sink = FakeCloudSync(result = null, throwing = true)
        val holder = EmotionSessionStateHolder(graph(repo, sink, analytics), EmotionExerciseId.EM1_PAUSA, "u1", scope = this, seed = 1L)
        driveToTerminal(holder)

        assertTrue(analytics.events.any { it is EmotionAnalyticsEvent.SyncFailed }, "a sync failure must be observable; got ${analytics.events}")
        val saved = repo.recentSummaries("u1")
        assertTrue(saved.isNotEmpty(), "sync failure must NEVER delete local data")
        assertEquals(EmotionSyncStatus.LOCAL_ONLY, saved.first().syncStatus, "on failure the local record keeps its LOCAL_ONLY posture")
        coroutineContext.cancelChildren()
    }

    @Test
    fun charter_default_noop_never_pushes_and_stays_local_only() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        // The production default (com.mindos...NoOpEmotionCloudSync) — reached via the real EmotionGraph default.
        val holder = EmotionSessionStateHolder(
            EmotionGraph(
                generators = EmotionExerciseGenerators.registry,
                metricsEngine = DefaultEmotionMetricsEngine(),
                assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
                adaptiveEngine = DefaultEmotionAdaptiveEngine(),
                overloadDetector = DefaultOverloadDetector(),
                pauseGovernor = PauseGovernor(),
                feedbackBuilder = EmotionFeedbackBuilder(),
                repository = repo,
                epochClock = EpochClock { 1_000L },
                monotonicClock = SystemMonotonicClock(),
            ),
            EmotionExerciseId.EM1_PAUSA, "u1", scope = this, seed = 1L,
        )
        driveToTerminal(holder)

        assertEquals(EmotionSyncStatus.LOCAL_ONLY, repo.recentSummaries("u1").first().syncStatus, "local-first default never leaves LOCAL_ONLY")
        coroutineContext.cancelChildren()
    }
}

package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanQualityDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanTelemetrySummaryV1
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanTrialOutcome
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC9-C-R2 · E5 — historical telemetry persistence: store round-trip, per-user deletion, and summary serialization. */
class VisualScanTelemetryPersistenceTest {

    private fun record(sessionId: String, userId: String) = ExerciseTelemetryRecord(
        sessionId = sessionId,
        userId = userId,
        exerciseId = "E5",
        telemetryVersion = "1.1.0-mc9c-r2",
        summaryJson = "{\"k\":1}",
        createdAtEpochMs = 123L,
    )

    @Test
    fun secure_store_round_trips_and_isolates_users() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("s1", "u1"))
        store.save(record("s2", "u2"))
        assertEquals("E5", store.forSession("s1")?.exerciseId)
        assertNull(store.forSession("nope"))
        assertEquals(1, store.forUser("u1").size)
        assertEquals(1, store.deleteUserData("u1"))
        assertEquals(0, store.forUser("u1").size, "u1 telemetry erased")
        assertEquals(1, store.forUser("u2").size, "u2 telemetry intact")
    }

    @Test
    fun repeated_save_for_the_same_session_overwrites_not_duplicates() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("s1", "u1"))
        store.save(record("s1", "u1").copy(telemetryVersion = "v2"))
        assertEquals(1, store.forUser("u1").size)
        assertEquals("v2", store.forSession("s1")?.telemetryVersion)
    }

    @Test
    fun in_memory_store_matches_the_secure_contract() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        store.save(record("s1", "u1"))
        assertEquals("E5", store.forSession("s1")?.exerciseId)
        assertEquals(1, store.deleteUserData("u1"))
        assertEquals(0, store.forUser("u1").size)
    }

    @Test
    fun the_summary_serializes_and_round_trips_with_measured_and_unavailable_signals() = runTest {
        val outcomes = List(8) {
            VisualScanTrialOutcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400_000_000L, correct = true, anticipation = false)
        }
        val summary = VisualScanRunSummary.from(outcomes, prematureTaps = 1)
        val metrics = VisualScanMetricsMapper.metrics(summary)
        val diagnostics = VisualScanQualityDiagnostics.evaluate(outcomes)
        val v1 = VisualScanTelemetrySummaryV1.of("s1", "E5", metrics, summary, diagnostics, createdAtEpochMs = 123L)

        val json = SpeedJson.instance.encodeToString(v1)
        val back = SpeedJson.instance.decodeFromString<VisualScanTelemetrySummaryV1>(json)
        assertEquals(v1, back, "the sealed VisualScanSignal (Measured/Unavailable) must round-trip")
        assertTrue("similarityCost" in back.unavailableSignals, "structurally-unavailable signals are listed")
        assertTrue("omissionPressureIndex" in back.unavailableSignals)
    }
}

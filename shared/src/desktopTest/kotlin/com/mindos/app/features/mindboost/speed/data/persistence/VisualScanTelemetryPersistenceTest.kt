@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.score.VISUAL_SCAN_TELEMETRY_SUMMARY_VERSION
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanQualityDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanTelemetrySummaryV1
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanTrialOutcome
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MC9-C-R2/R3 · E5 — historical telemetry persistence: store round-trip, per-user privacy, deterministic history
 * order, sync-status defaults, version separation, typed E5 helpers, idempotency, and namespace-wide erasure.
 */
class VisualScanTelemetryPersistenceTest {

    private fun record(
        sessionId: String,
        userId: String,
        exerciseId: String = "E5",
        createdAtEpochMs: Long = 123L,
        summaryJson: String = "{\"k\":1}",
    ) = ExerciseTelemetryRecord(
        sessionId = sessionId,
        userId = userId,
        exerciseId = exerciseId,
        telemetryVersion = "1.1.0-mc9c-r2",
        summaryJson = summaryJson,
        createdAtEpochMs = createdAtEpochMs,
    )

    private fun e5Summary(sessionId: String, createdAtEpochMs: Long = 123L): VisualScanTelemetrySummaryV1 {
        val outcomes = List(8) {
            VisualScanTrialOutcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400_000_000L, correct = true, anticipation = false)
        }
        val summary = VisualScanRunSummary.from(outcomes, prematureTaps = 1)
        val metrics = VisualScanMetricsMapper.metrics(summary)
        val diagnostics = VisualScanQualityDiagnostics.evaluate(outcomes)
        return VisualScanTelemetrySummaryV1.of(sessionId, "E5", metrics, summary, diagnostics, createdAtEpochMs)
    }

    // --- store contract ---

    @Test
    fun secure_store_round_trips_and_isolates_users() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("s1", "u1"))
        store.save(record("s2", "u2"))
        assertEquals("E5", store.forSession("u1", "s1")?.exerciseId)
        assertNull(store.forSession("u1", "nope"))
        assertEquals(1, store.forUser("u1").size)
        assertEquals(1, store.deleteUserData("u1"))
        assertEquals(0, store.forUser("u1").size, "u1 telemetry erased")
        assertEquals(1, store.forUser("u2").size, "u2 telemetry intact")
    }

    // --- §2 privacy: same sessionId across users never cross-contaminates ---

    @Test
    fun for_session_requires_the_owner_user_and_never_returns_another_users_record() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("shared-session", "userA").copy(summaryJson = "{\"who\":\"A\"}"))
        store.save(record("shared-session", "userB").copy(summaryJson = "{\"who\":\"B\"}"))
        assertEquals("{\"who\":\"A\"}", store.forSession("userA", "shared-session")?.summaryJson)
        assertEquals("{\"who\":\"B\"}", store.forSession("userB", "shared-session")?.summaryJson)
        assertEquals(listOf("userA"), store.forUser("userA").map { it.userId })
        assertEquals(listOf("userB"), store.forUser("userB").map { it.userId })
    }

    @Test
    fun in_memory_store_also_isolates_the_same_session_across_users() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        store.save(record("shared", "userA"))
        store.save(record("shared", "userB"))
        assertEquals("userA", store.forSession("userA", "shared")?.userId)
        assertEquals("userB", store.forSession("userB", "shared")?.userId)
    }

    // --- §3 deterministic history order: most recent first ---

    @Test
    fun for_user_returns_history_most_recent_first_in_both_stores() = runTest {
        val secure = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val memory = InMemoryExerciseTelemetryStore()
        listOf(
            record("s-mid", "u1", createdAtEpochMs = 200L),
            record("s-old", "u1", createdAtEpochMs = 100L),
            record("s-new", "u1", createdAtEpochMs = 300L),
        ).forEach { secure.save(it); memory.save(it) }
        val expected = listOf("s-new", "s-mid", "s-old")
        assertEquals(expected, secure.forUser("u1").map { it.sessionId }, "secure: newest first")
        assertEquals(expected, memory.forUser("u1").map { it.sessionId }, "in-memory matches secure")
    }

    // --- §4 sync-status + schema defaults ---

    @Test
    fun a_new_record_defaults_to_local_only_and_stamps_schema_version() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("s1", "u1", createdAtEpochMs = 500L))
        val back = store.forSession("u1", "s1")!!
        assertEquals(TelemetrySyncStatus.LOCAL_ONLY, back.syncStatus, "MVP local ⇒ LOCAL_ONLY")
        assertEquals(EXERCISE_TELEMETRY_RECORD_VERSION, back.schemaVersion)
        assertEquals(500L, back.updatedAtEpochMs, "updatedAt defaults to createdAt until a sync layer touches it")
    }

    // --- §5 version separation: record schema vs summary schema vs metrics formulas ---

    @Test
    fun summary_round_trips_with_its_own_version_distinct_from_metrics_version() = runTest {
        val v1 = e5Summary("s1")
        val json = SpeedJson.instance.encodeToString(v1)
        val back = SpeedJson.instance.decodeFromString<VisualScanTelemetrySummaryV1>(json)
        assertEquals(v1, back, "the sealed VisualScanSignal (Measured/Unavailable) + summaryVersion must round-trip")
        assertEquals(VISUAL_SCAN_TELEMETRY_SUMMARY_VERSION, back.summaryVersion)
        assertNotEquals(back.summaryVersion, back.metricsVersion, "summaryVersion (schema) != metricsVersion (formulas)")
        assertTrue("similarityCost" in back.unavailableSignals, "structurally-unavailable signals are listed")
        assertTrue("omissionPressureIndex" in back.unavailableSignals)
    }

    @Test
    fun typed_save_stamps_the_wrapper_with_the_summary_schema_version_not_the_formula_version() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val summary = e5Summary("s1")
        store.saveVisualScanSummary("u1", summary)
        val record = store.forSession("u1", "s1")!!
        // telemetryVersion mirrors the summary SCHEMA/format, never the mapper FORMULAS.
        assertEquals(summary.summaryVersion, record.telemetryVersion)
        assertNotEquals(summary.metricsVersion, record.telemetryVersion, "wrapper format != formula version")
        // The mapper formula version is NOT lost: it stays inside the persisted summary JSON.
        assertEquals(summary.metricsVersion, store.visualScanSummaryForSession("u1", "s1")?.metricsVersion)
    }

    // --- §6 typed E5 helpers ---

    @Test
    fun typed_helper_saves_and_reads_an_e5_summary() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val summary = e5Summary("s1")
        store.saveVisualScanSummary("u1", summary)
        assertEquals(summary, store.visualScanSummaryForSession("u1", "s1"))
        assertEquals(listOf(summary), store.visualScanSummariesForUser("u1"))
    }

    @Test
    fun a_non_e5_record_is_not_parsed_as_an_e5_summary() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        // A valid E5 payload but wrongly tagged as E1 must NOT be surfaced by the E5 typed reader.
        val payload = SpeedJson.instance.encodeToString(e5Summary("s1"))
        store.save(record("s1", "u1", exerciseId = "E1", summaryJson = payload))
        assertNull(store.visualScanSummaryForSession("u1", "s1"), "exercise mismatch ⇒ not an E5 summary")
        assertEquals(emptyList(), store.visualScanSummariesForUser("u1"))
    }

    @Test
    fun corrupt_json_is_skipped_safely_and_never_crashes_the_session() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("bad", "u1", summaryJson = "{ not valid json"))
        store.saveVisualScanSummary("u1", e5Summary("good"))
        assertNull(store.visualScanSummaryForSession("u1", "bad"), "corrupt JSON ⇒ null, no throw")
        assertEquals(listOf("good"), store.visualScanSummariesForUser("u1").map { it.sessionId }, "the corrupt one is skipped")
    }

    @Test
    fun typed_history_preserves_the_most_recent_first_order() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        store.saveVisualScanSummary("u1", e5Summary("s-old", createdAtEpochMs = 100L))
        store.saveVisualScanSummary("u1", e5Summary("s-new", createdAtEpochMs = 300L))
        assertEquals(listOf("s-new", "s-old"), store.visualScanSummariesForUser("u1").map { it.sessionId })
    }

    // --- §7 idempotency ---

    @Test
    fun repeated_save_for_the_same_session_overwrites_not_duplicates() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(record("s1", "u1"))
        store.save(record("s1", "u1").copy(telemetryVersion = "v2"))
        assertEquals(1, store.forUser("u1").size)
        assertEquals("v2", store.forSession("u1", "s1")?.telemetryVersion)
    }

    @Test
    fun saving_a_distinct_session_creates_a_second_record() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.saveVisualScanSummary("u1", e5Summary("s1"))
        store.saveVisualScanSummary("u1", e5Summary("s2"))
        assertEquals(2, store.forUser("u1").size)
    }

    @Test
    fun typed_save_twice_for_the_same_session_stays_a_single_record() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val summary = e5Summary("s1")
        store.saveVisualScanSummary("u1", summary)
        store.saveVisualScanSummary("u1", summary)
        assertEquals(1, store.forUser("u1").size, "idempotent by userId::sessionId")
    }

    // --- in-memory contract parity ---

    @Test
    fun in_memory_store_matches_the_secure_contract() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        store.save(record("s1", "u1"))
        assertEquals("E5", store.forSession("u1", "s1")?.exerciseId)
        assertEquals(1, store.deleteUserData("u1"))
        assertEquals(0, store.forUser("u1").size)
    }

    // --- §9 account erasure: a namespace-wide wipe removes telemetry too ---

    @Test
    fun clearing_the_shared_namespace_also_erases_telemetry() = runTest {
        // Telemetry and MeasurementRun live in the SAME secure store (namespace `mindboost_speed`), so the
        // account-deletion eraser's clearNamespace() wipes telemetry as well. This proves the coverage locally.
        val secure = InMemorySecureKeyValueStore()
        val telemetry = SecureExerciseTelemetryStore(secure)
        telemetry.save(record("s1", "u1"))
        assertEquals(1, telemetry.forUser("u1").size)
        secure.clearNamespace()
        assertEquals(0, telemetry.forUser("u1").size, "namespace wipe (account deletion) removes tele: records")
    }
}

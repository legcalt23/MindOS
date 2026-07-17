@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOutcome
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoQualityDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoResponse
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScorer
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoSignal
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoStimulus
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTelemetrySummaryV1
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC9-D · E8 — typed telemetry persistence: round-trip, exercise/JSON guards, version wiring, idempotency. */
class GoNoGoTelemetryPersistenceTest {

    private fun stim(type: GoNoGoTrialType, pos: Int) = GoNoGoStimulus(
        trialType = type,
        stimulusId = "id-$pos",
        stimulusFamily = if (type == GoNoGoTrialType.GO) "go" else "nogo",
        deadlineMs = 500L,
        foreperiodMs = 600L,
        difficultyLevel = 0,
        runPosition = pos,
        trialConfigVersion = "test",
    )

    private fun e8Summary(sessionId: String, createdAtEpochMs: Long = 123L): GoNoGoTelemetrySummaryV1 {
        val outcomes = (0 until 6).map { GoNoGoOutcome.resolve(stim(GoNoGoTrialType.GO, it), GoNoGoResponse(false, 300L)) } +
            (6 until 10).map { GoNoGoOutcome.resolve(stim(GoNoGoTrialType.NO_GO, it), GoNoGoResponse(false, null)) }
        val summary = GoNoGoRunSummary.from(outcomes)
        val metrics = GoNoGoMetricsMapper.metrics(summary)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes)
        val score = GoNoGoScorer.score(metrics, diagnostics)
        return GoNoGoTelemetrySummaryV1.of(sessionId, "E8", metrics, summary, diagnostics, score, createdAtEpochMs)
    }

    @Test
    fun the_persisted_summary_carries_the_scored_result_and_raw_counts_not_only_rates() = runTest {
        // MC9-D-R3 persistence completeness: the run is reconstructible (score + counts + RT distribution), not
        // just a rate projection. A strong 6-GO/4-NO-GO run scores high and stays CLEAN/eligible.
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.saveGoNoGoSummary("u1", e8Summary("s1"))
        val read = assertNotNull(store.goNoGoSummaryForSession("u1", "s1"))
        assertNotNull(read.preliminaryScore, "the scored index is persisted, not recomputed on the fly")
        assertEquals("STRONG_CONTROL", read.scoreCategory)
        assertEquals(6, read.goCorrect)
        assertEquals(4, read.noGoCorrect)
        assertEquals(0, read.commissions)
        assertTrue(read.profileEligible, "a clean strong run is profile eligible")
        assertTrue(!read.calibrationEligible, "but only 4 NO-GO is below the confidence bar for calibration")
        assertEquals("CLEAN", read.timingIntegrityLevel)
        assertNotNull(read.correctGoRtMinMs as? GoNoGoSignal.Measured, "RT min is persisted as a real measurement")
    }

    @Test
    fun typed_helper_saves_and_reads_an_e8_summary() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val summary = e8Summary("s1")
        store.saveGoNoGoSummary("u1", summary)
        assertEquals(summary, store.goNoGoSummaryForSession("u1", "s1"))
        assertEquals(listOf(summary), store.goNoGoSummariesForUser("u1"))
    }

    @Test
    fun a_non_e8_record_is_not_parsed_as_an_e8_summary() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val payload = SpeedJson.instance.encodeToString(e8Summary("s1"))
        store.save(
            ExerciseTelemetryRecord(
                sessionId = "s1", userId = "u1", exerciseId = "E5",
                telemetryVersion = "x", summaryJson = payload, createdAtEpochMs = 1L,
            ),
        )
        assertNull(store.goNoGoSummaryForSession("u1", "s1"), "exercise mismatch ⇒ not an E8 summary")
        assertEquals(emptyList(), store.goNoGoSummariesForUser("u1"))
    }

    @Test
    fun corrupt_json_is_skipped_safely() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.save(
            ExerciseTelemetryRecord(
                sessionId = "bad", userId = "u1", exerciseId = "E8",
                telemetryVersion = "x", summaryJson = "{ not valid json", createdAtEpochMs = 1L,
            ),
        )
        assertNull(store.goNoGoSummaryForSession("u1", "bad"), "corrupt JSON ⇒ null, no throw")
    }

    @Test
    fun the_wrapper_version_mirrors_the_summary_schema_not_the_formulas() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        val summary = e8Summary("s1").copy(
            summaryVersion = "summary-schema-test-only",
            metricsVersion = "formula-version-test-only",
        )
        store.saveGoNoGoSummary("u1", summary)
        val record = store.forSession("u1", "s1")!!
        assertEquals("summary-schema-test-only", record.telemetryVersion)
        assertNotEquals(summary.metricsVersion, record.telemetryVersion, "wrapper format != formula version")
        assertEquals("formula-version-test-only", store.goNoGoSummaryForSession("u1", "s1")?.metricsVersion)
    }

    @Test
    fun typed_save_is_idempotent_and_isolates_users() = runTest {
        val store = SecureExerciseTelemetryStore(InMemorySecureKeyValueStore())
        store.saveGoNoGoSummary("u1", e8Summary("s1"))
        store.saveGoNoGoSummary("u1", e8Summary("s1"))
        store.saveGoNoGoSummary("u2", e8Summary("s1"))
        assertEquals(1, store.forUser("u1").size, "idempotent by userId::sessionId")
        assertEquals(1, store.forUser("u2").size, "another user is isolated")
    }
}

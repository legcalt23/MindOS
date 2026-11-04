@file:Suppress("MagicNumber") // Session fixtures and cell counts ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.PressureCellsDto
import com.mindos.app.features.mindboost.emotions.domain.metrics.RobustnessGap
import com.mindos.app.features.mindboost.emotions.domain.metrics.RobustnessVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 3 · R3 · P4 — the robustness index is POOLED at the aggregate, which is the only place it can exist.
 *
 * The pair that carries this suite is the last two tests: four sessions of identical accuracies produce a
 * verdict that one session of the same accuracies cannot. If a single session ever starts answering, the index
 * is being published on a sample the ratified floor does not allow.
 */
class EmotionAggregateRobustnessTest {

    private fun session(startedAt: Long, cells: PressureCellsDto?) = EmotionSessionSummaryDto(
        exerciseId = "EM3_SEPARADOR",
        startedAtMillis = startedAt,
        durationMs = EmotionTerminalSemantics.MIN_SESSION_DURATION_MS,
        difficultySummary = DifficultySummaryDto("CATEGORY_AMBIGUITY", 0.3, 1),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = "sess_$startedAt",
        endReason = "COMPLETED",
        accuracy = 0.8,
        pressureCells = cells,
    )

    /** One session of the live geometry: block 0 calm (10 trials), blocks 1 and 2 under pressure (20). */
    private fun liveSession(startedAt: Long) = session(
        startedAt,
        PressureCellsDto(calmCorrect = 9, calmTotal = 10, pressureCorrect = 14, pressureTotal = 20),
    )

    @Test
    fun a_history_with_no_pressure_reports_that_and_not_an_index() {
        val aggregate = assertNotNull(EmotionAggregateFactory.fromSummaries(listOf(session(1, cells = null)), now = 10))

        assertEquals(RobustnessGap.NO_PRESSURE_SERVED.name, aggregate.robustness.gap)
        assertNull(aggregate.robustness.index, "an index appeared without a single pressure trial behind it")
        assertNull(aggregate.robustness.verdict)
    }

    /**
     * A calm-only history is a different fact from a locked one, and it must not read as a user who is done:
     * cells were recorded, pressure simply never ran.
     */
    @Test
    fun a_calm_only_history_still_reports_that_pressure_was_never_served() {
        val calmOnly = session(
            1,
            PressureCellsDto(calmCorrect = 27, calmTotal = 30, pressureCorrect = 0, pressureTotal = 0),
        )
        val aggregate = assertNotNull(EmotionAggregateFactory.fromSummaries(listOf(calmOnly), now = 10))

        assertEquals(RobustnessGap.NO_PRESSURE_SERVED.name, aggregate.robustness.gap)
    }

    @Test
    fun one_pressure_session_is_short_of_the_floor_and_says_so() {
        val aggregate = assertNotNull(EmotionAggregateFactory.fromSummaries(listOf(liveSession(1)), now = 10))

        assertEquals(RobustnessGap.TOO_FEW_TRIALS.name, aggregate.robustness.gap)
        assertNull(aggregate.robustness.index, "one session published an index the sampling floor forbids")
    }

    /**
     * The discriminating half, and the reason the block was not grown: the SAME per-session accuracies, pooled
     * across four sessions, do produce a reading. Nothing about the effect changed between this test and the one
     * above — only how much of it had been seen.
     */
    @Test
    fun four_pressure_sessions_pool_into_a_reading_the_single_session_could_not_support() {
        val aggregate = assertNotNull(
            EmotionAggregateFactory.fromSummaries((1L..4L).map { liveSession(it) }, now = 10),
        )
        val robustness = aggregate.robustness

        assertNull(robustness.gap, "a pooled history should measure, not refuse")
        assertEquals(0.70 / 0.90, assertNotNull(robustness.index), 1e-12)
        assertEquals(RobustnessVerdict.PRESSURE_COSTS_ACCURACY.name, robustness.verdict)
        assertTrue(
            assertNotNull(robustness.intervalHigh) < 1.0,
            "the interval must clear 1 before the aggregate claims pressure costs anything",
        )
        assertTrue(assertNotNull(robustness.intervalLow) > 0.0)
    }

    /** Sessions that carry no cells must not dilute the pool — absence is not a session of zeros. */
    @Test
    fun sessions_without_cells_are_skipped_rather_than_counted_as_empty_ones() {
        val mixed = listOf(liveSession(1), session(2, cells = null), liveSession(3), session(4, cells = null)) +
            (5L..6L).map { liveSession(it) }
        val pooledOnly = assertNotNull(
            EmotionAggregateFactory.fromSummaries((1L..4L).map { liveSession(it) }, now = 10),
        )
        val withGaps = assertNotNull(EmotionAggregateFactory.fromSummaries(mixed, now = 10))

        assertEquals(pooledOnly.robustness.index, withGaps.robustness.index, "null-cell sessions moved the index")
    }
}

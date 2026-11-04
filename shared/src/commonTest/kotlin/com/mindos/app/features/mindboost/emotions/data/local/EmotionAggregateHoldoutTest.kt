@file:Suppress("MagicNumber") // Session fixtures and probe sizes ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.HoldoutProbeDto
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionHoldoutProbe
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionHoldoutEvidence
import com.mindos.app.features.mindboost.emotions.domain.model.TransferConfidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Ola 4 · P4 — the transfer claim in the aggregate is DERIVED from the held-out probes, not asserted.
 *
 * The third test here is the one that matters: without it, every assertion below would still pass against the
 * old hardcoded `NOT_DEMONSTRATED` literal, and this suite would prove nothing.
 */
class EmotionAggregateHoldoutTest {

    private fun session(
        startedAt: Long,
        accuracy: Double,
        probe: HoldoutProbeDto?,
    ) = EmotionSessionSummaryDto(
        exerciseId = "EM3_SEPARADOR",
        startedAtMillis = startedAt,
        durationMs = EmotionTerminalSemantics.MIN_SESSION_DURATION_MS,
        difficultySummary = DifficultySummaryDto("CATEGORY_AMBIGUITY", 0.3, 1),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = "sess_$startedAt",
        endReason = "COMPLETED",
        accuracy = accuracy,
        holdoutProbe = probe,
    )

    @Test
    fun with_no_probe_the_claim_stays_at_not_demonstrated() {
        val aggregate = EmotionAggregateFactory.fromSummaries(listOf(session(1, 0.9, probe = null)), now = 10)
        assertEquals(TransferConfidence.NOT_DEMONSTRATED.name, assertNotNull(aggregate).transferConfidence)
    }

    @Test
    fun probes_the_real_bank_can_serve_never_earn_a_transfer_claim_even_at_perfect_accuracy() {
        // Real-sized probes (4 items), answered perfectly, with trained accuracy climbing. The unseen-item
        // performance is as good as it can get and the claim STILL does not move, because the probes are too
        // small to show whether the improvement carried over.
        val summaries = (1..EmotionHoldoutProbe.MAX_PROBES).map { i ->
            session(
                startedAt = i.toLong(),
                accuracy = 0.5 + i * 0.1,
                probe = HoldoutProbeDto(
                    servedItemIds = List(EmotionHoldoutProbe.PROBE_SIZE) { "item_${i}_$it" },
                    correct = EmotionHoldoutProbe.PROBE_SIZE,
                    total = EmotionHoldoutProbe.PROBE_SIZE,
                ),
            )
        }
        val aggregate = assertNotNull(EmotionAggregateFactory.fromSummaries(summaries, now = 100))
        assertEquals(TransferConfidence.NOT_DEMONSTRATED.name, aggregate.transferConfidence)
    }

    @Test
    fun a_bank_large_enough_to_compare_probes_would_move_the_claim() {
        // The discriminating half. Probes of MIN_ITEMS_PER_PROBE_FOR_CHANGE items where the unseen items improve
        // as much as the trained ones DO lift the ceiling — proving this value is computed from the probes and
        // not a constant. Unreachable with the authored bank today; reachable the moment the bank grows.
        val summaries = listOf(
            session(1, accuracy = 0.50, probe = HoldoutProbeDto(List(8) { "a$it" }, correct = 4, total = 8)),
            session(2, accuracy = 0.875, probe = HoldoutProbeDto(List(8) { "b$it" }, correct = 7, total = 8)),
        )
        val aggregate = assertNotNull(EmotionAggregateFactory.fromSummaries(summaries, now = 100))
        assertEquals(TransferConfidence.LIMITED.name, aggregate.transferConfidence)
        assertEquals(
            8,
            EmotionHoldoutEvidence.MIN_ITEMS_PER_PROBE_FOR_CHANGE,
            "this fixture is sized against the comparison floor; keep them in step",
        )
    }
}

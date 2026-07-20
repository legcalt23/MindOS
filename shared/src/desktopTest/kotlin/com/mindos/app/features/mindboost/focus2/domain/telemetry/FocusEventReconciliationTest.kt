package com.mindos.app.features.mindboost.focus2.domain.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S1 — TOTALITY of the event reconciliation.
 *
 * The failure this guards against is not a wrong mapping; it is an event nobody mapped. An unmapped event is
 * how two vocabularies quietly become three. So the tests assert coverage in BOTH directions and refuse to let
 * either side grow silently.
 */
class FocusEventReconciliationTest {

    @Test
    fun the_two_vocabularies_are_the_sizes_the_reconciliation_claims() {
        assertEquals(13, PackEvent.entries.size, "The research pack proposes 13 events.")
        assertEquals(16, CanonEvent.entries.size, "The canonical standard has 16.")
    }

    @Test
    fun every_pack_event_has_a_disposition() {
        PackEvent.entries.forEach { event ->
            assertTrue(
                FocusEventMapping.dispositions.containsKey(event),
                "$event has no disposition. An event with no stated outcome is an omission, not a decision.",
            )
        }
        assertEquals(
            PackEvent.entries.toSet(),
            FocusEventMapping.dispositions.keys,
            "The table must cover the pack exactly — no gaps, and no entries for events that do not exist.",
        )
    }

    @Test
    fun every_canonical_event_is_either_produced_by_a_pack_event_or_explicitly_declared_canon_only() {
        val produced = FocusEventMapping.dispositions.values
            .flatMap { disposition ->
                when (disposition) {
                    is PackEventDisposition.MapsTo -> disposition.canon
                    is PackEventDisposition.Partial -> disposition.canon
                    is PackEventDisposition.LocalGranularity,
                    is PackEventDisposition.Redundant,
                    is PackEventDisposition.LocalOnly,
                    -> emptySet()
                }
            }
            .toSet()
        val declaredCanonOnly = FocusEventMapping.canonOnly.map { it.event }.toSet()

        assertEquals(
            CanonEvent.entries.toSet(),
            produced + declaredCanonOnly,
            "Every canonical event must be accounted for: produced by a pack event, or declared canon-only.",
        )
        assertTrue(
            (produced intersect declaredCanonOnly).isEmpty(),
            "An event cannot be both produced and canon-only — that is two answers to the same question.",
        )
    }

    @Test
    fun the_reconciliation_records_the_fourth_canon_only_event_the_plan_missed() {
        // A correction, pinned so it cannot be lost: the plan named three canon-only additions. SESSION_COMPLETED
        // is a fourth — nothing in the 13 says the session finished, which would make a completed session
        // indistinguishable from an abandoned one whose partial result was persisted.
        assertTrue(
            FocusEventMapping.canonOnly.any { it.event == CanonEvent.SESSION_COMPLETED },
            "SESSION_COMPLETED has no pack source and must be declared as such.",
        )
        assertEquals(4, FocusEventMapping.canonOnly.size)
    }

    @Test
    fun every_pack_event_maps_to_exactly_the_canonical_events_named_here() {
        // Hand-written from the reconciliation, NOT read from it. Coverage alone is permutation-insensitive:
        // swap SESSION_CREATED's target with TUTORIAL_RESULT's and the union is unchanged, so the totality
        // checks above would still pass while the vocabulary quietly means something else.
        val expected: Map<PackEvent, Set<CanonEvent>> = mapOf(
            PackEvent.SESSION_CREATED to setOf(CanonEvent.SESSION_STARTED),
            PackEvent.SESSION_COMMITTED to setOf(CanonEvent.RESULT_PERSISTED),
            PackEvent.TUTORIAL_RESULT to setOf(CanonEvent.TUTORIAL_COMPLETED),
            PackEvent.STIMULUS_PRESENTED to setOf(CanonEvent.TRIAL_PRESENTED),
            PackEvent.INPUT_RECEIVED to setOf(CanonEvent.RESPONSE_STARTED, CanonEvent.RESPONSE_SUBMITTED),
            PackEvent.TRIAL_EVALUATED to setOf(CanonEvent.TRIAL_RESOLVED),
            PackEvent.ADAPTATION_DECIDED to setOf(CanonEvent.DIFFICULTY_CHANGED),
            PackEvent.LIFECYCLE_CHANGED to setOf(
                CanonEvent.SESSION_PAUSED,
                CanonEvent.SESSION_RESUMED,
                CanonEvent.SESSION_INTERRUPTED,
            ),
            PackEvent.QUALITY_FLAGGED to setOf(CanonEvent.SESSION_INVALIDATED),
            // The four that produce no canonical event. Empty is a CLAIM here, not an absence of one.
            PackEvent.BLOCK_STARTED_ENDED to emptySet(),
            PackEvent.TRIAL_PLANNED to emptySet(),
            PackEvent.SCORE_MATERIALIZED to emptySet(),
            PackEvent.CONSENT_CHANGED to emptySet(),
        )

        val actual = PackEvent.entries.associateWith { event ->
            when (val disposition = FocusEventMapping.dispositions.getValue(event)) {
                is PackEventDisposition.MapsTo -> disposition.canon
                is PackEventDisposition.Partial -> disposition.canon
                is PackEventDisposition.LocalGranularity,
                is PackEventDisposition.Redundant,
                is PackEventDisposition.LocalOnly,
                -> emptySet()
            }
        }
        assertEquals(expected, actual, "A mapping changed. That is a contract change, not a refactor.")
    }

    @Test
    fun every_non_mapping_disposition_states_a_reason() {
        FocusEventMapping.dispositions.forEach { (event, disposition) ->
            val note = when (disposition) {
                is PackEventDisposition.MapsTo -> null
                is PackEventDisposition.Partial -> disposition.note
                is PackEventDisposition.LocalGranularity -> disposition.note
                is PackEventDisposition.Redundant -> disposition.note
                is PackEventDisposition.LocalOnly -> disposition.note
            }
            if (note != null) {
                assertTrue(note.length > MIN_REASON_LENGTH, "$event must state WHY, not just that it differs.")
            }
        }
        FocusEventMapping.canonOnly.forEach { canonOnly ->
            assertTrue(canonOnly.note.length > MIN_REASON_LENGTH, "${canonOnly.event} must state why it is unsourced.")
        }
    }

    @Test
    fun the_response_interval_survives_the_mapping() {
        // The single most consequential mapping decision: the pack has one input event, the canon has two. If
        // this ever collapses to one, the interval between response onset and submission — which IS the
        // measurement — stops being expressible.
        assertEquals(
            PackEventDisposition.MapsTo(setOf(CanonEvent.RESPONSE_STARTED, CanonEvent.RESPONSE_SUBMITTED)),
            FocusEventMapping.dispositions[PackEvent.INPUT_RECEIVED],
        )
    }

    @Test
    fun a_quality_flag_is_not_an_invalidation() {
        val disposition = FocusEventMapping.dispositions[PackEvent.QUALITY_FLAGGED]
        assertTrue(
            disposition is PackEventDisposition.Partial,
            "Mapping a flag straight onto SESSION_INVALIDATED would invalidate honest flagged sessions.",
        )
    }

    @Test
    fun the_trial_plan_is_not_persisted_twice() {
        assertTrue(
            FocusEventMapping.dispositions[PackEvent.TRIAL_PLANNED] is PackEventDisposition.Redundant,
            "The seed plus its RNG version re-derives the plan; a second copy can disagree with the first.",
        )
    }

    @Test
    fun the_table_is_versioned() {
        assertEquals(1, FocusEventReconciliation.VERSION)
    }

    private companion object {
        const val MIN_REASON_LENGTH = 40
    }
}

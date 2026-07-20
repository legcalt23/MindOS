package com.mindos.app.features.mindboost.focus2.domain.telemetry

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusAction
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusClock
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRunKind
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRunPlanner
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionRuntime
import com.mindos.app.features.mindboost.focus2.domain.runtime.VectorFilterTask
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S3 · tarea (8) — the emitted vocabulary matches the declared one.
 *
 * The important test here is the last one: it drives a session through EVERY lifecycle path and asserts that
 * the set of events actually emitted is exactly the set the catalog claims the reducer owns. Without it the
 * catalog would be a comment — a document that agrees with the code only until someone changes one of them.
 */
class FocusEventCatalogTest {

    private class SteadyClock : FocusClock {
        private var t = 0L
        override fun elapsedNanos(): Long = t.also { t += 100_000_000L }
        override fun wallClockMillis(): Long = 0L
    }

    private fun runtime(): FocusSessionRuntime {
        val run = FocusRunPlanner.prepare(
            FocusRunKind.MEASUREMENT,
            "s-events",
            SeedSource { SessionSeed(5L, RngVersion.V1) },
        )
        return FocusSessionRuntime(
            descriptor = run.descriptor,
            trials = run.trialFacts,
            taskSpec = VectorFilterTask.SPEC,
            clock = SteadyClock(),
        )
    }

    @Test
    fun every_canonical_event_has_a_declared_owner() {
        assertEquals(
            CanonEvent.entries.toSet(),
            FocusEventCatalog.ownership.keys,
            "an event with no entry is an undocumented decision",
        )
    }

    @Test
    fun every_event_that_is_not_emitted_says_why() {
        FocusEventCatalog.ownership
            .filterValues { it.layer == EmitterLayer.NOT_EMITTED }
            .forEach { (event, ownership) ->
                assertTrue(
                    ownership.note.length > MIN_REASON_LENGTH,
                    "$event is not emitted and must carry a reason, not a placeholder",
                )
            }
    }

    @Test
    fun a_local_only_tracer_never_claims_a_result_was_synced() {
        assertEquals(
            EmitterLayer.NOT_EMITTED,
            FocusEventCatalog.ownership.getValue(CanonEvent.RESULT_SYNCED).layer,
        )
        assertTrue(CanonEvent.RESULT_SYNCED !in FocusEventCatalog.applicable)
    }

    /**
     * Drives every lifecycle path and compares the emitted set against the declared one.
     *
     * Both directions are asserted. "Emitted but not declared" is a rogue event; "declared but never emitted"
     * is a catalog claiming coverage the code does not provide — and the second is the one a happy-path test
     * would miss.
     */
    @Test
    fun the_events_the_reducer_actually_emits_are_exactly_the_ones_it_claims() {
        val emitted = buildSet {
            // tutorial path
            runtime().let { r ->
                r.dispatch(FocusAction.START_TUTORIAL)
                r.dispatch(FocusAction.COMPLETE_TUTORIAL)
                addAll(r.emissions.map { it.event })
            }
            // ordinary session: present, answer, expire, pause, resume, complete
            runtime().let { r ->
                r.dispatch(FocusAction.START_SESSION)
                r.dispatch(FocusAction.PRESENT_TRIAL)
                r.respond(Response.LEFT)
                r.dispatch(FocusAction.PRESENT_TRIAL)
                r.dispatch(FocusAction.EXPIRE_WINDOW)
                r.dispatch(FocusAction.PAUSE)
                r.dispatch(FocusAction.RESUME)
                r.dispatch(FocusAction.COMPLETE)
                addAll(r.emissions.map { it.event })
            }
            // the two terminal paths that are not completion
            runtime().let { r ->
                r.dispatch(FocusAction.START_SESSION)
                r.dispatch(FocusAction.INTERRUPT)
                addAll(r.emissions.map { it.event })
            }
            runtime().let { r ->
                r.dispatch(FocusAction.START_SESSION)
                r.dispatch(FocusAction.INVALIDATE)
                addAll(r.emissions.map { it.event })
            }
        }

        assertEquals(FocusEventCatalog.reducerOwned, emitted)
    }

    @Test
    fun no_emission_carries_anything_beyond_a_trial_index() {
        val r = runtime()
        r.dispatch(FocusAction.START_SESSION)
        r.dispatch(FocusAction.PRESENT_TRIAL)
        r.respond(Response.LEFT)
        // The type makes this structural rather than aspirational: FocusEmission has exactly two fields, and a
        // response or a latency has nowhere to go. Asserting it keeps a future third field honest.
        assertTrue(r.emissions.all { it.trialIndex == null || it.trialIndex == 0 })
    }

    private companion object {
        const val MIN_REASON_LENGTH = 40
    }
}

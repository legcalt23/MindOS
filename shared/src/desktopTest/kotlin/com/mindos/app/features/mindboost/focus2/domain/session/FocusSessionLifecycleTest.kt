package com.mindos.app.features.mindboost.focus2.domain.session

import com.mindos.app.features.mindboost.core.session.SessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Focus V5 · S1 · ADR-FOCUS-SESSION-STATE — the lifecycle machine, covered EXHAUSTIVELY.
 *
 * The redteam requirement for this contract is that every transition, legal AND illegal, is pinned: an untested
 * transition is a fabricated state. So this file enumerates the full 7 x 8 cartesian product and checks each of
 * the 56 pairs against a table written INDEPENDENTLY here. Reading the expected table out of the implementation
 * would make the test a tautology that passes for any implementation, including a broken one.
 */
class FocusSessionLifecycleTest {

    /**
     * Hand-written expectation, deliberately NOT derived from `FocusSessionStateMachine`. Anything absent here
     * must be rejected by the machine. If someone adds a transition to production without adding it here, the
     * exhaustive test below fails — which is the point.
     */
    private val expectedLegal: Map<Pair<FocusSessionState, FocusSessionEvent>, FocusSessionState> = mapOf(
        (FocusSessionState.IDLE to FocusSessionEvent.TUTORIAL_STARTED) to FocusSessionState.TUTORIAL,
        (FocusSessionState.IDLE to FocusSessionEvent.SESSION_STARTED) to FocusSessionState.RUNNING,
        (FocusSessionState.TUTORIAL to FocusSessionEvent.TUTORIAL_COMPLETED) to FocusSessionState.IDLE,
        (FocusSessionState.TUTORIAL to FocusSessionEvent.SESSION_INTERRUPTED) to FocusSessionState.INTERRUPTED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_PAUSED) to FocusSessionState.PAUSED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_INTERRUPTED) to FocusSessionState.INTERRUPTED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_COMPLETED) to FocusSessionState.COMPLETED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_INVALIDATED) to FocusSessionState.INVALIDATED,
        (FocusSessionState.PAUSED to FocusSessionEvent.SESSION_RESUMED) to FocusSessionState.RUNNING,
        (FocusSessionState.PAUSED to FocusSessionEvent.SESSION_INTERRUPTED) to FocusSessionState.INTERRUPTED,
        (FocusSessionState.PAUSED to FocusSessionEvent.SESSION_INVALIDATED) to FocusSessionState.INVALIDATED,
        (FocusSessionState.COMPLETED to FocusSessionEvent.SESSION_INVALIDATED) to FocusSessionState.INVALIDATED,
    )

    @Test
    fun every_state_event_pair_is_pinned_as_either_a_named_target_or_a_typed_rejection() {
        var legal = 0
        var rejected = 0
        FocusSessionState.entries.forEach { state ->
            FocusSessionEvent.entries.forEach { event ->
                val actual = FocusSessionStateMachine.transition(state, event)
                val expected = expectedLegal[state to event]
                if (expected == null) {
                    assertEquals(
                        FocusSessionTransition.Rejected(state, event),
                        actual,
                        "$state + $event must be REJECTED — an unlisted transition is never silently legal.",
                    )
                    rejected++
                } else {
                    assertEquals(
                        FocusSessionTransition.Applied(expected),
                        actual,
                        "$state + $event must move to $expected.",
                    )
                    legal++
                }
            }
        }
        assertEquals(12, legal, "The machine has exactly 12 legal transitions.")
        assertEquals(44, rejected, "The remaining 44 of the 56 pairs must all be rejected.")
        assertEquals(56, legal + rejected, "7 states x 8 events must be covered with nothing skipped.")
    }

    @Test
    fun the_machine_declares_exactly_the_transitions_this_test_pins() {
        // Guards the reverse direction: production must not carry a legal transition this file never named.
        assertEquals(
            expectedLegal.keys,
            FocusSessionStateMachine.legalTransitions(),
            "Production and this test must agree on the legal set, in both directions.",
        )
    }

    @Test
    fun a_rejection_changes_nothing_and_says_what_was_refused() {
        val rejection = FocusSessionStateMachine.transition(
            FocusSessionState.IDLE,
            FocusSessionEvent.SESSION_COMPLETED,
        )
        assertEquals(FocusSessionTransition.Rejected(FocusSessionState.IDLE, FocusSessionEvent.SESSION_COMPLETED), rejection)
    }

    @Test
    fun a_session_cannot_be_completed_without_having_been_started() {
        // The single most consequential illegal path: a completed session that never ran would be evidence
        // fabricated out of nothing.
        listOf(FocusSessionState.IDLE, FocusSessionState.TUTORIAL, FocusSessionState.PAUSED).forEach { state ->
            assertTrue(
                FocusSessionStateMachine.transition(state, FocusSessionEvent.SESSION_COMPLETED)
                    is FocusSessionTransition.Rejected,
                "$state must not be completable — only a RUNNING session can complete.",
            )
        }
        assertEquals(
            FocusSessionTransition.Applied(FocusSessionState.COMPLETED),
            FocusSessionStateMachine.transition(FocusSessionState.RUNNING, FocusSessionEvent.SESSION_COMPLETED),
            "Discriminating half: RUNNING really can complete, or the assertions above prove nothing.",
        )
    }

    @Test
    fun a_tutorial_never_becomes_the_measured_session_by_completing() {
        assertEquals(
            FocusSessionTransition.Applied(FocusSessionState.IDLE),
            FocusSessionStateMachine.transition(FocusSessionState.TUTORIAL, FocusSessionEvent.TUTORIAL_COMPLETED),
            "Finishing the tutorial returns to IDLE; the session must then be STARTED explicitly.",
        )
    }

    @Test
    fun invalidation_is_the_only_way_out_of_a_terminal_state_and_only_from_COMPLETED() {
        val terminals = FocusSessionState.entries.filter { it.isTerminal }
        assertEquals(
            listOf(FocusSessionState.INTERRUPTED, FocusSessionState.COMPLETED, FocusSessionState.INVALIDATED),
            terminals,
        )
        val escapes = FocusSessionStateMachine.legalTransitions().filter { (state, _) -> state.isTerminal }
        assertEquals(
            listOf(FocusSessionState.COMPLETED to FocusSessionEvent.SESSION_INVALIDATED),
            escapes,
            "A terminal state may only move toward LESS credit, never back into a run.",
        )
    }

    @Test
    fun the_initial_state_is_idle() {
        assertEquals(FocusSessionState.IDLE, FocusSessionStateMachine.initial)
    }

    @Test
    fun the_global_status_mapping_is_total_and_never_falsifies_the_lifecycle() {
        assertNull(FocusSessionState.IDLE.toGlobalStatus(), "Nothing is recorded before a session exists.")
        assertNull(FocusSessionState.TUTORIAL.toGlobalStatus())
        assertEquals(SessionStatus.STARTED, FocusSessionState.RUNNING.toGlobalStatus())
        assertEquals(
            SessionStatus.STARTED,
            FocusSessionState.PAUSED.toGlobalStatus(),
            "A paused session is still a started one; PAUSED is runtime, not lifecycle.",
        )
        assertEquals(SessionStatus.FINALIZED, FocusSessionState.COMPLETED.toGlobalStatus())
        assertEquals(SessionStatus.ABANDONED, FocusSessionState.INTERRUPTED.toGlobalStatus())
        assertNull(
            FocusSessionState.INVALIDATED.toGlobalStatus(),
            "A completed-then-invalidated run really did finish: rewriting its lifecycle to ABANDONED would " +
                "falsify history to express a scientific verdict that belongs in the eligibility layer.",
        )
    }
}

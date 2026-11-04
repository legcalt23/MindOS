package com.mindos.app.features.mindboost.focus.domain.quality

import com.mindos.app.features.mindboost.core.antigaming.GamingAction
import com.mindos.app.features.mindboost.core.antigaming.GamingVerdict
import com.mindos.app.features.mindboost.core.lifecycle.ForegroundLoss
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.core.lifecycle.NotObservableReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** FOC-P0-05 + R15 · one test per reason, the precedence between them, and the discriminating half. */
class SessionQualityGateTest {

    private val watchedClean = LifecycleObservation.Observed(emptyList())
    private val watchedInterrupted = LifecycleObservation.Observed(listOf(ForegroundLoss(1_000L, 9_000L)))
    private val unwatched = LifecycleObservation.NotObservable(NotObservableReason.DISABLED_BY_FLAG)
    private val clean = GamingVerdict(GamingAction.NONE, emptyList())
    private val withheld = GamingVerdict(GamingAction.WITHHOLD_PROFILE, listOf("D4_TOO_FAST"))
    private val invalidated = GamingVerdict(GamingAction.INVALIDATE, listOf("FOCUS_BLIND_STRATEGY_CEILING"))

    @Test
    fun an_observed_interruption_is_not_measurable_for_that_reason() {
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED),
            SessionQualityGate.admit(watchedInterrupted, respondedTrials = 40, gaming = clean),
        )
    }

    @Test
    fun a_session_nobody_answered_is_not_measurable_for_that_reason() {
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES),
            SessionQualityGate.admit(watchedClean, respondedTrials = 0, gaming = clean),
        )
    }

    @Test
    fun an_unwatched_lifecycle_is_not_measurable_the_unknown_is_never_clean() {
        listOf(NotObservableReason.DISABLED_BY_FLAG, NotObservableReason.NO_PLATFORM_SOURCE).forEach { why ->
            assertEquals(
                SessionAdmission.NotMeasurable(NotMeasurableReason.LIFECYCLE_NOT_OBSERVED),
                SessionQualityGate.admit(LifecycleObservation.NotObservable(why), 40, clean),
                "reason $why",
            )
        }
    }

    /**
     * R15 · every non-clean action withholds, and BOTH of them do: reading only `INVALIDATE` would let a
     * `WITHHOLD_PROFILE` verdict through the gate that exists to honour it.
     */
    @Test
    fun a_fired_detector_withholds_the_session_whatever_its_action() {
        listOf(withheld, invalidated).forEach { verdict ->
            assertEquals(
                SessionAdmission.NotMeasurable(NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE),
                SessionQualityGate.admit(watchedClean, respondedTrials = 40, gaming = verdict),
                "verdict ${verdict.action}",
            )
        }
    }

    /** The discriminating half of the one above: a FLAG_ONLY verdict is clean and must NOT withhold. */
    @Test
    fun a_flag_only_verdict_is_clean_and_does_not_withhold() {
        assertEquals(
            SessionAdmission.Admitted,
            SessionQualityGate.admit(
                watchedClean,
                respondedTrials = 40,
                gaming = GamingVerdict(GamingAction.FLAG_ONLY, listOf("SOME_FLAG")),
            ),
        )
    }

    @Test
    fun observed_facts_take_precedence_over_the_unknown() {
        // Interrupted AND unanswered: the environment fact explains the behaviour, so it is the one reported.
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED),
            SessionQualityGate.admit(watchedInterrupted, respondedTrials = 0, gaming = clean),
        )
        // Unwatched AND unanswered: "nobody answered" was observed; "we could not watch" was not.
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES),
            SessionQualityGate.admit(unwatched, respondedTrials = 0, gaming = clean),
        )
        // A fired detector is an observed fact about the responses, so it outranks the unwatched sensor...
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE),
            SessionQualityGate.admit(unwatched, respondedTrials = 40, gaming = invalidated),
        )
        // ...and is outranked in turn by an interruption, which explains the pattern without blaming it.
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED),
            SessionQualityGate.admit(watchedInterrupted, respondedTrials = 40, gaming = invalidated),
        )
    }

    @Test
    fun an_honest_watched_answered_session_is_admitted() {
        assertEquals(SessionAdmission.Admitted, SessionQualityGate.admit(watchedClean, 1, clean))
        assertEquals(SessionAdmission.Admitted, SessionQualityGate.admit(watchedClean, 160, clean))
    }

    @Test
    fun a_negative_count_is_a_caller_bug_not_a_verdict() {
        assertFailsWith<IllegalArgumentException> { SessionQualityGate.admit(watchedClean, -1, clean) }
    }
}

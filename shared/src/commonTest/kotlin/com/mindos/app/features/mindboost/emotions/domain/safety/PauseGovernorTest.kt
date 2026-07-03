@file:Suppress("MagicNumber") // Pause counts / timings are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.safety

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PauseGovernorTest {

    private val governor = PauseGovernor()

    @Test
    fun four_rapid_pauses_trigger_reduce_and_shorter_session() {
        var state = PauseGovernorState()
        var decision = governor.onPause(state, nowMs = 0)
        state = decision.state
        decision = governor.onPause(state, nowMs = 10)
        state = decision.state
        decision = governor.onPause(state, nowMs = 20)
        state = decision.state
        assertFalse(decision.reduceIntensity, "three pauses do not reduce")
        decision = governor.onPause(state, nowMs = 30)
        assertTrue(decision.reduceIntensity, "the 4th pause trips reduce")
        assertTrue(decision.recommendShorterSession)
    }

    @Test
    fun cooldown_suppresses_reprompt_but_not_the_limit_counter() {
        val first = governor.onPause(PauseGovernorState(), nowMs = 0)
        assertTrue(first.promptCheckIn, "first pause prompts")
        val second = governor.onPause(first.state, nowMs = 1_000) // within 45s cooldown
        assertFalse(second.promptCheckIn, "re-prompt suppressed by cooldown")
        assertTrue(second.state.pauseCount == 2, "limit counter still increments under cooldown")
    }
}

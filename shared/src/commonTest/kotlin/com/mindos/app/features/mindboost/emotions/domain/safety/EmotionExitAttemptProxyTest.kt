@file:Suppress("MagicNumber", "MaxLineLength") // Threshold fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.safety

import com.mindos.app.features.mindboost.emotions.presentation.EmotionSessionStateHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ola 0 · S5 — pins a safety net that is ADVERTISED but structurally dead, so it cannot keep passing for armed.
 *
 * [OverloadThresholds.exitAttemptsPerBlock] promises that repeated attempts to leave a session raise an overload
 * proxy. A running session has no way to produce a second attempt: the first exit is already terminal. So the
 * signal the detector is handed is a constant, the proxy can never fire, and the module carries a safety feature
 * that reads as present in the code and in the thresholds while doing nothing at all.
 *
 * This is deliberately NOT "fixed" by adding a counter. The only way to generate a second attempt is to put a
 * confirmation step between the user and the exit of an emotion-regulation exercise, which trades a real safety
 * property — leaving is always immediate — for the ability to detect distress. That is a product decision.
 *
 * What these tests do instead is make the gap impossible to lose: they assert that the detector itself works, and
 * that the session currently feeds it nothing. If a producer is ever wired, the second test fails and whoever
 * wired it has to come here and say so.
 */
class EmotionExitAttemptProxyTest {

    private fun signals(exitAttempts: Int) = OverloadSignals(
        metrics = emptyMap(),
        consecutiveErrors = 0,
        exitAttempts = exitAttempts,
        pauseUses = 0,
        rtMedianBaseline = null,
        rtCvBaseline = null,
    )

    @Test
    fun the_detector_would_fire_if_a_producer_ever_existed() {
        // The detector is not the defect — proving that is what makes the next test's failure meaningful rather
        // than just another way of observing that the proxy never fires.
        val fired = DefaultOverloadDetector().detect(signals(OverloadThresholds().exitAttemptsPerBlock))
        assertTrue(
            OverloadProxy.REPEATED_EXIT_ATTEMPTS in fired,
            "the exit-attempt detector is itself broken, which is a different defect from having no producer",
        )
    }

    @Test
    fun the_session_feeds_the_detector_no_exit_attempts_at_all() {
        assertEquals(
            0,
            EmotionSessionStateHolder.NO_EXIT_ATTEMPT_SIGNAL,
            "the session still cannot observe a repeated exit attempt; if that changed, wire it and update this test",
        )
        val fired = DefaultOverloadDetector().detect(signals(EmotionSessionStateHolder.NO_EXIT_ATTEMPT_SIGNAL))
        assertFalse(
            OverloadProxy.REPEATED_EXIT_ATTEMPTS in fired,
            "the exit proxy cannot fire on the value the session actually supplies",
        )
    }

    @Test
    fun the_advertised_threshold_is_above_what_a_session_can_ever_reach() {
        // The precise shape of the gap: the threshold asks for more attempts than a session is capable of having.
        assertTrue(
            OverloadThresholds().exitAttemptsPerBlock > EmotionSessionStateHolder.NO_EXIT_ATTEMPT_SIGNAL,
            "if the threshold ever drops to the value the session supplies, the proxy would fire on EVERY block",
        )
    }
}

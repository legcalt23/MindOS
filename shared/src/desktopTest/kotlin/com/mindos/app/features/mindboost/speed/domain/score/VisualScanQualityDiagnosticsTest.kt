package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.exercise.opposite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC9-C-R · E5 §7 — data-quality diagnostics (not integrated into the official profile). */
class VisualScanQualityDiagnosticsTest {

    private fun o(
        items: Int,
        target: SpeedChoice,
        response: SpeedChoice?,
        rtMs: Long,
        correct: Boolean,
        anticipation: Boolean = false,
    ) = VisualScanTrialOutcome(items, target, response, rtMs * 1_000_000L, correct, anticipation)

    private fun honestRun() = (0 until 12).map { i ->
        val t = if (i % 2 == 0) SpeedChoice.LEFT else SpeedChoice.RIGHT
        o(4, t, t, 400L, correct = true)
    }

    @Test
    fun a_clean_run_triggers_no_flags() {
        val d = VisualScanQualityDiagnostics.evaluate(honestRun())
        assertTrue(d.clean)
        assertEquals(0, d.impossiblyFastTrials)
    }

    @Test
    fun impossibly_fast_trials_are_counted() {
        val outcomes = (0 until 12).map { i ->
            val t = if (i % 2 == 0) SpeedChoice.LEFT else SpeedChoice.RIGHT
            o(4, t, t, if (i < 3) 80L else 400L, correct = true)
        }
        val d = VisualScanQualityDiagnostics.evaluate(outcomes)
        assertEquals(3, d.impossiblyFastTrials)
        assertFalse(d.clean)
    }

    @Test
    fun extreme_side_bias_is_flagged() {
        val outcomes = (0 until 12).map { i ->
            val t = if (i % 2 == 0) SpeedChoice.LEFT else SpeedChoice.RIGHT
            o(4, t, SpeedChoice.LEFT, 400L, correct = t == SpeedChoice.LEFT) // always taps LEFT ⇒ bias 0.5
        }
        assertTrue(VisualScanQualityDiagnostics.evaluate(outcomes).extremeSideBias)
    }

    @Test
    fun anticipation_flood_is_flagged() {
        val outcomes = (0 until 12).map { i ->
            val t = if (i % 2 == 0) SpeedChoice.LEFT else SpeedChoice.RIGHT
            o(4, t, t, 400L, correct = true, anticipation = i < 5) // 5/12 > 0.30
        }
        assertTrue(VisualScanQualityDiagnostics.evaluate(outcomes).anticipationFlood)
    }

    @Test
    fun fast_wrong_burst_is_flagged() {
        val outcomes = (0 until 12).map { i ->
            val t = if (i % 2 == 0) SpeedChoice.LEFT else SpeedChoice.RIGHT
            if (i < 5) o(4, t, t.opposite(), 150L, correct = false) else o(4, t, t, 400L, correct = true)
        }
        assertTrue(VisualScanQualityDiagnostics.evaluate(outcomes).fastWrongBurst)
    }

    @Test
    fun improbable_perfect_under_load_is_flagged() {
        val low = (0 until 4).map { o(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val high = (0 until 4).map { o(6, SpeedChoice.LEFT, SpeedChoice.LEFT, 150L, correct = true) } // perfect + very fast
        assertTrue(VisualScanQualityDiagnostics.evaluate(low + high).improbablePerfectUnderLoad)
    }
}

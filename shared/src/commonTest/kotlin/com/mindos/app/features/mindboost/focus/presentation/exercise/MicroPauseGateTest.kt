package com.mindos.app.features.mindboost.focus.presentation.exercise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The impulsive micro-pause swallows precipitous taps (before the window) and accepts later ones; when
 * disabled it accepts everything (no gate). This is the pure decision behind the runtime input gate.
 */
class MicroPauseGateTest {

    private fun ms(value: Long) = value * 1_000_000L

    @Test
    fun windowMs_isZeroWhenDisabled_andPositiveWhenEnabled() {
        assertEquals(0L, MicroPauseGate.windowMs(enabled = false))
        assertEquals(MicroPauseGate.DEFAULT_WINDOW_MS, MicroPauseGate.windowMs(enabled = true))
    }

    @Test
    fun rejectsTapsBeforeWindow_acceptsAfter() {
        val window = MicroPauseGate.DEFAULT_WINDOW_MS // 200 ms
        // 120 ms after onset → still gated.
        assertFalse(MicroPauseGate.accepts(onsetNanos = 0, nowNanos = ms(120), windowMs = window))
        // 260 ms after onset → accepted.
        assertTrue(MicroPauseGate.accepts(onsetNanos = 0, nowNanos = ms(260), windowMs = window))
    }

    @Test
    fun zeroWindow_acceptsImmediately() {
        assertTrue(MicroPauseGate.accepts(onsetNanos = 0, nowNanos = ms(0), windowMs = 0L))
        assertTrue(MicroPauseGate.accepts(onsetNanos = ms(5), nowNanos = ms(5), windowMs = 0L))
    }
}

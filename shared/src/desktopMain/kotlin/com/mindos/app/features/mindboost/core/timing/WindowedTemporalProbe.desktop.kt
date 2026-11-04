package com.mindos.app.features.mindboost.core.timing

import androidx.compose.runtime.Composable

/**
 * Desktop has no portable vsync source: explicit fallback capability, never >T0 (ADR §3).
 *
 * No `DisposableEffect` here, deliberately (Paso 5, B3): the no-op probe registers no callback, so
 * there is nothing to leak — and it is a shared `object`, so stopping it on ONE composition's
 * disposal would reach into every other holder of the same singleton. The stop belongs where the
 * loop is, and there is no loop here.
 */
@Composable
actual fun rememberWindowedTemporalProbe(): WindowedTemporalProbe = NoOpWindowedTemporalProbe

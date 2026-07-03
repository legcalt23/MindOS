package com.mindos.app.features.mindboost.core.timing

import androidx.compose.runtime.Composable

/** Desktop has no portable vsync source → non-authoritative no-op probe (never produces real jank). */
@Composable
actual fun rememberFrameTimingProbe(): FrameTimingProbe = NoOpFrameTimingProbe

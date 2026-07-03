package com.mindos.app.features.mindboost.core.timing

import androidx.compose.runtime.Composable

/**
 * iOS has no `CADisplayLink` capture wired yet → non-authoritative no-op probe. Porting CADisplayLink
 * is deferred; until then iOS RT-critical metrics degrade (never authoritative), as designed.
 */
@Composable
actual fun rememberFrameTimingProbe(): FrameTimingProbe = NoOpFrameTimingProbe

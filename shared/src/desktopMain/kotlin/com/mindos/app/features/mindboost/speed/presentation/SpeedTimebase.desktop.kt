@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Actual de Desktop: delega en el fallback monotónico común (no-autoritativo). MVP-0 es
 * solo Android; este `actual` existe para que el target Desktop compile y para poder
 * ejercitar la UI de forma local.
 */
@Composable
actual fun rememberSpeedTimebaseHandle(): SpeedTimebaseHandle = rememberFallbackSpeedTimebaseHandle()

@Composable
actual fun SpeedTapArea(
    handle: SpeedTimebaseHandle,
    enabled: Boolean,
    onResponse: (responseUptimeNanos: Long) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) = FallbackTapArea(handle, enabled, onResponse, modifier, content)

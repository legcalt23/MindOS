@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Actual de iOS: delega en el fallback monotónico común (no-autoritativo). MVP-0 no
 * incluye adaptadores iOS reales (§2); este `actual` solo asegura que el target iOS
 * compile (todo `expect` necesita `actual` en cada target declarado).
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

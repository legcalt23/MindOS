package com.mindos.app.features.mindboost.speed.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Actual de iOS: exportador deshabilitado. MVP-0 es solo Android (sin adaptadores iOS);
 * este `actual` existe únicamente para que el target iOS compile (regla de proyecto:
 * todo `expect` necesita `actual` en todos los targets declarados).
 */
@Composable
actual fun rememberDebugExporter(): DebugExporter = remember { DisabledDebugExporter() }

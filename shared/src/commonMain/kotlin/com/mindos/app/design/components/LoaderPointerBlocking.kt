package com.mindos.app.design.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Consumes every pointer event so a blocking loading surface cannot be tapped
 * through. Shared by the global loading overlay ([AppLoadingOverlay]) and the
 * dedicated photo-upload overlay ([PhotoUploadLoadingOverlay]) so both block
 * interaction identically (single source of truth for the gesture-swallowing
 * behaviour).
 */
internal fun Modifier.blockAllPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

@file:Suppress("MagicNumber", "FunctionNaming")

package com.mindos.app.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppStrings

/*
 * AppLoadingOverlay — the global loading surface for MindOS.
 *
 * Wrap any screen/section; while `visible`, it blurs the content behind a
 * translucent scrim, centers the [AppBarsLoader] (tinted with the user's favorite
 * accent color) plus an optional message, and — by default — swallows touches so
 * the underlying flow cannot be interacted with mid-load.
 *
 * Blur is multiplatform-safe: `Modifier.blur` is backed by Skia on iOS/Desktop and
 * by RenderEffect on Android 12+. On older Android it is a graceful no-op — and
 * because the scrim is drawn regardless, the loading state still reads correctly
 * everywhere (scrim is the fallback, blur is the enhancement).
 *
 * This is for GENERAL full-screen / blocking loads. Small inline spinners and the
 * dedicated photo-upload loader ([PhotoUploadLoader]) are intentionally separate.
 */

/** Content blur radius while loading (enhancement only; the scrim is the fallback). */
private val OVERLAY_BLUR_RADIUS: Dp = 12.dp

/** Default scrim opacity for the wrapping overlay — light, since it sits over real content. */
private const val DEFAULT_SCRIM_ALPHA = 0.22f

/** Stronger scrim for the standalone full-screen loader (nothing meaningful behind it). */
private const val FULL_SCREEN_SCRIM_ALPHA = 0.6f

/**
 * Wraps [content] and shows the global loading overlay while [visible].
 *
 * @param visible whether the loading overlay is shown.
 * @param color loader tint — pass the user's favorite accent color.
 * @param message optional label under the loader (defaults to none; falls back to
 *   an accessible "loading" description for screen readers).
 * @param blurEnabled blur the content behind the scrim where supported.
 * @param blockInteraction consume touches so the content can't be used while loading.
 * @param scrimColor translucent veil drawn over the (optionally blurred) content.
 */
// Compose overlay API: visibility + tint + a11y/blur/scrim knobs + wrapped content.
@Suppress("LongParameterList")
@Composable
fun AppLoadingOverlay(
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    message: String? = null,
    blurEnabled: Boolean = true,
    blockInteraction: Boolean = true,
    scrimColor: Color = Color.Black.copy(alpha = DEFAULT_SCRIM_ALPHA),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        // The content wrapper (NOT matchParentSize) is what sizes the outer Box;
        // the scrim then matchParentSizes to it. Blur is a draw-only modifier, so
        // it never changes the measured size.
        Box(
            modifier = if (visible && blurEnabled) Modifier.blur(OVERLAY_BLUR_RADIUS) else Modifier,
        ) {
            content()
        }

        if (visible) {
            LoadingScrim(
                color = color,
                message = message,
                blockInteraction = blockInteraction,
                scrimColor = scrimColor,
            )
        }
    }
}

/**
 * Standalone full-screen loading state, for a blocking wait BEFORE any content
 * exists (initial screen load, gated routing, etc.). Renders the app background,
 * the centered loader and a "Cargando..." message by default.
 *
 * @param color loader tint — pass the user's favorite accent color.
 * @param message optional label (defaults to [AppStrings.Loading]).
 * @param blurEnabled kept for API symmetry; the background is a flat gradient so
 *   blur has nothing to soften, but it stays honest across platforms.
 */
@Composable
fun FullScreenAppLoading(
    color: Color,
    modifier: Modifier = Modifier,
    message: String? = null,
    blurEnabled: Boolean = true,
) {
    AppLoadingOverlay(
        visible = true,
        color = color,
        modifier = modifier.fillMaxSize(),
        message = message ?: AppStrings.Loading,
        blurEnabled = blurEnabled,
        blockInteraction = true,
        scrimColor = AppColors.DeepBlack.copy(alpha = FULL_SCREEN_SCRIM_ALPHA),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(AppColors.DarkVerticalGradient)),
        )
    }
}

/**
 * The scrim + centered loader + optional message drawn over the content while
 * loading. Declared as a [BoxScope] extension so it can [Modifier.matchParentSize].
 */
@Composable
private fun BoxScope.LoadingScrim(
    color: Color,
    message: String?,
    blockInteraction: Boolean,
    scrimColor: Color,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(scrimColor)
            .then(if (blockInteraction) Modifier.blockLoadingTouches() else Modifier)
            // One deterministic announcement for the whole loading group: the
            // message when present, else the generic "loading" label — plus an
            // indeterminate progress range and a polite live region. clearAndSet
            // (not merge) avoids the loader + Text double-announcing the message.
            .clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = message ?: AppStrings.A11yLoading
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppBarsLoader(color = color)
            if (message != null) {
                Spacer(Modifier.height(AppSpacing.L))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Consumes every pointer event so a blocking overlay cannot be tapped through.
 * (Deliberately parallels `PhotoUploadLoader`'s upload-blocking modifier; kept
 * local to avoid touching the specialized photo-upload loader.)
 */
private fun Modifier.blockLoadingTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

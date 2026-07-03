@file:Suppress("MagicNumber", "FunctionNaming", "LongMethod")

package com.mindos.app.design.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppStrings
import kotlin.math.roundToInt

/*
 * Global photo-upload loader for MindOS.
 *
 * This is a pure Compose Multiplatform translation of the Uiverse loader by
 * Shoh2008 (no CSS, no WebView, no external assets): a small white card holding
 * a rising orange "rombo" (with a stronger-orange offset shadow) plus a strong
 * orange dot that swings like a pendulum. Everything is drawn with [Canvas] and
 * driven by a single 2s infinite transition, so it renders identically on
 * Android, iOS and Desktop.
 *
 * Use it for EVERY "the app is uploading a photo" wait:
 *  - [PhotoUploadLoadingOverlay] for flows that must block interaction.
 *  - [InlinePhotoUploadLoading] for a localized card/thumbnail upload.
 *  - [PhotoUploadLoader] as the raw badge when you compose your own layout.
 *
 * The loader is always INDETERMINATE (it never fakes progress). A real,
 * measured `progress` (0f..1f) is only ever surfaced as an accessible percentage
 * label next to the loader — never injected into the animation.
 */

/** Corner radius ratio: 4dp on the reference's 64dp card (4 / 64). */
private const val CORNER_RATIO = 0.0625f

/** Full animation cycle. Matches the reference's `2s` loop. */
private const val CYCLE_MILLIS = 2000

/** Dot swing arc, in degrees. Reference `rotate`: -15deg .. 25deg. */
private const val DOT_SWING_START_DEG = -15f
private const val DOT_SWING_END_DEG = 25f

/** Default loader size for the compact inline variant. */
private val InlineLoaderSize: Dp = 48.dp

/** Content blur radius behind the blocking overlay (enhancement; scrim is the fallback). */
private val OverlayBlurRadius: Dp = 12.dp

/**
 * The raw animated photo-upload badge (white card + rising rombo + swinging dot).
 *
 * Accessibility: exposes "Subiendo foto" so it is meaningful when used on its own.
 * When embedded in [PhotoUploadLoadingOverlay] / [InlinePhotoUploadLoading] the
 * surrounding group merges semantics into a single spoken announcement.
 */
@Composable
fun PhotoUploadLoader(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    backgroundColor: Color = AppColors.PhotoUploadLoaderBackground,
    primaryColor: Color = AppColors.PhotoUploadLoaderPrimary,
    secondaryColor: Color = AppColors.PhotoUploadLoaderSecondary,
) {
    val transition = rememberInfiniteTransition(label = "photoUploadLoader")
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CYCLE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "slide",
    )
    val dotAngle by transition.animateFloat(
        initialValue = DOT_SWING_START_DEG,
        targetValue = DOT_SWING_END_DEG,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CYCLE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAngle",
    )

    Box(
        modifier = modifier
            .size(size)
            // overflow: hidden — clip the moving shapes to the rounded card.
            .clip(RoundedCornerShape(size * CORNER_RATIO))
            .background(backgroundColor)
            .semantics { contentDescription = AppStrings.A11yPhotoUploadLoading },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawPhotoUploadLoader(
                slide = slide,
                dotAngleDeg = dotAngle,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
            )
        }
    }
}

/**
 * Full-bleed overlay for photo uploads. Renders [content] normally and, while
 * [visible], blurs it behind a dimming scrim with the centered [PhotoUploadLoader]
 * on top.
 *
 * Mirrors the conventions of the general [AppLoadingOverlay] (blur enhancement +
 * scrim fallback + optional interaction blocking) but stays a dedicated,
 * photo-specific loader.
 *
 * @param message optional label; defaults to "Subiendo foto...".
 * @param progress optional REAL progress (0f..1f). When present it is shown as a
 *   percentage and exposed to screen readers — it never drives the animation and
 *   is never fabricated.
 * @param blockInteraction consume touches so the flow can't be used mid-upload.
 *   Pass `false` only when the flow legitimately allows cancellation underneath.
 * @param blurEnabled blur the content behind the scrim where supported (Skia on
 *   iOS/Desktop, RenderEffect on Android 12+; a graceful no-op on older Android —
 *   the scrim is the fallback, blur is the enhancement).
 */
@Suppress("LongParameterList")
@Composable
fun PhotoUploadLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: Float? = null,
    blockInteraction: Boolean = true,
    blurEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (visible && blurEnabled) Modifier.blur(OverlayBlurRadius) else Modifier),
        ) {
            content()
        }
        if (visible) {
            val label = message ?: AppStrings.PhotoUploadInProgress
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AppColors.DeepBlack.copy(alpha = AppColors.ScrimAlpha))
                    .then(if (blockInteraction) Modifier.blockAllPointerInput() else Modifier)
                    // One polite announcement for the whole upload group; a real
                    // progress value is also exposed as a range for screen readers.
                    .semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                        progress?.let {
                            progressBarRangeInfo = ProgressBarRangeInfo(it.coerceIn(0f, 1f), 0f..1f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.XXL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PhotoUploadLoader()
                    Spacer(modifier = Modifier.height(AppSpacing.L))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.OnBackground,
                        textAlign = TextAlign.Center,
                    )
                    progress?.let {
                        Spacer(modifier = Modifier.height(AppSpacing.XS))
                        Text(
                            text = percentLabel(it),
                            style = MaterialTheme.typography.labelLarge,
                            color = AppColors.Mist,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact inline photo-upload indicator for a localized surface (an image card
 * or thumbnail that is uploading). Does NOT block interaction.
 *
 * @param message optional caption under the loader.
 * @param progress optional REAL progress (0f..1f), shown as a percentage only
 *   when provided.
 */
@Composable
fun InlinePhotoUploadLoading(
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: Float? = null,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            progress?.let {
                progressBarRangeInfo = ProgressBarRangeInfo(it.coerceIn(0f, 1f), 0f..1f)
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PhotoUploadLoader(size = InlineLoaderSize)
        if (message != null || progress != null) {
            Spacer(modifier = Modifier.height(AppSpacing.S))
        }
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Mist,
                textAlign = TextAlign.Center,
            )
        }
        progress?.let {
            Text(
                text = percentLabel(it),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Mist,
            )
        }
    }
}

// ── Drawing ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawPhotoUploadLoader(
    slide: Float,
    dotAngleDeg: Float,
    primaryColor: Color,
    secondaryColor: Color,
) {
    val s = size.minDimension

    // Rombo half-diagonal.
    val half = s * 0.36f

    // Front rombo slides vertically: from mostly hidden below the card (slide=0)
    // up into view (slide=1), reversing every cycle — the reference `slide`.
    val frontCx = s * 0.40f
    val hiddenCy = s * 1.18f
    val visibleCy = s * 0.74f
    val frontCy = hiddenCy + (visibleCy - hiddenCy) * slide

    // Strong-accent "shadow" rombo, offset up-right of the soft one (echoes the
    // reference's rotated box-shadow direction). Drawn behind the front rombo.
    val shadowCx = frontCx + s * 0.30f
    val shadowCy = frontCy - s * 0.10f
    drawRhombus(cx = shadowCx, cy = shadowCy, half = half, color = primaryColor)
    drawRhombus(cx = frontCx, cy = frontCy, half = half, color = secondaryColor)

    // Swinging dot (top-left), pivoting around a point far below the card so the
    // small angle sweep reads as a gentle pendulum (reference origin 35,145 / 64).
    val dotRadius = s * 0.11f
    val dotCenter = Offset(x = s * 0.30f, y = s * 0.30f)
    val pivot = Offset(x = s * 0.55f, y = s * 2.27f)
    rotate(degrees = dotAngleDeg, pivot = pivot) {
        drawCircle(color = primaryColor, radius = dotRadius, center = dotCenter)
    }
}

private fun DrawScope.drawRhombus(cx: Float, cy: Float, half: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - half)
        lineTo(cx + half, cy)
        lineTo(cx, cy + half)
        lineTo(cx - half, cy)
        close()
    }
    drawPath(path = path, color = color)
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun percentLabel(progress: Float): String {
    val pct = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    return "$pct%"
}

// ── Example composables ─────────────────────────────────────────────────────
// This module has no @Preview tooling wired in commonMain, so these are plain
// example composables (the project-appropriate substitute). Render them from an
// @Preview host or an IDE run target; they double as living documentation.

@Composable
internal fun PhotoUploadLoaderExample() {
    Box(
        modifier = Modifier.background(AppColors.Background).padding(AppSpacing.XXL),
        contentAlignment = Alignment.Center,
    ) {
        PhotoUploadLoader()
    }
}

@Composable
internal fun PhotoUploadLoadingOverlayExample() {
    PhotoUploadLoadingOverlay(
        visible = true,
        message = AppStrings.PhotoUploadInProgress,
        progress = 0.42f,
        modifier = Modifier.size(240.dp),
    ) {
        Box(modifier = Modifier.background(AppColors.Surface).size(240.dp))
    }
}

@Composable
internal fun InlinePhotoUploadLoadingExample() {
    Column(
        modifier = Modifier.background(AppColors.Background).padding(AppSpacing.XXL),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.XXL),
    ) {
        InlinePhotoUploadLoading(message = AppStrings.PhotoUploadInProgress)
        InlinePhotoUploadLoading(message = AppStrings.PhotoUploadInProgress, progress = 0.42f)
    }
}

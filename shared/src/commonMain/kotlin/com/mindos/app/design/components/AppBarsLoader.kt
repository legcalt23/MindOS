@file:Suppress("MagicNumber", "FunctionNaming")

package com.mindos.app.design.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppStrings

/*
 * AppBarsLoader — global three-bar loading indicator for MindOS.
 *
 * A pure Compose Multiplatform translation of the Uiverse "jimu-primary-loading"
 * loader by Donewenfu (no CSS, no WebView, no GIF, no external/animated assets):
 * three vertical bars that pulse out of phase — each one rises, grows taller and
 * brightens at its peak, then settles back. It renders identically on Android,
 * iOS and Desktop because every frame is drawn with a single [Canvas] driven by
 * one infinite transition.
 *
 * Reference CSS keyframe (period 0.8s, ease-in-out) mapped 1:1:
 *   0% / 80% / 100% -> opacity .75, height 32px, offset 0      (rest)
 *   40%             -> opacity 1,   height 40px, offset -8px   (peak)
 * Per-bar animation-delay -> a persistent phase shift so the pulse travels across
 * the bars (left 0s, center 0.16s, right 0.32s == fractions 0, 0.2, 0.4).
 *
 * The color is REQUIRED and meant to come from the user's favorite accent color
 * (see `favoriteAccentColorOrThemePrimary`); `FavoriteColor.LoaderBlue` is only
 * the documented last-resort fallback.
 */

private const val BAR_COUNT = 3

/** Per-bar phase offset as a fraction of the cycle (0.16s / 0.32s over 0.8s). */
private const val LEFT_DELAY_FRACTION = 0f
private const val CENTER_DELAY_FRACTION = 0.2f
private const val RIGHT_DELAY_FRACTION = 0.4f

/** Keyframe boundaries: rise ends at 40%, fall ends at 80%, then a flat rest. */
private const val RISE_END_FRACTION = 0.4f
private const val FALL_END_FRACTION = 0.8f

/** Opacity endpoints from the reference keyframe. */
private const val REST_ALPHA = 0.75f
private const val PEAK_ALPHA = 1f

/** CSS `ease-in-out` == cubic-bezier(0.42, 0, 0.58, 1). Applied to both rise and fall. */
private val EASE_IN_OUT = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * Draws the three animated bars tinted with [color].
 *
 * @param color the loader tint — pass the user's favorite accent color.
 * @param barWidth width of each bar (reference `13.6px`).
 * @param minBarHeight resting bar height (reference `32px`).
 * @param maxBarHeight peak bar height (reference `40px`).
 * @param spacing horizontal gap between adjacent bars (reference `~6.4px`, so the
 *   bar centers sit `~19.992px` apart as in the CSS).
 * @param durationMillis full pulse cycle (reference `800ms`).
 */
// Compose component: a `modifier` plus the documented CSS-derived sizing knobs.
@Suppress("LongParameterList")
@Composable
fun AppBarsLoader(
    modifier: Modifier = Modifier,
    color: Color,
    barWidth: Dp = 13.6.dp,
    minBarHeight: Dp = 32.dp,
    maxBarHeight: Dp = 40.dp,
    spacing: Dp = 6.4.dp,
    durationMillis: Int = 800,
) {
    val transition = rememberInfiniteTransition(label = "appBarsLoader")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Linear sawtooth 0->1; the ease-in-out shaping lives in calculateBarProgress.
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cycle",
    )

    // Extra vertical headroom so the risen (offset up + taller) bar never clips.
    val rise = maxBarHeight - minBarHeight
    val totalWidth = barWidth * BAR_COUNT + spacing * (BAR_COUNT - 1)
    val totalHeight = maxBarHeight + rise * 2

    Canvas(
        modifier = modifier
            .size(width = totalWidth, height = totalHeight)
            .semantics {
                contentDescription = AppStrings.A11yLoading
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        val barW = barWidth.toPx()
        val gap = spacing.toPx()
        val minH = minBarHeight.toPx()
        val maxH = maxBarHeight.toPx()
        val risePx = rise.toPx()
        val corner = CornerRadius(barW / 2f, barW / 2f)
        val delays = floatArrayOf(LEFT_DELAY_FRACTION, CENTER_DELAY_FRACTION, RIGHT_DELAY_FRACTION)

        for (index in 0 until BAR_COUNT) {
            val intensity = calculateBarProgress(cycle, delays[index])
            val height = minH + (maxH - minH) * intensity
            val alpha = REST_ALPHA + (PEAK_ALPHA - REST_ALPHA) * intensity
            val yOffset = -risePx * intensity
            val left = index * (barW + gap)
            val top = (size.height - height) / 2f + yOffset
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = Size(barW, height),
                cornerRadius = corner,
            )
        }
    }
}

/**
 * Pure keyframe function: the animation intensity (0f rest .. 1f peak) of one bar
 * at cycle position [elapsedFraction] (0f..1f), phase-shifted by [delayFraction].
 *
 * Mirrors the reference keyframe with a symmetric ease-in-out ([EASE_IN_OUT],
 * cubic-bezier(0.42, 0, 0.58, 1)) on both the rise and the fall, then a flat rest
 * between 80% and 100%. Exposed as `internal` so the keyframe math is
 * unit-testable without a UI.
 */
internal fun calculateBarProgress(elapsedFraction: Float, delayFraction: Float): Float {
    val local = (elapsedFraction - delayFraction).mod(1f)
    return when {
        local < RISE_END_FRACTION ->
            EASE_IN_OUT.transform(local / RISE_END_FRACTION)
        local < FALL_END_FRACTION ->
            EASE_IN_OUT.transform((FALL_END_FRACTION - local) / (FALL_END_FRACTION - RISE_END_FRACTION))
        else -> 0f
    }
}

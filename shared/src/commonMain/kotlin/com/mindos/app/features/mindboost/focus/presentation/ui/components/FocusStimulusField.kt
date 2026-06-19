@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FeedbackKind
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState

/**
 * The heart of the live exercise: a single focal element on a calm field that
 * smoothly transitions between idle breathing, the green signal to catch, a
 * violet lure to ignore, and a brief feedback flash. All transitions are
 * animated so the experience feels alive and premium, never jumpy.
 */
@Composable
fun FocusStimulusField(
    field: FocusFieldState,
    modifier: Modifier = Modifier,
) {
    val target = field.toVisual()

    val scale by animateFloatAsState(
        targetValue = target.scale,
        animationSpec = tween(durationMillis = target.durationMs, easing = LinearEasing),
        label = "stim-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = target.alpha,
        animationSpec = tween(durationMillis = target.durationMs),
        label = "stim-alpha",
    )
    val color by animateColorAsState(
        targetValue = target.color,
        animationSpec = tween(durationMillis = 160),
        label = "stim-color",
    )

    val infinite = rememberInfiniteTransition(label = "stim-breathe")
    val breathe by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = minOf(size.width, size.height) * 0.22f
        val pulse = if (target.breathing) breathe else 1f
        val radius = baseRadius * scale * pulse

        // Outer glow halo.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.40f * alpha), Color.Transparent),
                center = center,
                radius = radius * 2.1f,
            ),
            radius = radius * 2.1f,
            center = center,
        )

        // Focal ring.
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.10f),
        )

        // Filled core — only the catchable signal gets a solid, inviting center.
        if (target.solidCore) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.95f * alpha), color.copy(alpha = 0.10f * alpha)),
                    center = center,
                    radius = radius * 0.78f,
                ),
                radius = radius * 0.78f,
                center = center,
            )
        }
    }
}

private data class FieldVisual(
    val color: Color,
    val scale: Float,
    val alpha: Float,
    val durationMs: Int,
    val breathing: Boolean,
    val solidCore: Boolean,
)

private fun FocusFieldState.toVisual(): FieldVisual = when (this) {
    is FocusFieldState.Idle -> FieldVisual(
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0.35f,
        durationMs = 420,
        breathing = true,
        solidCore = false,
    )

    is FocusFieldState.Signal -> FieldVisual(
        color = AppColors.FocusSignal,
        scale = 1.0f,
        alpha = (0.55f + 0.45f * contrast).coerceIn(0.4f, 1f),
        durationMs = 200,
        breathing = false,
        solidCore = true,
    )

    is FocusFieldState.Lure -> FieldVisual(
        color = AppColors.FocusLure,
        scale = 0.82f,
        alpha = (0.45f + 0.35f * contrast).coerceIn(0.35f, 0.85f),
        durationMs = 200,
        breathing = false,
        solidCore = false,
    )

    is FocusFieldState.Choice -> FieldVisual(
        // Two-choice trials render via FocusChoiceField; this single-element field is not used for
        // them, so a fully transparent visual keeps the sealed `when` exhaustive without drawing.
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0f,
        durationMs = 200,
        breathing = false,
        solidCore = false,
    )

    is FocusFieldState.Stop -> FieldVisual(
        // Stop-signal (E4): a vivid red "brake" — the user must withhold/abort.
        color = AppColors.FocusError,
        scale = 1.1f,
        alpha = 1f,
        durationMs = 120,
        breathing = false,
        solidCore = true,
    )

    is FocusFieldState.Search -> FieldVisual(
        // Visual search (E7) renders via FocusSearchField; transparent here keeps `when` exhaustive.
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0f,
        durationMs = 200,
        breathing = false,
        solidCore = false,
    )

    is FocusFieldState.RuleSwitch -> FieldVisual(
        // Task switching (E6) renders via FocusRuleSwitchField; transparent here keeps `when` exhaustive.
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0f,
        durationMs = 200,
        breathing = false,
        solidCore = false,
    )

    is FocusFieldState.Anchor -> FieldVisual(
        // Digital distraction (E11) renders via FocusAnchorField; transparent here keeps `when` exhaustive.
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0f,
        durationMs = 200,
        breathing = false,
        solidCore = false,
    )

    is FocusFieldState.Tracking,
    is FocusFieldState.Stream,
    is FocusFieldState.Dual,
    -> FieldVisual(
        // E9/E10/E8 render via their own animated fields; transparent here keeps `when` exhaustive.
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0f,
        durationMs = 200,
        breathing = false,
        solidCore = false,
    )

    is FocusFieldState.Feedback -> FieldVisual(
        color = kind.toColor(),
        scale = 1.28f,
        alpha = 0.9f,
        durationMs = 180,
        breathing = false,
        solidCore = kind == FeedbackKind.HIT,
    )
}

private fun FeedbackKind.toColor(): Color = when (this) {
    FeedbackKind.HIT -> AppColors.FocusHit
    FeedbackKind.CORRECT_REJECTION -> AppColors.FocusReadout
    FeedbackKind.MISS -> AppColors.FocusMiss
    FeedbackKind.FALSE_ALARM -> AppColors.FocusError
    FeedbackKind.TOO_EARLY -> AppColors.FocusError
}

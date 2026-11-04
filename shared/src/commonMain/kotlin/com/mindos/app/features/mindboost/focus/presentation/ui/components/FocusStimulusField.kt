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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

        // Achromatic marks. Colour alone cannot carry a distinction the task depends on: STOP used to differ
        // from the GO signal only by hue plus 10 % of radius, and TWO feedback outcomes rendered identically.
        if (target.innerRing) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius * 0.52f,
                center = center,
                style = Stroke(width = radius * 0.10f),
            )
        }
        if (target.bar) {
            val halfWidth = radius * 0.92f
            val thickness = radius * 0.26f
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(center.x - halfWidth, center.y),
                end = Offset(center.x + halfWidth, center.y),
                strokeWidth = thickness,
                cap = StrokeCap.Round,
            )
        }
    }
}

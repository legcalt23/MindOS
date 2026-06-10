@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import com.mindos.app.design.AppColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient living background for the MindBoost surfaces: two slow, breathing
 * aurora blooms drifting over a deep canvas. Purely decorative and GPU-cheap
 * (two radial gradients). Animation is intentional here — the user asked for an
 * advanced, polished experience.
 */
@Composable
fun FocusAtmosphere(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "focus-atmosphere")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Canvas(modifier = modifier) {
        // Base wash.
        drawRect(color = AppColors.FocusCanvas)

        val w = size.width
        val h = size.height
        val baseRadius = maxOf(w, h) * 0.65f * breathe * intensity
        val d = drift.toDouble()

        val centerA = Offset(
            x = w * (0.30f + 0.10f * cos(d).toFloat()),
            y = h * (0.32f + 0.08f * sin(d).toFloat()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.FocusAuroraA.copy(alpha = 0.55f * intensity),
                    Color.Transparent,
                ),
                center = centerA,
                radius = baseRadius,
            ),
            radius = baseRadius,
            center = centerA,
        )

        val centerB = Offset(
            x = w * (0.72f - 0.10f * sin(d * 0.8).toFloat()),
            y = h * (0.70f + 0.09f * cos(d * 0.8).toFloat()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.FocusAuroraB.copy(alpha = 0.45f * intensity),
                    Color.Transparent,
                ),
                center = centerB,
                radius = baseRadius * 0.9f,
            ),
            radius = baseRadius * 0.9f,
            center = centerB,
        )
    }
}

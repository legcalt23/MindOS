@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders a multiple-object-tracking trial (E9 Enjambre) in three phases over one animation:
 * HIGHLIGHT (targets ringed green) → MOVE (all dots identical, orbiting) → PROBE (one dot ringed
 * white). The user keeps the targets in mind through the motion and the session screen captures a
 * single tap meaning "the probed dot was a target". The motion is the task — without it there is no
 * tracking — so the animation is functional, not decorative.
 */
@Composable
fun FocusTrackingField(field: FocusFieldState.Tracking, modifier: Modifier = Modifier) {
    val dots = remember(field) { layout(field) }
    val probe = remember(field) { pickProbe(field) }
    var started by remember(field) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = TRIAL_MS, easing = LinearEasing),
        label = "mot",
    )
    LaunchedEffect(field) { started = true }

    val highlighting = progress < HIGHLIGHT_END
    val probing = progress > PROBE_START
    val moveFraction = ((progress - HIGHLIGHT_END) / (PROBE_START - HIGHLIGHT_END)).coerceIn(0f, 1f)

    Canvas(modifier) {
        val dotRadius = minOf(size.width, size.height) * 0.035f
        dots.forEachIndexed { i, dot ->
            val angle = 2.0 * PI * dot.turns * moveFraction + dot.phase
            val x = (dot.cx + dot.orbit * cos(angle).toFloat()) * size.width
            val y = (dot.cy + dot.orbit * sin(angle).toFloat()) * size.height
            val center = Offset(x, y)
            drawCircle(color = AppColors.FocusReadout, radius = dotRadius, center = center)
            if (highlighting && i in field.targetIndices) {
                drawCircle(
                    color = AppColors.FocusSignal,
                    radius = dotRadius * 1.8f,
                    center = center,
                    style = Stroke(width = dotRadius * 0.45f),
                )
            }
            if (probing && i == probe) {
                drawCircle(
                    color = AppColors.ProfileTextPrimary,
                    radius = dotRadius * 2.0f,
                    center = center,
                    style = Stroke(width = dotRadius * 0.55f),
                )
            }
        }
    }
}

private const val TRIAL_MS = 1800
private const val HIGHLIGHT_END = 0.28f
private const val PROBE_START = 0.82f

private data class Dot(val cx: Float, val cy: Float, val orbit: Float, val turns: Double, val phase: Double)

private fun layout(field: FocusFieldState.Tracking): List<Dot> {
    val random = Random(field.layoutSeed)
    return (0 until field.objectCount).map {
        Dot(
            cx = 0.18f + random.nextFloat() * 0.64f,
            cy = 0.20f + random.nextFloat() * 0.60f,
            orbit = 0.05f + random.nextFloat() * 0.07f,
            turns = 1.0 + field.speed * 0.5,
            phase = random.nextDouble() * 2.0 * PI,
        )
    }
}

/** A probe dot consistent with [FocusFieldState.Tracking.probedIsTarget]. */
private fun pickProbe(field: FocusFieldState.Tracking): Int =
    (0 until field.objectCount).firstOrNull { (it in field.targetIndices) == field.probedIsTarget } ?: 0

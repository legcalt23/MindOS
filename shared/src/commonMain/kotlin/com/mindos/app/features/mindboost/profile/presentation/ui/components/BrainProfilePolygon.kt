@file:Suppress("FunctionNaming", "MagicNumber") // Composable name + canvas geometry literals.

package com.mindos.app.features.mindboost.profile.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.profile.presentation.state.ProductMotorUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Radar of the 8 product motors. HONEST by construction: a measured motor is a bright vertex at its value
 * fraction; a CALIBRATING motor (null score) is a hollow muted ring near the centre — it is NOT plotted at
 * 0, so "no data yet" never reads as "worst score". The filled shape is only drawn over the measured
 * vertices (when ≥3 exist). Deliberately animation-free: the shape is data.
 */
@Composable
fun BrainProfilePolygon(motors: List<ProductMotorUi>, modifier: Modifier = Modifier) {
    val grid = AppColors.Stroke
    val measured = AppColors.FocusSignal
    val calibrating = AppColors.Disabled

    Canvas(modifier = modifier.size(260.dp)) {
        val axisCount = motors.size
        if (axisCount < 3) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(center.x, center.y) * 0.78f

        listOf(1f, 0.66f, 0.33f).forEach { fraction ->
            drawPath(ringPath(center, radius, axisCount, fraction), color = grid, style = Stroke(width = 1f))
        }
        for (index in 0 until axisCount) {
            drawLine(grid, start = center, end = vertex(center, radius, index, axisCount, 1f), strokeWidth = 1f)
        }

        val measuredPoints = motors.mapIndexedNotNull { index, motor ->
            motor.score0to100?.let { vertex(center, radius, index, axisCount, fractionOf(it)) }
        }
        if (measuredPoints.size >= 3) {
            val path = Path().apply {
                measuredPoints.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(path, color = measured.copy(alpha = 0.22f))
            drawPath(path, color = measured, style = Stroke(width = 2f))
        }

        motors.forEachIndexed { index, motor ->
            val score = motor.score0to100
            if (score != null) {
                drawCircle(measured, radius = 4f, center = vertex(center, radius, index, axisCount, fractionOf(score)))
            } else {
                // CALIBRATING: hollow marker near the centre means "not measured yet", NOT a 0 value.
                drawCircle(
                    calibrating,
                    radius = 4f,
                    center = vertex(center, radius, index, axisCount, 0.12f),
                    style = Stroke(width = 1.5f),
                )
            }
        }
    }
}

private fun fractionOf(score0to100: Int): Float = (score0to100 / 100f).coerceIn(0f, 1f)

private fun vertex(center: Offset, radius: Float, index: Int, axisCount: Int, fraction: Float): Offset {
    val angle = -PI / 2 + index * 2.0 * PI / axisCount
    val r = radius * fraction
    return Offset(center.x + (cos(angle) * r).toFloat(), center.y + (sin(angle) * r).toFloat())
}

private fun ringPath(center: Offset, radius: Float, axisCount: Int, fraction: Float): Path {
    val path = Path()
    for (index in 0 until axisCount) {
        val point = vertex(center, radius, index, axisCount, fraction)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

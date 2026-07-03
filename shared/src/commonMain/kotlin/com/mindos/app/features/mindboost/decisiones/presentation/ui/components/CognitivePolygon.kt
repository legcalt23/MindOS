@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.decisiones.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.decisiones.presentation.state.PolygonAxis
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Static radar/polygon chart of the cognitive axes. Each axis value is read as a 0..100 fraction of
 * the outer ring. Deliberately animation-free: the shape is data, not decoration.
 */
@Composable
fun CognitivePolygon(axes: List<PolygonAxis>, modifier: Modifier = Modifier) {
    val signal = AppColors.FocusSignal
    val grid = AppColors.Stroke

    Canvas(modifier = modifier.size(240.dp)) {
        val axisCount = axes.size
        if (axisCount < 3) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(center.x, center.y) * 0.82f

        listOf(1f, 0.66f, 0.33f).forEach { fraction ->
            drawPath(ringPath(center, radius, axisCount, fraction), color = grid, style = Stroke(width = 1f))
        }
        for (index in 0 until axisCount) {
            drawLine(grid, start = center, end = vertex(center, radius, index, axisCount, 1f), strokeWidth = 1f)
        }

        val dataPath = dataPath(center, radius, axes)
        drawPath(dataPath, color = signal.copy(alpha = 0.25f))
        drawPath(dataPath, color = signal, style = Stroke(width = 2f))
        for (index in 0 until axisCount) {
            val point = vertex(center, radius, index, axisCount, axisFraction(axes[index]))
            drawCircle(signal, radius = 3f, center = point)
        }
    }
}

/** Simple label/value legend for the polygon axes. */
@Composable
fun PolygonAxisLegend(axes: List<PolygonAxis>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        axes.forEach { axis ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = axis.label, color = AppColors.FocusReadout, fontSize = 13.sp)
                Text(
                    text = "${axis.value.roundToInt()}",
                    color = AppColors.ProfileTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

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

private fun dataPath(center: Offset, radius: Float, axes: List<PolygonAxis>): Path {
    val path = Path()
    axes.forEachIndexed { index, axis ->
        val point = vertex(center, radius, index, axes.size, axisFraction(axis))
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

private fun axisFraction(axis: PolygonAxis): Float = (axis.value / 100.0).coerceIn(0.0, 1.0).toFloat()

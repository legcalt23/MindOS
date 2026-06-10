@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors

/**
 * Circular gauge for a 0..100 focus index. When [confident] is false it renders a
 * faint, incomplete ring and a "—" instead of a number — the system never shows
 * an under-powered estimate as a hard fact (honest UX, 09_ui_ux §5).
 */
@Composable
fun FocusGauge(
    value: Double?,
    label: String,
    confident: Boolean,
    modifier: Modifier = Modifier,
    diameter: Int = 132,
) {
    val pct = ((value ?: 0.0).coerceIn(0.0, 100.0) / 100.0).toFloat()
    val animated by animateFloatAsState(
        targetValue = if (confident) pct else 0.18f,
        animationSpec = tween(durationMillis = 900),
        label = "gauge",
    )

    Box(modifier = modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter.dp)) {
            val stroke = size.minDimension * 0.085f
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)

            // Track.
            drawArc(
                color = AppColors.Stroke,
                startAngle = 130f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Value sweep.
            val sweepColors = if (confident) {
                listOf(AppColors.FocusSignal, AppColors.NeuCyan)
            } else {
                listOf(AppColors.FocusReadout.copy(alpha = 0.5f), AppColors.FocusReadout.copy(alpha = 0.5f))
            }
            drawArc(
                brush = Brush.sweepGradient(sweepColors),
                startAngle = 130f,
                sweepAngle = 280f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (confident && value != null) value.toInt().toString() else "—",
                color = AppColors.ProfileTextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                color = AppColors.FocusReadout,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

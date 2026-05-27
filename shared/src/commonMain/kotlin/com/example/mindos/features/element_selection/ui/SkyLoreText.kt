package com.example.mindos.features.element_selection.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.ui.toSanctuaryColor
import com.example.mindos.features.element_selection.domain.ElementMaster
import kotlin.math.PI
import kotlin.math.sin

private const val LoreTwoPi = (PI * 2.0).toFloat()

@Composable
fun SkyLoreText(
    element: ElementMaster?,
    time: Float,
    modifier: Modifier = Modifier,
) {
    val current = element ?: return
    val reveal = 1f
    val baseColor = current.baseColor.toSanctuaryColor()
    val coreColor = current.coreColor.toSanctuaryColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 202.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(min = 202.dp)
                .matchParentSize(),
        ) {
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            val panelInset = 6.dp.toPx()
            val pulse = 0.66f + wave(time * 0.52f) * 0.34f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF11172A).copy(alpha = 0.72f),
                        Color(0xFF080A14).copy(alpha = 0.88f),
                        Color(0xFF03040A).copy(alpha = 0.80f),
                    ),
                ),
                cornerRadius = corner,
                size = size,
            )

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.18f * pulse),
                        coreColor.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * (0.50f + sin(time * LoreTwoPi) * 0.018f), size.height * 0.28f),
                    radius = size.maxDimension * 0.72f,
                ),
                cornerRadius = corner,
                size = size,
                blendMode = BlendMode.Screen,
            )

            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(panelInset, panelInset),
                size = androidx.compose.ui.geometry.Size(size.width - panelInset * 2f, size.height - panelInset * 2f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 0.8f),
                blendMode = BlendMode.Screen,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.24f * pulse),
                cornerRadius = corner,
                size = size,
                style = Stroke(width = 1.1f),
                blendMode = BlendMode.Screen,
            )

            val y = size.height * 0.34f
            drawLine(
                color = baseColor.copy(alpha = 0.34f * pulse),
                start = Offset(size.width * 0.18f, y),
                end = Offset(size.width * 0.82f, y),
                strokeWidth = 0.9f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(size.width * 0.25f, size.height - 22.dp.toPx()),
                end = Offset(size.width * 0.75f, size.height - 22.dp.toPx()),
                strokeWidth = 0.8f,
                cap = StrokeCap.Round,
            )

            drawEditorialCorners(baseColor = baseColor, pulse = pulse)
        }

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "MATERIA PRIMORDIAL",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                ),
                color = baseColor.copy(alpha = 0.78f),
                maxLines = 1,
            )

            Text(
                text = current.name.uppercase(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                ),
                color = Color(0xFFF7F1E8).copy(alpha = 0.96f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = current.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                ),
                color = baseColor.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = current.skyLore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                ),
                color = Color(0xFFE9E2D6).copy(alpha = 0.66f + reveal * 0.24f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEditorialCorners(
    baseColor: Color,
    pulse: Float,
) {
    val inset = 14.dp.toPx()
    val long = 30.dp.toPx()
    val alpha = 0.22f * pulse
    val points = listOf(
        Offset(inset, inset),
        Offset(size.width - inset, inset),
        Offset(inset, size.height - inset),
        Offset(size.width - inset, size.height - inset),
    )
    points.forEachIndexed { index, point ->
        val sx = if (index % 2 == 0) 1f else -1f
        val sy = if (index < 2) 1f else -1f
        drawLine(
            color = baseColor.copy(alpha = alpha),
            start = point,
            end = Offset(point.x + long * sx, point.y),
            strokeWidth = 1.0f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
        drawLine(
            color = baseColor.copy(alpha = alpha),
            start = point,
            end = Offset(point.x, point.y + long * sy),
            strokeWidth = 1.0f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
    }

    val chevron = Path().apply {
        moveTo(size.width * 0.47f, size.height - 12.dp.toPx())
        lineTo(size.width * 0.50f, size.height - 7.dp.toPx())
        lineTo(size.width * 0.53f, size.height - 12.dp.toPx())
    }
    drawPath(
        path = chevron,
        color = baseColor.copy(alpha = 0.18f * pulse),
        style = Stroke(width = 1.0f, cap = StrokeCap.Round),
        blendMode = BlendMode.Screen,
    )
}

private fun wave(phase: Float): Float = (sin(phase * LoreTwoPi) + 1f) * 0.5f

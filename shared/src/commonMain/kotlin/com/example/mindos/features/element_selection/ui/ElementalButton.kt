package com.example.mindos.features.element_selection.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.ui.toSanctuaryColor
import com.example.mindos.features.element_selection.domain.ElementMaster
import com.example.mindos.features.element_selection.domain.ElementType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ButtonTwoPi = (PI * 2.0).toFloat()

@Composable
fun ElementalButton(
    element: ElementMaster?,
    time: Float,
    modifier: Modifier = Modifier,
    onClick: (ElementMaster) -> Unit,
) {
    val active = element != null
    var pressed by remember { mutableStateOf(false) }
    val entrance = if (active) 1f else 0.56f
    val press = if (pressed && active) 1f else 0f
    val baseColor = element?.baseColor?.toSanctuaryColor() ?: Color(0xFF7BE7E9)
    val coreColor = element?.coreColor?.toSanctuaryColor() ?: Color(0xFF110B1D)
    val textColor = if (element?.type == ElementType.Light) Color(0xFF181104) else Color(0xFFF7F1E8)

    Box(
        modifier = modifier
            .widthIn(min = 272.dp, max = 356.dp)
            .height(64.dp)
            .graphicsLayer {
                alpha = 0.50f + entrance * 0.50f
                scaleX = 0.970f + entrance * 0.030f - press * 0.018f
                scaleY = 0.940f + entrance * 0.060f - press * 0.030f
                shadowElevation = if (active) 28f + press * 10f else 8f
                ambientShadowColor = baseColor
                spotShadowColor = baseColor
            }
            .clip(RoundedCornerShape(8.dp))
            .elementalButtonInput(
                element = element,
                onPressedChange = { pressed = it },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            val pulse = 0.62f + wave(time * 0.64f) * 0.38f
            val activeAlpha = if (active) 1f else 0.42f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151A2E).copy(alpha = 0.95f),
                        Color(0xFF070910).copy(alpha = 0.98f),
                        Color.Black.copy(alpha = 0.92f),
                    ),
                ),
                cornerRadius = corner,
                size = size,
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.34f * activeAlpha),
                        baseColor.copy(alpha = (0.54f + press * 0.18f) * activeAlpha),
                        coreColor.copy(alpha = 0.40f * activeAlpha),
                    ),
                ),
                cornerRadius = corner,
                size = size,
                blendMode = BlendMode.Screen,
            )

            when (element?.type) {
                ElementType.Gravity -> drawGravityButton(baseColor = baseColor, time = time, press = press)
                ElementType.Shadow -> drawShadowButton(baseColor = baseColor, time = time, press = press)
                ElementType.Light -> drawLightButton(baseColor = baseColor, time = time, press = press)
                ElementType.Lightning -> drawLightningButton(baseColor = baseColor, time = time, press = press)
                ElementType.Air -> drawAirButton(baseColor = baseColor, time = time, press = press)
                null -> drawIdleButton(baseColor = baseColor, time = time)
            }

            drawLine(
                color = Color.White.copy(alpha = 0.14f * activeAlpha),
                start = Offset(18.dp.toPx(), 1.dp.toPx()),
                end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                strokeWidth = 0.9f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = (0.28f + pulse * 0.10f + press * 0.15f) * activeAlpha),
                cornerRadius = corner,
                size = size,
                style = Stroke(width = 1.2f + press * 0.6f),
                blendMode = BlendMode.Screen,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.11f * activeAlpha),
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 0.75f),
                blendMode = BlendMode.Screen,
            )
        }

        Text(
            text = "Seleccionar",
            modifier = Modifier
                .defaultMinSize(minHeight = 24.dp)
                .padding(horizontal = 34.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),
            color = textColor.copy(alpha = if (active) 0.96f else 0.44f),
            maxLines = 1,
        )
    }
}

private fun Modifier.elementalButtonInput(
    element: ElementMaster?,
    onPressedChange: (Boolean) -> Unit,
    onClick: (ElementMaster) -> Unit,
): Modifier = pointerInput(element?.type) {
    detectTapGestures(
        onPress = {
            onPressedChange(true)
            val released = tryAwaitRelease()
            onPressedChange(false)
            if (released) {
                element?.let(onClick)
            }
        },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGravityButton(
    baseColor: Color,
    time: Float,
    press: Float,
) {
    val c = center
    for (i in 0 until 5) {
        val t = fract(time * 0.92f + i * 0.20f)
        drawCircle(
            color = baseColor.copy(alpha = (0.12f + press * 0.05f) * t),
            radius = size.minDimension * (0.18f + (1f - t) * 0.78f),
            center = c,
            style = Stroke(width = 1.0f + t * 2.1f + press),
            blendMode = BlendMode.Screen,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShadowButton(
    baseColor: Color,
    time: Float,
    press: Float,
) {
    for (i in 0 until 5) {
        val y = size.height * (0.22f + i * 0.13f + sin((time * 0.7f + i * 0.11f) * ButtonTwoPi) * 0.035f)
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.26f + press * 0.10f),
                    baseColor.copy(alpha = 0.13f + press * 0.06f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(-size.width * 0.12f + i * size.width * 0.08f, y),
            size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.30f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightButton(
    baseColor: Color,
    time: Float,
    press: Float,
) {
    val pulse = 0.55f + wave(time * 0.8f) * 0.45f + press * 0.18f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.36f * pulse),
                baseColor.copy(alpha = 0.30f * pulse),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.50f, size.height * 0.50f),
            radius = size.width * 0.62f,
        ),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        size = size,
        blendMode = BlendMode.Screen,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightningButton(
    baseColor: Color,
    time: Float,
    press: Float,
) {
    val flicker = 0.26f + wave(time * 5.6f) * 0.74f + press * 0.18f
    for (i in 0 until 6) {
        val x = size.width * (0.10f + i * 0.16f)
        val start = Offset(x, size.height * (0.20f + wave(time + i * 0.07f) * 0.17f))
        val end = Offset(x + size.width * 0.12f, size.height * (0.76f - wave(time + i * 0.13f) * 0.15f))
        drawLine(
            color = if (i % 2 == 0) Color.White.copy(alpha = 0.22f * flicker) else baseColor.copy(alpha = 0.38f * flicker),
            start = start,
            end = end,
            strokeWidth = 1.2f + flicker,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAirButton(
    baseColor: Color,
    time: Float,
    press: Float,
) {
    for (i in 0 until 5) {
        val y = size.height * (0.26f + i * 0.12f)
        drawLine(
            color = baseColor.copy(alpha = 0.14f + i * 0.020f + press * 0.05f),
            start = Offset(size.width * 0.10f, y + sin((time * 0.62f + i * 0.05f) * ButtonTwoPi) * 3.2f),
            end = Offset(size.width * 0.90f, y + cos((time * 0.58f + i * 0.09f) * ButtonTwoPi) * 3.2f),
            strokeWidth = 1.0f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIdleButton(
    baseColor: Color,
    time: Float,
) {
    drawLine(
        color = baseColor.copy(alpha = 0.13f + wave(time * 0.5f) * 0.05f),
        start = Offset(size.width * 0.18f, size.height * 0.50f),
        end = Offset(size.width * 0.82f, size.height * 0.50f),
        strokeWidth = 1.1f,
        cap = StrokeCap.Round,
        blendMode = BlendMode.Screen,
    )
}

private fun wave(phase: Float): Float = (sin(phase * ButtonTwoPi) + 1f) * 0.5f

private fun fract(value: Float): Float {
    val floor = value.toInt()
    return value - floor
}

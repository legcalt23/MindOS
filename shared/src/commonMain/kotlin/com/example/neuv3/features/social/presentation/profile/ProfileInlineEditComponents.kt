package com.example.neuv3.features.social.presentation.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val EditableOverlayFill = Color(0xFF020304)
private val EditableOverlayBorder = Color(0xFF35DBFF)
private val EditableOverlayGlow = Color(0xFF35DBFF).copy(alpha = 0.26f)

@Composable
internal fun EditableOverlayButton(
    modifier: Modifier = Modifier,
    text: String = "EDITAR",
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    val orbitTransition = rememberInfiniteTransition(label = "EditableOverlayOrbit")
    val orbitDegrees by orbitTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "EditableOverlayOrbitDegrees",
    )

    Box(
        modifier = modifier
            .shadow(18.dp, shape)
            .clip(shape)
            .background(EditableOverlayFill)
            .drawWithCache {
                val strokeWidth = 2.dp.toPx()
                val glowStrokeWidth = strokeWidth + 3.dp.toPx()
                val inset = glowStrokeWidth / 2f
                val drawSize = Size(
                    width = size.width - (inset * 2f),
                    height = size.height - (inset * 2f),
                )
                val cornerRadius = CornerRadius(drawSize.height / 2f, drawSize.height / 2f)
                val orbitBrush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.46f to Color.White.copy(alpha = 0.10f),
                        0.50f to Color.White,
                        0.54f to Color.White.copy(alpha = 0.12f),
                        0.60f to Color.Transparent,
                        1.00f to Color.Transparent,
                    ),
                )

                onDrawWithContent {
                    drawRoundRect(
                        color = EditableOverlayGlow,
                        topLeft = Offset(inset, inset),
                        size = drawSize,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = glowStrokeWidth),
                    )
                    drawContent()
                    drawRoundRect(
                        color = EditableOverlayBorder,
                        topLeft = Offset(inset, inset),
                        size = drawSize,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = strokeWidth),
                    )
                    rotate(degrees = orbitDegrees, pivot = center) {
                        drawRoundRect(
                            brush = orbitBrush,
                            topLeft = Offset(inset, inset),
                            size = drawSize,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = strokeWidth + 1.dp.toPx()),
                        )
                    }
                }
            }
            .border(1.dp, EditableOverlayBorder.copy(alpha = 0.34f), shape)
            .defaultMinSize(minWidth = 120.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = text
            }
            .padding(horizontal = 26.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

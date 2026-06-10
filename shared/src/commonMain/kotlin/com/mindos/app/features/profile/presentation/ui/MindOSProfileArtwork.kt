package com.mindos.app.features.profile.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.features.profile.domain.audio.AudioWaveform

/**
 * Premium gradient placeholder for the cover (`foto_portada`) when no photo is
 * set — a fog/mountain silhouette mirroring the Perfil.html "fog" cover.
 */
@Composable
fun CoverArtworkFallback(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color(0xFF3A4150),
                    0.55f to Color(0xFF1D2027),
                    1f to AppColors.ProfileCanvas,
                ),
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x40B8C2CF), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                radius = size.minDimension * 0.7f,
            ),
        )
        val far = Path().apply {
            moveTo(0f, size.height * 0.62f)
            lineTo(size.width * 0.2f, size.height * 0.42f)
            lineTo(size.width * 0.37f, size.height * 0.56f)
            lineTo(size.width * 0.6f, size.height * 0.36f)
            lineTo(size.width * 0.8f, size.height * 0.55f)
            lineTo(size.width, size.height * 0.46f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(far, color = Color(0xFF0E1116))
        val near = Path().apply {
            moveTo(0f, size.height * 0.78f)
            lineTo(size.width * 0.25f, size.height * 0.66f)
            lineTo(size.width * 0.5f, size.height * 0.75f)
            lineTo(size.width * 0.75f, size.height * 0.64f)
            lineTo(size.width, size.height * 0.75f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(near, color = Color(0xFF06070A))
    }
}

/** Warm radial orb placeholder for the avatar (`foto_perfil`) when no photo is set. */
@Composable
fun PortraitArtworkFallback(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFE8D4B8),
                    0.3f to Color(0xFFA87B5A),
                    0.7f to Color(0xFF4A3328),
                    1f to Color(0xFF1A1410),
                ),
                center = Offset(size.width * 0.38f, size.height * 0.32f),
                radius = size.maxDimension * 0.78f,
            ),
        )
    }
}

/** Minimal single-line bear glyph for the donation block. */
@Composable
fun BearGlyph(modifier: Modifier = Modifier, color: Color = AppColors.ProfileTextPrimary) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.06f
        val strokeStyle = Stroke(width = stroke)
        // ears
        drawCircle(color = color, radius = w * 0.11f, center = Offset(w * 0.27f, h * 0.27f), style = strokeStyle)
        drawCircle(color = color, radius = w * 0.11f, center = Offset(w * 0.73f, h * 0.27f), style = strokeStyle)
        // head
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.21f, h * 0.4f),
            size = Size(w * 0.58f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.24f, w * 0.24f),
            style = strokeStyle,
        )
        // eyes
        drawCircle(color = color, radius = w * 0.035f, center = Offset(w * 0.4f, h * 0.56f))
        drawCircle(color = color, radius = w * 0.035f, center = Offset(w * 0.6f, h * 0.56f))
    }
}

/**
 * Premium fine-bar waveform. When [bars] carries no signal a single flat line is
 * drawn instead (the "no audio yet" state).
 */
@Composable
fun ProfileWaveformBars(
    bars: List<Float>,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(WaveformHeight.dp), contentAlignment = Alignment.CenterStart) {
        if (!AudioWaveform.hasSignal(bars)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp)),
            ) {
                Box(modifier = Modifier.fillMaxSize().background(inactiveColor))
            }
            return@Box
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(BarGap.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bars.forEachIndexed { index, raw ->
                val fraction = index.toFloat() / bars.size
                val color = if (fraction <= progress) activeColor else inactiveColor
                val barHeight = (raw.coerceIn(0f, 1f)).coerceAtLeast(AudioWaveform.MinBarHeight)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(barHeight)
                        .width(BarWidth.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
    }
}

private const val WaveformHeight = 32
private const val BarGap = 2
private const val BarWidth = 2

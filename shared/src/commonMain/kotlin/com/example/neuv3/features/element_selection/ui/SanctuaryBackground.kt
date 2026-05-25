package com.example.neuv3.features.element_selection.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.neuv3.core.ui.toSanctuaryColor
import com.example.neuv3.features.element_selection.domain.ElementMaster
import com.example.neuv3.features.element_selection.domain.ElementType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val BackgroundParticles = 132
private const val ConstellationNodes = 34
private const val TwoPi = (PI * 2.0).toFloat()

@Immutable
private data class SanctuaryParticle(
    val xSeed: Float,
    val ySeed: Float,
    val speed: Float,
    val radius: Float,
    val drift: Float,
    val warm: Float,
)

@Immutable
private data class ConstellationNode(
    val x: Float,
    val y: Float,
    val group: Int,
)

@Composable
fun SanctuaryBackground(
    selectedElement: ElementMaster?,
    time: Float,
    impact: Float,
    parallax: Offset,
    modifier: Modifier = Modifier,
) {
    val particles = remember {
        List(BackgroundParticles) { index ->
            SanctuaryParticle(
                xSeed = fract(index * 0.38197f + 0.17f),
                ySeed = fract(index * 0.61803f + 0.29f),
                speed = 0.16f + fract(index * 0.173f) * 0.38f,
                radius = 0.45f + fract(index * 0.417f) * 1.45f,
                drift = 0.35f + fract(index * 0.271f) * 0.95f,
                warm = fract(index * 0.619f + 0.11f),
            )
        }
    }
    val nodes = remember {
        List(ConstellationNodes) { index ->
            ConstellationNode(
                x = 0.08f + fract(index * 0.329f + 0.07f) * 0.84f,
                y = 0.10f + fract(index * 0.217f + 0.19f) * 0.58f,
                group = index % 5,
            )
        }
    }
    val baseColor = selectedElement?.baseColor?.toSanctuaryColor() ?: Color(0xFF7BE7E9)
    val coreColor = selectedElement?.coreColor?.toSanctuaryColor() ?: Color(0xFF110B1D)

    Canvas(modifier = modifier) {
        drawDeepCosmos(baseColor = baseColor, coreColor = coreColor, time = time, impact = impact, parallax = parallax)
        drawCosmicVeils(baseColor = baseColor, type = selectedElement?.type, time = time, impact = impact, parallax = parallax)
        drawConstellationGeometry(nodes = nodes, baseColor = baseColor, time = time, parallax = parallax)
        drawFineParticles(particles = particles, baseColor = baseColor, time = time, impact = impact, parallax = parallax)
        drawObsidianArchitecture(baseColor = baseColor, time = time, impact = impact, parallax = parallax)
        drawGlobalImpact(baseColor = baseColor, impact = impact)
    }
}

private fun DrawScope.drawDeepCosmos(
    baseColor: Color,
    coreColor: Color,
    time: Float,
    impact: Float,
    parallax: Offset,
) {
    drawRect(
        brush = Brush.verticalGradient(
            0.00f to Color(0xFF060816),
            0.26f to Color(0xFF0B1020),
            0.58f to Color(0xFF110B1D),
            1.00f to Color(0xFF03040B),
        ),
        size = size,
    )

    val wide = max(size.width, size.height)
    val upperVault = Offset(
        x = size.width * (0.22f + parallax.x * 0.026f),
        y = size.height * (0.16f + parallax.y * 0.016f),
    )
    val ritualCore = Offset(
        x = size.width * (0.54f + parallax.x * 0.034f),
        y = size.height * (0.47f + parallax.y * 0.020f),
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1D2D58).copy(alpha = 0.34f),
                Color(0xFF24172F).copy(alpha = 0.20f),
                Color.Transparent,
            ),
            center = upperVault,
            radius = wide * 0.72f,
        ),
        size = size,
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.18f + impact * 0.18f),
                coreColor.copy(alpha = 0.13f),
                Color(0xFF050612).copy(alpha = 0.03f),
                Color.Transparent,
            ),
            center = ritualCore,
            radius = wide * (0.52f + wave(time * 0.36f) * 0.035f),
        ),
        size = size,
        blendMode = BlendMode.Screen,
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.38f),
                Color.Black.copy(alpha = 0.82f),
            ),
            center = Offset(size.width * 0.50f, size.height * 0.42f),
            radius = wide * 0.82f,
        ),
        size = size,
    )
}

private fun DrawScope.drawCosmicVeils(
    baseColor: Color,
    type: ElementType?,
    time: Float,
    impact: Float,
    parallax: Offset,
) {
    val mistAlpha = when (type) {
        ElementType.Gravity -> 0.18f
        ElementType.Light -> 0.10f
        ElementType.Lightning -> 0.15f
        ElementType.Shadow -> 0.17f
        ElementType.Air -> 0.14f
        null -> 0.12f
    }

    for (i in 0 until 7) {
        val phase = time * (0.38f + i * 0.025f) + i * 0.127f
        val left = -size.width * (0.24f + i * 0.03f) + wave(phase) * size.width * 0.24f + parallax.x * (18f + i * 4f)
        val top = size.height * (0.22f + i * 0.073f + sin(phase * TwoPi) * 0.012f) + parallax.y * (8f + i * 2f)
        val veilSize = Size(
            width = size.width * (0.86f + i * 0.12f),
            height = size.height * (0.12f + i * 0.018f),
        )
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF7D86B8).copy(alpha = mistAlpha * 0.10f),
                    baseColor.copy(alpha = mistAlpha * (0.20f + impact * 0.22f)),
                    Color.White.copy(alpha = mistAlpha * 0.055f),
                    Color.Transparent,
                ),
                startX = left,
                endX = left + veilSize.width,
            ),
            topLeft = Offset(left, top),
            size = veilSize,
            blendMode = BlendMode.Screen,
        )
    }
}

private fun DrawScope.drawConstellationGeometry(
    nodes: List<ConstellationNode>,
    baseColor: Color,
    time: Float,
    parallax: Offset,
) {
    val driftX = parallax.x * 10f
    val driftY = parallax.y * 6f
    nodes.forEachIndexed { index, node ->
        val p = Offset(
            x = node.x * size.width + driftX * (0.6f + node.group * 0.10f),
            y = node.y * size.height + driftY * (0.6f + node.group * 0.08f),
        )
        val pulse = wave(time * 0.9f + index * 0.071f)
        drawCircle(
            color = Color.White.copy(alpha = 0.035f + pulse * 0.045f),
            radius = 0.75f + pulse * 0.85f,
            center = p,
            blendMode = BlendMode.Screen,
        )

        val next = nodes.getOrNull(index + 1)
        if (next != null && next.group == node.group) {
            val q = Offset(
                x = next.x * size.width + driftX * (0.6f + next.group * 0.10f),
                y = next.y * size.height + driftY * (0.6f + next.group * 0.08f),
            )
            drawLine(
                color = baseColor.copy(alpha = 0.018f + pulse * 0.018f),
                start = p,
                end = q,
                strokeWidth = 0.8f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen,
            )
        }
    }

    val center = Offset(size.width * 0.50f + parallax.x * 8f, size.height * 0.50f + parallax.y * 5f)
    for (i in 0 until 3) {
        val r = size.minDimension * (0.28f + i * 0.12f)
        drawCircle(
            color = Color.White.copy(alpha = 0.018f - i * 0.003f),
            radius = r,
            center = center,
            style = Stroke(width = 0.8f),
            blendMode = BlendMode.Screen,
        )
    }
}

private fun DrawScope.drawFineParticles(
    particles: List<SanctuaryParticle>,
    baseColor: Color,
    time: Float,
    impact: Float,
    parallax: Offset,
) {
    particles.forEachIndexed { index, particle ->
        val ascent = fract(particle.ySeed - time * particle.speed)
        val x = particle.xSeed * size.width +
            sin((time * particle.drift + particle.xSeed) * TwoPi) * size.width * 0.020f +
            parallax.x * 22f
        val y = ascent * size.height + parallax.y * 12f
        val warmColor = if (particle.warm > 0.62f) Color(0xFFF5E7A1) else Color.White
        val color = if (index % 5 == 0) baseColor else warmColor
        val alpha = (0.055f + wave(time * 1.2f + index * 0.071f) * 0.16f + impact * 0.12f) *
            (1f - (y / size.height).coerceIn(0f, 1f) * 0.16f)

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = particle.radius + impact * 1.4f,
            center = Offset(x.wrap(size.width), y.wrap(size.height)),
            blendMode = BlendMode.Screen,
        )

        if (index % 11 == 0) {
            drawLine(
                color = color.copy(alpha = alpha * 0.18f),
                start = Offset(x.wrap(size.width), y.wrap(size.height) + 10f),
                end = Offset(x.wrap(size.width) + parallax.x * 5f, y.wrap(size.height) - 18f),
                strokeWidth = 0.8f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen,
            )
        }
    }
}

private fun DrawScope.drawObsidianArchitecture(
    baseColor: Color,
    time: Float,
    impact: Float,
    parallax: Offset,
) {
    val bottom = size.height
    val deckTop = size.height * 0.78f
    val centerX = size.width * (0.50f + parallax.x * 0.006f)
    val platform = Path().apply {
        moveTo(0f, bottom)
        lineTo(size.width * 0.12f, deckTop + size.height * 0.075f)
        lineTo(size.width * 0.31f, deckTop + size.height * 0.030f)
        lineTo(centerX, deckTop + size.height * 0.006f)
        lineTo(size.width * 0.69f, deckTop + size.height * 0.030f)
        lineTo(size.width * 0.88f, deckTop + size.height * 0.075f)
        lineTo(size.width, bottom)
        close()
    }
    val innerFacet = Path().apply {
        moveTo(size.width * 0.22f, bottom)
        lineTo(size.width * 0.36f, deckTop + size.height * 0.092f)
        lineTo(centerX, deckTop + size.height * 0.040f)
        lineTo(size.width * 0.64f, deckTop + size.height * 0.092f)
        lineTo(size.width * 0.78f, bottom)
        close()
    }

    drawPath(
        path = platform,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF24172F).copy(alpha = 0.30f),
                Color(0xFF05050A).copy(alpha = 0.92f),
            ),
            startY = deckTop,
            endY = bottom,
        ),
    )
    drawPath(
        path = innerFacet,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B1020).copy(alpha = 0.52f),
                Color.Black.copy(alpha = 0.72f),
            ),
            startY = deckTop,
            endY = bottom,
        ),
    )

    val pulse = 0.55f + wave(time * 0.72f) * 0.45f + impact * 0.35f
    drawLine(
        color = baseColor.copy(alpha = 0.12f * pulse),
        start = Offset(size.width * 0.18f, deckTop + size.height * 0.090f),
        end = Offset(size.width * 0.82f, deckTop + size.height * 0.090f),
        strokeWidth = 1.2f + impact * 2f,
        cap = StrokeCap.Round,
        blendMode = BlendMode.Screen,
    )
    for (i in 0 until 5) {
        val t = i / 4f
        val x = size.width * (0.22f + t * 0.56f)
        drawLine(
            color = Color.White.copy(alpha = 0.035f),
            start = Offset(x, bottom),
            end = Offset(centerX + (x - centerX) * 0.30f + parallax.x * 6f, deckTop + size.height * 0.050f),
            strokeWidth = 0.9f,
        )
    }
}

private fun DrawScope.drawGlobalImpact(
    baseColor: Color,
    impact: Float,
) {
    if (impact <= 0.002f) return
    drawRect(color = Color.White.copy(alpha = impact * 0.07f), size = size, blendMode = BlendMode.Screen)
    drawCircle(
        color = baseColor.copy(alpha = impact * 0.16f),
        radius = size.maxDimension * (0.18f + (1f - impact) * 0.72f),
        center = Offset(size.width * 0.50f, size.height * 0.52f),
        style = Stroke(width = 1.5f + impact * 7f),
        blendMode = BlendMode.Screen,
    )
}

private fun wave(phase: Float): Float = (sin(phase * TwoPi) + 1f) * 0.5f

private fun fract(value: Float): Float {
    val floor = value.toInt()
    return value - floor
}

private fun Float.wrap(limit: Float): Float {
    if (limit <= 0f) return 0f
    val wrapped = this % limit
    return if (wrapped < 0f) wrapped + limit else wrapped
}

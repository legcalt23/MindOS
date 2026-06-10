package com.mindos.app.features.element_selection.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.mindos.app.core.ui.toSanctuaryColor
import com.mindos.app.features.element_selection.domain.ElementMaster
import com.mindos.app.features.element_selection.domain.ElementType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val OrbTwoPi = (PI * 2.0).toFloat()

@Immutable
private data class OrbVisualPalette(
    val base: Color,
    val bright: Color,
    val core: Color,
    val shadow: Color,
)

@Composable
fun OrbComponent(
    master: ElementMaster,
    time: Float,
    selected: Boolean,
    intense: Boolean,
    impact: Float,
    modifier: Modifier = Modifier,
    onTap: (ElementMaster) -> Unit,
    onLongPressStarted: (ElementMaster) -> Unit,
    onLongPressEnded: (ElementMaster) -> Unit,
) {
    val fallbackBase = master.baseColor.toSanctuaryColor()
    val fallbackCore = master.coreColor.toSanctuaryColor()
    val palette = master.type.orbPalette(fallbackBase, fallbackCore)

    Canvas(
        modifier = modifier.orbInput(
            master = master,
            onTap = onTap,
            onLongPressStarted = onLongPressStarted,
            onLongPressEnded = onLongPressEnded,
        ),
    ) {
        val speed = when {
            intense -> 1.72f
            selected -> 1.04f
            else -> 0.58f
        }
        val localTime = time * speed + impact * 0.032f
        val radius = size.minSide * (0.265f + if (selected) 0.010f else 0f)
        val c = center

        drawOrbAtmosphere(palette = palette, radius = radius, time = localTime, selected = selected, intense = intense, impact = impact)
        drawOuterSeal(palette = palette, radius = radius, time = localTime, selected = selected, intense = intense, impact = impact)
        drawVolumetricBody(palette = palette, radius = radius, time = localTime, selected = selected, impact = impact)

        when (master.type) {
            ElementType.Gravity -> drawGravityOrb(palette, radius, c, localTime, intense, impact)
            ElementType.Shadow -> drawShadowOrb(palette, radius, c, localTime, intense, impact)
            ElementType.Light -> drawLightOrb(palette, radius, c, localTime, intense, impact)
            ElementType.Lightning -> drawLightningOrb(palette, radius, c, localTime, intense, impact)
            ElementType.Air -> drawEtherOrb(palette, radius, c, localTime, intense, impact)
        }

        drawGlassRim(palette = palette, radius = radius, time = localTime, selected = selected, impact = impact)
        drawSpecularDetail(palette = palette, radius = radius, time = localTime, impact = impact)
    }
}

private fun Modifier.orbInput(
    master: ElementMaster,
    onTap: (ElementMaster) -> Unit,
    onLongPressStarted: (ElementMaster) -> Unit,
    onLongPressEnded: (ElementMaster) -> Unit,
): Modifier = pointerInput(master.type) {
    detectTapGestures(
        onPress = {
            try {
                tryAwaitRelease()
            } finally {
                onLongPressEnded(master)
            }
        },
        onLongPress = {
            onLongPressStarted(master)
        },
        onTap = {
            onTap(master)
        },
    )
}

private fun ElementType.orbPalette(
    fallbackBase: Color,
    fallbackCore: Color,
): OrbVisualPalette = when (this) {
    ElementType.Gravity -> OrbVisualPalette(
        base = Color(0xFFC02C72),
        bright = Color(0xFFFF5AA5),
        core = Color(0xFF030204),
        shadow = Color(0xFF4A0E2D),
    )
    ElementType.Light -> OrbVisualPalette(
        base = Color(0xFFF5E7A1),
        bright = Color(0xFFFFF6D6),
        core = Color(0xFFB79B47),
        shadow = Color(0xFF241A07),
    )
    ElementType.Lightning -> OrbVisualPalette(
        base = Color(0xFF9C7BFF),
        bright = Color(0xFFC9B8FF),
        core = Color(0xFF5D3BEA),
        shadow = Color(0xFF150B35),
    )
    ElementType.Shadow -> OrbVisualPalette(
        base = Color(0xFF4B3B6F),
        bright = Color(0xFF8B78C4),
        core = Color(0xFF090712),
        shadow = Color(0xFF1C132B),
    )
    ElementType.Air -> OrbVisualPalette(
        base = Color(0xFF7BE7E9),
        bright = Color(0xFFB8FFFF),
        core = Color(0xFF2FA7B9),
        shadow = Color(0xFF062329),
    )
}.let {
    if (fallbackBase == Color.Unspecified || fallbackCore == Color.Unspecified) it else it
}

private fun DrawScope.drawOrbAtmosphere(
    palette: OrbVisualPalette,
    radius: Float,
    time: Float,
    selected: Boolean,
    intense: Boolean,
    impact: Float,
) {
    val breath = wave(time * 0.62f)
    val auraRadius = radius * (2.15f + breath * 0.16f + impact * 0.38f + if (selected) 0.36f else 0f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.bright.copy(alpha = if (selected) 0.20f + impact * 0.20f else 0.080f),
                palette.base.copy(alpha = if (selected) 0.16f else 0.060f),
                palette.shadow.copy(alpha = 0.050f),
                Color.Transparent,
            ),
            center = center,
            radius = auraRadius,
        ),
        radius = auraRadius,
        center = center,
        blendMode = BlendMode.Screen,
    )

    if (selected || intense) {
        for (i in 0 until 3) {
            val ring = radius * (1.62f + i * 0.29f + wave(time * 0.72f + i * 0.18f) * 0.045f)
            drawCircle(
                color = palette.base.copy(alpha = (0.10f + impact * 0.09f) / (i + 1)),
                radius = ring,
                center = center,
                style = Stroke(width = 1.0f + i * 0.45f + if (intense) 0.8f else 0f),
                blendMode = BlendMode.Screen,
            )
        }
    }
}

private fun DrawScope.drawOuterSeal(
    palette: OrbVisualPalette,
    radius: Float,
    time: Float,
    selected: Boolean,
    intense: Boolean,
    impact: Float,
) {
    val c = center
    val alpha = if (selected) 0.26f else 0.12f
    val outer = radius * (1.36f + wave(time * 0.44f) * 0.025f + impact * 0.03f)
    val inner = radius * 1.16f
    drawCircle(
        color = palette.bright.copy(alpha = alpha + impact * 0.12f),
        radius = outer,
        center = c,
        style = Stroke(width = if (selected) 1.35f else 0.85f),
        blendMode = BlendMode.Screen,
    )
    drawCircle(
        color = palette.shadow.copy(alpha = 0.22f),
        radius = inner,
        center = c,
        style = Stroke(width = 1.1f),
    )

    val tickCount = if (selected || intense) 32 else 24
    for (i in 0 until tickCount) {
        val a = time * OrbTwoPi * 0.055f + i / tickCount.toFloat() * OrbTwoPi
        val major = i % 4 == 0
        val startR = outer + radius * if (major) 0.045f else 0.020f
        val endR = outer + radius * if (major) 0.170f else 0.090f
        drawLine(
            color = palette.bright.copy(alpha = (if (major) 0.22f else 0.11f) + impact * 0.05f),
            start = Offset(c.x + cos(a) * startR, c.y + sin(a) * startR),
            end = Offset(c.x + cos(a) * endR, c.y + sin(a) * endR),
            strokeWidth = if (major) 1.1f else 0.75f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
    }

    drawPolygonSeal(
        radius = outer * 0.94f,
        sides = 6,
        angle = -time * OrbTwoPi * 0.025f,
        color = palette.base.copy(alpha = if (selected) 0.16f else 0.060f),
    )
    drawPolygonSeal(
        radius = outer * 0.72f,
        sides = 3,
        angle = time * OrbTwoPi * 0.032f,
        color = palette.bright.copy(alpha = if (selected) 0.13f else 0.050f),
    )
}

private fun DrawScope.drawPolygonSeal(
    radius: Float,
    sides: Int,
    angle: Float,
    color: Color,
) {
    if (sides < 3) return
    var previous: Offset? = null
    var first: Offset? = null
    for (i in 0..sides) {
        val a = angle + i / sides.toFloat() * OrbTwoPi
        val point = Offset(center.x + cos(a) * radius, center.y + sin(a) * radius)
        if (i == 0) first = point
        previous?.let {
            drawLine(
                color = color,
                start = it,
                end = if (i == sides) first ?: point else point,
                strokeWidth = 0.9f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen,
            )
        }
        previous = point
    }
}

private fun DrawScope.drawVolumetricBody(
    palette: OrbVisualPalette,
    radius: Float,
    time: Float,
    selected: Boolean,
    impact: Float,
) {
    val c = center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.42f),
                palette.shadow.copy(alpha = 0.52f),
                Color.Transparent,
            ),
            center = Offset(c.x + radius * 0.23f, c.y + radius * 0.38f),
            radius = radius * 1.48f,
        ),
        radius = radius * 1.10f,
        center = c,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (selected) 0.30f else 0.20f),
                palette.bright.copy(alpha = 0.36f + impact * 0.13f),
                palette.base.copy(alpha = 0.34f),
                palette.shadow.copy(alpha = 0.88f),
                Color.Black.copy(alpha = 0.72f),
            ),
            center = Offset(
                x = c.x - radius * (0.24f + sin(time * OrbTwoPi) * 0.018f),
                y = c.y - radius * 0.28f,
            ),
            radius = radius * 1.28f,
        ),
        radius = radius * 1.02f,
        center = c,
    )
}

private fun DrawScope.drawGravityOrb(
    palette: OrbVisualPalette,
    radius: Float,
    c: Offset,
    time: Float,
    intense: Boolean,
    impact: Float,
) {
    for (i in 0 until 8) {
        val collapse = fract(time * (0.82f + if (intense) 0.28f else 0f) + i / 8f)
        val ringRadius = radius * (1.22f - collapse * 0.48f)
        drawCircle(
            color = palette.bright.copy(alpha = (0.20f + impact * 0.16f) * collapse),
            radius = ringRadius,
            center = c,
            style = Stroke(width = 0.9f + (1f - collapse) * 3.2f),
            blendMode = BlendMode.Screen,
        )
    }

    for (i in 0 until 5) {
        val a = time * OrbTwoPi * (0.16f + i * 0.018f) + i * 0.87f
        val w = radius * (1.05f - i * 0.075f)
        val h = radius * (0.34f + i * 0.055f)
        drawArc(
            color = palette.base.copy(alpha = 0.14f + impact * 0.08f),
            startAngle = a.toDegrees(),
            sweepAngle = 104f + wave(time + i * 0.13f) * 74f,
            useCenter = false,
            topLeft = Offset(c.x - w, c.y - h),
            size = Size(w * 2f, h * 2f),
            style = Stroke(width = 1.0f + i * 0.15f, cap = StrokeCap.Round),
            blendMode = BlendMode.Screen,
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 1f),
                Color.Black.copy(alpha = 0.96f),
                palette.shadow.copy(alpha = 0.82f),
                palette.base.copy(alpha = 0.20f),
                Color.Transparent,
            ),
            center = c,
            radius = radius * 1.0f,
        ),
        radius = radius * 0.84f,
        center = c,
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.98f),
        radius = radius * (0.38f + wave(time * 1.1f) * 0.030f + impact * 0.025f),
        center = c,
    )
}

private fun DrawScope.drawShadowOrb(
    palette: OrbVisualPalette,
    radius: Float,
    c: Offset,
    time: Float,
    intense: Boolean,
    impact: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF05040A).copy(alpha = 0.98f),
                palette.shadow.copy(alpha = 0.88f),
                palette.base.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = c,
            radius = radius * 1.08f,
        ),
        radius = radius * 0.98f,
        center = c,
    )

    for (i in 0 until 10) {
        val phase = time * (0.26f + i * 0.011f) + i * 0.092f
        val a = phase * OrbTwoPi
        val w = radius * (0.38f + wave(phase) * 0.38f) * if (intense) 1.15f else 1f
        val h = radius * (0.16f + wave(phase + 0.21f) * 0.22f)
        val center = Offset(
            x = c.x + cos(a) * radius * 0.22f,
            y = c.y + sin(a * 1.23f) * radius * 0.20f,
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.34f + impact * 0.12f),
                    palette.bright.copy(alpha = 0.075f),
                    Color.Transparent,
                ),
                center = center,
                radius = w,
            ),
            topLeft = Offset(center.x - w, center.y - h),
            size = Size(w * 2f, h * 2f),
        )
    }

    for (i in 0 until 4) {
        val r = radius * (0.42f + i * 0.13f)
        drawArc(
            color = palette.bright.copy(alpha = 0.08f + impact * 0.05f),
            startAngle = (time * -180f + i * 82f) % 360f,
            sweepAngle = 92f + wave(time + i * 0.16f) * 120f,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = 1.0f + i * 0.22f, cap = StrokeCap.Round),
            blendMode = BlendMode.Screen,
        )
    }
}

private fun DrawScope.drawLightOrb(
    palette: OrbVisualPalette,
    radius: Float,
    c: Offset,
    time: Float,
    intense: Boolean,
    impact: Float,
) {
    val pulse = 0.70f + wave(time * 0.55f) * 0.30f + impact * 0.22f
    for (i in 0 until 20) {
        val angle = i / 20f * OrbTwoPi + time * OrbTwoPi * 0.035f
        val inner = radius * 0.34f
        val outer = radius * (1.52f + wave(time + i * 0.05f) * 0.20f + if (intense) 0.16f else 0f)
        drawLine(
            color = palette.base.copy(alpha = 0.055f * pulse),
            start = Offset(c.x + cos(angle) * inner, c.y + sin(angle) * inner),
            end = Offset(c.x + cos(angle) * outer, c.y + sin(angle) * outer),
            strokeWidth = 0.9f + pulse * 0.75f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                palette.bright.copy(alpha = 0.86f),
                palette.base.copy(alpha = 0.58f),
                palette.core.copy(alpha = 0.26f),
                Color.Transparent,
            ),
            center = Offset(c.x - radius * 0.07f, c.y - radius * 0.08f),
            radius = radius * (1.12f + pulse * 0.12f),
        ),
        radius = radius * (0.98f + pulse * 0.030f),
        center = c,
        blendMode = BlendMode.Screen,
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.24f + impact * 0.12f),
        radius = radius * (0.28f + wave(time * 0.9f) * 0.025f),
        center = c,
        blendMode = BlendMode.Screen,
    )
}

private fun DrawScope.drawLightningOrb(
    palette: OrbVisualPalette,
    radius: Float,
    c: Offset,
    time: Float,
    intense: Boolean,
    impact: Float,
) {
    val flicker = 0.42f + wave(time * 5.2f) * 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.26f + flicker * 0.18f),
                palette.bright.copy(alpha = 0.34f),
                palette.core.copy(alpha = 0.50f),
                palette.shadow.copy(alpha = 0.72f),
                Color.Transparent,
            ),
            center = c,
            radius = radius * 1.06f,
        ),
        radius = radius * 0.98f,
        center = c,
        blendMode = BlendMode.Screen,
    )

    val bolts = if (intense) 11 else 8
    for (i in 0 until bolts) {
        val angle = time * OrbTwoPi * (0.36f + i * 0.012f) + i * 0.79f
        val start = Offset(
            x = c.x + cos(angle) * radius * (0.12f + i % 3 * 0.035f),
            y = c.y + sin(angle) * radius * (0.12f + i % 3 * 0.035f),
        )
        val end = Offset(
            x = c.x + cos(angle + 1.56f) * radius * (0.70f + wave(time + i * 0.10f) * 0.25f),
            y = c.y + sin(angle + 1.56f) * radius * (0.70f + wave(time + i * 0.14f) * 0.25f),
        )
        drawJaggedBolt(
            start = start,
            end = end,
            color = if (i % 3 == 0) Color.White else palette.bright,
            seed = time + i * 0.13f,
            intensity = flicker * 0.82f + impact * 0.52f,
        )
    }
}

private fun DrawScope.drawEtherOrb(
    palette: OrbVisualPalette,
    radius: Float,
    c: Offset,
    time: Float,
    intense: Boolean,
    impact: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                palette.bright.copy(alpha = 0.32f),
                palette.base.copy(alpha = 0.30f),
                palette.core.copy(alpha = 0.18f),
                palette.shadow.copy(alpha = 0.64f),
            ),
            center = Offset(c.x - radius * 0.18f, c.y - radius * 0.24f),
            radius = radius * 1.18f,
        ),
        radius = radius * 0.98f,
        center = c,
        blendMode = BlendMode.Screen,
    )

    for (i in 0 until 9) {
        val arcRadius = radius * (0.36f + i * 0.066f + impact * 0.025f)
        drawArc(
            color = palette.bright.copy(alpha = 0.095f + i * 0.010f + if (intense) 0.05f else 0f),
            startAngle = (time * 150f + i * 36f) % 360f,
            sweepAngle = 62f + wave(time + i * 0.085f) * 92f,
            useCenter = false,
            topLeft = Offset(c.x - arcRadius, c.y - arcRadius * 0.78f),
            size = Size(arcRadius * 2f, arcRadius * 1.56f),
            style = Stroke(width = 0.95f + i * 0.10f + if (intense) 0.72f else 0f, cap = StrokeCap.Round),
            blendMode = BlendMode.Screen,
        )
    }

    for (i in 0 until 5) {
        val p = Offset(
            x = c.x + sin((time + i * 0.17f) * OrbTwoPi) * radius * (0.18f + i * 0.050f),
            y = c.y + cos((time * 0.72f + i * 0.13f) * OrbTwoPi) * radius * (0.16f + i * 0.045f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    palette.bright.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = p,
                radius = radius * 0.22f,
            ),
            radius = radius * 0.22f,
            center = p,
            blendMode = BlendMode.Screen,
        )
    }
}

private fun DrawScope.drawGlassRim(
    palette: OrbVisualPalette,
    radius: Float,
    time: Float,
    selected: Boolean,
    impact: Float,
) {
    drawCircle(
        color = Color.White.copy(alpha = if (selected) 0.24f + impact * 0.12f else 0.14f),
        radius = radius * (1.02f + wave(time * 0.42f) * 0.008f),
        center = center,
        style = Stroke(width = 1.2f),
        blendMode = BlendMode.Screen,
    )
    drawCircle(
        color = palette.shadow.copy(alpha = 0.34f),
        radius = radius * 1.04f,
        center = Offset(center.x + radius * 0.025f, center.y + radius * 0.035f),
        style = Stroke(width = 1.8f),
    )
}

private fun DrawScope.drawSpecularDetail(
    palette: OrbVisualPalette,
    radius: Float,
    time: Float,
    impact: Float,
) {
    val point = Offset(
        x = center.x - radius * 0.31f + sin(time * OrbTwoPi) * radius * 0.035f,
        y = center.y - radius * 0.38f + cos(time * OrbTwoPi * 0.7f) * radius * 0.026f,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.48f + impact * 0.20f),
                palette.bright.copy(alpha = 0.16f),
                Color.Transparent,
            ),
            center = point,
            radius = radius * 0.32f,
        ),
        radius = radius * 0.32f,
        center = point,
        blendMode = BlendMode.Screen,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = Offset(center.x - radius * 0.17f, center.y - radius * 0.23f),
            radius = radius * 0.76f,
        ),
        radius = radius * 0.76f,
        center = center,
        blendMode = BlendMode.Screen,
    )
}

private fun DrawScope.drawJaggedBolt(
    start: Offset,
    end: Offset,
    color: Color,
    seed: Float,
    intensity: Float,
) {
    val normal = Offset(-(end.y - start.y), end.x - start.x).unit()
    var previous = start
    val segments = 5
    for (step in 1..segments) {
        val t = step / segments.toFloat()
        val bend = sin((seed + t * 1.93f) * OrbTwoPi) * size.minSide * 0.030f
        val current = Offset(
            x = start.x + (end.x - start.x) * t,
            y = start.y + (end.y - start.y) * t,
        ) + normal * bend
        drawLine(
            color = color.copy(alpha = 0.12f * intensity),
            start = previous,
            end = current,
            strokeWidth = 5.8f * intensity,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
        drawLine(
            color = color.copy(alpha = 0.68f * intensity),
            start = previous,
            end = current,
            strokeWidth = 1.1f + intensity * 0.8f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
        previous = current
    }
}

private val Size.minSide: Float
    get() = min(width, height)

private fun Offset.unit(): Offset {
    val distance = getDistance()
    return if (distance == 0f) Offset.Zero else Offset(x / distance, y / distance)
}

private fun Float.toDegrees(): Float = this * 180f / PI.toFloat()

private fun wave(phase: Float): Float = (sin(phase * OrbTwoPi) + 1f) * 0.5f

private fun fract(value: Float): Float {
    val floor = value.toInt()
    return value - floor
}

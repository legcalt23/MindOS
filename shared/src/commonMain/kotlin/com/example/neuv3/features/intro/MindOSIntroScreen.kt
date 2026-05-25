package com.example.neuv3.features.intro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.neuv3.features.intro.MindOSIntroConfig.Palette
import com.example.neuv3.features.intro.MindOSIntroConfig.Phase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TWO_PI = (PI * 2).toFloat()
private val GOLDEN_ANGLE = (PI * (3.0 - sqrt(5.0))).toFloat()

// ─── Easing ──────────────────────────────────────────────────────────────────

private fun easeOut(t: Float): Float = 1f - (1f - t).pow(3)
private fun easeSmooth(t: Float): Float =
    if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
private fun easeExpo(t: Float): Float = if (t >= 1f) 1f else 1f - 2f.pow(-10f * t)
private fun easeOutQuart(t: Float): Float = 1f - (1f - t).pow(4)

private fun phaseProgress(time: Long, range: LongRange): Float {
    if (time < range.first) return 0f
    if (time > range.last) return 1f
    return (time - range.first).toFloat() / (range.last - range.first).toFloat()
}

private fun Float.clamp01() = this.coerceIn(0f, 1f)

// ─── Data Models ─────────────────────────────────────────────────────────────

private class IntroParticle(
    index: Int,
    total: Int,
    cx: Float,
    cy: Float,
    viewWidth: Float,
    viewHeight: Float,
    private val rng: Random,
) {
    val originX: Float
    val originY: Float
    var x: Float = 0f
    var y: Float = 0f
    val size: Float = 1f + rng.nextFloat() * 2.2f
    val baseOpacity: Float = 0.35f + rng.nextFloat() * 0.55f
    var currentOpacity: Float = 0f
    var visible: Boolean = false

    private val revealDelay: Long = (index * 12L + (rng.nextFloat() * 60).toLong())
    private val driftPhase: Float = rng.nextFloat() * TWO_PI
    private val convergenceAngle: Float
    private val spiralTurns: Float = 0.6f + rng.nextFloat() * 1.2f
    private val convergenceSpeed: Float = 0.85f + rng.nextFloat() * 0.3f

    init {
        val angle = index * GOLDEN_ANGLE + (rng.nextFloat() - 0.5f) * 0.3f
        val rMin = 80f
        val rMax = min(viewWidth, viewHeight) * 0.42f
        val normalizedR = (index + 0.5f) / total
        val dist = rMin + sqrt(normalizedR) * (rMax - rMin)
        originX = cx + cos(angle) * dist
        originY = cy + sin(angle) * dist
        x = originX
        y = originY
        convergenceAngle = angle + (rng.nextFloat() - 0.5f) * 0.8f
    }

    fun update(time: Long, cx: Float, cy: Float) {
        val convP = phaseProgress(time, Phase.CONVERGENCE)
        val revealTime = Phase.CONSTELLATION.first + revealDelay
        if (time >= revealTime) {
            val fadeIn = ((time - revealTime).toFloat() / 350f).clamp01()
            currentOpacity = baseOpacity * easeOut(fadeIn)
            visible = true
        }
        if (convP > 0f) {
            val t = easeSmooth((convP * convergenceSpeed).clamp01())
            val spiralAng = convergenceAngle + t * PI.toFloat() * spiralTurns
            val origDist = hypot(originX - cx, originY - cy)
            val dist = (1f - t) * origDist
            x = cx + cos(spiralAng) * dist
            y = cy + sin(spiralAng) * dist
            if (t > 0.65f) currentOpacity *= 1f - ((t - 0.65f) / 0.35f)
        } else {
            val drift = sin(time * 0.0012f + driftPhase) * 2.5f
            x = originX + drift
            y = originY + cos(time * 0.0009f + driftPhase) * 2f
        }
    }
}

private class IntroConnection(
    val p1: IntroParticle,
    val p2: IntroParticle,
    index: Int,
    rng: Random,
) {
    private val revealDelay = Phase.CONSTELLATION.first + 250L + index * 18L
    var opacity: Float = 0f
    val curveStrength: Float = (rng.nextFloat() - 0.5f) * 40f

    fun update(time: Long) {
        if (time < revealDelay) { opacity = 0f; return }
        val fadeIn = ((time - revealDelay).toFloat() / 500f).clamp01()
        val convP = phaseProgress(time, Phase.CONVERGENCE)
        val fadeOut = if (convP > 0.4f) 1f - easeSmooth((convP - 0.4f) / 0.6f) else 1f
        opacity = easeOut(fadeIn) * 0.2f * fadeOut.coerceAtLeast(0f)
    }
}

private class IntroCrystalCore {
    private val vertices: List<FloatArray>
    private val edges: List<IntArray>
    var rotX = 0f; var rotY = 0f; var rotZ = 0f
    var scale = 0f; var opacity = 0f; var glowIntensity = 0f

    init {
        val phi = (1f + sqrt(5f)) / 2f
        val raw = listOf(
            floatArrayOf(-1f, phi, 0f), floatArrayOf(1f, phi, 0f),
            floatArrayOf(-1f, -phi, 0f), floatArrayOf(1f, -phi, 0f),
            floatArrayOf(0f, -1f, phi), floatArrayOf(0f, 1f, phi),
            floatArrayOf(0f, -1f, -phi), floatArrayOf(0f, 1f, -phi),
            floatArrayOf(phi, 0f, -1f), floatArrayOf(phi, 0f, 1f),
            floatArrayOf(-phi, 0f, -1f), floatArrayOf(-phi, 0f, 1f),
        )
        val len = sqrt(1f + phi * phi)
        vertices = raw.map { floatArrayOf(it[0] / len, it[1] / len, it[2] / len) }

        val threshold = 2f / sqrt(1f + phi * phi) * 1.1f
        val edgeList = mutableListOf<IntArray>()
        for (i in vertices.indices) {
            for (j in i + 1 until vertices.size) {
                val dx = vertices[i][0] - vertices[j][0]
                val dy = vertices[i][1] - vertices[j][1]
                val dz = vertices[i][2] - vertices[j][2]
                if (sqrt(dx * dx + dy * dy + dz * dz) < threshold) {
                    edgeList.add(intArrayOf(i, j))
                }
            }
        }
        edges = edgeList
    }

    fun project(v: FloatArray, radius: Float, cx: Float, cy: Float): FloatArray {
        val cxr = cos(rotX); val sxr = sin(rotX)
        val cyr = cos(rotY); val syr = sin(rotY)
        val czr = cos(rotZ); val szr = sin(rotZ)
        val (x, y, z) = v
        val y1 = y * cxr - z * sxr; val z1 = y * sxr + z * cxr
        val x2 = x * cyr + z1 * syr; val z2 = -x * syr + z1 * cyr
        val x3 = x2 * czr - y1 * szr; val y3 = x2 * szr + y1 * czr
        val p = 4f / (4f + z2)
        return floatArrayOf(cx + x3 * radius * scale * p, cy + y3 * radius * scale * p, z2, p)
    }

    fun update(time: Long) {
        val convP = phaseProgress(time, Phase.CONVERGENCE)
        val transP = phaseProgress(time, Phase.TRANSITION)
        if (convP > 0.35f) {
            val t = ((convP - 0.35f) / 0.65f).clamp01()
            opacity = easeOutQuart(t)
            scale = easeOutQuart(t)
        }
        glowIntensity = if (convP > 0.6f) easeOut(((convP - 0.6f) / 0.4f).clamp01()) else 0f
        val breath = sin(time * MindOSIntroConfig.CORE_BREATH_SPEED) * MindOSIntroConfig.CORE_BREATH_AMPLITUDE
        scale *= (1f + breath)
        rotX += MindOSIntroConfig.CORE_ROTATION_SPEED * 1.3f
        rotY += MindOSIntroConfig.CORE_ROTATION_SPEED
        rotZ += MindOSIntroConfig.CORE_ROTATION_SPEED * 0.5f
        if (transP > 0f) {
            val t = easeSmooth(transP)
            scale *= 1f + t * 2.5f
            opacity *= 1f - t
        }
    }

    fun draw(scope: DrawScope, cx: Float, cy: Float) {
        if (opacity <= 0.01f) return
        val R = MindOSIntroConfig.CORE_RADIUS

        if (glowIntensity > 0f) {
            val outerR = R * scale * 3f
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Palette.QuantumViolet.copy(alpha = opacity * glowIntensity * 0.25f),
                        0.4f to Palette.QuantumViolet.copy(alpha = opacity * glowIntensity * 0.08f),
                        1f to Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = outerR,
                ),
                center = Offset(cx, cy),
                radius = outerR,
            )
            val innerR = R * scale * 1.2f
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Palette.StellarWhite.copy(alpha = opacity * glowIntensity * 0.15f),
                        0.5f to Palette.AuroraLilac.copy(alpha = opacity * glowIntensity * 0.1f),
                        1f to Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = innerR,
                ),
                center = Offset(cx, cy),
                radius = innerR,
            )
        }

        for (edge in edges) {
            val a = project(vertices[edge[0]], R, cx, cy)
            val b = project(vertices[edge[1]], R, cx, cy)
            val depth = 0.35f + 0.65f * ((a[2] + b[2] + 2f) / 4f)
            scope.drawLine(
                color = Palette.AuroraLilac.copy(alpha = opacity * depth * 0.85f),
                start = Offset(a[0], a[1]),
                end = Offset(b[0], b[1]),
                strokeWidth = 1f + depth * 0.4f,
            )
        }

        for (v in vertices) {
            val pt = project(v, R, cx, cy)
            val depth = 0.3f + 0.7f * ((pt[2] + 1f) / 2f)
            val sz = 1.8f * pt[3]
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Palette.AuroraLilac.copy(alpha = opacity * depth * 0.4f),
                        1f to Color.Transparent,
                    ),
                    center = Offset(pt[0], pt[1]),
                    radius = sz * 4f,
                ),
                center = Offset(pt[0], pt[1]),
                radius = sz * 4f,
            )
            scope.drawCircle(
                color = Palette.StellarWhite.copy(alpha = opacity * depth),
                center = Offset(pt[0], pt[1]),
                radius = sz,
            )
        }
    }
}

// ─── Drawing Functions ───────────────────────────────────────────────────────

private fun DrawScope.drawIntroBackground(cx: Float, cy: Float) {
    val maxR = max(size.width, size.height) * 0.7f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Palette.CosmicInk, Palette.DeepVoid),
            center = Offset(cx, cy),
            radius = maxR,
        ),
    )
}

private fun DrawScope.drawSpark(time: Long, cx: Float, cy: Float) {
    val appearP = ((time - 200f) / 400f).clamp01()
    if (appearP <= 0f) return

    var sz = 2f + (6f - 2f) * easeOut(appearP)

    if (time in 500..800) {
        val pp = (time - 500f) / 300f
        sz = sz + (14f - sz) * sin(pp * PI.toFloat())
        if (pp > 0.2f) {
            val wp = (pp - 0.2f) / 0.8f
            drawCircle(
                color = Palette.QuantumViolet.copy(alpha = (1f - wp) * 0.25f),
                center = Offset(cx, cy),
                radius = wp * min(size.width, size.height) * 0.18f,
                style = Stroke(width = 1f),
            )
        }
    }

    val convP = phaseProgress(time, Phase.CONVERGENCE)
    val sparkAlpha = if (convP > 0.3f) (1f - (convP - 0.3f) / 0.4f).coerceAtLeast(0f) else 1f
    if (sparkAlpha <= 0f) return

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Palette.QuantumViolet.copy(alpha = 0.45f * sparkAlpha),
                0.4f to Palette.QuantumViolet.copy(alpha = 0.12f * sparkAlpha),
                1f to Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = sz * 8f,
        ),
        center = Offset(cx, cy),
        radius = sz * 8f,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Palette.StellarWhite.copy(alpha = sparkAlpha),
                0.5f to Palette.AuroraLilac.copy(alpha = sparkAlpha * 0.7f),
                1f to Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = sz,
        ),
        center = Offset(cx, cy),
        radius = sz,
    )
}

private fun DrawScope.drawParticle(p: IntroParticle) {
    if (!p.visible || p.currentOpacity <= 0.01f) return

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Palette.QuantumViolet.copy(alpha = p.currentOpacity * 0.35f),
                1f to Color.Transparent,
            ),
            center = Offset(p.x, p.y),
            radius = p.size * 5f,
        ),
        center = Offset(p.x, p.y),
        radius = p.size * 5f,
    )

    drawCircle(
        color = Palette.StellarWhite.copy(alpha = p.currentOpacity),
        center = Offset(p.x, p.y),
        radius = p.size,
    )
}

private fun DrawScope.drawConnection(c: IntroConnection) {
    if (c.opacity <= 0.005f) return
    val mx = (c.p1.x + c.p2.x) / 2f
    val my = (c.p1.y + c.p2.y) / 2f
    val dx = c.p2.x - c.p1.x
    val dy = c.p2.y - c.p1.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val cpX = mx + nx * c.curveStrength
    val cpY = my + ny * c.curveStrength
    val midX = 0.25f * c.p1.x + 0.5f * cpX + 0.25f * c.p2.x
    val midY = 0.25f * c.p1.y + 0.5f * cpY + 0.25f * c.p2.y
    drawLine(
        color = Palette.QuantumViolet.copy(alpha = c.opacity * 0.7f),
        start = Offset(c.p1.x, c.p1.y),
        end = Offset(midX, midY),
        strokeWidth = 0.5f,
    )
    drawLine(
        color = Palette.QuantumViolet.copy(alpha = c.opacity * 0.7f),
        start = Offset(midX, midY),
        end = Offset(c.p2.x, c.p2.y),
        strokeWidth = 0.5f,
    )
}

// ─── Main Composable ─────────────────────────────────────────────────────────

@Composable
fun MindOSIntroScreen(
    onIntroFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeMs by remember { mutableStateOf(0L) }
    val textMeasurer = rememberTextMeasurer()

    val rng = remember { Random(42) }

    var particles by remember { mutableStateOf<List<IntroParticle>>(emptyList()) }
    var connections by remember { mutableStateOf<List<IntroConnection>>(emptyList()) }
    val core = remember { IntroCrystalCore() }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            val now = withInfiniteAnimationFrameMillis { it }
            val elapsed = now - start
            timeMs = elapsed.coerceAtMost(MindOSIntroConfig.DURATION_MS)
            if (elapsed >= MindOSIntroConfig.DURATION_MS) {
                onIntroFinished()
                break
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        if (!initialized) {
            val newParticles = (0 until MindOSIntroConfig.PARTICLE_COUNT).map { i ->
                IntroParticle(i, MindOSIntroConfig.PARTICLE_COUNT, cx, cy, size.width, size.height, rng)
            }
            val newConnections = mutableListOf<IntroConnection>()
            val maxDist = MindOSIntroConfig.CONNECTION_DISTANCE
            for (i in newParticles.indices) {
                if (newConnections.size >= MindOSIntroConfig.MAX_CONNECTIONS) break
                for (j in i + 1 until newParticles.size) {
                    if (newConnections.size >= MindOSIntroConfig.MAX_CONNECTIONS) break
                    val dist = hypot(
                        newParticles[i].originX - newParticles[j].originX,
                        newParticles[i].originY - newParticles[j].originY,
                    )
                    if (dist < maxDist) {
                        newConnections.add(IntroConnection(newParticles[i], newParticles[j], newConnections.size, rng))
                    }
                }
            }
            particles = newParticles
            connections = newConnections
            initialized = true
        }

        drawIntroBackground(cx, cy)
        drawSpark(timeMs, cx, cy)

        for (p in particles) p.update(timeMs, cx, cy)
        for (c in connections) { c.update(timeMs); drawConnection(c) }
        for (p in particles) drawParticle(p)

        core.update(timeMs)
        core.draw(this, cx, cy)

        drawWordmark(timeMs, cx, cy, textMeasurer)
    }
}

private fun DrawScope.drawWordmark(
    time: Long,
    cx: Float,
    cy: Float,
    textMeasurer: TextMeasurer,
) {
    val nameP = phaseProgress(time, Phase.NAME_EMERGES)
    val transP = phaseProgress(time, Phase.TRANSITION)
    if (nameP <= 0f) return

    val revealT = easeExpo((nameP * 1.4f).clamp01())
    var opacity = revealT
    var yOff = (1f - revealT) * 8f

    if (transP > 0f) {
        val t = easeSmooth(transP)
        opacity *= 1f - t
        yOff -= t * 10f
    }

    if (opacity <= 0.01f) return

    val textY = cy + MindOSIntroConfig.CORE_RADIUS + 55f + yOff

    val mindStyle = TextStyle(
        color = Palette.StellarWhite.copy(alpha = opacity),
        fontSize = MindOSIntroConfig.TYPE_SIZE.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default,
        letterSpacing = MindOSIntroConfig.TYPE_TRACKING.sp,
    )
    val osStyle = TextStyle(
        color = Palette.AuroraLilac.copy(alpha = opacity),
        fontSize = MindOSIntroConfig.TYPE_SIZE.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default,
        letterSpacing = MindOSIntroConfig.TYPE_TRACKING.sp,
    )

    val mindResult = textMeasurer.measure("Mind ", mindStyle)
    val osResult = textMeasurer.measure("OS", osStyle)
    val totalW = mindResult.size.width + osResult.size.width
    val startX = cx - totalW / 2f
    val baseY = textY - mindResult.size.height / 2f

    drawText(mindResult, topLeft = Offset(startX, baseY))
    drawText(osResult, topLeft = Offset(startX + mindResult.size.width, baseY))

    if (revealT > 0.4f && opacity > 0.2f) {
        val lineAlpha = ((revealT - 0.4f) / 0.6f).coerceAtMost(1f) * opacity * 0.45f
        val lw = totalW * 0.55f
        val ly = textY + mindResult.size.height / 2f + 12f

        drawLine(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.2f to Palette.QuantumViolet.copy(alpha = lineAlpha),
                    0.8f to Palette.QuantumViolet.copy(alpha = lineAlpha),
                    1f to Color.Transparent,
                ),
                startX = cx - lw,
                endX = cx + lw,
            ),
            start = Offset(cx - lw, ly),
            end = Offset(cx + lw, ly),
            strokeWidth = 0.7f,
        )
    }
}

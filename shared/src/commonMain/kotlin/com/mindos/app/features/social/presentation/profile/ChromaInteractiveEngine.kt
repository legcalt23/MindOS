package com.mindos.app.features.social.presentation.profile

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Mathematically unified HSV Color Engine processing Drag and Hold Energy
 */
@Composable
fun ChromaInteractiveEngine(
    session: ChromaCoreSessionState,
    onColorUpdate: (Long, ChromaCoreInteractionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Math model initialized from current UI preview color
    val initialHsv = remember(session.isVisible) {
        if (session.isVisible) argbToHsv(session.previewColor) else floatArrayOf(0f, 0f, 0f)
    }

    var currentHue by remember(session.isVisible) { mutableStateOf(initialHsv[0]) }
    var currentSat by remember(session.isVisible) { mutableStateOf(initialHsv[1]) }
    var currentVal by remember(session.isVisible) { mutableStateOf(initialHsv[2]) }
    var interactionMode by remember { mutableStateOf(ChromaCoreInteractionMode.Idle) }

    // Physical bounds calculations
    var center by remember { mutableStateOf(Offset.Zero) }
    var coreRadius by remember { mutableStateOf(0f) }

    // State variable for Hold Energy (Luminance control)
    var holdStartTime by remember { mutableStateOf(0L) }
    var isHolding by remember { mutableStateOf(false) }

    // Fluid visual feedback states
    val coreGlowAlpha by animateFloatAsState(
        targetValue = if (interactionMode != ChromaCoreInteractionMode.Idle) 1.0f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )

    val activeColor = remember(currentHue, currentSat, currentVal) {
        hsvToArgb(currentHue, currentSat, currentVal)
    }

    LaunchedEffect(activeColor, interactionMode) {
        if (interactionMode != ChromaCoreInteractionMode.Idle || session.previewColor != activeColor) {
            onColorUpdate(activeColor, interactionMode)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    interactionMode = ChromaCoreInteractionMode.Dragging
                    holdStartTime = down.uptimeMillis
                    isHolding = true

                    // Hysteresis threshold
                    var draggingStarted = false

                    drag(down.id) { change ->
                        change.consume()
                        val currentOffset = change.position - center
                        val dist = currentOffset.getDistance()

                        val timeSinceDown = change.uptimeMillis - holdStartTime
                        
                        // Check for precision mode
                        if (!draggingStarted && timeSinceDown > 250 && change.positionChange().getDistance() < 10f) {
                            interactionMode = ChromaCoreInteractionMode.Precision
                        }

                        if (change.positionChange().getDistance() > 3f) {
                            draggingStarted = true
                            isHolding = false
                            if (interactionMode == ChromaCoreInteractionMode.Idle) {
                                interactionMode = ChromaCoreInteractionMode.Dragging
                            }
                        }

                        // Apply mapping
                        if (interactionMode == ChromaCoreInteractionMode.Precision) {
                            // Micro-adjustments (decelerated drag)
                            val angleDelta = calculateAngleDelta(change.previousPosition - center, currentOffset)
                            currentHue = (currentHue + angleDelta * 0.1f + 360f) % 360f
                            val radDelta = (dist - (change.previousPosition - center).getDistance()) / coreRadius
                            currentSat = (currentSat + radDelta * 0.1f).coerceIn(0f, 1f)
                        } else {
                            // Standard interaction
                            if (dist > coreRadius * 0.2f) { // Protect dead zone
                                currentHue = normalizeAngle(atan2(currentOffset.y, currentOffset.x) * (180 / PI).toFloat())
                                currentSat = (dist / coreRadius).coerceIn(0f, 1f)
                            }
                        }
                    }

                    // On Release
                    isHolding = false
                    interactionMode = ChromaCoreInteractionMode.Settling
                    onColorUpdate(activeColor, interactionMode)
                    // Briefly settle then idle
                    interactionMode = ChromaCoreInteractionMode.Idle
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            center = Offset(size.width / 2f, size.height / 2f)
            coreRadius = min(size.width, size.height) * 0.4f

            drawChromaCore(
                center = center,
                radius = coreRadius,
                hue = currentHue,
                sat = currentSat,
                glowAlpha = coreGlowAlpha,
                mainColor = Color(activeColor)
            )
        }
    }
}

private fun DrawScope.drawChromaCore(
    center: Offset,
    radius: Float,
    hue: Float,
    sat: Float,
    glowAlpha: Float,
    mainColor: Color
) {
    // Base ring (Dark orbit)
    drawCircle(
        color = Color.White.copy(alpha = 0.05f),
        center = center,
        radius = radius,
        style = Stroke(width = 2f)
    )

    // Intermediate traction ring
    drawCircle(
        color = Color.White.copy(alpha = 0.1f),
        center = center,
        radius = radius * 0.6f,
        style = Stroke(width = 1f)
    )

    // Radial gradient glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(mainColor.copy(alpha = 0.6f * glowAlpha), Color.Transparent),
            center = center,
            radius = radius * 1.5f
        ),
        center = center,
        radius = radius * 1.5f
    )

    // Active pointer
    val px = center.x + radius * sat * cos(hue * PI / 180).toFloat()
    val py = center.y + radius * sat * sin(hue * PI / 180).toFloat()

    drawCircle(
        color = mainColor,
        center = Offset(px, py),
        radius = 16f
    )
    
    // Pointer halo
    drawCircle(
        color = Color.White,
        center = Offset(px, py),
        radius = 16f,
        style = Stroke(width = 2f)
    )
}

// -------------------------------------------------------------
// Mathematical HSV to ARGB Utilities
// -------------------------------------------------------------

private fun normalizeAngle(angle: Float): Float {
    var a = angle % 360f
    if (a < 0) a += 360f
    return a
}

private fun calculateAngleDelta(prev: Offset, curr: Offset): Float {
    val a1 = atan2(prev.y, prev.x)
    val a2 = atan2(curr.y, curr.x)
    var diff = (a2 - a1) * (180 / PI).toFloat()
    if (diff > 180) diff -= 360
    if (diff < -180) diff += 360
    return diff
}

fun hsvToArgb(hue: Float, sat: Float, v: Float): Long {
    val hh = hue % 360f
    val c = v * sat
    val x = c * (1 - kotlin.math.abs((hh / 60) % 2 - 1))
    val m = v - c

    var r = 0f
    var g = 0f
    var b = 0f

    when {
        hh < 60 -> { r = c; g = x; b = 0f }
        hh < 120 -> { r = x; g = c; b = 0f }
        hh < 180 -> { r = 0f; g = c; b = x }
        hh < 240 -> { r = 0f; g = x; b = c }
        hh < 300 -> { r = x; g = 0f; b = c }
        else -> { r = c; g = 0f; b = x }
    }

    val ri = ((r + m) * 255f).roundToInt().coerceIn(0, 255)
    val gi = ((g + m) * 255f).roundToInt().coerceIn(0, 255)
    val bi = ((b + m) * 255f).roundToInt().coerceIn(0, 255)

    return (0xFFL shl 24) or (ri.toLong() shl 16) or (gi.toLong() shl 8) or bi.toLong()
}

fun argbToHsv(color: Long): FloatArray {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f

    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin

    var hue = 0f
    if (delta > 0) {
        when (cMax) {
            r -> hue = 60f * (((g - b) / delta) % 6f)
            g -> hue = 60f * (((b - r) / delta) + 2f)
            b -> hue = 60f * (((r - g) / delta) + 4f)
        }
    }
    if (hue < 0f) hue += 360f

    val sat = if (cMax == 0f) 0f else delta / cMax
    val v = cMax

    return floatArrayOf(hue, sat, v)
}

package com.mindos.app.core.ui.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class FireBackState {
    Idle, Dragging, Cancelled, Completing, Navigated
}

class Spark {
    val initialAngle = Random.nextFloat() * 2f * kotlin.math.PI.toFloat()
    val speedMultiplier = Random.nextFloat() * 1.5f + 0.5f
    val size = Random.nextFloat() * 4f + 2f
    val maxAlpha = Random.nextFloat() * 0.5f + 0.5f
}

fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
fun FireBackGestureHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf(FireBackState.Idle) }
    var dragX by remember { mutableStateOf(0f) }
    var touchY by remember { mutableStateOf(0f) }

    val edgeWidthPx = with(density) { 28.dp.toPx() }
    val triggerDistancePx = with(density) { 96.dp.toPx() }
    val maxDragDistancePx = with(density) { 180.dp.toPx() }

    val animatedDragX = remember { Animatable(0f) }
    val sparks = remember { List(24) { Spark() } }

    val pointerInputModifier = if (enabled) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.x > edgeWidthPx) {
                    return@awaitEachGesture
                }

                var overSlop = 0f
                val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    overSlop = over
                }

                if (drag != null && (state == FireBackState.Idle || state == FireBackState.Cancelled)) {
                    state = FireBackState.Dragging
                    dragX = overSlop
                    touchY = drag.position.y
                    coroutineScope.launch {
                        animatedDragX.snapTo(dragX)
                    }

                    var success = false
                    try {
                        success = horizontalDrag(drag.id) { change ->
                            change.consume()
                            dragX += change.positionChange().x
                            if (dragX < 0f) dragX = 0f
                            
                            // Smooth vertical follow but limited
                            val dy = change.positionChange().y
                            touchY += dy * 0.2f 

                            coroutineScope.launch {
                                animatedDragX.snapTo(dragX)
                            }
                        }
                    } catch (e: CancellationException) {
                        success = false
                    }

                    if (success) {
                        if (dragX >= triggerDistancePx) {
                            state = FireBackState.Completing
                            coroutineScope.launch {
                                animatedDragX.animateTo(
                                    targetValue = dragX + with(density) { 40.dp.toPx() },
                                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                                )
                                if (state != FireBackState.Navigated) {
                                    state = FireBackState.Navigated
                                    onBack()
                                    delay(400)
                                    state = FireBackState.Idle
                                    animatedDragX.snapTo(0f)
                                }
                            }
                        } else {
                            state = FireBackState.Cancelled
                            coroutineScope.launch {
                                animatedDragX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                                state = FireBackState.Idle
                            }
                        }
                    } else {
                        state = FireBackState.Cancelled
                        coroutineScope.launch {
                            animatedDragX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                            state = FireBackState.Idle
                        }
                    }
                }
            }
        }
    } else Modifier

    Box(modifier = Modifier.fillMaxSize().then(pointerInputModifier)) {
        content()

        if (state != FireBackState.Idle && state != FireBackState.Navigated) {
            FireCanvas(
                dragX = animatedDragX.value,
                touchY = touchY,
                triggerDistancePx = triggerDistancePx,
                maxDragDistancePx = maxDragDistancePx,
                state = state,
                sparks = sparks
            )
        }
    }
}

@Composable
private fun FireCanvas(
    dragX: Float,
    touchY: Float,
    triggerDistancePx: Float,
    maxDragDistancePx: Float,
    state: FireBackState,
    sparks: List<Spark>
) {
    val progress = (dragX / triggerDistancePx).coerceIn(0f, 1f)
    val visualProgress = (dragX / maxDragDistancePx).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val burstProgress by animateFloatAsState(
        targetValue = if (state == FireBackState.Completing) 1f else 0f,
        animationSpec = tween(150),
        label = "burstProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val density = this.density
        val ballRadius = 32.dp.toPx() + (visualProgress * 12.dp.toPx()) + (burstProgress * 20.dp.toPx())
        val ballX = lerpFloat(-ballRadius * 1.6f, dragX, visualProgress)
        val ballCenter = Offset(ballX, touchY)

        // 1. Trail
        val trailLength = dragX * 1.2f
        if (trailLength > 0 && ballX > -ballRadius) {
            val trailPath = Path().apply {
                moveTo(ballX, touchY - ballRadius * 0.7f)
                quadraticBezierTo(ballX - trailLength * 0.6f, touchY, 0f, touchY)
                quadraticBezierTo(ballX - trailLength * 0.6f, touchY, ballX, touchY + ballRadius * 0.7f)
                close()
            }
            val trailBrush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0x66FF4500), Color(0xCCFF8C00)),
                startX = 0f,
                endX = ballX
            )
            drawPath(path = trailPath, brush = trailBrush)
        }

        // 2. Fire Crown (Glow)
        val glowRadius = ballRadius * (1.6f + 0.2f * sin(time * 3f)) + burstProgress * ballRadius * 1.5f
        val glowAlpha = (0.5f + 0.15f * sin(time * 5f) + burstProgress * 0.5f) * progress
        if (glowAlpha > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF3300).copy(alpha = glowAlpha.coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = ballCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = ballCenter
            )
        }

        // 3. Energy Arcs
        if (progress > 0.1f) {
            val arcRadius = ballRadius * 1.15f
            drawArc(
                color = Color(0xFFFFD700).copy(alpha = (0.7f * progress).coerceIn(0f, 1f)),
                startAngle = (time * 180f * 180f / kotlin.math.PI.toFloat()) % 360f,
                sweepAngle = 70f + 30f * sin(time * 2f),
                useCenter = false,
                topLeft = Offset(ballCenter.x - arcRadius, ballCenter.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            val arcRadius2 = ballRadius * 0.9f
            drawArc(
                color = Color(0xFFFFA500).copy(alpha = (0.9f * progress).coerceIn(0f, 1f)),
                startAngle = (-time * 240f * 180f / kotlin.math.PI.toFloat()) % 360f + 180f,
                sweepAngle = 50f + 40f * cos(time * 3f),
                useCenter = false,
                topLeft = Offset(ballCenter.x - arcRadius2, ballCenter.y - arcRadius2),
                size = Size(arcRadius2 * 2, arcRadius2 * 2),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 4. Core
        val coreOffset = Offset(
            ballCenter.x + 3.dp.toPx() * sin(time * 8f),
            ballCenter.y + 3.dp.toPx() * cos(time * 7f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White,
                    0.25f to Color(0xFFFFE066),
                    0.6f to Color(0xFFFF4500),
                    1f to Color.Transparent
                ),
                center = coreOffset,
                radius = ballRadius
            ),
            radius = ballRadius,
            center = coreOffset
        )

        // 5. Sparks
        if (progress > 0.05f) {
            sparks.forEachIndexed { index, spark ->
                val life = (time * spark.speedMultiplier + index) % (2f * kotlin.math.PI.toFloat())
                val sparkProgress = (life / (2f * kotlin.math.PI.toFloat()))
                
                val distance = ballRadius + (sparkProgress * ballRadius * 3.5f)
                val angle = time * spark.speedMultiplier + spark.initialAngle
                
                val sx = ballCenter.x + cos(angle) * distance - (sparkProgress * dragX * 0.6f)
                val sy = ballCenter.y + sin(angle) * distance
                
                val sAlpha = (1f - sparkProgress) * spark.maxAlpha * progress
                if (sAlpha > 0f) {
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = sAlpha.coerceIn(0f, 1f)),
                        radius = spark.size * density * (1f - sparkProgress * 0.4f),
                        center = Offset(sx, sy)
                    )
                }
            }
        }
    }
}

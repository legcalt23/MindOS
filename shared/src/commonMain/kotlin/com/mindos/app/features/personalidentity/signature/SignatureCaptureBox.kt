@file:Suppress("FunctionNaming")

package com.mindos.app.features.personalidentity.signature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppShapes
import com.mindos.app.design.AppSpacing
import com.mindos.app.features.personalidentity.model.SignaturePoint
import com.mindos.app.features.personalidentity.model.SignatureStroke
import com.mindos.app.features.personalidentity.model.simplified

@Composable
fun SignatureCaptureBox(
    strokes: List<SignatureStroke>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = AppSpacing.XS,
    strokeColor: Color = AppColors.OnBackground,
    onStrokesChanged: (List<SignatureStroke>) -> Unit,
) {
    var currentStroke by remember { mutableStateOf<List<SignaturePoint>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppSpacing.AvatarSizeLarge * CAPTURE_HEIGHT_MULTIPLIER)
            .clip(AppShapes.Card)
            .background(AppColors.DeepBlack)
            .border(AppSpacing.DividerThickness, AppColors.Stroke, AppShapes.Card)
            .onSizeChanged { canvasSize = it }
            .pointerInput(canvasSize, strokes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke = listOf(offset.toSignaturePoint(canvasSize))
                    },
                    onDrag = { change, _ ->
                        currentStroke = currentStroke + change.position.toSignaturePoint(canvasSize)
                        change.consume()
                    },
                    onDragEnd = {
                        if (currentStroke.size >= MIN_DRAWABLE_POINT_COUNT) {
                            onStrokesChanged(strokes + SignatureStroke(currentStroke).simplified())
                        }
                        currentStroke = emptyList()
                    },
                    onDragCancel = {
                        currentStroke = emptyList()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val allStrokes = strokes + SignatureStroke(currentStroke)
            for (stroke in allStrokes) {
                if (!stroke.isDrawable) continue
                drawPath(
                    path = stroke.toPath(size),
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

@Composable
fun SignaturePreview(
    strokes: List<SignatureStroke>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = AppSpacing.XXS,
    strokeColor: Color = AppColors.OnBackground,
) {
    Canvas(modifier = modifier) {
        for (stroke in strokes) {
            if (!stroke.isDrawable) continue
            drawPath(
                path = stroke.toPath(size),
                color = strokeColor,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

private fun Offset.toSignaturePoint(size: IntSize): SignaturePoint {
    if (size.width <= 0 || size.height <= 0) return SignaturePoint(0f, 0f)
    return SignaturePoint(
        x = (x / size.width.toFloat()).coerceIn(0f, 1f),
        y = (y / size.height.toFloat()).coerceIn(0f, 1f),
    )
}

private fun SignatureStroke.toPath(size: Size): Path {
    val path = Path()
    val first = points.firstOrNull()?.clamped() ?: return path
    path.moveTo(first.x * size.width, first.y * size.height)

    for (index in 1 until points.size) {
        val point = points[index].clamped()
        path.lineTo(point.x * size.width, point.y * size.height)
    }

    return path
}

private const val CAPTURE_HEIGHT_MULTIPLIER = 3
private const val MIN_DRAWABLE_POINT_COUNT = 2

package com.mindos.app.features.search.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.features.search.domain.UsernameSearchMinQueryLength
import com.mindos.app.features.search.domain.UsernameSearchUser
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun UsernameRadarMode(
    state: UsernameSearchUiState,
    currentUserId: String,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (UsernameSearchUser) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "USERNAME RADAR",
            color = NeuWhite,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = radarStatusText(state),
            color = NeuGreen.copy(alpha = 0.78f),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        RadarField(
            state = state,
            currentUserId = currentUserId,
            onResultSelected = onResultSelected,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 12.dp, bottom = 14.dp),
        )

        RadarCommandInput(
            query = state.query,
            onQueryChanged = onQueryChanged,
            helper = inputHelper(state),
            modifier = Modifier.fillMaxWidth(),
        )
        state.selectionMessage?.let { message ->
            Text(
                text = message,
                color = NeuGreen.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun RadarField(
    state: UsernameSearchUiState,
    currentUserId: String,
    onResultSelected: (UsernameSearchUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600,
                easing = LinearEasing,
            ),
        ),
        label = "sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
        ),
        label = "pulse",
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val radarSize = minOf(maxWidth, maxHeight)
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .size(radarSize)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            RadarCanvas(sweep = sweep, pulse = pulse, modifier = Modifier.fillMaxSize())

            if (!state.canSearch) {
                RadarCoreMessage(
                    title = "SISTEMA LISTO",
                    detail = "${UsernameSearchMinQueryLength - state.normalizedQuery.length} caracteres para activar deteccion",
                )
            } else if (state.status is UsernameSearchStatus.Empty) {
                RadarCoreMessage(
                    title = "SIN ECO",
                    detail = "No hay coincidencias en esta frecuencia",
                )
            } else if (state.status is UsernameSearchStatus.Failed) {
                RadarCoreMessage(
                    title = "CANAL CERRADO",
                    detail = state.status.message,
                )
            }

            state.results.take(UsernameSearchMaxRadarContacts).forEachIndexed { index, user ->
                val slot = radarSlots[index % radarSlots.size]
                val relevanceRadius = 0.78f - (user.matchScore.coerceIn(0f, 1f) * 0.36f)
                val contactSize = if (index == 0) 82.dp else 72.dp
                val contactWidth = contactSize + RadarContactHorizontalChrome
                val contactHeight = contactSize + RadarContactVerticalChrome
                val safeMargin = RadarContactSafeMargin
                val maxContactSide = maxOf(contactWidth, contactHeight)
                val safeRadius = (radarSize / 2f - maxContactSide / 2f - safeMargin).coerceAtLeast(0.dp)
                val radius = (safeRadius * relevanceRadius * slot.radiusBias).coerceAtMost(safeRadius)
                val angle = slot.angleDegrees.toRadians()
                val rawX = (radarSize / 2f) + (radius * cos(angle)) - (contactWidth / 2f)
                val rawY = (radarSize / 2f) + (radius * sin(angle)) - (contactHeight / 2f)
                val x = rawX.clampedBetween(safeMargin, radarSize - contactWidth - safeMargin)
                val y = rawY.clampedBetween(safeMargin, radarSize - contactHeight - safeMargin)

                RadarContact(
                    user = user,
                    selected = state.selectedUserId == user.userId,
                    isCurrentUser = user.userId == currentUserId,
                    onClick = { onResultSelected(user) },
                    modifier = Modifier.offset {
                        with(density) {
                            IntOffset(x.toPx().roundToInt(), y.toPx().roundToInt())
                        }
                    },
                    size = contactSize,
                )
            }
        }
    }
}

@Composable
private fun RadarCanvas(
    sweep: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeuGreen.copy(alpha = 0.20f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
        )
        repeat(4) { index ->
            val ringRadius = radius * ((index + 1) / 4f)
            drawCircle(
                color = NeuGreen.copy(alpha = 0.10f + index * 0.035f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.2f),
            )
        }
        drawLine(
            color = NeuGreen.copy(alpha = 0.18f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1f,
        )
        drawLine(
            color = NeuGreen.copy(alpha = 0.18f),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1f,
        )
        drawCircle(
            color = NeuGreen.copy(alpha = 0.15f * (1f - pulse)),
            radius = radius * (0.22f + pulse * 0.72f),
            center = center,
            style = Stroke(width = 2.4f),
        )
        rotate(degrees = sweep, pivot = center) {
            drawArc(
                color = NeuGreen.copy(alpha = 0.20f),
                startAngle = -18f,
                sweepAngle = 34f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
            )
            drawLine(
                color = NeuGreen.copy(alpha = 0.62f),
                start = center,
                end = Offset(center.x + radius, center.y),
                strokeWidth = 2.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun RadarContact(
    user: UsernameSearchUser,
    selected: Boolean,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val transition = rememberInfiniteTransition(label = "contact")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "contactGlow",
    )

    Column(
        modifier = modifier
            .width(size + 24.dp)
            .height(size + RadarContactVerticalChrome)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size + 14.dp)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(
                    color = NeuGreen.copy(alpha = 0.10f + glow * 0.08f),
                    radius = this.size.minDimension * 0.48f,
                    center = center,
                    style = Stroke(width = 1.4f),
                )
                drawCircle(
                    color = if (selected) TargetGold.copy(alpha = 0.85f) else NeuGreen.copy(alpha = 0.55f),
                    radius = this.size.minDimension * 0.38f,
                    center = center,
                    style = Stroke(width = if (selected) 2.5f else 1.5f),
                )
            }
            DetectedAvatar(user = user, modifier = Modifier.size(size))
            if (isCurrentUser) {
                Text(
                    text = "YOU",
                    color = NeuBlack,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeuGreen)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = "@${user.username}",
            color = if (selected) TargetGold else NeuWhite,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RadarCoreMessage(
    title: String,
    detail: String,
) {
    Column(
        modifier = Modifier
            .width(194.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NeuBlack.copy(alpha = 0.76f))
            .border(BorderStroke(1.dp, NeuGreen.copy(alpha = 0.32f)), RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = NeuGreen,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            color = NeuMist,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private data class RadarSlot(
    val angleDegrees: Float,
    val radiusBias: Float,
)

private val radarSlots = listOf(
    RadarSlot(angleDegrees = -52f, radiusBias = 0.92f),
    RadarSlot(angleDegrees = 24f, radiusBias = 1.03f),
    RadarSlot(angleDegrees = 126f, radiusBias = 0.98f),
    RadarSlot(angleDegrees = -154f, radiusBias = 0.86f),
    RadarSlot(angleDegrees = 78f, radiusBias = 0.76f),
    RadarSlot(angleDegrees = -104f, radiusBias = 1.08f),
)

private const val UsernameSearchMaxRadarContacts = 6

private val RadarContactHorizontalChrome = 24.dp
private val RadarContactVerticalChrome = 46.dp
private val RadarContactSafeMargin = 8.dp

private fun Dp.clampedBetween(min: Dp, max: Dp): Dp =
    if (max < min) min else coerceIn(min, max)

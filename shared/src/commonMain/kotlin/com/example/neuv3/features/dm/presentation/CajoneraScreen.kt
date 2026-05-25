package com.example.neuv3.features.dm.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.neuv3.features.social.presentation.media.ViewerDesign
import com.example.neuv3.features.waves.domain.service.WaveRequestService
import com.example.neuv3.features.waves.presentation.WaveMailboxIcon
import com.example.neuv3.features.waves.presentation.rememberWaveMailboxViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun Cajonera(
    currentUserId: String,
    waveRequestService: WaveRequestService,
    onBack: () -> Unit,
    onMailboxClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var openedCajon by remember { mutableStateOf<CajonType?>(null) }
    var pressedCajon by remember { mutableStateOf<CajonType?>(null) }
    var touchJob by remember { mutableStateOf<Job?>(null) }
    var isMailboxPreviewVisible by remember { mutableStateOf(false) }
    var mailboxPreviewDecision by remember { mutableStateOf<MailboxPreviewDecision?>(null) }
    val scope = rememberCoroutineScope()
    val mailboxViewModel = rememberWaveMailboxViewModel(
        service = waveRequestService,
        currentUserId = currentUserId,
    )
    val mailboxState by mailboxViewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            touchJob?.cancel()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CajoneraPalette.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .semantics { contentDescription = "Cajonera" },
    ) {
        val scale = min(
            maxWidth.value / CajoneraLayout.BaseWidth.value,
            maxHeight.value / CajoneraLayout.BaseHeight.value,
        )
        fun s(value: Dp) = value * scale

        val sceneWidth = CajoneraLayout.BaseWidth * scale
        val sceneHeight = CajoneraLayout.BaseHeight * scale
        val topInset = ((maxHeight - sceneHeight) / 2f).coerceAtLeast(0.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            CajoneraAtmosphere(modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = topInset)
                    .size(sceneWidth, sceneHeight),
            ) {
                CajoneraHeader(
                    scale = scale,
                    onBack = onBack,
                    hasPendingRequests = mailboxState.hasPendingRequests,
                    onMailboxClick = {
                        isMailboxPreviewVisible = !isMailboxPreviewVisible
                        mailboxViewModel.onMailboxClick()
                        onMailboxClick()
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                CajoneraFurniture(
                    scale = scale,
                    openedCajon = openedCajon,
                    pressedCajon = pressedCajon,
                    onDrawerClick = { cajon ->
                        if (openedCajon == cajon && pressedCajon == null) return@CajoneraFurniture
                        touchJob?.cancel()
                        pressedCajon = null
                        touchJob = scope.launch {
                            pressedCajon = cajon
                            delay(110)
                            openedCajon = cajon
                            pressedCajon = null
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = s(CajoneraLayout.FurnitureTop))
                        .size(
                            width = s(CajoneraLayout.FurnitureWidth),
                            height = s(CajoneraLayout.FurnitureHeight),
                        ),
                )

                if (isMailboxPreviewVisible) {
                    FakeWaveMailboxPreview(
                        scale = scale,
                        decision = mailboxPreviewDecision,
                        onAccept = { mailboxPreviewDecision = MailboxPreviewDecision.Accepted },
                        onDeny = { mailboxPreviewDecision = MailboxPreviewDecision.Denied },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = s(188.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CajoneraHeader(
    scale: Float,
    onBack: () -> Unit,
    hasPendingRequests: Boolean,
    onMailboxClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun s(value: Dp) = value * scale

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset(x = s(28.dp), y = s(42.dp))
                .size(s(48.dp))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.055f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .cajoneraClickable(onBack)
                .semantics {
                    role = Role.Button
                    contentDescription = "Volver"
                },
            contentAlignment = Alignment.Center,
        ) {
            CajoneraBackGlyph(scale = scale)
        }

        Text(
            text = "CAJONERA",
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = s(38.dp)),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = ViewerDesign.White,
                fontSize = (42f * scale).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (1.7f * scale).sp,
                shadow = Shadow(
                    color = ViewerDesign.Green.copy(alpha = 0.22f),
                    offset = Offset(0f, 8f * scale),
                    blurRadius = 24f * scale,
                ),
            ),
        )

        MailboxBlock(
            scale = scale,
            hasPendingRequests = hasPendingRequests,
            onClick = onMailboxClick,
            modifier = Modifier.offset(x = s(283.dp), y = s(116.dp)),
        )
    }
}

@Composable
private fun MailboxBlock(
    scale: Float,
    hasPendingRequests: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun s(value: Dp) = value * scale

    Column(
        modifier = modifier
            .width(s(70.dp))
            .height(s(82.dp))
            .cajoneraClickable(onClick)
            .semantics { contentDescription = "BUZON" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WaveMailboxIcon(
            hasPendingRequests = hasPendingRequests,
            modifier = Modifier.size(width = s(70.dp), height = s(58.dp)),
        )
        Spacer(modifier = Modifier.height(s(5.dp)))
        Text(
            text = "BUZÓN",
            color = ViewerDesign.Mist,
            fontSize = (11f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (0.9f * scale).sp,
        )
    }
}

@Composable
private fun FakeWaveMailboxPreview(
    scale: Float,
    decision: MailboxPreviewDecision?,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun s(value: Dp) = value * scale
    val shape = RoundedCornerShape(s(24.dp))

    Column(
        modifier = modifier
            .width(s(324.dp))
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF152022).copy(alpha = 0.97f),
                        Color(0xFF091012).copy(alpha = 0.97f),
                    ),
                ),
            )
            .border(1.dp, ViewerDesign.Green.copy(alpha = 0.22f), shape)
            .padding(horizontal = s(18.dp), vertical = s(18.dp)),
    ) {
        Text(
            text = "Solicitud WAVES",
            color = ViewerDesign.White,
            fontSize = (18f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (0.6f * scale).sp,
        )
        Spacer(modifier = Modifier.height(s(16.dp)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s(14.dp)),
        ) {
            Box(
                modifier = Modifier
                    .size(s(56.dp))
                    .clip(CircleShape)
                    .background(ViewerDesign.Green.copy(alpha = 0.10f))
                    .border(1.dp, ViewerDesign.Green.copy(alpha = 0.32f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    color = ViewerDesign.White,
                    fontSize = (26f * scale).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keko Balderas Elsaleh",
                    color = ViewerDesign.White,
                    fontSize = (17f * scale).sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(s(4.dp)))
                Text(
                    text = when (decision) {
                        MailboxPreviewDecision.Accepted -> "Solicitud aceptada."
                        MailboxPreviewDecision.Denied -> "Solicitud denegada."
                        null -> "Quiere conectar contigo en WAVES."
                    },
                    color = ViewerDesign.Mist.copy(alpha = 0.92f),
                    fontSize = (12.5f * scale).sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(s(18.dp)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(s(12.dp)),
        ) {
            OutlinedButton(
                onClick = onDeny,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(s(14.dp)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ViewerDesign.White),
            ) {
                Text(
                    text = "Denegar",
                    fontSize = (13f * scale).sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(s(14.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ViewerDesign.Green,
                    contentColor = Color(0xFF03110B),
                ),
            ) {
                Text(
                    text = "Aceptar",
                    fontSize = (13f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun DefaultMailboxIcon(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.028f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ViewerDesign.Green.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w * 0.45f, h * 0.30f),
                radius = w * 0.65f,
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.45f, h * 0.30f),
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFB2C2C8), Color(0xFF4E5B61), Color(0xFF12191C)),
            ),
            topLeft = Offset(w * 0.44f, h * 0.70f),
            size = Size(w * 0.11f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f),
        )

        val front = Path().apply {
            moveTo(w * 0.18f, h * 0.34f)
            cubicTo(w * 0.20f, h * 0.14f, w * 0.43f, h * 0.14f, w * 0.58f, h * 0.26f)
            lineTo(w * 0.58f, h * 0.68f)
            lineTo(w * 0.18f, h * 0.76f)
            close()
        }
        val side = Path().apply {
            moveTo(w * 0.58f, h * 0.26f)
            lineTo(w * 0.86f, h * 0.34f)
            lineTo(w * 0.86f, h * 0.60f)
            lineTo(w * 0.58f, h * 0.68f)
            close()
        }
        val top = Path().apply {
            moveTo(w * 0.18f, h * 0.34f)
            cubicTo(w * 0.26f, h * 0.10f, w * 0.48f, h * 0.10f, w * 0.66f, h * 0.22f)
            lineTo(w * 0.86f, h * 0.34f)
            lineTo(w * 0.58f, h * 0.26f)
            close()
        }

        drawPath(
            path = front,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE0EBEE), Color(0xFF8F9BA3), Color(0xFF455159)),
            ),
        )
        drawPath(
            path = side,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFC5D0D5), Color(0xFF7A868D), Color(0xFF2C353A)),
            ),
        )
        drawPath(
            path = top,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF2F8FA), Color(0xFF9AA5AD), Color(0xFF5C666C)),
            ),
        )

        drawPath(front, color = Color.White.copy(alpha = 0.42f), style = Stroke(width = stroke))
        drawPath(side, color = Color.Black.copy(alpha = 0.35f), style = Stroke(width = stroke))
        drawPath(top, color = Color.White.copy(alpha = 0.26f), style = Stroke(width = stroke))

        drawLine(
            color = Color.Black.copy(alpha = 0.38f),
            start = Offset(w * 0.28f, h * 0.48f),
            end = Offset(w * 0.28f, h * 0.70f),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(w * 0.60f, h * 0.30f),
            end = Offset(w * 0.78f, h * 0.37f),
            strokeWidth = stroke * 0.6f,
            cap = StrokeCap.Round,
        )

        drawLine(
            color = Color(0xFFBBC9CE),
            start = Offset(w * 0.76f, h * 0.17f),
            end = Offset(w * 0.76f, h * 0.48f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        val flag = Path().apply {
            moveTo(w * 0.76f, h * 0.20f)
            lineTo(w * 0.90f, h * 0.24f)
            lineTo(w * 0.76f, h * 0.31f)
            close()
        }
        drawPath(
            path = flag,
            brush = Brush.linearGradient(
                colors = listOf(ViewerDesign.Green, Color(0xFF0C6B52)),
            ),
        )

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.43f, h * 0.92f),
            size = Size(w * 0.16f, h * 0.05f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
        )
    }
}

@Composable
private fun CajoneraBackGlyph(
    scale: Float,
) {
    Canvas(modifier = Modifier.size(34.dp * scale)) {
        val stroke = scale * 2.1f
        drawLine(
            color = ViewerDesign.White,
            start = Offset(size.width * 0.76f, size.height * 0.18f),
            end = Offset(size.width * 0.24f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ViewerDesign.White,
            start = Offset(size.width * 0.24f, size.height * 0.50f),
            end = Offset(size.width * 0.76f, size.height * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CajoneraAtmosphere(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(CajoneraPalette.Background)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ViewerDesign.Green.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.10f),
                radius = size.minDimension * 0.82f,
            ),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.18f, size.height * 0.10f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(size.width * 0.84f, size.height * 0.18f),
                radius = size.minDimension * 0.76f,
            ),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.84f, size.height * 0.18f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ViewerDesign.Green.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.52f, size.height * 0.86f),
                radius = size.minDimension * 0.90f,
            ),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.52f, size.height * 0.86f),
        )
        repeat(7) { index ->
            val y = size.height * (0.16f + index * 0.105f)
            drawLine(
                color = ViewerDesign.Green.copy(alpha = 0.032f),
                start = Offset(size.width * 0.05f, y),
                end = Offset(size.width * 0.95f, y - size.height * 0.045f),
                strokeWidth = 1f,
            )
        }
    }
}

@Composable
private fun CajoneraFurniture(
    scale: Float,
    openedCajon: CajonType?,
    pressedCajon: CajonType?,
    onDrawerClick: (CajonType) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun s(value: Dp) = value * scale

    Box(modifier = modifier) {
        CajoneraFurnitureShell(
            scale = scale,
            modifier = Modifier.fillMaxSize(),
        )

        CajonType.values().forEach { cajon ->
            DrawerCavity(
                scale = scale,
                isOpen = openedCajon == cajon,
                modifier = Modifier
                    .offset(x = s(CajoneraLayout.SlotInsetX), y = s(cajon.top))
                    .size(width = s(CajoneraLayout.SlotWidth), height = s(CajoneraLayout.SlotHeight)),
            )
        }

        CajonType.values().forEach { cajon ->
            CajonDrawer(
                type = cajon,
                scale = scale,
                isOpen = openedCajon == cajon,
                isPressed = pressedCajon == cajon,
                onClick = { onDrawerClick(cajon) },
                modifier = Modifier
                    .offset(x = s(CajoneraLayout.SlotInsetX), y = s(cajon.top))
                    .size(width = s(CajoneraLayout.SlotWidth), height = s(CajoneraLayout.SlotHeight)),
            )
        }
    }
}

@Composable
private fun CajoneraFurnitureShell(
    scale: Float,
    modifier: Modifier = Modifier,
) {
    fun s(value: Dp) = value * scale

    Canvas(modifier = modifier) {
        val bodyHeight = s(483.dp).toPx()
        val plinthTop = s(CajoneraLayout.PlinthTop).toPx()
        val plinthHeight = s(CajoneraLayout.PlinthHeight).toPx()
        val legTop = s(CajoneraLayout.LegTop).toPx()
        val legWidth = s(CajoneraLayout.LegWidth).toPx()
        val legHeight = s(CajoneraLayout.LegHeight).toPx()
        val corner = s(8.dp).toPx()
        val line = scale * 1.3f

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.32f),
            topLeft = Offset(0f, s(10.dp).toPx()),
            size = Size(size.width, bodyHeight),
            cornerRadius = CornerRadius(corner, corner),
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(CajoneraPalette.FrameTop, CajoneraPalette.FrameBottom),
            ),
            size = Size(size.width, bodyHeight),
            cornerRadius = CornerRadius(corner, corner),
        )

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    ViewerDesign.Green.copy(alpha = 0.16f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.28f),
                ),
            ),
            size = Size(size.width, bodyHeight),
            cornerRadius = CornerRadius(corner, corner),
            alpha = 0.65f,
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
            ),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, s(12.dp).toPx()),
        )

        fun separator(y: Float) {
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(s(7.dp).toPx(), y),
                end = Offset(size.width - s(7.dp).toPx(), y),
                strokeWidth = line,
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.48f),
                start = Offset(s(7.dp).toPx(), y + scale * 1.5f),
                end = Offset(size.width - s(7.dp).toPx(), y + scale * 1.5f),
                strokeWidth = line,
            )
        }

        separator(s(155.dp).toPx())
        separator(s(300.dp).toPx())
        separator(plinthTop)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(CajoneraPalette.PlinthTop, CajoneraPalette.PlinthBottom),
            ),
            topLeft = Offset(0f, plinthTop),
            size = Size(size.width, plinthHeight),
            cornerRadius = CornerRadius(corner * 0.8f, corner * 0.8f),
        )

        repeat(4) { index ->
            val y = plinthTop + plinthHeight * (0.18f + index * 0.18f)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(s(10.dp).toPx(), y),
                end = Offset(size.width - s(10.dp).toPx(), y),
                strokeWidth = scale,
            )
        }

        val legBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4A575D), Color(0xFF111719)),
        )
        drawRoundRect(
            brush = legBrush,
            topLeft = Offset(0f, legTop),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
        )
        drawRoundRect(
            brush = legBrush,
            topLeft = Offset(size.width - legWidth, legTop),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
        )

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent),
            ),
            topLeft = Offset(size.width * 0.10f, legTop + legHeight * 0.58f),
            size = Size(size.width * 0.80f, legHeight),
        )
    }
}

@Composable
private fun DrawerCavity(
    scale: Float,
    isOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    val revealAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.96f else 0.76f,
        animationSpec = tween(durationMillis = if (isOpen) 620 else 420, easing = FastOutSlowInEasing),
        label = "drawerCavityAlpha",
    )

    Box(
        modifier = modifier
            .graphicsLayer { alpha = revealAlpha }
            .clip(RoundedCornerShape(4.dp * scale))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CajoneraPalette.CavityTop, CajoneraPalette.CavityBottom),
                ),
            )
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent),
                    ),
                    size = Size(size.width, size.height * 0.36f),
                )
                drawLine(
                    color = ViewerDesign.Green.copy(alpha = if (isOpen) 0.10f else 0.04f),
                    start = Offset(0f, size.height * 0.08f),
                    end = Offset(size.width, size.height * 0.04f),
                    strokeWidth = scale,
                )
            },
    )
}

@Composable
private fun CajonDrawer(
    type: CajonType,
    scale: Float,
    isOpen: Boolean,
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun s(value: Dp) = value * scale

    val openOffset by animateDpAsState(
        targetValue = if (isOpen) s(30.dp) else 0.dp,
        animationSpec = if (isOpen) {
            spring(dampingRatio = 0.82f, stiffness = 280f)
        } else {
            tween(durationMillis = 420, easing = FastOutSlowInEasing)
        },
        label = "${type.semanticId}_offset",
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) s(3.dp) else 0.dp,
        animationSpec = tween(durationMillis = if (isPressed) 110 else 150, easing = FastOutSlowInEasing),
        label = "${type.semanticId}_pressOffset",
    )
    val openScaleX by animateFloatAsState(
        targetValue = if (isOpen) 1.035f else 1f,
        animationSpec = if (isOpen) {
            spring(dampingRatio = 0.84f, stiffness = 300f)
        } else {
            tween(durationMillis = 420, easing = FastOutSlowInEasing)
        },
        label = "${type.semanticId}_scaleX",
    )
    val openScaleY by animateFloatAsState(
        targetValue = if (isOpen) 1.025f else 1f,
        animationSpec = if (isOpen) {
            spring(dampingRatio = 0.84f, stiffness = 300f)
        } else {
            tween(durationMillis = 420, easing = FastOutSlowInEasing)
        },
        label = "${type.semanticId}_scaleY",
    )
    val pressFraction by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 110 else 150, easing = FastOutSlowInEasing),
        label = "${type.semanticId}_pressFraction",
    )
    val openFraction by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = if (isOpen) {
            spring(dampingRatio = 0.84f, stiffness = 300f)
        } else {
            tween(durationMillis = 420, easing = FastOutSlowInEasing)
        },
        label = "${type.semanticId}_openFraction",
    )
    val shadowDepth by animateDpAsState(
        targetValue = if (isOpen) s(30.dp) else s(10.dp),
        animationSpec = if (isOpen) {
            spring(dampingRatio = 0.88f, stiffness = 260f)
        } else {
            tween(durationMillis = 420, easing = FastOutSlowInEasing)
        },
        label = "${type.semanticId}_shadow",
    )

    val faceShape = RoundedCornerShape(7.dp * scale)

    Box(
        modifier = modifier
            .zIndex(1f + openFraction * 10f)
            .graphicsLayer {
                translationY = (openOffset + pressOffset).toPx()
                scaleX = openScaleX - (pressFraction * 0.012f)
                scaleY = openScaleY - (pressFraction * 0.008f)
                shadowElevation = shadowDepth.toPx()
                shape = faceShape
                clip = false
            }
            .cajoneraClickable(onClick)
            .semantics {
                role = Role.Button
                contentDescription = type.semanticId
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(faceShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CajoneraPalette.DrawerTop,
                            CajoneraPalette.DrawerMid,
                            CajoneraPalette.DrawerBottom,
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f + openFraction * 0.10f), faceShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f + openFraction * 0.10f),
                                Color.Transparent,
                            ),
                        ),
                        size = Size(size.width, size.height * 0.18f),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f + openFraction * 0.12f),
                            ),
                        ),
                        topLeft = Offset(0f, size.height * 0.64f),
                        size = Size(size.width, size.height * 0.36f),
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ViewerDesign.Green.copy(alpha = 0.06f + openFraction * 0.05f),
                                Color.Transparent,
                            ),
                        ),
                        size = Size(size.width * 0.18f, size.height),
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f + openFraction * 0.10f),
                            ),
                        ),
                        topLeft = Offset(size.width * 0.82f, 0f),
                        size = Size(size.width * 0.18f, size.height),
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.14f + openFraction * 0.10f),
                        start = Offset(0f, scale * 1.4f),
                        end = Offset(size.width, scale * 1.4f),
                        strokeWidth = scale * 1.5f,
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.32f + openFraction * 0.10f),
                        start = Offset(0f, size.height - scale * 2f),
                        end = Offset(size.width, size.height - scale * 2f),
                        strokeWidth = scale * 1.6f,
                    )
                },
        )

        MetalPlate(
            label = type.label,
            scale = scale,
            openFraction = openFraction,
            modifier = Modifier
                .offset(
                    x = s(CajoneraLayout.PlateStart),
                    y = s(CajoneraLayout.PlateTop),
                )
                .size(
                    width = s(type.plateWidth),
                    height = s(CajoneraLayout.PlateHeight),
                ),
        )

        DrawerHandle(
            scale = scale,
            openFraction = openFraction,
            modifier = Modifier
                .offset(
                    x = s(CajoneraLayout.HandleStart),
                    y = s(CajoneraLayout.HandleTop),
                )
                .size(
                    width = s(CajoneraLayout.HandleWidth),
                    height = s(CajoneraLayout.HandleHeight),
                ),
        )
    }
}

@Composable
private fun MetalPlate(
    label: String,
    scale: Float,
    openFraction: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(5.dp * scale)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF5F8F9),
                        Color(0xFFC3CDD2),
                        Color(0xFF8E989F),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.56f), shape)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f + openFraction * 0.16f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.72f, size.height),
                    ),
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.18f),
                    start = Offset(0f, size.height - scale * 1.2f),
                    end = Offset(size.width, size.height - scale * 1.2f),
                    strokeWidth = scale * 1.2f,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(0xFF12181B),
            fontSize = (12.2f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (0.5f * scale).sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DrawerHandle(
    scale: Float,
    openFraction: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(3.dp * scale)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CajoneraPalette.HandleTop,
                        CajoneraPalette.HandleBottom,
                    ),
                ),
            )
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.18f + openFraction * 0.10f),
                    start = Offset(size.width * 0.10f, scale * 0.9f),
                    end = Offset(size.width * 0.90f, scale * 0.9f),
                    strokeWidth = scale * 0.9f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.38f),
                    start = Offset(size.width * 0.10f, size.height - scale * 0.8f),
                    end = Offset(size.width * 0.90f, size.height - scale * 0.8f),
                    strokeWidth = scale * 0.9f,
                    cap = StrokeCap.Round,
                )
            },
    )
}

private enum class CajonType(
    val semanticId: String,
    val label: String,
    val top: Dp,
    val plateWidth: Dp,
) {
    CAJON_MENSAJES(
        semanticId = "CAJON_MENSAJES",
        label = "MENSAJES",
        top = 10.dp,
        plateWidth = 78.dp,
    ),
    CAJON_CHATS(
        semanticId = "CAJON_CHATS",
        label = "CHATS",
        top = 155.dp,
        plateWidth = 58.dp,
    ),
    CAJON_CARTAS(
        semanticId = "CAJON_CARTAS",
        label = "CARTAS",
        top = 300.dp,
        plateWidth = 66.dp,
    ),
}

private enum class MailboxPreviewDecision {
    Accepted,
    Denied,
}

private object CajoneraLayout {
    val BaseWidth = 390.dp
    val BaseHeight = 844.dp

    val FurnitureTop = 260.dp
    val FurnitureWidth = 220.dp
    val FurnitureHeight = 505.dp

    val SlotInsetX = 3.dp
    val SlotWidth = 214.dp
    val SlotHeight = 145.dp

    val PlateStart = 17.dp
    val PlateTop = 30.dp
    val PlateHeight = 32.dp

    val HandleStart = 76.dp
    val HandleTop = 95.dp
    val HandleWidth = 62.dp
    val HandleHeight = 6.dp

    val PlinthTop = 445.dp
    val PlinthHeight = 38.dp
    val LegTop = 480.dp
    val LegWidth = 18.dp
    val LegHeight = 25.dp
}

private object CajoneraPalette {
    val Background = Color(0xFF020504)
    val FrameTop = Color(0xFF223232)
    val FrameBottom = Color(0xFF0A1011)
    val DrawerTop = Color(0xFF394749)
    val DrawerMid = Color(0xFF1A2326)
    val DrawerBottom = Color(0xFF0D1315)
    val CavityTop = Color(0xFF040707)
    val CavityBottom = Color(0xFF131B1C)
    val PlinthTop = Color(0xFF2E3D40)
    val PlinthBottom = Color(0xFF11181A)
    val HandleTop = Color(0xFF454E53)
    val HandleBottom = Color(0xFF060809)
}

private fun Modifier.cajoneraClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

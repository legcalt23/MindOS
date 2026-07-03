@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod", "LongParameterList", "MaxLineLength")

package com.mindos.app.features.profile.card.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.features.profile.card.domain.CanvasItem
import com.mindos.app.features.profile.card.domain.ProfileCardState
import com.mindos.app.features.profile.card.domain.ProfilePost
import com.mindos.app.features.profile.card.domain.PostKind
import com.mindos.app.features.profile.card.presentation.CardMedia
import com.mindos.app.features.social.presentation.profile.ProfileMediaRenderSource
import com.mindos.app.features.social.presentation.profile.ProfilePlatformMediaPreview

/** Panel 0: free media canvas with draggable + resizable items. */
@Composable
fun CanvasPanel(
    state: ProfileCardState,
    accent: Color,
    mediaFor: (String?) -> CardMedia?,
    onSelect: (Long?) -> Unit,
    onMove: (Long, Float, Float, Float, Float) -> Unit,
    onResize: (Long, Float, Float, Float, Float) -> Unit,
    onFront: (Long) -> Unit,
    onBack: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    failedItemIds: List<Long>,
    onRetry: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onSelect(null) } },
    ) {
        val boundsW = maxWidth.value
        val boundsH = maxHeight.value

        if (state.canvasItems.isEmpty()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✦", fontSize = 24.sp, color = ProfileCardTokens.TextMuted)
                Spacer(Modifier.height(6.dp))
                Text("Lienzo libre · añade y coloca", color = ProfileCardTokens.TextGhost, fontSize = 10.sp)
            }
        }

        state.canvasItems.sortedBy { it.z }.forEach { item ->
            CanvasItemView(
                item = item,
                media = mediaFor(item.imageRef),
                selected = state.selectedCanvasItemId == item.id,
                accent = accent,
                onSelect = { onSelect(item.id) },
                onDrag = { dx, dy ->
                    val dpx = with(density) { dx.toDp().value }
                    val dpy = with(density) { dy.toDp().value }
                    onMove(item.id, dpx, dpy, boundsW, boundsH)
                },
                onResize = { dx, dy ->
                    val dpx = with(density) { dx.toDp().value }
                    val dpy = with(density) { dy.toDp().value }
                    onResize(item.id, dpx, dpy, boundsW, boundsH)
                },
                onFront = { onFront(item.id) },
                onBack = { onBack(item.id) },
                onRemove = { onRemove(item.id) },
                failed = item.id in failedItemIds,
                onRetry = { onRetry(item.id) },
            )
        }

        // add button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(100))
                .background(Brush.verticalGradient(listOf(accent, ProfileCardTokens.GoldDeep)))
                .clickable(onClick = onAdd)
                .padding(horizontal = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("＋", color = ProfileCardTokens.ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(7.dp))
                Text("Añadir", color = ProfileCardTokens.ScreenInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CanvasItemView(
    item: CanvasItem,
    media: CardMedia?,
    selected: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onFront: () -> Unit,
    onBack: () -> Unit,
    onRemove: () -> Unit,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(item.x.dp, item.y.dp)
            .size(item.width.dp, item.height.dp)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, drag -> change.consume(); onDrag(drag.x, drag.y) },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .then(
                    when {
                        failed -> Modifier.border(2.dp, ProfileCardTokens.Negative, RoundedCornerShape(12.dp))
                        selected -> Modifier.border(2.dp, accent, RoundedCornerShape(12.dp))
                        else -> Modifier.border(1.dp, ProfileCardTokens.HairlineSoft, RoundedCornerShape(12.dp))
                    },
                )
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (media != null) {
                ProfilePlatformMediaPreview(
                    source = ProfileMediaRenderSource(
                        storagePath = media.storagePath,
                        thumbnailStoragePath = null,
                        pendingPreviewBytes = media.bytes,
                        isVideo = media.isVideo,
                        contentDescription = "Medio del lienzo",
                        directUrl = media.downloadUrl,
                    ),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallback = { CanvasPlaceholderFill(item) },
                )
            } else {
                CanvasPlaceholderFill(item)
                if (item.isPlaceholder && item.label.isNotEmpty()) {
                    Text(item.label, color = ProfileCardTokens.TextMuted, fontSize = 9.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
        if (selected) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).offset(y = (-14).dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MiniControl("⬓", onBack)
                MiniControl("⬒", onFront)
                MiniControl("✕", onRemove, ProfileCardTokens.Negative)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 7.dp, y = 7.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(accent)
                    .pointerInput(item.id) {
                        detectDragGestures(onDrag = { change, drag -> change.consume(); onResize(drag.x, drag.y) })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("⤡", color = ProfileCardTokens.ScreenInk, fontSize = 11.sp)
            }
        }
        if (failed) {
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠ No se subió", color = ProfileCardTokens.Negative, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "Reintentar",
                            color = ProfileCardTokens.ScreenInk,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent)
                                .clickable(onClick = onRetry).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                        Text(
                            "Descartar",
                            color = ProfileCardTokens.White,
                            fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x40FFFFFF))
                                .clickable(onClick = onRemove).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasPlaceholderFill(item: CanvasItem) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(item.placeholderGradientStart), Color(item.placeholderGradientEnd)),
            ),
        ),
    )
}

@Composable
private fun MiniControl(glyph: String, onClick: () -> Unit, tint: Color = ProfileCardTokens.White) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xF20C0E16))
            .border(1.dp, ProfileCardTokens.HairlineSoft, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = tint, fontSize = 12.sp)
    }
}

/** Panel 1: the 4x4 posts grid. */
@Composable
fun PostsPanel(posts: List<ProfilePost>, accent: Color, onOpenPost: (ProfilePost) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        posts.chunked(4).forEach { rowPosts ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                rowPosts.forEach { post ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(listOf(Color(post.gradientStart), Color(post.gradientEnd))))
                            .clickable { onOpenPost(post) },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (post.kind) {
                            PostKind.VIDEO -> Box(
                                Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(Color(0x80000000)),
                                contentAlignment = Alignment.Center,
                            ) { Text("▶", color = ProfileCardTokens.White, fontSize = 9.sp) }
                            PostKind.GIF -> Box(
                                Modifier.align(Alignment.TopStart).padding(4.dp).clip(RoundedCornerShape(3.dp))
                                    .background(accent).padding(horizontal = 3.dp),
                            ) { Text("GIF", color = ProfileCardTokens.ScreenInk, fontSize = 6.sp, fontWeight = FontWeight.Bold) }
                            PostKind.IMAGE -> Unit
                        }
                    }
                }
            }
        }
    }
}

/** Panel 2: the competitive stats panel. */
@Composable
fun StatsPanel(state: ProfileCardState, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        // hero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(Color(0x2E4F7CFF), Color(0x1AE8C77A))))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF9FC6FF), Color(0xFF3A6FD0)))),
                contentAlignment = Alignment.Center,
            ) { Text("💎", fontSize = 20.sp) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("CLASIFICACIÓN GLOBAL", color = ProfileCardTokens.TextFaint, fontSize = 8.sp, letterSpacing = 1.5.sp)
                Text(state.generalRank.label, color = ProfileCardTokens.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(state.level.toString(), color = accent, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("NIVEL", color = ProfileCardTokens.TextFaint, fontSize = 7.5.sp, letterSpacing = 1.sp)
            }
        }
        // stat chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.statChips.forEach { chip ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0x0AFFFFFF))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(chip.value, color = ProfileCardTokens.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(chip.label.uppercase(), color = ProfileCardTokens.TextFaint, fontSize = 7.sp, letterSpacing = 0.5.sp)
                }
            }
        }
        // sparkline + win rate
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(
                modifier = Modifier.weight(1.4f).fillMaxSize().clip(RoundedCornerShape(13.dp))
                    .background(Color(0x0AFFFFFF)).padding(11.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("RENDIMIENTO", color = ProfileCardTokens.TextFaint, fontSize = 8.sp, letterSpacing = 1.sp)
                    Text("+${state.performanceTrendPercent}%", color = ProfileCardTokens.Positive, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Sparkline(accent = accent, modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(13.dp))
                    .background(Color(0x0AFFFFFF)).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WinRateRadial(percent = state.winRatePercent, accent = accent)
            }
        }
        // mode split
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.modes.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.name, color = ProfileCardTokens.TextMuted, fontSize = 8.5.sp, modifier = Modifier.width(54.dp))
                    Box(
                        Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x12FFFFFF)),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(mode.percent / 100f).fillMaxSize().clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(accent, ProfileCardTokens.GoldDeep))),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${mode.percent}%", color = ProfileCardTokens.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                }
            }
        }
    }
}

@Composable
private fun Sparkline(accent: Color, modifier: Modifier = Modifier) {
    // Normalized sample series mirroring the design's polyline (y inverted: higher = better).
    val series = listOf(0.23f, 0.36f, 0.32f, 0.59f, 0.50f, 0.77f, 0.68f, 0.91f)
    androidx.compose.foundation.Canvas(modifier) {
        val stepX = if (series.size > 1) size.width / (series.size - 1) else size.width
        val path = Path()
        series.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height * (1f - v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)))
        drawPath(path, color = accent, style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
}

@Composable
private fun WinRateRadial(percent: Float, accent: Color) {
    Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            drawArc(
                color = Color(0x14FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * (percent / 100f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatPercent(percent), color = ProfileCardTokens.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("VICTORIAS", color = ProfileCardTokens.TextFaint, fontSize = 6.5.sp, letterSpacing = 1.sp)
        }
    }
}

private fun formatPercent(value: Float): String {
    val rounded = (value * 10).toInt()
    val whole = rounded / 10
    val frac = rounded % 10
    return "$whole,$frac%"
}

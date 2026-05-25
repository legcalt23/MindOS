package com.example.neuv3.features.dm.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuv3.core.session.AuthenticatedUser
import com.example.neuv3.features.dm.domain.model.DMMessage
import com.example.neuv3.features.dm.domain.model.DMMessageType
import com.example.neuv3.features.social.domain.model.AppMediaContent
import com.example.neuv3.features.social.presentation.media.AppMediaThumbnail
import com.example.neuv3.features.social.presentation.media.ViewerDesign
import com.example.neuv3.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.neuv3.features.social.presentation.profile.ProfilePlatformMediaPreview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class GalleryFilter {
    FOTOS,
    VIDEOS,
    PUBLICACIONES,
}

enum class GalleryMediaType {
    Photo,
    Video,
}

data class GalleryMediaItem(
    val id: String,
    val type: GalleryMediaType,
    val content: AppMediaContent? = null,
    val accentColor: Color = ViewerDesign.Green,
)

@Composable
fun DMChatUserInfoRoute(
    user: AuthenticatedUser,
    chatId: String,
    otherUserId: String,
    stateHolder: DMStateHolder,
    isOnline: Boolean = true,
    onBack: () -> Unit,
    onOpenCajonera: () -> Unit = {},
    onBlockClick: () -> Unit = {},
    onOpenMedia: (AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableUsers by stateHolder.availableUsers.collectAsState()
    val messages by stateHolder.currentMessages.collectAsState()

    LaunchedEffect(user.uid, chatId, otherUserId) {
        stateHolder.resolveAndLoadChat(user.uid, otherUserId, chatId)
    }

    val partner = remember(availableUsers, otherUserId) {
        availableUsers.find { it.userId == otherUserId }
    }
    val username = remember(partner, otherUserId) {
        val rawUsername = partner?.username
            ?.trim()
            ?.removePrefix("@")
            ?.takeIf { it.isNotBlank() }
        if (rawUsername != null) {
            "@$rawUsername"
        } else {
            "@usuario"
        }
    }
    val galleryItems = remember(messages) { messages.toGalleryMediaItems() }

    WavesChatUserInfoScreen(
        username = username,
        isOnline = isOnline,
        profileImageUrl = partner?.avatarUrl,
        galleryItems = galleryItems,
        onBackClick = onBack,
        onOpenCajonera = onOpenCajonera,
        onBlockClick = onBlockClick,
        onGalleryItemClick = { item ->
            item.content?.let(onOpenMedia)
        },
        modifier = modifier,
    )
}

@Composable
fun WavesChatUserInfoScreen(
    username: String,
    isOnline: Boolean,
    profileImageUrl: String? = null,
    galleryItems: List<GalleryMediaItem> = wavesPreviewGalleryItems(),
    onBackClick: () -> Unit = {},
    onOpenCajonera: () -> Unit = {},
    onBlockClick: () -> Unit = {},
    onDrawerClick: () -> Unit = {},
    onGalleryItemClick: (GalleryMediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isMuted by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<GalleryFilter?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(WavesInfoPalette.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val scaleX = maxWidth.value / 360f
        val scaleY = maxHeight.value / 640f
        val scale = minOf(scaleX, scaleY)

        fun Float.sdp() = (this * scale).dp

        val filteredItems = remember(galleryItems, selectedFilter) {
            when (selectedFilter) {
                GalleryFilter.FOTOS -> galleryItems.filter { it.type == GalleryMediaType.Photo }
                GalleryFilter.VIDEOS -> galleryItems.filter { it.type == GalleryMediaType.Video }
                GalleryFilter.PUBLICACIONES -> emptyList()
                null -> galleryItems
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            WavesUserInfoAtmosphere(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24f.sdp()),
            ) {
                Spacer(modifier = Modifier.height(18f.sdp()))

                WavesUserInfoHeader(
                    username = username,
                    isOnline = isOnline,
                    profileImageUrl = profileImageUrl,
                    scale = scale,
                    onBackClick = onBackClick,
                    onOpenCajonera = onOpenCajonera,
                )

                Spacer(modifier = Modifier.height(18f.sdp()))

                WavesActionButtonsRow(
                    isMuted = isMuted,
                    scale = scale,
                    onToggleMute = { isMuted = !isMuted },
                    onBlockClick = onBlockClick,
                    onDrawerClick = onDrawerClick,
                )

                Spacer(modifier = Modifier.height(18f.sdp()))

                WavesMessagesButton(scale = scale)

                Spacer(modifier = Modifier.height(18f.sdp()))

                WavesGallerySection(
                    modifier = Modifier.weight(1f),
                    selectedFilter = selectedFilter,
                    filteredItems = filteredItems,
                    scale = scale,
                    onFilterSelected = { selectedFilter = it },
                    onGalleryItemClick = onGalleryItemClick,
                )
            }
        }
    }
}

@Composable
private fun WavesUserInfoHeader(
    username: String,
    isOnline: Boolean,
    profileImageUrl: String?,
    scale: Float,
    onBackClick: () -> Unit,
    onOpenCajonera: () -> Unit,
) {
    fun Float.sdp() = (this * scale).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118f.sdp())
    ) {
        WavesCircleIconButton(
            onClick = onBackClick,
            scale = scale,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            WavesBackArrowGlyph(scale = scale)
        }

        WavesOrganicButton(
            scale = scale,
            onClick = onOpenCajonera,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WavesProfileAvatar(
                username = username,
                profileImageUrl = profileImageUrl,
                scale = scale,
            )
            Spacer(modifier = Modifier.height(10f.sdp()))
            WavesUserStatus(
                username = username,
                isOnline = isOnline,
                scale = scale,
            )
        }
    }
}

@Composable
private fun WavesProfileAvatar(
    username: String,
    profileImageUrl: String?,
    scale: Float,
) {
    fun Float.sdp() = (this * scale).dp

    Box(
        modifier = Modifier
            .size(72f.sdp())
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ViewerDesign.Green.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!profileImageUrl.isNullOrBlank()) {
            ProfilePlatformMediaPreview(
                source = ProfileMediaRenderSource(
                    storagePath = null,
                    thumbnailStoragePath = null,
                    pendingPreviewBytes = null,
                    isVideo = false,
                    contentDescription = username,
                    directUrl = profileImageUrl,
                    thumbnailUrl = profileImageUrl,
                ),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                WavesAvatarFallback(username = username, scale = scale)
            }
        } else {
            WavesAvatarFallback(username = username, scale = scale)
        }
    }
}

@Composable
private fun WavesAvatarFallback(
    username: String,
    scale: Float,
) {
    val initial = remember(username) {
        username
            .removePrefix("@")
            .trim()
            .firstOrNull()
            ?.uppercase()
            ?: "W"
    }

    Text(
        text = initial,
        color = ViewerDesign.White,
        fontSize = (22f * scale).sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun WavesOrganicButton(
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun Float.sdp() = (this * scale).dp
    fun Float.ssp() = (this * scale).sp

    Box(
        modifier = modifier
            .width(104f.sdp())
            .height(34f.sdp())
            .clip(RoundedCornerShape(10f.sdp()))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10f.sdp()))
            .wavesClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "CAJONERA",
            color = ViewerDesign.White,
            fontSize = 10.2f.ssp(),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WavesUserStatus(
    username: String,
    isOnline: Boolean,
    scale: Float,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6f * scale).dp),
    ) {
        Box(
            modifier = Modifier
                .size((8f * scale).dp)
                .clip(CircleShape)
                .background(if (isOnline) WavesInfoPalette.Online else WavesInfoPalette.Alert),
        )
        Text(
            text = username,
            color = ViewerDesign.White,
            fontSize = (14f * scale).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WavesActionButtonsRow(
    isMuted: Boolean,
    scale: Float,
    onToggleMute: () -> Unit,
    onBlockClick: () -> Unit,
    onDrawerClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((12f * scale).dp),
    ) {
        WavesActionButton(
            borderColor = if (isMuted) WavesInfoPalette.Alert else Color.White.copy(alpha = 0.16f),
            scale = scale,
            onClick = onToggleMute,
            modifier = Modifier.weight(1f),
        ) {
            WavesSpeakerGlyph(
                scale = scale,
                tint = if (isMuted) WavesInfoPalette.Alert else ViewerDesign.White,
                muted = isMuted,
            )
        }

        WavesActionButton(
            borderColor = Color.White.copy(alpha = 0.16f),
            scale = scale,
            onClick = onBlockClick,
            modifier = Modifier.weight(1f),
        ) {
            WavesBlockGlyph(scale = scale, tint = ViewerDesign.White)
        }

        WavesActionButton(
            borderColor = Color.White.copy(alpha = 0.16f),
            scale = scale,
            onClick = onDrawerClick,
            modifier = Modifier.weight(1f),
        ) {
            WavesDrawerGlyph(scale = scale, tint = ViewerDesign.White)
        }
    }
}

@Composable
private fun WavesActionButton(
    borderColor: Color,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
) {
    fun Float.sdp() = (this * scale).dp

    Box(
        modifier = modifier
            .height(48f.sdp())
            .clip(RoundedCornerShape(14f.sdp()))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, borderColor, RoundedCornerShape(14f.sdp()))
            .wavesClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(26f.sdp()),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
private fun WavesMessagesButton(scale: Float) {
    fun Float.sdp() = (this * scale).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38f.sdp())
            .clip(RoundedCornerShape(12f.sdp()))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12f.sdp())),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "MENSAJES",
            color = ViewerDesign.White,
            fontSize = (12f * scale).sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(8f.sdp()))
        WavesStarGlyph(scale = scale, tint = ViewerDesign.White)
    }
}

@Composable
private fun WavesGallerySection(
    modifier: Modifier = Modifier,
    selectedFilter: GalleryFilter?,
    filteredItems: List<GalleryMediaItem>,
    scale: Float,
    onFilterSelected: (GalleryFilter) -> Unit,
    onGalleryItemClick: (GalleryMediaItem) -> Unit,
) {
    fun Float.sdp() = (this * scale).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8f.sdp()))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8f.sdp()))
                .padding(horizontal = 10f.sdp(), vertical = 5f.sdp()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GALER\u00CDA",
                color = WavesInfoPalette.Mist,
                fontSize = (12.5f * scale).sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(14f.sdp()))

        WavesGalleryFilters(
            selectedFilter = selectedFilter,
            scale = scale,
            onFilterSelected = onFilterSelected,
        )

        Spacer(modifier = Modifier.height(12f.sdp()))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f))
        )

        Spacer(modifier = Modifier.height(12f.sdp()))

        if (selectedFilter == GalleryFilter.PUBLICACIONES) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No hay publicaciones compartidas",
                    color = ViewerDesign.White.copy(alpha = 0.45f),
                    fontSize = (12f * scale).sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No hay contenido compartido",
                    color = ViewerDesign.White.copy(alpha = 0.40f),
                    fontSize = (12f * scale).sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6f.sdp()),
                verticalArrangement = Arrangement.spacedBy(6f.sdp()),
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    WavesGalleryItem(
                        item = item,
                        scale = scale,
                        onClick = { onGalleryItemClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WavesGalleryFilters(
    selectedFilter: GalleryFilter?,
    scale: Float,
    onFilterSelected: (GalleryFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WavesGalleryFilterLabel(
            label = "FOTOS",
            selected = selectedFilter == GalleryFilter.FOTOS,
            scale = scale,
            onClick = { onFilterSelected(GalleryFilter.FOTOS) },
        )
        WavesGalleryFilterLabel(
            label = "VIDEOS",
            selected = selectedFilter == GalleryFilter.VIDEOS,
            scale = scale,
            onClick = { onFilterSelected(GalleryFilter.VIDEOS) },
        )
        WavesGalleryFilterLabel(
            label = "PUBLICACIONES",
            selected = selectedFilter == GalleryFilter.PUBLICACIONES,
            scale = scale,
            onClick = { onFilterSelected(GalleryFilter.PUBLICACIONES) },
        )
    }
}

@Composable
private fun WavesGalleryFilterLabel(
    label: String,
    selected: Boolean,
    scale: Float,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.wavesClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((4f * scale).dp),
    ) {
        Text(
            text = label,
            color = if (selected) ViewerDesign.White else ViewerDesign.White.copy(alpha = 0.48f),
            fontSize = (10.5f * scale).sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) (label.length * 4.2f * scale).dp else 0.dp)
                .background(if (selected) ViewerDesign.White else Color.Transparent)
        )
    }
}

@Composable
private fun WavesGalleryItem(
    item: GalleryMediaItem,
    scale: Float,
    onClick: () -> Unit,
) {
    fun Float.sdp() = (this * scale).dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10f.sdp()))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        item.accentColor.copy(alpha = 0.15f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10f.sdp()))
            .wavesClickable(onClick),
    ) {
        if (item.content != null) {
            AppMediaThumbnail(
                content = item.content,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                WavesGalleryPlaceholder(item = item)
            }
        } else {
            WavesGalleryPlaceholder(item = item)
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f)),
                    ),
                )
        )

        if (item.type == GalleryMediaType.Video) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30f.sdp())
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f)),
                contentAlignment = Alignment.Center,
            ) {
                WavesPlayGlyph(scale = scale, tint = ViewerDesign.White)
            }
        }
    }
}

@Composable
private fun WavesGalleryPlaceholder(item: GalleryMediaItem) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(4) { index ->
            val y = size.height * (0.18f + index * 0.18f)
            drawLine(
                color = item.accentColor.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y - size.height * 0.08f),
                strokeWidth = 1f,
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(item.accentColor.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * 0.28f, size.height * 0.28f),
                radius = size.minDimension * 0.7f,
            ),
            radius = size.minDimension * 0.5f,
            center = Offset(size.width * 0.28f, size.height * 0.28f),
        )
    }
}

@Composable
private fun WavesCircleIconButton(
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    fun Float.sdp() = (this * scale).dp

    Box(
        modifier = modifier
            .size(44f.sdp())
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .wavesClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun WavesUserInfoAtmosphere(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(ViewerDesign.Black)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ViewerDesign.Green.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.20f, size.height * 0.12f),
                radius = size.minDimension * 0.90f,
            ),
            radius = size.minDimension * 0.45f,
            center = Offset(size.width * 0.20f, size.height * 0.12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.18f),
                radius = size.minDimension * 0.72f,
            ),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.82f, size.height * 0.18f),
        )
        repeat(6) { index ->
            val y = size.height * (0.20f + index * 0.105f)
            drawLine(
                color = ViewerDesign.Green.copy(alpha = 0.035f),
                start = Offset(size.width * 0.06f, y),
                end = Offset(size.width * 0.94f, y - size.height * 0.04f),
                strokeWidth = 1f,
            )
        }
    }
}

@Composable
private fun WavesBackArrowGlyph(scale: Float) {
    Canvas(modifier = Modifier.size((16f * scale).dp)) {
        val strokeWidth = 2f * scale
        drawLine(
            color = ViewerDesign.White,
            start = Offset(size.width * 0.72f, size.height * 0.16f),
            end = Offset(size.width * 0.28f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ViewerDesign.White,
            start = Offset(size.width * 0.28f, size.height * 0.50f),
            end = Offset(size.width * 0.72f, size.height * 0.84f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun WavesSpeakerGlyph(
    scale: Float,
    tint: Color,
    muted: Boolean,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.8f * scale
        val speaker = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.38f)
            lineTo(size.width * 0.32f, size.height * 0.38f)
            lineTo(size.width * 0.54f, size.height * 0.18f)
            lineTo(size.width * 0.54f, size.height * 0.82f)
            lineTo(size.width * 0.32f, size.height * 0.62f)
            lineTo(size.width * 0.16f, size.height * 0.62f)
            close()
        }
        drawPath(speaker, color = tint, style = Stroke(width = strokeWidth))
        if (!muted) {
            drawArc(
                color = tint,
                startAngle = -42f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(size.width * 0.48f, size.height * 0.28f),
                size = Size(size.width * 0.26f, size.height * 0.44f),
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = tint,
                startAngle = -42f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(size.width * 0.58f, size.height * 0.18f),
                size = Size(size.width * 0.24f, size.height * 0.64f),
                style = Stroke(width = strokeWidth),
            )
        } else {
            drawLine(
                color = tint,
                start = Offset(size.width * 0.22f, size.height * 0.82f),
                end = Offset(size.width * 0.82f, size.height * 0.18f),
                strokeWidth = strokeWidth * 1.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WavesBlockGlyph(
    scale: Float,
    tint: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.8f * scale
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.36f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.28f, size.height * 0.74f),
            end = Offset(size.width * 0.72f, size.height * 0.26f),
            strokeWidth = strokeWidth * 1.15f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun WavesDrawerGlyph(
    scale: Float,
    tint: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.8f * scale
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.18f, size.height * 0.24f),
            size = Size(size.width * 0.64f, size.height * 0.52f),
            cornerRadius = CornerRadius(size.width * 0.08f, size.width * 0.08f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.30f, size.height * 0.50f),
            end = Offset(size.width * 0.70f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun WavesStarGlyph(
    scale: Float,
    tint: Color,
) {
    Canvas(modifier = Modifier.size((15f * scale).dp)) {
        val path = starPath(
            centerX = size.width / 2f,
            centerY = size.height / 2f,
            outerRadius = size.minDimension * 0.42f,
            innerRadius = size.minDimension * 0.18f,
        )
        drawPath(path = path, color = tint, style = Stroke(width = 1.5f * scale))
    }
}

@Composable
private fun WavesPlayGlyph(
    scale: Float,
    tint: Color,
) {
    Canvas(modifier = Modifier.size((10f * scale).dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.18f)
            lineTo(size.width * 0.80f, size.height * 0.50f)
            lineTo(size.width * 0.26f, size.height * 0.82f)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

private fun List<DMMessage>.toGalleryMediaItems(): List<GalleryMediaItem> =
    asSequence()
        .filter { message ->
            message.type == DMMessageType.Image || message.type == DMMessageType.Video
        }
        .sortedByDescending { it.createdAt }
        .mapNotNull { message ->
            val content = message.toAppMediaContent() ?: return@mapNotNull null
            GalleryMediaItem(
                id = message.id,
                type = if (message.type == DMMessageType.Video) GalleryMediaType.Video else GalleryMediaType.Photo,
                content = content,
                accentColor = if (message.type == DMMessageType.Video) WavesInfoPalette.VideoAccent else WavesInfoPalette.PhotoAccent,
            )
        }
        .toList()

private fun wavesPreviewGalleryItems(): List<GalleryMediaItem> =
    listOf(
        GalleryMediaItem(id = "photo-1", type = GalleryMediaType.Photo, accentColor = WavesInfoPalette.PhotoAccent),
        GalleryMediaItem(id = "video-1", type = GalleryMediaType.Video, accentColor = WavesInfoPalette.VideoAccent),
        GalleryMediaItem(id = "photo-2", type = GalleryMediaType.Photo, accentColor = Color(0xFF8F5CFF)),
        GalleryMediaItem(id = "photo-3", type = GalleryMediaType.Photo, accentColor = Color(0xFF5AC8FA)),
        GalleryMediaItem(id = "video-2", type = GalleryMediaType.Video, accentColor = Color(0xFFFF6A5B)),
        GalleryMediaItem(id = "photo-4", type = GalleryMediaType.Photo, accentColor = WavesInfoPalette.PhotoAccent),
    )

private fun starPath(
    centerX: Float,
    centerY: Float,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path().apply { fillType = PathFillType.EvenOdd }
    val section = PI / 5.0
    path.moveTo(centerX, centerY - outerRadius)
    repeat(5) { index ->
        val outerAngle = -PI / 2 + section * 2.0 * index
        val innerAngle = outerAngle + section
        path.lineTo(
            centerX + (cos(innerAngle) * innerRadius).toFloat(),
            centerY + (sin(innerAngle) * innerRadius).toFloat(),
        )
        path.lineTo(
            centerX + (cos(outerAngle + section * 2.0) * outerRadius).toFloat(),
            centerY + (sin(outerAngle + section * 2.0) * outerRadius).toFloat(),
        )
    }
    path.close()
    return path
}

private fun Modifier.wavesClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

private object WavesInfoPalette {
    val Black = Color(0xFF020504)
    val Mist = Color(0xFFA7CFC1)
    val Online = Color(0xFF20F26B)
    val Alert = Color(0xFFFF4545)
    val PhotoAccent = Color(0xFF13ECA4)
    val VideoAccent = Color(0xFF8F5CFF)
}

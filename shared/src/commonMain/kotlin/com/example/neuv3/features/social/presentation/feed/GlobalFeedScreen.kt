package com.example.neuv3.features.social.presentation.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuv3.core.session.AuthenticatedUser
import com.example.neuv3.features.social.domain.feed.GlobalFeedAttachment
import com.example.neuv3.features.social.domain.feed.GlobalFeedPost
import com.example.neuv3.features.social.domain.feed.GlobalFeedPostType
import com.example.neuv3.features.social.domain.feed.GlobalFeedRepository
import com.example.neuv3.features.social.domain.feed.GlobalFeedTextAlign
import kotlin.math.sin

@Composable
fun GlobalFeedScreen(
    user: AuthenticatedUser,
    repository: GlobalFeedRepository,
    onBack: () -> Unit,
    onOpenProfile: (userId: String, username: String, displayName: String) -> Unit,
    onOpenMedia: (com.example.neuv3.features.social.domain.model.AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val holder = rememberGlobalFeedStateHolder(user, repository, scope)
    val state = holder.uiState
    val player = rememberFeedAudioPlayer()
    val playerState by player.state.collectAsStateCompat()
    val listState = rememberLazyListState()
    val visibleIndex by rememberVisibleFeedIndex(listState)
    var expandedPostId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(holder) { holder.load() }

    Box(modifier = modifier.fillMaxSize().background(FeedBlack)) {
        when {
            state.isLoading && state.posts.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FeedGreen, modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
            }
            state.posts.isEmpty() -> FeedNotice(state.errorMessage ?: "SIN POSTS", onRefresh = holder::refresh)
            else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight
                LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                    itemsIndexed(state.posts, key = { _, post -> post.id }) { index, post ->
                        FeedPostCard(
                            post = post,
                            isActive = index == visibleIndex,
                            viewportHeight = viewportHeight,
                            playerState = playerState,
                            onPlayPauseAudio = { source -> player.playOrPause(source) },
                            onOpenProfile = onOpenProfile,
                            onOpenMedia = onOpenMedia,
                            displayMode = if (post.isFullscreenPreviewPost()) {
                                FeedPostDisplayMode.ImmersivePreview
                            } else {
                                FeedPostDisplayMode.CompactPreview
                            },
                            onOpenDetail = { expandedPostId = post.id },
                        )
                    }
                }
                val expandedPost = remember(state.posts, expandedPostId) {
                    state.posts.firstOrNull { it.id == expandedPostId }
                }
                if (expandedPost != null) {
                    FeedPostCard(
                        post = expandedPost,
                        isActive = true,
                        viewportHeight = viewportHeight,
                        playerState = playerState,
                        onPlayPauseAudio = { source -> player.playOrPause(source) },
                        onOpenProfile = onOpenProfile,
                        onOpenMedia = onOpenMedia,
                        displayMode = FeedPostDisplayMode.FullscreenDetail,
                        onOpenDetail = {},
                        onDismissDetail = { expandedPostId = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberVisibleFeedIndex(listState: LazyListState): State<Int> = remember(listState) {
    derivedStateOf {
        val layoutInfo = listState.layoutInfo
        val visible = layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) {
            listState.firstVisibleItemIndex
        } else {
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            visible.maxByOrNull { item ->
                val top = maxOf(item.offset, viewportStart)
                val bottom = minOf(item.offset + item.size, viewportEnd)
                (bottom - top).coerceAtLeast(0)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }
}

@Composable
private fun FeedPostCard(
    post: GlobalFeedPost,
    isActive: Boolean,
    viewportHeight: androidx.compose.ui.unit.Dp,
    playerState: FeedAudioPlayerState,
    onPlayPauseAudio: (FeedAudioSource) -> Unit,
    onOpenProfile: (String, String, String) -> Unit,
    onOpenMedia: (com.example.neuv3.features.social.domain.model.AppMediaContent) -> Unit,
    displayMode: FeedPostDisplayMode,
    onOpenDetail: () -> Unit,
    onDismissDetail: (() -> Unit)? = null,
) {
    val primaryAttachment = post.attachments.firstOrNull()
    val isVideoPost = post.postType == GlobalFeedPostType.VIDEO && primaryAttachment != null
    val isFullscreenLayout = displayMode != FeedPostDisplayMode.CompactPreview
    val showBottomChrome = displayMode != FeedPostDisplayMode.CompactPreview
    val downloader = rememberFeedAttachmentDownloader()
    var isEyeOpen by rememberSaveable(post.id) { mutableStateOf(true) }
    var isBottomMenuOpen by rememberSaveable(post.id) { mutableStateOf(false) }
    var isVideoPlaying by rememberSaveable(post.id) { mutableStateOf(isVideoPost && isActive) }
    var showPlayStopOverlay by remember { mutableStateOf(false) }
    var playStopState by remember { mutableStateOf(PlayStopState.PLAY) }
    var doubleTapOffset by remember { mutableStateOf(Offset.Unspecified) }
    var showBurst by remember { mutableStateOf(false) }
    var burstNonce by remember { mutableIntStateOf(0) }
    var isSquareBroken by rememberSaveable(post.id) { mutableStateOf(false) }
    var isDownloading by rememberSaveable(post.id) { mutableStateOf(false) }
    var downloadProgress by rememberSaveable(post.id) { mutableFloatStateOf(0f) }
    var showDescriptionOverlay by rememberSaveable(post.id) { mutableStateOf(false) }
    val onDownloadProgress by rememberUpdatedState { progress: Float -> downloadProgress = progress.coerceIn(0f, 1f) }
    val onDownloadState by rememberUpdatedState { downloading: Boolean -> isDownloading = downloading }
    val hasDescription = !post.description.isNullOrBlank()
    val triggerBurst: (Offset) -> Unit = { offset ->
        doubleTapOffset = offset
        showBurst = true
        burstNonce += 1
        isSquareBroken = true
    }

    LaunchedEffect(isActive, isVideoPost) {
        if (isVideoPost) {
            isVideoPlaying = isActive
            if (!isActive) showPlayStopOverlay = false
        }
    }

    LaunchedEffect(burstNonce) {
        if (burstNonce > 0) {
            kotlinx.coroutines.delay(1000L)
            showBurst = false
        }
    }

    LaunchedEffect(showPlayStopOverlay, playStopState) {
        if (showPlayStopOverlay) {
            kotlinx.coroutines.delay(900L)
            showPlayStopOverlay = false
        }
    }

    DisposableEffect(downloader) {
        onDispose { downloader.cancel() }
    }

    if (displayMode == FeedPostDisplayMode.CompactPreview) {
        CompactFeedPost(
            post = post,
            isEyeOpen = isEyeOpen,
            onProfileClick = { onOpenProfile(post.userId, post.authorUsername, post.authorDisplayName) },
            onEyeToggle = { isEyeOpen = !isEyeOpen },
            playerState = playerState,
            onOpenDetail = onOpenDetail,
            onDoubleTap = triggerBurst,
            burstVisible = showBurst,
            burstNonce = burstNonce,
            burstOffset = doubleTapOffset,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewportHeight)
            .then(
                if (post.isFullscreenPreviewPost()) {
                    Modifier.background(FeedBlack)
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF050908), Color(0xFF0C1715), Color(0xFF050908)),
                        ),
                    )
                }
            )
    ) {
        if (post.isFullscreenPreviewPost()) {
            FeedPostMediaSurface(
                post = post,
                attachment = primaryAttachment,
                isActive = isActive,
                isPlaying = isVideoPlaying,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (displayMode == FeedPostDisplayMode.ImmersivePreview && primaryAttachment != null) {
                            Modifier.pointerInput(primaryAttachment.id) {
                                detectTapGestures(onLongPress = { onOpenMedia(primaryAttachment.toAppMediaContent()) })
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
            FeedScrims()
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(post.id, displayMode, isVideoPost, isVideoPlaying, showDescriptionOverlay) {
                    if (!isFullscreenLayout) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (showDescriptionOverlay) return@detectTapGestures
                            triggerBurst(offset)
                        },
                        onTap = {
                            if (showDescriptionOverlay) {
                                showDescriptionOverlay = false
                                return@detectTapGestures
                            }
                            if (displayMode == FeedPostDisplayMode.FullscreenDetail) {
                                onDismissDetail?.invoke()
                                return@detectTapGestures
                            }
                            if (isVideoPost) {
                                isVideoPlaying = !isVideoPlaying
                                playStopState = if (isVideoPlaying) PlayStopState.PLAY else PlayStopState.STOP
                                showPlayStopOverlay = true
                            }
                        },
                    )
                },
        )
        FeedHeaderOverlay(
            username = post.authorUsername,
            isEyeOpen = isEyeOpen,
            onProfileClick = { onOpenProfile(post.userId, post.authorUsername, post.authorDisplayName) },
            onEyeToggle = { isEyeOpen = !isEyeOpen },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
        if (hasDescription) {
            DescriptionFloatingButton(
                onClick = { showDescriptionOverlay = !showDescriptionOverlay },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 12.dp),
            )
        }
        if (displayMode == FeedPostDisplayMode.FullscreenDetail && onDismissDetail != null) {
            DismissDetailChip(
                onDismiss = onDismissDetail,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = if (hasDescription) 68.dp else 16.dp, end = 16.dp),
            )
        } else if (!showDescriptionOverlay) {
            PlayStopOverlay(
                visible = showPlayStopOverlay,
                state = playStopState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 18.dp, end = 16.dp),
            )
        }
        BrokenSquareBurstOverlay(
            visible = showBurst,
            burstNonce = burstNonce,
            offset = doubleTapOffset,
            modifier = Modifier.fillMaxSize(),
        )
        DownloadProgressEdge(
            progress = downloadProgress,
            visible = showBottomChrome && (isDownloading || downloadProgress > 0f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = if (isBottomMenuOpen) 18.dp else 124.dp, vertical = if (isBottomMenuOpen) 118.dp else 84.dp),
        )
        BottomCapsuleHandle(
            visible = showBottomChrome && !isBottomMenuOpen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .pointerInput(post.id, isBottomMenuOpen, showBottomChrome) {
                    if (!showBottomChrome) return@pointerInput
                    detectVerticalDragGestures(onVerticalDrag = { _, dragAmount ->
                        if (dragAmount < -14f) isBottomMenuOpen = true
                    })
                },
        )
        BottomGlassMenu(
            visible = showBottomChrome && isBottomMenuOpen,
            isSquareBroken = isSquareBroken,
            onClose = { isBottomMenuOpen = false },
            onToggleSquare = { isSquareBroken = !isSquareBroken },
            onDownload = {
                primaryAttachment?.let { attachment ->
                    downloader.download(
                        attachment = attachment,
                        onProgress = onDownloadProgress,
                        onStateChanged = onDownloadState,
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        )
        when (post.postType) {
            GlobalFeedPostType.TEXT -> TextPostOverlay(
                post = post,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            )
            GlobalFeedPostType.AUDIO -> AudioPostOverlay(
                post = post,
                playerState = playerState,
                onPlayPauseAudio = onPlayPauseAudio,
                compactPreview = false,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            )
            GlobalFeedPostType.PHOTO,
            GlobalFeedPostType.VIDEO -> Unit
        }
        if (primaryAttachment != null && showBottomChrome && !isBottomMenuOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 96.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .pointerInput(primaryAttachment.id) {
                        detectTapGestures(onLongPress = { onOpenMedia(primaryAttachment.toAppMediaContent()) })
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(primaryAttachment.fileName, color = FeedWhite.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (hasDescription && showDescriptionOverlay) {
            PostDescriptionOverlay(
                description = post.description.orEmpty(),
                onDismiss = { showDescriptionOverlay = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CompactFeedPost(
    post: GlobalFeedPost,
    isEyeOpen: Boolean,
    onProfileClick: () -> Unit,
    onEyeToggle: () -> Unit,
    playerState: FeedAudioPlayerState,
    onOpenDetail: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
    burstVisible: Boolean,
    burstNonce: Int,
    burstOffset: Offset,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color(0xFF0D1413).copy(alpha = 0.92f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
            .pointerInput(post.id) {
                detectTapGestures(
                    onTap = { onOpenDetail() },
                    onDoubleTap = onDoubleTap,
                )
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeedHeaderOverlay(
                username = post.authorUsername,
                isEyeOpen = isEyeOpen,
                onProfileClick = onProfileClick,
                onEyeToggle = onEyeToggle,
            )
            when (post.postType) {
                GlobalFeedPostType.TEXT -> TextPostOverlay(
                    post = post,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth(),
                )
                GlobalFeedPostType.AUDIO -> AudioPostOverlay(
                    post = post,
                    playerState = playerState,
                    onPlayPauseAudio = {},
                    compactPreview = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                GlobalFeedPostType.PHOTO,
                GlobalFeedPostType.VIDEO -> Unit
            }
        }
        BrokenSquareBurstOverlay(
            visible = burstVisible,
            burstNonce = burstNonce,
            offset = burstOffset,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun BoxScope.FeedScrims() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.64f), Color.Transparent))),
    )
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(280.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))),
    )
}

@Composable
private fun FeedHeaderOverlay(
    username: String,
    isEyeOpen: Boolean,
    onProfileClick: () -> Unit,
    onEyeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "@${username.removePrefix("@")}",
            color = FeedWhite,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.pointerInput(username) { detectTapGestures(onTap = { onProfileClick() }) },
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .pointerInput(isEyeOpen) { detectTapGestures(onTap = { onEyeToggle() }) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isEyeOpen) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = FeedWhite,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DismissDetailChip(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "CERRAR",
            color = FeedWhite,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        )
    }
}

@Composable
private fun PlayStopOverlay(
    visible: Boolean,
    state: PlayStopState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(tween(240), initialScale = 0.84f),
        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.92f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (state == PlayStopState.PLAY) FeedPlayGreen.copy(alpha = 0.92f)
                    else FeedStopRed.copy(alpha = 0.92f),
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (state == PlayStopState.PLAY) "PLAY" else "STOP",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
            )
        }
    }
}

@Composable
private fun PostDescriptionOverlay(
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(onClick = onDismiss),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .widthIn(max = 680.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Descripción",
                    color = FeedWhite.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = description,
                    color = FeedWhite,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                )
            }
        }
    }
}

@Composable
private fun BrokenSquareBurstOverlay(
    visible: Boolean,
    burstNonce: Int,
    offset: Offset,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.72f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(burstNonce) {
        if (burstNonce == 0 || offset == Offset.Unspecified) return@LaunchedEffect
        scale.snapTo(0.72f)
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(durationMillis = 120))
        scale.animateTo(1.16f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
        alpha.animateTo(0f, tween(durationMillis = 640))
    }

    if (visible && offset != Offset.Unspecified) {
        Canvas(modifier = modifier) {
            val sizePx = 116.dp.toPx() * scale.value
            val topLeft = Offset(offset.x - sizePx / 2f, offset.y - sizePx / 2f)
            drawRoundRect(
                color = FeedWhite.copy(alpha = 0.16f * alpha.value),
                topLeft = topLeft,
                size = Size(sizePx, sizePx),
                cornerRadius = CornerRadius(sizePx * 0.18f, sizePx * 0.18f),
            )
            drawRoundRect(
                color = FeedWhite.copy(alpha = alpha.value),
                topLeft = topLeft,
                size = Size(sizePx, sizePx),
                cornerRadius = CornerRadius(sizePx * 0.18f, sizePx * 0.18f),
                style = Stroke(width = 5.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(sizePx * 0.12f)),
            )
            drawLine(
                color = FeedWhite.copy(alpha = alpha.value),
                start = Offset(topLeft.x + sizePx * 0.84f, topLeft.y + sizePx * 0.16f),
                end = Offset(topLeft.x + sizePx * 0.16f, topLeft.y + sizePx * 0.84f),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun BottomCapsuleHandle(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.9f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(width = 112.dp, height = 28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.18f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp)),
        ) {
            Canvas(modifier = Modifier.align(Alignment.Center).size(width = 48.dp, height = 4.dp)) {
                drawRoundRect(color = FeedWhite.copy(alpha = 0.9f), cornerRadius = CornerRadius(size.height / 2f, size.height / 2f))
            }
        }
    }
}

@Composable
private fun BottomGlassMenu(
    visible: Boolean,
    isSquareBroken: Boolean,
    onClose: () -> Unit,
    onToggleSquare: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(240), initialScale = 0.92f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 14f) onClose()
                    })
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(modifier = Modifier.align(Alignment.CenterHorizontally).size(width = 48.dp, height = 4.dp)) {
                drawRoundRect(color = FeedWhite.copy(alpha = 0.84f), cornerRadius = CornerRadius(size.height / 2f, size.height / 2f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedMenuButton(icon = { Icon(Icons.Filled.Redeem, null, tint = FeedWhite, modifier = Modifier.size(22.dp)) }, onTap = {})
                FeedMenuButton(icon = { Icon(Icons.Filled.ModeComment, null, tint = FeedWhite, modifier = Modifier.size(22.dp)) }, onTap = {})
                FeedMenuButton(icon = { BrokenSquareIcon(isBroken = isSquareBroken, modifier = Modifier.size(24.dp)) }, onTap = onToggleSquare)
                FeedMenuButton(icon = { SquareCounterIcon() }, onTap = {})
                FeedMenuButton(icon = { Icon(Icons.Filled.Download, null, tint = FeedWhite, modifier = Modifier.size(22.dp)) }, onTap = onDownload)
            }
        }
    }
}

@Composable
private fun FeedMenuButton(
    icon: @Composable () -> Unit,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun DownloadProgressEdge(
    progress: Float,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxSize()
                    .background(FeedGreen),
            )
        }
    }
}

@Composable
private fun TextPostOverlay(
    post: GlobalFeedPost,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val style = post.textStyle
    val align = when (style?.textAlign ?: GlobalFeedTextAlign.Center) {
        GlobalFeedTextAlign.Start -> TextAlign.Start
        GlobalFeedTextAlign.Center -> TextAlign.Center
        GlobalFeedTextAlign.End -> TextAlign.End
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape((style?.cornerRadiusDp ?: 28f).dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(style?.backgroundPrimaryArgb ?: 0xFF071311),
                        Color(style?.backgroundSecondaryArgb ?: 0xFF102A24),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape((style?.cornerRadiusDp ?: 28f).dp))
            .padding((style?.paddingDp ?: 24f).dp),
    ) {
        Text(
            text = post.text.orEmpty(),
            color = Color(style?.textColorArgb ?: 0xFFF4FFF9),
            textAlign = align,
            maxLines = maxLines,
            overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = (style?.fontSizeSp ?: 30f).sp,
                fontWeight = FontWeight(style?.fontWeight ?: 700),
                fontStyle = if (style?.italic == true) FontStyle.Italic else FontStyle.Normal,
                lineHeight = ((style?.fontSizeSp ?: 30f) * (style?.lineHeightEm ?: 1.18f)).sp,
            ),
        )
    }
}

@Composable
private fun AudioPostOverlay(
    post: GlobalFeedPost,
    playerState: FeedAudioPlayerState,
    onPlayPauseAudio: (FeedAudioSource) -> Unit,
    compactPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        post.attachments.forEach { audio ->
            val playback = playerState.forAudio(audio.id)
            AudioFeedCapsule(audio, playback, compactPreview) {
                onPlayPauseAudio(
                    FeedAudioSource(
                        audioId = audio.id,
                        remoteUrl = audio.url,
                        mimeType = audio.mimeType,
                        durationMs = audio.durationMs,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AudioFeedCapsule(
    audio: GlobalFeedAttachment,
    playback: FeedAudioPlaybackUiState,
    compactPreview: Boolean,
    onPlayPause: () -> Unit,
) {
    val waveform = fakeWaveform(audio.id, audio.durationMs ?: 0L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .then(
                    if (compactPreview) {
                        Modifier
                    } else {
                        Modifier.pointerInput(playback.isPlaying) { detectTapGestures(onTap = { onPlayPause() }) }
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = if (playback.isPlaying) "II" else ">", color = FeedWhite, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
        }
        Canvas(modifier = Modifier.weight(1f).height(42.dp)) { drawWaveform(waveform, playback.progress.coerceIn(0f, 1f)) }
        Text(formatDuration(if (playback.durationMs > 0L) playback.durationMs else audio.durationMs), color = FeedWhite, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
    }
}

private enum class FeedPostDisplayMode {
    ImmersivePreview,
    CompactPreview,
    FullscreenDetail,
}

private fun GlobalFeedPost.isFullscreenPreviewPost(): Boolean =
    postType == GlobalFeedPostType.PHOTO || postType == GlobalFeedPostType.VIDEO

@Composable
private fun FeedNotice(title: String, onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = FeedWhite, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text("Actualiza el feed o publica desde Home > Creador.", color = FeedMist, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(
                "ACTUALIZAR",
                color = FeedWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onRefresh() }) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun BrokenSquareIcon(isBroken: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(color = FeedWhite, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()), style = Stroke(width = 3.dp.toPx()))
        if (isBroken) {
            drawLine(
                color = FeedWhite,
                start = Offset(size.width * 0.84f, size.height * 0.16f),
                end = Offset(size.width * 0.16f, size.height * 0.84f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SquareCounterIcon() {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawRoundRect(color = FeedWhite, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()), style = Stroke(width = 3.dp.toPx()))
        }
        Text("1", color = FeedWhite, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
    }
}

private fun DrawScope.drawWaveform(values: List<Float>, progress: Float) {
    if (values.isEmpty()) return
    val step = size.width / (values.size - 1).coerceAtLeast(1)
    val centerY = size.height / 2f
    values.forEachIndexed { index, value ->
        val x = step * index
        val amplitude = value * size.height * 0.42f
        val lineColor = if (x <= size.width * progress) FeedGreen else Color.White.copy(alpha = 0.18f)
        drawLine(
            color = lineColor,
            start = Offset(x, centerY - amplitude),
            end = Offset(x, centerY + amplitude),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun fakeWaveform(seed: String, durationMs: Long): List<Float> = List(56) { index ->
    val phase = index / 56f
    val base = sin((phase * 10f + seed.hashCode() * 0.0001f + durationMs * 0.0002f).toDouble()).toFloat()
    (0.25f + kotlin.math.abs(base) * 0.55f).coerceIn(0.12f, 0.9f)
}

private fun formatDuration(durationMs: Long?): String {
    val total = ((durationMs ?: 0L) / 1000L).toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
private fun kotlinx.coroutines.flow.StateFlow<FeedAudioPlayerState>.collectAsStateCompat(): State<FeedAudioPlayerState> =
    collectAsState(initial = value)

private enum class PlayStopState {
    PLAY,
    STOP,
}

interface FeedAttachmentDownloader {
    fun download(attachment: GlobalFeedAttachment, onProgress: (Float) -> Unit, onStateChanged: (Boolean) -> Unit)
    fun cancel()
}

@Composable
expect fun rememberFeedAttachmentDownloader(): FeedAttachmentDownloader

@Composable
expect fun FeedPostMediaSurface(
    post: GlobalFeedPost,
    attachment: GlobalFeedAttachment?,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
)

private fun GlobalFeedAttachment.toAppMediaContent(): com.example.neuv3.features.social.domain.model.AppMediaContent =
    com.example.neuv3.features.social.domain.model.AppMediaContent(
        url = url,
        storagePath = storagePath,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        thumbnailUrl = thumbnailUrl,
    )

private val FeedBlack = Color(0xFF040707)
private val FeedWhite = Color(0xFFF3FFF9)
private val FeedMist = Color(0xFF99BBB0)
private val FeedGreen = Color(0xFF28F0C6)
private val FeedPlayGreen = Color(0xFF1BCF72)
private val FeedStopRed = Color(0xFFE24848)

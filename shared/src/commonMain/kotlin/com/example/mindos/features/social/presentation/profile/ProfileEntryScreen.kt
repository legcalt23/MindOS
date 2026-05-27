package com.example.mindos.features.social.presentation.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.mindos.features.social.domain.model.AppMediaContent
import com.example.mindos.features.social.domain.model.IdentityPosition
import com.example.mindos.features.social.domain.model.ProfileColorTarget
import com.example.mindos.features.social.domain.model.ProfileLayoutSlot
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.presentation.state.ProfileUiState
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileEntryScreen(
    profileState: ProfileUiState,
    entryState: ProfileEntryUiState,
    postsState: ProfilePublicationsUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenCreator: () -> Unit,
    onMarkVisited: suspend () -> Unit,
    onEnterEdit: (ProfileEditTarget?) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onSaveInlineEdit: () -> Unit,
    onEditTargetSelected: (ProfileEditTarget) -> Unit,
    onClearEditTarget: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onUserphraseChange: (String) -> Unit,
    onIdentityPositionChange: (IdentityPosition) -> Unit,
    onColorTargetSelected: (ProfileColorTarget) -> Unit,
    onColorApplied: (Long) -> Unit,
    onCustomColorChanged: (Long, ChromaCoreInteractionMode) -> Unit,
    onToggleCustomColorPanel: () -> Unit,
    onChromaConfirm: () -> Unit,
    onChromaCancel: () -> Unit,
    onModuleMoved: (ProfileModuleId, ProfileLayoutSlot) -> Unit,
    onModuleCleared: (ProfileModuleId) -> Unit,
    onTextModuleChanged: (ProfileModuleId, String) -> Unit,
    onMediaPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onMediaPickerError: (String) -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = entryState.initialPage, pageCount = { 2 })
    val panelSpace = 0.dp
    var showNowsScreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entryState.initialPage, entryState.isPreparing) {
        if (!entryState.isPreparing) {
            pagerState.scrollToPage(entryState.initialPage)
        }
    }

    LaunchedEffect(pagerState.currentPage, entryState.isOwner, entryState.hasRecordedVisit) {
        if (!entryState.isOwner && pagerState.currentPage == 1 && !entryState.hasRecordedVisit) {
            onMarkVisited()
        }
    }

    if (showNowsScreen) {
        NowsScreen(
            onBack = { showNowsScreen = false },
            modifier = modifier,
        )
        return
    }

    if (entryState.showSettings) {
        ProfileSettingsScreen(
            onBackClick = onCloseSettings,
            onEditProfileClick = onCloseSettings,
            onEditProfileCoverClick = onCloseSettings,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
        ProfileEntryAtmosphere(
            pageBlend = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
        )

        if (entryState.isPreparing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                ProfileEntryTopBar(
                    pagerState = pagerState,
                    isOwner = entryState.isOwner,
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                )

                entryState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFFA7A7),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp, start = 6.dp, end = 6.dp),
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 1f - (pageOffset * 0.24f)
                                scaleX = 1f - (pageOffset * 0.025f)
                                scaleY = 1f - (pageOffset * 0.025f)
                            },
                    ) {
                        if (page == 0) {
                            ProfileCoverScreen(
                                state = profileState,
                                heroModifier = Modifier.coverPinchToProfile {
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                },
                                contentBottomPadding = panelSpace,
                                onEnterEdit = onEnterEdit,
                                onCancelEdit = onCancelEdit,
                                onSaveInlineEdit = onSaveInlineEdit,
                                onUserphraseChange = onUserphraseChange,
                                onMediaPicked = onMediaPicked,
                                onMediaPickerError = onMediaPickerError,
                                onOpenMedia = onOpenMedia,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            ProfileScreen(
                                state = profileState,
                                postsState = postsState,
                                onBack = onBack,
                                onOpenOptions = onOpenSettings,
                                onOpenCreator = onOpenCreator,
                                onFollow = onFollow,
                                onUnfollow = onUnfollow,
                                onBlock = onBlock,
                                onUnblock = onUnblock,
                                onEnterEdit = { onEnterEdit(null) },
                                onCancelEdit = onCancelEdit,
                                onSaveEdit = onSaveEdit,
                                onSaveInlineEdit = onSaveInlineEdit,
                                onDisplayNameChange = onDisplayNameChange,
                                onUsernameChange = onUsernameChange,
                                onUserphraseChange = onUserphraseChange,
                                onIdentityPositionChange = onIdentityPositionChange,
                                onColorTargetSelected = onColorTargetSelected,
                                onColorApplied = onColorApplied,
                                onCustomColorChanged = onCustomColorChanged,
                                onToggleCustomColorPanel = onToggleCustomColorPanel,
                                onEditTargetSelected = onEditTargetSelected,
                                onClearEditTarget = onClearEditTarget,
                                onChromaConfirm = onChromaConfirm,
                                onChromaCancel = onChromaCancel,
                                onModuleMoved = onModuleMoved,
                                onModuleCleared = onModuleCleared,
                                onTextModuleChanged = onTextModuleChanged,
                                onMediaPicked = onMediaPicked,
                                onMediaPickerError = onMediaPickerError,
                                onOpenMedia = onOpenMedia,
                                onOpenNows = { showNowsScreen = true },
                                showHeader = false,
                                showEditorPanel = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .profilePinchToCover { scope.launch { pagerState.animateScrollToPage(0) } }
                                    .padding(bottom = panelSpace),
                            )
                        }
                    }
                }
            }
        }

        if (profileState.editor.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC0000000))
                    .clickable(
                        indication = null,
                        interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Guardando perfil...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileEntryTopBar(
    pagerState: PagerState,
    isOwner: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        PremiumProfilePagerIndicator(
            currentProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f),
            isOwner = isOwner,
            onPortadaClick = { scope.launch { pagerState.animateScrollToPage(0) } },
            onPerfilClick = { scope.launch { pagerState.animateScrollToPage(1) } },
            modifier = Modifier
                .align(Alignment.Center)
                .width(280.dp)
                .height(56.dp),
        )

        IconButton(
            onClick = onOpenSettings,
            enabled = isOwner,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Configuración del perfil",
                tint = if (isOwner) Color.White else Color.White.copy(alpha = 0.24f),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun PremiumProfilePagerIndicator(
    currentProgress: Float,
    isOwner: Boolean,
    onPortadaClick: () -> Unit,
    onPerfilClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(durationMillis = 220),
        label = "ProfilePagerProgress",
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF101214))
            .border(1.dp, Color(0xFF262A2E), RoundedCornerShape(30.dp)),
    ) {
        val pillWidth = (maxWidth - 8.dp) / 2f
        Box(
            modifier = Modifier
                .padding(4.dp)
                .offset { IntOffset((with(this) { (pillWidth * animatedProgress).roundToPx() }), 0) }
                .width(pillWidth)
                .height(48.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFFE1FFF8)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            IndicatorLabel(
                text = "Portada",
                selected = animatedProgress < 0.5f,
                enabled = true,
                onClick = onPortadaClick,
                modifier = Modifier.weight(1f),
            )
            IndicatorLabel(
                text = "Perfil",
                selected = animatedProgress >= 0.5f,
                enabled = true,
                onClick = onPerfilClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IndicatorLabel(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF050505) else Color(0xFFEAEAEA),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ProfileEntryAtmosphere(
    pageBlend: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF05070B))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF13ECA4).copy(alpha = 0.14f - pageBlend * 0.04f), Color.Transparent),
                center = Offset(size.width * (0.18f + pageBlend * 0.36f), size.height * 0.18f),
                radius = size.minDimension * 0.78f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * (0.12f + pageBlend * 0.18f)),
                radius = size.minDimension * 0.54f,
            ),
        )
    }
}

private fun Modifier.coverPinchToProfile(onPinchOut: () -> Unit): Modifier =
    pointerInput(Unit) {
        var cumulativeZoom = 1f
        detectTransformGestures { _, _, zoom, _ ->
            cumulativeZoom *= zoom
            if (cumulativeZoom > 1.12f) {
                cumulativeZoom = 1f
                onPinchOut()
            }
        }
    }

private fun Modifier.profilePinchToCover(onPinchIn: () -> Unit): Modifier =
    pointerInput(Unit) {
        var cumulativeZoom = 1f
        detectTransformGestures { _, _, zoom, _ ->
            cumulativeZoom *= zoom
            if (cumulativeZoom < 0.9f) {
                cumulativeZoom = 1f
                onPinchIn()
            }
        }
    }

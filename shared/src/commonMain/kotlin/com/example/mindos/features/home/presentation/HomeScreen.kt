package com.example.mindos.features.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.session.AuthenticatedUser
import com.example.mindos.core.logging.safeDebugLog
import com.example.mindos.features.search.data.FirebaseUsernameSearchRepository
import com.example.mindos.features.search.domain.UsernameSearchUser
import com.example.mindos.features.search.presentation.UsernameSearchScreen
import com.example.mindos.features.search.presentation.UsernameSearchViewModel
import com.example.mindos.features.social.data.local.InMemorySocialGraphComponent
import com.example.mindos.features.social.domain.feed.GlobalFeedRepository
import com.example.mindos.features.social.data.repository.FirebaseProfileVisitRepository
import com.example.mindos.features.social.data.repository.FirebaseProfileVisualRepository
import com.example.mindos.features.social.data.repository.FirebaseSocialInteractionRepository
import com.example.mindos.features.social.domain.model.InAppNotification
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.domain.valueobject.Bio
import com.example.mindos.features.social.domain.valueobject.DisplayName
import com.example.mindos.features.social.domain.valueobject.Handle
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.presentation.notifications.NotificationsScreen
import com.example.mindos.features.social.presentation.notifications.rememberNotificationsStateHolder
import com.example.mindos.features.social.presentation.feed.GlobalFeedComposerScreen
import com.example.mindos.features.social.presentation.profile.ProfileEditTarget
import com.example.mindos.features.social.presentation.profile.ProfileEntryScreen
import com.example.mindos.features.social.presentation.profile.rememberProfileEntryStateHolder
import com.example.mindos.features.social.presentation.profile.rememberProfilePublicationsStateHolder
import com.example.mindos.features.social.presentation.profile.rememberProfileStateHolder
import com.example.mindos.features.social.shared.util.getOrNull
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    user: AuthenticatedUser,
    globalFeedRepository: GlobalFeedRepository,
    onOpenFeed: () -> Unit,
    onSignOut: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    initialProfileId: String? = null,
    initialProfileHandle: String? = null,
    initialProfileName: String? = null,
    onOpenMedia: (com.example.mindos.features.social.domain.model.AppMediaContent) -> Unit,
    onOpenDMList: () -> Unit = {},
    onOpenMoneda: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val socialComponent = remember { InMemorySocialGraphComponent.create() }
    val visualRepository = remember { FirebaseProfileVisualRepository() }
    val visitRepository = remember { FirebaseProfileVisitRepository() }
    val searchRepository = remember { FirebaseUsernameSearchRepository() }
    val socialInteractionRepository = remember { FirebaseSocialInteractionRepository() }
    val scope = rememberCoroutineScope()
    val ownerId = remember(user.uid) { UserId(user.uid) }
    val viewerId = ownerId
    val handle = remember(user.uid, user.email, user.displayName) { user.safeHandle() }
    val displayName = remember(user.uid, user.email, user.displayName) { user.safeDisplayName() }
    val hasInitialProfile = initialProfileId?.isNotBlank() == true
    var destination by remember(user.uid, initialProfileId) {
        mutableStateOf(if (hasInitialProfile) HomeDestination.Profile else HomeDestination.Hub)
    }
    var creatorReturnDestination by remember(user.uid, initialProfileId) {
        mutableStateOf(if (hasInitialProfile) HomeDestination.Profile else HomeDestination.Hub)
    }
    var activeProfileOwnerId by remember(user.uid, initialProfileId) {
        mutableStateOf(initialProfileId?.trim()?.takeIf { it.isNotBlank() } ?: ownerId.value)
    }

    val searchViewModel = remember(searchRepository, scope) {
        UsernameSearchViewModel(
            repository = searchRepository,
            scope = scope,
        )
    }
    val profileStateHolder = rememberProfileStateHolder(
        getProfile = socialComponent.useCases.getProfile,
        followUser = socialComponent.useCases.followUser,
        unfollowUser = socialComponent.useCases.unfollowUser,
        blockUser = socialComponent.useCases.blockUser,
        unblockUser = socialComponent.useCases.unblockUser,
        profileVisualRepository = visualRepository,
        observationRepository = socialInteractionRepository,
        clock = socialComponent.dependencies.clock,
    )
    val profileEntryStateHolder = rememberProfileEntryStateHolder(
        visitRepository = visitRepository,
        clock = socialComponent.dependencies.clock,
    )
    val profilePublicationsStateHolder = rememberProfilePublicationsStateHolder(
        repository = globalFeedRepository,
        scope = scope,
    )
    val notificationsStateHolder = rememberNotificationsStateHolder(
        repository = socialInteractionRepository,
    )

    LaunchedEffect(visualRepository, ownerId, handle, displayName) {
        runCatching {
            val existing = visualRepository.getConfig(ownerId)
            if (existing == null) {
                val now = socialComponent.dependencies.clock.now()
                val defaultDiscoveryProfile = ProfileVisualConfig.createDefault(
                    userId = ownerId,
                    displayName = displayName.value,
                    username = handle.value,
                    observersCount = 0,
                    now = now,
                )
                safeDebugLog("HomeScreen", "event=bootstrap_profile_create")
                visualRepository.saveConfig(
                    defaultDiscoveryProfile,
                    attemptId = "dm_discovery_bootstrap",
                )
            } else {
                val now = socialComponent.dependencies.clock.now()
                val normalizedProfile = existing.copy(
                    displayName = if (existing.displayName.isGenericProfileDisplayName()) {
                        displayName.value
                    } else {
                        existing.displayName
                    },
                    username = if (existing.username.isGenericProfileUsername()) {
                        handle.value
                    } else {
                        existing.username
                    },
                    searchable = existing.searchable,
                    updatedAt = now,
                ).normalized()
                safeDebugLog(
                    "HomeScreen",
                    "event=bootstrap_profile_normalize searchable=${normalizedProfile.searchable}",
                )
                visualRepository.saveConfig(
                    normalizedProfile,
                    attemptId = "dm_discovery_bootstrap",
                )
            }
        }.onFailure { error ->
            safeDebugLog(
                "HomeScreen",
                "event=bootstrap_profile_failed exception=${error::class.simpleName.orEmpty()}",
            )
        }
    }

    suspend fun loadProfile(
        targetId: UserId,
        targetHandle: Handle,
        targetDisplayName: DisplayName,
        bio: Bio = Bio.Empty,
    ) {
        activeProfileOwnerId = targetId.value
        socialComponent.seedUserIfMissing(
            userId = targetId,
            handle = targetHandle,
            displayName = targetDisplayName,
            bio = bio,
        )
        profileStateHolder.load(viewerId = viewerId, ownerId = targetId)
        profileEntryStateHolder.load(viewerId = viewerId, ownerId = targetId)
        profilePublicationsStateHolder.load(targetId.value)
    }

    fun openProfile(
        targetId: UserId,
        targetHandle: Handle,
        targetDisplayName: DisplayName,
        bio: Bio = Bio.Empty,
    ) {
        destination = HomeDestination.Profile
        scope.launch {
            loadProfile(
                targetId = targetId,
                targetHandle = targetHandle,
                targetDisplayName = targetDisplayName,
                bio = bio,
            )
        }
    }

    LaunchedEffect(
        profileStateHolder,
        viewerId,
        ownerId,
        socialComponent,
        handle,
        displayName,
        initialProfileId,
        initialProfileHandle,
        initialProfileName,
    ) {
        val requestedProfileId = initialProfileId?.trim()?.takeIf { it.isNotBlank() }
        if (requestedProfileId != null) {
            val targetId = UserId(requestedProfileId)
            loadProfile(
                targetId = targetId,
                targetHandle = safeHandleFrom(
                    primary = initialProfileHandle?.takeIf { it.isNotBlank() } ?: requestedProfileId,
                    fallback = requestedProfileId,
                ),
                targetDisplayName = initialProfileName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.take(DisplayName.MaxLength)
                    ?.let { DisplayName.from(it).getOrNull() }
                    ?: DisplayName(requestedProfileId.take(DisplayName.MaxLength)),
            )
        } else {
            loadProfile(
                targetId = ownerId,
                targetHandle = handle,
                targetDisplayName = displayName,
                bio = Bio("Perfil visual MindOS."),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (destination) {
            HomeDestination.Hub -> NeuHomeHub(
                user = user,
                onOpenCreator = {
                    creatorReturnDestination = HomeDestination.Hub
                    destination = HomeDestination.Creator
                },
                onOpenFeed = onOpenFeed,
                onOpenSearch = { destination = HomeDestination.Search },
                onOpenDMList = onOpenDMList,
                onOpenMoneda = onOpenMoneda,
                onOpenProfile = {
                    openProfile(
                        targetId = ownerId,
                        targetHandle = handle,
                        targetDisplayName = displayName,
                        bio = Bio("Perfil visual MindOS."),
                    )
                },
                onOpenNotifications = {
                    destination = HomeDestination.Notifications
                    scope.launch { notificationsStateHolder.load(ownerId) }
                },
                onSignOut = onSignOut,
            )

            HomeDestination.Search -> UsernameSearchScreen(
                viewModel = searchViewModel,
                currentUserId = ownerId.value,
                onBack = { destination = HomeDestination.Hub },
                onUserSelected = { selected ->
                    openProfile(
                        targetId = UserId(selected.userId),
                        targetHandle = selected.safeHandle(),
                        targetDisplayName = selected.safeDisplayName(),
                    )
                },
            )

            HomeDestination.Profile -> Box(modifier = Modifier.fillMaxSize()) {
                ProfileEntryScreen(
                    profileState = profileStateHolder.uiState,
                    entryState = profileEntryStateHolder.uiState,
                    postsState = profilePublicationsStateHolder.uiState,
                    onBack = { destination = HomeDestination.Hub },
                    onOpenSettings = profileEntryStateHolder::openSettings,
                    onCloseSettings = profileEntryStateHolder::closeSettings,
                    onOpenCreator = {
                        creatorReturnDestination = HomeDestination.Profile
                        destination = HomeDestination.Creator
                    },
                    onMarkVisited = { profileEntryStateHolder.markVisitedIfNeeded() },
                    onEnterEdit = { target -> profileStateHolder.enterEditMode(target) },
                    onCancelEdit = profileStateHolder::cancelEdit,
                    onSaveEdit = { scope.launch { profileStateHolder.saveEdit() } },
                    onSaveInlineEdit = { scope.launch { profileStateHolder.saveEdit(keepEditing = true) } },
                    onEditTargetSelected = profileStateHolder::focusEditTarget,
                    onClearEditTarget = profileStateHolder::clearEditTarget,
                    onFollow = { scope.launch { profileStateHolder.follow() } },
                    onUnfollow = { scope.launch { profileStateHolder.unfollow() } },
                    onBlock = { scope.launch { profileStateHolder.block() } },
                    onUnblock = { scope.launch { profileStateHolder.unblock() } },
                    onDisplayNameChange = profileStateHolder::updateDisplayName,
                    onUsernameChange = profileStateHolder::updateUsername,
                    onUserphraseChange = profileStateHolder::updateUserphrase,
                    onIdentityPositionChange = profileStateHolder::updateIdentityPosition,
                    onColorTargetSelected = profileStateHolder::selectColorTarget,
                    onColorApplied = profileStateHolder::applyColor,
                    onCustomColorChanged = profileStateHolder::updateCustomColor,
                    onToggleCustomColorPanel = profileStateHolder::toggleCustomColorPanel,
                    onChromaConfirm = profileStateHolder::confirmChromaSession,
                    onChromaCancel = profileStateHolder::cancelChromaSession,
                    onModuleMoved = profileStateHolder::moveModule,
                    onModuleCleared = profileStateHolder::clearModule,
                    onTextModuleChanged = profileStateHolder::updateTextModule,
                    onMediaPicked = profileStateHolder::setModuleMedia,
                    onMediaPickerError = profileStateHolder::reportMediaPickerError,
                    onOpenMedia = onOpenMedia,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            HomeDestination.Notifications -> NotificationsScreen(
                state = notificationsStateHolder.uiState,
                onBack = { destination = HomeDestination.Hub },
                onOpenActorProfile = { notification ->
                    openProfile(
                        targetId = notification.actorUserId,
                        targetHandle = notification.safeActorHandle(),
                        targetDisplayName = notification.safeActorDisplayName(),
                    )
                },
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
            )

            HomeDestination.Creator -> GlobalFeedComposerScreen(
                user = user,
                onBack = { destination = creatorReturnDestination },
                onPublished = { post ->
                    profilePublicationsStateHolder.onPostPublished(post)
                    if (activeProfileOwnerId == post.userId) {
                        scope.launch { profilePublicationsStateHolder.load(post.userId) }
                    }
                    destination = creatorReturnDestination
                },
                repository = globalFeedRepository,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun NeuHomeHub(
    user: AuthenticatedUser,
    onOpenCreator: () -> Unit,
    onOpenFeed: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenDMList: () -> Unit,
    onOpenMoneda: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBlack),
    ) {
        HomeAtmosphere(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 42.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "MindOS",
                        color = HomeWhite,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 29.sp),
                    )
                    HomeOverlayButton(text = "CUENTA", onClick = onSignOut)
                }

                Spacer(modifier = Modifier.height(46.dp))

                Text(
                    text = "MindOS Home",
                    color = HomeWhite,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 35.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.email?.takeIf { it.isNotBlank() }
                        ?: user.uid.take(18),
                    color = HomeGreen.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(modifier = Modifier.height(42.dp))

                HomeAccessTile(
                    title = "CREADOR PUBLICADOR",
                    subtitle = "Se admiten desnudos!",
                    icon = HomeAccessIcon.Creator,
                    onClick = onOpenCreator,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomeAccessTile(
                    title = "FEED",
                    subtitle = "Entrar al feed global",
                    icon = HomeAccessIcon.Feed,
                    onClick = onOpenFeed,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomeAccessTile(
                    title = "BUSCADOR",
                    subtitle = "Buscar usuarios por username",
                    icon = HomeAccessIcon.Search,
                    onClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomeAccessTile(
                    title = "PERFIL",
                    subtitle = "Entrar en tu perfil",
                    icon = HomeAccessIcon.Profile,
                    onClick = onOpenProfile,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomeAccessTile(
                    title = "NOTIFICACIONES",
                    subtitle = "Ver quien te observa",
                    icon = HomeAccessIcon.Notifications,
                    onClick = onOpenNotifications,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomeAccessTile(
                    title = "DM",
                    subtitle = "Hablar por privado",
                    icon = HomeAccessIcon.Profile, // Reutilizamos icono por ahora
                    onClick = onOpenDMList,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomeAccessTile(
                    title = "MONEDA",
                    subtitle = "Tu billetera virtual MindOS",
                    icon = HomeAccessIcon.Moneda,
                    onClick = onOpenMoneda,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(42.dp))
                Text(
                    text = "Publicando pornografía desde 1978.",
                    color = HomeMist,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HomeAccessTile(
    title: String,
    subtitle: String,
    icon: HomeAccessIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.070f),
                        HomeGreen.copy(alpha = 0.050f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, HomeGreen.copy(alpha = 0.28f)), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeIcon(icon = icon, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HomeWhite,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = HomeMist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HomeIcon(
    icon: HomeAccessIcon,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(HomeGreen.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, HomeGreen.copy(alpha = 0.40f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            when (icon) {
                HomeAccessIcon.Creator -> {
                    drawCircle(
                        color = HomeGreen,
                        radius = size.minDimension * 0.28f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 3f),
                    )
                    drawLine(
                        color = HomeGreen,
                        start = Offset(size.width * 0.50f, size.height * 0.24f),
                        end = Offset(size.width * 0.50f, size.height * 0.76f),
                        strokeWidth = 3f,
                    )
                    drawLine(
                        color = HomeGreen,
                        start = Offset(size.width * 0.24f, size.height * 0.50f),
                        end = Offset(size.width * 0.76f, size.height * 0.50f),
                        strokeWidth = 3f,
                    )
                }

                HomeAccessIcon.Feed -> {
                    repeat(3) { index ->
                        val y = size.height * (0.28f + index * 0.22f)
                        drawLine(
                            color = HomeGreen,
                            start = Offset(size.width * 0.18f, y),
                            end = Offset(size.width * 0.82f, y),
                            strokeWidth = 3f,
                        )
                        drawCircle(
                            color = HomeGreen.copy(alpha = 0.26f),
                            radius = size.minDimension * 0.08f,
                            center = Offset(size.width * (0.20f + index * 0.18f), y),
                            style = Stroke(width = 1.1f),
                        )
                    }
                }

                HomeAccessIcon.Search -> {
                    drawCircle(
                        color = HomeGreen,
                        radius = size.minDimension * 0.30f,
                        center = Offset(size.width * 0.42f, size.height * 0.42f),
                        style = Stroke(width = 3.1f),
                    )
                    drawLine(
                        color = HomeGreen,
                        start = Offset(size.width * 0.64f, size.height * 0.64f),
                        end = Offset(size.width * 0.88f, size.height * 0.88f),
                        strokeWidth = 3.1f,
                    )
                    drawCircle(
                        color = HomeGreen.copy(alpha = 0.23f),
                        radius = size.minDimension * 0.48f,
                        center = Offset(size.width * 0.42f, size.height * 0.42f),
                        style = Stroke(width = 1.1f),
                    )
                }

                HomeAccessIcon.Profile -> {
                    drawCircle(
                        color = HomeGreen,
                        radius = size.minDimension * 0.17f,
                        center = Offset(size.width / 2f, size.height * 0.32f),
                        style = Stroke(width = 3f),
                    )
                    val body = Path().apply {
                        moveTo(size.width * 0.20f, size.height * 0.86f)
                        cubicTo(
                            size.width * 0.24f,
                            size.height * 0.56f,
                            size.width * 0.76f,
                            size.height * 0.56f,
                            size.width * 0.80f,
                            size.height * 0.86f,
                        )
                    }
                    drawPath(path = body, color = HomeGreen, style = Stroke(width = 3f))
                }

                HomeAccessIcon.Notifications -> {
                    drawCircle(
                        color = HomeGreen.copy(alpha = 0.20f),
                        radius = size.minDimension * 0.46f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 1.1f),
                    )
                    drawArc(
                        color = HomeGreen,
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.22f, size.height * 0.18f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
                        style = Stroke(width = 3f),
                    )
                    drawLine(
                        color = HomeGreen,
                        start = Offset(size.width * 0.26f, size.height * 0.62f),
                        end = Offset(size.width * 0.74f, size.height * 0.62f),
                        strokeWidth = 3f,
                    )
                    drawCircle(
                        color = HomeGreen,
                        radius = size.minDimension * 0.07f,
                        center = Offset(size.width / 2f, size.height * 0.76f),
                    )
                }

                HomeAccessIcon.Moneda -> {
                    // Coin outline
                    drawCircle(
                        color = HomeGreen,
                        radius = size.minDimension * 0.40f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 3f),
                    )
                    // Inner ghost ring
                    drawCircle(
                        color = HomeGreen.copy(alpha = 0.22f),
                        radius = size.minDimension * 0.29f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 1.1f),
                    )
                    // Stylised "N" — left vertical, diagonal, right vertical
                    val nLeft   = size.width  * 0.37f
                    val nRight  = size.width  * 0.63f
                    val nTop    = size.height * 0.30f
                    val nBottom = size.height * 0.70f
                    drawLine(
                        color = HomeGreen,
                        start = Offset(nLeft, nBottom),
                        end   = Offset(nLeft, nTop),
                        strokeWidth = 2.8f,
                    )
                    drawLine(
                        color = HomeGreen,
                        start = Offset(nLeft, nTop),
                        end   = Offset(nRight, nBottom),
                        strokeWidth = 2.8f,
                    )
                    drawLine(
                        color = HomeGreen,
                        start = Offset(nRight, nBottom),
                        end   = Offset(nRight, nTop),
                        strokeWidth = 2.8f,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeOverlayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = HomeWhite,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun HomeAtmosphere(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(HomeBlack)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HomeGreen.copy(alpha = 0.17f), Color.Transparent),
                center = Offset(size.width * 0.16f, size.height * 0.18f),
                radius = size.minDimension * 0.72f,
            ),
        )
        repeat(7) { index ->
            val y = size.height * (0.23f + index * 0.085f)
            drawLine(
                color = HomeGreen.copy(alpha = 0.045f),
                start = Offset(size.width * 0.08f, y),
                end = Offset(size.width * 0.92f, y - size.height * 0.04f),
                strokeWidth = 1f,
            )
        }
    }
}

private enum class HomeDestination {
    Hub,
    Search,
    Profile,
    Notifications,
    Creator,
}

private enum class HomeAccessIcon {
    Creator,
    Feed,
    Search,
    Profile,
    Notifications,
    Moneda,
}

private fun AuthenticatedUser.safeDisplayName(): DisplayName {
    val candidate = displayName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: email?.substringBefore("@")?.trim()?.takeIf { it.isNotBlank() }
        ?: uid.take(DisplayName.MaxLength)
    return DisplayName.from(candidate.take(DisplayName.MaxLength)).getOrNull()
        ?: DisplayName(uid.take(DisplayName.MaxLength))
}

private fun AuthenticatedUser.safeHandle(): Handle {
    val candidates = listOfNotNull(
        email?.substringBefore("@"),
        displayName,
        uid.take(18),
    )

    candidates.forEach { candidate ->
        val normalized = candidate
            .lowercase()
            .map { char -> if (char.isLetterOrDigit() || char == '.' || char == '_') char else '_' }
            .joinToString("")
            .trim('.')
            .replace(Regex("\\.{2,}"), ".")
            .take(30)
        Handle.from(normalized).getOrNull()?.let { return it }
    }

    val uidAlias = uid
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .ifBlank { "neu" }
        .take(18)
    return Handle.from("user_$uidAlias".take(30)).getOrNull()
        ?: Handle("user_neu")
}

private fun UsernameSearchUser.safeHandle(): Handle =
    safeHandleFrom(username, userId)

private fun UsernameSearchUser.safeDisplayName(): DisplayName {
    val candidate = displayName.trim().takeIf { it.isNotBlank() }
        ?: username.trim().takeIf { it.isNotBlank() }
        ?: userId.take(18)
    return DisplayName.from(candidate.take(DisplayName.MaxLength)).getOrNull()
        ?: DisplayName(userId.take(DisplayName.MaxLength))
}

private fun InAppNotification.safeActorHandle(): Handle =
    safeHandleFrom(actorUsername, actorUserId.value)

private fun InAppNotification.safeActorDisplayName(): DisplayName {
    val candidate = actorDisplayName.trim().takeIf { it.isNotBlank() }
        ?: actorUsername.trim().takeIf { it.isNotBlank() }
        ?: actorUserId.value.take(18)
    return DisplayName.from(candidate.take(DisplayName.MaxLength)).getOrNull()
        ?: DisplayName(actorUserId.value.take(DisplayName.MaxLength))
}

private fun String.isGenericProfileDisplayName(): Boolean =
    trim().equals("Perfil MindOS", ignoreCase = true)

private fun String.isGenericProfileUsername(): Boolean {
    val normalized = trim().removePrefix("@").lowercase()
    return normalized.isBlank() || normalized == "usuario"
}

private fun safeHandleFrom(primary: String, fallback: String): Handle {
    val candidates = listOf(primary, fallback)
    candidates.forEach { candidate ->
        val normalized = candidate
            .lowercase()
            .removePrefix("@")
            .map { char -> if (char.isLetterOrDigit() || char == '.' || char == '_') char else '_' }
            .joinToString("")
            .trim('.')
            .replace(Regex("\\.{2,}"), ".")
            .take(30)
        Handle.from(normalized).getOrNull()?.let { return it }
    }
    val fallbackAlias = fallback
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .ifBlank { "neu" }
        .take(18)
    return Handle.from("user_$fallbackAlias".take(30)).getOrNull()
        ?: Handle("user_neu")
}

private val HomeBlack = Color(0xFF020504)
private val HomeWhite = Color(0xFFF4FFF9)
private val HomeGreen = Color(0xFF13ECA4)
private val HomeMist = Color(0xFFA7CFC1)

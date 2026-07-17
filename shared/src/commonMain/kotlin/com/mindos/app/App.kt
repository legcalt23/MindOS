package com.mindos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.core.di.AppGraph
import com.mindos.app.core.di.rememberAppGraph
import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.navigation.AppRoute
import com.mindos.app.core.navigation.AppRouteStack
import com.mindos.app.core.navigation.LaunchDestinationResolver
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.core.navigation.telemetryName
import com.mindos.app.core.session.PreviewSessionRepository
import com.mindos.app.core.session.SessionRepository
import com.mindos.app.design.AppColors
import com.mindos.app.design.LocalFavoriteAccent
import com.mindos.app.design.MindOSTheme
import com.mindos.app.features.auth.ui.AuthScreen
import com.mindos.app.features.element_selection.ui.ElementSelectionScreen
import com.mindos.app.features.home.presentation.HomeScreen
import com.mindos.app.features.mindboost.focus.data.local.SecureStoreValueCache
import com.mindos.app.features.privacy.application.AccountDeletionCoordinator
import com.mindos.app.features.privacy.application.AccountDeletionRecovery
import com.mindos.app.features.social.presentation.feed.GlobalFeedScreen
import com.mindos.app.features.wallet.presentation.MoneyScreen
import com.mindos.app.features.wallet.presentation.rememberWalletStateHolder
import kotlinx.coroutines.launch

@Composable
fun App() {
    MindOSApp(
        sessionRepository = remember { PreviewSessionRepository(isAuthenticated = false) },
    )
}

@Composable
fun MindOSApp(
    sessionRepository: SessionRepository,
    // All long-lived collaborators come bundled in an AppGraph. The default
    // factory wires production dependencies (Firebase + in-memory waves); tests
    // and previews construct their own AppGraph with fakes. See core/di/AppGraph.
    appGraph: AppGraph = rememberAppGraph(),
) {
    val destinationResolver = remember(sessionRepository) {
        LaunchDestinationResolver(sessionRepository)
    }
    val stack = remember(destinationResolver) {
        AppRouteStack(destinationResolver.resolveInitialRoute())
    }
    val scope = rememberCoroutineScope()
    val accountDeletionCoordinator = remember(
        sessionRepository,
        appGraph.userDataEraser,
        appGraph.pendingAccountDeletionStore,
    ) {
        AccountDeletionCoordinator(
            sessionRepository = sessionRepository,
            userDataEraser = appGraph.userDataEraser,
            pendingStore = appGraph.pendingAccountDeletionStore,
        )
    }
    var deletionRecoveryComplete by remember(accountDeletionCoordinator) { mutableStateOf(false) }

    LaunchedEffect(accountDeletionCoordinator) {
        deletionRecoveryComplete =
            accountDeletionCoordinator.recoverPendingDeletion() != AccountDeletionRecovery.LocalEraseBlocked
    }

    if (!deletionRecoveryComplete) {
        MindOSTheme { LaunchLoadingScreen() }
        return
    }

    // Current user's favorite accent (packed ARGB) for LocalFavoriteAccent. Loaded
    // from the profile card on sign-in; null (brand/theme fallback) otherwise.
    var favoriteAccentArgb by remember { mutableStateOf<Long?>(null) }

    // React to auth state: clear stack on sign-out, rebind every entry on user refresh.
    LaunchedEffect(sessionRepository) {
        sessionRepository.authState.collect { user ->
            if (user == null) {
                // Drop every cached decrypted value so no plaintext survives a user switch (S5).
                SecureStoreValueCache.evictAll()
                stack.replaceAll(AppRoute.Auth)
                favoriteAccentArgb = null
                ObservabilityHub.customKey("authenticated", false)
            } else {
                stack.rebindStackWithUser(user)
                favoriteAccentArgb = appGraph.profileCardRepository.load(user.uid)?.appearance?.accentArgb
                ObservabilityHub.customKey("authenticated", true)
                ObservabilityHub.breadcrumb("auth_state_authenticated")
            }
        }
    }

    // Emit a breadcrumb + update the current_route custom key on every nav change.
    // Tagging crashes with the route helps cluster reports by feature and is the
    // single most useful piece of context for triage.
    LaunchedEffect(stack.current) {
        val name = stack.current.telemetryName()
        ObservabilityHub.customKey("current_route", name)
        ObservabilityHub.customKey("nav_depth", stack.size)
        ObservabilityHub.breadcrumb("nav_to:$name")
    }

    // Wave service comes from the graph (single shared instance). The DM state
    // holder stays composition-scoped — it owns a UI-lifecycle CoroutineScope —
    // but is fed entirely from the graph, so App.kt no longer constructs collaborators.
    val waveRequestService = appGraph.waveRequestService
    val dmStateHolder = com.mindos.app.features.dm.presentation.rememberDMStateHolder(
        repository = appGraph.dmRepository,
        waveRequestService = waveRequestService,
    )

    // Publish the current user's favorite accent to the whole authenticated tree,
    // so every migrated loader (AppBarsLoader) tints with it via LocalFavoriteAccent.
    CompositionLocalProvider(LocalFavoriteAccent provides favoriteAccentArgb) {
    MindOSTheme {
        com.mindos.app.features.privacy.PrivacyScreenOverlay(
            // Gated by BOTH the user's privacy toggle AND the remote kill switch:
            // ops can disable the continuous-camera overlay via Remote Config
            // (privacy_overlay_enabled) without an app update if it misbehaves.
            enabled = appGraph.privacyEnabledState.value &&
                com.mindos.app.core.config.FeatureFlagsHub.current.privacyOverlayEnabled,
        ) {
            // Hardware/gesture back is wired to stack.pop() when there is somewhere to go.
            PlatformBackHandler(enabled = stack.canGoBack, onBack = { stack.pop() })

            com.mindos.app.core.ui.gesture.FireBackGestureHandler(
                enabled = stack.canGoBack,
                onBack = { stack.pop() },
            ) {
                when (val currentRoute = stack.current) {
                    AppRoute.Loading -> LaunchLoadingScreen()

                    AppRoute.Auth -> AuthScreen(
                        sessionRepository = sessionRepository,
                        onAuthenticated = {
                            scope.launch {
                                accountDeletionCoordinator.recoverPendingDeletion()
                                stack.replaceAll(destinationResolver.resolvePostLoginRoute())
                            }
                        },
                    )

                    AppRoute.ElementSelection -> ElementSelectionScreen(
                        onElementSelected = {
                            stack.replaceAll(destinationResolver.resolvePostLoginRoute())
                        },
                    )

                    is AppRoute.Main -> HomeScreen(
                        user = currentRoute.user,
                        globalFeedRepository = appGraph.globalFeedRepository,
                        mindOSProfileRepository = appGraph.mindOSProfileRepository,
                        mindOSProfileObserverRepository = appGraph.mindOSProfileObserverRepository,
                        mindOSProfilePresenceRepository = appGraph.mindOSProfilePresenceRepository,
                        appClock = appGraph.appClock,
                        onOpenFeed = { stack.push(AppRoute.GlobalFeed(currentRoute.user)) },
                        onSignOut = {
                            scope.launch {
                                sessionRepository.signOut()
                                stack.replaceAll(AppRoute.Auth)
                            }
                        },
                        onOpenSettings = { stack.push(AppRoute.Settings(currentRoute.user)) },
                        initialProfileId = currentRoute.initialProfileId,
                        initialProfileHandle = currentRoute.initialProfileHandle,
                        initialProfileName = currentRoute.initialProfileName,
                        onOpenMedia = { content ->
                            stack.push(AppRoute.MediaViewer(currentRoute.user, content))
                        },
                        onOpenDMList = { stack.push(AppRoute.DMList(currentRoute.user)) },
                        onOpenMoneda = { stack.push(AppRoute.Moneda(currentRoute.user)) },
                    )

                    is AppRoute.GlobalFeed -> GlobalFeedScreen(
                        user = currentRoute.user,
                        repository = appGraph.globalFeedRepository,
                        onBack = { stack.pop() },
                        onOpenProfile = { userId, username, displayName ->
                            stack.replace(
                                AppRoute.Main(
                                    user = currentRoute.user,
                                    initialProfileId = userId,
                                    initialProfileHandle = username,
                                    initialProfileName = displayName,
                                ),
                            )
                        },
                        onOpenMedia = { content ->
                            stack.push(AppRoute.MediaViewer(currentRoute.user, content))
                        },
                    )

                    is AppRoute.Settings -> com.mindos.app.features.privacy.SettingsScreen(
                        privacyState = appGraph.privacyEnabledState,
                        accountDeletionCoordinator = accountDeletionCoordinator,
                        onBack = { stack.pop() },
                    )

                    is AppRoute.MediaViewer -> com.mindos.app.features.social.presentation.media.AppMediaViewer(
                        content = currentRoute.content,
                        onDismiss = { stack.pop() },
                    )

                    is AppRoute.DMList -> com.mindos.app.features.dm.presentation.DMListScreen(
                        user = currentRoute.user,
                        dmRepository = appGraph.dmRepository,
                        waveRequestService = waveRequestService,
                        onBack = { stack.pop() },
                        onOpenChat = { chatId, otherUserId ->
                            stack.push(AppRoute.DMChat(currentRoute.user, chatId, otherUserId))
                        },
                        onOpenCajonera = { stack.push(AppRoute.DMCajonera(currentRoute.user)) },
                    )

                    is AppRoute.DMChat -> com.mindos.app.features.dm.presentation.DMChatScreen(
                        user = currentRoute.user,
                        chatId = currentRoute.chatId,
                        otherUserId = currentRoute.otherUserId,
                        stateHolder = dmStateHolder,
                        onBack = { stack.pop() },
                        onOpenUserInfo = {
                            stack.push(
                                AppRoute.DMChatUserInfo(
                                    user = currentRoute.user,
                                    chatId = currentRoute.chatId,
                                    otherUserId = currentRoute.otherUserId,
                                    // No real presence system exists yet, so we must not
                                    // fabricate an "online" status (it misled users into
                                    // thinking presence was tracked). Default to not-online
                                    // until a real presence backend lands (see deliverable).
                                    isOnline = false,
                                ),
                            )
                        },
                        onOpenMedia = { content ->
                            stack.push(AppRoute.MediaViewer(currentRoute.user, content))
                        },
                    )

                    is AppRoute.DMChatUserInfo -> com.mindos.app.features.dm.presentation.DMChatUserInfoRoute(
                        user = currentRoute.user,
                        chatId = currentRoute.chatId,
                        otherUserId = currentRoute.otherUserId,
                        stateHolder = dmStateHolder,
                        isOnline = currentRoute.isOnline,
                        onBack = { stack.pop() },
                        onOpenCajonera = {
                            stack.push(
                                AppRoute.DMCajonera(
                                    user = currentRoute.user,
                                    chatId = currentRoute.chatId,
                                    otherUserId = currentRoute.otherUserId,
                                    isOnline = currentRoute.isOnline,
                                ),
                            )
                        },
                        onOpenMedia = { content ->
                            stack.push(AppRoute.MediaViewer(currentRoute.user, content))
                        },
                    )

                    is AppRoute.DMCajonera -> com.mindos.app.features.dm.presentation.Cajonera(
                        currentUserId = currentRoute.user.uid,
                        waveRequestService = waveRequestService,
                        onBack = { stack.pop() },
                    )

                    is AppRoute.Moneda -> {
                        if (FeatureFlagsHub.current.walletEnabled) {
                            MoneyScreen(
                                stateHolder = rememberWalletStateHolder(appGraph.walletClient),
                                onBack = { stack.pop() },
                            )
                        } else {
                            walletDisabledScreen(onBack = { stack.pop() })
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun walletDisabledScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Moneda no disponible",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}

@Composable
private fun LaunchLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "MindOS",
            color = AppColors.NeuGold,
        )
    }
}

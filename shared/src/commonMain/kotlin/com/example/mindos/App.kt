package com.example.mindos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mindos.core.di.AppGraph
import com.example.mindos.core.di.rememberAppGraph
import com.example.mindos.core.logging.ObservabilityHub
import com.example.mindos.core.navigation.AppRoute
import com.example.mindos.core.navigation.AppRouteStack
import com.example.mindos.core.navigation.LaunchDestinationResolver
import com.example.mindos.core.navigation.PlatformBackHandler
import com.example.mindos.core.navigation.telemetryName
import com.example.mindos.core.session.PreviewSessionRepository
import com.example.mindos.core.session.SessionRepository
import com.example.mindos.design.AppColors
import com.example.mindos.design.MindOSTheme
import com.example.mindos.features.auth.ui.AuthScreen
import com.example.mindos.features.element_selection.ui.ElementSelectionScreen
import com.example.mindos.features.home.presentation.HomeScreen
import com.example.mindos.features.social.presentation.feed.GlobalFeedScreen
import com.example.mindos.features.wallet.presentation.MoneyScreen
import com.example.mindos.features.wallet.presentation.rememberWalletStateHolder
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

    // React to auth state: clear stack on sign-out, rebind every entry on user refresh.
    LaunchedEffect(sessionRepository) {
        sessionRepository.authState.collect { user ->
            if (user == null) {
                stack.replaceAll(AppRoute.Auth)
                ObservabilityHub.customKey("authenticated", false)
            } else {
                stack.rebindStackWithUser(user)
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
    val dmStateHolder = com.example.mindos.features.dm.presentation.rememberDMStateHolder(
        repository = appGraph.dmRepository,
        waveRequestService = waveRequestService,
    )

    MindOSTheme {
        com.example.mindos.features.privacy.PrivacyScreenOverlay(
            // Gated by BOTH the user's privacy toggle AND the remote kill switch:
            // ops can disable the continuous-camera overlay via Remote Config
            // (privacy_overlay_enabled) without an app update if it misbehaves.
            enabled = appGraph.privacyEnabledState.value &&
                com.example.mindos.core.config.FeatureFlagsHub.current.privacyOverlayEnabled,
        ) {
            // Hardware/gesture back is wired to stack.pop() when there is somewhere to go.
            PlatformBackHandler(enabled = stack.canGoBack, onBack = { stack.pop() })

            com.example.mindos.core.ui.gesture.FireBackGestureHandler(
                enabled = stack.canGoBack,
                onBack = { stack.pop() },
            ) {
                when (val currentRoute = stack.current) {
                    AppRoute.Loading -> LaunchLoadingScreen()

                    AppRoute.Auth -> AuthScreen(
                        sessionRepository = sessionRepository,
                        onAuthenticated = {
                            stack.replaceAll(destinationResolver.resolvePostLoginRoute())
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

                    is AppRoute.Settings -> com.example.mindos.features.privacy.SettingsScreen(
                        privacyState = appGraph.privacyEnabledState,
                        onBack = { stack.pop() },
                    )

                    is AppRoute.MediaViewer -> com.example.mindos.features.social.presentation.media.AppMediaViewer(
                        content = currentRoute.content,
                        onDismiss = { stack.pop() },
                    )

                    is AppRoute.DMList -> com.example.mindos.features.dm.presentation.DMListScreen(
                        user = currentRoute.user,
                        dmRepository = appGraph.dmRepository,
                        waveRequestService = waveRequestService,
                        onBack = { stack.pop() },
                        onOpenChat = { chatId, otherUserId ->
                            stack.push(AppRoute.DMChat(currentRoute.user, chatId, otherUserId))
                        },
                        onOpenCajonera = { stack.push(AppRoute.DMCajonera(currentRoute.user)) },
                    )

                    is AppRoute.DMChat -> com.example.mindos.features.dm.presentation.DMChatScreen(
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

                    is AppRoute.DMChatUserInfo -> com.example.mindos.features.dm.presentation.DMChatUserInfoRoute(
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

                    is AppRoute.DMCajonera -> com.example.mindos.features.dm.presentation.Cajonera(
                        currentUserId = currentRoute.user.uid,
                        waveRequestService = waveRequestService,
                        onBack = { stack.pop() },
                    )

                    is AppRoute.Moneda -> MoneyScreen(
                        stateHolder = rememberWalletStateHolder(appGraph.walletClient),
                        onBack = { stack.pop() },
                    )
                }
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

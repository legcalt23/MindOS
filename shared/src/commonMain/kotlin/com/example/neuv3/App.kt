package com.example.neuv3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import com.example.neuv3.core.navigation.AppRoute
import com.example.neuv3.core.navigation.LaunchDestinationResolver
import com.example.neuv3.core.session.PreviewSessionRepository
import com.example.neuv3.core.session.SessionRepository
import com.example.neuv3.design.NEUTheme
import com.example.neuv3.feature.home.presentation.HomeScreen
import com.example.neuv3.features.element_selection.ui.ElementSelectionScreen
import com.example.neuv3.features.auth.ui.AuthScreen
import com.example.neuv3.features.social.data.feed.rememberGlobalFeedRepository
import com.example.neuv3.features.social.presentation.feed.GlobalFeedScreen
import com.example.neuv3.features.wallet.data.FirebaseWalletApiClient
import com.example.neuv3.features.wallet.presentation.MoneyScreen
import com.example.neuv3.features.wallet.presentation.rememberWalletStateHolder
import kotlinx.coroutines.launch

@Composable
fun App() {
    NEUApp(
        sessionRepository = remember { PreviewSessionRepository(isAuthenticated = false) },
    )
}

@Composable
fun NEUApp(
    sessionRepository: SessionRepository,
) {
    val destinationResolver = remember(sessionRepository) {
        LaunchDestinationResolver(sessionRepository)
    }
    var route by remember(destinationResolver) {
        mutableStateOf(destinationResolver.resolveInitialRoute())
    }
    var previousMediaRoute by remember(destinationResolver) {
        mutableStateOf<AppRoute?>(null)
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionRepository) {
        sessionRepository.authState.collect { user ->
            route = if (user == null) {
                AppRoute.Auth
            } else {
                when (route) {
                    is AppRoute.GlobalFeed -> AppRoute.GlobalFeed(user)
                    is AppRoute.MediaViewer -> AppRoute.MediaViewer(user, (route as AppRoute.MediaViewer).content)
                    is AppRoute.DMChat -> AppRoute.DMChat(user, (route as AppRoute.DMChat).chatId, (route as AppRoute.DMChat).otherUserId)
                    is AppRoute.DMChatUserInfo -> AppRoute.DMChatUserInfo(
                        user = user,
                        chatId = (route as AppRoute.DMChatUserInfo).chatId,
                        otherUserId = (route as AppRoute.DMChatUserInfo).otherUserId,
                        isOnline = (route as AppRoute.DMChatUserInfo).isOnline,
                    )
                    is AppRoute.DMCajonera -> AppRoute.DMCajonera(
                        user = user,
                        chatId = (route as AppRoute.DMCajonera).chatId,
                        otherUserId = (route as AppRoute.DMCajonera).otherUserId,
                        isOnline = (route as AppRoute.DMCajonera).isOnline,
                    )
                    is AppRoute.Moneda -> AppRoute.Moneda(user)
                    else -> AppRoute.Main(user)
                }
            }
        }
    }

    val privacyEnabledState = com.example.neuv3.features.privacy.rememberPrivacyEnabled()
    val globalFeedRepository = rememberGlobalFeedRepository()
    
    val dmRepository = remember { com.example.neuv3.features.dm.data.FirebaseDMRepository() }
    val waveRequestRepository = remember { com.example.neuv3.features.waves.data.InMemoryWaveRequestRepository() }
    val waveRequestService = remember {
        com.example.neuv3.features.waves.domain.service.WaveRequestService(waveRequestRepository)
    }
    val dmStateHolder = com.example.neuv3.features.dm.presentation.rememberDMStateHolder(
        repository = dmRepository,
        waveRequestService = waveRequestService,
    )
    val walletClient = remember { FirebaseWalletApiClient() }

    NEUTheme {
        com.example.neuv3.features.privacy.PrivacyScreenOverlay(enabled = privacyEnabledState.value) {
            val onGlobalBack: (() -> Unit)? = when (val currentRoute = route) {
                is AppRoute.GlobalFeed -> { { route = AppRoute.Main(currentRoute.user) } }
                is AppRoute.Settings -> { { route = AppRoute.Main(currentRoute.user) } }
                is AppRoute.MediaViewer -> { {
                    route = previousMediaRoute ?: AppRoute.Main(currentRoute.user)
                    previousMediaRoute = null
                } }
                is AppRoute.DMList -> { { route = AppRoute.Main(currentRoute.user) } }
                is AppRoute.DMChat -> { { route = AppRoute.DMList(currentRoute.user) } }
                is AppRoute.DMChatUserInfo -> { {
                    route = AppRoute.DMChat(currentRoute.user, currentRoute.chatId, currentRoute.otherUserId)
                } }
                is AppRoute.DMCajonera -> { {
                    val chatId = currentRoute.chatId
                    val otherUserId = currentRoute.otherUserId
                    route = if (chatId != null && otherUserId != null) {
                        AppRoute.DMChatUserInfo(
                            user = currentRoute.user,
                            chatId = chatId,
                            otherUserId = otherUserId,
                            isOnline = currentRoute.isOnline,
                        )
                    } else {
                        AppRoute.DMList(currentRoute.user)
                    }
                } }
                is AppRoute.Moneda -> { { route = AppRoute.Main(currentRoute.user) } }
                else -> null
            }

            com.example.neuv3.core.ui.gesture.FireBackGestureHandler(
                enabled = onGlobalBack != null,
                onBack = { onGlobalBack?.invoke() }
            ) {
                when (val currentRoute = route) {
                    AppRoute.Loading -> LaunchLoadingScreen()
                AppRoute.Auth -> AuthScreen(
                    sessionRepository = sessionRepository,
                    onAuthenticated = {
                        route = destinationResolver.resolvePostLoginRoute()
                    },
                )
    
                AppRoute.ElementSelection -> ElementSelectionScreen(
                    onElementSelected = {
                        route = destinationResolver.resolvePostLoginRoute()
                    },
                )
    
                is AppRoute.Main -> HomeScreen(
                    user = currentRoute.user,
                    globalFeedRepository = globalFeedRepository,
                    onOpenFeed = {
                        route = AppRoute.GlobalFeed(currentRoute.user)
                    },
                    onSignOut = {
                        scope.launch {
                            sessionRepository.signOut()
                            route = AppRoute.Auth
                        }
                    },
                    onOpenSettings = {
                        route = AppRoute.Settings(currentRoute.user)
                    },
                    initialProfileId = currentRoute.initialProfileId,
                    initialProfileHandle = currentRoute.initialProfileHandle,
                    initialProfileName = currentRoute.initialProfileName,
                    onOpenMedia = { content ->
                        previousMediaRoute = currentRoute
                        route = AppRoute.MediaViewer(currentRoute.user, content)
                    },
                    onOpenDMList = {
                        route = AppRoute.DMList(currentRoute.user)
                    },
                    onOpenMoneda = {
                        route = AppRoute.Moneda(currentRoute.user)
                    },
                )
    
                is AppRoute.GlobalFeed -> GlobalFeedScreen(
                    user = currentRoute.user,
                    repository = globalFeedRepository,
                    onBack = {
                        route = AppRoute.Main(currentRoute.user)
                    },
                    onOpenProfile = { userId, username, displayName ->
                        route = AppRoute.Main(
                            user = currentRoute.user,
                            initialProfileId = userId,
                            initialProfileHandle = username,
                            initialProfileName = displayName,
                        )
                    },
                    onOpenMedia = { content ->
                        previousMediaRoute = currentRoute
                        route = AppRoute.MediaViewer(currentRoute.user, content)
                    }
                )
                
                is AppRoute.Settings -> com.example.neuv3.features.privacy.SettingsScreen(
                    privacyState = privacyEnabledState,
                    onBack = { route = AppRoute.Main(currentRoute.user) }
                )

                is AppRoute.MediaViewer -> com.example.neuv3.features.social.presentation.media.AppMediaViewer(
                    content = currentRoute.content,
                    onDismiss = {
                        route = previousMediaRoute ?: AppRoute.Main(currentRoute.user)
                        previousMediaRoute = null
                    }
                )

                is AppRoute.DMList -> com.example.neuv3.features.dm.presentation.DMListScreen(
                    user = currentRoute.user,
                    dmRepository = dmRepository,
                    waveRequestService = waveRequestService,
                    onBack = { route = AppRoute.Main(currentRoute.user) },
                    onOpenChat = { chatId, otherUserId ->
                        route = AppRoute.DMChat(currentRoute.user, chatId, otherUserId)
                    },
                    onOpenCajonera = {
                        route = AppRoute.DMCajonera(currentRoute.user)
                    },
                )

                is AppRoute.DMChat -> com.example.neuv3.features.dm.presentation.DMChatScreen(
                    user = currentRoute.user,
                    chatId = currentRoute.chatId,
                    otherUserId = currentRoute.otherUserId,
                    stateHolder = dmStateHolder,
                    onBack = { route = AppRoute.DMList(currentRoute.user) },
                    onOpenUserInfo = {
                        route = AppRoute.DMChatUserInfo(
                            user = currentRoute.user,
                            chatId = currentRoute.chatId,
                            otherUserId = currentRoute.otherUserId,
                            isOnline = true,
                        )
                    },
                    onOpenMedia = { content ->
                        previousMediaRoute = currentRoute
                        route = AppRoute.MediaViewer(currentRoute.user, content)
                    }
                )

                is AppRoute.DMChatUserInfo -> com.example.neuv3.features.dm.presentation.DMChatUserInfoRoute(
                    user = currentRoute.user,
                    chatId = currentRoute.chatId,
                    otherUserId = currentRoute.otherUserId,
                    stateHolder = dmStateHolder,
                    isOnline = currentRoute.isOnline,
                    onBack = {
                        route = AppRoute.DMChat(currentRoute.user, currentRoute.chatId, currentRoute.otherUserId)
                    },
                    onOpenCajonera = {
                        route = AppRoute.DMCajonera(
                            user = currentRoute.user,
                            chatId = currentRoute.chatId,
                            otherUserId = currentRoute.otherUserId,
                            isOnline = currentRoute.isOnline,
                        )
                    },
                    onOpenMedia = { content ->
                        previousMediaRoute = currentRoute
                        route = AppRoute.MediaViewer(currentRoute.user, content)
                    }
                )

                is AppRoute.DMCajonera -> com.example.neuv3.features.dm.presentation.Cajonera(
                    currentUserId = currentRoute.user.uid,
                    waveRequestService = waveRequestService,
                    onBack = {
                        val chatId = currentRoute.chatId
                        val otherUserId = currentRoute.otherUserId
                        route = if (chatId != null && otherUserId != null) {
                            AppRoute.DMChatUserInfo(
                                user = currentRoute.user,
                                chatId = chatId,
                                otherUserId = otherUserId,
                                isOnline = currentRoute.isOnline,
                            )
                        } else {
                            AppRoute.DMList(currentRoute.user)
                        }
                    }
                )

                is AppRoute.Moneda -> MoneyScreen(
                    stateHolder = rememberWalletStateHolder(walletClient),
                    onBack = { route = AppRoute.Main(currentRoute.user) },
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
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "NEU",
            color = Color(0xFFE7E0CF),
        )
    }
}

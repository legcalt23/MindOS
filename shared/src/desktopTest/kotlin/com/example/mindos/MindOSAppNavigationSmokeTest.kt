package com.example.mindos

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.example.mindos.core.di.AppGraph
import com.example.mindos.core.session.PreviewSessionRepository
import com.example.mindos.fakes.NoOpDMRepository
import com.example.mindos.fakes.NoOpWalletApiClient
import com.example.mindos.features.social.domain.feed.InMemoryGlobalFeedRepository
import com.example.mindos.features.waves.data.InMemoryWaveRequestRepository
import com.example.mindos.features.waves.domain.service.WaveRequestService
import kotlin.test.Test

/**
 * Top-level navigation smoke test for MindOSApp.
 *
 * Exercises the same composable graph that MainActivity drives in production,
 * but with an injected [AppGraph] containing fake collaborators so no Firebase
 * initialisation is needed.
 *
 * Scope is intentionally limited to the unauthenticated path:
 *   - PreviewSessionRepository(isAuthenticated = false) → resolveInitialRoute
 *     returns AppRoute.Auth → AuthScreen renders.
 *
 * Once richer fakes exist for the authenticated flow (Main / DM / Wallet),
 * additional tests can drive sign-in transitions and route switches.
 */
@OptIn(ExperimentalTestApi::class)
class MindOSAppNavigationSmokeTest {

    @Test
    fun neuApp_with_empty_session_lands_on_auth_screen() = runComposeUiTest {
        val sessionRepository = PreviewSessionRepository(isAuthenticated = false)
        val waves = InMemoryWaveRequestRepository()
        val testGraph = AppGraph(
            dmRepository = NoOpDMRepository(),
            walletClient = NoOpWalletApiClient(),
            waveRequestRepository = waves,
            globalFeedRepository = InMemoryGlobalFeedRepository(),
            privacyEnabledState = mutableStateOf(false),
            waveRequestService = WaveRequestService(waves),
        )

        setContent {
            MindOSApp(
                sessionRepository = sessionRepository,
                appGraph = testGraph,
            )
        }

        // Initial route resolves to AppRoute.Auth when no user is authenticated.
        // AuthScreen's primary "Entrar" button confirms the route lands correctly.
        onNodeWithText("Entrar").assertIsDisplayed()
    }
}

package com.mindos.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mindos.app.core.di.AppGraph
import com.mindos.app.core.session.PreviewSessionRepository
import com.mindos.app.fakes.NoOpDMRepository
import com.mindos.app.fakes.NoOpWalletApiClient
import com.mindos.app.features.profile.data.InMemoryMindOSProfileRepository
import com.mindos.app.features.profile.data.InMemoryProfileObserverRepository
import com.mindos.app.features.profile.data.InMemoryProfilePresenceRepository
import com.mindos.app.features.social.domain.feed.InMemoryGlobalFeedRepository
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.shared.util.AppClock
import com.mindos.app.features.waves.data.InMemoryWaveRequestRepository
import com.mindos.app.features.waves.domain.service.WaveRequestService
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
            appClock = AppClock { Timestamp(123L) },
            mindOSProfileRepository = InMemoryMindOSProfileRepository(),
            mindOSProfileObserverRepository = InMemoryProfileObserverRepository(baseObserverCount = 0),
            mindOSProfilePresenceRepository = InMemoryProfilePresenceRepository(),
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

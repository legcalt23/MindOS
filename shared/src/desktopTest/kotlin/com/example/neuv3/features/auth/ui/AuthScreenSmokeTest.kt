package com.example.neuv3.features.auth.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.example.neuv3.core.session.PreviewSessionRepository
import kotlin.test.Test

/**
 * Desktop smoke test: AuthScreen renders its primary affordances when the
 * session is empty. Lives in desktopTest (not commonTest) because Compose UI
 * tests require Skiko/AWT, which only the desktop target wires up.
 *
 * This is intentionally minimal — it does NOT assert behavior, only that the
 * composable graph builds and the main labels reach the screen. It exists to:
 *   1. Prove the Compose Multiplatform UI test infra is wired correctly.
 *   2. Catch any future regression that prevents AuthScreen from rendering at all.
 */
@OptIn(ExperimentalTestApi::class)
class AuthScreenSmokeTest {

    @Test
    fun authScreen_renders_primary_buttons_when_session_is_empty() = runComposeUiTest {
        val sessionRepository = PreviewSessionRepository(isAuthenticated = false)

        setContent {
            AuthScreen(
                sessionRepository = sessionRepository,
                onAuthenticated = { /* no-op for smoke test */ },
            )
        }

        // Default UI is the sign-in mode → primary button reads "Entrar",
        // secondary toggle reads "Nueva cuenta".
        onNodeWithText("Entrar").assertIsDisplayed()
        onNodeWithText("Nueva cuenta").assertIsDisplayed()
        onNodeWithText("Email").assertIsDisplayed()
    }
}

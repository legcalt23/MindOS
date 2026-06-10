package com.mindos.app.features.auth.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mindos.app.core.session.PreviewSessionRepository
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

    @Test
    fun authScreen_toggling_to_create_account_reveals_display_name_field() = runComposeUiTest {
        val sessionRepository = PreviewSessionRepository(isAuthenticated = false)

        setContent {
            AuthScreen(
                sessionRepository = sessionRepository,
                onAuthenticated = { /* no-op */ },
            )
        }

        // Sign-in mode (default): the "Nombre visible" field is hidden.
        onAllNodesWithText("Nombre visible").assertCountEquals(0)

        // Toggling into create-account mode reveals the displayName input and
        // flips the primary button label from "Entrar" to "Crear cuenta".
        onNodeWithText("Nueva cuenta").performClick()

        onNodeWithText("Nombre visible").assertIsDisplayed()
        onNodeWithText("Crear cuenta").assertIsDisplayed()
        // The secondary toggle now offers the inverse action.
        onNodeWithText("Ya tengo cuenta").assertIsDisplayed()
    }
}

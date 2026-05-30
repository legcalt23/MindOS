package com.example.mindos.features.privacy

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.example.mindos.core.session.PreviewSessionRepository
import com.example.mindos.design.MindOSTheme
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * UI coverage for the account-deletion flow in [SettingsScreen] (B1.6).
 *
 * Pins the safety gating that store reviewers / GDPR require:
 *  - the deletion is behind an explicit confirmation dialog,
 *  - the confirm button is disabled until the user types the exact word,
 *  - confirming actually invokes SessionRepository.deleteAccount() (session cleared).
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenDeleteTest {

    @Test
    fun deleteAccount_requiresTypedConfirmation_thenClearsSession() = runComposeUiTest {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        assertNotNull(repo.currentUser())

        setContent {
            MindOSTheme {
                SettingsScreen(
                    privacyState = mutableStateOf(false),
                    sessionRepository = repo,
                    onBack = {},
                )
            }
        }

        // Open the confirmation dialog from the danger zone.
        onNodeWithText("ELIMINAR CUENTA").performClick()
        onNodeWithText("Eliminar cuenta").assertIsDisplayed()

        // Confirm is disabled until the exact word is typed.
        onNodeWithText("Eliminar definitivamente").assertIsNotEnabled()

        onNode(hasSetTextAction()).performTextInput("ELIMINAR")
        onNodeWithText("Eliminar definitivamente").assertIsEnabled()

        // Confirm → deleteAccount() runs → session cleared.
        onNodeWithText("Eliminar definitivamente").performClick()
        waitForIdle()

        assertNull(repo.currentUser(), "Confirming deletion must clear the session")
    }

    @Test
    fun deleteAccount_cancel_keepsSession() = runComposeUiTest {
        val repo = PreviewSessionRepository(isAuthenticated = true)

        setContent {
            MindOSTheme {
                SettingsScreen(
                    privacyState = mutableStateOf(false),
                    sessionRepository = repo,
                    onBack = {},
                )
            }
        }

        onNodeWithText("ELIMINAR CUENTA").performClick()
        onNodeWithText("Cancelar").performClick()
        waitForIdle()

        assertNotNull(repo.currentUser(), "Cancelling must NOT delete the account")
    }
}

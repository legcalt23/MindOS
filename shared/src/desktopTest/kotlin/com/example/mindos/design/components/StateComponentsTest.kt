package com.example.mindos.design.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.example.mindos.core.ui.UserFacingError
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose UI tests for the shared state components, rendered on the desktop
 * (Skiko) test target. These pin the accessibility contract added in Phase 4:
 *  - the retry button is reachable (>= 48dp touch target) and actionable
 *  - the loading state exposes a content description for screen readers
 *  - retryable vs non-retryable errors render the right affordances
 */
@OptIn(ExperimentalTestApi::class)
class StateComponentsTest {

    private fun error(retryable: Boolean) = UserFacingError(
        title = "Algo salió mal",
        message = "No se pudo cargar.",
        code = "test_error",
        isRetryable = retryable,
    )

    @Test
    fun errorScreen_retryable_showsButton_andFiresCallback() = runComposeUiTest {
        var retried = false
        setContent {
            ErrorScreen(error = error(retryable = true), onRetry = { retried = true })
        }
        onNodeWithText("Algo salió mal").assertIsDisplayed()
        onNodeWithText("Reintentar").assertHasClickAction()
        onNodeWithText("Reintentar").performClick()
        assertTrue(retried, "Tapping Reintentar must invoke onRetry")
    }

    @Test
    fun errorScreen_retryButton_meetsMinTouchTarget() = runComposeUiTest {
        setContent {
            ErrorScreen(error = error(retryable = true), onRetry = {})
        }
        // WCAG 2.5.5 / Material minimum interactive target.
        onNodeWithText("Reintentar").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun errorScreen_nonRetryable_hidesRetryButton() = runComposeUiTest {
        setContent {
            ErrorScreen(error = error(retryable = false), onRetry = {})
        }
        onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun loadingScreen_exposesContentDescriptionForScreenReaders() = runComposeUiTest {
        setContent { LoadingScreen(message = "Cargando datos") }
        onNodeWithContentDescription("Cargando datos").assertExists()
    }

    @Test
    fun emptyScreen_showsTitleAndMessage() = runComposeUiTest {
        setContent { EmptyScreen(title = "Sin resultados", message = "Nada por aquí.") }
        onNodeWithText("Sin resultados").assertIsDisplayed()
        onNodeWithText("Nada por aquí.").assertIsDisplayed()
    }
}

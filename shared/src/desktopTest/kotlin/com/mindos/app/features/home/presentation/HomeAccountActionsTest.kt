package com.mindos.app.features.home.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mindos.app.design.MindOSTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HomeAccountActionsTest {

    @Test
    fun settingsEntryInvokesApplicationNavigationCallback() = runComposeUiTest {
        var settingsClicks = 0
        var signOutClicks = 0

        setContent {
            MindOSTheme {
                HomeAccountActions(
                    onOpenSettings = { settingsClicks += 1 },
                    onSignOut = { signOutClicks += 1 },
                )
            }
        }

        onNodeWithText("AJUSTES").performClick()

        assertEquals(1, settingsClicks)
        assertEquals(0, signOutClicks)
    }
}

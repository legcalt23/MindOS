package com.mindos.action.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationContractsTest {

    @Test
    fun commandTypesAreStable() {
        assertEquals("open_profile", MindNavigationCommand.OpenProfile("u1").commandType)
        assertEquals("open_story", MindNavigationCommand.OpenStory("s1").commandType)
        assertEquals("open_chat", MindNavigationCommand.OpenChat("c1").commandType)
        assertEquals("open_messages", MindNavigationCommand.OpenMessages.commandType)
        assertEquals("open_settings", MindNavigationCommand.OpenSettings.commandType)
    }

    @Test
    fun commandsCarryResolvedIds() {
        assertEquals("u1", MindNavigationCommand.OpenProfile("u1").userId)
    }

    @Test
    fun navigationOutcomesAreTyped() {
        val outcomes: List<NavigationOutcome> = listOf(
            NavigationOutcome.Success,
            NavigationOutcome.Rejected("denied"),
            NavigationOutcome.Failed("boom"),
        )
        assertTrue(outcomes[0] is NavigationOutcome.Success)
        assertEquals("denied", (outcomes[1] as NavigationOutcome.Rejected).reason)
    }
}

package com.mindos.action.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MindActionTest {

    private val id = RequestId("req-1")

    @Test
    fun openProfileExposesUsernameReference() {
        val action = MindAction.OpenProfile(id, "ana")
        assertEquals("open_profile", action.actionType)
        assertEquals(listOf(ActionReference(ReferenceKind.USERNAME_TO_USER_ID, "ana")), action.references())
    }

    @Test
    fun openStoryAndChatExposeTheirReferences() {
        assertEquals(ReferenceKind.STORY_SLUG_TO_STORY_ID, MindAction.OpenStory(id, "s").references().single().kind)
        assertEquals(ReferenceKind.CHAT_ALIAS_TO_CHAT_ID, MindAction.OpenChat(id, "c").references().single().kind)
    }

    @Test
    fun referencelessActionsHaveNoReferences() {
        assertTrue(MindAction.OpenSettings(id).references().isEmpty())
        assertTrue(MindAction.OpenMessages(id).references().isEmpty())
    }

    @Test
    fun requestIdRejectsBlank() {
        assertFailsWith<IllegalArgumentException> { RequestId("  ") }
    }

    @Test
    fun requestIdFactoryIsInjectable() {
        var n = 0
        val factory = RequestIdFactory { RequestId("r${n++}") }
        assertEquals(RequestId("r0"), factory.next())
        assertEquals(RequestId("r1"), factory.next())
    }
}

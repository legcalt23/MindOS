package com.mindos.app.features.dm.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class DMConversationIdsTest {

    @Test
    fun chatIdIsStableRegardlessOfParticipantOrder() {
        val first = DMConversationIds.chatId("user_alpha", "user-beta")
        val second = DMConversationIds.chatId("user-beta", "user_alpha")

        assertEquals(first, second)
        assertEquals(listOf("user-beta", "user_alpha"), DMConversationIds.participants("user_alpha", "user-beta"))
    }

    @Test
    fun chatIdDoesNotCollideWhenParticipantIdsContainUnderscores() {
        val firstPair = DMConversationIds.chatId("aaa_bbb", "ccc")
        val secondPair = DMConversationIds.chatId("aaa", "bbb_ccc")

        assertEquals(
            DMConversationIds.legacyChatId("aaa_bbb", "ccc"),
            DMConversationIds.legacyChatId("aaa", "bbb_ccc")
        )
        assertNotEquals(firstPair, secondPair)
    }

    @Test
    fun chatIdRejectsInvalidPairs() {
        assertFailsWith<IllegalArgumentException> {
            DMConversationIds.chatId("same_user", "same_user")
        }
        assertFailsWith<IllegalArgumentException> {
            DMConversationIds.chatId("ok_user", "../bad")
        }
    }
}

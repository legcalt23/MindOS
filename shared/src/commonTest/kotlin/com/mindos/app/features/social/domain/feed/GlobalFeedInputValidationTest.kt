package com.mindos.app.features.social.domain.feed

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GlobalFeedInputValidationTest {
    @Test
    fun rejectsMaliciousIdsAndControlCharactersBeforePersistence() {
        val maliciousId = validTextRequest().copy(userId = "admin'--")
        val controlText = validTextRequest().copy(text = "hola\u0000mundo")

        assertNotNull(validateGlobalFeedPostDraft(maliciousId))
        assertNotNull(validateGlobalFeedPostDraft(controlText))
    }

    @Test
    fun allowsSqlLikeHumanTextBecauseFirestoreIsNotSqlBacked() {
        val request = validTextRequest().copy(text = "' OR '1'='1\n%_%_%")

        assertNull(validateGlobalFeedPostDraft(request))
    }

    private fun validTextRequest(): CreateGlobalFeedPostRequest =
        CreateGlobalFeedPostRequest(
            userId = "user_123",
            authorUsername = "user_123",
            authorDisplayName = "User 123",
            postType = GlobalFeedPostType.TEXT,
            text = "Hola",
            attachments = emptyList(),
        )
}

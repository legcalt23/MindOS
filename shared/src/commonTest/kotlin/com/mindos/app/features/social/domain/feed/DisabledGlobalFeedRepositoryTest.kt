package com.mindos.app.features.social.domain.feed

import com.mindos.app.features.social.shared.util.DomainResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DisabledGlobalFeedRepositoryTest {
    @Test
    fun disabledRepositoryFailsClosedForReadsAndWrites() = runTest {
        val repository = DisabledGlobalFeedRepository("disabled for test")

        val feed = repository.loadGlobalFeed()
        val userPosts = repository.loadPostsByUser(userId = "user")
        val create = repository.createPost(
            CreateGlobalFeedPostRequest(
                userId = "user",
                authorUsername = "user",
                authorDisplayName = "User",
                postType = GlobalFeedPostType.TEXT,
                text = "hello",
                attachments = emptyList(),
            ),
        )

        assertEquals("persistence_failure", assertIs<DomainResult.Failure>(feed).error.code)
        assertEquals("persistence_failure", assertIs<DomainResult.Failure>(userPosts).error.code)
        assertEquals("persistence_failure", assertIs<DomainResult.Failure>(create).error.code)
    }
}

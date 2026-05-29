package com.example.mindos.features.social.domain.feed

import com.example.mindos.features.social.shared.util.DomainResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InMemoryGlobalFeedRepositoryTest {

    @Test
    fun emptyFeedReturnsEmptyList() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        val result = repo.loadGlobalFeed()
        assertTrue(result is DomainResult.Success)
        assertEquals(0, result.value.size)
    }

    @Test
    fun createdPostIsReturnedInFeed() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        val createResult = repo.createPost(validTextRequest("hello world"))
        assertTrue(createResult is DomainResult.Success)

        val feed = repo.loadGlobalFeed()
        assertTrue(feed is DomainResult.Success)
        assertEquals(1, feed.value.size)
        assertEquals("hello world", feed.value.first().text)
    }

    @Test
    fun feedRespectsLimit() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        repeat(5) { i ->
            val r = repo.createPost(validTextRequest("post-$i"))
            assertTrue(r is DomainResult.Success)
        }

        val feed = repo.loadGlobalFeed(limit = 3)
        assertTrue(feed is DomainResult.Success)
        assertEquals(3, feed.value.size)
    }

    @Test
    fun feedIsSortedByCreatedAtDescending() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        repeat(3) { i -> repo.createPost(validTextRequest("post-$i")) }

        val feed = repo.loadGlobalFeed()
        assertTrue(feed is DomainResult.Success)
        val timestamps = feed.value.map { it.createdAtMillis }
        // Descending order: each timestamp should be >= the next
        timestamps.zipWithNext().forEach { (a, b) ->
            assertTrue(a >= b, "Feed should be descending by createdAt: $a !>= $b")
        }
    }

    @Test
    fun loadPostsByUserFiltersCorrectly() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        repo.createPost(validTextRequest("from-alice", userId = "alice_uid"))
        repo.createPost(validTextRequest("from-bob", userId = "bob_uid"))
        repo.createPost(validTextRequest("from-alice-2", userId = "alice_uid"))

        val aliceFeed = repo.loadPostsByUser("alice_uid")
        assertTrue(aliceFeed is DomainResult.Success)
        assertEquals(2, aliceFeed.value.size)
        assertTrue(aliceFeed.value.all { it.userId == "alice_uid" })
    }

    @Test
    fun loadPostsByUserReturnsEmptyForUnknownUser() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        repo.createPost(validTextRequest("only-post"))

        val result = repo.loadPostsByUser("ghost_uid")
        assertTrue(result is DomainResult.Success)
        assertEquals(0, result.value.size)
    }

    @Test
    fun invalidPostIsRejected() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        // Empty text on TEXT post should fail validation
        val result = repo.createPost(validTextRequest(""))
        assertTrue(result is DomainResult.Failure)
    }

    @Test
    fun limitIsCoercedToValidRange() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        repeat(3) { repo.createPost(validTextRequest("post-$it")) }

        // limit=0 should be coerced to at least 1
        val zeroLimit = repo.loadGlobalFeed(limit = 0)
        assertTrue(zeroLimit is DomainResult.Success)
        assertTrue(zeroLimit.value.size >= 1)
    }

    @Test
    fun postIdIsUniqueForSequentialCreates() = runTest {
        val repo = InMemoryGlobalFeedRepository()
        val r1 = repo.createPost(validTextRequest("first"))
        val r2 = repo.createPost(validTextRequest("second"))
        assertTrue(r1 is DomainResult.Success)
        assertTrue(r2 is DomainResult.Success)
        assertNotNull(r1.value.id)
        assertNotNull(r2.value.id)
        assertTrue(r1.value.id != r2.value.id, "Sequential posts must have distinct ids")
    }

    private fun validTextRequest(
        text: String,
        userId: String = "valid_user_id",
    ): CreateGlobalFeedPostRequest =
        CreateGlobalFeedPostRequest(
            userId = userId,
            authorUsername = "user.name",
            authorDisplayName = "User Name",
            postType = GlobalFeedPostType.TEXT,
            text = text,
            attachments = emptyList(),
        )
}

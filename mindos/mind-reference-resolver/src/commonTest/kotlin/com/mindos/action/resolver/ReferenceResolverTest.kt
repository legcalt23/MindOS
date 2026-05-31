package com.mindos.action.resolver

import com.mindos.action.model.ReferenceKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferenceResolverTest {

    private fun usernameResolver() = InMemoryReferenceResolver(
        ReferenceKind.USERNAME_TO_USER_ID,
        mapOf("ana" to "u_ana", "bob" to "u_bob"),
    )

    @Test
    fun inMemoryResolvesHit() = runTest {
        val r = usernameResolver().resolve("ana")
        assertEquals("u_ana", (r as ReferenceResolutionResult.Resolved).canonicalId)
    }

    @Test
    fun inMemoryReturnsNotFoundOnMiss() = runTest {
        assertTrue(usernameResolver().resolve("nobody") is ReferenceResolutionResult.NotFound)
    }

    @Test
    fun cachingResolverCachesResolvedOnly() = runTest {
        val caching = CachingReferenceResolver(usernameResolver())
        assertTrue(caching.resolve("ana") is ReferenceResolutionResult.Resolved)
        assertTrue(caching.resolve("ana") is ReferenceResolutionResult.Resolved) // served from cache
        assertEquals(1, caching.cachedCount())
        assertTrue(caching.resolve("ghost") is ReferenceResolutionResult.NotFound)
        assertEquals(1, caching.cachedCount()) // misses are not cached
    }

    @Test
    fun registryLooksUpByKind() {
        val registry = ResolverRegistry(listOf(usernameResolver()))
        assertTrue(registry.supports(ReferenceKind.USERNAME_TO_USER_ID))
        assertTrue(!registry.supports(ReferenceKind.CHAT_ALIAS_TO_CHAT_ID))
        assertEquals(ReferenceKind.USERNAME_TO_USER_ID, registry.forKind(ReferenceKind.USERNAME_TO_USER_ID)?.kind)
    }
}

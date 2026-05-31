package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.patterns.PatternMatch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ResolverRegistryTest {

    private val stubResolver = object : CommandResolver {
        override val id = ResolverId("stub")
        override suspend fun resolve(match: PatternMatch, context: ResolutionContext) =
            ResolutionResult.Failed(ResolutionFailure.UnsupportedCommand("stub"))
    }

    @Test
    fun returnsResolver_forRegisteredPattern() {
        val registry = ResolverRegistry(mapOf(PatternId("p1") to stubResolver))
        assertNotNull(registry.getResolver(PatternId("p1")))
    }

    @Test
    fun returnsNull_forUnregisteredPattern() {
        val registry = ResolverRegistry(emptyMap())
        assertNull(registry.getResolver(PatternId("missing")))
    }

    @Test
    fun patternIds_reflectsRegisteredKeys() = runTest {
        val registry = ResolverRegistry(mapOf(
            PatternId("a") to stubResolver,
            PatternId("b") to stubResolver
        ))
        val ids = registry.patternIds()
        assert(PatternId("a") in ids)
        assert(PatternId("b") in ids)
    }
}

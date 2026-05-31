package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionIdFactory
import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.actions.TimestampProvider
import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.patterns.PatternMatch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavigationIntentResolverTest {

    private var idCounter = 0
    private val factory = ActionFactory(
        idFactory = ActionIdFactory { ActionId("id-${++idCounter}") },
        timestampProvider = TimestampProvider { 1000L }
    )
    private val resolver = NavigationIntentResolver(factory)

    private val ctx = ResolutionContext(source = CommandSource.Test)

    @Test
    fun resolves_targetParam_toNavigateAction() = runTest {
        val match = PatternMatch(PatternId("open-target"), mapOf("target" to "settings"))
        val result = resolver.resolve(match, ctx)
        assertIs<ResolutionResult.Resolved>(result)
        val action = result.action as NavigateAction
        assertEquals("settings", action.targetId)
        assertEquals(ActionTypes.Navigate, action.metadata.actionType)
    }

    @Test
    fun resolves_multiWord_target() = runTest {
        val match = PatternMatch(PatternId("open-target"), mapOf("target" to "privacy settings"))
        val result = resolver.resolve(match, ctx)
        assertIs<ResolutionResult.Resolved>(result)
        assertEquals("privacy settings", (result.action as NavigateAction).targetId)
    }

    @Test
    fun fails_whenTargetParamMissing() = runTest {
        val match = PatternMatch(PatternId("open-target"), emptyMap())
        val result = resolver.resolve(match, ctx)
        assertIs<ResolutionResult.Failed>(result)
        assertIs<ResolutionFailure.InvalidParameters>(result.failure)
    }

    @Test
    fun resolverId_isStable() {
        assertEquals(ResolverId("navigation-intent-resolver"), resolver.id)
    }

    @Test
    fun action_hasCorrectSource() = runTest {
        val match = PatternMatch(PatternId("p"), mapOf("target" to "home"))
        val result = resolver.resolve(match, ResolutionContext(source = CommandSource.ApiCall))
        assertIs<ResolutionResult.Resolved>(result)
        assertEquals(CommandSource.ApiCall, result.action.metadata.source)
    }
}

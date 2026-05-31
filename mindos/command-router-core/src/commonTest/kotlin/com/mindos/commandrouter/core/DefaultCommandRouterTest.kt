package com.mindos.commandrouter.core

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionIdFactory
import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.actions.TimestampProvider
import com.mindos.commandrouter.patterns.ExactCommandPattern
import com.mindos.commandrouter.patterns.ParameterizedCommandPattern
import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.patterns.PatternPriority
import com.mindos.commandrouter.resolver.ActionFactory
import com.mindos.commandrouter.resolver.NavigationIntentResolver
import com.mindos.commandrouter.resolver.ResolutionContext
import com.mindos.commandrouter.resolver.ResolutionFailure
import com.mindos.commandrouter.resolver.ResolutionResult
import com.mindos.commandrouter.resolver.ResolverId
import com.mindos.commandrouter.patterns.PatternMatch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DefaultCommandRouterTest {

    private var seq = 0
    private val fixedFactory = ActionFactory(
        ActionIdFactory { ActionId("id-${++seq}") },
        TimestampProvider { 1000L }
    )
    private val navResolver = NavigationIntentResolver(fixedFactory)

    private fun router(vararg pairs: Pair<com.mindos.commandrouter.patterns.CommandPattern, com.mindos.commandrouter.resolver.CommandResolver>): CommandRouter {
        val builder = CommandRouterBuilder()
        pairs.forEach { (pattern, resolver) -> builder.addPattern(pattern, resolver) }
        return builder.build()
    }

    private fun openPattern(id: String = "open-target") =
        ParameterizedCommandPattern(PatternId(id), "open {target}")

    @Test
    fun successfulRoute_navigateAction() = runTest {
        val r = router(openPattern() to navResolver)
        val result = r.route(CommandInput("open settings"))
        assertIs<CommandRouterResult.Success>(result)
        val action = result.action as NavigateAction
        assertEquals("settings", action.targetId)
        assertEquals(ActionTypes.Navigate, action.metadata.actionType)
    }

    @Test
    fun emptyInput_returnsEmptyInputFailure() = runTest {
        val r = router(openPattern() to navResolver)
        val result = r.route(CommandInput("   "))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.EmptyInput>(result.failure)
    }

    @Test
    fun unknownCommand_returnsNoPatternMatched() = runTest {
        val r = router(openPattern() to navResolver)
        val result = r.route(CommandInput("do something weird"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.NoPatternMatched>(result.failure)
    }

    @Test
    fun ambiguousCommand_returnsAmbiguousFailure() = runTest {
        val r = router(
            ParameterizedCommandPattern(PatternId("p1"), "do {thing}", PatternPriority(0)) to navResolver,
            ParameterizedCommandPattern(PatternId("p2"), "do {action}", PatternPriority(0)) to navResolver
        )
        val result = r.route(CommandInput("do something"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.AmbiguousCommand>(result.failure)
    }

    @Test
    fun inputTooLong_returnsInputTooLongFailure() = runTest {
        val r = CommandRouterBuilder()
            .withConfig(CommandRouterConfig(maxInputLength = 10))
            .build()
        val result = r.route(CommandInput("this input is way too long for the config"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.InputTooLong>(result.failure)
    }

    @Test
    fun unsafeControlInput_returnsUnsafeInputBeforeNormalization() = runTest {
        val r = router(openPattern() to navResolver)
        val result = r.route(CommandInput("open\u0001settings"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.UnsafeInput>(result.failure)
        assertEquals("invalid_control_character", result.trace.failureReason)
    }

    @Test
    fun noResolverForPattern_returnsNoResolverFailure() = runTest {
        val patternWithoutResolver = ExactCommandPattern(PatternId("orphan"), "orphan command")
        val r = DefaultCommandRouter(
            normalizer = com.mindos.commandrouter.patterns.DefaultCommandNormalizer(),
            patternRegistry = com.mindos.commandrouter.patterns.PatternRegistry(listOf(patternWithoutResolver)),
            resolverRegistry = com.mindos.commandrouter.resolver.ResolverRegistry(emptyMap())
        )
        val result = r.route(CommandInput("orphan command"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.NoResolverForPattern>(result.failure)
    }

    @Test
    fun trace_containsNormalizedInput() = runTest {
        val r = router(openPattern() to navResolver)
        val result = r.route(CommandInput("  OPEN  Settings  "))
        assertIs<CommandRouterResult.Success>(result)
        assertEquals("open settings", result.trace.normalizedInput)
    }

    @Test
    fun trace_containsPatternAndResolver_onSuccess() = runTest {
        val r = router(openPattern("open-target") to navResolver)
        val result = r.route(CommandInput("open home"))
        assertIs<CommandRouterResult.Success>(result)
        assertEquals(PatternId("open-target"), result.trace.patternId)
        assertEquals(ResolverId("navigation-intent-resolver"), result.trace.resolverId)
        assertNotNull(result.trace.actionId)
    }

    @Test
    fun trace_containsFailureReason_onNoMatch() = runTest {
        val r = router(openPattern() to navResolver)
        val result = r.route(CommandInput("xyz unknown"))
        assertIs<CommandRouterResult.Failure>(result)
        assertNotNull(result.trace.failureReason)
    }

    @Test
    fun determinism_sameInput_sameActionType() = runTest {
        seq = 0
        val r = router(openPattern() to navResolver)
        val r1 = r.route(CommandInput("open settings", source = CommandSource.Test))
        seq = 0
        val r2 = r.route(CommandInput("open settings", source = CommandSource.Test))
        assertIs<CommandRouterResult.Success>(r1)
        assertIs<CommandRouterResult.Success>(r2)
        assertEquals(r1.action.metadata.actionType, r2.action.metadata.actionType)
        assertEquals((r1.action as NavigateAction).targetId, (r2.action as NavigateAction).targetId)
        assertEquals(r1.trace.patternId, r2.trace.patternId)
        assertEquals(r1.trace.normalizedInput, r2.trace.normalizedInput)
    }

    @Test
    fun resolutionFailed_typedFailure_propagated() = runTest {
        val failingResolver = object : com.mindos.commandrouter.resolver.CommandResolver {
            override val id = ResolverId("failing")
            override suspend fun resolve(match: PatternMatch, context: ResolutionContext) =
                ResolutionResult.Failed(ResolutionFailure.Unauthorized("fail-pattern", "not allowed"))
        }
        val r = router(ExactCommandPattern(PatternId("fail-pattern"), "forbidden") to failingResolver)
        val result = r.route(CommandInput("forbidden"))
        assertIs<CommandRouterResult.Failure>(result)
        val failure = result.failure
        assertIs<CommandRouterFailure.ResolutionFailed>(failure)
        assertIs<ResolutionFailure.Unauthorized>(failure.cause)
    }
}

package com.mindos.commandrouter.core

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionIdFactory
import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.actions.TimestampProvider
import com.mindos.commandrouter.executor.ActionExecutionContext
import com.mindos.commandrouter.executor.ActionExecutionResult
import com.mindos.commandrouter.executor.ActionHandlerRegistry
import com.mindos.commandrouter.executor.DefaultActionExecutor
import com.mindos.commandrouter.executor.NavigationActionHandler
import com.mindos.commandrouter.navigation.NavigationCommand
import com.mindos.commandrouter.navigation.NavigationResult
import com.mindos.commandrouter.navigation.NavigationTarget
import com.mindos.commandrouter.navigation.Navigator
import com.mindos.commandrouter.patterns.ExactCommandPattern
import com.mindos.commandrouter.patterns.ParameterizedCommandPattern
import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.patterns.PatternMatch
import com.mindos.commandrouter.patterns.PatternPriority
import com.mindos.commandrouter.resolver.ActionFactory
import com.mindos.commandrouter.resolver.CommandResolver
import com.mindos.commandrouter.resolver.NavigationIntentResolver
import com.mindos.commandrouter.resolver.ResolutionContext
import com.mindos.commandrouter.resolver.ResolutionResult
import com.mindos.commandrouter.resolver.ResolverId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EndToEndTest {

    private var seq = 0
    private val factory = ActionFactory(
        ActionIdFactory { ActionId("e2e-${++seq}") },
        TimestampProvider { 42L }
    )
    private val navResolver = NavigationIntentResolver(factory)

    // Resolver for exact patterns: returns a NavigateAction with a fixed targetId.
    private fun fixedNavResolver(targetId: String): CommandResolver = object : CommandResolver {
        override val id = ResolverId("fixed-nav-$targetId")
        override suspend fun resolve(match: PatternMatch, context: ResolutionContext): ResolutionResult {
            val action = NavigateAction(
                actionId = factory.nextId(),
                metadata = factory.metadata(ActionTypes.Navigate, context.source, context.correlationId),
                targetId = targetId
            )
            return ResolutionResult.Resolved(action, id)
        }
    }

    private val capturedCommands = mutableListOf<NavigationCommand>()
    private val fakeNavigator = object : Navigator {
        override suspend fun navigate(command: NavigationCommand): NavigationResult {
            capturedCommands += command
            return NavigationResult.Success(command.target)
        }
    }

    private val executor = DefaultActionExecutor(
        ActionHandlerRegistry(listOf(NavigationActionHandler(fakeNavigator)))
    )

    // Router with only parameterized patterns (navResolver handles {target})
    private fun router() = CommandRouterBuilder()
        .addPattern(ParameterizedCommandPattern(PatternId("open-target"), "open {target}"), navResolver)
        .addPattern(ParameterizedCommandPattern(PatternId("go-to-target"), "go to {target}"), navResolver)
        .build()

    // Router that includes an exact pattern paired with a fixed resolver — higher priority wins
    private fun routerWithExact() = CommandRouterBuilder()
        .addPattern(ParameterizedCommandPattern(PatternId("open-target"), "open {target}"), navResolver)
        .addPattern(ExactCommandPattern(PatternId("open-settings-exact"), "open settings", PatternPriority(10)),
            fixedNavResolver("settings"))
        .build()

    @Test
    fun openSettings_navigatesToSettings_viaExecutor() = runTest {
        val router = router()
        val result = router.route(CommandInput("open settings", source = CommandSource.Test))
        assertIs<CommandRouterResult.Success>(result)

        val execResult = executor.execute(result.action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Success>(execResult)
        assertEquals(1, capturedCommands.size)
        val nav = capturedCommands.first().target as NavigationTarget.Screen
        assertEquals("settings", nav.id)
    }

    @Test
    fun exactPatternBeatsParameterized_forOpenSettings() = runTest {
        val router = routerWithExact()
        val result = router.route(CommandInput("open settings"))
        assertIs<CommandRouterResult.Success>(result)
        // exact pattern has priority 10, parameterized has 0 → exact wins
        assertEquals(PatternId("open-settings-exact"), result.trace.patternId)
        assertEquals("settings", (result.action as NavigateAction).targetId)
    }

    @Test
    fun goTo_routesToCorrectTarget() = runTest {
        val router = router()
        val result = router.route(CommandInput("go to home screen"))
        assertIs<CommandRouterResult.Success>(result)
        val action = result.action as NavigateAction
        assertEquals("home screen", action.targetId)
    }

    @Test
    fun unknownCommand_executorNotInvoked() = runTest {
        val router = router()
        val countBefore = capturedCommands.size
        val result = router.route(CommandInput("blorp zorp"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.NoPatternMatched>(result.failure)
        assertEquals(countBefore, capturedCommands.size)
    }

    @Test
    fun ambiguousCommand_executorNotInvoked() = runTest {
        val ambigRouter = CommandRouterBuilder()
            .addPattern(ParameterizedCommandPattern(PatternId("pa"), "do {thing}", PatternPriority(0)), navResolver)
            .addPattern(ParameterizedCommandPattern(PatternId("pb"), "do {action}", PatternPriority(0)), navResolver)
            .build()
        val countBefore = capturedCommands.size
        val result = ambigRouter.route(CommandInput("do something"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.AmbiguousCommand>(result.failure)
        assertEquals(countBefore, capturedCommands.size)
    }

    @Test
    fun repeatedCommand_sameNormalizedInput_sameActionType() = runTest {
        val router = router()
        seq = 0
        val r1 = router.route(CommandInput("open home", source = CommandSource.Test))
        seq = 0
        val r2 = router.route(CommandInput("open home", source = CommandSource.Test))
        assertIs<CommandRouterResult.Success>(r1)
        assertIs<CommandRouterResult.Success>(r2)
        assertEquals(r1.action.metadata.actionType, r2.action.metadata.actionType)
        assertEquals(r1.trace.normalizedInput, r2.trace.normalizedInput)
        assertEquals(r1.trace.patternId, r2.trace.patternId)
        assertEquals((r1.action as NavigateAction).targetId, (r2.action as NavigateAction).targetId)
    }

    @Test
    fun trace_hasNoFailureReason_onSuccess() = runTest {
        val router = router()
        val result = router.route(CommandInput("open home"))
        assertIs<CommandRouterResult.Success>(result)
        assertNull(result.trace.failureReason)
    }

    @Test
    fun invalidParameter_typedFailure_propagated() = runTest {
        // A resolver that requires a param the pattern doesn't produce
        val badResolver = object : CommandResolver {
            override val id = ResolverId("bad")
            override suspend fun resolve(match: PatternMatch, context: ResolutionContext) =
                com.mindos.commandrouter.resolver.ResolutionResult.Failed(
                    com.mindos.commandrouter.resolver.ResolutionFailure.Unauthorized("exact-cmd", "not allowed")
                )
        }
        val r = CommandRouterBuilder()
            .addPattern(ExactCommandPattern(PatternId("exact-cmd"), "forbidden"), badResolver)
            .build()
        val result = r.route(CommandInput("forbidden"))
        assertIs<CommandRouterResult.Failure>(result)
        assertIs<CommandRouterFailure.ResolutionFailed>(result.failure)
    }
}

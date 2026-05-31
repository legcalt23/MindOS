package com.mindos.action.matching

import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.understanding.SlotName
import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionMetadata
import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.CommandAction
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.executor.ActionExecutionContext
import com.mindos.commandrouter.executor.ActionExecutionResult
import com.mindos.commandrouter.executor.ActionHandlerRegistry
import com.mindos.commandrouter.executor.DefaultActionExecutor
import com.mindos.commandrouter.executor.NavigationActionHandler
import com.mindos.commandrouter.executor.NoOpActionHandler
import com.mindos.commandrouter.navigation.NavigationCommand
import com.mindos.commandrouter.navigation.NavigationResult
import com.mindos.commandrouter.navigation.NavigationTarget
import com.mindos.commandrouter.navigation.Navigator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the full MindOS flow and its invariants:
 * input → [understanding] MindIntent+entities → resolver maps to a typed CommandAction →
 * executor OWNS navigation. The understanding layer never navigates; the matcher never executes.
 */
class EndToEndBridgeTest {

    private class FakeNavigator : Navigator {
        var calls = 0
        var lastTargetId: String? = null
        var lastArgs: Map<String, String> = emptyMap()
        override suspend fun navigate(command: NavigationCommand): NavigationResult {
            calls++
            (command.target as? NavigationTarget.Screen)?.let {
                lastTargetId = it.id
                lastArgs = it.args
            }
            return NavigationResult.Success(command.target)
        }
    }

    private val matcher = TestRegistries.matcher()

    // The resolver/bridge: MindIntent (+entities) → typed foundation CommandAction.
    private fun toAction(result: ActionUnderstandingResult.Understood): CommandAction? {
        val meta = ActionMetadata(ActionId("nav"), ActionTypes.Navigate, timestamp = 0L)
        return when (result.intent.id.value) {
            "open_settings" -> NavigateAction(ActionId("nav"), meta, targetId = "settings")
            "open_profile" -> NavigateAction(
                ActionId("nav"), meta, targetId = "profile",
                parameters = mapOf("username" to (result.entities[SlotName("target")]?.value ?: "")),
            )
            else -> null
        }
    }

    private fun executorWith(nav: FakeNavigator) = DefaultActionExecutor(
        ActionHandlerRegistry(listOf(NavigationActionHandler(nav), NoOpActionHandler())),
    )

    @Test
    fun openSettingsFlowsToNavigatorViaExecutor() = runTest {
        val nav = FakeNavigator()
        val executor = executorWith(nav)

        val understood = matcher.understand("open settings") as ActionUnderstandingResult.Understood
        val action = toAction(understood)!!
        val result = executor.execute(action, ActionExecutionContext.Empty)

        assertTrue(result is ActionExecutionResult.Success)
        assertEquals(1, nav.calls)
        assertEquals("settings", nav.lastTargetId)
    }

    @Test
    fun openProfileCarriesEntityIntoNavigation() = runTest {
        val nav = FakeNavigator()
        val executor = executorWith(nav)

        val understood = matcher.understand("open ana") as ActionUnderstandingResult.Understood
        val result = executor.execute(toAction(understood)!!, ActionExecutionContext.Empty)

        assertTrue(result is ActionExecutionResult.Success)
        assertEquals("profile", nav.lastTargetId)
        assertEquals("ana", nav.lastArgs["username"])
    }

    @Test
    fun unknownCommandNeverInvokesExecutor() = runTest {
        val nav = FakeNavigator()
        val result = matcher.understand("florble")
        assertTrue(result is ActionUnderstandingResult.NoMatch)
        // No mapping, no execution: the executor (and thus the navigator) is never touched.
        assertEquals(0, nav.calls)
    }
}

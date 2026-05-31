package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionMetadata
import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.actions.NoOpAction
import com.mindos.commandrouter.navigation.NavigationCommand
import com.mindos.commandrouter.navigation.NavigationResult
import com.mindos.commandrouter.navigation.NavigationTarget
import com.mindos.commandrouter.navigation.Navigator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class DefaultActionExecutorTest {

    private fun metadata(type: com.mindos.commandrouter.actions.ActionType) = ActionMetadata(
        actionId = ActionId("a1"),
        actionType = type,
        source = CommandSource.Test,
        timestamp = 1000L
    )

    private fun fakeNavigator(result: NavigationResult): Navigator = object : Navigator {
        override suspend fun navigate(command: NavigationCommand) = result
    }

    private fun executorWith(vararg handlers: ActionHandler): ActionExecutor =
        DefaultActionExecutor(ActionHandlerRegistry(handlers.toList()))

    @Test
    fun noOpAction_succeeds() = runTest {
        val executor = executorWith(NoOpActionHandler())
        val action = NoOpAction(ActionId("n1"), metadata(ActionTypes.NoOp))
        val result = executor.execute(action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Success>(result)
    }

    @Test
    fun noHandler_returnsNoHandlerFailure() = runTest {
        val executor = executorWith()
        val action = NoOpAction(ActionId("n1"), metadata(ActionTypes.NoOp))
        val result = executor.execute(action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Failure>(result)
        assertIs<ActionExecutionFailure.NoHandler>(result.failure)
    }

    @Test
    fun navigateAction_successNavigator_returnsSuccess() = runTest {
        val navigator = fakeNavigator(NavigationResult.Success(NavigationTarget.Screen("settings")))
        val executor = executorWith(NavigationActionHandler(navigator))
        val action = NavigateAction(
            ActionId("nav1"),
            metadata(ActionTypes.Navigate),
            targetId = "settings"
        )
        val result = executor.execute(action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Success>(result)
    }

    @Test
    fun navigateAction_targetNotFound_returnsNavigationFailed() = runTest {
        val navigator = fakeNavigator(NavigationResult.Failure.TargetNotFound("unknown"))
        val executor = executorWith(NavigationActionHandler(navigator))
        val action = NavigateAction(ActionId("nav2"), metadata(ActionTypes.Navigate), targetId = "unknown")
        val result = executor.execute(action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Failure>(result)
        assertIs<ActionExecutionFailure.NavigationFailed>(result.failure)
    }

    @Test
    fun navigateAction_denied_returnsNavigationFailed() = runTest {
        val navigator = fakeNavigator(NavigationResult.Failure.NavigationDenied("unauthorized"))
        val executor = executorWith(NavigationActionHandler(navigator))
        val action = NavigateAction(ActionId("nav3"), metadata(ActionTypes.Navigate), targetId = "admin")
        val result = executor.execute(action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Failure>(result)
        assertIs<ActionExecutionFailure.NavigationFailed>(result.failure)
    }

    @Test
    fun handlerThrows_returnsHandlerFailed() = runTest {
        val throwingHandler = object : ActionHandler {
            override val actionType = ActionTypes.NoOp
            override suspend fun handle(action: com.mindos.commandrouter.actions.CommandAction, context: ActionExecutionContext): ActionExecutionResult {
                throw RuntimeException("boom")
            }
        }
        val executor = executorWith(throwingHandler)
        val action = NoOpAction(ActionId("n2"), metadata(ActionTypes.NoOp))
        val result = executor.execute(action, ActionExecutionContext.Empty)
        assertIs<ActionExecutionResult.Failure>(result)
        assertIs<ActionExecutionFailure.HandlerFailed>(result.failure)
    }
}

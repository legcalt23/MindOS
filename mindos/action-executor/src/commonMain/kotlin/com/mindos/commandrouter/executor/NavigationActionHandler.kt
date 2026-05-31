package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.ActionType
import com.mindos.commandrouter.actions.CommandAction
import com.mindos.commandrouter.actions.NavigateAction
import com.mindos.commandrouter.navigation.NavigationCommand
import com.mindos.commandrouter.navigation.NavigationResult
import com.mindos.commandrouter.navigation.NavigationTarget
import com.mindos.commandrouter.navigation.Navigator
import com.mindos.commandrouter.navigation.RouteParameters

class NavigationActionHandler(private val navigator: Navigator) : ActionHandler {

    override val actionType: ActionType = ActionTypes.Navigate

    override suspend fun handle(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult {
        if (action !is NavigateAction) {
            return ActionExecutionResult.Failure(ActionExecutionFailure.InvalidAction(action.actionId))
        }

        val target = NavigationTarget.Screen(action.targetId, action.parameters)
        val command = NavigationCommand(
            target = target,
            parameters = RouteParameters(action.parameters),
            commandId = action.actionId.value
        )

        return when (val navResult = navigator.navigate(command)) {
            is NavigationResult.Success ->
                ActionExecutionResult.Success(action.actionId)

            is NavigationResult.Failure.TargetNotFound ->
                ActionExecutionResult.Failure(ActionExecutionFailure.NavigationFailed("Target not found: ${navResult.targetId}"))

            is NavigationResult.Failure.NavigationDenied ->
                ActionExecutionResult.Failure(ActionExecutionFailure.NavigationFailed("Denied: ${navResult.reason}"))

            is NavigationResult.Failure.NavigationError ->
                ActionExecutionResult.Failure(ActionExecutionFailure.NavigationFailed(navResult.message))
        }
    }
}

package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionType

data class ActionExecutionContext(
    val extra: Map<String, String> = emptyMap()
) {
    companion object {
        val Empty = ActionExecutionContext()
    }
}

sealed interface ActionExecutionFailure {
    data class NoHandler(val actionType: ActionType) : ActionExecutionFailure
    data class HandlerFailed(val actionType: ActionType, val message: String) : ActionExecutionFailure
    data class NavigationFailed(val reason: String) : ActionExecutionFailure
    data class Unauthorized(val actionId: ActionId) : ActionExecutionFailure
    data class InvalidAction(val actionId: ActionId) : ActionExecutionFailure
    data object Cancelled : ActionExecutionFailure
}

sealed interface ActionExecutionResult {
    data class Success(val actionId: ActionId) : ActionExecutionResult
    data class Failure(val failure: ActionExecutionFailure) : ActionExecutionResult
}

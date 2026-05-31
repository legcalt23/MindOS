package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.ActionTypes
import com.mindos.commandrouter.actions.ActionType
import com.mindos.commandrouter.actions.CommandAction

class NoOpActionHandler : ActionHandler {
    override val actionType: ActionType = ActionTypes.NoOp

    override suspend fun handle(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult =
        ActionExecutionResult.Success(action.actionId)
}

package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.ActionType
import com.mindos.commandrouter.actions.CommandAction

interface ActionHandler {
    val actionType: ActionType
    suspend fun handle(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult
}

package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.CommandAction

interface ActionExecutor {
    suspend fun execute(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult
}

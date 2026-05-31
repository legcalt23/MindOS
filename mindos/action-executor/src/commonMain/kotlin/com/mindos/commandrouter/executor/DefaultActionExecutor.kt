package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.CommandAction
import kotlinx.coroutines.CancellationException

class DefaultActionExecutor(
    private val registry: ActionHandlerRegistry
) : ActionExecutor {

    override suspend fun execute(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult {
        val handler = registry.getHandler(action.metadata.actionType)
            ?: return ActionExecutionResult.Failure(
                ActionExecutionFailure.NoHandler(action.metadata.actionType)
            )

        return try {
            handler.handle(action, context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ActionExecutionResult.Failure(
                ActionExecutionFailure.HandlerFailed(
                    action.metadata.actionType,
                    e.message ?: "unknown error"
                )
            )
        }
    }
}

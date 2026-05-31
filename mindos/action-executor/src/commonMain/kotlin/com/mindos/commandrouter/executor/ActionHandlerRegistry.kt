package com.mindos.commandrouter.executor

import com.mindos.commandrouter.actions.ActionType

class ActionHandlerRegistry(handlers: List<ActionHandler>) {
    private val map: Map<ActionType, ActionHandler> = handlers.associateBy { it.actionType }

    fun getHandler(actionType: ActionType): ActionHandler? = map[actionType]
}

package com.mindos.commandrouter.actions

import kotlinx.serialization.Serializable

@Serializable
data class ActionMetadata(
    val actionId: ActionId,
    val actionType: ActionType,
    val correlationId: CorrelationId? = null,
    val source: CommandSource = CommandSource.UserInput,
    val timestamp: Long
)

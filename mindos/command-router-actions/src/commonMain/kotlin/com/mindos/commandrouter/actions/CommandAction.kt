package com.mindos.commandrouter.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface CommandAction {
    val actionId: ActionId
    val metadata: ActionMetadata
}

@Serializable
@SerialName("noop")
data class NoOpAction(
    override val actionId: ActionId,
    override val metadata: ActionMetadata
) : CommandAction

@Serializable
@SerialName("unknown")
data class UnknownAction(
    override val actionId: ActionId,
    override val metadata: ActionMetadata,
    val rawInput: String
) : CommandAction

@Serializable
@SerialName("system")
data class SystemAction(
    override val actionId: ActionId,
    override val metadata: ActionMetadata,
    val operation: String,
    val payload: Map<String, String> = emptyMap()
) : CommandAction

@Serializable
@SerialName("navigate")
data class NavigateAction(
    override val actionId: ActionId,
    override val metadata: ActionMetadata,
    val targetId: String,
    val parameters: Map<String, String> = emptyMap()
) : CommandAction

object ActionTypes {
    val NoOp = ActionType("noop")
    val Unknown = ActionType("unknown")
    val System = ActionType("system")
    val Navigate = ActionType("navigate")
}

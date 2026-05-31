package com.mindos.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RouterActionDto {
    val schemaVersion: Int
    val requestId: String
}

@Serializable
@SerialName("open_profile")
data class OpenProfileActionDto(
    override val requestId: String,
    val username: String,
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterActionDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(username.isNotBlank()) { "username must not be blank." }
    }
}

@Serializable
@SerialName("open_story")
data class OpenStoryActionDto(
    override val requestId: String,
    val storyRef: String,
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterActionDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(storyRef.isNotBlank()) { "storyRef must not be blank." }
    }
}

@Serializable
@SerialName("open_chat")
data class OpenChatActionDto(
    override val requestId: String,
    val chatRef: String,
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterActionDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(chatRef.isNotBlank()) { "chatRef must not be blank." }
    }
}

@Serializable
@SerialName("open_messages")
data class OpenMessagesActionDto(
    override val requestId: String,
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterActionDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
    }
}

@Serializable
@SerialName("open_settings")
data class OpenSettingsActionDto(
    override val requestId: String,
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterActionDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
    }
}

val RouterActionDto.actionType: String
    get() = when (this) {
        is OpenProfileActionDto -> "open_profile"
        is OpenStoryActionDto -> "open_story"
        is OpenChatActionDto -> "open_chat"
        is OpenMessagesActionDto -> "open_messages"
        is OpenSettingsActionDto -> "open_settings"
    }

package com.mindos.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EntityDto {
    val schemaVersion: Int
    val id: String
}

@Serializable
@SerialName("user")
data class UserEntityDto(
    override val id: String,
    val username: String,
    val displayName: String? = null,
    override val schemaVersion: Int = ContractSchema.Current,
) : EntityDto {
    init {
        require(id.isNotBlank()) { "id must not be blank." }
        require(username.isNotBlank()) { "username must not be blank." }
    }
}

@Serializable
@SerialName("story")
data class StoryEntityDto(
    override val id: String,
    val authorUsername: String? = null,
    override val schemaVersion: Int = ContractSchema.Current,
) : EntityDto {
    init {
        require(id.isNotBlank()) { "id must not be blank." }
        require(authorUsername == null || authorUsername.isNotBlank()) { "authorUsername must not be blank when present." }
    }
}

@Serializable
@SerialName("chat")
data class ChatEntityDto(
    override val id: String,
    val title: String? = null,
    override val schemaVersion: Int = ContractSchema.Current,
) : EntityDto {
    init {
        require(id.isNotBlank()) { "id must not be blank." }
        require(title == null || title.isNotBlank()) { "title must not be blank when present." }
    }
}

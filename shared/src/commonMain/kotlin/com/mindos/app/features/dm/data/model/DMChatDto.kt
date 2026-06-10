package com.mindos.app.features.dm.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DMChatDto(
    val participants: List<String> = emptyList(),
    val participantKey: String = "",
    val lastMessage: String? = null,
    val lastMessageType: String? = null,
    val lastTimestamp: Long = 0L,
    val updatedAt: Long = 0L,
    val lastReadAtByUser: Map<String, Long> = emptyMap(),
)

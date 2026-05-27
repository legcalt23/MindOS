package com.example.mindos.features.dm.domain.model

data class DMChat(
    val id: String,
    val participants: List<String>,
    val participantKey: String,
    val lastMessage: String?,
    val lastMessageType: DMMessageType?,
    val lastTimestamp: Long,
    val updatedAt: Long,
    val lastReadAtByUser: Map<String, Long>,
)

enum class MessageStatus {
    Sending,
    Sent,
    Read,
    Failed
}

data class DMMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val type: DMMessageType,
    val status: MessageStatus,
    val text: String?,
    val attachmentKind: String?,
    val attachmentUrl: String?,
    val attachmentMimeType: String?,
    val attachmentName: String?,
    val attachmentSizeBytes: Long?,
    val thumbnailUrl: String?,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val createdAt: Long,
)

enum class DMMessageType {
    Text,
    Image,
    Video,
    Audio,
    File;
    
    companion object {
        fun fromString(value: String?): DMMessageType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: Text
    }
}

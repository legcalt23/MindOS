package com.example.neuv3.features.dm.data.mapper

import com.example.neuv3.features.dm.data.model.DMChatDto
import com.example.neuv3.features.dm.data.model.DMMessageDto
import com.example.neuv3.features.dm.domain.model.DMChat
import com.example.neuv3.features.dm.domain.model.DMMessage
import com.example.neuv3.features.dm.domain.model.DMMessageType
import com.example.neuv3.features.dm.domain.model.MessageStatus

fun DMChatDto.toDomain(id: String): DMChat {
    return DMChat(
        id = id,
        participants = participants,
        participantKey = participantKey,
        lastMessage = lastMessage,
        lastMessageType = lastMessageType?.let { DMMessageType.fromString(it) },
        lastTimestamp = lastTimestamp,
        updatedAt = updatedAt,
        lastReadAtByUser = lastReadAtByUser,
    )
}

fun DMMessageDto.toDomain(id: String): DMMessage {
    val messageType = DMMessageType.fromString(type)
    val parsedStatus = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.Sent)
    
    return DMMessage(
        id = id,
        chatId = chatId,
        senderId = senderId,
        type = messageType,
        status = parsedStatus,
        text = text,
        attachmentKind = attachmentKind,
        attachmentUrl = attachmentUrl,
        attachmentMimeType = attachmentMimeType,
        attachmentName = attachmentName,
        attachmentSizeBytes = attachmentSizeBytes,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        width = width,
        height = height,
        createdAt = createdAt,
    )
}

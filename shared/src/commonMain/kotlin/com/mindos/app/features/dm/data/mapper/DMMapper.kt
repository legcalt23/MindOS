package com.mindos.app.features.dm.data.mapper

import com.mindos.app.features.dm.data.model.DMChatDto
import com.mindos.app.features.dm.data.model.DMMessageDto
import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.model.DMMessageType
import com.mindos.app.features.dm.domain.model.MessageStatus

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

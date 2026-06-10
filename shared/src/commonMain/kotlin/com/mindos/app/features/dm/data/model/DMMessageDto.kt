package com.mindos.app.features.dm.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DMMessageDto(
    val chatId: String = "",
    val senderId: String = "",
    val type: String = "Text",
    val status: String = "Sent",
    val text: String? = null,
    val attachmentKind: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentName: String? = null,
    val attachmentSizeBytes: Long? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val createdAt: Long = 0L,
)

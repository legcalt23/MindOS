package com.mindos.app.features.social.domain.model

import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId

enum class InAppNotificationType {
    Observed,
}

data class InAppNotification(
    val notificationId: String,
    val recipientUserId: UserId,
    val actorUserId: UserId,
    val actorUsername: String,
    val actorDisplayName: String,
    val actorMainPhotoUrl: String?,
    val actorPhotoStoragePath: String?,
    val actorPhotoThumbnailStoragePath: String?,
    val type: InAppNotificationType,
    val timestamp: Timestamp,
    val targetUserId: UserId?,
    val isRead: Boolean = false,
)


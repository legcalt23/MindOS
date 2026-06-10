package com.mindos.app.features.social.domain.repository

import com.mindos.app.features.social.domain.model.InAppNotification
import com.mindos.app.features.social.domain.valueobject.UserId
import com.mindos.app.features.social.shared.util.DomainResult

sealed interface ObserveUserResult {
    data object Created : ObserveUserResult
    data object AlreadyObserving : ObserveUserResult
}

interface ObservationRepository {
    suspend fun observe(observerUserId: UserId, observedUserId: UserId): DomainResult<ObserveUserResult>
    suspend fun isObserving(observerUserId: UserId, observedUserId: UserId): DomainResult<Boolean>
    suspend fun observersCount(userId: UserId): DomainResult<Int?>
}

interface InAppNotificationRepository {
    suspend fun listForRecipient(
        recipientUserId: UserId,
        limit: Int = 50,
    ): DomainResult<List<InAppNotification>>
}


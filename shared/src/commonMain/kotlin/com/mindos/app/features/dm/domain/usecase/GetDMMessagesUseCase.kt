package com.mindos.app.features.dm.domain.usecase

import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.repository.DMRepository
import kotlinx.coroutines.flow.Flow

class GetDMMessagesUseCase(
    private val repository: DMRepository
) {
    operator fun invoke(chatId: String): Flow<List<DMMessage>> {
        require(chatId.isNotBlank()) { "Chat ID must not be blank" }
        return repository.getChatMessages(chatId)
    }
}

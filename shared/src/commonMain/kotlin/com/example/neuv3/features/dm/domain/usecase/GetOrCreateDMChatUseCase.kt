package com.example.neuv3.features.dm.domain.usecase

import com.example.neuv3.features.dm.domain.model.DMChat
import com.example.neuv3.features.dm.domain.model.DMConversationIds
import com.example.neuv3.features.dm.domain.repository.DMRepository

class GetOrCreateDMChatUseCase(
    private val repository: DMRepository
) {
    suspend operator fun invoke(user1Id: String, user2Id: String): DMChat {
        DMConversationIds.participants(user1Id, user2Id)
        return repository.getOrCreateChat(user1Id, user2Id)
    }
}

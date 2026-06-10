package com.mindos.app.features.dm.domain.usecase

import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.repository.DMRepository
import kotlinx.coroutines.flow.Flow

class GetDMConversationsUseCase(
    private val repository: DMRepository
) {
    operator fun invoke(userId: String): Flow<List<DMChat>> {
        return repository.getUserChats(userId)
    }
}

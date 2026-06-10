package com.mindos.app.features.dm.presentation

import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.model.MessageStatus
import com.mindos.app.features.dm.domain.repository.DMMessageDraft
import com.mindos.app.features.dm.domain.repository.DMRepository
import com.mindos.app.features.dm.domain.repository.DMUser
import com.mindos.app.features.dm.domain.repository.MediaUploadResult
import com.mindos.app.features.waves.data.DisabledWaveRequestRepository
import com.mindos.app.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DMStateHolderWaveGateTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun existingChatRouteLoadsEvenWhenWaveGateIsClosed() = runTest {
        val repository = FakeDMRepository()
        val holder = DMStateHolder(
            repository = repository,
            waveRequestService = WaveRequestService(DisabledWaveRequestRepository()),
            scope = this,
        )

        holder.resolveAndLoadChat(
            myUserId = "current",
            peerUserId = "other_user",
            inputChatId = "chat_existing",
        )
        advanceUntilIdle()

        assertEquals("chat_existing", holder.resolvedChatId.value)
        assertEquals(listOf("chat_existing"), repository.messageSubscriptions)
        assertNull(holder.chatAccess.value)
    }

    private class FakeDMRepository : DMRepository {
        val messageSubscriptions = mutableListOf<String>()

        override fun getUserChats(userId: String): Flow<List<DMChat>> = flowOf(emptyList())

        override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat =
            error("Existing chat route must not create a chat")

        override fun getChatMessages(chatId: String): Flow<List<DMMessage>> {
            messageSubscriptions += chatId
            return flowOf(emptyList())
        }

        override suspend fun getUsersByIds(
            currentUserId: String,
            userIds: Collection<String>,
        ): Map<String, DMUser> = userIds.associateWith { userId ->
            DMUser(userId = userId, username = userId, displayName = null, avatarUrl = null)
        }

        override suspend fun createMessageRecord(
            chatId: String,
            messageId: String,
            senderId: String,
            draft: DMMessageDraft,
            status: MessageStatus,
        ) = Unit

        override suspend fun uploadMessageMedia(
            chatId: String,
            messageId: String,
            draft: DMMessageDraft,
        ): MediaUploadResult = error("Not used")

        override suspend fun updateMessage(
            chatId: String,
            messageId: String,
            updates: Map<String, Any?>,
        ) = Unit

        override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> = emptyList()
    }
}

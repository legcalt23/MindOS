package com.example.mindos.features.dm.presentation

import com.example.mindos.features.dm.domain.model.DMChat
import com.example.mindos.features.dm.domain.model.DMMessage
import com.example.mindos.features.dm.domain.model.MessageStatus
import com.example.mindos.features.dm.domain.repository.DMMessageDraft
import com.example.mindos.features.dm.domain.repository.DMRepository
import com.example.mindos.features.dm.domain.repository.DMRepositoryError
import com.example.mindos.features.dm.domain.repository.DMRepositoryException
import com.example.mindos.features.dm.domain.repository.DMUser
import com.example.mindos.features.dm.domain.repository.MediaUploadResult
import com.example.mindos.features.waves.data.InMemoryWaveRequestRepository
import com.example.mindos.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DMListViewModelTest {
    @Test
    fun notAuthenticatedDiscoveryFailureShowsActionableMessage() = runTest {
        val repository = FakeDMRepository(
            discoveryFailure = DMRepositoryException(DMRepositoryError.NotAuthenticated),
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        val status = assertIs<DMListStatus.Error>(viewModel.uiState.value.status)
        assertEquals(DMRepositoryError.NotAuthenticated.userMessage, status.message)
        assertEquals(listOf("current"), repository.discoveryRequests)
    }

    @Test
    fun discoveryFailureKeepsExistingChatsVisible() = runTest {
        val repository = FakeDMRepository(
            discoveryFailure = DMRepositoryException(DMRepositoryError.NotAuthenticated),
            chats = listOf(
                DMChat(
                    id = "chat_1",
                    participants = listOf("current", "other_user"),
                    participantKey = "chat_1",
                    lastMessage = "Ultimo mensaje",
                    lastMessageType = null,
                    lastTimestamp = 10L,
                    updatedAt = 10L,
                    lastReadAtByUser = emptyMap(),
                )
            ),
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        assertIs<DMListStatus.Ready>(viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.items.size)
        val item = viewModel.uiState.value.items.single()
        assertEquals("chat_1", item.chatId)
        assertEquals("Usuario desconocido", item.title)
    }

    @Test
    fun permissionDeniedDiscoveryFailureShowsPermissionMessage() = runTest {
        val error = DMRepositoryError.PermissionDenied(
            operation = "firestore_list_dm_discovery",
            path = "profile_visuals",
            causeMessage = "Missing or insufficient permissions.",
        )
        val repository = FakeDMRepository(discoveryFailure = DMRepositoryException(error))

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        val status = assertIs<DMListStatus.Error>(viewModel.uiState.value.status)
        assertEquals(error.userMessage, status.message)
    }

    @Test
    fun chatUsesResolvedOtherUsernameInsteadOfTechnicalId() = runTest {
        val repository = FakeDMRepository(
            chats = listOf(validChat()),
            profilesById = mapOf(
                "other_user" to DMUser(
                    userId = "other_user",
                    username = "waves_b",
                    displayName = "Wave B",
                    avatarUrl = null,
                )
            ),
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.single()
        assertEquals("waves_b", item.title)
        assertEquals("waves_b", item.username)
        assertEquals(listOf(setOf("other_user")), repository.profileRequests)
    }

    @Test
    fun chatShowsLoadingPlaceholderWhileProfileIsResolving() = runTest {
        val profileGate = CompletableDeferred<Unit>()
        val repository = FakeDMRepository(
            chats = listOf(validChat()),
            profilesById = mapOf(
                "other_user" to DMUser(
                    userId = "other_user",
                    username = "waves_b",
                    displayName = "Wave B",
                    avatarUrl = null,
                )
            ),
            beforeProfileResolution = { profileGate.await() },
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        runCurrent()

        assertEquals("Cargando...", viewModel.uiState.value.items.single().title)

        profileGate.complete(Unit)
        advanceUntilIdle()

        assertEquals("waves_b", viewModel.uiState.value.items.single().title)
    }

    @Test
    fun chatFallsBackCleanlyWhenUsernameIsMissing() = runTest {
        val repository = FakeDMRepository(
            chats = listOf(validChat()),
            profilesById = mapOf(
                "other_user" to DMUser(
                    userId = "other_user",
                    username = null,
                    displayName = "Wave B",
                    avatarUrl = null,
                )
            ),
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.single()
        assertEquals("Usuario desconocido", item.title)
        assertEquals(null, item.username)
    }

    @Test
    fun chatWithMissingCurrentUserIsIgnoredWithoutCrash() = runTest {
        val repository = FakeDMRepository(
            chats = listOf(
                validChat(participants = listOf("someone_else", "other_user"))
            ),
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        assertIs<DMListStatus.Empty>(viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun chatWithMoreThanTwoParticipantsIsIgnoredWithoutCrash() = runTest {
        val repository = FakeDMRepository(
            chats = listOf(
                validChat(participants = listOf("current", "other_user", "third_user"))
            ),
        )

        val viewModel = DMListViewModel(repository, allowAllWaveRequestService(), currentUserId = "current", scope = this)
        advanceUntilIdle()

        assertIs<DMListStatus.Empty>(viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    private class FakeDMRepository(
        private val discoveryFailure: Throwable? = null,
        private val chats: List<DMChat> = emptyList(),
        private val profilesById: Map<String, DMUser> = emptyMap(),
        private val availableUsers: List<DMUser> = emptyList(),
        private val beforeProfileResolution: suspend () -> Unit = {},
    ) : DMRepository {
        val discoveryRequests = mutableListOf<String>()
        val profileRequests = mutableListOf<Set<String>>()

        override fun getUserChats(userId: String): Flow<List<DMChat>> = flowOf(chats)

        override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat =
            error("Not used")

        override fun getChatMessages(chatId: String): Flow<List<DMMessage>> = flowOf(emptyList())

        override suspend fun getUsersByIds(
            currentUserId: String,
            userIds: Collection<String>,
        ): Map<String, DMUser> {
            val requested = userIds.toSet()
            profileRequests += requested
            beforeProfileResolution()
            return profilesById.filterKeys { it in requested }
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

        override suspend fun updateMessage(chatId: String, messageId: String, updates: Map<String, Any?>) = Unit

        override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> {
            discoveryRequests += currentUserId
            discoveryFailure?.let { throw it }
            return availableUsers
        }
    }

    private fun validChat(
        participants: List<String> = listOf("current", "other_user"),
    ): DMChat =
        DMChat(
            id = "chat_1",
            participants = participants,
            participantKey = "chat_1",
            lastMessage = "Ultimo mensaje",
            lastMessageType = null,
            lastTimestamp = 10L,
            updatedAt = 10L,
            lastReadAtByUser = emptyMap(),
        )

    private fun allowAllWaveRequestService(): WaveRequestService =
        WaveRequestService(InMemoryWaveRequestRepository())
}

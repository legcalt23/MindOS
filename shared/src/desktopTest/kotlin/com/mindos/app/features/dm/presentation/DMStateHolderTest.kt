package com.mindos.app.features.dm.presentation

import com.mindos.app.fakes.InMemoryDMRepository
import com.mindos.app.features.dm.domain.model.DMConversationIds
import com.mindos.app.features.dm.domain.model.DMMessageType
import com.mindos.app.features.dm.domain.repository.DMUser
import com.mindos.app.features.waves.data.DefaultWaveRequestIdGenerator
import com.mindos.app.features.waves.data.InMemoryWaveRequestRepository
import com.mindos.app.features.waves.data.WaveRequestIdGenerator
import com.mindos.app.features.waves.data.WaveTimeProvider
import com.mindos.app.features.waves.domain.model.WaveRelationshipStatus
import com.mindos.app.features.waves.domain.repository.WaveRequestRepository
import com.mindos.app.features.waves.domain.service.WaveChatAccessResult
import com.mindos.app.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Coverage for [DMStateHolder] — orchestrates DM lists, chat resolution,
 * messages and wave-chat access checks across multiple use cases.
 *
 * Focus areas:
 *  - loadChats publishes the repository's chat list and triggers availableUsers fetch
 *  - openOrCreateChat returns existing chat id; fails when wave access is denied
 *  - resolveAndLoadChat cancels in-flight messages and rebinds to the new chat
 *  - resolveAndLoadChat surfaces a Denied access result via chatAccess StateFlow
 *  - sendMessage dispatches the use case and invokes onComplete with the result
 *  - loadAvailableUsers swallows errors silently (no UI crash)
 */
class DMStateHolderTest {

    private val alice = "uid_alice"
    private val bob = "uid_bob"

    private val knownUsers = mapOf(
        bob to DMUser(userId = bob, username = "bob", displayName = "Bob", avatarUrl = null),
    )

    // ── loadChats ────────────────────────────────────────────────────────

    @Test
    fun loadChats_publishesAvailableUsersFromDirectory() = runTest {
        val repo = InMemoryDMRepositoryWithDirectory(directoryUsers = listOf(knownUsers.getValue(bob)))
        val holder = newHolder(repo)

        holder.loadChats(alice)
        runCurrent()

        val users = holder.availableUsers.first()
        assertEquals(1, users.size)
        assertEquals(bob, users.first().userId)
        coroutineContext.cancelChildren()
    }

    // ── openOrCreateChat ─────────────────────────────────────────────────

    @Test
    fun openOrCreateChat_succeeds_whenWaveAccessAllowed() = runTest {
        val waveRepo = StaticWaveRequestRepository(canChat = true)
        val holder = newHolder(InMemoryDMRepository(knownUsers), waveRepo = waveRepo)

        val chatId = holder.openOrCreateChat(alice, bob)

        assertTrue(chatId.isNotBlank())
    }

    @Test
    fun openOrCreateChat_throws_whenWaveAccessDenied() = runTest {
        val waveRepo = StaticWaveRequestRepository(canChat = false)
        val holder = newHolder(InMemoryDMRepository(knownUsers), waveRepo = waveRepo)

        try {
            holder.openOrCreateChat(alice, bob)
            fail("openOrCreateChat must throw when wave access is denied")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message?.contains("Wave chat is not authorized") == true)
        }
    }

    // ── resolveAndLoadChat ───────────────────────────────────────────────

    @Test
    fun resolveAndLoadChat_publishesAllowedAccess_andResolvesChatId() = runTest {
        val waveRepo = StaticWaveRequestRepository(canChat = true)
        val holder = newHolder(InMemoryDMRepository(knownUsers), waveRepo = waveRepo)

        holder.resolveAndLoadChat(alice, peerUserId = bob, inputChatId = null)
        advanceUntilIdle()

        assertIs<WaveChatAccessResult.Allowed>(holder.chatAccess.first())
        assertNotNull(holder.resolvedChatId.first(), "Chat id must resolve when access is allowed")
        coroutineContext.cancelChildren()
    }

    @Test
    fun resolveAndLoadChat_surfacesDeniedAccess_andDoesNotResolveChat() = runTest {
        val waveRepo = StaticWaveRequestRepository(canChat = false)
        val holder = newHolder(InMemoryDMRepository(knownUsers), waveRepo = waveRepo)

        holder.resolveAndLoadChat(alice, peerUserId = bob, inputChatId = null)
        advanceUntilIdle()

        val access = holder.chatAccess.first()
        assertIs<WaveChatAccessResult.Denied>(access)
        assertNull(holder.resolvedChatId.first(), "Denied access must NOT resolve a chat id")
        coroutineContext.cancelChildren()
    }

    @Test
    fun resolveAndLoadChat_withExplicitChatId_skipsCreation() = runTest {
        val waveRepo = StaticWaveRequestRepository(canChat = true)
        val holder = newHolder(InMemoryDMRepository(knownUsers), waveRepo = waveRepo)

        holder.resolveAndLoadChat(alice, peerUserId = bob, inputChatId = "explicit_chat_id")
        advanceUntilIdle()

        assertEquals("explicit_chat_id", holder.resolvedChatId.first())
        coroutineContext.cancelChildren()
    }

    @Test
    fun resolveAndLoadChat_normalizesLiteralNullPeerId() = runTest {
        val waveRepo = StaticWaveRequestRepository(canChat = true)
        val holder = newHolder(InMemoryDMRepository(knownUsers), waveRepo = waveRepo)

        // "null" string and empty are treated as no-peer.
        holder.resolveAndLoadChat(alice, peerUserId = "null", inputChatId = null)
        advanceUntilIdle()
        assertNull(holder.resolvedChatId.first(), "Literal 'null' must not be treated as a peer id")

        holder.resolveAndLoadChat(alice, peerUserId = "  ", inputChatId = null)
        advanceUntilIdle()
        assertNull(holder.resolvedChatId.first(), "Blank peer must not be treated as a peer id")
        coroutineContext.cancelChildren()
    }

    // ── sendMessage ──────────────────────────────────────────────────────

    @Test
    fun sendMessage_text_invokesOnCompleteWithSuccess() = runTest {
        val repo = InMemoryDMRepository(knownUsers)
        val holder = newHolder(repo)
        // SendDMMessageUseCase requires the canonical chat id format
        // "dm1_<lenA>_<idA>_<lenB>_<idB>" — using the legacy underscore-joined
        // id would fail the use case's pre-check and surface as Result.failure.
        val canonicalChatId = DMConversationIds.chatId(alice, bob)

        var result: Result<Unit>? = null
        holder.sendMessage(
            chatId = canonicalChatId,
            senderId = alice,
            type = DMMessageType.Text,
            text = "hola",
            onComplete = { result = it },
        )
        advanceUntilIdle()

        assertNotNull(result)
        assertTrue(result!!.isSuccess, "sendMessage must complete successfully for valid text")
        coroutineContext.cancelChildren()
    }

    // ── loadAvailableUsers error path ────────────────────────────────────

    @Test
    fun loadAvailableUsers_swallowsRepositoryException() = runTest {
        // An error here must NOT crash the state holder — UI just sees an empty list.
        val repo = ThrowingDirectoryRepository()
        val holder = newHolder(repo)

        holder.loadAvailableUsers(alice)
        advanceUntilIdle()

        assertEquals(emptyList(), holder.availableUsers.first())
        coroutineContext.cancelChildren()
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun TestScope.newHolder(
        repo: com.mindos.app.features.dm.domain.repository.DMRepository,
        waveRepo: WaveRequestRepository = InMemoryWaveRequestRepository(
            idGenerator = DefaultWaveRequestIdGenerator(),
            timeProvider = FixedWaveTimeProvider(),
        ),
    ): DMStateHolder = DMStateHolder(
        repository = repo,
        waveRequestService = WaveRequestService(waveRepo),
        // The holder launches infinite Flow-collect jobs. Use the test scope so
        // advanceUntilIdle picks them up; each test ends with cancelChildren()
        // to let runTest's tear-down see a clean scope.
        scope = this,
    )

    private class FixedWaveTimeProvider : WaveTimeProvider {
        override fun nowMillis(): Long = 1_000L
    }

    /**
     * Wave repository that hardcodes [canUsersChat] and returns a stable
     * relationship status. Avoids the choreography of sending + accepting a
     * request just to flip the access bit in tests.
     */
    private class StaticWaveRequestRepository(
        private val canChat: Boolean,
    ) : WaveRequestRepository {
        override fun observePendingRequestsReceived(userId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.mindos.app.features.waves.domain.model.WaveRequest>>()
        override fun observePendingRequestCountReceived(userId: String) = kotlinx.coroutines.flow.emptyFlow<Int>()
        override suspend fun sendRequest(fromUserId: String, toUserId: String) = error("not used")
        override suspend fun acceptRequest(requestId: String, actingUserId: String) = error("not used")
        override suspend fun rejectRequest(requestId: String, actingUserId: String) = error("not used")
        override suspend fun cancelRequest(requestId: String, actingUserId: String) = error("not used")
        override suspend fun canUsersChat(userIdA: String, userIdB: String): Boolean = canChat
        override suspend fun getRelationshipStatus(
            userIdA: String,
            userIdB: String,
        ): WaveRelationshipStatus = WaveRelationshipStatus.None
    }

    private class InMemoryDMRepositoryWithDirectory(
        private val directoryUsers: List<DMUser>,
    ) : com.mindos.app.features.dm.domain.repository.DMRepository {
        private val delegate = InMemoryDMRepository()
        override fun getUserChats(userId: String) = delegate.getUserChats(userId)
        override suspend fun getOrCreateChat(user1Id: String, user2Id: String) =
            delegate.getOrCreateChat(user1Id, user2Id)
        override fun getChatMessages(chatId: String) = delegate.getChatMessages(chatId)
        override suspend fun getUsersByIds(currentUserId: String, userIds: Collection<String>) =
            delegate.getUsersByIds(currentUserId, userIds)
        override suspend fun createMessageRecord(
            chatId: String,
            messageId: String,
            senderId: String,
            draft: com.mindos.app.features.dm.domain.repository.DMMessageDraft,
            status: com.mindos.app.features.dm.domain.model.MessageStatus,
        ) = delegate.createMessageRecord(chatId, messageId, senderId, draft, status)
        override suspend fun uploadMessageMedia(
            chatId: String,
            messageId: String,
            draft: com.mindos.app.features.dm.domain.repository.DMMessageDraft,
        ) = delegate.uploadMessageMedia(chatId, messageId, draft)
        override suspend fun updateMessage(
            chatId: String,
            messageId: String,
            updates: Map<String, Any?>,
        ) = delegate.updateMessage(chatId, messageId, updates)
        override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> = directoryUsers
    }

    private class ThrowingDirectoryRepository : com.mindos.app.features.dm.domain.repository.DMRepository {
        private val delegate = InMemoryDMRepository()
        override fun getUserChats(userId: String) = delegate.getUserChats(userId)
        override suspend fun getOrCreateChat(user1Id: String, user2Id: String) =
            delegate.getOrCreateChat(user1Id, user2Id)
        override fun getChatMessages(chatId: String) = delegate.getChatMessages(chatId)
        override suspend fun getUsersByIds(currentUserId: String, userIds: Collection<String>) = emptyMap<String, DMUser>()
        override suspend fun createMessageRecord(
            chatId: String,
            messageId: String,
            senderId: String,
            draft: com.mindos.app.features.dm.domain.repository.DMMessageDraft,
            status: com.mindos.app.features.dm.domain.model.MessageStatus,
        ) = delegate.createMessageRecord(chatId, messageId, senderId, draft, status)
        override suspend fun uploadMessageMedia(
            chatId: String,
            messageId: String,
            draft: com.mindos.app.features.dm.domain.repository.DMMessageDraft,
        ) = delegate.uploadMessageMedia(chatId, messageId, draft)
        override suspend fun updateMessage(
            chatId: String,
            messageId: String,
            updates: Map<String, Any?>,
        ) = delegate.updateMessage(chatId, messageId, updates)
        override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> =
            throw RuntimeException("directory unavailable")
    }
}

package com.example.mindos.core.di

import androidx.compose.runtime.mutableStateOf
import com.example.mindos.features.dm.domain.model.DMChat
import com.example.mindos.features.dm.domain.model.DMMessage
import com.example.mindos.features.dm.domain.model.MessageStatus
import com.example.mindos.features.dm.domain.repository.DMMessageDraft
import com.example.mindos.features.dm.domain.repository.DMRepository
import com.example.mindos.features.dm.domain.repository.DMUser
import com.example.mindos.features.dm.domain.repository.MediaUploadResult
import com.example.mindos.features.profile.data.InMemoryMindOSProfileRepository
import com.example.mindos.features.profile.data.InMemoryProfileObserverRepository
import com.example.mindos.features.profile.data.InMemoryProfilePresenceRepository
import com.example.mindos.features.social.domain.feed.CreateGlobalFeedPostRequest
import com.example.mindos.features.social.domain.feed.GlobalFeedPost
import com.example.mindos.features.social.domain.feed.GlobalFeedRepository
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.shared.util.AppClock
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.wallet.data.WalletApiClient
import com.example.mindos.features.wallet.domain.model.SandboxPurchasePending
import com.example.mindos.features.wallet.domain.model.WalletBalance
import com.example.mindos.features.wallet.domain.model.WalletTransaction
import com.example.mindos.features.wallet.domain.model.WalletVerifyResult
import com.example.mindos.features.waves.data.InMemoryWaveRequestRepository
import com.example.mindos.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class AppGraphTest {

    @Test
    fun constructor_acceptsAllFakes() {
        // The whole point of AppGraph is that tests can build one with fakes and
        // hand it to MindOSApp without invoking any real Firebase wiring. Pin
        // the constructor surface so refactors don't accidentally tighten it
        // (e.g. forcing a concrete FirebaseDMRepository).
        val dm = FakeDMRepository()
        val wallet = FakeWalletApiClient()
        val waves = InMemoryWaveRequestRepository()
        val feed = FakeGlobalFeedRepository()
        val privacy = mutableStateOf(false)
        val appClock = AppClock { Timestamp(123L) }
        val profileRepository = InMemoryMindOSProfileRepository()
        val observerRepository = InMemoryProfileObserverRepository(baseObserverCount = 0)
        val presenceRepository = InMemoryProfilePresenceRepository()
        val waveService = WaveRequestService(waves)

        val graph = AppGraph(
            dmRepository = dm,
            walletClient = wallet,
            waveRequestRepository = waves,
            globalFeedRepository = feed,
            appClock = appClock,
            mindOSProfileRepository = profileRepository,
            mindOSProfileObserverRepository = observerRepository,
            mindOSProfilePresenceRepository = presenceRepository,
            privacyEnabledState = privacy,
            waveRequestService = waveService,
        )

        assertSame(dm, graph.dmRepository)
        assertSame(wallet, graph.walletClient)
        assertSame(waves, graph.waveRequestRepository)
        assertSame(feed, graph.globalFeedRepository)
        assertSame(appClock, graph.appClock)
        assertSame(profileRepository, graph.mindOSProfileRepository)
        assertSame(observerRepository, graph.mindOSProfileObserverRepository)
        assertSame(presenceRepository, graph.mindOSProfilePresenceRepository)
        assertSame(privacy, graph.privacyEnabledState)
        assertSame(waveService, graph.waveRequestService)
    }

    @Test
    fun privacyState_isMutableAndObservable() {
        // privacyEnabledState must be a real MutableState so the overlay and the
        // settings screen can mutate it from either side.
        val state = mutableStateOf(false)
        val waves = InMemoryWaveRequestRepository()
        val graph = AppGraph(
            dmRepository = FakeDMRepository(),
            walletClient = FakeWalletApiClient(),
            waveRequestRepository = waves,
            globalFeedRepository = FakeGlobalFeedRepository(),
            appClock = AppClock { Timestamp(123L) },
            mindOSProfileRepository = InMemoryMindOSProfileRepository(),
            mindOSProfileObserverRepository = InMemoryProfileObserverRepository(baseObserverCount = 0),
            mindOSProfilePresenceRepository = InMemoryProfilePresenceRepository(),
            privacyEnabledState = state,
            waveRequestService = WaveRequestService(waves),
        )

        graph.privacyEnabledState.value = true
        assertEquals(true, state.value, "Mutations through the graph must propagate to the underlying state")
    }

    @Test
    fun graph_isEquatable_dataClass() {
        // The data class contract enables test assertions like assertEquals(graph1, graph2).
        // Pin it so the @Stable annotation never accidentally drops the data modifier.
        val dm = FakeDMRepository()
        val wallet = FakeWalletApiClient()
        val waves = InMemoryWaveRequestRepository()
        val feed = FakeGlobalFeedRepository()
        val privacy = mutableStateOf(false)
        val appClock = AppClock { Timestamp(123L) }
        val profileRepository = InMemoryMindOSProfileRepository()
        val observerRepository = InMemoryProfileObserverRepository(baseObserverCount = 0)
        val presenceRepository = InMemoryProfilePresenceRepository()
        // Shared service instance so structural equality holds (WaveRequestService
        // uses reference equality — two separate wrappers would not be equal).
        val waveService = WaveRequestService(waves)

        val a = AppGraph(
            dmRepository = dm,
            walletClient = wallet,
            waveRequestRepository = waves,
            globalFeedRepository = feed,
            appClock = appClock,
            mindOSProfileRepository = profileRepository,
            mindOSProfileObserverRepository = observerRepository,
            mindOSProfilePresenceRepository = presenceRepository,
            privacyEnabledState = privacy,
            waveRequestService = waveService,
        )
        val b = AppGraph(
            dmRepository = dm,
            walletClient = wallet,
            waveRequestRepository = waves,
            globalFeedRepository = feed,
            appClock = appClock,
            mindOSProfileRepository = profileRepository,
            mindOSProfileObserverRepository = observerRepository,
            mindOSProfilePresenceRepository = presenceRepository,
            privacyEnabledState = privacy,
            waveRequestService = waveService,
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotNull(a.copy())
    }

    // ── Minimal in-test fakes ─────────────────────────────────────────────

    private class FakeDMRepository : DMRepository {
        override fun getUserChats(userId: String): Flow<List<DMChat>> = emptyFlow()
        override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat = error("not used")
        override fun getChatMessages(chatId: String): Flow<List<DMMessage>> = emptyFlow()
        override suspend fun getUsersByIds(
            currentUserId: String,
            userIds: Collection<String>,
        ): Map<String, DMUser> = emptyMap()
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
        ): MediaUploadResult = error("not used")
        override suspend fun updateMessage(
            chatId: String,
            messageId: String,
            updates: Map<String, Any?>,
        ) = Unit
        override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> = emptyList()
    }

    private class FakeWalletApiClient : WalletApiClient {
        override suspend fun getBalance(): WalletBalance = error("not used")
        override suspend fun getTransactions(limit: Int): List<WalletTransaction> = emptyList()
        override suspend fun initiatePurchase(
            packId: String,
            idempotencyKey: String,
        ): SandboxPurchasePending = error("not used")
        override suspend fun verifyPurchase(
            sandboxPurchaseId: String,
            sandboxToken: String,
            idempotencyKey: String,
        ): WalletVerifyResult = error("not used")
    }

    private class FakeGlobalFeedRepository : GlobalFeedRepository {
        override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
            DomainResult.success(emptyList())
        override suspend fun loadPostsByUser(
            userId: String,
            limit: Int,
        ): DomainResult<List<GlobalFeedPost>> = DomainResult.success(emptyList())
        override suspend fun createPost(
            request: CreateGlobalFeedPostRequest,
        ): DomainResult<GlobalFeedPost> = error("not used")
    }
}

package com.mindos.app.fakes

import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.model.MessageStatus
import com.mindos.app.features.dm.domain.repository.DMMessageDraft
import com.mindos.app.features.dm.domain.repository.DMRepository
import com.mindos.app.features.dm.domain.repository.DMUser
import com.mindos.app.features.dm.domain.repository.MediaUploadResult
import com.mindos.app.features.wallet.data.WalletApiClient
import com.mindos.app.features.wallet.domain.model.SandboxPurchasePending
import com.mindos.app.features.wallet.domain.model.WalletBalance
import com.mindos.app.features.wallet.domain.model.WalletTransaction
import com.mindos.app.features.wallet.domain.model.WalletVerifyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op fakes for desktop UI tests. They satisfy the interface contract so
 * MindOSApp can be constructed without instantiating Firebase, but they do NOT
 * implement behavior — any invocation throws, which deliberately surfaces an
 * unintended code path during testing.
 *
 * Smoke tests should never exercise these fakes because they only render the
 * Auth surface (no session). Once richer navigation tests arrive, replace
 * these with feature-specific in-memory fakes.
 */

internal class NoOpDMRepository : DMRepository {
    override fun getUserChats(userId: String): Flow<List<DMChat>> = emptyFlow()

    override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat =
        unsupported("getOrCreateChat")

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
    ) = unsupported("createMessageRecord")

    override suspend fun uploadMessageMedia(
        chatId: String,
        messageId: String,
        draft: DMMessageDraft,
    ): MediaUploadResult = unsupported("uploadMessageMedia")

    override suspend fun updateMessage(
        chatId: String,
        messageId: String,
        updates: Map<String, Any?>,
    ) = unsupported("updateMessage")

    override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> = emptyList()

    private fun unsupported(operation: String): Nothing =
        throw UnsupportedOperationException(
            "NoOpDMRepository.$operation must not be invoked from a smoke test.",
        )
}

internal class NoOpWalletApiClient : WalletApiClient {
    override suspend fun getBalance(): WalletBalance = unsupported("getBalance")
    override suspend fun getTransactions(limit: Int): List<WalletTransaction> = unsupported("getTransactions")
    override suspend fun initiatePurchase(packId: String, idempotencyKey: String): SandboxPurchasePending =
        unsupported("initiatePurchase")
    override suspend fun verifyPurchase(
        sandboxPurchaseId: String,
        sandboxToken: String,
        idempotencyKey: String,
    ): WalletVerifyResult = unsupported("verifyPurchase")

    private fun unsupported(operation: String): Nothing =
        throw UnsupportedOperationException(
            "NoOpWalletApiClient.$operation must not be invoked from a smoke test.",
        )
}

package com.example.mindos.features.wallet.presentation

import com.example.mindos.features.wallet.data.WalletApiClient
import com.example.mindos.features.wallet.domain.model.SandboxPurchasePending
import com.example.mindos.features.wallet.domain.model.WalletBalance
import com.example.mindos.features.wallet.domain.model.WalletError
import com.example.mindos.features.wallet.domain.model.WalletTransaction
import com.example.mindos.features.wallet.domain.model.WalletVerifyResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WalletStateHolderTest
 *
 * Covers:
 *  - load() → Loading → Loaded on success
 *  - load() → Loading → Error on failure
 *  - refresh() from Loaded: balance updated without full Loading transition
 *  - refresh() from Error: falls back to full load
 *  - buyPack() success: initiate + verify + getBalance called; balance updated from server
 *  - buyPack() initiate failure: isBuying cleared, buyError set
 *  - buyPack() verify failure: isBuying cleared, buyError set
 *  - buyPack() is a no-op while isBuying is true
 *  - dismissBuyError() clears buyError
 *  - Balance is NEVER computed locally (server getBalance called after every buy)
 */
class WalletStateHolderTest {

    // ── load() ────────────────────────────────────────────────────────────────

    @Test
    fun load_success_transitionsToLoaded() = runTest {
        val client = FakeWalletApiClient(balance = balance(42))
        val holder = WalletStateHolder(client, scope = this)

        holder.load()
        advanceUntilIdle()

        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertEquals(42, state.balance.availableBalance)
    }

    @Test
    fun load_setsLoadingStateBeforeCompletion() = runTest {
        val gate   = CompletableDeferred<Unit>()
        val client = FakeWalletApiClient(balance = balance(10), beforeBalance = { gate.await() })
        val holder = WalletStateHolder(client, scope = this)

        holder.load()
        runCurrent()

        assertIs<WalletUiState.Loading>(holder.uiState.value)

        gate.complete(Unit)
        advanceUntilIdle()

        assertIs<WalletUiState.Loaded>(holder.uiState.value)
    }

    @Test
    fun load_failure_transitionsToError() = runTest {
        val client = FakeWalletApiClient(balanceError = WalletError.NotAuthenticated)
        val holder = WalletStateHolder(client, scope = this)

        holder.load()
        advanceUntilIdle()

        val state = assertIs<WalletUiState.Error>(holder.uiState.value)
        assertIs<WalletError.NotAuthenticated>(state.error)
    }

    @Test
    fun load_rateLimitError_transitionsToError() = runTest {
        val client = FakeWalletApiClient(balanceError = WalletError.RateLimited)
        val holder = WalletStateHolder(client, scope = this)

        holder.load()
        advanceUntilIdle()

        val state = assertIs<WalletUiState.Error>(holder.uiState.value)
        assertIs<WalletError.RateLimited>(state.error)
    }

    // ── refresh() ─────────────────────────────────────────────────────────────

    @Test
    fun refresh_fromLoaded_updatesBalanceWithoutLoadingTransition() = runTest {
        val client = FakeWalletApiClient(balance = balance(10))
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()
        assertIs<WalletUiState.Loaded>(holder.uiState.value)

        // Now update balance that the server will return
        client.balance = balance(99)
        holder.refresh()
        advanceUntilIdle()

        // Should remain Loaded (no transition through Loading)
        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertEquals(99, state.balance.availableBalance)
    }

    @Test
    fun refresh_fromError_fallsBackToFullLoad() = runTest {
        val client = FakeWalletApiClient(balanceError = WalletError.NetworkError())
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()
        assertIs<WalletUiState.Error>(holder.uiState.value)

        // Fix the error and refresh
        client.balanceError = null
        client.balance = balance(5)
        holder.refresh()
        advanceUntilIdle()

        assertIs<WalletUiState.Loaded>(holder.uiState.value)
    }

    @Test
    fun refresh_networkErrorWhileLoaded_keepsStaleBalanceAndSurfacesError() = runTest {
        // Bad-connection / offline resilience: once a balance is shown, a failed
        // background refresh must NOT wipe it — the user keeps seeing the last
        // known balance and the error surfaces non-destructively.
        val client = FakeWalletApiClient(balance = balance(42))
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()
        assertIs<WalletUiState.Loaded>(holder.uiState.value)

        client.balanceError = WalletError.NetworkError()
        holder.refresh()
        advanceUntilIdle()

        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertEquals(42, state.balance.availableBalance, "Stale balance must survive a failed refresh")
        assertFalse(state.isRefreshing)
        assertNotNull(state.buyError)
        assertIs<WalletError.NetworkError>(state.buyError)
    }

    // ── buyPack() ─────────────────────────────────────────────────────────────

    @Test
    fun buyPack_success_callsInitiateVerifyAndGetBalance() = runTest {
        val freshBalance = balance(110)
        val client = FakeWalletApiClient(
            balance             = balance(100),
            pendingPurchase     = pending("coins_10"),
            verifyResult        = verify("tx-1", 10),
            balanceAfterVerify  = freshBalance,
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_10")
        advanceUntilIdle()

        // Server's getBalance was called after verify — not locally computed
        assertEquals(1, client.initiateCallCount)
        assertEquals(1, client.verifyCallCount)
        assertEquals(2, client.balanceCallCount)    // initial load + post-buy refresh

        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertEquals(110, state.balance.availableBalance)
        assertNull(state.buyError)
    }

    @Test
    fun buyPack_balanceComesFromServer_notFromVerifyResult() = runTest {
        // Even though verifyResult says 10 coins received, the NEW balance comes from getBalance()
        val serverBalance = balance(77)  // deliberately different from what you'd compute locally
        val client = FakeWalletApiClient(
            balance             = balance(100),
            pendingPurchase     = pending("coins_10"),
            verifyResult        = verify("tx-1", 10),
            balanceAfterVerify  = serverBalance,
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_10")
        advanceUntilIdle()

        // Balance must be exactly what the server returned, not 100+10=110
        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertEquals(77, state.balance.availableBalance)
    }

    @Test
    fun buyPack_initiateFailure_setsBuyError() = runTest {
        val client = FakeWalletApiClient(
            balance          = balance(50),
            initiateError    = WalletError.ServerError("Pack not found"),
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_10")
        advanceUntilIdle()

        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertNotNull(state.buyError)
        assertIs<WalletError.ServerError>(state.buyError)
        assertEquals(false, state.isBuying)
        assertEquals(0, client.verifyCallCount)   // verify was not called
    }

    @Test
    fun buyPack_verifyFailure_setsBuyError() = runTest {
        val client = FakeWalletApiClient(
            balance         = balance(50),
            pendingPurchase = pending("coins_10"),
            verifyError     = WalletError.PurchaseAlreadyCredited,
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_10")
        advanceUntilIdle()

        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertNotNull(state.buyError)
        assertIs<WalletError.PurchaseAlreadyCredited>(state.buyError)
        assertEquals(false, state.isBuying)
    }

    @Test
    fun buyPack_whileAlreadyBuying_isNoOp() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val client = FakeWalletApiClient(
            balance         = balance(50),
            pendingPurchase = pending("coins_10"),
            verifyResult    = verify("tx-1", 10),
            balanceAfterVerify = balance(60),
            beforeInitiate  = { gate.await() },
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_10")
        runCurrent()

        // While first buy is in-flight, try another
        holder.buyPack("coins_10")
        gate.complete(Unit)
        advanceUntilIdle()

        // Only one purchase should have happened
        assertEquals(1, client.initiateCallCount)
    }

    @Test
    fun buyPack_setsIsBuyingDuringOperation() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val client = FakeWalletApiClient(
            balance         = balance(10),
            pendingPurchase = pending("coins_1"),
            verifyResult    = verify("tx-1", 1),
            balanceAfterVerify = balance(11),
            beforeInitiate  = { gate.await() },
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_1")
        runCurrent()

        val buying = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertTrue(buying.isBuying)

        gate.complete(Unit)
        advanceUntilIdle()

        val done = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertEquals(false, done.isBuying)
    }

    // ── dismissBuyError() ─────────────────────────────────────────────────────

    @Test
    fun dismissBuyError_clearsBuyError() = runTest {
        val client = FakeWalletApiClient(
            balance       = balance(50),
            initiateError = WalletError.RateLimited,
        )
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.buyPack("coins_10")
        advanceUntilIdle()
        assertNotNull(assertIs<WalletUiState.Loaded>(holder.uiState.value).buyError)

        holder.dismissBuyError()

        assertNull(assertIs<WalletUiState.Loaded>(holder.uiState.value).buyError)
    }

    @Test
    fun dismissBuyError_isNoOpWhenNoError() = runTest {
        val client = FakeWalletApiClient(balance = balance(10))
        val holder = WalletStateHolder(client, scope = this)
        holder.load()
        advanceUntilIdle()

        holder.dismissBuyError()  // should not crash or change state

        val state = assertIs<WalletUiState.Loaded>(holder.uiState.value)
        assertNull(state.buyError)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun balance(available: Int) = WalletBalance(
        availableBalance   = available,
        purchasedBalance   = available,
        promotionalBalance = 0,
        earnedBalance      = 0,
    )

    private fun pending(packId: String) = SandboxPurchasePending(
        purchaseId    = "purchase-001",
        sandboxToken  = "sandbox_token_abc",
        packId        = packId,
        coinsAmount   = 10,
        correlationId = "corr-001",
    )

    private fun verify(txId: String, coins: Int) = WalletVerifyResult(
        transactionId = txId,
        coinsReceived = coins,
        purchaseId    = "purchase-001",
        correlationId = "corr-002",
    )

    // ── FakeWalletApiClient ───────────────────────────────────────────────────

    private class FakeWalletApiClient(
        var balance: WalletBalance? = null,
        var balanceError: WalletError? = null,
        var balanceAfterVerify: WalletBalance? = null,
        var pendingPurchase: SandboxPurchasePending? = null,
        var initiateError: WalletError? = null,
        var verifyResult: WalletVerifyResult? = null,
        var verifyError: WalletError? = null,
        private val beforeBalance: suspend () -> Unit = {},
        private val beforeInitiate: suspend () -> Unit = {},
    ) : WalletApiClient {

        var balanceCallCount = 0
            private set
        var initiateCallCount = 0
            private set
        var verifyCallCount = 0
            private set

        override suspend fun getBalance(): WalletBalance {
            beforeBalance()
            balanceError?.let { throw it }
            // After first verify, optionally return a different balance
            return if (balanceCallCount > 0 && balanceAfterVerify != null) {
                balanceCallCount++
                balanceAfterVerify!!
            } else {
                balanceCallCount++
                balance ?: error("balance not configured in FakeWalletApiClient")
            }
        }

        override suspend fun getTransactions(limit: Int): List<WalletTransaction> = emptyList()

        override suspend fun initiatePurchase(
            packId: String,
            idempotencyKey: String,
        ): SandboxPurchasePending {
            beforeInitiate()
            initiateCallCount++
            initiateError?.let { throw it }
            return pendingPurchase ?: error("pendingPurchase not configured in FakeWalletApiClient")
        }

        override suspend fun verifyPurchase(
            sandboxPurchaseId: String,
            sandboxToken: String,
            idempotencyKey: String,
        ): WalletVerifyResult {
            verifyCallCount++
            verifyError?.let { throw it }
            return verifyResult ?: error("verifyResult not configured in FakeWalletApiClient")
        }
    }
}

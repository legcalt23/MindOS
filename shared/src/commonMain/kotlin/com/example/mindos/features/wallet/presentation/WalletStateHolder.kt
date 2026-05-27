package com.example.mindos.features.wallet.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mindos.features.wallet.data.WalletApiClient
import com.example.mindos.features.wallet.domain.model.SandboxPurchaseCatalog
import com.example.mindos.features.wallet.domain.model.SandboxPurchaseOffer
import com.example.mindos.features.wallet.domain.model.WalletBalance
import com.example.mindos.features.wallet.domain.model.WalletError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// ── UI state ──────────────────────────────────────────────────────────────────

sealed interface WalletUiState {

    /** Balance is being fetched for the first time (or after a hard error). */
    data object Loading : WalletUiState

    /** Balance is available and the wallet is usable. */
    data class Loaded(
        val balance: WalletBalance,
        val offers: List<SandboxPurchaseOffer> = SandboxPurchaseCatalog.offers,
        /** True while a background balance refresh is in progress (no full Loading spinner). */
        val isRefreshing: Boolean = false,
        /** True while initiate+verify is running. Prevents concurrent purchases. */
        val isBuying: Boolean = false,
        /** Non-null when a buy operation failed. Call [WalletStateHolder.dismissBuyError] to clear. */
        val buyError: WalletError? = null,
    ) : WalletUiState

    /**
     * Fatal error prevented the balance from loading.
     * User can retry via [WalletStateHolder.load].
     */
    data class Error(val error: WalletError) : WalletUiState
}

// ── StateHolder ───────────────────────────────────────────────────────────────

/**
 * WalletStateHolder
 *
 * Security contract:
 *  - Balance is NEVER computed on the client. After every mutation the server is
 *    queried via [WalletApiClient.getBalance] and the result is displayed as-is.
 *  - Idempotency keys are generated client-side per purchase intent.
 *    New keys are generated on each retry (since retrying a failed attempt is a
 *    new intent in the sandbox flow). The server deduplicates completed operations.
 *  - [buyPack] will not start a second concurrent purchase while [WalletUiState.Loaded.isBuying]
 *    is true.
 */
class WalletStateHolder(
    private val client: WalletApiClient,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var buyJob: Job? = null

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Loads the wallet balance from the server.
     * Transitions to [WalletUiState.Loading] → [WalletUiState.Loaded] or [WalletUiState.Error].
     * Call on screen entry.
     */
    fun load() {
        loadJob?.cancel()
        _uiState.value = WalletUiState.Loading
        loadJob = scope.launch {
            fetchBalanceSafely(
                onSuccess = { balance ->
                    _uiState.value = WalletUiState.Loaded(balance = balance)
                },
                onError = { error ->
                    _uiState.value = WalletUiState.Error(error = error)
                },
            )
        }
    }

    /**
     * Silently refreshes the balance in the background.
     * If the wallet is already [WalletUiState.Loaded], the current balance remains visible while
     * the refresh is in progress ([WalletUiState.Loaded.isRefreshing] becomes true).
     * Falls back to [load] if the state is not Loaded (e.g., after a hard error).
     */
    fun refresh() {
        val current = _uiState.value
        if (current !is WalletUiState.Loaded) {
            load()
            return
        }
        if (current.isRefreshing) return     // already refreshing

        loadJob?.cancel()
        _uiState.value = current.copy(isRefreshing = true)
        loadJob = scope.launch {
            fetchBalanceSafely(
                onSuccess = { balance ->
                    _uiState.update { state ->
                        if (state is WalletUiState.Loaded) state.copy(balance = balance, isRefreshing = false)
                        else WalletUiState.Loaded(balance = balance)
                    }
                },
                onError = { error ->
                    // On refresh failure, keep the stale balance and surface the error as a buyError
                    // so the user can see the problem without losing their current view.
                    _uiState.update { state ->
                        if (state is WalletUiState.Loaded) state.copy(isRefreshing = false, buyError = error)
                        else WalletUiState.Error(error = error)
                    }
                },
            )
        }
    }

    /**
     * Initiates and verifies a sandbox purchase for [packId].
     *
     * Flow:
     *  1. Sets [WalletUiState.Loaded.isBuying] = true
     *  2. Calls [WalletApiClient.initiatePurchase] — server validates packId against catalog
     *  3. Calls [WalletApiClient.verifyPurchase] — server credits the wallet
     *  4. Calls [WalletApiClient.getBalance] — refreshes balance from server (never computed locally)
     *  5. Updates state to [WalletUiState.Loaded] with fresh balance
     *
     * On any error, [WalletUiState.Loaded.buyError] is set and isBuying is cleared.
     * Does nothing if a purchase is already in progress.
     *
     * @param packId One of "coins_1", "coins_10", "coins_100".
     */
    fun buyPack(packId: String) {
        val current = _uiState.value
        if (current !is WalletUiState.Loaded) return
        if (current.isBuying) return                // guard against double-tap

        buyJob?.cancel()
        _uiState.value = current.copy(isBuying = true, buyError = null)

        buyJob = scope.launch {
            try {
                // Step 1 – initiate (server validates packId; generates token server-side)
                val initiateKey = generateIdempotencyKey()
                val pending = client.initiatePurchase(packId, initiateKey)

                // Step 2 – verify (token used exactly once; server credits the wallet)
                val verifyKey = generateIdempotencyKey()
                client.verifyPurchase(pending.purchaseId, pending.sandboxToken, verifyKey)

                // Step 3 – refresh balance from server; NEVER use coinsReceived to compute locally
                val freshBalance = client.getBalance()

                _uiState.update { state ->
                    if (state is WalletUiState.Loaded)
                        state.copy(balance = freshBalance, isBuying = false, buyError = null)
                    else
                        WalletUiState.Loaded(balance = freshBalance)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: WalletError) {
                _uiState.update { state ->
                    if (state is WalletUiState.Loaded)
                        state.copy(isBuying = false, buyError = e)
                    else
                        WalletUiState.Error(error = e)
                }
            } catch (e: Exception) {
                val error = WalletError.Unknown(detail = e.message ?: "")
                _uiState.update { state ->
                    if (state is WalletUiState.Loaded)
                        state.copy(isBuying = false, buyError = error)
                    else
                        WalletUiState.Error(error = error)
                }
            }
        }
    }

    /**
     * Clears a transient buy error so the UI can return to its idle loaded state.
     * No-op if the current state has no buy error.
     */
    fun dismissBuyError() {
        _uiState.update { state ->
            if (state is WalletUiState.Loaded) state.copy(buyError = null) else state
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun fetchBalanceSafely(
        onSuccess: (WalletBalance) -> Unit,
        onError: (WalletError) -> Unit,
    ) {
        try {
            val balance = client.getBalance()
            onSuccess(balance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: WalletError) {
            onError(e)
        } catch (e: Exception) {
            onError(WalletError.Unknown(detail = e.message ?: ""))
        }
    }
}

// ── Composable factory ────────────────────────────────────────────────────────

/**
 * Creates and remembers a [WalletStateHolder] bound to the current composition lifecycle.
 * Automatically loads the balance when first composed.
 */
@Composable
fun rememberWalletStateHolder(client: WalletApiClient): WalletStateHolder {
    val scope   = rememberCoroutineScope()
    val holder  = remember(client, scope) { WalletStateHolder(client, scope) }
    return holder
}

// ── Idempotency key generation ────────────────────────────────────────────────

/**
 * Generates a random idempotency key using the platform's PRNG.
 * Format is UUID v4-compatible (random hex). Not cryptographically strong,
 * but sufficient for deduplication in the sandbox payment flow.
 */
private fun generateIdempotencyKey(): String {
    fun Long.toHex16(): String = toULong().toString(16).padStart(16, '0')
    val high = Random.Default.nextLong().toHex16()
    val low  = Random.Default.nextLong().toHex16()
    val hex  = high + low
    // Format: xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-4${hex.substring(13, 16)}" +
           "-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}

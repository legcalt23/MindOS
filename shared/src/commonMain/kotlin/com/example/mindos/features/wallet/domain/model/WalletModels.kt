package com.example.mindos.features.wallet.domain.model

/**
 * WalletModels.kt
 * Client-side domain models for the MindOS virtual currency wallet.
 *
 * Security contract:
 *  - Balance is NEVER the source of truth on the client — always fetched from the server.
 *  - Client never sends balance, newBalance, or final price to the server.
 *  - Idempotency keys are generated client-side (one UUID per request) and passed in.
 *  - No wallet state is persisted locally beyond the current UI session.
 *  - sandboxToken in SandboxPurchasePending is single-use; never stored or logged.
 */

// ── Balance ───────────────────────────────────────────────────────────────────

/**
 * Current wallet balance, sourced exclusively from the server's balance_snapshots.
 * All fields are non-negative integers representing virtual coin amounts.
 */
data class WalletBalance(
    val availableBalance: Int,
    val purchasedBalance: Int,
    val promotionalBalance: Int,
    val earnedBalance: Int,
) {
    init {
        require(availableBalance >= 0)    { "availableBalance must be >= 0" }
        require(purchasedBalance >= 0)    { "purchasedBalance must be >= 0" }
        require(promotionalBalance >= 0)  { "promotionalBalance must be >= 0" }
        require(earnedBalance >= 0)       { "earnedBalance must be >= 0" }
    }

    companion object {
        val EMPTY = WalletBalance(
            availableBalance   = 0,
            purchasedBalance   = 0,
            promotionalBalance = 0,
            earnedBalance      = 0,
        )
    }
}

// ── Transaction ───────────────────────────────────────────────────────────────

/**
 * A summary of a single ledger transaction for display in the transaction history.
 * Amount is the net coin movement (positive = credit, negative = debit).
 */
data class WalletTransaction(
    val transactionId: String,
    val type: String,       // e.g. "purchase_credit", "spend", "refund"
    val totalAmount: Int,   // net coins changed; positive = credit, negative = debit
    val createdAt: Long,    // epoch millis
)

// ── Purchase offers ───────────────────────────────────────────────────────────

/**
 * A sandbox purchase pack as defined in the server-side PACK_CATALOG.
 * Client catalog is for display only — the server validates packId and determines
 * coin amounts; these values are never trusted from the client.
 *
 * Server catalog (functions/src/lib/walletValidators.ts):
 *   coins_1   → 1 coin   (SANDBOX, free)
 *   coins_10  → 10 coins  (SANDBOX, free)
 *   coins_100 → 100 coins (SANDBOX, free)
 */
data class SandboxPurchaseOffer(
    val packId: String,
    val coinsAmount: Int,
    val displayLabel: String,
)

object SandboxPurchaseCatalog {
    /**
     * Mirrors the server-side PACK_CATALOG.
     * These are sandbox packs only — priceCents is 0, currency is SANDBOX.
     * The server validates and enforces these values; the client only uses
     * this catalog to render UI before the purchase is initiated.
     */
    val offers: List<SandboxPurchaseOffer> = listOf(
        SandboxPurchaseOffer(packId = "coins_1",   coinsAmount = 1,   displayLabel = "1 moneda"),
        SandboxPurchaseOffer(packId = "coins_10",  coinsAmount = 10,  displayLabel = "10 monedas"),
        SandboxPurchaseOffer(packId = "coins_100", coinsAmount = 100, displayLabel = "100 monedas"),
    )

    fun findByPackId(packId: String): SandboxPurchaseOffer? = offers.find { it.packId == packId }
}

// ── Real-money IAP catalog (I3) ────────────────────────────────────────────────

/**
 * A real-money coin pack sold via store IAP (Google Play Billing / Apple StoreKit).
 *
 * Single source of truth for the *display* catalog of the real purchase flow. The SERVER
 * stays authoritative: after the store verifies the purchase token, the backend credits the
 * coins from ITS catalog keyed by [productId]. The client NEVER credits from [coins] here, and
 * the real charged price is whatever the store shows for the user's locale — [priceLabel] is a
 * display hint only (anchored to the configured store tier).
 *
 * Decision I1/I3 (docs/security/IAP_COMPLIANCE_DECISION.md): coins sold for real money at
 * 100 coins = 1 €; consumable products; larger packs carry a bonus.
 */
data class CoinPack(
    val productId: String,
    val coins: Int,
    val priceLabel: String,
    /** Marketing bonus vs the linear 100-coins-per-euro base rate. 0 for the base pack. */
    val bonusPercent: Int = 0,
)

object CoinPackCatalog {
    val packs: List<CoinPack> = listOf(
        CoinPack(productId = "coins_100", coins = 100, priceLabel = "0,99 €"),
        CoinPack(productId = "coins_550", coins = 550, priceLabel = "4,99 €", bonusPercent = 10),
        CoinPack(productId = "coins_1200", coins = 1200, priceLabel = "9,99 €", bonusPercent = 20),
        CoinPack(productId = "coins_3000", coins = 3000, priceLabel = "24,99 €", bonusPercent = 20),
    )

    fun findByProductId(productId: String): CoinPack? = packs.find { it.productId == productId }
}

// ── Purchase flow ─────────────────────────────────────────────────────────────

/**
 * Result of a successful initiatePurchase call.
 *
 * Security note: sandboxToken is returned ONCE by the server and must be
 * used immediately in verifyPurchase. It is NOT stored in Firestore — only
 * its SHA-256 hash is kept server-side. Do NOT persist or log sandboxToken.
 */
data class SandboxPurchasePending(
    val purchaseId: String,
    val sandboxToken: String,   // single-use; use in verifyPurchase then discard
    val packId: String,
    val coinsAmount: Int,       // from server response — authoritative
    val correlationId: String,
)

/**
 * Result of a successful verifyPurchase call.
 * After this succeeds, the ViewModel must re-fetch balance from the server —
 * never compute or update balance optimistically on the client.
 */
data class WalletVerifyResult(
    val transactionId: String,
    val coinsReceived: Int,     // from server response — authoritative
    val purchaseId: String,
    val correlationId: String,
)

// ── Errors ────────────────────────────────────────────────────────────────────

/**
 * Typed errors for all wallet operations.
 * Maps Firebase Functions error codes to domain-level failures.
 */
sealed class WalletError(override val message: String) : Exception(message) {

    /** Firebase Auth token is missing or expired. */
    data object NotAuthenticated : WalletError("Authentication required.")

    /** Available balance is too low to complete the operation. */
    data object InsufficientBalance : WalletError("Insufficient balance.")

    /** Purchase ID not found, or belongs to a different user. */
    data object PurchaseNotFound : WalletError("Purchase not found.")

    /** Purchase has already been credited — double-credit prevented. */
    data object PurchaseAlreadyCredited : WalletError("Purchase already credited.")

    /** Rate limit hit — too many requests in a short window. */
    data object RateLimited : WalletError("Too many requests. Please wait and try again.")

    /** Wallet is frozen or suspended. */
    data object WalletSuspended : WalletError("Wallet is suspended.")

    /** Server returned a known error with a message. */
    data class ServerError(val serverMessage: String) : WalletError(serverMessage)

    /** Network or connectivity failure. */
    data class NetworkError(val rootCause: Throwable? = null) :
        WalletError("Network error. Check your connection.")

    /** Unexpected error not mapped to any known code. */
    data class Unknown(val rootCause: Throwable? = null, val detail: String = "") :
        WalletError("An unexpected error occurred.")
}

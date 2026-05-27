package com.example.mindos.features.wallet.domain.model

import com.example.mindos.features.wallet.data.mapErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WalletModelsTest
 *
 * Covers:
 *  - WalletBalance construction and invariants
 *  - SandboxPurchaseCatalog contents
 *  - WalletError sealed hierarchy
 *  - mapErrorCode — pure error-mapping function
 */
class WalletModelsTest {

    // ── WalletBalance ─────────────────────────────────────────────────────────

    @Test
    fun walletBalance_constructsWithValidValues() {
        val balance = WalletBalance(
            availableBalance   = 110,
            purchasedBalance   = 100,
            promotionalBalance = 5,
            earnedBalance      = 5,
        )
        assertEquals(110, balance.availableBalance)
        assertEquals(100, balance.purchasedBalance)
        assertEquals(5,   balance.promotionalBalance)
        assertEquals(5,   balance.earnedBalance)
    }

    @Test
    fun walletBalance_zeroIsValid() {
        val balance = WalletBalance.EMPTY
        assertEquals(0, balance.availableBalance)
        assertEquals(0, balance.purchasedBalance)
        assertEquals(0, balance.promotionalBalance)
        assertEquals(0, balance.earnedBalance)
    }

    @Test
    fun walletBalance_rejectsNegativeAvailableBalance() {
        assertFailsWith<IllegalArgumentException> {
            WalletBalance(
                availableBalance   = -1,
                purchasedBalance   = 0,
                promotionalBalance = 0,
                earnedBalance      = 0,
            )
        }
    }

    @Test
    fun walletBalance_rejectsNegativePurchasedBalance() {
        assertFailsWith<IllegalArgumentException> {
            WalletBalance(
                availableBalance   = 0,
                purchasedBalance   = -1,
                promotionalBalance = 0,
                earnedBalance      = 0,
            )
        }
    }

    @Test
    fun walletBalance_rejectsNegativePromotionalBalance() {
        assertFailsWith<IllegalArgumentException> {
            WalletBalance(
                availableBalance   = 0,
                purchasedBalance   = 0,
                promotionalBalance = -1,
                earnedBalance      = 0,
            )
        }
    }

    @Test
    fun walletBalance_rejectsNegativeEarnedBalance() {
        assertFailsWith<IllegalArgumentException> {
            WalletBalance(
                availableBalance   = 0,
                purchasedBalance   = 0,
                promotionalBalance = 0,
                earnedBalance      = -1,
            )
        }
    }

    @Test
    fun walletBalance_equalityByValue() {
        val a = WalletBalance(10, 10, 0, 0)
        val b = WalletBalance(10, 10, 0, 0)
        assertEquals(a, b)
    }

    // ── SandboxPurchaseCatalog ────────────────────────────────────────────────

    @Test
    fun sandboxCatalog_hasExactlyThreeOffers() {
        assertEquals(3, SandboxPurchaseCatalog.offers.size)
    }

    @Test
    fun sandboxCatalog_containsExpectedPackIds() {
        val packIds = SandboxPurchaseCatalog.offers.map { it.packId }.toSet()
        assertTrue("coins_1"   in packIds)
        assertTrue("coins_10"  in packIds)
        assertTrue("coins_100" in packIds)
    }

    @Test
    fun sandboxCatalog_coinsAmountsMatchServerCatalog() {
        val byId = SandboxPurchaseCatalog.offers.associateBy { it.packId }
        assertEquals(1,   byId["coins_1"]?.coinsAmount)
        assertEquals(10,  byId["coins_10"]?.coinsAmount)
        assertEquals(100, byId["coins_100"]?.coinsAmount)
    }

    @Test
    fun sandboxCatalog_findByPackId_returnsCorrectOffer() {
        val offer = SandboxPurchaseCatalog.findByPackId("coins_10")
        assertNotNull(offer)
        assertEquals("coins_10", offer.packId)
        assertEquals(10, offer.coinsAmount)
    }

    @Test
    fun sandboxCatalog_findByPackId_returnsNullForUnknownId() {
        assertNull(SandboxPurchaseCatalog.findByPackId("coins_9999"))
    }

    // ── WalletError sealed class ──────────────────────────────────────────────

    @Test
    fun walletError_notAuthenticated_isException() {
        val error: Exception = WalletError.NotAuthenticated
        assertTrue(error is WalletError)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun walletError_serverError_preservesMessage() {
        val error = WalletError.ServerError("Custom server message")
        assertEquals("Custom server message", error.message)
        assertEquals("Custom server message", error.serverMessage)
    }

    @Test
    fun walletError_networkError_hasNullRootCauseByDefault() {
        val error = WalletError.NetworkError()
        assertNull(error.rootCause)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun walletError_networkError_storesRootCauseWhenProvided() {
        val original = RuntimeException("socket closed")
        val error = WalletError.NetworkError(rootCause = original)
        assertEquals(original, error.rootCause)
    }

    @Test
    fun walletError_unknown_hasEmptyDetailByDefault() {
        val error = WalletError.Unknown()
        assertEquals("", error.detail)
    }

    // ── mapErrorCode — pure error mapping ─────────────────────────────────────

    @Test
    fun mapErrorCode_unauthenticated_mapsToNotAuthenticated() {
        val result = mapErrorCode("UNAUTHENTICATED", "Token expired")
        assertIs<WalletError.NotAuthenticated>(result)
    }

    @Test
    fun mapErrorCode_permissionDenied_mapsToNotAuthenticated() {
        val result = mapErrorCode("PERMISSION_DENIED", "")
        assertIs<WalletError.NotAuthenticated>(result)
    }

    @Test
    fun mapErrorCode_failedPrecondition_insufficientBalance() {
        val result = mapErrorCode("FAILED_PRECONDITION", "INSUFFICIENT_BALANCE: not enough coins")
        assertIs<WalletError.InsufficientBalance>(result)
    }

    @Test
    fun mapErrorCode_failedPrecondition_walletSuspended() {
        val result = mapErrorCode("FAILED_PRECONDITION", "WALLET_SUSPENDED")
        assertIs<WalletError.WalletSuspended>(result)
    }

    @Test
    fun mapErrorCode_failedPrecondition_walletFrozen_mapsToSuspended() {
        val result = mapErrorCode("FAILED_PRECONDITION", "WALLET_FROZEN")
        assertIs<WalletError.WalletSuspended>(result)
    }

    @Test
    fun mapErrorCode_failedPrecondition_purchaseAlreadyCredited() {
        val result = mapErrorCode("FAILED_PRECONDITION", "PURCHASE_ALREADY_CREDITED")
        assertIs<WalletError.PurchaseAlreadyCredited>(result)
    }

    @Test
    fun mapErrorCode_failedPrecondition_genericMessage_mapsToServerError() {
        val result = mapErrorCode("FAILED_PRECONDITION", "Some other precondition failure")
        assertIs<WalletError.ServerError>(result)
        assertEquals("Some other precondition failure", result.serverMessage)
    }

    @Test
    fun mapErrorCode_failedPrecondition_blankMessage_usesDefault() {
        val result = mapErrorCode("FAILED_PRECONDITION", "")
        assertIs<WalletError.ServerError>(result)
        assertTrue(result.serverMessage.isNotBlank())
    }

    @Test
    fun mapErrorCode_notFound_mapsToPurchaseNotFound() {
        val result = mapErrorCode("NOT_FOUND", "PURCHASE_NOT_FOUND")
        assertIs<WalletError.PurchaseNotFound>(result)
    }

    @Test
    fun mapErrorCode_resourceExhausted_mapsToRateLimited() {
        val result = mapErrorCode("RESOURCE_EXHAUSTED", "RATE_LIMIT_EXCEEDED")
        assertIs<WalletError.RateLimited>(result)
    }

    @Test
    fun mapErrorCode_unknownCode_mapsToServerError() {
        val result = mapErrorCode("INTERNAL", "Something broke on the server")
        assertIs<WalletError.ServerError>(result)
    }

    @Test
    fun mapErrorCode_unknownCode_blankMessage_usesDefault() {
        val result = mapErrorCode("INTERNAL", "")
        assertIs<WalletError.ServerError>(result)
        assertTrue(result.serverMessage.isNotBlank())
    }

    // ── WalletTransaction ─────────────────────────────────────────────────────

    @Test
    fun walletTransaction_constructsCorrectly() {
        val tx = WalletTransaction(
            transactionId = "tx-001",
            type          = "purchase_credit",
            totalAmount   = 10,
            createdAt     = 1_700_000_000_000L,
        )
        assertEquals("tx-001", tx.transactionId)
        assertEquals("purchase_credit", tx.type)
        assertEquals(10, tx.totalAmount)
        assertEquals(1_700_000_000_000L, tx.createdAt)
    }

    @Test
    fun walletTransaction_negativeAmountRepresentsDebit() {
        val tx = WalletTransaction(
            transactionId = "tx-002",
            type          = "spend",
            totalAmount   = -5,
            createdAt     = 1_700_000_000_001L,
        )
        assertTrue(tx.totalAmount < 0)
    }
}

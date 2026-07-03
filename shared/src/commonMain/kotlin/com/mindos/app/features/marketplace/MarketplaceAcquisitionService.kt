package com.mindos.app.features.marketplace

interface MarketplaceAcquisitionService {
    suspend fun claimFreeProduct(productId: String, uid: String, idempotencyKey: String): AcquisitionResult
    suspend fun unlockProgressionProduct(productId: String, uid: String, idempotencyKey: String): AcquisitionResult
}

sealed class AcquisitionResult {
    data class Success(val entitlementId: String, val grantedAtMillis: Long) : AcquisitionResult()
    data class Failure(val errorCode: MarketplaceErrorCode, val errorMessage: String) : AcquisitionResult()
}

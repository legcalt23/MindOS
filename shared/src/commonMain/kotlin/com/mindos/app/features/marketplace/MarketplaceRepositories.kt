package com.mindos.app.features.marketplace

import kotlinx.coroutines.flow.Flow

interface MarketplaceCatalogRepository {
    fun getCatalogMetadata(): Flow<CatalogMetadataDto?>
    suspend fun loadCatalog(forceRefresh: Boolean): List<MarketplaceProductDto>
    suspend fun getProduct(productId: String): MarketplaceProductDto?
}

interface UserEntitlementRepository {
    fun observeEntitlements(uid: String): Flow<List<EntitlementDto>>
    suspend fun loadEntitlements(uid: String): List<EntitlementDto>
    suspend fun hasEntitlement(uid: String, productId: String): Boolean
}

interface UserInventoryRepository {
    fun observeInventory(uid: String): Flow<List<InventoryItemDto>>
    suspend fun loadInventory(uid: String): List<InventoryItemDto>
}

interface MarketplaceAssetResolver {
    suspend fun resolveAsset(assetId: String): MarketplaceAssetDto?
    suspend fun getLocalAssetPath(assetId: String): String?
    suspend fun isAssetCached(assetId: String): Boolean
}

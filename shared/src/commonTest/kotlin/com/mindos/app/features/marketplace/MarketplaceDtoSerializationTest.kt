package com.mindos.app.features.marketplace

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testProductSerialization() {
        val product = MarketplaceProductDto(
            productId = "prod_1",
            sku = "sku_1",
            status = "ACTIVE",
            title = LocalizedTextDto(en = "Title EN", es = "Título ES"),
            description = LocalizedTextDto(en = "Desc EN", es = "Desc ES"),
            assetIds = listOf("asset_1"),
            rarity = "COMMON",
            productType = "BACKGROUND",
            acquisitionType = "FREE_CLAIM",
            availability = AvailabilityDto(startAtMillis = 1782384000L),
            accessibilityLabel = LocalizedTextDto(en = "Acc EN", es = "Acc ES"),
            createdAtMillis = 1782384000L,
            updatedAtMillis = 1782384000L
        )

        val stringPayload = json.encodeToString(MarketplaceProductDto.serializer(), product)
        val decoded = json.decodeFromString(MarketplaceProductDto.serializer(), stringPayload)

        assertEquals(product.productId, decoded.productId)
        assertEquals(product.title.en, decoded.title.en)
        assertEquals(2, decoded.schemaVersion)
    }

    @Test
    fun testAssetSerialization() {
        val asset = MarketplaceAssetDto(
            assetId = "asset_1",
            format = "WEBP_STATIC",
            status = "ACTIVE",
            storagePath = "marketplace/assets/asset_1/original.webp",
            hashSha256 = "hash_123",
            sizeBytes = 1024L,
            compatibleSlots = listOf("profile.background"),
            createdAtMillis = 1782384000L
        )

        val stringPayload = json.encodeToString(MarketplaceAssetDto.serializer(), asset)
        val decoded = json.decodeFromString(MarketplaceAssetDto.serializer(), stringPayload)

        assertEquals(asset.assetId, decoded.assetId)
        assertEquals(asset.format, decoded.format)
        assertEquals(2, decoded.schemaVersion)
    }
}

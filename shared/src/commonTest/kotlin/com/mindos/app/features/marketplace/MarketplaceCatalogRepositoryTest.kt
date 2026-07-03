package com.mindos.app.features.marketplace

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MarketplaceCatalogRepositoryTest {

    private class FakeCatalogRepository : MarketplaceCatalogRepository {
        private val mockProduct = MarketplaceProductDto(
            productId = "prod_Nebula",
            sku = "com.mindos.frames.nebula",
            status = "ACTIVE",
            title = LocalizedTextDto("Nebula Frame", "Marco Nebulosa"),
            description = LocalizedTextDto("Epic Nebula cosmic profile frame", "Marco cósmico de perfil épico"),
            assetIds = listOf("asset_nebula_01"),
            rarity = "EPIC",
            productType = "FRAME",
            acquisitionType = "FREE_CLAIM",
            availability = AvailabilityDto(1782384000L),
            accessibilityLabel = LocalizedTextDto("Frame", "Marco"),
            createdAtMillis = 1782384000L,
            updatedAtMillis = 1782384000L
        )

        override fun getCatalogMetadata(): Flow<CatalogMetadataDto?> {
            return flowOf(CatalogMetadataDto(1, "hash_123", 1782384000L))
        }

        override suspend fun loadCatalog(forceRefresh: Boolean): List<MarketplaceProductDto> {
            return listOf(mockProduct)
        }

        override suspend fun getProduct(productId: String): MarketplaceProductDto? {
            return if (productId == "prod_Nebula") mockProduct else null
        }
    }

    @Test
    fun testFakeCatalogRepositoryRetrieval() = runBlocking {
        val repo = FakeCatalogRepository()
        val products = repo.loadCatalog(forceRefresh = true)
        assertEquals(1, products.size)
        assertEquals("prod_Nebula", products[0].productId)
        assertEquals("com.mindos.frames.nebula", products[0].sku)

        val found = repo.getProduct("prod_Nebula")
        assertNotNull(found)
        assertEquals("com.mindos.frames.nebula", found.sku)

        val notFound = repo.getProduct("prod_Invalid")
        assertNull(notFound)
    }
}

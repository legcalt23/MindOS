package com.mindos.app.features.marketplace

import kotlinx.serialization.Serializable

@Serializable
data class LocalizedTextDto(
    val en: String,
    val es: String
)

@Serializable
data class PricingDto(
    val amount: Double,
    val currency: String
)

@Serializable
data class AvailabilityDto(
    val startAtMillis: Long,
    val endAtMillis: Long? = null
)

@Serializable
data class MarketplaceProductDto(
    val schemaVersion: Int = 2,
    val productId: String,
    val sku: String,
    val status: String,
    val title: LocalizedTextDto,
    val description: LocalizedTextDto,
    val assetIds: List<String>,
    val collectionId: String? = null,
    val rarity: String,
    val productType: String,
    val acquisitionType: String,
    val pricing: PricingDto? = null,
    val availability: AvailabilityDto,
    val tags: List<String> = emptyList(),
    val accessibilityLabel: LocalizedTextDto,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
data class MarketplaceAssetDto(
    val schemaVersion: Int = 2,
    val assetId: String,
    val format: String,
    val status: String,
    val storagePath: String,
    val downloadUrl: String? = null,
    val hashSha256: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val compatibleSlots: List<String>,
    val systemMetadata: Map<String, String> = emptyMap(),
    val createdAtMillis: Long
)

@Serializable
data class EntitlementDto(
    val schemaVersion: Int = 2,
    val entitlementId: String,
    val uid: String,
    val productId: String,
    val assetIds: List<String>,
    val source: String,
    val status: String,
    val grantedAtMillis: Long,
    val expiresAtMillis: Long? = null,
    val revokedAtMillis: Long? = null,
    val revocationReason: String? = null,
    val transactionRef: String? = null
)

@Serializable
data class InventoryItemDto(
    val schemaVersion: Int = 2,
    val inventoryItemId: String,
    val uid: String,
    val productId: String,
    val quantity: Int = 1,
    val acquiredAtMillis: Long
)

@Serializable
data class EquippedItemDto(
    val slotId: String,
    val productId: String,
    val assetId: String,
    val equippedAtMillis: Long
)

@Serializable
data class CatalogMetadataDto(
    val catalogVersion: Int,
    val catalogHash: String,
    val updatedAtMillis: Long
)

@Serializable
enum class MarketplaceErrorCode(
    val technicalMessage: String,
    val userMessageEs: String,
    val userMessageEn: String,
    val isRetryable: Boolean,
    val severity: ErrorSeverity,
    val analyticsAllowed: Boolean
) {
    AUTH_REQUIRED(
        "Authentication is required to perform this action.",
        "Inicia sesión para continuar.",
        "Log in to continue.",
        false, ErrorSeverity.MEDIUM, true
    ),
    APP_CHECK_REQUIRED(
        "App Check verification failed.",
        "Error de seguridad de la aplicación.",
        "Application security error.",
        false, ErrorSeverity.HIGH, true
    ),
    PRODUCT_NOT_FOUND(
        "The requested product does not exist.",
        "El producto solicitado no existe.",
        "The requested product does not exist.",
        false, ErrorSeverity.LOW, true
    ),
    PRODUCT_UNAVAILABLE(
        "The requested product is currently unavailable.",
        "El producto no está disponible en este momento.",
        "The product is currently unavailable.",
        false, ErrorSeverity.LOW, true
    ),
    ENTITLEMENT_NOT_FOUND(
        "The user does not own this entitlement.",
        "No tienes derecho a usar este cosmético.",
        "You do not own this cosmetic.",
        false, ErrorSeverity.MEDIUM, true
    ),
    ENTITLEMENT_EXPIRED(
        "The entitlement has expired.",
        "El tiempo de uso de este elemento ha expirado.",
        "The item usage time has expired.",
        false, ErrorSeverity.LOW, true
    ),
    ENTITLEMENT_REVOKED(
        "The entitlement has been revoked.",
        "Este cosmético ha sido revocado.",
        "This cosmetic has been revoked.",
        false, ErrorSeverity.MEDIUM, true
    ),
    ASSET_SUSPENDED_SOFT(
        "The requested asset is softly suspended.",
        "El elemento visual está temporalmente suspendido.",
        "The item is temporarily suspended.",
        false, ErrorSeverity.LOW, true
    ),
    ASSET_SUSPENDED_HARD(
        "The requested asset is suspended for legal/security reasons.",
        "El elemento visual no está disponible.",
        "The item is unavailable.",
        false, ErrorSeverity.HIGH, true
    ),
    ASSET_INCOMPATIBLE_WITH_SLOT(
        "The asset is not compatible with the designated slot.",
        "El elemento no es compatible con este espacio.",
        "The item is not compatible with this slot.",
        false, ErrorSeverity.LOW, true
    ),
    PAYLOAD_TOO_LARGE(
        "The profile payload exceeds the maximum permitted size.",
        "El diseño de tu perfil es demasiado grande.",
        "The profile size exceeds the limit.",
        false, ErrorSeverity.LOW, true
    ),
    TOO_MANY_CANVAS_ITEMS(
        "The profile canvas exceeds the maximum sticker count.",
        "Has superado el límite de elementos en tu perfil.",
        "You have exceeded the profile item limit.",
        false, ErrorSeverity.LOW, true
    ),
    INVALID_SLOT(
        "The target slot is invalid or unsupported.",
        "El espacio de personalización es inválido.",
        "The customization slot is invalid.",
        false, ErrorSeverity.MEDIUM, true
    ),
    INVALID_ASSET_REFERENCE(
        "The payload references a non-existent or invalid asset.",
        "Referencia visual no válida.",
        "Invalid visual reference.",
        false, ErrorSeverity.MEDIUM, true
    ),
    IDEMPOTENCY_REPLAY(
        "Transaction replayed with matching idempotency key.",
        "Transacción repetida.",
        "Transaction repeated.",
        true, ErrorSeverity.LOW, false
    ),
    RATE_LIMITED(
        "Too many requests. Please try again later.",
        "Demasiadas solicitudes. Inténtalo más tarde.",
        "Too many requests. Try again later.",
        true, ErrorSeverity.LOW, true
    ),
    OFFLINE_UNSUPPORTED(
        "This action cannot be performed while offline.",
        "Esta acción requiere conexión a internet.",
        "This action requires internet connection.",
        true, ErrorSeverity.LOW, false
    ),
    SERVER_VALIDATION_FAILED(
        "The server failed to validate the transaction.",
        "Error al validar la transacción.",
        "Failed to validate transaction.",
        false, ErrorSeverity.HIGH, true
    )
}

enum class ErrorSeverity { LOW, MEDIUM, HIGH }

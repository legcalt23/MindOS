package com.mindos.action.entity

/** Why an entity candidate was rejected. Precision-first: any doubt → rejection, never a guess. */
sealed interface EntityRejectionReason {
    val detail: String

    data class ShapeMismatch(override val detail: String) : EntityRejectionReason
    data class Empty(override val detail: String) : EntityRejectionReason
    data class TooShort(override val detail: String) : EntityRejectionReason
    data class TooLong(override val detail: String) : EntityRejectionReason
    data class WrongTokenCount(override val detail: String) : EntityRejectionReason
}

/** Outcome of one extraction attempt. A rejection is a *typed* result, not an exception. */
sealed interface EntityExtractionResult {
    data class Extracted(val entity: ExtractedEntity) : EntityExtractionResult
    data class Rejected(val type: EntityType, val reason: EntityRejectionReason) : EntityExtractionResult
}

package com.mindos.action.understanding

/**
 * Typed reasons an input could not be understood. Rejections are *results*, never
 * exceptions. [InvalidEntity] is the precision guard: a structurally matched pattern whose
 * slot value failed entity validation is rejected, not coerced into a false positive.
 */
sealed interface RejectionReason {
    val detail: String

    data class EmptyInput(override val detail: String = "empty input") : RejectionReason

    data class InputTooLong(override val detail: String) : RejectionReason

    data class InvalidEntity(
        val slot: SlotName,
        val entityTypeId: String,
        override val detail: String,
    ) : RejectionReason

    data class UnsupportedInput(override val detail: String) : RejectionReason
}

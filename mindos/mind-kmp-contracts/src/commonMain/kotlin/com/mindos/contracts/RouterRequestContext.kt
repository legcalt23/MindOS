package com.mindos.contracts

import kotlinx.serialization.Serializable

@Serializable
enum class RouterCommandSourceDto {
    TYPED_TEXT,
    VOICE_BATCH,
    VOICE_STREAMING_FINAL,
    SHORTCUT,
    DEEP_LINK,
    NOTIFICATION,
    SYSTEM,
}

@Serializable
data class RouterAbuseContextDto(
    val opaqueUserHash: String? = null,
    val deviceHash: String? = null,
    val sessionHash: String? = null,
) {
    init {
        require(opaqueUserHash == null || opaqueUserHash.isNotBlank()) {
            "opaqueUserHash must be null or non-blank."
        }
        require(deviceHash == null || deviceHash.isNotBlank()) {
            "deviceHash must be null or non-blank."
        }
        require(sessionHash == null || sessionHash.isNotBlank()) {
            "sessionHash must be null or non-blank."
        }
    }
}

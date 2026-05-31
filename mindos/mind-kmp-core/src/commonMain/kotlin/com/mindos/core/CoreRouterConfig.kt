package com.mindos.core

data class CoreRouterConfig(
    val maxInputLength: Int = 512,
    val defaultLocaleTag: String = "und",
    val explain: Boolean = false,
) {
    init {
        require(maxInputLength > 0) { "maxInputLength must be positive." }
        require(defaultLocaleTag.isNotBlank()) { "defaultLocaleTag must not be blank." }
    }
}

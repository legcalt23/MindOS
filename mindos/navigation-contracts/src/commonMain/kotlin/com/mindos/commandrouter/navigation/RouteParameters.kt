package com.mindos.commandrouter.navigation

import kotlinx.serialization.Serializable

@Serializable
data class RouteParameters(val params: Map<String, String> = emptyMap()) {
    fun get(key: String): String? = params[key]
    fun require(key: String): String = params[key]
        ?: error("Required route parameter '$key' is missing")

    companion object {
        val Empty = RouteParameters()
    }
}

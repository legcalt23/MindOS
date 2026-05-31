package com.mindos.commandrouter.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavigationTarget {
    @Serializable @SerialName("screen")
    data class Screen(
        val id: String,
        val args: Map<String, String> = emptyMap()
    ) : NavigationTarget

    @Serializable @SerialName("external")
    data class External(val uri: String) : NavigationTarget

    @Serializable @SerialName("back")
    data object Back : NavigationTarget
}

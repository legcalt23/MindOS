package com.mindos.commandrouter.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavigationResult {

    @Serializable @SerialName("success")
    data class Success(val target: NavigationTarget) : NavigationResult

    @Serializable
    sealed interface Failure : NavigationResult {

        @Serializable @SerialName("target_not_found")
        data class TargetNotFound(val targetId: String) : Failure

        @Serializable @SerialName("navigation_denied")
        data class NavigationDenied(val reason: String) : Failure

        @Serializable @SerialName("navigation_error")
        data class NavigationError(val message: String) : Failure
    }
}

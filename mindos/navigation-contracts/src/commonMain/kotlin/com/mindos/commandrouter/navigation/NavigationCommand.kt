package com.mindos.commandrouter.navigation

import kotlinx.serialization.Serializable

@Serializable
data class NavigationCommand(
    val target: NavigationTarget,
    val parameters: RouteParameters = RouteParameters.Empty,
    val commandId: String
)

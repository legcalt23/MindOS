package com.mindos.commandrouter.navigation

interface Navigator {
    suspend fun navigate(command: NavigationCommand): NavigationResult
}

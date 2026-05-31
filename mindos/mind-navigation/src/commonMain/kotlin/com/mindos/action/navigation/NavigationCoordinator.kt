package com.mindos.action.navigation

/** Typed outcome of a navigation side-effect. Never an exception for expected failures. */
sealed interface NavigationOutcome {
    data object Success : NavigationOutcome
    data class Rejected(val reason: String) : NavigationOutcome
    data class Failed(val message: String) : NavigationOutcome
}

/**
 * **Owner of navigation side-effects** (ZIP03). The executor produces a [MindNavigationCommand];
 * the coordinator performs the actual navigation. Implemented in the app over Decompose's
 * StackNavigation — but this contract has no Decompose/Compose dependency, preserving the rule:
 * router never knows UI, executor never knows Compose, coordinator owns navigation.
 */
interface NavigationCoordinator {
    suspend fun navigate(command: MindNavigationCommand): NavigationOutcome
}

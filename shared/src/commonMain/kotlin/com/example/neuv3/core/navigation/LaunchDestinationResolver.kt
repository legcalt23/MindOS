package com.example.neuv3.core.navigation

import com.example.neuv3.core.session.SessionRepository

class LaunchDestinationResolver(
    private val sessionRepository: SessionRepository,
) {
    /**
     * Initial launch destination: Main if there is a persisted user session, otherwise Auth.
     * Kept as a distinct entry point from [resolvePostLoginRoute] so future onboarding /
     * first-run / deeplink logic can diverge without touching call sites.
     */
    fun resolveInitialRoute(): AppRoute = currentMainRouteOrAuth()

    /**
     * Destination right after a successful sign-in / sign-up / element selection. Returns
     * Auth defensively if the session repository does not yet observe the new user.
     */
    fun resolvePostLoginRoute(): AppRoute = currentMainRouteOrAuth()

    private fun currentMainRouteOrAuth(): AppRoute =
        sessionRepository.currentUser()
            ?.let(AppRoute::Main)
            ?: AppRoute.Auth
}

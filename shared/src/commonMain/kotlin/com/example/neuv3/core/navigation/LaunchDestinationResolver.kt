package com.example.neuv3.core.navigation

import com.example.neuv3.core.session.SessionRepository

class LaunchDestinationResolver(
    private val sessionRepository: SessionRepository,
) {
    fun resolveInitialRoute(): AppRoute {
        return sessionRepository.currentUser()
            ?.let(AppRoute::Main)
            ?: AppRoute.Auth
    }

    fun resolvePostLoginRoute(): AppRoute {
        return sessionRepository.currentUser()
            ?.let(AppRoute::Main)
            ?: AppRoute.Auth
    }
}

package com.example.mindos.core.navigation

import com.example.mindos.core.session.AuthenticatedUser

sealed interface AppRoute {
    data object Loading : AppRoute
    data object Auth : AppRoute
    data object ElementSelection : AppRoute
    data class Main(
        val user: AuthenticatedUser,
        val initialProfileId: String? = null,
        val initialProfileHandle: String? = null,
        val initialProfileName: String? = null,
    ) : AppRoute
    data class GlobalFeed(val user: AuthenticatedUser) : AppRoute
    data class Settings(val user: AuthenticatedUser) : AppRoute
    data class MediaViewer(val user: AuthenticatedUser, val content: com.example.mindos.features.social.domain.model.AppMediaContent) : AppRoute
    data class DMList(val user: AuthenticatedUser) : AppRoute
    data class DMChat(val user: AuthenticatedUser, val chatId: String, val otherUserId: String) : AppRoute
    data class DMChatUserInfo(
        val user: AuthenticatedUser,
        val chatId: String,
        val otherUserId: String,
        val isOnline: Boolean = true,
    ) : AppRoute
    data class DMCajonera(
        val user: AuthenticatedUser,
        val chatId: String? = null,
        val otherUserId: String? = null,
        val isOnline: Boolean = true,
    ) : AppRoute

    /** Virtual currency wallet — sandbox mode only. */
    data class Moneda(val user: AuthenticatedUser) : AppRoute
}

package com.example.mindos.core.navigation

import com.example.mindos.core.session.AuthenticatedUser

/**
 * Returns the equivalent of this route bound to a new authenticated user.
 *
 * Used by the auth-state observer to keep the user travelling through screens
 * that require authentication when the session refreshes. Routes that do not
 * depend on a user (e.g. [AppRoute.Auth], [AppRoute.Loading]) are NOT handled
 * by this function — call sites must short-circuit to [AppRoute.Auth] when
 * [user] is null before invoking this.
 *
 * For routes that don't have a one-to-one rebinding (e.g. ElementSelection),
 * we conservatively fall back to [AppRoute.Main] for the new user.
 */
internal fun AppRoute.rebindWithUser(user: AuthenticatedUser): AppRoute = when (this) {
    is AppRoute.GlobalFeed -> AppRoute.GlobalFeed(user)
    is AppRoute.MediaViewer -> AppRoute.MediaViewer(user, content)
    is AppRoute.DMChat -> AppRoute.DMChat(user, chatId, otherUserId)
    is AppRoute.DMChatUserInfo -> AppRoute.DMChatUserInfo(
        user = user,
        chatId = chatId,
        otherUserId = otherUserId,
        isOnline = isOnline,
    )
    is AppRoute.DMCajonera -> AppRoute.DMCajonera(
        user = user,
        chatId = chatId,
        otherUserId = otherUserId,
        isOnline = isOnline,
    )
    is AppRoute.Moneda -> AppRoute.Moneda(user)
    is AppRoute.Settings -> AppRoute.Settings(user)
    is AppRoute.DMList -> AppRoute.DMList(user)
    is AppRoute.Main -> AppRoute.Main(
        user = user,
        initialProfileId = initialProfileId,
        initialProfileHandle = initialProfileHandle,
        initialProfileName = initialProfileName,
    )
    AppRoute.Loading, AppRoute.Auth, AppRoute.ElementSelection -> AppRoute.Main(user)
}

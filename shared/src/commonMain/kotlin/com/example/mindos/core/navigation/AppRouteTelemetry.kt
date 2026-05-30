package com.example.mindos.core.navigation

/**
 * Stable, PII-free name for an [AppRoute] suitable for analytics, breadcrumbs
 * and Crashlytics custom keys.
 *
 * Rules:
 *  - Match the sealed-class name verbatim — easy to grep in Crashlytics.
 *  - NEVER include the [com.example.mindos.core.session.AuthenticatedUser].
 *    The uid is PII; if the reporter needs it, hash it at the platform layer.
 *  - NEVER include free-form fields (chat ids, profile handles). Those are
 *    high-cardinality and useless for crash bucketing.
 */
fun AppRoute.telemetryName(): String = when (this) {
    AppRoute.Loading -> "loading"
    AppRoute.Auth -> "auth"
    AppRoute.ElementSelection -> "element_selection"
    is AppRoute.Main -> "main"
    is AppRoute.GlobalFeed -> "global_feed"
    is AppRoute.Settings -> "settings"
    is AppRoute.MediaViewer -> "media_viewer"
    is AppRoute.DMList -> "dm_list"
    is AppRoute.DMChat -> "dm_chat"
    is AppRoute.DMChatUserInfo -> "dm_chat_user_info"
    is AppRoute.DMCajonera -> "dm_cajonera"
    is AppRoute.Moneda -> "moneda"
}

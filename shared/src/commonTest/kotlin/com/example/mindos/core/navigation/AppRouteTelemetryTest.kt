package com.example.mindos.core.navigation

import com.example.mindos.core.session.AuthenticatedUser
import com.example.mindos.features.social.domain.model.AppMediaContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppRouteTelemetryTest {

    private val user = AuthenticatedUser(
        uid = "uid_secret_value",
        email = "leak@example.com",
        displayName = "Leaky Display",
        isEmailVerified = true,
    )

    @Test
    fun telemetryName_isStableAndLowercase_snakeCase() {
        // Pinning the names: changing these would break Crashlytics dashboards
        // / saved searches keyed on them.
        assertEquals("loading", AppRoute.Loading.telemetryName())
        assertEquals("auth", AppRoute.Auth.telemetryName())
        assertEquals("element_selection", AppRoute.ElementSelection.telemetryName())
        assertEquals("main", AppRoute.Main(user).telemetryName())
        assertEquals("global_feed", AppRoute.GlobalFeed(user).telemetryName())
        assertEquals("settings", AppRoute.Settings(user).telemetryName())
        assertEquals("dm_list", AppRoute.DMList(user).telemetryName())
        assertEquals("dm_chat", AppRoute.DMChat(user, "c", "o").telemetryName())
        assertEquals("dm_chat_user_info", AppRoute.DMChatUserInfo(user, "c", "o").telemetryName())
        assertEquals("dm_cajonera", AppRoute.DMCajonera(user).telemetryName())
        assertEquals("moneda", AppRoute.Moneda(user).telemetryName())
    }

    @Test
    fun telemetryName_neverLeaksPii() {
        // No route's telemetry name may contain the user's uid, email, or display name,
        // nor any free-form field (chat id, profile handle). This is the contract that
        // makes the telemetry safe to ship to Crashlytics. We use realistic PII tokens
        // (>=4 chars, non-dictionary) so that the substring check doesn't false-positive
        // on legitimate route words like "chat" or "cajonera".
        val piiCandidates = listOf(
            user.uid, user.email!!, user.displayName!!,
            "secret_chat_id", "leaky_handle", "another_secret_uid",
        )
        val sampleRoutes = listOf<AppRoute>(
            AppRoute.Main(user, initialProfileId = "leaky_handle", initialProfileName = "Leaky"),
            AppRoute.GlobalFeed(user),
            AppRoute.DMChat(user, "secret_chat_id", "another_secret_uid"),
            AppRoute.DMChatUserInfo(user, "secret_chat_id", "another_secret_uid"),
            AppRoute.DMCajonera(user, "secret_chat_id", "another_secret_uid"),
            AppRoute.MediaViewer(
                user,
                AppMediaContent(
                    url = "https://example.com/secret.jpg",
                    storagePath = "media/secret",
                    fileName = "secret.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 1L,
                ),
            ),
        )
        for (route in sampleRoutes) {
            val name = route.telemetryName()
            for (pii in piiCandidates) {
                assertFalse(
                    name.contains(pii),
                    "telemetryName() for ${route::class.simpleName} leaks '$pii' → '$name'",
                )
            }
            // And it must be a stable identifier, not a free-form description.
            assertTrue(
                name.matches(Regex("[a-z_]+")),
                "telemetryName() must be snake_case lowercase, got '$name'",
            )
        }
    }
}

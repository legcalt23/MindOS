package com.mindos.app.core.navigation

import com.mindos.app.core.session.AuthenticatedUser
import com.mindos.app.features.social.domain.model.AppMediaContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppRouteRebinderTest {

    private val originalUser = AuthenticatedUser(
        uid = "uid_a",
        email = "a@x.com",
        displayName = "Alice",
        isEmailVerified = true,
    )
    private val newUser = AuthenticatedUser(
        uid = "uid_b",
        email = "b@x.com",
        displayName = "Bob",
        isEmailVerified = true,
    )

    @Test
    fun mainRoute_preservesProfileContext() {
        val original = AppRoute.Main(
            user = originalUser,
            initialProfileId = "p1",
            initialProfileHandle = "@handle",
            initialProfileName = "Name",
        )
        val rebound = original.rebindWithUser(newUser)
        assertIs<AppRoute.Main>(rebound)
        assertEquals(newUser, rebound.user)
        assertEquals("p1", rebound.initialProfileId)
        assertEquals("@handle", rebound.initialProfileHandle)
        assertEquals("Name", rebound.initialProfileName)
    }

    @Test
    fun globalFeed_isReboundToNewUser() {
        val rebound = AppRoute.GlobalFeed(originalUser).rebindWithUser(newUser)
        assertIs<AppRoute.GlobalFeed>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun settings_isReboundToNewUser() {
        val rebound = AppRoute.Settings(originalUser).rebindWithUser(newUser)
        assertIs<AppRoute.Settings>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun dmList_isReboundToNewUser() {
        val rebound = AppRoute.DMList(originalUser).rebindWithUser(newUser)
        assertIs<AppRoute.DMList>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun dmChat_preservesChatIdAndOtherUserId() {
        val original = AppRoute.DMChat(originalUser, chatId = "chat-1", otherUserId = "other-1")
        val rebound = original.rebindWithUser(newUser)
        assertIs<AppRoute.DMChat>(rebound)
        assertEquals(newUser, rebound.user)
        assertEquals("chat-1", rebound.chatId)
        assertEquals("other-1", rebound.otherUserId)
    }

    @Test
    fun dmChatUserInfo_preservesAllFields() {
        val original = AppRoute.DMChatUserInfo(
            user = originalUser,
            chatId = "chat-x",
            otherUserId = "other-x",
            isOnline = false,
        )
        val rebound = original.rebindWithUser(newUser)
        assertIs<AppRoute.DMChatUserInfo>(rebound)
        assertEquals(newUser, rebound.user)
        assertEquals("chat-x", rebound.chatId)
        assertEquals("other-x", rebound.otherUserId)
        assertEquals(false, rebound.isOnline)
    }

    @Test
    fun dmCajonera_preservesNullableChatFields() {
        val withFields = AppRoute.DMCajonera(originalUser, chatId = "c", otherUserId = "o", isOnline = true)
        val reboundWith = withFields.rebindWithUser(newUser)
        assertIs<AppRoute.DMCajonera>(reboundWith)
        assertEquals("c", reboundWith.chatId)
        assertEquals("o", reboundWith.otherUserId)

        val withoutFields = AppRoute.DMCajonera(originalUser)
        val reboundWithout = withoutFields.rebindWithUser(newUser)
        assertIs<AppRoute.DMCajonera>(reboundWithout)
        assertEquals(null, reboundWithout.chatId)
        assertEquals(null, reboundWithout.otherUserId)
    }

    @Test
    fun moneda_isReboundToNewUser() {
        val rebound = AppRoute.Moneda(originalUser).rebindWithUser(newUser)
        assertIs<AppRoute.Moneda>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun mediaViewer_preservesContent() {
        val content = AppMediaContent(
            url = "https://example.com/img.jpg",
            storagePath = "media/img.jpg",
            fileName = "img.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 12345L,
        )
        val rebound = AppRoute.MediaViewer(originalUser, content).rebindWithUser(newUser)
        assertIs<AppRoute.MediaViewer>(rebound)
        assertEquals(newUser, rebound.user)
        assertEquals(content, rebound.content)
    }

    @Test
    fun loading_fallsBackToMain() {
        val rebound = AppRoute.Loading.rebindWithUser(newUser)
        assertIs<AppRoute.Main>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun auth_fallsBackToMain() {
        // Defensive: the rebinder shouldn't be called when user is null,
        // but if called from Auth with a fresh user it lands on Main.
        val rebound = AppRoute.Auth.rebindWithUser(newUser)
        assertIs<AppRoute.Main>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun elementSelection_fallsBackToMain() {
        val rebound = AppRoute.ElementSelection.rebindWithUser(newUser)
        assertIs<AppRoute.Main>(rebound)
        assertEquals(newUser, rebound.user)
    }

    @Test
    fun rebindingIsIdempotent_forSameUser() {
        val original = AppRoute.GlobalFeed(originalUser)
        val rebound = original.rebindWithUser(originalUser)
        assertEquals(original, rebound)
    }

    @Test
    fun allRouteVariants_returnSameRouteClass_exceptUnauthenticatedRoutes() {
        // Routes that carry user → same class
        val routesPreservingClass = listOf<AppRoute>(
            AppRoute.Main(originalUser),
            AppRoute.GlobalFeed(originalUser),
            AppRoute.Settings(originalUser),
            AppRoute.DMList(originalUser),
            AppRoute.DMChat(originalUser, "c", "o"),
            AppRoute.DMChatUserInfo(originalUser, "c", "o"),
            AppRoute.DMCajonera(originalUser),
            AppRoute.Moneda(originalUser),
        )
        for (route in routesPreservingClass) {
            val rebound = route.rebindWithUser(newUser)
            assertTrue(
                route::class == rebound::class,
                "Route ${route::class.simpleName} should rebind to same class, got ${rebound::class.simpleName}",
            )
        }
    }
}

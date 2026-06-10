package com.mindos.app.core.navigation

import com.mindos.app.core.session.AuthenticatedUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppRouteStackTest {

    private val user = AuthenticatedUser(
        uid = "u1",
        email = "a@x.com",
        displayName = "Alice",
        isEmailVerified = true,
    )
    private val newUser = AuthenticatedUser(
        uid = "u2",
        email = "b@x.com",
        displayName = "Bob",
        isEmailVerified = true,
    )

    @Test
    fun initialStateHasOneEntryAndCannotGoBack() {
        val stack = AppRouteStack(AppRoute.Auth)
        assertEquals(AppRoute.Auth, stack.current)
        assertEquals(1, stack.size)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun pushIncreasesDepthAndUpdatesCurrent() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.GlobalFeed(user))
        assertEquals(2, stack.size)
        assertIs<AppRoute.GlobalFeed>(stack.current)
        assertTrue(stack.canGoBack)
    }

    @Test
    fun pushIsIdempotentForIdenticalTop() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.GlobalFeed(user))
        stack.push(AppRoute.GlobalFeed(user))
        assertEquals(2, stack.size, "Pushing the same route twice must not duplicate")
    }

    @Test
    fun popReturnsToPreviousRoute() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        stack.push(AppRoute.DMChat(user, "chat-1", "other-1"))
        val popped = stack.pop()
        assertTrue(popped)
        assertIs<AppRoute.DMList>(stack.current)
    }

    @Test
    fun popReturnsFalseWhenAtRoot() {
        val stack = AppRouteStack(AppRoute.Auth)
        assertFalse(stack.pop())
        assertEquals(AppRoute.Auth, stack.current)
    }

    @Test
    fun replaceSwapsTopWithoutChangingDepth() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        stack.replace(AppRoute.GlobalFeed(user))
        assertEquals(2, stack.size)
        assertIs<AppRoute.GlobalFeed>(stack.current)
    }

    @Test
    fun replaceAllClearsAndSeeds() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        stack.push(AppRoute.DMChat(user, "c", "o"))
        stack.replaceAll(AppRoute.Auth)
        assertEquals(1, stack.size)
        assertEquals(AppRoute.Auth, stack.current)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun popToReturnsToMatchingRoute() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        stack.push(AppRoute.DMChat(user, "c", "o"))
        stack.push(AppRoute.DMChatUserInfo(user, "c", "o"))
        val matched = stack.popTo { it is AppRoute.DMList }
        assertTrue(matched)
        assertIs<AppRoute.DMList>(stack.current)
    }

    @Test
    fun popToStopsAtRootIfNothingMatches() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        val matched = stack.popTo { it is AppRoute.GlobalFeed }
        assertFalse(matched)
        assertEquals(1, stack.size, "popTo must not pop below the root")
        assertIs<AppRoute.Main>(stack.current)
    }

    @Test
    fun rebindStackWithUserUpdatesAllEntries() {
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        stack.push(AppRoute.DMChat(user, "c", "o"))
        stack.rebindStackWithUser(newUser)

        val snapshot = stack.snapshot()
        assertEquals(3, snapshot.size)
        for (entry in snapshot) {
            val entryUser = when (entry) {
                is AppRoute.Main -> entry.user
                is AppRoute.DMList -> entry.user
                is AppRoute.DMChat -> entry.user
                else -> error("unexpected route $entry")
            }
            assertEquals(newUser, entryUser)
        }
    }

    @Test
    fun snapshotReturnsImmutableCopy() {
        val stack = AppRouteStack(AppRoute.Auth)
        stack.push(AppRoute.Main(user))
        val snap = stack.snapshot()
        stack.push(AppRoute.DMList(user))
        // Original snapshot stays the same size after further mutations.
        assertEquals(2, snap.size)
        assertEquals(3, stack.size)
    }

    @Test
    fun deepNavigation_thenPop_walksBack() {
        // Simulate Main → DMList → DMChat → DMChatUserInfo → DMCajonera, then pop all the way.
        val stack = AppRouteStack(AppRoute.Main(user))
        stack.push(AppRoute.DMList(user))
        stack.push(AppRoute.DMChat(user, "c", "o"))
        stack.push(AppRoute.DMChatUserInfo(user, "c", "o"))
        stack.push(AppRoute.DMCajonera(user, "c", "o"))

        assertTrue(stack.pop()); assertIs<AppRoute.DMChatUserInfo>(stack.current)
        assertTrue(stack.pop()); assertIs<AppRoute.DMChat>(stack.current)
        assertTrue(stack.pop()); assertIs<AppRoute.DMList>(stack.current)
        assertTrue(stack.pop()); assertIs<AppRoute.Main>(stack.current)
        assertFalse(stack.pop())
    }
}

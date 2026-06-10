package com.mindos.app.core.navigation

import com.mindos.app.core.session.PreviewSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Session-presence routing contract (T5.4).
 *
 * Covers the "no session / expired session → Auth" behaviour that gates the
 * entire app entry. Uses PreviewSessionRepository as a real, in-memory session
 * source (no Firebase).
 */
class LaunchDestinationResolverTest {

    @Test
    fun noSession_resolvesToAuth() {
        val resolver = LaunchDestinationResolver(PreviewSessionRepository(isAuthenticated = false))
        assertEquals(AppRoute.Auth, resolver.resolveInitialRoute())
        assertEquals(AppRoute.Auth, resolver.resolvePostLoginRoute())
    }

    @Test
    fun withSession_resolvesToMainForThatUser() {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        val resolver = LaunchDestinationResolver(repo)

        val initial = assertIs<AppRoute.Main>(resolver.resolveInitialRoute())
        assertEquals(repo.currentUser()?.uid, initial.user.uid)

        val postLogin = assertIs<AppRoute.Main>(resolver.resolvePostLoginRoute())
        assertEquals(repo.currentUser()?.uid, postLogin.user.uid)
    }

    @Test
    fun expiredSession_afterSignOut_resolvesToAuth() = runTest {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        val resolver = LaunchDestinationResolver(repo)
        assertIs<AppRoute.Main>(resolver.resolveInitialRoute())

        // Session ends/expires (signOut clears the current user).
        repo.signOut()

        assertEquals(AppRoute.Auth, resolver.resolveInitialRoute())
        assertEquals(AppRoute.Auth, resolver.resolvePostLoginRoute())
    }
}

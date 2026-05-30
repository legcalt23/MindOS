package com.example.mindos.core.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.mindos.core.session.AuthenticatedUser

/**
 * Observable back stack for [AppRoute] navigation.
 *
 * Semantics:
 *  - [push]: navigate deeper. The current route stays on the stack and becomes the back target.
 *  - [pop]: go back one level. Returns false when there is nothing to pop.
 *  - [replace]: swap the top entry without changing depth. Used for same-level transitions
 *    (e.g. post-login routing, auth-state user rebinding).
 *  - [replaceAll]: clear the stack and seed it with [route]. Used for sign-in/sign-out resets.
 *
 * The stack is backed by a [SnapshotStateList] so Compose recomposes when [current] changes.
 *
 * NOT thread-safe; mutate only from Compose main scope.
 */
@Stable
class AppRouteStack(initial: AppRoute) {

    private val entries: SnapshotStateList<AppRoute> = mutableStateListOf(initial)

    /** The route at the top of the stack. */
    val current: AppRoute
        get() = entries.last()

    /** Whether [pop] would succeed. */
    val canGoBack: Boolean
        get() = entries.size > 1

    /** Current depth (1 for a single entry). */
    val size: Int
        get() = entries.size

    /** Push a new route. No-op if it equals the current top. */
    fun push(route: AppRoute) {
        if (entries.last() != route) entries.add(route)
    }

    /** Pop the top route. Returns true if a pop happened. */
    fun pop(): Boolean {
        if (!canGoBack) return false
        entries.removeAt(entries.size - 1)
        return true
    }

    /** Replace the top entry without changing depth. */
    fun replace(route: AppRoute) {
        entries[entries.size - 1] = route
    }

    /** Clear the stack and seed it with [route]. */
    fun replaceAll(route: AppRoute) {
        entries.clear()
        entries.add(route)
    }

    /**
     * Pop until [predicate] matches the current top or the stack hits depth 1.
     * Returns true if [predicate] matched the final top.
     */
    fun popTo(predicate: (AppRoute) -> Boolean): Boolean {
        while (canGoBack && !predicate(current)) {
            entries.removeAt(entries.size - 1)
        }
        return predicate(current)
    }

    /**
     * Rebind every entry in the stack to a new [user]. Used when [SessionRepository.authState]
     * emits a refreshed user without changing the navigation depth.
     *
     * @see rebindWithUser
     */
    fun rebindStackWithUser(user: AuthenticatedUser) {
        for (index in entries.indices) {
            entries[index] = entries[index].rebindWithUser(user)
        }
    }

    /** Immutable snapshot for testing / debugging. */
    fun snapshot(): List<AppRoute> = entries.toList()
}

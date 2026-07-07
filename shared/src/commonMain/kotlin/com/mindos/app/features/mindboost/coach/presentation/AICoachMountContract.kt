@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.coach.presentation

/**
 * MC8u-G §9 — the AI Coach mount contract. The coach screen ([AICoachScreen] over [AICoachStateHolder]) is a
 * self-contained, standalone Compose surface under `features/mindboost/coach/presentation`. It is NOT yet reachable
 * from live navigation because its natural entry points are foreign-WIP in this worktree:
 *
 *  - `presentation/ui/MindBoostHubScreen.kt` — the MindBoost hub (a new Coach entry card would live here).
 *  - `androidApp/.../MindOSApplication.kt` — registers the real analytics sink + (later) the LLM provider.
 *
 * When those files are free, mounting is: add a Coach entry to the hub that builds `rememberAICoachStateHolder(userId)`
 * and renders [AICoachScreen]. Until then the coach is buildable/testable in isolation (state holder unit tests +
 * a preview host) but must NOT be advertised as reachable.
 *
 * [AICoachMountState] is the single source of truth the app flips to true when the Coach entry is actually wired into
 * navigation. [AICoachMountPolicy] then blocks a user-beta / release build that claims the coach without mounting it —
 * so "user beta ready" can never be declared while the coach is unreachable.
 */
object AICoachMountState {
    /** Flipped to true by the app layer ONLY once a visible Coach entry is wired into navigation. Default: not mounted. */
    const val MOUNTED_IN_NAVIGATION = false
}

object AICoachMountPolicy {
    /** A user-beta build that intends to ship the coach must have a visible entry point wired, not just the module. */
    fun requireMountedForUserBeta(isMounted: Boolean, isUserBetaBuild: Boolean) {
        require(!(isUserBetaBuild && !isMounted)) {
            "AI Coach: a user-beta build must mount a visible Coach entry point (AICoachScreen) into navigation."
        }
    }
}

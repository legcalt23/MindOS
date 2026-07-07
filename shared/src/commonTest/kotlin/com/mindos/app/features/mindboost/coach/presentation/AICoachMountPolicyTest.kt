@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.coach.presentation

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/** MC8u-G §9 — the coach must be mounted into a visible entry point before a user-beta build can claim it. */
class AICoachMountPolicyTest {

    @Test
    fun a_user_beta_build_must_mount_a_visible_entry_point() {
        assertFailsWith<IllegalArgumentException> {
            AICoachMountPolicy.requireMountedForUserBeta(isMounted = false, isUserBetaBuild = true)
        }
    }

    @Test
    fun a_mounted_coach_passes_for_user_beta() {
        AICoachMountPolicy.requireMountedForUserBeta(isMounted = true, isUserBetaBuild = true)
    }

    @Test
    fun a_non_beta_build_is_not_blocked_by_the_mount_gate() {
        AICoachMountPolicy.requireMountedForUserBeta(isMounted = false, isUserBetaBuild = false)
    }

    @Test
    fun the_coach_is_shipped_unmounted_in_this_slice() {
        assertFalse(
            AICoachMountState.MOUNTED_IN_NAVIGATION,
            "MC8u-G ships the coach as a standalone surface; its navigation entry points are foreign-WIP.",
        )
    }
}

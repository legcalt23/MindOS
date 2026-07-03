package com.mindos.app.features.mindboost.creativity.config

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreativityRuntimePolicyTest {

    @Test
    fun allRuntimeCapabilityFlagsAreBlockedInTheLocalMvp() {
        assertFalse(CreativityRuntimePolicy.AI_RUNTIME_ENABLED, "AI runtime must be blocked")
        assertFalse(CreativityRuntimePolicy.NETWORK_SYNC_ENABLED, "Network sync must be blocked")
        assertFalse(CreativityRuntimePolicy.REMOTE_TELEMETRY_ENABLED, "Remote telemetry must be blocked")
    }

    @Test
    fun evidenceGatesAreNotPassed() {
        assertFalse(CreativityRuntimePolicy.EVIDENCE_GATES_PASSED, "A written gate is not a passed gate")
    }

    @Test
    fun moduleReportsAsLocalOnly() {
        assertTrue(CreativityRuntimePolicy.isLocalOnlyMvp)
    }

    @Test
    fun aiSurfaceStaysLockedWhileGatesArePending() {
        assertFalse(CreativityRuntimePolicy.isAiSurfaceUnlocked)
    }

    @Test
    fun requireLocalOnlyDoesNotThrowForShippedConfig() {
        // Fails the test instead of throwing if the invariant ever regresses.
        CreativityRuntimePolicy.requireLocalOnly()
    }
}

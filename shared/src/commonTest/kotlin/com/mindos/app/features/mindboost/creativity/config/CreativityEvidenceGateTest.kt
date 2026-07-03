package com.mindos.app.features.mindboost.creativity.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreativityEvidenceGateTest {

    @Test
    fun everyManifestGateStartsDesignedAndUnpassed() {
        assertEquals(10, CreativityEvidenceGates.all.size, "Manifest defines G01..G10")
        assertTrue(CreativityEvidenceGates.all.all { it.state == CreativityGateState.DESIGNED })
        assertTrue(CreativityEvidenceGates.all.none { it.isPassed })
    }

    @Test
    fun aiRuntimeStaysLockedWhileAnyGateIsPending() {
        assertFalse(CreativityEvidenceGates.aiRuntimeUnlocked)
    }

    @Test
    fun localMvpOfflineReleaseGateIsPresentAndPending() {
        val g10 = CreativityEvidenceGates.byId("G10")
        assertNotNull(g10, "G10 Pre-Gate AI Inactivity must exist")
        assertEquals("Local MVP Offline Release", g10.blocks)
        assertFalse(g10.isPassed)
    }

    @Test
    fun gateCannotBePassedWithoutExecutedEvidence() {
        assertFailsWith<IllegalArgumentException> {
            CreativityEvidenceGate(
                id = "G10",
                title = "Pre-Gate AI Inactivity",
                state = CreativityGateState.PASSED,
                blocks = "Local MVP Offline Release",
                evidenceRef = null,
            )
        }
    }

    @Test
    fun gateMayBePassedOnlyWithAnEvidenceReference() {
        val passed = CreativityEvidenceGate(
            id = "G10",
            title = "Pre-Gate AI Inactivity",
            state = CreativityGateState.PASSED,
            blocks = "Local MVP Offline Release",
            evidenceRef = "log:sha256:deadbeef",
        )
        assertTrue(passed.isPassed)
    }
}

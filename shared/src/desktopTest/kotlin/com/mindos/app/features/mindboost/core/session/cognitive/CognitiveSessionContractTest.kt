package com.mindos.app.features.mindboost.core.session.cognitive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 1 · Charter guard. Pins the fields that are Not Applicable to a cognitive session BY CHARTER,
 * mirroring the Emociones contract. Adding a raw / delta / XP field later must consciously edit this set (and
 * this test), never slip in silently — the removed "loaded gun" stays removed.
 */
class CognitiveSessionContractTest {

    @Test
    fun `the charter-excluded set is exactly the loaded-gun economy fields`() {
        assertEquals(
            setOf("rawScore", "scoreDelta", "xpEarned"),
            CognitiveSessionContract.NOT_APPLICABLE_BY_CHARTER,
        )
    }

    @Test
    fun `each loaded-gun field is pinned as not applicable`() {
        assertTrue("rawScore" in CognitiveSessionContract.NOT_APPLICABLE_BY_CHARTER)
        assertTrue("scoreDelta" in CognitiveSessionContract.NOT_APPLICABLE_BY_CHARTER)
        assertTrue("xpEarned" in CognitiveSessionContract.NOT_APPLICABLE_BY_CHARTER)
    }
}

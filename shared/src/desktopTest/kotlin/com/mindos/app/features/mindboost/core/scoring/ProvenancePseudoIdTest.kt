package com.mindos.app.features.mindboost.core.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Golden + invariants for the shared provenance pseudonymizer. The GOLDEN pin makes the FNV-1a output
 * STABLE across versions: any accidental drift in the constants/algorithm breaks this test (the existing
 * "!= raw uid" provenance assertions would NOT catch a drift). pseudoUserId must stay deterministic and
 * collision-distinct so provenance tags remain consistent and never leak the raw uid.
 */
class ProvenancePseudoIdTest {

    @Test
    fun goldenOutput_isStable() {
        // FNV-1a (offset 0xcbf29ce484222325, prime 0x100000001b3, base-36) of "u1".
        assertEquals("4ssm1ltuo9gz", pseudoUserId("u1"), "pseudoUserId output drifted — provenance tags would change")
    }

    @Test
    fun isDeterministic() {
        assertEquals(pseudoUserId("user-42"), pseudoUserId("user-42"))
    }

    @Test
    fun distinctUsers_distinctTags_andNeverTheRawId() {
        assertNotEquals(pseudoUserId("u1"), pseudoUserId("u2"))
        assertNotEquals("u1", pseudoUserId("u1"))
        assertNotEquals("", pseudoUserId("u1"))
    }
}

package com.mindos.app.features.arena

import com.mindos.app.features.arena.redteam.RedTeamCatalog
import com.mindos.app.features.arena.redteam.ThreatStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 11 / P11.1: The 27-threat red-team catalog is complete and each threat has a test mapping. */
class RedTeamCatalogTest {

    @Test
    fun catalogContainsAll27ThreatsWithUniqueIds() {
        val ids = RedTeamCatalog.THREATS.map { it.id }
        assertEquals(27, RedTeamCatalog.THREATS.size)
        assertEquals(27, ids.toSet().size)
        assertEquals((1..27).map { "RT_%02d".format(it) }.toSet(), ids.toSet())
    }

    @Test
    fun everyThreatDocumentsAttackPreventionExpectedAndTestMapping() {
        for (threat in RedTeamCatalog.THREATS) {
            assertTrue(threat.attack.isNotBlank(), "${threat.id} attack")
            assertTrue(threat.prevention.isNotBlank(), "${threat.id} prevention")
            assertTrue(threat.expectedOutcome.isNotBlank(), "${threat.id} expected")
            assertTrue(threat.mappedTest.isNotBlank(), "${threat.id} mappedTest")
        }
    }

    @Test
    fun mitigatedThreatsMapToASimulationTestAndOnlyDocumentedGapsAreNotModeled() {
        val notModeled = RedTeamCatalog.THREATS.filter { it.status == ThreatStatus.NOT_MODELED }.map { it.id }
        assertEquals(setOf("RT_19", "RT_21"), notModeled.toSet()) // item-pool + ops review queue
        RedTeamCatalog.THREATS.filter { it.status == ThreatStatus.MITIGATED }.forEach {
            assertTrue(it.mappedTest != "NOT_MODELED", "${it.id} should map to a real test")
        }
    }
}

package com.mindos.app.features.mindboost.memory.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemorySessionBlueprintTest {

    @Test
    fun everyBlueprintSumsToTemplateLength() {
        MemorySessionBlueprints.all.forEach { bp ->
            assertEquals(
                bp.template.totalMinutes * 60_000L,
                bp.totalMs,
                "Blueprint ${bp.template} must sum to its template length.",
            )
        }
    }

    @Test
    fun blueprintRejectsWrongSum() {
        assertFailsWith<IllegalArgumentException> {
            MemorySessionBlueprint(
                MemorySessionTemplate.MICRO_5,
                listOf(MemoryPhaseSlot(MemorySessionPhase.WARMUP, 1_000)),
            )
        }
    }

    @Test
    fun blueprintRejectsDuplicatePhase() {
        assertFailsWith<IllegalArgumentException> {
            MemorySessionBlueprint(
                MemorySessionTemplate.MICRO_5,
                listOf(
                    MemoryPhaseSlot(MemorySessionPhase.WARMUP, 150_000),
                    MemoryPhaseSlot(MemorySessionPhase.WARMUP, 150_000),
                ),
            )
        }
    }

    @Test
    fun micro_hasNoInterferenceOrDelayed() {
        val micro = MemorySessionBlueprints.MICRO_5
        assertFalse(micro.hasPhase(MemorySessionPhase.INTERFERENCE))
        assertFalse(micro.hasPhase(MemorySessionPhase.DELAYED_RECALL))
        assertTrue(micro.hasPhase(MemorySessionPhase.DEEP_ENCODING))
    }

    @Test
    fun standardAndFull_haveInterferenceAndDelayed() {
        listOf(MemorySessionBlueprints.STANDARD_10, MemorySessionBlueprints.FULL_20).forEach { bp ->
            assertTrue(bp.hasPhase(MemorySessionPhase.INTERFERENCE), "${bp.template} needs interference.")
            assertTrue(bp.hasPhase(MemorySessionPhase.DELAYED_RECALL), "${bp.template} needs delayed recall.")
        }
    }

    @Test
    fun onlyFullAndIntensive_haveTransfer() {
        assertFalse(MemorySessionBlueprints.MICRO_5.hasPhase(MemorySessionPhase.TRANSFER))
        assertFalse(MemorySessionBlueprints.STANDARD_10.hasPhase(MemorySessionPhase.TRANSFER))
        assertTrue(MemorySessionBlueprints.FULL_20.hasPhase(MemorySessionPhase.TRANSFER))
        assertTrue(MemorySessionBlueprints.INTENSIVE_30.hasPhase(MemorySessionPhase.TRANSFER))
    }

    @Test
    fun forMinutes_picksClosestTemplate() {
        assertEquals(MemorySessionTemplate.MICRO_5, MemorySessionTemplate.forMinutes(4))
        assertEquals(MemorySessionTemplate.FULL_20, MemorySessionTemplate.forMinutes(18))
        assertEquals(MemorySessionTemplate.INTENSIVE_30, MemorySessionTemplate.forMinutes(40))
    }
}

package com.mindos.app.features.mindboost.memory.domain.transfer

import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransferProtocolsTest {

    @Test
    fun hasTenProtocols() {
        assertEquals(10, TransferProtocols.all.size)
    }

    @Test
    fun idsAreUnique() {
        assertEquals(TransferProtocols.all.size, TransferProtocols.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyProtocolLinksAtLeastOneSubcapacity() {
        assertTrue(TransferProtocols.all.all { it.linkedSubcapacities.isNotEmpty() })
    }

    @Test
    fun v1AvailableIsTheContentSupportableSubset() {
        val v1 = TransferProtocols.v1Available.map { it.id }.toSet()
        assertTrue(v1.isNotEmpty())
        assertTrue("study_efficient" in v1)
        assertTrue("language_vocabulary" in v1)
        // Protocols needing later exercises are not yet available.
        assertTrue("work_meetings" !in v1)
        assertTrue("names_faces_context" !in v1)
    }

    @Test
    fun forSubcapacity_returnsLinkedProtocols() {
        val assoc = TransferProtocols.forSubcapacity(MemorySubcapacity.ASSOCIATIVE)
        assertTrue(assoc.all { MemorySubcapacity.ASSOCIATIVE in it.linkedSubcapacities })
        assertTrue(TransferProtocols.VOCABULARY in assoc)
    }

    @Test
    fun blankIdRejected() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            TransferProtocol(" ", "t", setOf(MemorySubcapacity.EPISODIC), "b", "m", false)
        }
    }
}

package com.mindos.app.features.arena

import com.mindos.app.features.arena.validation.ValidationCheckRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 12 / P12.1: The 25-check validation registry is complete. */
class ValidationCheckRegistryTest {

    @Test
    fun registryContainsAll25ChecksWithUniqueIds() {
        val ids = ValidationCheckRegistry.CHECKS.map { it.id }
        assertEquals(25, ValidationCheckRegistry.CHECKS.size)
        assertEquals(25, ids.toSet().size)
        assertEquals((1..25).map { "VAL_%02d".format(it) }.toSet(), ids.toSet())
    }

    @Test
    fun everyCheckHasADescriptionAndOnlyVal25GatesGoLive() {
        assertTrue(ValidationCheckRegistry.CHECKS.all { it.description.isNotBlank() })
        assertEquals(
            setOf("VAL_25"),
            ValidationCheckRegistry.CHECKS.filter { it.blockingBeforeLiveShadow }.map { it.id }.toSet(),
        )
    }
}

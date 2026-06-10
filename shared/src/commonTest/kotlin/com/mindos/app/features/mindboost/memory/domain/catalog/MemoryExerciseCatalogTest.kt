package com.mindos.app.features.mindboost.memory.domain.catalog

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryModule
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the memory exercise catalogue to the functional design (doc §4/§15). The
 * catalogue is the single source of truth, so structural drift must fail loudly.
 */
class MemoryExerciseCatalogTest {

    @Test
    fun catalogue_hasAllSixteenExercises() {
        assertEquals(16, MemoryExerciseCatalog.all.size)
        assertEquals(MemoryExerciseId.entries.size, MemoryExerciseCatalog.all.size)
    }

    @Test
    fun everyExerciseIdHasADefinition() {
        MemoryExerciseId.entries.forEach { id ->
            // definition() throws if missing; also asserts the id is keyed correctly.
            assertEquals(id, MemoryExerciseCatalog.definition(id).id)
        }
    }

    @Test
    fun definitionsAreSelfConsistent() {
        // Each definition's invariants are enforced in its constructor; iterating
        // `all` proves all 16 construct, and re-checks the cross-field contracts.
        MemoryExerciseCatalog.all.values.forEach { def ->
            assertTrue(def.governingAxis in def.relevantAxes, "governingAxis ⊄ relevantAxes: ${def.id}")
            assertTrue(def.governingMetric in def.primaryMetrics, "governingMetric ⊄ primary: ${def.id}")
            assertTrue(def.relevantAxes.isNotEmpty(), "empty relevantAxes: ${def.id}")
            assertTrue(def.primaryMetrics.isNotEmpty(), "empty primaryMetrics: ${def.id}")
            assertTrue(def.displayName.isNotBlank(), "blank displayName: ${def.id}")
        }
    }

    @Test
    fun everyModuleHasAtLeastOneExercise() {
        MemoryModule.entries.forEach { module ->
            assertTrue(
                MemoryExerciseCatalog.byModule(module).isNotEmpty(),
                "Module $module has no exercises.",
            )
        }
    }

    @Test
    fun moduleGroupingMatchesMasterTable() {
        // Spot-check the canonical module assignments from doc §3/§15.
        assertEquals(MemoryModule.M1_EXECUTIVE_WM, MemoryExerciseCatalog.EX1_SPAN_COMPLEJO.module)
        assertEquals(MemoryModule.M2_DEEP_ENCODING, MemoryExerciseCatalog.EX3_LISTA_ESTRATEGIA.module)
        assertEquals(MemoryModule.M3_ASSOCIATIVE, MemoryExerciseCatalog.EX4_PARES_ASOCIADOS.module)
        assertEquals(MemoryModule.M4_SPATIAL_VISUAL, MemoryExerciseCatalog.EX5_GRID_ESPACIAL.module)
        assertEquals(MemoryModule.M5_RESISTANCE, MemoryExerciseCatalog.EX11_INTERFERENCIA.module)
        assertEquals(MemoryModule.M6_PROSPECTIVE_META, MemoryExerciseCatalog.EX10_PROSPECTIVA.module)
    }

    @Test
    fun bySubcapacity_returnsMatchingExercises() {
        val wm = MemoryExerciseCatalog.bySubcapacity(MemorySubcapacity.WORKING_MEMORY)
        assertTrue(wm.all { it.subcapacity == MemorySubcapacity.WORKING_MEMORY })
        assertTrue(MemoryExerciseCatalog.EX1_SPAN_COMPLEJO in wm)
    }

    @Test
    fun implemented_isTheConfirmedV1FirstWave() {
        assertEquals(
            listOf(
                MemoryExerciseId.EX1_SPAN_COMPLEJO,
                MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                MemoryExerciseId.EX4_PARES_ASOCIADOS,
            ),
            MemoryExerciseCatalog.implemented,
        )
    }

    @Test
    fun implemented_areAllInCatalogue() {
        MemoryExerciseCatalog.implemented.forEach { id ->
            assertTrue(MemoryExerciseCatalog.all.containsKey(id), "implemented id not in catalogue: $id")
        }
    }
}

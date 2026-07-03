@file:Suppress("MagicNumber") // Catalogue size (15) is the spec under test.

package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the catalogue ↔ registry ↔ implemented relationship so a finished exercise can never be
 * silently unreachable, and an exposed id can never lack a runnable generator. In Ola 0 `implemented`
 * and the registry are empty; Ola 1 step 1 sets both to [EM1, EM2, EM3] in the same change.
 */
class EmotionExerciseCatalogConsistencyTest {

    @Test
    fun catalogue_covers_15_ids_without_duplicates() {
        assertEquals(15, EmotionExerciseId.entries.size)
        assertEquals(EmotionExerciseId.entries.size, EmotionExerciseCatalog.all.size)
        assertEquals(EmotionExerciseId.entries.toSet(), EmotionExerciseCatalog.all.keys)
    }

    @Test
    fun implemented_equals_registered_generators() {
        assertEquals(
            EmotionExerciseCatalog.implemented.toSet(),
            EmotionExerciseGenerators.registry.keys,
            "Every selectable exercise must have a registered generator and vice versa.",
        )
    }

    @Test
    fun registered_generator_id_matches_its_key() {
        EmotionExerciseGenerators.registry.forEach { (id, generator) ->
            assertEquals(id, generator.exerciseId, "Generator registered under the wrong id.")
        }
    }

    @Test
    fun catalog_only_ids_have_no_generator() {
        val catalogOnly = EmotionExerciseCatalog.all.keys - EmotionExerciseCatalog.implemented.toSet()
        catalogOnly.forEach { id ->
            assertTrue(id !in EmotionExerciseGenerators.registry.keys, "$id is catalog-only but has a generator.")
        }
    }

    @Test
    fun no_duplicate_ids_in_implemented() {
        assertEquals(
            EmotionExerciseCatalog.implemented.size,
            EmotionExerciseCatalog.implemented.toSet().size,
            "Duplicate id in EmotionExerciseCatalog.implemented.",
        )
    }

    @Test
    fun domain_ready_partitions_into_implemented_and_pending() {
        val union = EmotionExerciseCatalog.implemented.toSet() + EmotionExerciseCatalog.pendingUiIntegration.toSet()
        assertEquals(EmotionExerciseCatalog.domainReady.toSet(), union, "domainReady must equal implemented ∪ pending.")
        assertTrue(
            EmotionExerciseCatalog.implemented.none { it in EmotionExerciseCatalog.pendingUiIntegration },
            "An id cannot be both implemented and pending UI integration.",
        )
    }

    @Test
    fun governing_metric_is_a_primary_metric_for_every_definition() {
        EmotionExerciseCatalog.all.values.forEach { def ->
            assertTrue(def.governingMetric in def.primaryMetrics, "${def.id} governing metric not in primaries.")
        }
    }

    @Test
    fun transfer_labels_are_the_honest_pins() {
        assertEquals(TransferLabel.NOT_DEMONSTRATED, EmotionExerciseCatalog.EM3_SEPARADOR.transferLabel)
        assertEquals(TransferLabel.TASK_ONLY, EmotionExerciseCatalog.EM1_PAUSA.transferLabel)
        assertEquals(TransferLabel.PLAUSIBLE, EmotionExerciseCatalog.EM2_RECUPERACION.transferLabel)
    }
}

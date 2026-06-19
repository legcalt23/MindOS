package com.mindos.app.features.mindboost.focus.domain.catalog

import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the registro ↔ implemented ↔ renderer relationship so a finished exercise can never be
 * silently unreachable, and an exposed id can never lack a runnable path.
 *
 * When you finish a new paradigm (generator + renderer + response capture): add its generator to
 * [FocusExerciseGenerators.registry], add its id to [ExerciseCatalog.implemented], and remove it
 * from the `expectedPending` set below. All three moves together keep this suite green; doing only
 * one fails CI — which is the whole point.
 */
class ExerciseCatalogConsistencyTest {

    @Test
    fun every_implemented_id_has_a_catalogue_definition() {
        ExerciseCatalog.implemented.forEach { id ->
            // definition() throws if missing; this pins the catalogue as exhaustive for implemented ids.
            assertEquals(id, ExerciseCatalog.definition(id).id)
        }
    }

    @Test
    fun implemented_set_equals_registered_generators() {
        assertEquals(
            ExerciseCatalog.implemented.toSet(),
            FocusExerciseGenerators.registry.keys,
            "Every selectable exercise must have a registered generator and vice versa.",
        )
    }

    @Test
    fun registered_generator_id_matches_its_key() {
        FocusExerciseGenerators.registry.forEach { (id, generator) ->
            assertEquals(id, generator.exerciseId, "Generator registered under the wrong id.")
        }
    }

    @Test
    fun no_duplicate_ids_in_implemented() {
        assertEquals(
            ExerciseCatalog.implemented.size,
            ExerciseCatalog.implemented.toSet().size,
            "Duplicate id in ExerciseCatalog.implemented.",
        )
    }

    @Test
    fun catalogue_covers_every_exercise_id_without_duplicates() {
        assertEquals(ExerciseId.entries.size, ExerciseCatalog.all.size)
        assertEquals(ExerciseId.entries.toSet(), ExerciseCatalog.all.keys)
    }

    @Test
    fun domain_ready_partitions_into_implemented_and_pending() {
        val union = ExerciseCatalog.implemented.toSet() + ExerciseCatalog.pendingUiIntegration.toSet()
        assertEquals(ExerciseCatalog.domainReady.toSet(), union, "domainReady must equal implemented ∪ pending.")
        assertTrue(
            ExerciseCatalog.implemented.none { it in ExerciseCatalog.pendingUiIntegration },
            "An id cannot be both implemented and pending UI integration.",
        )
    }

    @Test
    fun pending_ui_integration_is_the_explicit_current_delta() {
        // Falla si terminas un generador render-compatible y olvidas exponerlo, o si expones uno sin
        // renderer/respuesta. Vacío: los 13 ejercicios están integrados end-to-end.
        val expectedPending = emptySet<ExerciseId>()
        assertEquals(expectedPending, ExerciseCatalog.pendingUiIntegration.toSet())
    }
}

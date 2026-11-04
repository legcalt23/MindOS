package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every curated probe must map to a single-tap / two-choice exercise that can actually render and score
 * it (PVT→E1, FLANKER→E2, GO_NO_GO→E3). If a future probe used an unsupported paradigm it would fail the
 * `isRenderable` invariant here before it could be scheduled with no renderer.
 */
class ProbeExerciseMappingTest {

    @Test
    fun curatedProbesMapToTheirRenderableExercise() {
        assertEquals(ExerciseId.E1_VIGIA, ProbeExerciseMapping.exerciseFor(ProbeBank.VIGILANCE_PROBE))
        assertEquals(ExerciseId.E2_FILTRO, ProbeExerciseMapping.exerciseFor(ProbeBank.SELECTIVE_PROBE))
        assertEquals(ExerciseId.E3_GATILLO, ProbeExerciseMapping.exerciseFor(ProbeBank.INHIBITION_PROBE))
    }

    @Test
    fun everyCuratedProbeIsRenderable() {
        ProbeBank.all.forEach { probe ->
            assertTrue(ProbeExerciseMapping.isRenderable(probe), "Probe ${probe.probeId} has no renderer.")
            // definitionFor mirrors the mapped exercise's capacity.
            assertEquals(probe.capacity, ProbeExerciseMapping.definitionFor(probe).capacity)
        }
    }

    @Test
    fun everyProbeRendersTheParadigmItDeclares() {
        ProbeBank.all.forEach { probe ->
            val rendered = ProbeExerciseMapping.definitionFor(probe).paradigm
            assertEquals(
                probe.paradigm, rendered,
                "Probe ${probe.probeId} declares ${probe.paradigm} but renders $rendered. The capacity check " +
                    "above structurally cannot see this: a mapping that routes one paradigm to an exercise " +
                    "running another leaves the capacity intact, so relabelling a probe would satisfy a gate " +
                    "asking for a paradigm the catalogue does not train while the user runs the " +
                    "byte-identical trained task (`MB-FOCUS-0123`).",
            )
        }
    }

    @Test
    fun paradigmsTheCatalogueDoesNotTrainFailClosed() {
        // Discriminating half. The guard above only sees paradigms some probe already declares, so it
        // cannot notice a mapping branch standing ready for one that no probe uses yet. Every paradigm the
        // catalogue does not train must resolve to NO renderer, so a probe adopting one is rejected at the
        // mapping instead of being handed a trained exercise.
        val trained = ExerciseCatalog.all.values.map { it.paradigm }.toSet()
        val untrained = Paradigm.entries.toSet() - trained
        assertTrue(untrained.isNotEmpty(), "no untrained paradigm read — taxonomy or catalogue changed")
        untrained.forEach { paradigm ->
            assertNull(
                ProbeExerciseMapping.exerciseFor(ProbeBank.VIGILANCE_PROBE.copy(paradigm = paradigm)),
                "No exercise runs $paradigm, yet the mapping still hands it a renderer.",
            )
        }
    }
}

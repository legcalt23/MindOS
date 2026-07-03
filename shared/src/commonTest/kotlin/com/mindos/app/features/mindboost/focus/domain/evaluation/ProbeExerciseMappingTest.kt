package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
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
}

package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.Capacity
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm

/**
 * Resolves which training-family exercise can *render and score* a reserved probe.
 *
 * A [ProbeDefinition] only declares a [Capacity] + [Paradigm] + reserved seeds — it has no renderer of
 * its own. Each probe paradigm mirrors an existing single-tap / two-choice exercise (PVT→E1, FLANKER→E2,
 * GO_NO_GO→E3), so the live runtime reuses that exercise's deterministic generator (with the probe's
 * RESERVED seed) and its [ExerciseDefinition] (so the metrics engine gates on the right primary metrics).
 *
 * Anti-contamination is preserved: the *seed* is reserved (never trained), even though the *renderer*
 * is shared. Probes are scored with [com.mindos.app.features.mindboost.focus.domain.model.Mode.EVALUATION]
 * (fixed difficulty, no adaptation).
 */
object ProbeExerciseMapping {

    /** The exercise whose generator + definition reproduce [probe]'s paradigm, or null if unsupported. */
    fun exerciseFor(probe: ProbeDefinition): ExerciseId? = when (probe.paradigm) {
        Paradigm.PVT, Paradigm.MACKWORTH_CLOCK -> ExerciseId.E1_VIGIA
        Paradigm.FLANKER -> ExerciseId.E2_FILTRO
        Paradigm.GO_NO_GO -> ExerciseId.E3_GATILLO
        else -> null
    }

    /** True when [probe] has a render/score path (its paradigm maps to a single-tap or two-choice exercise). */
    fun isRenderable(probe: ProbeDefinition): Boolean = exerciseFor(probe) != null

    /** Definition backing [probe]'s metrics; fails fast if a probe is scheduled without a renderer. */
    fun definitionFor(probe: ProbeDefinition): ExerciseDefinition {
        val id = requireNotNull(exerciseFor(probe)) {
            "No renderable exercise for probe ${probe.probeId} (paradigm ${probe.paradigm})."
        }
        return ExerciseCatalog.definition(id)
    }
}

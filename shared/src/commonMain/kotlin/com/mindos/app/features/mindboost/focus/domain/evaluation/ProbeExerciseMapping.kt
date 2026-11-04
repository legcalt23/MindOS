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
 * This mapping is the mechanism that makes a probe a PARALLEL FORM of a trained task rather than an
 * independent instrument: the *seed* is reserved (its items are never trained), but the *paradigm, the
 * generator and the renderer are the trained ones*. Item-level contamination is prevented; paradigm-level
 * practice is not, and no claim above C0 may rest on this (`MB-FOCUS-0089`). Probes are scored with
 * [com.mindos.app.features.mindboost.focus.domain.model.Mode.EVALUATION] (fixed difficulty, no adaptation).
 */
object ProbeExerciseMapping {

    /**
     * The exercise whose generator + definition reproduce [probe]'s paradigm, or null if unsupported.
     *
     * Every branch must reproduce the paradigm it is keyed on. A branch routing paradigm A to an exercise
     * that runs paradigm B renders a task the probe does not declare, and nothing downstream can see it:
     * [definitionFor] is only checked against the probe CAPACITY, which such a pair still satisfies. The
     * `MACKWORTH_CLOCK -> E1_VIGIA` branch removed here was exactly that shape. No probe declared that
     * paradigm, so the branch was dead, but it was armed: relabelling one probe (a single token) would
     * have satisfied a gate asking for a paradigm the catalogue does not train, while the user kept
     * running the byte-identical trained task. Both untrained paradigms now fall through to null and fail
     * closed alike. Pinned by `ProbeExerciseMappingTest` (`MB-FOCUS-0123`).
     */
    fun exerciseFor(probe: ProbeDefinition): ExerciseId? = when (probe.paradigm) {
        Paradigm.PVT -> ExerciseId.E1_VIGIA
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

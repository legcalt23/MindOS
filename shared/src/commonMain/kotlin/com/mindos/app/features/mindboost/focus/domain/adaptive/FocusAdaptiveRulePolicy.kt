package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm

/**
 * Which adaptive rules a given exercise family is allowed to run.
 *
 * A metric allow-list is not enough. `ContainImpulsivity` is **outcome-keyed**: it reads `TrialOutcome` and raw
 * trial RTs directly, so it passes underneath any policy that only governs metric keys. Its construct is a
 * go/no-go **commission error** — failing to withhold a response to a stimulus that demands none.
 *
 * A flanker has no such stimulus: every E2 trial demands a response (`E2FiltroGenerator` sets `isTarget = true`
 * unconditionally). A fast wrong-direction press there is a **choice error**, not a failure of inhibition — and
 * the rule's effect is `stepUp`, so it would *raise* difficulty on a user who is simply erring fast. That is
 * construct contamination with a real cost to the user, which is why applicability is decided by family here
 * rather than inferred from generic outcomes at the call site.
 *
 * Not applicable is not the same as "did not fire": a rule outside its family's competence yields
 * `Hold(NOT_APPLICABLE_TO_FAMILY)` and never touches difficulty.
 */
object FocusAdaptiveRulePolicy {

    /**
     * Rules that must never run outside a go/no-go-shaped paradigm, because their construct does not exist
     * elsewhere. Keyed on the paradigm's shape, not on an exercise id list, so a new exercise inherits the
     * right answer instead of being forgotten.
     */
    private val GO_NO_GO_PARADIGMS = setOf(Paradigm.GO_NO_GO, Paradigm.STOP_SIGNAL, Paradigm.PVT)

    /**
     * True when [exerciseId]'s paradigm actually contains a withhold-response demand, so a commission error is
     * a meaningful construct for it.
     */
    fun allowsImpulsivityContainment(exerciseId: ExerciseId, paradigm: Paradigm): Boolean =
        paradigm in GO_NO_GO_PARADIGMS && exerciseId != ExerciseId.E2_FILTRO
}

package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState

/**
 * E2 Reversibilidad (Decisiones · P2) · Pure bridge from a finished triage run to the domain trials the
 * honest scorer consumes. Each presented card's per-card feedback becomes one [ReversibilityTrial]
 * carrying ONLY the two facts the discrimination signal needs — whether the scenario was actually
 * reversible and what the user chose. RT / lock / delay-penalty are deliberately dropped (they are not
 * part of the profile signal). A future sub-phase appends these trials to the per-user store on session
 * finish; this mapper itself is side-effect-free and unit-testable without persistence.
 */
fun ReversibilityTriageUiState.Finished.toReversibilityTrials(): List<ReversibilityTrial> =
    perCard.map {
        ReversibilityTrial(actuallyReversible = it.actuallyReversible, selectedReversible = it.selectedReversible)
    }

/**
 * Trials from a triage run IF it has finished, else an empty list (Idle/Running/aborted). Lets the live
 * wiring persist exactly once on completion without unsafe casts or persisting a partial run.
 */
fun ReversibilityTriageUiState.reversibilityTrialsOrEmpty(): List<ReversibilityTrial> =
    (this as? ReversibilityTriageUiState.Finished)?.toReversibilityTrials() ?: emptyList()

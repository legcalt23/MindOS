package com.mindos.app.features.mindboost.language.presentation.state

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TraitCode

/**
 * Skill Genome + Evidence Cockpit UI models (v6 §11.2/§11.3). Pure view data. The Genome maps every
 * trait and its evidence state; the Cockpit shows, per measured skill, the honest picture: state,
 * probable range (only when earned), reliability, repaired pattern, the next evidence needed, and the
 * current limitation — never false precision.
 */

/** One node of the Skill Genome (v6 §11.2). [isMeasured] false means "still calibrating". */
data class SkillNodeUiModel(
    val trait: TraitCode,
    val displayName: String,
    val evidenceState: EvidenceState,
    val stateLabel: String,
    val reliabilityBand: ReliabilityBand,
    val isMeasured: Boolean,
)

/** The full Skill Genome map. */
data class SkillGenomeUiModel(val nodes: List<SkillNodeUiModel>)

/** One row of the Evidence Cockpit (v6 §11.3). [probableRange] is null until evidence earns it. */
data class EvidenceCockpitItemUiModel(
    val trait: TraitCode,
    val displayName: String,
    val evidenceState: EvidenceState,
    val stateLabel: String,
    val probableRange: ScoreRange?,
    val reliabilityBand: ReliabilityBand,
    val repairedPattern: String?,
    val nextEvidenceNeeded: String,
    val limitation: String,
)

/** The Evidence Cockpit (measured skills only, strongest first). */
data class EvidenceCockpitUiModel(val items: List<EvidenceCockpitItemUiModel>)

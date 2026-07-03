package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.AbilityPosterior
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.TraitEstimate
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import com.mindos.app.features.mindboost.language.presentation.state.EvidenceCockpitItemUiModel
import com.mindos.app.features.mindboost.language.presentation.state.EvidenceCockpitUiModel
import com.mindos.app.features.mindboost.language.presentation.state.EvidenceCopy
import com.mindos.app.features.mindboost.language.presentation.state.ReliabilityBand
import com.mindos.app.features.mindboost.language.presentation.state.ScoreRange
import com.mindos.app.features.mindboost.language.presentation.state.SkillGenomeUiModel
import com.mindos.app.features.mindboost.language.presentation.state.SkillNodeUiModel

/**
 * Pure builder for the Skill Genome and Evidence Cockpit (v6 §11.2/§11.3). No Compose, no state
 * holder — a deterministic projection from domain types so the honesty rules live in one tested
 * place.
 *
 * Honesty by construction: a [ScoreRange] is produced ONLY from a real posterior whose reliability
 * clears [MIN_RANGE_RELIABILITY]. With the current placeholder θ (low reliability), ranges stay null
 * and the UI shows the state + reliability instead of a fabricated number (§11.6).
 */
object GenomeUiBuilder {

    /** The full genome over [traits] (all traits by default; unmeasured ones read as calibrating). */
    fun skillGenome(
        profile: UserLanguageProfile,
        traits: List<TraitCode> = TraitCode.entries,
    ): SkillGenomeUiModel {
        val nodes = traits.map { trait ->
            val estimate = profile.traitEstimates[trait]
            val state = estimate?.evidenceState ?: EvidenceState.LOWEST
            SkillNodeUiModel(
                trait = trait,
                displayName = trait.display,
                evidenceState = state,
                stateLabel = state.stateLabel,
                reliabilityBand = ReliabilityBand.fromReliability(estimate?.reliability ?: 0.0),
                isMeasured = estimate != null,
            )
        }
        return SkillGenomeUiModel(nodes)
    }

    /** The cockpit over measured skills only, strongest evidence state first. */
    fun evidenceCockpit(
        profile: UserLanguageProfile,
        posteriors: Map<TraitCode, AbilityPosterior> = emptyMap(),
        repairedPatterns: Map<TraitCode, String> = emptyMap(),
    ): EvidenceCockpitUiModel {
        val items = profile.traitEstimates
            .entries
            .sortedByDescending { it.value.evidenceState.rank }
            .map { (trait, estimate) ->
                EvidenceCockpitItemUiModel(
                    trait = trait,
                    displayName = trait.display,
                    evidenceState = estimate.evidenceState,
                    stateLabel = estimate.evidenceState.stateLabel,
                    probableRange = rangeFor(estimate, posteriors[trait]),
                    reliabilityBand = ReliabilityBand.fromReliability(estimate.reliability),
                    repairedPattern = repairedPatterns[trait],
                    nextEvidenceNeeded = EvidenceCopy.nextEvidenceNeeded(estimate.evidenceState),
                    limitation = EvidenceCopy.limitation(estimate.evidenceState),
                )
            }
        return EvidenceCockpitUiModel(items)
    }

    private fun rangeFor(estimate: TraitEstimate, posterior: AbilityPosterior?): ScoreRange? {
        if (posterior == null || estimate.reliability < MIN_RANGE_RELIABILITY) return null
        return ScoreRange(toScore(posterior.credibleInterval90Low), toScore(posterior.credibleInterval90High))
    }

    private fun toScore(theta: Double): Int =
        (SCORE_MIDPOINT + theta * SCORE_PER_LOGIT).coerceIn(0.0, MAX_SCORE).toInt()

    const val MIN_RANGE_RELIABILITY = 0.6
    private const val SCORE_MIDPOINT = 50.0
    private const val SCORE_PER_LOGIT = 15.0
    private const val MAX_SCORE = 100.0
}

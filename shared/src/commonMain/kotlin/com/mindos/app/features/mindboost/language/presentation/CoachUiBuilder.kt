package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.repair.RepairPlan
import com.mindos.app.features.mindboost.language.presentation.state.CoachCardUiModel
import com.mindos.app.features.mindboost.language.presentation.state.EvidenceCopy
import com.mindos.app.features.mindboost.language.presentation.state.SessionFeedbackUiModel

/**
 * Pure builder for the Coach Card and Session Feedback (v6 §11.5). No Compose, no claims invented:
 * the evidence line and next step come from [EvidenceCopy], and the result line is a factual,
 * non-overstated reading of the aggregates.
 */
object CoachUiBuilder {

    /**
     * Builds the post-session card for the trait that was the session's focus. When a [repairPlan]
     * is present, the card names the pattern, the micro-lesson applied, and the isomorphic retry as
     * the next step; otherwise the next step is the next evidence to earn.
     */
    fun coachCard(
        trait: TraitCode,
        summary: SessionSummary,
        evidenceState: EvidenceState,
        repairPlan: RepairPlan? = null,
    ): CoachCardUiModel = CoachCardUiModel(
        trainedSkill = trait.display,
        detectedPattern = repairPlan?.explanation,
        repairApplied = repairPlan?.microLesson,
        result = resultLine(summary),
        evidenceLine = EvidenceCopy.evidenceLine(evidenceState),
        nextStep = repairPlan?.isomorphicRetry ?: EvidenceCopy.nextEvidenceNeeded(evidenceState),
    )

    /** Builds the aggregate feedback panel. Accuracy is null (not zero) when nothing was attempted. */
    fun sessionFeedback(summary: SessionSummary): SessionFeedbackUiModel = SessionFeedbackUiModel(
        headline = "Entrenaste ${summary.itemsAttempted} ítems.",
        accuracyPercent = summary.accuracy()?.percent,
        itemsAttempted = summary.itemsAttempted,
        strongUpdates = summary.strongUpdateCount,
        perTraitLines = summary.perTraitAttempts.entries.map { (trait, attempts) ->
            "${trait.display}: ${summary.perTraitCorrect[trait] ?: 0}/$attempts"
        },
    )

    private fun resultLine(summary: SessionSummary): String = when {
        summary.itemsAttempted == 0 -> "Sesión sin ítems registrados."
        (summary.accuracy()?.value ?: 0.0) >= GOOD_ACCURACY -> "Buen desempeño; conservaste el núcleo."
        else -> "Sesión registrada; hay margen de mejora."
    }

    const val GOOD_ACCURACY = 0.7
}

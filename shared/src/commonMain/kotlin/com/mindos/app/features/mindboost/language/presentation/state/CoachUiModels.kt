package com.mindos.app.features.mindboost.language.presentation.state

/**
 * Coach Card + Session Feedback UI models (v6 §11.5). Pure view data. The Coach Card mirrors what a
 * great human coach would say after a session: what you trained, which pattern showed up, the repair
 * applied, the result, the honest evidence line, and the next step.
 */

/** Actionable post-session coach card (v6 §11.5). [detectedPattern]/[repairApplied] are null when no
 * repair was triggered. */
data class CoachCardUiModel(
    val trainedSkill: String,
    val detectedPattern: String?,
    val repairApplied: String?,
    val result: String,
    val evidenceLine: String,
    val nextStep: String,
)

/** Aggregate session feedback (v6 §6.1 step 10). Built from the aggregates-only [SessionSummary]. */
data class SessionFeedbackUiModel(
    val headline: String,
    val accuracyPercent: Int?,
    val itemsAttempted: Int,
    val strongUpdates: Int,
    val perTraitLines: List<String>,
)

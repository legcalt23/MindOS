package com.mindos.app.features.mindboost.language.presentation.state

/**
 * Next Best Action + Claim-safe Progress UI models (v6 §11.4/§11.6). Pure view data.
 *
 * The Progress model is intentionally minimal: a single honest [headline] plus [caveats] — there is
 * no field for an exact score or percentage, because that copy is forbidden (§11.6) and the headline
 * is always produced by the ClaimEngine, never by the UI.
 */

/** "Why this, why now" recommended action (v6 §11.4 Decision Explanation). */
data class NextBestActionUiModel(
    val sessionTypeLabel: String,
    val focusTrait: String,
    val reasonLines: List<String>,
    val estimatedMinutes: Int?,
)

/** Claim-safe progress headline + caveats (v6 §11.6). Headline is always ClaimEngine-approved copy. */
data class ClaimSafeProgressUiModel(
    val headline: String,
    val caveats: List<String>,
    val evidenceStateLabel: String,
)

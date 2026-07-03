package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel

/**
 * The sober, four-part post-session feedback (decision H). All four parts are ALWAYS rendered together
 * with the [transferLabel] and [confidenceNote] — never collapsed — so the product never over-promises.
 *
 * @param whatHappened neutral description of what occurred in the session.
 * @param whatItMeans what it means WITHIN the training (never a clinical or life claim).
 * @param whatToPractice a concrete next practice cue.
 * @param whatNotToConclude the explicit limits of what can be concluded.
 */
data class FourPartFeedback(
    val whatHappened: String,
    val whatItMeans: String,
    val whatToPractice: String,
    val whatNotToConclude: String,
    val transferLabel: TransferLabel,
    val confidenceNote: String,
)

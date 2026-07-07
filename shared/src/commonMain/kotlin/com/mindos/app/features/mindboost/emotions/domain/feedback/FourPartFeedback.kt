package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel

/**
 * The sober post-session feedback (decision H · rebuilt MC8u P0-1). All parts render together with the
 * [transferLabel] and [confidenceNote] — never collapsed — so the product never over-promises. The measured
 * slots ([whatPerformanceShowed]/[strongSignal]/[weakSignal]) are populated ONLY when the governing metric is
 * confident; otherwise they read as "insufficient data", never a performance judgment. On a safety stop the
 * performance slots are suppressed and [safetyNote] leads.
 *
 * @param whatHappened neutral description of what occurred in the session.
 * @param whatItMeans what it means WITHIN the training (never a clinical or life claim).
 * @param whatTrained the capacity trained this session.
 * @param whatPerformanceShowed the confident, direction-aware reading (or an insufficient-data line).
 * @param strongSignal set only when the confident reading is STRONG; else null.
 * @param weakSignal set only when the confident reading is WEAK; else null.
 * @param whatToPractice a concrete, capacity-specific next practice cue (empty on a safety stop).
 * @param whatNotToConclude the explicit limits of what can be concluded (mandatory, constant).
 * @param nextRecommendedExerciseId the recommended next exercise (always an implemented exercise).
 * @param nextRecommendationReason plain, non-clinical reason for the recommendation.
 * @param safetyNote non-null only on a safety stop; protective, non-clinical.
 * @param confident whether the governing metric produced a trustworthy reading this session (MC8u-B §5): drives the
 *   low-confidence vs calibrating copy split on the result screen. False after a safety stop or an under-powered/gamed
 *   block.
 */
data class FourPartFeedback(
    val whatHappened: String,
    val whatItMeans: String,
    val whatToPractice: String,
    val whatNotToConclude: String,
    val transferLabel: TransferLabel,
    val confidenceNote: String,
    val whatTrained: String = "",
    val whatPerformanceShowed: String = "",
    val strongSignal: String? = null,
    val weakSignal: String? = null,
    val nextRecommendedExerciseId: EmotionExerciseId? = null,
    val nextRecommendationReason: String = "",
    val safetyNote: String? = null,
    val confident: Boolean = false,
)

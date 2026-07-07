@file:Suppress("LongParameterList") // An intents bag for a dumb screen reads best whole.

package com.mindos.app.features.mindboost.coach.presentation.ui

/** MC8u-G — the intents the [AICoachScreen] raises. The state holder owns all logic; the screen is a dumb renderer. */
data class AICoachActions(
    val onGrantConsent: () -> Unit,
    val onShowConsentExplanation: () -> Unit,
    val onDismissConsentExplanation: () -> Unit,
    val onRevokeConsent: () -> Unit,
    val onSend: (String) -> Unit,
    val onViewMemory: () -> Unit,
    val onRequestDeleteMemory: () -> Unit,
    val onConfirmDeleteMemory: () -> Unit,
    val onCancelConfirmation: () -> Unit,
    val onShowRecommendation: () -> Unit,
    val onConfirmApplyRecommendation: () -> Unit,
    val onRejectRecommendation: () -> Unit,
)

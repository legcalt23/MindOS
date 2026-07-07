@file:Suppress("MaxLineLength") // State-model KDoc reads best whole.

package com.mindos.app.features.mindboost.coach.presentation

import com.mindos.app.features.mindboost.coach.application.CoachResponseKind
import com.mindos.app.features.mindboost.coach.domain.CoachPlanSuggestion

/**
 * MC8u-G — the AI Coach product UI state. Every one of the 18 required screen states is a representable (and testable)
 * configuration of this model: the access phase, the conversation transcript, the remaining daily quota, the memory
 * panel, the recommendation panel, and a pending confirmation. The composable is a dumb renderer over this state.
 */
enum class CoachAccessState {
    /** 1. Still loading durable consent/quota/memory. */
    LOADING,

    /** 2. Free user — the coach is Elite; training is unaffected. */
    FREE_LOCKED,

    /** 3. Elite user without IA consent. */
    CONSENT_REQUIRED,

    /** 4. Showing the consent explanation. */
    CONSENT_EXPLANATION,

    /** 5+6. Elite, consented, quota available — the usable coach. */
    READY,

    /** 7. Daily quota reached (training still available). */
    QUOTA_REACHED,
}

/** One line of the coach transcript. A user line has [kind] = null; a coach line carries its [CoachResponseKind]. */
data class CoachMessageUi(val fromUser: Boolean, val kind: CoachResponseKind?, val text: String)

/** The memory panel (§6). */
sealed interface CoachMemoryUi {
    /** 13. No saved summary yet. */
    data object Empty : CoachMemoryUi

    /** 14. A saved summary exists. [cloudActive] is false until Firestore memory is wired (shown as "solo en este dispositivo"). */
    data class Loaded(
        val summary: String,
        val strengths: String,
        val weaknesses: String,
        val recommendations: String,
        val cloudActive: Boolean = false,
    ) : CoachMemoryUi

    /** 15. Memory was just deleted by the user. */
    data object Deleted : CoachMemoryUi
}

/** The recommendation panel (§8). Applying requires explicit confirmation; a pending applier never mutates. */
sealed interface CoachRecommendationUi {
    data object None : CoachRecommendationUi

    /** 16. A suggestion is shown, awaiting confirm/reject. */
    data class Shown(val suggestion: CoachPlanSuggestion) : CoachRecommendationUi

    /** 17a. Applied after confirmation (requires a real applier). */
    data object Applied : CoachRecommendationUi

    /** 17b. Confirmed but the real plan applier is not wired — nothing changed. */
    data object PendingNotWired : CoachRecommendationUi

    /** 18. Rejected — the applier was never called. */
    data object Rejected : CoachRecommendationUi
}

/** A destructive action awaiting confirmation. */
enum class CoachConfirm { DELETE_MEMORY }

data class AICoachUiState(
    val access: CoachAccessState = CoachAccessState.LOADING,
    val messages: List<CoachMessageUi> = emptyList(),
    val remainingQuota: Int = 0,
    val memory: CoachMemoryUi = CoachMemoryUi.Empty,
    val recommendation: CoachRecommendationUi = CoachRecommendationUi.None,
    val confirming: CoachConfirm? = null,
    val isSending: Boolean = false,
) {
    /** 9./10./11./12. — the last coach line (result of the most recent ask), if any. */
    val lastCoachMessage: CoachMessageUi? get() = messages.lastOrNull { !it.fromUser }
}

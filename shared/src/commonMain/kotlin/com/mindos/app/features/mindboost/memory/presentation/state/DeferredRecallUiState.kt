package com.mindos.app.features.mindboost.memory.presentation.state

/**
 * Top-level state of a between-session delayed-recall run (EX3 DR-P2). The flow tests material studied a
 * day earlier WITHOUT re-study: load due items → recognise (multi-select) → scored, honest summary.
 */
sealed interface DeferredRecallUiState {
    data object Loading : DeferredRecallUiState

    /** No registered item is inside its 24h retention window right now. */
    data object NothingDue : DeferredRecallUiState

    /** Recognise the studied words among [candidates] (studied + lures); the user selects what they recall. */
    data class Recall(
        val instruction: String,
        val candidates: List<RecallOptionUi>,
    ) : DeferredRecallUiState

    /** Honest finished summary: [recalled] of [studied] presented words recognised after the delay. */
    data class Finished(
        val recalled: Int,
        val studied: Int,
        val honestNote: String,
    ) : DeferredRecallUiState

    data class Aborted(val reason: String) : DeferredRecallUiState
}

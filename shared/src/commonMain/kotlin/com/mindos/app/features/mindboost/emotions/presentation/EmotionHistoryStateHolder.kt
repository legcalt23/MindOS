package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.emotions.data.local.EmotionRepository

/**
 * Loads the PII-free history/progress model (MC8u-B §4) from the local encrypted repository. Read-only: it never
 * writes, and the model is derived by the pure [EmotionHistoryCardBuilder]. Local-first — no remote sink.
 */
class EmotionHistoryStateHolder(
    private val repository: EmotionRepository,
    private val userId: String,
) {
    var model: EmotionHistoryUiModel by mutableStateOf(EmotionHistoryCardBuilder.empty())
        private set

    suspend fun load() {
        val summaries = repository.recentSummaries(userId)
        val aggregate = repository.readAggregate(userId)
        model = EmotionHistoryCardBuilder.build(summaries, aggregate)
    }
}

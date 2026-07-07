package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalytics
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalyticsEvent
import com.mindos.app.features.mindboost.emotions.analytics.NoOpEmotionAnalytics
import com.mindos.app.features.mindboost.emotions.data.local.EmotionRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the PII-free history/progress model (MC8u-B §4) from the local encrypted repository. Read-only: it never
 * writes, and the model is derived by the pure [EmotionHistoryCardBuilder]. Local-first — no remote sink. Emits a
 * PII-free `emotion_history_viewed` event on load (MC8u-D §3), best-effort — analytics never breaks the surface.
 */
class EmotionHistoryStateHolder(
    private val repository: EmotionRepository,
    private val userId: String,
    private val analytics: EmotionAnalytics = NoOpEmotionAnalytics,
) {
    var model: EmotionHistoryUiModel by mutableStateOf(EmotionHistoryCardBuilder.empty())
        private set

    // MC8u-E §4/§6: the reusable calibration-progress model, now rendered on the block screen (previously dead code).
    var progress: EmotionCalibrationProgressUiModel by mutableStateOf(
        EmotionCalibrationProgressBuilder.fromHistory(emptyList(), null, null),
    )
        private set

    suspend fun load() {
        val summaries = repository.recentSummaries(userId)
        val aggregate = repository.readAggregate(userId)
        model = EmotionHistoryCardBuilder.build(summaries, aggregate)
        progress = EmotionCalibrationProgressBuilder.fromHistory(summaries, aggregate, model.nextSuggestion)
        // Module-level surface: exerciseId is the block tag (never per-exercise, never content).
        runCatching { analytics.log(EmotionAnalyticsEvent.HistoryViewed(HISTORY_SURFACE_TAG)) }
            .exceptionOrNull()?.let { if (it is CancellationException || it is Error) throw it }
    }

    private companion object {
        const val HISTORY_SURFACE_TAG = "EMOTIONS"
    }
}

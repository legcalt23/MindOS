package com.mindos.app.features.mindboost.logic.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.logic.data.local.LocalLogicTrainingRepository
import com.mindos.app.features.mindboost.logic.domain.adaptive.DefaultLogicDifficultyAdapter
import com.mindos.app.features.mindboost.logic.domain.adaptive.DifficultyAdapter
import com.mindos.app.features.mindboost.logic.domain.content.DefaultLogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.content.LogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.feedback.DefaultLogicFeedbackEngine
import com.mindos.app.features.mindboost.logic.domain.feedback.FeedbackEngine
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.logic.domain.scoring.DefaultReasoningScorer
import com.mindos.app.features.mindboost.logic.domain.scoring.ReasoningScorer
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

@Stable
data class LogicGraph(
    val contentProvider: LogicContentProvider,
    val scorer: ReasoningScorer,
    val difficultyAdapter: DifficultyAdapter,
    val feedbackEngine: FeedbackEngine,
    val repository: LogicTrainingRepository,
    val epochClock: EpochClock,
)

@Composable
fun rememberLogicGraph(): LogicGraph {
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Logic)
    return remember(secureStore) {
        LogicGraph(
            contentProvider = DefaultLogicContentProvider(),
            scorer = DefaultReasoningScorer(),
            difficultyAdapter = DefaultLogicDifficultyAdapter(),
            feedbackEngine = DefaultLogicFeedbackEngine(),
            repository = LocalLogicTrainingRepository(secureStore),
            epochClock = SystemEpochClock,
        )
    }
}

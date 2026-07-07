@file:Suppress("MaxLineLength") // Dense DI wiring reads best whole.

package com.mindos.app.features.mindboost.coach.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.coach.analytics.AICoachAnalytics
import com.mindos.app.features.mindboost.coach.data.SecureAICoachConsentStore
import com.mindos.app.features.mindboost.coach.data.SecureAICoachMemoryStore
import com.mindos.app.features.mindboost.coach.data.SecureAICoachQuotaStore
import com.mindos.app.features.mindboost.coach.domain.EliteAccessPolicy
import com.mindos.app.features.mindboost.coach.domain.FreeEliteAccessPolicy
import com.mindos.app.features.mindboost.coach.prompt.AICoachProvider
import com.mindos.app.features.mindboost.coach.prompt.PendingAICoachProvider
import com.mindos.app.features.mindboost.emotions.coach.EmotionCoachContributionSource
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * MC8u-G — production wiring of the AI Coach state holder. Durable consent/quota/memory live in the local encrypted
 * `mindboost_coach` namespace; the first (and, in this slice, only) contribution source is Emociones over its own
 * encrypted namespace. Elite billing and the LLM provider default to their charter stubs (FREE + pending), so the
 * coach is safe and blocked-by-default until the app registers the real ones (behind release guards).
 */
@Composable
fun rememberAICoachStateHolder(
    userId: String,
    elite: EliteAccessPolicy = FreeEliteAccessPolicy,
    provider: AICoachProvider = PendingAICoachProvider,
): AICoachStateHolder {
    val coachStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Coach)
    val emotionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Emotions)
    val scope = rememberCoroutineScope()
    return remember(userId, coachStore, emotionsStore, scope, elite, provider) {
        AICoachStateHolder(
            userId = userId,
            scope = scope,
            sources = listOf(EmotionCoachContributionSource(LocalEmotionRepository(emotionsStore))),
            consentStore = SecureAICoachConsentStore(coachStore),
            quotaStore = SecureAICoachQuotaStore(coachStore),
            memoryStore = SecureAICoachMemoryStore(coachStore),
            clock = SystemEpochClock,
            elite = elite,
            provider = provider,
            analytics = AICoachAnalytics(),
        )
    }
}

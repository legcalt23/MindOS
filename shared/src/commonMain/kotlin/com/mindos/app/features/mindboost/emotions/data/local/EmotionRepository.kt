package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto

/**
 * Local-first, encrypted persistence for emotion training. Only session SUMMARIES and the aggregate
 * are stored (never raw trials/free text). All operations are account-bound and erasable.
 */
interface EmotionRepository {
    suspend fun saveSessionSummary(userId: String, summary: EmotionSessionSummaryDto)
    suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto>
    suspend fun saveAggregate(userId: String, aggregate: EmotionAggregateDto)
    suspend fun readAggregate(userId: String): EmotionAggregateDto?

    /** Erases ALL local emotion data for this device's namespace (account deletion / opt-out). */
    suspend fun deleteAll(userId: String)
}

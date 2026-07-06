package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionPendingTerminalDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto

/**
 * Local-first, encrypted persistence for emotion training. Only session SUMMARIES, the aggregate, and durable
 * "pending terminals" (write-ahead retry markers) are stored — never raw trials/free text. All operations are
 * account-bound and erasable.
 */
interface EmotionRepository {
    suspend fun saveSessionSummary(userId: String, summary: EmotionSessionSummaryDto)
    suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto>
    suspend fun saveAggregate(userId: String, aggregate: EmotionAggregateDto)
    suspend fun readAggregate(userId: String): EmotionAggregateDto?

    /** Durable pending terminal (MC8g): retained until its summary persists so a completed/safety-stop is never
     * lost to a save failure. Keyed by the summary's opaque sessionId. */
    suspend fun savePendingTerminal(userId: String, pending: EmotionPendingTerminalDto)
    suspend fun listPendingTerminals(userId: String): List<EmotionPendingTerminalDto>
    suspend fun deletePendingTerminal(userId: String, sessionId: String)

    /** Erases ALL local emotion data for this device's namespace (account deletion / opt-out). */
    suspend fun deleteAll(userId: String)
}

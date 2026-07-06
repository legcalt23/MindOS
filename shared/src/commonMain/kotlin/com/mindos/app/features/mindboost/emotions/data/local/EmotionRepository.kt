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

    /** Deduplicated by canonical session identity (opaque sessionId, else legacy startedAt) — a session that
     * exists under both a legacy startedAt key and a new sessionId key is returned ONCE. */
    suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto>
    suspend fun saveAggregate(userId: String, aggregate: EmotionAggregateDto)
    suspend fun readAggregate(userId: String): EmotionAggregateDto?

    /** Removes the derived aggregate (MC8i self-heal): used when no completed session remains, so a stale
     * phantom profile from an older/looser version can never survive. No-op if absent. */
    suspend fun deleteAggregate(userId: String)

    /** Durable pending terminal (MC8g): retained until its summary persists so a completed/safety-stop is never
     * lost to a save failure. Keyed by the summary's opaque sessionId. */
    suspend fun savePendingTerminal(userId: String, pending: EmotionPendingTerminalDto)
    suspend fun listPendingTerminals(userId: String): List<EmotionPendingTerminalDto>
    suspend fun deletePendingTerminal(userId: String, sessionId: String)

    /** Erases ALL local emotion data for this device's namespace (account deletion / opt-out). */
    suspend fun deleteAll(userId: String)
}

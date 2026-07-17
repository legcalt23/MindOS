package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId

/** ISP provider: the recent-sessions slice of the history read-model. */
fun interface RecentCognitiveSessionsProvider {
    suspend fun recent(limit: Int): List<CognitiveSessionHistoryItem>
}

/** ISP provider: the per-block history slice of the read-model. */
fun interface BlockSessionHistoryProvider {
    suspend fun byBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionHistoryItem>
}

/** ISP provider: the single-session detail slice of the read-model. */
fun interface CognitiveSessionDetailProvider {
    suspend fun detail(sessionId: String): CognitiveSessionDetail?
}

/**
 * SYSTEM-1 slice 5 · The first OFFICIAL consumer of [CognitiveSessionRun]: a read-only HISTORY read-model over
 * [CognitiveSessionRunStore]. It turns SYSTEM-1 from "just a store" into consumable official history.
 *
 * Strictly a projection: it MUTATES nothing, produces NO `ModuleContribution`, wires NO aggregator, NO sync,
 * NO Firestore, NO outbox, NO recovery — its only collaborator is the run store. It lives in `core` and depends
 * on NO feature (the block taxonomy stays [ScoreModuleId]; profile mapping is the ingestor's job, not here).
 */
interface CognitiveSessionHistoryRepository :
    RecentCognitiveSessionsProvider,
    BlockSessionHistoryProvider,
    CognitiveSessionDetailProvider

/**
 * Default read-model. Ordering, limit semantics (non-positive ⇒ empty, excessive ⇒ capped) and corrupt/orphan
 * skipping for the LIST paths are INHERITED from [CognitiveSessionRunStore.listRecent]/[listByBlock] (index-backed,
 * no mass re-scan). For [detail], a non-`Found` read (missing / corrupt / transient-failed) yields `null` — it
 * never crashes and never fabricates an item.
 */
class DefaultCognitiveSessionHistoryRepository(
    private val store: CognitiveSessionRunStore,
) : CognitiveSessionHistoryRepository {

    override suspend fun recent(limit: Int): List<CognitiveSessionHistoryItem> =
        store.listRecent(limit).map { it.toHistoryItem() }

    override suspend fun byBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionHistoryItem> =
        store.listByBlock(blockId, limit).map { it.toHistoryItem() }

    override suspend fun detail(sessionId: String): CognitiveSessionDetail? =
        when (val read = store.get(sessionId)) {
            is ReadResult.Found -> read.value.toDetail()
            is ReadResult.Missing -> null
            is ReadResult.Corrupt -> null
            is ReadResult.Failed -> null
        }
}

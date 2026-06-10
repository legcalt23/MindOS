package com.mindos.app.features.mindboost.memory.domain.deferred

import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/** Where a registered between-session recall item stands relative to its window. */
enum class DeferredRecallStatus {
    /** Too soon — the 24h retention window has not opened yet. */
    PENDING,

    /** Inside the valid retention window — test it now (no re-study). */
    DUE,

    /** The window was missed (e.g. the user did not open the app in time). */
    EXPIRED,
}

/**
 * Timing window for between-session delayed recall (doc §9). The ideal probe is at
 * ~24h; a tolerance band keeps it valid, and beyond [maxWindowMillis] the measure
 * is no longer a 24h-retention measure (doc §16 Q13). Defaults ARE the data, hence
 * MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
data class DeferredRecallConfig(
    val minDelayMillis: Long = 20L * 3_600_000L, // 20h
    val idealDelayMillis: Long = 24L * 3_600_000L, // 24h
    val maxWindowMillis: Long = 48L * 3_600_000L, // 48h
) {
    init {
        require(minDelayMillis in 0..idealDelayMillis) { "minDelay must be in 0..ideal." }
        require(idealDelayMillis <= maxWindowMillis) { "ideal must be <= maxWindow." }
    }
}

/**
 * Material studied in a prior session, registered so it can be re-tested between
 * sessions WITHOUT re-study (the strongest learning signal, doc §9/§14.2). Pure:
 * time is supplied by the caller (an EpochClock), never read here.
 */
data class DeferredRecallItem(
    val id: String,
    val exerciseId: MemoryExerciseId,
    val language: Language,
    val studiedWordIds: List<WordId>,
    val studiedAtEpoch: Long,
    val config: DeferredRecallConfig = DeferredRecallConfig(),
) {
    init {
        require(id.isNotBlank()) { "DeferredRecallItem id cannot be blank." }
        require(studiedAtEpoch >= 0) { "studiedAtEpoch cannot be negative." }
        require(studiedWordIds.isNotEmpty()) { "A deferred item must reference studied material." }
    }

    fun elapsedMillis(nowEpoch: Long): Long = (nowEpoch - studiedAtEpoch).coerceAtLeast(0)

    fun status(nowEpoch: Long): DeferredRecallStatus {
        val elapsed = elapsedMillis(nowEpoch)
        return when {
            elapsed < config.minDelayMillis -> DeferredRecallStatus.PENDING
            elapsed <= config.maxWindowMillis -> DeferredRecallStatus.DUE
            else -> DeferredRecallStatus.EXPIRED
        }
    }
}

/**
 * Decides which registered material to re-test on app open (doc §9). Pure and
 * stateless: it operates on a list the caller supplies (persistence is Phase 10)
 * and a `nowEpoch` the caller reads from an EpochClock.
 */
class DeferredRecallScheduler {

    /** Factory for a new registration (made when material is encoded). */
    @Suppress("LongParameterList") // Each argument is an intrinsic field of the registered item.
    fun register(
        id: String,
        exerciseId: MemoryExerciseId,
        language: Language,
        studiedWordIds: List<WordId>,
        studiedAtEpoch: Long,
        config: DeferredRecallConfig = DeferredRecallConfig(),
    ): DeferredRecallItem = DeferredRecallItem(id, exerciseId, language, studiedWordIds, studiedAtEpoch, config)

    /** Items inside their valid retention window — present these for delayed recall. */
    fun due(items: List<DeferredRecallItem>, nowEpoch: Long): List<DeferredRecallItem> =
        items.filter { it.status(nowEpoch) == DeferredRecallStatus.DUE }

    /** Items not yet ready (window has not opened). */
    fun pending(items: List<DeferredRecallItem>, nowEpoch: Long): List<DeferredRecallItem> =
        items.filter { it.status(nowEpoch) == DeferredRecallStatus.PENDING }

    /** Items whose window was missed — the caller decides to discard or downgrade them. */
    fun expired(items: List<DeferredRecallItem>, nowEpoch: Long): List<DeferredRecallItem> =
        items.filter { it.status(nowEpoch) == DeferredRecallStatus.EXPIRED }

    /** All items grouped by status — convenient for the session-open routine. */
    fun classify(items: List<DeferredRecallItem>, nowEpoch: Long): Map<DeferredRecallStatus, List<DeferredRecallItem>> =
        items.groupBy { it.status(nowEpoch) }
}

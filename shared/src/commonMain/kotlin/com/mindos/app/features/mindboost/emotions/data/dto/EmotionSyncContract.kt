@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.emotions.data.dto

/**
 * MC8u-D §2/§6 — the Emociones persistence/sync contract. The block is LOCAL-FIRST and encrypted by charter
 * (CLAUDE.md §5): a completed session is durable on-device the instant it is saved, independent of any network.
 * Cloud sync is an OPTIONAL, additive layer on top of that durable local record — never a prerequisite for a
 * session to be safe.
 *
 * This file makes the sync posture EXPLICIT and ENFORCEABLE in code (not just report text):
 *  - [EmotionSyncStatus] is a real first-class field on the summary ([EmotionSessionSummaryDto.syncStatus]).
 *  - [EmotionPlatform] records the device platform on the summary ([EmotionSessionSummaryDto.platform]).
 *  - [EmotionCloudSyncSink] is the boundary a real Firestore/backend sync must implement.
 *  - [EmotionSyncPolicy] is a release-blocking guard: IF a build declares cloud sync required, it must wire a real
 *    sink (not the no-op) — otherwise startup fails fast. A build that does NOT require cloud sync (single-device
 *    MVP, charter local-first) legitimately keeps the no-op and every session stays [EmotionSyncStatus.LOCAL_ONLY].
 */
object EmotionSyncStatus {
    /** Durable on-device only; no cloud copy exists (the charter default). */
    const val LOCAL_ONLY = "LOCAL_ONLY"

    /** Saved locally and queued for cloud sync; not yet confirmed remote. */
    const val PENDING_SYNC = "PENDING_SYNC"

    /** Confirmed persisted to the cloud backend. */
    const val SYNCED = "SYNCED"

    /** A cloud-sync attempt failed; the local record is intact and a retry is expected. */
    const val SYNC_FAILED = "SYNC_FAILED"

    val ALL: Set<String> = setOf(LOCAL_ONLY, PENDING_SYNC, SYNCED, SYNC_FAILED)

    fun isKnown(status: String): Boolean = status in ALL
}

/** The device platform a session was recorded on (PRD requires it; UNKNOWN until the app injects the real value). */
object EmotionPlatform {
    const val UNKNOWN = "UNKNOWN"
    const val ANDROID = "ANDROID"
    const val IOS = "IOS"
    const val DESKTOP = "DESKTOP"

    val ALL: Set<String> = setOf(UNKNOWN, ANDROID, IOS, DESKTOP)
}

/**
 * The boundary a real cloud-sync implementation (Firestore/Cloud Function) must satisfy. It is DEFINED here (in the
 * module) so the contract is enforceable, but a concrete implementation is app-level and out of the emotions
 * module scope. A failed push must NEVER delete or corrupt the local record — it flips the summary to
 * [EmotionSyncStatus.SYNC_FAILED] and emits `emotion_sync_failed` for observability.
 */
interface EmotionCloudSyncSink {
    /** Attempts to push a durable local summary to the cloud. Returns the resulting status; never throws for a
     * normal network failure (returns [EmotionSyncStatus.SYNC_FAILED]). Local data is never mutated by the caller. */
    suspend fun push(userId: String, summary: EmotionSessionSummaryDto): String
}

/** No-op sync sink: every session stays [EmotionSyncStatus.LOCAL_ONLY]. The charter default for a single-device MVP. */
object NoOpEmotionCloudSync : EmotionCloudSyncSink {
    override suspend fun push(userId: String, summary: EmotionSessionSummaryDto): String = EmotionSyncStatus.LOCAL_ONLY
}

/**
 * MC8u-D §2/§6 release-blocking guard. A build that DECLARES cloud sync required (`cloudSyncRequired = true`) must
 * wire a real [EmotionCloudSyncSink], not [NoOpEmotionCloudSync] — otherwise this fails fast at startup. A build that
 * does not require cloud sync (charter local-first, single-device MVP) legitimately keeps the no-op. This converts
 * "sync is just report text" into an enforceable contract without forcing cloud sync onto the local-first charter.
 */
object EmotionSyncPolicy {
    fun requireCloudSyncWiredWhenRequired(sink: EmotionCloudSyncSink, cloudSyncRequired: Boolean) {
        require(!(cloudSyncRequired && sink === NoOpEmotionCloudSync)) {
            "Emociones sync: this build declares cloud sync required but wired NoOpEmotionCloudSync (no real backend)."
        }
    }
}

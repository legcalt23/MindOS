package com.mindos.app.features.mindboost.focus2.data.session

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus2.domain.runtime.EngineVersions
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeSnapshot
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionDescriptor
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionRestore
import com.mindos.app.features.mindboost.focus2.domain.runtime.RestoreOutcome
import com.mindos.app.features.mindboost.focus2.domain.runtime.RestoreRejection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Focus V5 · S3 · tarea (4) — WRITE-AHEAD persistence for a running session, on the `mindboost_focus2`
 * namespace.
 *
 * "Write-ahead" is the whole design: the seal is written BEFORE the first trial and each checkpoint is written
 * BEFORE the run continues past it, so a process death can only ever lose work that had not happened yet. The
 * opposite order — play first, persist at the end — loses the entire session to a phone call.
 *
 * Two rules from the repo's history are load-bearing here:
 *
 *  - **A necessary write never returns `Unit`.** Every write returns a typed [FocusWriteOutcome], because a
 *    write-ahead whose failure is swallowed reports a session as recoverable when nothing was recorded
 *    (learning 2026-07-14). The caller must not start the first trial until the seal came back `Stored`.
 *  - **A failed read is never "absent".** `Missing` and `Failure` map to DIFFERENT rejections, and nothing here
 *    calls `quarantine`: quarantine DELETES, and an in-flight session is not regenerable — a transient backend
 *    fault would destroy the only copy of a run in progress (learnings 2026-07-05 and 2026-07-20).
 */
class SecureFocusSessionStore(
    private val store: SecureKeyValueStore,
    private val versions: EngineVersions = EngineVersions.CURRENT,
) {

    /**
     * Writes the pre-trial seal. The caller MUST NOT present a trial unless this returns
     * [FocusWriteOutcome.Stored].
     */
    suspend fun sealDescriptor(userId: String, descriptor: FocusSessionDescriptor): FocusWriteOutcome =
        write(descriptorKey(userId, descriptor.sessionId), json.encodeToString(descriptor))

    /** Records progress. Called at each checkpoint, before the run moves past it. */
    suspend fun checkpoint(userId: String, snapshot: FocusRuntimeSnapshot): FocusWriteOutcome =
        write(snapshotKey(userId, snapshot.sessionId), json.encodeToString(snapshot))

    /**
     * Reads both records back and delegates the verdict to [FocusSessionRestore].
     *
     * The mapping from read result to rejection happens HERE rather than by handing `null` upward, because a
     * store that reports a decryption failure as "no record" would let a damaged session look like one that
     * never started — and the two need different responses from an operator reading a bug report.
     */
    suspend fun restore(userId: String, sessionId: String): RestoreOutcome {
        val descriptor = readOrNull(descriptorKey(userId, sessionId))
        val snapshot = readOrNull(snapshotKey(userId, sessionId))
        return when {
            descriptor is StoredText.Damaged ->
                RestoreOutcome.Rejected(RestoreRejection.DESCRIPTOR_UNREADABLE)
            snapshot is StoredText.Damaged ->
                RestoreOutcome.Rejected(RestoreRejection.SNAPSHOT_UNREADABLE)
            else -> FocusSessionRestore.restore(descriptor.textOrNull(), snapshot.textOrNull(), versions)
        }
    }

    /**
     * Removes a session's records once it has been sealed into history, or when the user discards it.
     *
     * Both removes run under [NonCancellable], and that is the point of the block rather than an idiom. The two
     * deletes are not atomic: cancelling between them — a composable scope torn down at exactly that moment —
     * would delete the counts and leave the descriptor behind, so "purged" would be true of one record and
     * false of the other. A purge is the one operation that must finish once it has started, because the state
     * it leaves halfway is retention.
     *
     * Best-effort TOTAL, not fail-fast: the second remove is attempted even when the first failed, and the
     * FIRST failure is what surfaces. Aborting on the first error is how an erasure quietly leaves rows behind
     * (learning 2026-07-14).
     */
    suspend fun discard(userId: String, sessionId: String): FocusWriteOutcome = withContext(NonCancellable) {
        val snapshot = remove(snapshotKey(userId, sessionId))
        val descriptor = remove(descriptorKey(userId, sessionId))
        if (snapshot is FocusWriteOutcome.Stored) descriptor else snapshot
    }

    /**
     * The recovery scan: which sessions have a seal on disk for this user.
     *
     * Typed, not a bare list. A scan that cannot read the index must not answer "none" — that reads as "nothing
     * to recover" and quietly abandons every in-flight session on the device.
     */
    suspend fun scanSessionIds(userId: String): SessionScan = try {
        SessionScan.Scanned(
            store.keys(prefix(userId))
                .filter { it.endsWith(DESCRIPTOR_SUFFIX) }
                .map { it.removePrefix(prefix(userId)).removeSuffix(DESCRIPTOR_SUFFIX) }
                .toSet(),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        SessionScan.Failed(error::class.simpleName ?: "unknown")
    }

    private suspend fun write(key: String, value: String): FocusWriteOutcome = try {
        store.put(key, value)
        FocusWriteOutcome.Stored
    } catch (cancellation: CancellationException) {
        // Structured cancellation is not a storage failure and must never be reported as one.
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        FocusWriteOutcome.Failed(error::class.simpleName ?: "unknown")
    }

    private suspend fun remove(key: String): FocusWriteOutcome = try {
        store.remove(key)
        FocusWriteOutcome.Stored
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        FocusWriteOutcome.Failed(error::class.simpleName ?: "unknown")
    }

    private suspend fun readOrNull(key: String): StoredText = when (val result = store.read(key)) {
        SecureReadResult.Missing -> StoredText.Absent
        is SecureReadResult.Failure -> StoredText.Damaged
        is SecureReadResult.Value -> StoredText.Present(result.value)
    }

    private sealed interface StoredText {
        data class Present(val value: String) : StoredText
        data object Absent : StoredText
        data object Damaged : StoredText
    }

    /** Only ever called once [StoredText.Damaged] has been ruled out, so `null` here really does mean absent. */
    private fun StoredText.textOrNull(): String? = (this as? StoredText.Present)?.value

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        const val DESCRIPTOR_SUFFIX = ":descriptor"
        const val SNAPSHOT_SUFFIX = ":snapshot"

        /** Rejects anything that could corrupt the key namespace, `:` above all (it is the key separator). */
        val ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")

        fun prefix(userId: String): String {
            require(ID_REGEX.matches(userId)) { "userId must match $ID_REGEX" }
            return "focus2_session:$userId:"
        }

        fun descriptorKey(userId: String, sessionId: String): String = keyOf(userId, sessionId, DESCRIPTOR_SUFFIX)

        fun snapshotKey(userId: String, sessionId: String): String = keyOf(userId, sessionId, SNAPSHOT_SUFFIX)

        fun keyOf(userId: String, sessionId: String, suffix: String): String {
            require(ID_REGEX.matches(sessionId)) { "sessionId must match $ID_REGEX" }
            return prefix(userId) + sessionId + suffix
        }
    }
}

/**
 * The outcome of a necessary write. Never `Unit`: the caller has to be able to tell "recorded" from "not
 * recorded", because a write-ahead that reports success without writing turns recovery into fiction.
 */
sealed interface FocusWriteOutcome {
    data object Stored : FocusWriteOutcome

    /** [reason] is the exception's type name only — never its message, which can carry user content. */
    data class Failed(val reason: String) : FocusWriteOutcome
}

/** The recovery scan's answer. `Failed` is not an empty set, and callers must not treat it as one. */
sealed interface SessionScan {
    data class Scanned(val sessionIds: Set<String>) : SessionScan
    data class Failed(val reason: String) : SessionScan
}

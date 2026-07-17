package com.mindos.app.features.mindboost.core.session

import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import com.mindos.app.features.mindboost.storage.LegacyNamespacePurge
import com.mindos.app.features.mindboost.storage.MindBoostLegacyPurgeSpecs
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Versioned on-disk shape for a [SessionRecord]. A dedicated DTO (not the domain model) carries
 * [schemaVersion] so migrations stay explicit; [block] and [status] persist as enum names for
 * forward-compatibility. This field set IS the minimization allowlist (master plan 19.4): adding a field is a
 * conscious privacy decision and breaks SessionRecordStoreTest's canary until reviewed.
 */
@Serializable
private data class SessionRecordDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val sessionId: String,
    val block: String,
    val status: String,
    val startedAtMs: Long,
    val updatedAtMs: Long,
    val completedTrials: Int,
    val plannedTrials: Int,
    val activeDurationMs: Long,
    /**
     * ADDITIVE (schema 2): absent in every v1 row, which decodes to null and stays honestly non-reproducible.
     * A reproducible form is never invented for a session that did not record one.
     */
    val reproducibleIdentity: ReproducibleSessionIdentity? = null,
) {
    companion object {
        /** 2 since ENG-01 added [reproducibleIdentity]. Additive: v1 rows still decode (the field defaults null). */
        const val SCHEMA_VERSION = 2
    }
}

/** Delimiter separating the subject from the rest of a key. Reserved: [validateSessionRecordSubject] rejects it. */
private const val SESSION_RECORD_SUBJECT_DELIMITER = ':'

/**
 * SYSTEM-1 slice 6-R3 (P0/P1 fold-in fix) · every physical key [SecureSessionRecordStore] touches is scoped by
 * [subject] so two different subjects sharing the SAME underlying [SecureKeyValueStore] namespace (today's
 * reality: `mindboost_sessions` is one device-wide physical file/backend, NOT a per-account one) can never read,
 * list, or delete one another's rows. [subject] must not contain [SESSION_RECORD_SUBJECT_DELIMITER] (the
 * key-prefix delimiter) — otherwise one subject's prefix could be a prefix of a DIFFERENT subject's keys (e.g.
 * subject `"abc"` vs `"abc:def"`), reopening the isolation gap this fix closes. Mirrors the guard in
 * [com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore].
 */
private fun validateSessionRecordSubject(subject: String) {
    require(subject.isNotBlank()) { "subject must not be blank (a pseudonymous per-account scope label)." }
    require(SESSION_RECORD_SUBJECT_DELIMITER !in subject) {
        "subject must not contain '$SESSION_RECORD_SUBJECT_DELIMITER' (the key-prefix delimiter)."
    }
}

/**
 * Persists [SessionRecord]s as encrypted blobs via [SecureKeyValueStore] (Android Keystore AES-256-GCM;
 * in-memory on desktop/iOS) in the account-bound `mindboost_sessions` namespace. One session is one
 * namespaced key; a corrupt, unknown-enum, or future-schema entry is skipped (quarantined) rather than
 * misread. Mirrors the repo's standard secure-store pattern (see SecureDecisionSessionRepository).
 *
 * SECURITY NOTE (SYSTEM-1 slice 6-R3, §14 fold-in of 6R3-P0C-01's sibling P1): [store] itself is
 * namespace-scoped, NOT account-scoped — one physical backend per namespace, shared by every
 * locally-authenticated account on the device (see
 * [com.mindos.app.features.mindboost.focus.data.local.IndexedSecureKeyValueStore]). [subject] is a MANDATORY,
 * first-class constructor parameter (never a naming convention a caller must remember to apply per-call), so a
 * `SecureSessionRecordStore` instance can only ever touch ITS OWN subject's keys — every physical key is
 * `session_record:<subject>:<sessionId>`.
 *
 * REVOCATION (P2-01 ENMIENDA 2026-07-14): the tombstone that fail-closes a write after account deletion, and
 * that gates reads against a revoked subject's residual rows, now lives in [SubjectRevocationAuthority] over a
 * DEDICATED namespace ([revocationStore] = `mindboost_revocation`), SHARED with
 * [com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore] — one tombstone per
 * subject consulted by both stores, not two independent per-store tombstones. The status is TRIADIC
 * (ACTIVE/REVOKED/STATUS_UNAVAILABLE): a read/write is refused unless the subject is provably ACTIVE, and an
 * UNCONFIRMABLE status (transient tombstone fault) NEVER proceeds to touch payload (closing the prior fail-open
 * exposure) — reads with a failure channel ([findEvidence]) surface it honestly, channel-less reads
 * ([find]/[all]) breadcrumb + return the conservative default (these feed recovery/write-ahead, not the profile).
 */
class SecureSessionRecordStore(
    private val store: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    private val subject: String,
    private val json: Json = defaultJson,
) : SessionRecordStore {

    // SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C1): sweeps pre-6R3 subject-less rows the first time ANY
    // read/list/delete method below runs — see LegacyNamespacePurge's KDoc for why write methods are exempt.
    private val legacyPurge = LegacyNamespacePurge(MindBoostLegacyPurgeSpecs.Sessions)

    // SYSTEM-1 slice 6-R3 (P2-01): THE single per-subject revocation authority over the dedicated
    // `mindboost_revocation` namespace, shared with SecureCognitiveSessionRunStore (same subject → same tombstone
    // + same process-wide lock via SubjectLocks). Multiple instances over the same revocationStore are equivalent.
    private val revocation = SubjectRevocationAuthority(revocationStore)

    init {
        validateSessionRecordSubject(subject)
    }

    /**
     * SYSTEM-1 slice 6-R3 (P2-01): returns a TYPED [SessionRecordUpsertOutcome] — never a silent success. The
     * write runs INSIDE the per-subject lock ([SubjectRevocationAuthority.guardedWrite]) only if the subject is
     * ACTIVE; a REVOKED subject → [SessionRecordUpsertOutcome.RejectedRevoked]; an unconfirmable revocation
     * status → [SessionRecordUpsertOutcome.RetryableFailure]; a genuine backend/Keystore write fault →
     * [SessionRecordUpsertOutcome.RetryableFailure] (never thrown — a caller in a `LaunchedEffect` must not crash).
     */
    override suspend fun upsert(record: SessionRecord): SessionRecordUpsertOutcome {
        require(record.sessionId.isNotBlank()) { "sessionId must not be blank." }
        val encoded = json.encodeToString(SessionRecordDto.serializer(), record.toDto())
        val key = keyFor(record.sessionId)
        val guarded = revocation.guardedWrite(subject) { putOutcome(store, key, encoded) }
        return when (guarded) {
            is GuardedWrite.Written -> guarded.value
            GuardedWrite.SubjectRevoked -> SessionRecordUpsertOutcome.RejectedRevoked
            is GuardedWrite.StatusUnavailable -> SessionRecordUpsertOutcome.RetryableFailure(guarded.reason)
        }
    }

    // Channel-less reads (P2-01 §40.7.8 R1): these feed recovery/write-ahead/history, NOT the cognitive profile,
    // and have no Failed-representable return — so STATUS_UNAVAILABLE breadcrumbs + returns the conservative
    // default (transient, self-correcting) rather than throwing (would crash LaunchedEffect callers) or
    // fabricating a confident absence in a profile path. REVOKED → conservative default (payload never touched).
    override suspend fun find(sessionId: String): SessionRecord? {
        legacyPurge.ensurePurged(store)
        val guarded = revocation.guardedRead(subject) { store.readParsed(keyFor(sessionId), ::parse) }
        return when (guarded) {
            is GuardedRead.Allowed -> guarded.value
            GuardedRead.SubjectRevoked -> null
            is GuardedRead.StatusUnavailable -> {
                breadcrumbUnavailableRead()
                null
            }
        }
    }

    override suspend fun all(): List<SessionRecord> {
        legacyPurge.ensurePurged(store)
        val guarded = revocation.guardedRead(subject) {
            store.keys(prefix())
                .mapNotNull { key -> store.readParsed(key, ::parse) }
                .sortedWith(compareByDescending<SessionRecord> { it.startedAtMs }.thenBy { it.sessionId })
        }
        return when (guarded) {
            is GuardedRead.Allowed -> guarded.value
            GuardedRead.SubjectRevoked -> emptyList()
            is GuardedRead.StatusUnavailable -> {
                breadcrumbUnavailableRead()
                emptyList()
            }
        }
    }

    // A single-key removal (no live production caller today). It never READS payload so it cannot leak, but it runs
    // under the per-subject lock so it can never interleave a concurrent deleteAll's hold-through-erase
    // (single-authority hygiene). It deliberately does NOT gate on revocation status — a delete is always safe.
    override suspend fun delete(sessionId: String) {
        legacyPurge.ensurePurged(store)
        revocation.withSubjectLock(subject) { store.remove(keyFor(sessionId)) }
    }

    /**
     * Channelled read (P2-01 §40.7.8 R1): [SessionRecordRead] HAS a failure variant, so STATUS_UNAVAILABLE maps
     * honestly to [SessionRecordRead.ReadFailed] (retryable, never a fabricated absence — `null≠failure`), and a
     * REVOKED subject maps to [SessionRecordRead.Missing]. The purge is a narrow, idempotent side effect on a
     * reserved infra key, never on a SessionRecord.
     */
    override suspend fun findEvidence(sessionId: String): SessionRecordRead {
        legacyPurge.ensurePurged(store)
        val guarded = revocation.guardedRead(subject) { readEvidence(store, keyFor(sessionId), ::parse) }
        return when (guarded) {
            is GuardedRead.Allowed -> guarded.value
            GuardedRead.SubjectRevoked -> SessionRecordRead.Missing
            is GuardedRead.StatusUnavailable -> SessionRecordRead.ReadFailed
        }
    }

    /**
     * Removes ONLY [subject]'s own rows, via per-key [SecureKeyValueStore.remove] — NEVER
     * [SecureKeyValueStore.clearNamespace], which would destroy every OTHER subject's rows sharing this same
     * physical namespace. Mirrors `SecureCognitiveSessionRunStore.deleteAll`. Used ONLY by account-deletion
     * erasure (`SecureNamespaceUserDataEraser`); never called from training/recovery paths.
     *
     * SYSTEM-1 slice 6-R3 (P2-01, HOLD-THROUGH-ERASE): the whole operation runs under the per-subject lock
     * ([SubjectRevocationAuthority.withSubjectLock]); [SubjectRevocationAuthority.markRevoked] durably tombstones
     * the subject as the FIRST action, so a concurrent [upsert] that queues behind the lock always sees the
     * subject already revoked and is refused rather than resurrecting a row after this method reports completion.
     * Deadlock-free: the delete loop only calls `store.remove`, never re-entering a guarded op. A post-delete
     * re-scan (R3-AUD-P1-02) then proves no ENUMERABLE subject key survives before reporting Cleared.
     *
     * RESIDUAL (6R3-ERASURE-P2-01, ledger `quarantine`/P0-D — OPEN, not closed here): a row quarantined BEFORE
     * deletion (via [find]/[all] → `readParsed`, Android backend) is retained under a subject-unlinkable
     * `hashCode` quarantine key that `keys(prefix)` excludes — undecryptable ciphertext, not readable personal
     * data — so this re-scan cannot see it; full removal needs the deferred subject-scoped quarantine API.
     */
    @Suppress("SwallowedException")
    suspend fun deleteAll(): SessionRecordDeleteResult {
        legacyPurge.ensurePurged(store)
        return revocation.withSubjectLock(subject) {
            if (!revocation.markRevoked(subject)) {
                SessionRecordDeleteResult.Failed
            } else {
                try {
                    val keys = store.keys(prefix())
                    // Best-effort TOTAL erasure (contract §8, GDPR fail-closed): attempt EVERY key even if one
                    // faults -- mirrors SecureCognitiveSessionRunStore.deleteAllRuns; a plain `forEach { remove }`
                    // aborts on the first throw and abandons the rest of the deleted subject's rows.
                    var firstFailure: Throwable? = null
                    for (key in keys) {
                        val failure = removeCatching(store, key)
                        if (firstFailure == null) firstFailure = failure
                    }
                    when {
                        firstFailure != null -> SessionRecordDeleteResult.Failed
                        stillHasSubjectRows() -> SessionRecordDeleteResult.Failed
                        else -> SessionRecordDeleteResult.Cleared(keys.size)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                    SessionRecordDeleteResult.Failed
                }
            }
        }
    }

    /** R3-AUD-P1-02: never report [SessionRecordDeleteResult.Cleared] while a physical row for [subject] survives. */
    private suspend fun stillHasSubjectRows(): Boolean = store.keys(prefix()).isNotEmpty()

    private fun parse(raw: String): SessionRecord {
        val dto = json.decodeFromString(SessionRecordDto.serializer(), raw)
        require(dto.schemaVersion <= SessionRecordDto.SCHEMA_VERSION) {
            "SessionRecord schemaVersion ${dto.schemaVersion} is newer than supported."
        }
        return dto.toDomain()
    }

    private fun prefix(): String = "$KEY_PREFIX$subject$SESSION_RECORD_SUBJECT_DELIMITER"

    private fun keyFor(sessionId: String): String = "${prefix()}$sessionId"

    private companion object {
        const val KEY_PREFIX = "session_record:"

        val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/** Breadcrumbs a transient STATUS_UNAVAILABLE on a channel-less read (see [SecureSessionRecordStore.find]/[all]). */
private fun breadcrumbUnavailableRead() {
    ObservabilityHub.breadcrumb("session_record_status_unavailable_on_read")
}

/**
 * Diagnostic evidence read: distinguishes Found / Missing / Unreadable (corrupt row) / ReadFailed (the read
 * THREW). Top-level so [SecureSessionRecordStore] stays under detekt's per-class function threshold.
 * [CancellationException] always propagates.
 */
@Suppress("SwallowedException")
private suspend fun readEvidence(
    store: SecureKeyValueStore,
    key: String,
    parse: (String) -> SessionRecord,
): SessionRecordRead = try {
    when (val result = store.read(key)) {
        is SecureReadResult.Value -> runCatching { parse(result.value) }
            .fold({ SessionRecordRead.Found(it) }, { SessionRecordRead.Unreadable })
        SecureReadResult.Missing -> SessionRecordRead.Missing
        is SecureReadResult.Failure -> SessionRecordRead.Unreadable
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    SessionRecordRead.ReadFailed
}

/** Removes [key], returning the failure (if any) instead of throwing. [CancellationException] always propagates. */
private suspend fun removeCatching(store: SecureKeyValueStore, key: String): Throwable? = try {
    store.remove(key)
    null
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    error
}

/**
 * SYSTEM-1 slice 6-R3 (P2-01): writes [key]=[value] returning a TYPED [SessionRecordUpsertOutcome] — a genuine
 * backend/Keystore fault becomes [SessionRecordUpsertOutcome.RetryableFailure] (never thrown: [SessionRecoveryScanner
 * .recover] calls `upsert` directly from a module root-screen `LaunchedEffect`, and Android's
 * [SecureKeyValueStore.put] is not internally try/caught). [CancellationException] always propagates.
 */
@Suppress("SwallowedException")
private suspend fun putOutcome(
    store: SecureKeyValueStore,
    key: String,
    value: String,
): SessionRecordUpsertOutcome = try {
    store.put(key, value)
    SessionRecordUpsertOutcome.Stored
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    SessionRecordUpsertOutcome.RetryableFailure("session_record_put_failed")
}

private fun SessionRecord.toDto(): SessionRecordDto = SessionRecordDto(
    sessionId = sessionId,
    block = block.name,
    status = status.name,
    startedAtMs = startedAtMs,
    updatedAtMs = updatedAtMs,
    completedTrials = completedTrials,
    plannedTrials = plannedTrials,
    activeDurationMs = activeDurationMs,
    reproducibleIdentity = reproducibleIdentity,
)

private fun SessionRecordDto.toDomain(): SessionRecord = SessionRecord(
    sessionId = sessionId,
    block = ScoreModuleId.valueOf(block),
    status = SessionStatus.valueOf(status),
    startedAtMs = startedAtMs,
    updatedAtMs = updatedAtMs,
    completedTrials = completedTrials,
    plannedTrials = plannedTrials,
    activeDurationMs = activeDurationMs,
    reproducibleIdentity = reproducibleIdentity,
)

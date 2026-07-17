package com.mindos.app.features.mindboost.core.session.payload

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Versioned on-disk shape of a [JournaledPayload]. A dedicated DTO carries [schemaVersion] so migrations stay
 * explicit; [kind] persists as the enum `name` and is FAIL-CLOSED on read — an unknown kind (a payload written by a
 * newer build) makes `parse` throw, so the row is quarantined + skipped rather than misapplied to the wrong sink.
 */
@Serializable
private data class JournaledPayloadDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val entryId: String,
    val kind: String,
    val payload: String,
    val enqueuedAtMs: Long?,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** Delimiter separating the subject from the entry id in a physical key. Reserved: [validateSubject] rejects it. */
private const val PAYLOAD_JOURNAL_SUBJECT_DELIMITER = ':'

/**
 * [subject] must not contain [PAYLOAD_JOURNAL_SUBJECT_DELIMITER]; otherwise one subject's key prefix could be a
 * prefix of a DIFFERENT subject's keys (e.g. `"abc"` vs `"abc:def"`), leaking rows across accounts on [pending].
 * Mirrors the guard in `SecureSessionRecordStore`. The [entryId] itself MAY contain the delimiter (the Decisiones
 * session id is `"$userId:$now"`): it is the terminal key suffix and is read back from the DTO, never parsed from
 * the key, so its internal delimiters are harmless.
 */
private fun validateSubject(subject: String) {
    require(subject.isNotBlank()) { "subject must not be blank (a pseudonymous per-account scope label)." }
    require(PAYLOAD_JOURNAL_SUBJECT_DELIMITER !in subject) {
        "subject must not contain '$PAYLOAD_JOURNAL_SUBJECT_DELIMITER' (the key-prefix delimiter)."
    }
}

/**
 * Durable [PayloadJournalStore] backed by [SecureKeyValueStore] (Android Keystore AES-256-GCM; in-memory on
 * desktop/iOS) in the account-bound `mindboost_payload_journal` namespace. One entry is one namespaced key
 * `payload_journal:<subject>:<entryId>`; a corrupt / unknown-kind / future-schema entry is skipped (quarantined via
 * [readParsed]) rather than misread. Mirrors the repo's standard secure-store pattern (see `SecureSessionRecordStore`).
 *
 * SUBJECT SCOPING: [subject] is a MANDATORY, first-class constructor parameter — every physical key is prefixed by
 * it — so this instance can only ever touch its OWN subject's entries. Isolation depends on every construction site
 * supplying the CURRENT authenticated user's id (see the `*RootScreen`/hub callers); this class cannot verify that
 * on its own (same contract as `SecureSessionRecordStore`).
 *
 * ACCOUNT DELETION: this namespace is erased by the standard blanket `clearNamespace()` in
 * `SecureNamespaceUserDataEraser` (registered in `MindBoostStorageNamespaces.AccountBound` + the AppGraph eraser
 * map), NOT the subject-scoped `deleteAll()` path — matching the accepted single-user-per-device status quo of every
 * namespace except `mindboost_sessions`/`mindboost_session_runs`. The deleting user's own journal entries are fully
 * erased. There is NO backend/cloud mirror of this journal, so `functions/src/accountDeletion.ts` has nothing to do
 * for it (it deletes Firestore docs + Storage prefixes only) — the local blanket clear is the complete erasure.
 *
 * [clock] stamps `enqueuedAtMs` via the injected [EpochClock] (never a global clock); "unknown" is modeled as
 * `null`, never a `0L` sentinel.
 */
class SecurePayloadJournalStore(
    private val store: SecureKeyValueStore,
    private val subject: String,
    private val clock: EpochClock,
    private val json: Json = defaultJson,
) : PayloadJournalStore {

    init {
        validateSubject(subject)
    }

    override suspend fun enqueue(entryId: String, kind: PayloadKind, payload: String) {
        require(entryId.isNotBlank()) { "entryId must not be blank." }
        val dto = JournaledPayloadDto(
            entryId = entryId,
            kind = kind.name,
            payload = payload,
            enqueuedAtMs = clock.nowMillis(),
        )
        store.put(keyFor(entryId), json.encodeToString(JournaledPayloadDto.serializer(), dto))
    }

    override suspend fun pending(): List<JournaledPayload> =
        store.keys(prefix()).mapNotNull { key -> store.readParsed(key, ::parse) }

    override suspend fun confirm(entryId: String) {
        store.remove(keyFor(entryId))
    }

    private fun parse(raw: String): JournaledPayload {
        val dto = json.decodeFromString(JournaledPayloadDto.serializer(), raw)
        require(dto.schemaVersion <= JournaledPayloadDto.SCHEMA_VERSION) {
            "JournaledPayload schemaVersion ${dto.schemaVersion} is newer than supported."
        }
        return JournaledPayload(
            entryId = dto.entryId,
            kind = PayloadKind.valueOf(dto.kind),
            payload = dto.payload,
            enqueuedAtMs = dto.enqueuedAtMs,
        )
    }

    private fun prefix(): String = "$KEY_PREFIX$subject$PAYLOAD_JOURNAL_SUBJECT_DELIMITER"

    private fun keyFor(entryId: String): String = "${prefix()}$entryId"

    private companion object {
        const val KEY_PREFIX = "payload_journal:"
        val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

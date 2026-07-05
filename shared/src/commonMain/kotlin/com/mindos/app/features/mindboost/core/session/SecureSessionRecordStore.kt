package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
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
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Persists [SessionRecord]s as encrypted blobs via [SecureKeyValueStore] (Android Keystore AES-256-GCM;
 * in-memory on desktop/iOS) in the account-bound `mindboost_sessions` namespace. One session is one
 * namespaced key; a corrupt, unknown-enum, or future-schema entry is skipped (quarantined) rather than
 * misread. Mirrors the repo's standard secure-store pattern (see SecureDecisionSessionRepository).
 */
class SecureSessionRecordStore(
    private val store: SecureKeyValueStore,
    private val json: Json = defaultJson,
) : SessionRecordStore {

    override suspend fun upsert(record: SessionRecord) {
        require(record.sessionId.isNotBlank()) { "sessionId must not be blank." }
        store.put(keyFor(record.sessionId), json.encodeToString(SessionRecordDto.serializer(), record.toDto()))
    }

    override suspend fun find(sessionId: String): SessionRecord? = store.readParsed(keyFor(sessionId), ::parse)

    override suspend fun all(): List<SessionRecord> =
        store.keys(KEY_PREFIX)
            .mapNotNull { key -> store.readParsed(key, ::parse) }
            .sortedWith(compareByDescending<SessionRecord> { it.startedAtMs }.thenBy { it.sessionId })

    override suspend fun delete(sessionId: String) {
        store.remove(keyFor(sessionId))
    }

    private fun parse(raw: String): SessionRecord {
        val dto = json.decodeFromString(SessionRecordDto.serializer(), raw)
        require(dto.schemaVersion <= SessionRecordDto.SCHEMA_VERSION) {
            "SessionRecord schemaVersion ${dto.schemaVersion} is newer than supported."
        }
        return dto.toDomain()
    }

    private fun keyFor(sessionId: String): String = "$KEY_PREFIX$sessionId"

    private companion object {
        const val KEY_PREFIX = "session_record:"
        val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
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
)

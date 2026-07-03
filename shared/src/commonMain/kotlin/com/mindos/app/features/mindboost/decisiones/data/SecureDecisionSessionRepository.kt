package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import com.mindverse.mindboost.core.domain.DecisionSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Versioned on-disk shape for a [DecisionSession]. A dedicated DTO (rather than serializing the domain
 * model directly) carries [schemaVersion] so future migrations stay explicit.
 */
@Serializable
private data class DecisionSessionDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val sessionId: String,
    val timestampMs: Long,
    val icd: Double,
    val ied: Double,
    val cde: Double,
    val fatigueScore: Double,
    val gamingScore: Double,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Persists [DecisionSession]s as encrypted blobs via [SecureKeyValueStore] — the repo's standard
 * at-rest encryption surface (Android Keystore-backed AES-256-GCM; in-memory on desktop/iOS). Each
 * session is one namespaced key; reads tolerate a corrupt entry by skipping it (the store quarantines
 * it). Suspend-based to match the store; fully unit-testable against the in-memory store.
 */
class SecureDecisionSessionRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = defaultJson,
) {
    suspend fun save(session: DecisionSession) {
        require(session.sessionId.isNotBlank()) { "sessionId must not be blank." }
        store.put(keyFor(session.sessionId), json.encodeToString(DecisionSessionDto.serializer(), session.toDto()))
    }

    suspend fun recent(limit: Int): List<DecisionSession> {
        require(limit >= 0) { "limit must be >= 0." }
        if (limit == 0) return emptyList()

        return store.keys(KEY_PREFIX)
            .mapNotNull { key -> store.readParsed(key, ::parse) }
            .sortedWith(compareByDescending<DecisionSession> { it.timestampMs }.thenBy { it.sessionId })
            .take(limit)
    }

    private fun parse(raw: String): DecisionSession =
        json.decodeFromString(DecisionSessionDto.serializer(), raw).toDomain()

    private fun keyFor(sessionId: String): String = "$KEY_PREFIX$sessionId"

    private companion object {
        const val KEY_PREFIX = "decision_session:"
        val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

private fun DecisionSession.toDto(): DecisionSessionDto = DecisionSessionDto(
    sessionId = sessionId,
    timestampMs = timestampMs,
    icd = icd,
    ied = ied,
    cde = cde,
    fatigueScore = fatigueScore,
    gamingScore = gamingScore,
)

private fun DecisionSessionDto.toDomain(): DecisionSession = DecisionSession(
    sessionId = sessionId,
    timestampMs = timestampMs,
    icd = icd,
    ied = ied,
    cde = cde,
    fatigueScore = fatigueScore,
    gamingScore = gamingScore,
)

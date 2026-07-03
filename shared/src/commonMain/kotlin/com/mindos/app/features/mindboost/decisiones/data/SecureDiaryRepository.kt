package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import com.mindverse.mindboost.core.domain.DiaryEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Versioned on-disk shape for a [DiaryEntry]; [schemaVersion] keeps future migrations explicit. */
@Serializable
private data class DiaryEntryDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val timestampMs: Long,
    val content: ByteArray,
    val safetyFlag: Boolean,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Persists [DiaryEntry]s as encrypted blobs via [SecureKeyValueStore] — the repo's standard at-rest
 * encryption surface (Android Keystore-backed AES-256-GCM; in-memory on desktop/iOS). The diary text
 * is the sensitive payload; the store provides the encryption, so the entry's bytes are stored as-is
 * inside the encrypted blob. Suspend-based to match the store; fully unit-testable in memory.
 */
class SecureDiaryRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = defaultJson,
) {
    suspend fun save(entry: DiaryEntry) {
        require(entry.id.isNotBlank()) { "diary entry id must not be blank." }
        store.put(keyFor(entry.id), json.encodeToString(DiaryEntryDto.serializer(), entry.toDto()))
    }

    suspend fun get(id: String): DiaryEntry? = store.readParsed(keyFor(id), ::parse)

    suspend fun wipe() {
        store.keys(KEY_PREFIX).forEach { key -> store.remove(key) }
    }

    private fun parse(raw: String): DiaryEntry =
        json.decodeFromString(DiaryEntryDto.serializer(), raw).toDomain()

    private fun keyFor(id: String): String = "$KEY_PREFIX$id"

    private companion object {
        const val KEY_PREFIX = "decision_diary:"
        val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

private fun DiaryEntry.toDto(): DiaryEntryDto = DiaryEntryDto(
    id = id,
    timestampMs = timestampMs,
    content = encryptedContent.copyOf(),
    safetyFlag = safetyFlag,
)

private fun DiaryEntryDto.toDomain(): DiaryEntry = DiaryEntry(
    id = id,
    timestampMs = timestampMs,
    encryptedContent = content.copyOf(),
    safetyFlag = safetyFlag,
)

@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.coach.data

import com.mindos.app.features.mindboost.coach.domain.CoachSummary
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MC8u-F — the Elite user's saved coach memory. MINIMIZED by construction: only coarse Markdown views derived from the
 * PII-free [CoachSummary] — never raw trials, self-report values, free-text prompts, PII, or clinical/medical data.
 * Local-first encrypted (SecureKeyValueStore); a cloud copy is a documented MVP blocker (Firestore not wired).
 */
@kotlinx.serialization.Serializable
data class AICoachMemory(
    val schemaVersion: Int = SCHEMA_VERSION,
    val summaryMd: String,
    val strengthsMd: String,
    val weaknessesMd: String,
    val currentPlanMd: String,
    val recommendationsMd: String,
    val lastUpdatedAtMillis: Long,
    val version: Int,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** Save/load/delete for the coach memory. Delete supports the user "borrar memoria IA" flow + account deletion. */
interface AICoachMemoryStore {
    suspend fun load(userId: String): AICoachMemory?
    suspend fun save(userId: String, memory: AICoachMemory)
    suspend fun delete(userId: String)
}

/** Builds a minimized memory from the coarse summary — never introduces raw or clinical content. */
object AICoachMemoryFactory {
    fun fromSummary(summary: CoachSummary, currentPlanMd: String, nowMillis: Long, version: Int): AICoachMemory {
        val strengths = summary.contributions.flatMap { it.strengths }.distinct()
        val weaknesses = summary.contributions.flatMap { it.weaknesses }.distinct()
        val patterns = summary.contributions.flatMap { it.patterns }.distinct()
        val recs = summary.contributions.flatMap { it.recommendations }.map { "${it.type}: ${it.reason}" }.distinct()
        return AICoachMemory(
            summaryMd = "Bloques con datos: ${summary.contributions.joinToString(", ") { it.blockId }}. Confianza global: ${summary.overallConfidence}.",
            strengthsMd = if (strengths.isEmpty()) "—" else strengths.joinToString("; "),
            weaknessesMd = if (weaknesses.isEmpty()) "—" else weaknesses.joinToString("; "),
            currentPlanMd = currentPlanMd,
            recommendationsMd = (patterns + recs).joinToString("; ").ifBlank { "—" },
            lastUpdatedAtMillis = nowMillis,
            version = version,
        )
    }
}

private val CoachMemoryJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val USER_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")

/** Local encrypted memory store. `userId` is validated so it can't corrupt the key; reads quarantine a future schema. */
class SecureAICoachMemoryStore(private val store: SecureKeyValueStore) : AICoachMemoryStore {

    override suspend fun load(userId: String): AICoachMemory? {
        requireUserId(userId)
        return store.readVersioned(
            key = key(userId),
            currentVersion = AICoachMemory.SCHEMA_VERSION,
            parser = { CoachMemoryJson.decodeFromString<AICoachMemory>(it) },
            versionOf = { it.schemaVersion },
        )
    }

    override suspend fun save(userId: String, memory: AICoachMemory) {
        requireUserId(userId)
        store.put(key(userId), CoachMemoryJson.encodeToString(memory))
    }

    override suspend fun delete(userId: String) {
        requireUserId(userId)
        store.remove(key(userId))
    }

    private fun key(userId: String) = "memory:$userId"
    private fun requireUserId(userId: String) = require(USER_ID_REGEX.matches(userId)) { "userId must match [A-Za-z0-9_-]{1,128}." }
}

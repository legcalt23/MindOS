// The four repositories share key conventions and a delete-by-prefix routine; grouping them in
// one file keeps that convention in one place. Wide-ish but cohesive.
@file:Suppress("TooManyFunctions")

package com.mindos.app.features.mindboost.language.data.local

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readValueOrNull
import com.mindos.app.features.mindboost.language.domain.adaptation.ExposureSnapshot
import com.mindos.app.features.mindboost.language.domain.model.ConsentState
import com.mindos.app.features.mindboost.language.domain.model.ConsentType
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import com.mindos.app.features.mindboost.language.domain.projection.ProfileProjector
import com.mindos.app.features.mindboost.language.domain.repository.ConsentRepository
import com.mindos.app.features.mindboost.language.domain.repository.EvidenceRepository
import com.mindos.app.features.mindboost.language.domain.repository.ExposureRepository
import com.mindos.app.features.mindboost.language.domain.repository.SessionSummaryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encrypted-at-rest local persistence for MindBoost · Lenguaje, over the SAME
 * [SecureKeyValueStore] (AES-256-GCM + Android Keystore) already used by Enfoque/Velocidad.
 * Vetted crypto is reused — none is invented (project rule, CLAUDE.md 2026-06-05).
 *
 * SECURITY/PRIVACY DECISION (documented, not a blocker): only evidence **abstractions**
 * ([EvidencePacket]), aggregate [SessionSummary] (no raw responses), per-user [ExposureSnapshot]
 * and [ConsentState] are persisted. Raw responses / free text are never stored here. Every
 * repository keys by `userIdHash` and offers `deleteUserData` for granular erasure (§13). The
 * `userIdHash::id` separator disambiguates ids that themselves contain `:`.
 */

private const val SEP = "::"

private fun userPrefix(prefix: String, user: UserIdHash): String = "$prefix${user.value}$SEP"

/** Evidence packets + profile projection. The profile is replayed, never stored. */
class LocalEvidenceRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = LanguageJson.instance,
) : EvidenceRepository {

    private val mutex = Mutex()

    override suspend fun append(packet: EvidencePacket) {
        store.put(key(packet.userIdHash, packet.packetId), json.encodeToString(PersistedPacket(packet = packet)))
    }

    override suspend fun packets(userIdHash: UserIdHash): List<EvidencePacket> =
        store.keys(userPrefix(PREFIX, userIdHash))
            .mapNotNull { k -> store.readValueOrNull(k)?.let { decode(it) } }

    override suspend fun loadProfile(userIdHash: UserIdHash): UserLanguageProfile {
        val packets = packets(userIdHash)
        val asOf = packets.maxOfOrNull { it.createdAtEpochMs } ?: 0L
        return ProfileProjector.project(userIdHash, packets, asOf)
    }

    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = mutex.withLock {
        val keys = store.keys(userPrefix(PREFIX, userIdHash))
        keys.forEach { store.remove(it) }
        keys.size
    }

    private fun key(user: UserIdHash, packetId: String): String = "${userPrefix(PREFIX, user)}$packetId"

    private fun decode(raw: String): EvidencePacket? =
        runCatching { json.decodeFromString<PersistedPacket>(raw).packet }.getOrNull()

    private companion object {
        const val PREFIX = "pkt:"
    }
}

/** Aggregated session summaries (no raw responses). */
class LocalSessionSummaryRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = LanguageJson.instance,
) : SessionSummaryRepository {

    private val mutex = Mutex()

    override suspend fun save(summary: SessionSummary) {
        store.put(key(summary.userIdHash, summary.sessionId), json.encodeToString(PersistedSummary(summary = summary)))
    }

    override suspend fun summaries(userIdHash: UserIdHash): List<SessionSummary> =
        store.keys(userPrefix(PREFIX, userIdHash))
            .mapNotNull { k -> store.readValueOrNull(k)?.let { decode(it) } }

    override suspend fun get(userIdHash: UserIdHash, sessionId: LanguageSessionId): SessionSummary? =
        store.readValueOrNull(key(userIdHash, sessionId))?.let { decode(it) }

    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = mutex.withLock {
        val keys = store.keys(userPrefix(PREFIX, userIdHash))
        keys.forEach { store.remove(it) }
        keys.size
    }

    private fun key(user: UserIdHash, sessionId: LanguageSessionId): String =
        "${userPrefix(PREFIX, user)}${sessionId.value}"

    private fun decode(raw: String): SessionSummary? =
        runCatching { json.decodeFromString<PersistedSummary>(raw).summary }.getOrNull()

    private companion object {
        const val PREFIX = "sum:"
    }
}

/** Per-user exposure snapshot, stored as a single blob and updated copy-on-write. */
class LocalExposureRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = LanguageJson.instance,
) : ExposureRepository {

    private val mutex = Mutex()

    override suspend fun record(userIdHash: UserIdHash, taskId: TaskId) = mutex.withLock {
        val current = snapshot(userIdHash)
        store.put(key(userIdHash), json.encodeToString(PersistedExposure(snapshot = current.record(taskId))))
    }

    override suspend fun snapshot(userIdHash: UserIdHash): ExposureSnapshot =
        store.readValueOrNull(key(userIdHash))?.let { decode(it) } ?: ExposureSnapshot()

    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = mutex.withLock {
        val k = key(userIdHash)
        val existed = store.readValueOrNull(k) != null
        store.remove(k)
        if (existed) 1 else 0
    }

    private fun key(user: UserIdHash): String = "$PREFIX${user.value}"

    private fun decode(raw: String): ExposureSnapshot? =
        runCatching { json.decodeFromString<PersistedExposure>(raw).snapshot }.getOrNull()

    private companion object {
        const val PREFIX = "exp:"
    }
}

/** Granular per-purpose consent. */
class LocalConsentRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = LanguageJson.instance,
) : ConsentRepository {

    private val mutex = Mutex()

    override suspend fun set(consent: ConsentState) {
        val k = key(consent.userIdHash, consent.consentType)
        store.put(k, json.encodeToString(PersistedConsent(consent = consent)))
    }

    override suspend fun get(userIdHash: UserIdHash, type: ConsentType): ConsentState? =
        store.readValueOrNull(key(userIdHash, type))?.let { decode(it) }

    override suspend fun all(userIdHash: UserIdHash): List<ConsentState> =
        store.keys(userPrefix(PREFIX, userIdHash))
            .mapNotNull { k -> store.readValueOrNull(k)?.let { decode(it) } }

    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = mutex.withLock {
        val keys = store.keys(userPrefix(PREFIX, userIdHash))
        keys.forEach { store.remove(it) }
        keys.size
    }

    private fun key(user: UserIdHash, type: ConsentType): String = "${userPrefix(PREFIX, user)}${type.name}"

    private fun decode(raw: String): ConsentState? =
        runCatching { json.decodeFromString<PersistedConsent>(raw).consent }.getOrNull()

    private companion object {
        const val PREFIX = "cns:"
    }
}

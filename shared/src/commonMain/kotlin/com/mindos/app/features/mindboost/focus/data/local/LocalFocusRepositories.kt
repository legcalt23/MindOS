package com.mindos.app.features.mindboost.focus.data.local

import com.mindos.app.features.mindboost.focus.data.dto.BaselineSnapshotDto
import com.mindos.app.features.mindboost.focus.data.dto.SessionSummaryDto
import com.mindos.app.features.mindboost.focus.data.dto.UserCognitiveProfileDto
import com.mindos.app.features.mindboost.focus.data.mapper.toDomain
import com.mindos.app.features.mindboost.focus.data.mapper.toDto
import com.mindos.app.features.mindboost.focus.data.mapper.toSummaryDto
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeAdministration
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeBank
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeDefinition
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.repository.BaselineRepository
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.ProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Logical key prefixes within a focus namespace. */
private object FocusKeys {
    const val PROFILE = "profile:"
    const val BASELINE = "baseline:"
    const val SESSION = "session:"
    const val PROBE = "probe:"
    const val PROBE_CURSOR = "probecursor:"
}

/** Stores the cognitive profile, encrypted via [SecureKeyValueStore]. */
class LocalFocusProfileRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = FocusJson.instance,
) : FocusProfileRepository {

    override suspend fun load(userId: String): UserCognitiveProfile? =
        store.get(FocusKeys.PROFILE + userId)
            ?.let { runCatching { json.decodeFromString<UserCognitiveProfileDto>(it).toDomain() }.getOrNull() }

    override suspend fun save(profile: UserCognitiveProfile) {
        store.put(FocusKeys.PROFILE + profile.userId, json.encodeToString(profile.toDto()))
    }
}

/** Stores session SUMMARIES (no raw trials), one entry per session, listable by user. */
class LocalSessionHistoryRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = FocusJson.instance,
) : SessionHistoryRepository {

    override suspend fun append(result: SessionResult) {
        // sessionId.value is "<userId>:<endedAtMs>", so the key is naturally user-scoped.
        store.put(FocusKeys.SESSION + result.sessionId.value, json.encodeToString(result.toSummaryDto()))
    }

    override suspend fun recent(userId: String, limit: Int): List<SessionResult> {
        require(limit > 0) { "limit must be > 0." }
        return store.keys(FocusKeys.SESSION + "$userId:")
            .mapNotNull { key -> store.get(key)?.let { json.decodeSessionOrNull(it) } }
            .sortedByDescending { it.endedAtMs }
            .take(limit)
    }
}

/** Stores the individual baseline snapshot. */
class LocalBaselineRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = FocusJson.instance,
) : BaselineRepository {

    override suspend fun load(userId: String): BaselineSnapshot? =
        store.get(FocusKeys.BASELINE + userId)
            ?.let { runCatching { json.decodeFromString<BaselineSnapshotDto>(it).toDomain() }.getOrNull() }

    override suspend fun save(snapshot: BaselineSnapshot) {
        store.put(FocusKeys.BASELINE + snapshot.userId, json.encodeToString(snapshot.toDto()))
    }
}

/**
 * Persists reserved-probe results (kept apart from training history) and the
 * per-probe form-rotation cursor, all encrypted.
 */
class LocalProbeBankRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = FocusJson.instance,
    private val probes: List<ProbeDefinition> = ProbeBank.all,
) : ProbeBankRepository {

    override suspend fun nextProbe(userId: String, week: Int): ProbeAdministration {
        require(probes.isNotEmpty()) { "Probe bank is empty." }
        val probe = probes[((week - 1).coerceAtLeast(0)) % probes.size]
        val cursorKey = "${FocusKeys.PROBE_CURSOR}$userId:${probe.probeId}"
        val index = store.get(cursorKey)?.toIntOrNull() ?: 0
        val formSeed = probe.reservedForms[index % probe.reservedForms.size]
        store.put(cursorKey, (index + 1).toString())
        return ProbeAdministration(probe = probe, formSeed = formSeed, week = week)
    }

    override suspend fun recordProbe(userId: String, result: SessionResult) {
        store.put("${FocusKeys.PROBE}$userId:${result.endedAtMs}", json.encodeToString(result.toSummaryDto()))
    }

    override suspend fun probeHistory(userId: String, limit: Int): List<SessionResult> {
        require(limit > 0) { "limit must be > 0." }
        return store.keys("${FocusKeys.PROBE}$userId:")
            .mapNotNull { key -> store.get(key)?.let { json.decodeSessionOrNull(it) } }
            .sortedByDescending { it.endedAtMs }
            .take(limit)
    }
}

/** Shared decode of a persisted session summary; corrupt entries become null. */
private fun Json.decodeSessionOrNull(raw: String): SessionResult? =
    runCatching { decodeFromString<SessionSummaryDto>(raw).toDomain() }.getOrNull()

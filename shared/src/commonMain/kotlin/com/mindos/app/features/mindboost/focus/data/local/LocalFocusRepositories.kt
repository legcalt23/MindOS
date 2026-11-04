package com.mindos.app.features.mindboost.focus.data.local

import com.mindos.app.features.mindboost.focus.data.dto.BaselineSnapshotDto
import com.mindos.app.features.mindboost.focus.data.dto.FocusSchema
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
        store.readVersioned(
            key = FocusKeys.PROFILE + userId,
            currentVersion = FocusSchema.CURRENT,
            parser = json::decodeFromString,
            versionOf = UserCognitiveProfileDto::schemaVersion,
        )?.toDomain()

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
            .mapNotNull { key -> json.decodeSessionOrNull(store, key) }
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
        store.readVersioned(
            key = FocusKeys.BASELINE + userId,
            currentVersion = FocusSchema.CURRENT,
            parser = json::decodeFromString,
            versionOf = BaselineSnapshotDto::schemaVersion,
        )?.toDomain()

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
        val cursorKey = cursorKey(userId, probe.probeId)
        val index = readCursor(cursorKey)
        val formSeed = probe.reservedForms[(index ?: 0) % probe.reservedForms.size]
        // Only a cursor we could actually READ may be advanced. Writing here on an unreadable one would
        // make the reset permanent and destroy the single owner of "which forms were already served".
        if (index != null) {
            store.put(cursorKey, (index + 1).toString())
        }
        // The cursor counts forms actually spent (releaseProbe is the exact inverse of a burn) and forms are
        // served in strict rotation order, so index >= size is EXACTLY "every reserved form has been served,
        // and this one comes round again". Derived from the one owner: no second store, no second counter.
        return ProbeAdministration(
            probe = probe,
            formSeed = formSeed,
            week = week,
            // A cursor we could not read cannot say this form is new, and "cannot say" must never render
            // as the favourable reading: it declares the repeat (the disclosure clause is shown) and it
            // withholds reliability, exactly as a real wrap does.
            repeatsEarlierForm = index == null || index >= probe.reservedForms.size,
        )
    }

    override suspend fun releaseProbe(userId: String, probeId: String) {
        val cursorKey = cursorKey(userId, probeId)
        // An unreadable cursor cannot be rewound: writing a fabricated position here would hand back a
        // form the user has already seen. Leaving it untouched keeps a later read able to recover it.
        val index = readCursor(cursorKey) ?: return
        // coerceAtLeast(0) is a floor, not a fallback: a release without a preceding burn must not
        // rewind the cursor into a form the user already saw.
        store.put(cursorKey, (index - 1).coerceAtLeast(0).toString())
    }

    private fun cursorKey(userId: String, probeId: String) = "${FocusKeys.PROBE_CURSOR}$userId:$probeId"

    /**
     * Which reserved forms this user has already been served. THREE answers, not two: 0 for a genuine
     * first administration, the stored count, and `null` for "cannot say".
     *
     * Deliberately NOT [readParsed]/[readValueOrNull]: those quarantine on failure, which DELETES the
     * entry — and this counter is irreproducible (no second store, nothing to re-derive it from), so a
     * transient backend failure would erase it for good. Collapsing that failure into 0 would also be the
     * most favourable value possible ("no form has been served"), handing back the baseline's own form
     * stamped as fresh. Absence is a fact; unreadable is not the same fact.
     */
    private suspend fun readCursor(cursorKey: String): Int? = when (val result = store.read(cursorKey)) {
        is SecureReadResult.Value -> result.value.toIntOrNull()
        SecureReadResult.Missing -> 0
        is SecureReadResult.Failure -> null
    }

    override suspend fun recordProbe(userId: String, result: SessionResult) {
        store.put("${FocusKeys.PROBE}$userId:${result.endedAtMs}", json.encodeToString(result.toSummaryDto()))
    }

    override suspend fun probeHistory(userId: String, limit: Int): List<SessionResult> {
        require(limit > 0) { "limit must be > 0." }
        return store.keys("${FocusKeys.PROBE}$userId:")
            .mapNotNull { key -> json.decodeSessionOrNull(store, key) }
            .sortedByDescending { it.endedAtMs }
            .take(limit)
    }
}

/** Shared decode of a persisted session summary; unsafe entries are quarantined. */
private suspend fun Json.decodeSessionOrNull(store: SecureKeyValueStore, key: String): SessionResult? =
    store.readVersioned(
        key = key,
        currentVersion = FocusSchema.CURRENT,
        parser = this::decodeFromString,
        versionOf = SessionSummaryDto::schemaVersion,
    )?.toDomain()

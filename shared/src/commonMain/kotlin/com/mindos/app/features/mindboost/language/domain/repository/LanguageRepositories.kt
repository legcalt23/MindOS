package com.mindos.app.features.mindboost.language.domain.repository

import com.mindos.app.features.mindboost.language.domain.adaptation.ExposureSnapshot
import com.mindos.app.features.mindboost.language.domain.model.ConsentState
import com.mindos.app.features.mindboost.language.domain.model.ConsentType
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash

/**
 * Repository contracts (v6 §12.5). All are `suspend` and offline-first. Every store exposes a
 * per-user delete for granular erasure (§13). The profile is reconstructed from the evidence
 * stream — never stored as mutable truth — so deletion of packets fully removes derived ability.
 */

/**
 * Append-only evidence store + profile projection (v6 §12.3 event sourcing). [loadProfile]
 * folds the packet stream into a [UserLanguageProfile]; there is no setter for the profile.
 */
interface EvidenceRepository {
    suspend fun append(packet: EvidencePacket)
    suspend fun packets(userIdHash: UserIdHash): List<EvidencePacket>
    suspend fun loadProfile(userIdHash: UserIdHash): UserLanguageProfile
    suspend fun deleteUserData(userIdHash: UserIdHash): Int
}

/** Aggregated session summaries (v6 §6.1). Stores summaries only — never raw responses. */
interface SessionSummaryRepository {
    suspend fun save(summary: SessionSummary)
    suspend fun summaries(userIdHash: UserIdHash): List<SessionSummary>
    suspend fun get(userIdHash: UserIdHash, sessionId: LanguageSessionId): SessionSummary?
    suspend fun deleteUserData(userIdHash: UserIdHash): Int
}

/** Per-user item exposure (v6 §5.6 repeated-item gate, §8.6 rotation). */
interface ExposureRepository {
    suspend fun record(userIdHash: UserIdHash, taskId: TaskId)
    suspend fun snapshot(userIdHash: UserIdHash): ExposureSnapshot
    suspend fun deleteUserData(userIdHash: UserIdHash): Int
}

/** Granular consent state (v6 §13). Reads default to *denied* when no decision exists. */
interface ConsentRepository {
    suspend fun set(consent: ConsentState)
    suspend fun get(userIdHash: UserIdHash, type: ConsentType): ConsentState?
    suspend fun all(userIdHash: UserIdHash): List<ConsentState>
    suspend fun deleteUserData(userIdHash: UserIdHash): Int
}

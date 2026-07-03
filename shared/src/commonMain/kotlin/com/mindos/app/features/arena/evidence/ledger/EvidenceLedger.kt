package com.mindos.app.features.arena.evidence.ledger

import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.engine.TrialEvidence

/**
 * Phase 06: One immutable, auditable evidence record. Identifiers are synthetic UUIDv4 only; there
 * is no raw-UID field. Records preserve their insertion order via [sequence].
 */
data class EvidenceRecord(
    val runId: SimulationRunId,
    val profileId: SyntheticProfileId,
    val sequence: Int,
    val evidence: TrialEvidence,
    val timestampMs: Long,
) {
    init {
        require(sequence >= 0) { "sequence must be >= 0" }
        require(timestampMs >= 0) { "timestampMs must be >= 0" }
    }
}

/**
 * Phase 06: Offline, in-memory evidence ledger. No Firebase / Firestore / file output / telemetry.
 * Deterministic ordering: [records] returns records in append order.
 */
interface EvidenceLedger {
    fun append(record: EvidenceRecord)
    fun records(): List<EvidenceRecord>
    fun recordsFor(profileId: SyntheticProfileId): List<EvidenceRecord>
    fun size(): Int
}

/**
 * Phase 06: Default in-memory implementation. Backed by an ordered list; never touches disk or
 * network. Synthetic ids are enforced upstream by [SyntheticProfileId] / [SimulationRunId].
 */
class InMemoryEvidenceLedger : EvidenceLedger {

    private val store = mutableListOf<EvidenceRecord>()

    override fun append(record: EvidenceRecord) {
        store.add(record)
    }

    override fun records(): List<EvidenceRecord> = store.toList()

    override fun recordsFor(profileId: SyntheticProfileId): List<EvidenceRecord> =
        store.filter { it.profileId == profileId }

    override fun size(): Int = store.size
}

/**
 * Phase 06: Replays a profile's ledger evidence through [engine] from [initial], in deterministic
 * append order, returning the resulting posterior. Rejected updates leave the posterior unchanged.
 */
fun EvidenceLedger.replay(
    engine: BayesianRatingEngine,
    initial: RatingPosterior,
    profileId: SyntheticProfileId,
): RatingPosterior {
    var posterior = initial
    for (record in recordsFor(profileId)) {
        val result = engine.update(posterior, record.evidence)
        if (result is RatingUpdateResult.Applied) {
            posterior = result.posterior
        }
    }
    return posterior
}

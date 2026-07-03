package com.mindos.app.features.arena

import com.mindos.app.features.arena.evidence.ledger.EvidenceRecord
import com.mindos.app.features.arena.evidence.ledger.InMemoryEvidenceLedger
import com.mindos.app.features.arena.evidence.ledger.SimulationRunId
import com.mindos.app.features.arena.evidence.ledger.SyntheticProfileId
import com.mindos.app.features.arena.evidence.ledger.replay
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 06: In-memory, synthetic-only evidence ledger. */
class EvidenceLedgerTest {

    private val random = Random(42)
    private val runId = SimulationRunId.random(random)
    private val profile = SyntheticProfileId.random(random)

    private fun record(seq: Int, observed: Double = 0.8) = EvidenceRecord(
        runId = runId,
        profileId = profile,
        sequence = seq,
        evidence = ArenaTestFixtures.verifiedEvidence(observedPerformance = observed),
        timestampMs = seq.toLong(),
    )

    @Test
    fun storesSyntheticEvidence() {
        val ledger = InMemoryEvidenceLedger()
        ledger.append(record(0))
        ledger.append(record(1))
        assertEquals(2, ledger.size())
        assertEquals(2, ledger.recordsFor(profile).size)
    }

    @Test
    fun preservesDeterministicAppendOrder() {
        val ledger = InMemoryEvidenceLedger()
        (0 until 5).forEach { ledger.append(record(it)) }
        assertEquals(listOf(0, 1, 2, 3, 4), ledger.records().map { it.sequence })
    }

    @Test
    fun rejectsRawUidLikeIdentifiers() {
        // A Firebase-style raw UID is not a UUIDv4 and cannot become a synthetic id.
        assertFailsWith<IllegalArgumentException> { SyntheticProfileId("aB3xRawFirebaseUid0000000abcd") }
    }

    @Test
    fun canReplayEvidenceIntoBayesianEngine() {
        val ledger = InMemoryEvidenceLedger()
        (0 until 10).forEach { ledger.append(record(it, observed = 0.85)) }
        val engine = BayesianRatingEngine()
        val initial = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 350.0)

        val replayed = ledger.replay(engine, initial, profile)

        // Independent manual fold over the same records must match.
        var manual = initial
        for (r in ledger.recordsFor(profile)) {
            val res = engine.update(manual, r.evidence)
            if (res is RatingUpdateResult.Applied) manual = res.posterior
        }
        assertEquals(manual, replayed)
        assertTrue(replayed.mean.value > initial.mean.value, "strong evidence should raise rating")
    }

    @Test
    fun randomSyntheticIdsAreValidUuidV4() {
        assertTrue(com.mindos.app.features.arena.config.UuidV4.isValid(runId.value))
        assertTrue(com.mindos.app.features.arena.config.UuidV4.isValid(profile.value))
    }
}

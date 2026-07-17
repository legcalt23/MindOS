package com.mindos.app.features.mindboost.profile

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileDto
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileJson
import com.mindos.app.features.mindboost.profile.data.local.LocalMindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindBoostProfilePersistenceTest {

    private val aggregator = MindBoostProfileAggregator()

    private fun contrib(module: CognitiveModuleId, score: Double?, conf: Double) =
        ModuleContribution(module, score, dataSufficiency = 0.9, confidence = conf, ProfileTrend.STABLE, preliminary = false)

    @Test
    fun roundTrip_preservesDimensionsAndOverall() = runTest {
        val repo = LocalMindBoostProfileRepository(InMemorySecureKeyValueStore())
        val profile = aggregator.aggregate(
            "u1",
            listOf(
                contrib(CognitiveModuleId.VELOCIDAD, 0.6, conf = 0.8),
                contrib(CognitiveModuleId.LOGICA, 0.4, conf = 0.8),
                contrib(CognitiveModuleId.LENGUAJE, 0.8, conf = 0.8),
                contrib(CognitiveModuleId.ENFOQUE, 0.5, conf = 0.5), // emerging
            ),
            nowMs = 123L,
        )
        repo.save(profile)

        val loaded = assertNotNull(repo.load("u1"))
        assertEquals("u1", loaded.userId)
        assertEquals(123L, loaded.updatedAtMs)

        val speed = loaded.dimension(CognitiveDimension.PROCESSING_SPEED)
        assertEquals(0.6, speed.value!!, 1e-9)
        assertEquals(DimensionStatus.ESTABLISHED, speed.status)
        assertEquals(listOf(CognitiveModuleId.VELOCIDAD), speed.contributingModules)
        assertEquals(DimensionStatus.EMERGING, loaded.dimension(CognitiveDimension.ATTENTION).status)

        assertEquals(3, loaded.overall!!.establishedDimensionCount)
        assertEquals(CognitiveDimension.VERBAL, loaded.overall!!.strongest)
        assertEquals(CognitiveDimension.REASONING, loaded.overall!!.developing)
        // C2.5: with the default (OFF) flag, neither the official nor the provisional composite is ever
        // produced, even with 3 established dimensions — both stay null across the round trip.
        assertNull(profile.overall!!.compositeIndex)
        assertNull(loaded.overall!!.compositeIndex)
        assertNull(profile.overall!!.provisionalIndex)
        assertNull(loaded.overall!!.provisionalIndex)
    }

    @Test
    fun roundTrip_preservesTheProvisionalIndex_coverageAndMissingDimensions() = runTest {
        // Separate from the flag-off round trip above: proves ProvisionalGlobalIndex itself (its covered/missing
        // dimensions — §33/§34 D4: coverage-only, no scalar) survives the DTO round trip.
        val repo = LocalMindBoostProfileRepository(InMemorySecureKeyValueStore())
        val flaggedOnAggregator = MindBoostProfileAggregator(globalScoreEnabled = { true })
        val profile = flaggedOnAggregator.aggregate(
            "u1",
            listOf(
                contrib(CognitiveModuleId.VELOCIDAD, 0.6, conf = 0.8),
                contrib(CognitiveModuleId.LOGICA, 0.4, conf = 0.8),
                contrib(CognitiveModuleId.LENGUAJE, 0.8, conf = 0.8),
            ),
            nowMs = 321L,
        )
        repo.save(profile)

        val loaded = assertNotNull(repo.load("u1"))
        assertNull(loaded.overall!!.compositeIndex, "official composite stays blocked by ADR_SCI_006_APPROVED")
        val provisional = assertNotNull(loaded.overall!!.provisionalIndex)
        assertEquals(profile.overall!!.provisionalIndex!!.coveredDimensions, provisional.coveredDimensions)
        assertEquals(3, provisional.coveredDimensions.size)
        assertEquals(CognitiveDimension.entries.size - 3, provisional.missingDimensions.size)
        assertTrue(CognitiveDimension.ATTENTION in provisional.missingDimensions)
    }

    @Test
    fun loadMissing_returnsNull() = runTest {
        val repo = LocalMindBoostProfileRepository(InMemorySecureKeyValueStore())
        assertNull(repo.load("nobody"))
    }

    @Test
    fun futureSchemaVersion_isQuarantined_andLoadsNull() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalMindBoostProfileRepository(store)
        val future = MindBoostProfileJson.instance.encodeToString(MindBoostProfileDto(schemaVersion = 999, userId = "u1"))
        store.put("mbprofile:u1", future) // key mirrors LocalMindBoostProfileRepository's prefix
        assertNull(repo.load("u1"))
    }

    @Test
    fun aPreCutoverV1Snapshot_isDiscardedOnLoad_notResurfaced() = runTest {
        // SYSTEM-1 6R3 R3-FINAL D1/D4 migration: `readVersioned` only quarantines FUTURE versions, so an OLDER
        // (pre-cutover) v1 snapshot is still parseable and would resurface Focus/Logic dimensions ESTABLISHED off
        // the removed unsound estimator (or a global scalar). load() must discard it → null, forcing a recompute.
        val store = InMemorySecureKeyValueStore()
        val legacyV1 = MindBoostProfileDto(schemaVersion = 1, userId = "u1", updatedAtMs = 100L)
        store.put("mbprofile:u1", MindBoostProfileJson.instance.encodeToString(legacyV1))
        assertNull(
            LocalMindBoostProfileRepository(store).load("u1"),
            "a pre-cutover (v1) snapshot must be discarded on load, never resurfacing legacy ESTABLISHED",
        )
    }

    @Test
    fun corruptBlob_isQuarantined_andLoadsNull_neverCrashes() = runTest {
        // The future-version test covers a VALID DTO; this covers a MALFORMED blob (parser throws). The
        // read path must quarantine it and return null — never crash, never surface a fabricated profile.
        val store = InMemorySecureKeyValueStore()
        val repo = LocalMindBoostProfileRepository(store)
        store.put("mbprofile:u1", "{ not valid profile json") // key mirrors the repo's prefix
        assertNull(repo.load("u1"), "a corrupt snapshot loads as null (never crashes, never fabricates)")
    }

    @Test
    fun clear_removesSnapshot() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalMindBoostProfileRepository(store)
        repo.save(aggregator.aggregate("u1", listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.6, conf = 0.8)), 1L))
        assertNotNull(repo.load("u1"))
        repo.clear("u1")
        assertNull(repo.load("u1"))
    }
}

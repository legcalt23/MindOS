package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifica el contrato de [SpeedMeasurementStore] sobre AMBAS implementaciones (in-memory
 * y cifrada), centrando el borrado transaccional por usuario y la limpieza de huérfanos
 * (§16, acceptance #20/#21).
 */
class SpeedMeasurementStoreTest {

    private val ms = SpeedTestFixtures.MS

    private fun stores(): List<SpeedMeasurementStore> = listOf(
        InMemorySpeedMeasurementStore(),
        SecureSpeedMeasurementStore(InMemorySecureKeyValueStore()),
    )

    private fun blockMetrics(blockId: String): BlockMetrics = BlockMetrics(
        blockId = blockId,
        totalTrials = 2,
        pc = 1.0,
        rtMedianNanos = 300.0 * ms,
        rtMeanNanos = 300.0 * ms,
        cv = 0.1,
        lapseRate = 0.0,
        anticipationRate = 0.0,
        faRate = null,
        omissionRate = 0.0,
        impIdx = 0.0,
        fatIdx = null,
        vce = 2.0,
        timestampSource = SpeedTestFixtures.context.timestampSource,
        inputTimestampPrecision = SpeedTestFixtures.context.inputTimestampPrecision,
        timebase = SpeedTestFixtures.context.timebase,
    )

    private fun sessionMetrics(sessionId: String): SessionMetrics = SessionMetrics(
        sessionId = sessionId,
        totalTrials = 2,
        pc = 1.0,
        rtMedianNanos = 300.0 * ms,
        rtMeanNanos = 300.0 * ms,
        cv = 0.1,
        lapseRate = 0.0,
        anticipationRate = 0.0,
        faRate = null,
        omissionRate = 0.0,
        impIdx = 0.0,
        fatIdx = null,
        vce = 2.0,
        timestampSource = SpeedTestFixtures.context.timestampSource,
        inputTimestampPrecision = SpeedTestFixtures.context.inputTimestampPrecision,
        timebase = SpeedTestFixtures.context.timebase,
    )

    private fun buildRun(userId: String, sessionId: String, device: DeviceProfile, trialCount: Int): MeasurementRun {
        val blockId = "$sessionId:b0"
        return SpeedTestFixtures.run(
            userId = userId,
            sessionId = sessionId,
            device = device,
            blocks = listOf(SpeedTestFixtures.block(sessionId, blockId)),
            trials = (0 until trialCount).map { SpeedTestFixtures.trial(blockId, it, 300 * ms, isCorrect = true) },
            blockMetrics = listOf(blockMetrics(blockId)),
            sessionMetrics = sessionMetrics(sessionId),
            qualityLogs = listOf(SpeedTestFixtures.qualityLog(sessionId)),
        )
    }

    @Test
    fun saveRun_thenQueriesRoundTrip() = runTest {
        for (store in stores()) {
            val device = SpeedTestFixtures.deviceProfile("dev-rt")
            val run = buildRun("u1", "u1:s1", device, trialCount = 2)
            store.saveRun(run)

            assertNotNull(store.session("u1:s1"))
            assertEquals(2, store.trialsForBlock("u1:s1:b0").size)
            assertEquals(1, store.blocksForSession("u1:s1").size)
            assertEquals(1, store.blockMetricsForSession("u1:s1").size)
            assertNotNull(store.sessionMetrics("u1:s1"))
            assertNotNull(store.versionSnapshot("u1:s1"))
            assertEquals(1, store.qualityLogs("u1:s1").size)
            assertNotNull(store.deviceProfile("dev-rt"))
            assertNotNull(store.loadRun("u1:s1"))
        }
    }

    @Test
    fun invalidatedKey_isQuarantinedAndDoesNotBecomeMissingSilently() = runTest {
        val secureStore = InMemorySecureKeyValueStore()
        val store = SecureSpeedMeasurementStore(secureStore)
        val run = buildRun("u1", "u1:s1", SpeedTestFixtures.deviceProfile("dev-rt"), trialCount = 2)
        store.saveRun(run)
        val key = "run:u1::u1:s1"
        secureStore.simulateReadFailure(key, SecureReadResult.KeyInvalidated)

        assertNull(store.loadRun("u1:s1"))

        assertEquals(SecureReadResult.Missing, secureStore.read(key))
        assertNotNull(secureStore.quarantinedValue(key))
        assertEquals(SecureReadResult.KeyInvalidated, secureStore.quarantineFailure(key))
    }

    @Test
    fun deleteUserData_isTransactional_keepsOtherUsers_cleansOrphans_keepsSharedDevice() = runTest {
        for (store in stores()) {
            val shared = SpeedTestFixtures.deviceProfile("dev-shared")
            val onlyA = SpeedTestFixtures.deviceProfile("dev-onlyA")

            store.saveRun(buildRun("userA", "userA:s1", shared, trialCount = 2))
            store.saveRun(buildRun("userA", "userA:s2", onlyA, trialCount = 3))
            store.saveRun(buildRun("userB", "userB:s1", shared, trialCount = 4))

            val report = store.deleteUserData("userA")

            // Datos de userA borrados (cascada completa).
            assertTrue(store.sessionsForUser("userA").isEmpty())
            assertNull(store.session("userA:s1"))
            assertTrue(store.trialsForBlock("userA:s1:b0").isEmpty())
            assertNull(store.sessionMetrics("userA:s2"))

            // Datos de userB intactos.
            assertEquals(1, store.sessionsForUser("userB").size)
            assertEquals(4, store.trialsForBlock("userB:s1:b0").size)

            // DeviceProfile compartido se conserva (aún referenciado por userB); el huérfano se limpia.
            assertNotNull(store.deviceProfile("dev-shared"))
            assertNull(store.deviceProfile("dev-onlyA"))

            // Recuento del borrado.
            assertEquals(2, report.sessions)
            assertEquals(2, report.blocks)
            assertEquals(5, report.trials)
            assertEquals(2, report.blockMetrics)
            assertEquals(2, report.sessionMetrics)
            assertEquals(2, report.versionSnapshots)
            assertEquals(2, report.qualityLogs)
            assertEquals(1, report.orphanDeviceProfiles)
        }
    }
}

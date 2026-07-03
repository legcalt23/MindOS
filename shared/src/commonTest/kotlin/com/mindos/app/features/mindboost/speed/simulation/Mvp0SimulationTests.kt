package com.mindos.app.features.mindboost.speed.simulation

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.data.persistence.InMemorySpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Suite de validación sintética del §12 (acceptance #4/#23). DEBE pasar al 100% ANTES de
 * cualquier uso físico. Valida tanto la integridad de las métricas como la persistencia.
 */
class Mvp0SimulationTests {

    private val ms = SpeedTestFixtures.MS
    private val runner = SyntheticUserRunner()

    private fun config(
        sessionId: String,
        exerciseId: SpeedExerciseId = SpeedExerciseId.E1_DESTELLO,
        blockCount: Int = 2,
        trialsPerBlock: Int = 20,
        seed: Long = 42L,
    ) = SyntheticRunConfig(
        userId = "sim-user",
        sessionId = sessionId,
        deviceProfile = SpeedTestFixtures.deviceProfile("sim-device", apiLevel = 34),
        context = SpeedTestFixtures.context,
        exerciseId = exerciseId,
        blockCount = blockCount,
        trialsPerBlock = trialsPerBlock,
        nowEpochMs = 1_700_000_000_000L,
        seed = seed,
    )

    @Test
    fun impulsiveProfile_hasHighAnticipation_andLowQuality() = runTest {
        val run = runner.run(SyntheticProfile.IMPULSIVE, config("imp"))
        val metrics = assertNotNull(run.sessionMetrics)
        assertTrue(metrics.anticipationRate > 0.15, "anticipationRate=${metrics.anticipationRate}")
        assertNotNull(metrics.impIdx)
        val quality = run.qualityLogs.first().sessionQuality
        assertTrue(quality == SessionQuality.LOW || quality == SessionQuality.CONTAMINATED, "quality=$quality")
        assertPersists(run)
    }

    @Test
    fun slowPreciseProfile_isAccurate_slow_andHighQuality() = runTest {
        val run = runner.run(SyntheticProfile.SLOW_PRECISE, config("slow"))
        val metrics = assertNotNull(run.sessionMetrics)
        assertTrue(metrics.pc > 0.90, "pc=${metrics.pc}")
        assertNotNull(metrics.rtMedianNanos)
        assertTrue(metrics.rtMedianNanos!! > 500 * ms, "rtMedianNanos=${metrics.rtMedianNanos}")
        assertEquals(SessionQuality.HIGH, run.qualityLogs.first().sessionQuality)
        assertPersists(run)
    }

    @Test
    fun fatigableProfile_fatIdxNullBeforeThreeBlocks_computableAfter() = runTest {
        val twoBlocks = runner.run(SyntheticProfile.FATIGABLE, config("fat2", blockCount = 2, trialsPerBlock = 15))
        assertNull(twoBlocks.sessionMetrics?.fatIdx, "fatIdx debe ser null con < 3 bloques")

        val fiveBlocks = runner.run(SyntheticProfile.FATIGABLE, config("fat5", blockCount = 5, trialsPerBlock = 15))
        assertNotNull(fiveBlocks.sessionMetrics?.fatIdx, "fatIdx debe ser computable con >= 3 bloques")
        // El TR mediano del último bloque supera al del primero (degradación tras bloque 3).
        val firstRt = fiveBlocks.blockMetrics.first().rtMedianNanos
        val lastRt = fiveBlocks.blockMetrics.last().rtMedianNanos
        assertNotNull(firstRt); assertNotNull(lastRt)
        assertTrue(lastRt!! > firstRt!!, "last=$lastRt first=$firstRt")
        assertPersists(fiveBlocks)
    }

    @Test
    fun edgeCase_zeroTrials_blockDiscarded_runStillPersists() = runTest {
        val run = runner.run(SyntheticProfile.SLOW_PRECISE, config("edge0", blockCount = 1, trialsPerBlock = 0))
        assertTrue(run.trials.isEmpty())
        assertTrue(run.blockMetrics.isEmpty(), "bloque de 0 trials se descarta (métricas null)")
        assertEquals(0.0, run.sessionMetrics?.pc)
        assertNull(run.sessionMetrics?.rtMedianNanos)
        assertPersists(run)
    }

    @Test
    fun edgeCase_zeroCorrect_pcZero_rtNull() = runTest {
        val allErrors = SyntheticProfile(
            "all_errors", baseRtMs = 300.0, rtSpreadMs = 20.0, accuracy = 0.0, anticipationRate = 0.0,
        )
        val run = runner.run(allErrors, config("edgeErr", blockCount = 1, trialsPerBlock = 10))
        val block = assertNotNull(run.blockMetrics.firstOrNull())
        assertEquals(0.0, block.pc)
        assertNull(block.rtMedianNanos)
        assertNull(block.vce)
    }

    @Test
    fun edgeCase_oneCorrect_rtPresent_cvNull() = runTest {
        val perfect = SyntheticProfile(
            "one", baseRtMs = 300.0, rtSpreadMs = 0.0, accuracy = 1.0, anticipationRate = 0.0,
        )
        val run = runner.run(perfect, config("edgeOne", blockCount = 1, trialsPerBlock = 1))
        val block = assertNotNull(run.blockMetrics.firstOrNull())
        assertEquals(1, block.totalTrials)
        assertNotNull(block.rtMedianNanos)
        assertNull(block.cv)
    }

    @Test
    fun edgeCase_allAnticipations_treatedAsZeroCorrect() = runTest {
        val anticipator = SyntheticProfile(
            "anticipate", baseRtMs = 300.0, rtSpreadMs = 10.0, accuracy = 0.9, anticipationRate = 1.0,
        )
        val run = runner.run(anticipator, config("edgeAnt", blockCount = 1, trialsPerBlock = 10))
        val block = assertNotNull(run.blockMetrics.firstOrNull())
        assertEquals(0.0, block.pc)
        assertNull(block.rtMedianNanos)
        assertEquals(1.0, block.anticipationRate)
    }

    /** Persistencia íntegra: guardar y recargar reproduce la misma corrida. */
    private suspend fun assertPersists(run: com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun) {
        val store = InMemorySpeedMeasurementStore()
        store.saveRun(run)
        val loaded = assertNotNull(store.loadRun(run.session.id))
        assertEquals(run.trials.size, loaded.trials.size)
        assertEquals(run.blocks.size, loaded.blocks.size)
        assertEquals(run.blockMetrics.size, loaded.blockMetrics.size)
        assertEquals(run.session.userId, loaded.session.userId)
        assertEquals(run.qualityLogs.size, loaded.qualityLogs.size)
    }
}

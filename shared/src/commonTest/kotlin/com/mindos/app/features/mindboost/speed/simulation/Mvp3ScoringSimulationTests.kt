package com.mindos.app.features.mindboost.speed.simulation

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.data.persistence.InMemorySpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.score.SpeedMacroCategory
import com.mindos.app.features.mindboost.speed.domain.score.SpeedScoreEngine
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validación sintética del pipeline de MVP-3: medición → SessionMetrics → [SpeedScoreEngine]
 * → perfil. Verifica que el score es coherente con el perfil conductual y, sobre todo, que
 * es **SIEMPRE preliminar** (no autoritativo) mientras el gate físico §3/§15 esté abierto.
 */
class Mvp3ScoringSimulationTests {

    private val runner = SyntheticUserRunner()
    private val scoreEngine = SpeedScoreEngine()

    private fun sessionMetricsFor(profile: SyntheticProfile, seed: Long = 11L): SessionMetrics {
        val run = runner.run(
            profile,
            SyntheticRunConfig(
                userId = "score-user",
                sessionId = "score:${profile.name}",
                deviceProfile = SpeedTestFixtures.deviceProfile("score-device", apiLevel = 34),
                context = SpeedTestFixtures.context,
                exerciseId = SpeedExerciseId.E6_MULTIVIA,
                blockCount = 4,
                trialsPerBlock = 25,
                nowEpochMs = 1_700_000_000_000L,
                seed = seed,
            ),
        )
        return assertNotNull(run.sessionMetrics)
    }

    @Test
    fun slowPrecise_hasHighControl_andIsPreliminary() {
        val score = scoreEngine.score(sessionMetricsFor(SyntheticProfile.SLOW_PRECISE))
        assertNotNull(score.control)
        assertTrue(score.control!! > 0.7, "control=${score.control}")
        assertTrue(score.macroCategory != SpeedMacroCategory.FAST_IMPULSIVE)
        assertTrue(score.preliminary)
    }

    @Test
    fun impulsive_lowControl_ceilingPenalized_andPreliminary() {
        val score = scoreEngine.score(sessionMetricsFor(SyntheticProfile.IMPULSIVE))
        assertNotNull(score.control)
        assertTrue(score.control!! < 0.6, "control=${score.control}")
        assertTrue(score.ceilingPenaltyApplied, "pc baja debe disparar el techo del §9")
        assertTrue(score.preliminary)
    }

    @Test
    fun impulsiveTotal_isBelowSlowPreciseTotal() {
        val impulsive = scoreEngine.score(sessionMetricsFor(SyntheticProfile.IMPULSIVE))
        val precise = scoreEngine.score(sessionMetricsFor(SyntheticProfile.SLOW_PRECISE))
        assertNotNull(impulsive.total)
        assertNotNull(precise.total)
        assertTrue(impulsive.total!! < precise.total!!, "impulsivo=${impulsive.total} preciso=${precise.total}")
    }

    @Test
    fun scoreIsAlwaysPreliminary_forEveryProfile() {
        listOf(SyntheticProfile.IMPULSIVE, SyntheticProfile.SLOW_PRECISE, SyntheticProfile.FATIGABLE).forEach {
            assertTrue(scoreEngine.score(sessionMetricsFor(it)).preliminary, "perfil ${it.name} debe ser preliminar")
        }
    }

    @Test
    fun scoring_isReproducibleFromPersistedRun() = runTest {
        val store = InMemorySpeedMeasurementStore()
        val run = runner.run(
            SyntheticProfile.SLOW_PRECISE,
            SyntheticRunConfig(
                userId = "u",
                sessionId = "u:persist",
                deviceProfile = SpeedTestFixtures.deviceProfile("d", apiLevel = 34),
                context = SpeedTestFixtures.context,
                exerciseId = SpeedExerciseId.E6_MULTIVIA,
                blockCount = 3,
                trialsPerBlock = 20,
                nowEpochMs = 1_700_000_000_000L,
                seed = 3L,
            ),
        )
        store.saveRun(run)
        val loaded = assertNotNull(store.loadRun("u:persist"))
        val fromRun = scoreEngine.score(assertNotNull(run.sessionMetrics))
        val fromStore = scoreEngine.score(assertNotNull(loaded.sessionMetrics))
        assertEquals(fromRun, fromStore)
    }
}

package com.mindos.app.features.mindboost.speed.simulation

import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.domain.DefaultStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.MultiSignalStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.StaircaseConfig
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validación sintética del bucle adaptativo de MVP-1 (§12). Alimenta el [DefaultStaircaseEngine]
 * con las `BlockMetrics` que produce el [SyntheticUserRunner] y verifica que la dificultad
 * (CCE) y el deadline reaccionan correctamente a la precisión de cada perfil — SIN dispositivo
 * y SIN que la VCE intervenga.
 */
class StaircaseSimulationTests {

    private val runner = SyntheticUserRunner()

    private fun blockSeries(
        profile: SyntheticProfile,
        blocks: Int = 6,
        trials: Int = 25,
        seed: Long = 7L,
    ): List<BlockMetrics> {
        val run = runner.run(
            profile,
            SyntheticRunConfig(
                userId = "stair-user",
                sessionId = "stair:${profile.name}",
                deviceProfile = SpeedTestFixtures.deviceProfile("stair-device", apiLevel = 34),
                context = SpeedTestFixtures.context,
                exerciseId = SpeedExerciseId.E6_MULTIVIA,
                blockCount = blocks,
                trialsPerBlock = trials,
                nowEpochMs = 1_700_000_000_000L,
                seed = seed,
            ),
        )
        return run.blockMetrics
    }

    @Test
    fun slowPreciseProfile_drivesDifficultyUp_andDeadlineDown() {
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        val initialDeadline = engine.getNextDeadline(engine.currentCce())!!
        var finalCce = engine.currentCce()
        for (metrics in blockSeries(SyntheticProfile.SLOW_PRECISE)) {
            finalCce = engine.updateDifficulty(metrics)
        }
        assertTrue(finalCce > 0.5, "pc alta debe subir CCE; final=$finalCce")
        assertTrue(engine.getNextDeadline(finalCce)!! < initialDeadline, "deadline debe acortarse")
    }

    @Test
    fun impulsiveProfile_drivesDifficultyDown() {
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        var finalCce = engine.currentCce()
        for (metrics in blockSeries(SyntheticProfile.IMPULSIVE)) {
            finalCce = engine.updateDifficulty(metrics)
        }
        assertTrue(finalCce < 0.5, "pc baja (impulsivo) debe bajar CCE; final=$finalCce")
    }

    @Test
    fun midAccuracyProfile_holdsDifficulty() {
        // Perfil con pc en la banda de mantenimiento [0.70, 0.85).
        val mid = SyntheticProfile(
            "mid", baseRtMs = 350.0, rtSpreadMs = 25.0, accuracy = 0.78, anticipationRate = 0.02,
        )
        val engine = DefaultStaircaseEngine(initialCce = 0.5)
        var finalCce = engine.currentCce()
        for (metrics in blockSeries(mid)) {
            finalCce = engine.updateDifficulty(metrics)
        }
        // Con pc en banda media la CCE no debería alejarse mucho del inicio.
        assertTrue(finalCce in 0.40..0.60, "CCE debe mantenerse cerca del inicio; final=$finalCce")
    }

    @Test
    fun staircaseConverges_deadlineStaysWithinConfiguredBounds() {
        val config = StaircaseConfig.DEFAULT
        val engine = DefaultStaircaseEngine(config)
        for (metrics in blockSeries(SyntheticProfile.SLOW_PRECISE, blocks = 12)) {
            engine.updateDifficulty(metrics)
        }
        val deadline = engine.getNextDeadline(engine.currentCce())!!
        assertTrue(deadline in config.minDeadlineNanos..config.maxDeadlineNanos, "deadline=$deadline fuera de rango")
    }

    @Test
    fun multiSignal_slowPrecise_raisesDifficulty() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        var finalCce = engine.currentCce()
        for (metrics in blockSeries(SyntheticProfile.SLOW_PRECISE)) {
            finalCce = engine.updateDifficulty(metrics)
        }
        assertTrue(finalCce > 0.5, "multi-señal: pc alta y estable sube la dificultad; final=$finalCce")
    }

    @Test
    fun multiSignal_impulsive_lowersDifficulty() {
        val engine = MultiSignalStaircaseEngine(initialCce = 0.5)
        var finalCce = engine.currentCce()
        for (metrics in blockSeries(SyntheticProfile.IMPULSIVE)) {
            finalCce = engine.updateDifficulty(metrics)
        }
        assertTrue(finalCce < 0.5, "multi-señal: impulsivo baja la dificultad; final=$finalCce")
    }

    @Test
    fun deterministicSeed_reproducesSameTrajectory() {
        fun finalCce(): Double {
            val engine = DefaultStaircaseEngine(initialCce = 0.5)
            blockSeries(SyntheticProfile.IMPULSIVE, seed = 99L).forEach { engine.updateDifficulty(it) }
            return engine.currentCce()
        }
        assertEquals(finalCce(), finalCce(), "El simulador sembrado debe ser reproducible")
    }
}

@file:Suppress("MagicNumber") // Synthetic noise span / bands / network defaults are documented constants.

package com.mindos.app.features.arena.simulation.runner

import com.mindos.app.features.arena.comparability.model.DeviceClass
import com.mindos.app.features.arena.config.OsFamily
import com.mindos.app.features.arena.config.RefreshRateTier
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.integrity.model.IntegritySignals
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.model.JitterMs
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.session.model.SessionEnvironmentSnapshot
import com.mindos.app.features.arena.simulation.generators.AbuseType
import com.mindos.app.features.arena.simulation.generators.LatencyJitterGenerator
import com.mindos.app.features.arena.simulation.generators.SyntheticPlayer
import kotlin.random.Random

/** Phase 07: A fully generated synthetic session ready to feed the rating-update runner. */
data class GeneratedSession(
    val motorType: TaskMotorType,
    val taskDifficulty: Double,
    val observedPerformance: Double,
    val environment: SessionEnvironmentSnapshot,
    val hardwareClass: HardwareClass,
    val abuseSignals: IntegritySignals,
)

/**
 * Phase 07: Generates complete synthetic sessions (session_generator.md): observed performance from
 * true skill + noise, an environment snapshot (latency/jitter correlated with device & abuse), and
 * the integrity signals implied by the player's abuse archetype. Deterministic given the [Random].
 */
class SessionGenerator(
    private val engine: BayesianRatingEngine = BayesianRatingEngine(),
    private val latencyGenerator: LatencyJitterGenerator = LatencyJitterGenerator(),
) {

    fun generateSession(
        player: SyntheticPlayer,
        motorType: TaskMotorType,
        blockId: String,
        taskDifficulty: Double,
        random: Random,
    ): GeneratedSession {
        val trueSkill = player.skillByBlock[blockId] ?: 1500.0
        val expected = engine.expectedTaskPerformance(trueSkill, taskDifficulty)
        val observed = (expected + (random.nextDouble() - 0.5) * NOISE_SPAN).coerceIn(0.0, 1.0)
        val (latency, jitter) = latencyGenerator.generateLatency(player.deviceClass, player.abuseType, random)
        return GeneratedSession(
            motorType = motorType,
            taskDifficulty = taskDifficulty,
            observedPerformance = observed,
            environment = environmentFor(player, latency, jitter),
            hardwareClass = hardwareFor(player.deviceClass),
            abuseSignals = abuseSignalsFor(player.abuseType),
        )
    }

    private fun environmentFor(
        player: SyntheticPlayer,
        latency: Long,
        jitter: Long,
    ): SessionEnvironmentSnapshot = SessionEnvironmentSnapshot(
        deviceClass = player.deviceClass.name,
        inputClass = "TOUCH",
        fpsBand = refreshBandFor(player.deviceClass),
        refreshRateBand = refreshBandFor(player.deviceClass),
        screenSizeBand = "MEDIUM",
        latencyMs = LatencyMs(latency),
        jitterMs = JitterMs(jitter),
        networkInstability = 0.1,
        languageGroup = player.languageGroup,
        accessibilityClass = player.accessibilityClass,
        clientVersion = "SIM",
        osVersion = "SIM",
        appBuild = "SIM",
        timestampMs = 0L,
    )

    private fun abuseSignalsFor(type: AbuseType): IntegritySignals = when (type) {
        AbuseType.CLEAN -> IntegritySignals()
        AbuseType.SMURF -> IntegritySignals(smurf = true)
        AbuseType.BOOSTER, AbuseType.SOLVER -> IntegritySignals(boosting = true)
        AbuseType.COLLUDER -> IntegritySignals(collusion = true)
        AbuseType.LAG_SWITCHER -> IntegritySignals(lagSwitch = true)
        AbuseType.ACCOUNT_SHARER -> IntegritySignals(accountSharing = true)
    }

    private fun refreshBandFor(deviceClass: DeviceClass): Int = when (deviceClass) {
        DeviceClass.HIGH_END_120HZ -> 120
        DeviceClass.MID_RANGE_90HZ -> 90
        DeviceClass.LOW_END_60HZ -> 60
        DeviceClass.UNKNOWN -> 60
    }

    private fun hardwareFor(deviceClass: DeviceClass): HardwareClass = when (deviceClass) {
        DeviceClass.HIGH_END_120HZ -> HardwareClass(OsFamily.ANDROID, RefreshRateTier.HZ_120)
        DeviceClass.MID_RANGE_90HZ -> HardwareClass(OsFamily.ANDROID, RefreshRateTier.HZ_90)
        DeviceClass.LOW_END_60HZ -> HardwareClass(OsFamily.ANDROID, RefreshRateTier.HZ_60)
        DeviceClass.UNKNOWN -> HardwareClass(OsFamily.OTHER, RefreshRateTier.UNKNOWN)
    }

    companion object {
        private const val NOISE_SPAN = 0.2
    }
}

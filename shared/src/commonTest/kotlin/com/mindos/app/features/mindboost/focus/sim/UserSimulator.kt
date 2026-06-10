package com.mindos.app.features.mindboost.focus.sim

import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E4FrenoGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ReactionTimeMs
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.TrialId
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import kotlin.random.Random

/**
 * A virtual user that returns a response latency (ms) for a stimulus, or null for
 * no response. Drives the end-to-end loop (generator → classify → metrics →
 * adaptation) without a device, so the system's reactions can be asserted.
 */
fun interface UserSimulator {
    fun respondLatencyMs(stimulus: Stimulus, trialIndex: Int, totalTrials: Int, random: Random): Long?
}

/** The five behavioural profiles from 11_testing_validation_plan.md §6. */
object Simulators {

    /** Fast everywhere, commits on no-go trials (fast false alarms). */
    val IMPULSIVE = UserSimulator { stimulus, _, _, random ->
        if (stimulus.isTarget) jitter(random, 235) // fast hit
        else if (random.nextDouble() < 0.55) jitter(random, 195) else null // fast commission
    }

    /** Slow but correct: responds to targets late, withholds otherwise. */
    val SLOW_BUT_ACCURATE = UserSimulator { stimulus, _, _, random ->
        if (stimulus.isTarget) jitter(random, 470) else null
    }

    /** Fast and accurate, low variability. */
    val ADVANCED = UserSimulator { stimulus, _, _, random ->
        if (stimulus.isTarget) jitter(random, 315, spread = 12) else null
    }

    /** RT drifts up across the block and late targets are missed (lapses + rising RT-CV). */
    val FATIGUED = UserSimulator { stimulus, index, total, random ->
        if (!stimulus.isTarget) return@UserSimulator null
        val progress = index.toDouble() / total
        if (progress > 0.7 && random.nextDouble() < 0.30) return@UserSimulator null // late lapse / miss
        val base = (300 + progress * 420).toLong()
        jitter(random, base, spread = (40 * (1 + progress)).toInt())
    }

    /** E4 gamer: strategically slow on go trials so stops almost always succeed (breaks SSRT). */
    val GAMER_E4 = UserSimulator { stimulus, _, _, random ->
        if (stimulus.requiresStop) null // waits long enough to abort → successful stop
        else jitter(random, 690, spread = 25) // deliberately slow go responses
    }

    private fun jitter(random: Random, center: Long, spread: Int = 25): Long =
        (center + random.nextInt(spread * 2 + 1) - spread).coerceAtLeast(0)
}

/** Runs a simulator through the real go/no-go pipeline (generator → assembler → metrics). */
object SimulatedLoop {
    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())

    @Suppress("LongParameterList") // Test harness: each argument is an intrinsic input of one simulated block.
    fun runBlock(
        generator: TrialGenerator,
        simulator: UserSimulator,
        definition: ExerciseDefinition,
        difficulty: DifficultyVector,
        trialCount: Int,
        seed: Long,
        thresholds: IndividualThresholds = IndividualThresholds(),
    ): BlockResult {
        val stimuli = generator.generate(difficulty, BlockSpec(trialCount, Mode.TRAINING), seed)
        val random = Random(seed xor SIM_SEED_MIX)
        val recorded = stimuli.mapIndexed { i, s ->
            FocusSessionAssembler.RecordedTrial(s, simulator.respondLatencyMs(s, i, trialCount, random))
        }
        return assembler.assembleBlock(
            blockId = BlockId("sim"),
            definition = definition,
            mode = Mode.TRAINING,
            difficultyStart = difficulty,
            difficultyEnd = difficulty,
            thresholds = thresholds,
            recorded = recorded,
            sessionIdPrefix = "sim",
            epochMillis = 0,
        )
    }

    /**
     * Stop-signal pipeline (E4): the assembler's go/no-go classifier does not model
     * stop trials, so this applies stop-signal semantics (failed stop = false alarm,
     * successful stop = correct rejection) and drives the SSD staircase online.
     */
    fun runStopSignalMetrics(
        simulator: UserSimulator,
        difficulty: DifficultyVector,
        trialCount: Int,
        seed: Long,
        thresholds: IndividualThresholds = IndividualThresholds(),
    ): MetricSet {
        val generator = E4FrenoGenerator()
        val stimuli = generator.generate(difficulty, BlockSpec(trialCount, Mode.TRAINING), seed)
        val tracker = generator.tracker(difficulty)
        val random = Random(seed xor SIM_SEED_MIX)
        val trials = stimuli.mapIndexed { i, s ->
            val ssd = if (s.requiresStop) tracker.currentSsdMs else null
            val latency = simulator.respondLatencyMs(s, i, trialCount, random)
            val responded = latency != null && latency in 0..s.responseWindowMs
            if (s.requiresStop) tracker.onStopOutcome(successfulStop = !responded)
            classifyStop(i, s, latency, responded, ssd, difficulty)
        }
        return DefaultMetricsEngine().blockMetrics(trials, definition(ExerciseId.E4_FRENO), thresholds)
    }

    @Suppress("LongParameterList") // Test harness: builds one stop-signal trial from its raw inputs.
    private fun classifyStop(
        index: Int,
        stimulus: Stimulus,
        latency: Long?,
        responded: Boolean,
        ssd: Long?,
        difficulty: DifficultyVector,
    ): TrialResult {
        val outcome = when {
            stimulus.requiresStop && responded -> TrialOutcome.FALSE_ALARM
            stimulus.requiresStop -> TrialOutcome.CORRECT_REJECTION
            responded -> TrialOutcome.HIT
            else -> TrialOutcome.MISS
        }
        val rt = latency?.takeIf { responded }?.let { ReactionTimeMs(it) }
        return TrialResult(
            trialId = TrialId("simE4:$index"),
            exerciseId = ExerciseId.E4_FRENO,
            mode = Mode.TRAINING,
            stimulusIsTarget = stimulus.isTarget,
            requiredStop = stimulus.requiresStop,
            outcome = outcome,
            reactionTime = rt,
            difficulty = difficulty,
            stopSignalDelayMs = ssd,
            timestampMs = index.toLong(),
        )
    }

    private fun definition(id: ExerciseId): ExerciseDefinition =
        com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog.definition(id)

    private const val SIM_SEED_MIX = 0x5DEECE66DL
}

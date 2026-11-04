package com.mindos.app.features.mindboost.focus

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.ForegroundLoss
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.adaptive.FatigueLevel
import com.mindos.app.features.mindboost.focus.domain.adaptive.FatigueSignal
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols
import com.mindos.app.features.mindboost.focus.domain.evaluation.DefaultCalibrationEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MeasurementSufficiency
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ReactionTimeMs
import com.mindos.app.features.mindboost.focus.domain.model.TrialId
import com.mindos.app.features.mindboost.focus.domain.model.TrialMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultArchetypeInference
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultOvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultPersonalizedSessionScorer
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultProfileEvolution
import com.mindos.app.features.mindboost.focus.domain.repository.BaselineRepository
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.ProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent

/**
 * A lifecycle probe that really watched and saw the screen stay in the foreground — the ONLY input that lets
 * `SessionQualityGate` admit a session. Tests that assert a published measurement must wire this; the holders'
 * default is fail-closed on purpose, so a test that forgets it gets a withheld measurement, not a pass.
 */
object CleanLifecycleProbe : AppLifecycleProbe {
    override fun start() = Unit
    override fun stop(): LifecycleObservation = LifecycleObservation.Observed(interruptions = emptyList())
}

/** A probe that watched and SAW the screen leave the foreground mid-session. */
object InterruptedLifecycleProbe : AppLifecycleProbe {
    override fun start() = Unit
    override fun stop(): LifecycleObservation =
        LifecycleObservation.Observed(listOf(ForegroundLoss(lostAtNanos = 1_000L, regainedAtNanos = 2_000L)))
}

/**
 * A monotonic clock that advances with the test's VIRTUAL time. The default fixture clock is a constant, which
 * makes every reaction time 0 ms — below the 150 ms anticipation floor — so every response would be recorded as
 * an anticipation. Any test that asserts a published measurement needs this.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.virtualMonotonicClock(): MonotonicClock = MonotonicClock { testScheduler.currentTime * NANOS_PER_MS }

/**
 * Drives a fixed-block run to the end while RESPONDING, then settles. Call it right after `start()`.
 *
 * `advanceUntilIdle()` on its own lets every trial time out, and a session with zero responses is
 * `NotMeasurable(NO_RESPONSES)` — a withheld measurement, not a pass. So this waits for each stimulus to be on
 * screen and responds [REACTION_MS] later: long enough to clear the anticipation floor, short enough to land
 * inside any response window this lane uses. Pair it with [virtualMonotonicClock] or the RT is still 0.
 *
 * R15 · it plays the task, it does not tap. An earlier version answered EVERY visible field, which in a
 * target-rare lane IS the blind "respond to everything" strategy: it lands below the blind ceiling, so
 * `FocusAntiGaming` now withholds such a session and a fixture that plays blind cannot stand in for an honest
 * one. See [honestResponseTo] for what it answers and what it declines to model.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.driveWithResponses(fieldNow: () -> FocusFieldState?, respond: (ResponseKey) -> Unit) {
    runCurrent()
    var steps = 0
    while (true) {
        val field = fieldNow() ?: break
        val key = honestResponseTo(field)
        if (key == null) {
            advanceTimeBy(POLL_MS)
        } else {
            advanceTimeBy(REACTION_MS)
            respond(key)
        }
        runCurrent()
        check(steps++ < MAX_DRIVE_STEPS) { "The run never finished within the virtual-time budget." }
    }
    advanceUntilIdle()
}

/**
 * What a player who is DOING the task would answer, read off the same screen the user sees. `null` means
 * withhold: a gap, feedback, a lure, or a stop cue — withholding is the correct response there, and answering
 * it anyway is a commission.
 *
 * DECLARED CEILING: the remaining fields (search, rule switch, anchor, tracking, stream, dual) keep the old
 * "press PRIMARY" behaviour, so a fixture built on them is still an all-tap player. Nothing drives them through
 * an admitted session today; the day one does, it must be taught to play them or it will be withheld — which is
 * the gate working, not a regression.
 */
internal fun honestResponseTo(field: FocusFieldState): ResponseKey? = when (field) {
    FocusFieldState.Idle, FocusFieldState.Stop -> null
    is FocusFieldState.Feedback, is FocusFieldState.Lure -> null
    is FocusFieldState.Choice -> field.targetDirection
    else -> ResponseKey.PRIMARY
}

private const val NANOS_PER_MS = 1_000_000L

/** Sampling grain while waiting for a stimulus, so an onset is seen within one step of happening. */
private const val POLL_MS = 100L

/** Measured RT lands in [REACTION_MS, REACTION_MS + POLL_MS] — over the 150 ms floor, under every window. */
private const val REACTION_MS = 250L

/** A baseline is ~20 min of virtual time; this is well over that at [POLL_MS] a step. */
private const val MAX_DRIVE_STEPS = 40_000

/** Shared helpers for focus-feature unit tests. */
object FocusTestFixtures {

    /**
     * Builds a [FocusGraph] wired with in-memory repos and the real engines, for state-holder tests.
     * Clocks are injectable so timing is deterministic (and never trips the kotlinx-datetime runtime).
     */
    @Suppress("LongParameterList") // A graph factory naturally exposes its swappable collaborators.
    fun focusGraph(
        monotonicClock: MonotonicClock = MonotonicClock { 0L },
        epochClock: EpochClock = EpochClock { 0L },
        profileRepository: FocusProfileRepository = InMemoryFocusProfileRepository(),
        historyRepository: SessionHistoryRepository = InMemorySessionHistoryRepository(),
        baselineRepository: BaselineRepository = InMemoryBaselineRepository(),
        probeBankRepository: ProbeBankRepository = InMemoryProbeBankRepository(),
        evaluationService: FocusEvaluationService = FocusEvaluationService(),
    ): FocusGraph {
        val metricsEngine = DefaultMetricsEngine()
        val calibrationEngine = DefaultCalibrationEngine()
        return FocusGraph(
            generators = FocusExerciseGenerators.legacy,
            versionedGenerators = FocusExerciseGenerators.versioned,
            metricsEngine = metricsEngine,
            adaptiveEngine = DefaultAdaptiveEngine(),
            assembler = FocusSessionAssembler(metricsEngine),
            monotonicClock = monotonicClock,
            epochClock = epochClock,
            profileEvolution = DefaultProfileEvolution(DefaultArchetypeInference(), epochClock),
            overtrainingGuard = DefaultOvertrainingGuard(),
            personalizedScorer = DefaultPersonalizedSessionScorer(),
            calibrationEngine = calibrationEngine,
            baselineProtocol = BaselineProtocols.INITIAL,
            baselineAssembler = BaselineAssembler(calibrationEngine),
            evaluationService = evaluationService,
            profileRepository = profileRepository,
            historyRepository = historyRepository,
            baselineRepository = baselineRepository,
            probeBankRepository = probeBankRepository,
        )
    }

    @Suppress("LongParameterList") // Test builder mirroring TrialResult's fields.
    fun trial(
        index: Int,
        outcome: TrialOutcome,
        rtMs: Long? = null,
        isTarget: Boolean = outcome == TrialOutcome.HIT || outcome == TrialOutcome.MISS,
        congruent: Boolean? = null,
        requiredStop: Boolean = false,
        stopSignalDelayMs: Long? = null,
        exerciseId: ExerciseId = ExerciseId.E1_VIGIA,
        markers: TrialMarkers = TrialMarkers(congruent = congruent),
    ): TrialResult = TrialResult(
        trialId = TrialId("t$index"),
        exerciseId = exerciseId,
        mode = Mode.TRAINING,
        stimulusIsTarget = isTarget,
        requiredStop = requiredStop,
        outcome = outcome,
        reactionTime = rtMs?.let { ReactionTimeMs(it) },
        difficulty = DifficultyVector.EASIEST,
        timestampMs = 1_000L + index,
        markers = markers,
        stopSignalDelayMs = stopSignalDelayMs,
    )

    /**
     * A MEASURED "no fatigue": a real LOW/not-emerging/not-sustained signal for tests that exercise the
     * performance rules. Production has no such constant on purpose (FOC-P0-02): there, "not measured" is
     * `null` and the engine holds.
     */
    val noFatigueMeasured = FatigueSignal(FatigueLevel.LOW, emerging = false, sustained = false)

    /** Builds N consecutive HIT trials with the given RTs. */
    fun hits(rts: List<Long>, startIndex: Int = 0): List<TrialResult> =
        rts.mapIndexed { i, rt -> trial(startIndex + i, TrialOutcome.HIT, rt) }

    fun metricSet(vararg pairs: Pair<MetricKey, Double>): MetricSet =
        pairs.associate { (key, value) ->
            key to MetricValue(
                key = key,
                value = value,
                n = 50,
                confident = true,
                unit = MetricUnit.RATIO,
                // Derived from the same rule the engine applies, never hand-asserted: at n = 50 a governed
                // metric clears the central floor, and an ungoverned one honestly stays `null`.
                measurementSufficient = MeasurementSufficiency.evaluate(key, nEffective = 50, cellsPresent = true),
            )
        }
}

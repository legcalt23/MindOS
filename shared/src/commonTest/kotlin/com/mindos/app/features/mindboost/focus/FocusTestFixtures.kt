package com.mindos.app.features.mindboost.focus

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols
import com.mindos.app.features.mindboost.focus.domain.evaluation.DefaultCalibrationEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
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
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock

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
            generators = FocusExerciseGenerators.registry,
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

    /** Builds N consecutive HIT trials with the given RTs. */
    fun hits(rts: List<Long>, startIndex: Int = 0): List<TrialResult> =
        rts.mapIndexed { i, rt -> trial(startIndex + i, TrialOutcome.HIT, rt) }

    fun metricSet(vararg pairs: Pair<MetricKey, Double>): MetricSet =
        pairs.associate { (key, value) ->
            key to MetricValue(key, value, n = 50, confident = true, unit = MetricUnit.RATIO)
        }
}

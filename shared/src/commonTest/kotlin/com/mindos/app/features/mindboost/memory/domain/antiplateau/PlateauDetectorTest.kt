package com.mindos.app.features.mindboost.memory.domain.antiplateau

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlateauDetectorTest {

    private val detector = PlateauDetector()
    private val planner = InterventionPlanner()

    /** Healthy baseline (no plateau, no false improvement). */
    private val healthy = PlateauSignals(
        daysObserved = 10,
        accuracyChange = 0.10,
        speedImprovement = 0.05,
        delayedRecallChange = 0.08,
        difficultyChanged = true,
        trainedImprovement = 0.10,
        untrainedImprovement = 0.08,
        weeksWithoutImprovement = 0,
    )

    @Test
    fun healthyProgression_isNone() {
        assertIs<PlateauStatus.None>(detector.detect(healthy))
    }

    @Test
    fun trainedUpUntrainedFlat_isFalseImprovement() {
        val s = healthy.copy(trainedImprovement = 0.25, untrainedImprovement = 0.01)
        assertIs<PlateauStatus.FalseImprovement>(detector.detect(s))
    }

    @Test
    fun stableEverythingForAWeek_isPlateau() {
        val s = healthy.copy(
            daysObserved = 8,
            accuracyChange = 0.01,
            speedImprovement = 0.0,
            delayedRecallChange = -0.02,
            difficultyChanged = false,
            trainedImprovement = 0.01,
            untrainedImprovement = 0.01,
        )
        assertIs<PlateauStatus.Plateau>(detector.detect(s))
    }

    @Test
    fun notPlateauIfDifficultyMoved() {
        val s = healthy.copy(
            daysObserved = 8,
            accuracyChange = 0.01,
            speedImprovement = 0.0,
            delayedRecallChange = -0.02,
            difficultyChanged = true,
            trainedImprovement = 0.01,
            untrainedImprovement = 0.01,
        )
        assertIs<PlateauStatus.None>(detector.detect(s))
    }

    @Test
    fun falseImprovement_recommendsStimulusRotation() {
        val s = healthy.copy(trainedImprovement = 0.25, untrainedImprovement = 0.01)
        val plan = planner.plan(detector.detect(s), s)
        assertEquals(InterventionProtocol.STIMULUS_ROTATION, plan.protocol)
    }

    @Test
    fun plateauWithLaggingDelayed_recommendsDelayManipulation() {
        val s = healthy.copy(
            daysObserved = 8, accuracyChange = 0.01, speedImprovement = 0.0,
            delayedRecallChange = -0.05, difficultyChanged = false,
            trainedImprovement = 0.01, untrainedImprovement = 0.01,
        )
        val plan = planner.plan(detector.detect(s), s)
        assertEquals(InterventionProtocol.DELAY_MANIPULATION, plan.protocol)
    }

    @Test
    fun goldenRule_overridesWithGoalReplanning() {
        val s = healthy.copy(weeksWithoutImprovement = 2)
        val plan = planner.plan(detector.detect(s), s)
        assertEquals(InterventionProtocol.GOAL_REPLANNING, plan.protocol)
        assertTrue(plan.goldenRuleTriggered)
    }

    @Test
    fun nineInterventionProtocolsExist() {
        assertEquals(9, InterventionProtocol.entries.size)
    }
}

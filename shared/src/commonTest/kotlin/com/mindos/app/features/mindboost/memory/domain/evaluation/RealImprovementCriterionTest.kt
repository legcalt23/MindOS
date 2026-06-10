package com.mindos.app.features.mindboost.memory.domain.evaluation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealImprovementCriterionTest {

    private val base = ImprovementEvidence(
        trainedDelta = 0.0,
        untrainedDelta = 0.0,
        immediateDelta = 0.0,
        delayedDelta = 0.0,
        effectSize = 0.0,
        sustainedWeeks = 0,
    )

    @Test
    fun trainedAndUntrainedImproved_isGenuinePattern() {
        assertEquals(
            ImprovementPattern.GENUINE_IMPROVEMENT,
            RealImprovementCriterion.classify(base.copy(trainedDelta = 0.20, untrainedDelta = 0.15)),
        )
    }

    @Test
    fun trainedOnlyImproved_isPracticeEffect() {
        assertEquals(
            ImprovementPattern.PRACTICE_EFFECT,
            RealImprovementCriterion.classify(base.copy(trainedDelta = 0.25, untrainedDelta = 0.01)),
        )
    }

    @Test
    fun neitherImproved_isStagnation() {
        assertEquals(ImprovementPattern.STAGNATION, RealImprovementCriterion.classify(base))
    }

    @Test
    fun untrainedBeatsTrained_isExceptionalTransfer() {
        assertEquals(
            ImprovementPattern.EXCEPTIONAL_TRANSFER,
            RealImprovementCriterion.classify(base.copy(trainedDelta = 0.05, untrainedDelta = 0.30)),
        )
    }

    @Test
    fun immediateUpDelayedDown_isPoorConsolidation() {
        assertEquals(
            ImprovementPattern.POOR_CONSOLIDATION,
            RealImprovementCriterion.classify(base.copy(immediateDelta = 0.20, delayedDelta = -0.20)),
        )
    }

    @Test
    fun genuineClaim_requiresUntrainedEffectAndSustain() {
        assertTrue(
            RealImprovementCriterion.isGenuine(base.copy(untrainedDelta = 0.12, effectSize = 0.4, sustainedWeeks = 2)),
        )
        assertFalse(
            RealImprovementCriterion.isGenuine(base.copy(untrainedDelta = 0.12, effectSize = 0.2, sustainedWeeks = 2)),
        )
        assertFalse(
            RealImprovementCriterion.isGenuine(base.copy(untrainedDelta = 0.12, effectSize = 0.4, sustainedWeeks = 1)),
        )
        assertFalse(
            RealImprovementCriterion.isGenuine(base.copy(untrainedDelta = 0.0, effectSize = 0.9, sustainedWeeks = 5)),
        )
    }
}

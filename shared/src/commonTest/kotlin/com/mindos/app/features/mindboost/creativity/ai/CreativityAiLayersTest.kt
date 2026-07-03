package com.mindos.app.features.mindboost.creativity.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CreativityAiLayersTest {

    @Test
    fun noBlockedLayerReportsItselfActive() {
        assertFalse(BlockedCreativityEvaluatorCascade.isActive)
        assertFalse(BlockedCreativityPrivacyGateway.isActive)
        assertFalse(BlockedCreativityCostBudgetGateway.isActive)
        assertFalse(BlockedCreativityMetricIntegrityLayer.isActive)
    }

    @Test
    fun everyBlockedLayerReturnsBlocked() {
        assertEquals(CreativityAiOutcome.Blocked, BlockedCreativityEvaluatorCascade.evaluate("idea"))
        assertEquals(CreativityAiOutcome.Blocked, BlockedCreativityPrivacyGateway.redactForEvaluation("idea"))
        assertEquals(CreativityAiOutcome.Blocked, BlockedCreativityCostBudgetGateway.reserveBudget("session-1"))
        assertEquals(CreativityAiOutcome.Blocked, BlockedCreativityMetricIntegrityLayer.scrutinize("idea"))
    }

    @Test
    fun blockedPrivacyGatewayNeverEchoesRawInput() {
        // A prepared boundary must not become a leak channel: the outcome carries no text.
        val sensitive = "contact me at user@example.com / +34 600 000 000"
        val outcome = BlockedCreativityPrivacyGateway.redactForEvaluation(sensitive)
        assertEquals(CreativityAiOutcome.Blocked, outcome)
    }
}

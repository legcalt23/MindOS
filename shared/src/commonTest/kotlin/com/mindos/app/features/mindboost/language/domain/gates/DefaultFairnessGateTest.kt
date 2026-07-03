package com.mindos.app.features.mindboost.language.domain.gates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the §5.3/§8.6 fairness policy: escalation by DIF magnitude, mandatory uncertainty copy for
 * unvalidated variants or tiny samples, and retirement at high DIF.
 */
class DefaultFairnessGateTest {

    private val gate = DefaultFairnessGate()

    @Test
    fun cleanItemPassesWithNoAction() {
        val a = gate.assess(FairnessSignals(variantValidated = true, difLogits = 0.1, sampleSize = 200))
        assertEquals(FairnessAction.NONE, a.action)
        assertFalse(a.flagged)
        assertFalse(a.requiresUncertaintyCopy)
    }

    @Test
    fun unvalidatedVariantRequiresUncertaintyCopyAndMonitors() {
        val a = gate.assess(FairnessSignals(variantValidated = false, difLogits = 0.1, sampleSize = 200))
        assertEquals(FairnessAction.MONITOR, a.action)
        assertTrue(a.flagged)
        assertTrue(a.requiresUncertaintyCopy)
    }

    @Test
    fun highDifRetiresForGroup() {
        val a = gate.assess(FairnessSignals(variantValidated = true, difLogits = 0.6, sampleSize = 200))
        assertEquals(FairnessAction.RETIRE_FOR_GROUP, a.action)
        assertTrue(a.flagged)
    }

    @Test
    fun moderateDifTriggersLocalReview() {
        val a = gate.assess(FairnessSignals(variantValidated = true, difLogits = -0.4, sampleSize = 200))
        // Negative DIF uses magnitude.
        assertEquals(FairnessAction.REVIEW_LOCAL, a.action)
        assertTrue(a.flagged)
    }

    @Test
    fun borderlineDifOnlyMonitors() {
        val a = gate.assess(FairnessSignals(variantValidated = true, difLogits = 0.3, sampleSize = 200))
        assertEquals(FairnessAction.MONITOR, a.action)
        assertFalse(a.flagged) // monitor on a validated variant is not a hard flag
    }

    @Test
    fun userDisputeEscalatesToReview() {
        val a = gate.assess(
            FairnessSignals(variantValidated = true, difLogits = 0.0, sampleSize = 200, userDispute = true),
        )
        assertEquals(FairnessAction.REVIEW_LOCAL, a.action)
    }

    @Test
    fun smallSampleRequiresUncertaintyCopy() {
        val a = gate.assess(FairnessSignals(variantValidated = true, difLogits = 0.0, sampleSize = 5))
        assertTrue(a.requiresUncertaintyCopy)
        assertEquals(FairnessAction.NONE, a.action)
    }
}

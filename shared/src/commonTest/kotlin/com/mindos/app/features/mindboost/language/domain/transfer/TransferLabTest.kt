package com.mindos.app.features.mindboost.language.domain.transfer

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the Transfer Lab (§10): the transfer-level ladder, the functional mission contract
 * invariants, and the near-transfer verifier — which only counts unseen, non-confounded material.
 */
class TransferLabTest {

    private val verifier = DefaultNearTransferVerifier()
    private val config = NearTransferConfig()

    @Test
    fun transferLevelClassifiesSources() {
        assertEquals(TransferLevel.T1_SURFACE_VARIATION, TransferLevel.classify(BankType.TRAINING))
        assertEquals(TransferLevel.T2_NEAR, TransferLevel.classify(BankType.PARALLEL_FORM))
        assertEquals(TransferLevel.T2_NEAR, TransferLevel.classify(BankType.EVALUATION))
        assertEquals(TransferLevel.T3_FUNCTIONAL, TransferLevel.classify(BankType.TRANSFER_LAB))
        assertEquals(TransferLevel.T4_PERSONAL, TransferLevel.classify(BankType.EVALUATION, isOwnMaterial = true))
        assertEquals(TransferLevel.T5_MAINTAINED, TransferLevel.classify(BankType.RETENTION))
        assertEquals(
            TransferLevel.T6_MULTIDOMAIN,
            TransferLevel.classify(BankType.TRANSFER_LAB, isMultiDomain = true),
        )
        assertEquals(TransferLevel.T0_REPETITION, TransferLevel.classify(BankType.CALIBRATION))
    }

    @Test
    fun transferLevelIsStrictlyOrdered() {
        assertTrue(TransferLevel.T3_FUNCTIONAL.isAtLeast(TransferLevel.T2_NEAR))
        assertFalse(TransferLevel.T1_SURFACE_VARIATION.isAtLeast(TransferLevel.T2_NEAR))
    }

    @Test
    fun transferMissionRejectsBlankIdAndEmptyTraits() {
        assertFailsWith<IllegalArgumentException> {
            TransferMission("", TransferProfile.FOUNDER, "g", "brief", listOf(TraitCode.SUM), "rub")
        }
        assertFailsWith<IllegalArgumentException> {
            TransferMission("m1", TransferProfile.FOUNDER, "g", "brief", emptyList(), "rub")
        }
    }

    private fun parallelTrial(correct: Double, allowed: Boolean = true) =
        TransferTrial(BankType.PARALLEL_FORM, Proportion(correct), isStrongUpdateAllowed = allowed)

    @Test
    fun nearTransferDemonstratedOnUnseenMaterial() {
        val verdict = verifier.verify(TraitCode.INF, List(3) { parallelTrial(1.0) }, config)
        assertTrue(verdict.demonstrated)
        assertEquals(TransferLevel.T2_NEAR, verdict.level)
        assertEquals(3, verdict.trialsConsidered)
    }

    @Test
    fun confoundedTrialsAreExcludedAndBlockTransfer() {
        val verdict = verifier.verify(TraitCode.INF, List(3) { parallelTrial(1.0, allowed = false) }, config)
        assertFalse(verdict.demonstrated)
        assertEquals(0, verdict.trialsConsidered)
    }

    @Test
    fun trainingOnlyMaterialIsNotNearTransfer() {
        val trials = List(4) { TransferTrial(BankType.TRAINING, Proportion.ONE, isStrongUpdateAllowed = true) }
        val verdict = verifier.verify(TraitCode.INF, trials, config)
        assertFalse(verdict.demonstrated)
    }

    @Test
    fun belowMinTrialsIsNotDemonstrated() {
        val verdict = verifier.verify(TraitCode.INF, List(2) { parallelTrial(1.0) }, config)
        assertFalse(verdict.demonstrated)
    }

    @Test
    fun belowAccuracyIsNotDemonstrated() {
        val verdict = verifier.verify(TraitCode.INF, List(3) { parallelTrial(0.3) }, config)
        assertFalse(verdict.demonstrated)
        assertEquals(0.3, verdict.accuracy?.value)
    }

    @Test
    fun verdictLevelReflectsHighestEligibleTrial() {
        val trials = listOf(
            parallelTrial(1.0),
            parallelTrial(1.0),
            TransferTrial(BankType.TRANSFER_LAB, Proportion.ONE, isStrongUpdateAllowed = true),
        )
        val verdict = verifier.verify(TraitCode.INF, trials, config)
        assertTrue(verdict.demonstrated)
        assertEquals(TransferLevel.T3_FUNCTIONAL, verdict.level)
    }
}

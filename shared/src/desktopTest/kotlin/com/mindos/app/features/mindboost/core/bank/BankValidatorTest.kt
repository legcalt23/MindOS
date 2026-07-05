package com.mindos.app.features.mindboost.core.bank

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** INFRA-04 · the bank gate logic: small/unbalanced/no-holdout/low-inter-rater banks are rejected. */
class BankValidatorTest {

    private data class TestItem(
        override val id: String,
        override val partition: ItemPartition = ItemPartition.TRAINING,
        override val correctAnswerPosition: Int? = null,
        override val interRaterAgreement: Double? = null,
        override val difficultyLevel: Int = 3,
        override val constructTag: String = "tag",
        override val priorKnowledge: Boolean = false,
        override val culturalLoad: Double? = null,
    ) : BankItem

    private data class TestBank(
        override val items: List<BankItem>,
        override val requirements: BankRequirements,
        override val bankId: String = "test",
        override val profileUpdating: Boolean = true,
        override val kind: BankKind = BankKind.STATIC_FIXED,
    ) : ItemBank

    private val choiceReq = BankRequirements(
        minBankSize = 40,
        requiresHoldout = true,
        requiresInterRater = false,
        choiceCount = 2,
        maxPositionChiSquare = 3.84,
    )

    private val judgmentReq = BankRequirements(
        minBankSize = 20,
        requiresHoldout = true,
        requiresInterRater = true,
        choiceCount = null,
        maxPositionChiSquare = 3.84,
    )

    private fun items(
        n: Int,
        holdout: Int = 0,
        position: ((Int) -> Int?)? = null,
        interRater: ((Int) -> Double?)? = null,
    ): List<BankItem> = (0 until n).map { i ->
        TestItem(
            id = "i$i",
            partition = if (i < holdout) ItemPartition.HOLDOUT else ItemPartition.TRAINING,
            correctAnswerPosition = position?.invoke(i),
            interRaterAgreement = interRater?.invoke(i),
        )
    }

    @Test
    fun balancedChoiceBank_withHoldout_isValid() {
        val bank = TestBank(items(40, holdout = 8, position = { it % 2 }), choiceReq)
        assertTrue(BankValidator.validate(bank).isValid)
    }

    @Test
    fun smallBank_isTooSmall() {
        val bank = TestBank(items(10, holdout = 2, position = { it % 2 }), choiceReq)
        val result = BankValidator.validate(bank)
        assertFalse(result.isValid)
        assertTrue(result.violations.any { it is BankViolation.TooSmall })
    }

    @Test
    fun bankWithoutHoldout_isRejected() {
        val bank = TestBank(items(40, holdout = 0, position = { it % 2 }), choiceReq)
        val result = BankValidator.validate(bank)
        assertTrue(result.violations.any { it is BankViolation.NoHoldoutServed })
    }

    @Test
    fun alwaysSameCorrectPosition_isImbalanced() {
        // The "correct answer is 'b' in 6/6" exploit, at scale.
        val bank = TestBank(items(40, holdout = 8, position = { 1 }), choiceReq)
        val result = BankValidator.validate(bank)
        assertTrue(result.violations.any { it is BankViolation.PositionImbalance })
    }

    @Test
    fun judgmentBank_excludesLowInterRater_butStaysValidWhenEnoughRemain() {
        // 25 items ≥0.80 (incl. the 8 holdout), 15 below → effective 25 ≥ minSize 20 → valid.
        val bank = TestBank(
            items(40, holdout = 8, interRater = { if (it < 25) 0.9 else 0.5 }),
            judgmentReq,
        )
        assertEquals(25, BankValidator.effectiveItems(bank).size)
        assertTrue(BankValidator.validate(bank).isValid)
    }

    @Test
    fun judgmentBank_withTooFewHighInterRater_isTooSmall() {
        val bank = TestBank(
            items(40, holdout = 5, interRater = { if (it < 10) 0.9 else 0.5 }),
            judgmentReq,
        )
        val result = BankValidator.validate(bank)
        assertEquals(10, BankValidator.effectiveItems(bank).size)
        assertTrue(result.violations.any { it is BankViolation.TooSmall })
    }

    @Test
    fun chiSquareUniform_isZeroWhenBalanced_andHighWhenSkewed() {
        assertEquals(0.0, BankValidator.chiSquareUniform(listOf(0, 1, 0, 1), choiceCount = 2))
        assertTrue(BankValidator.chiSquareUniform(List(40) { 1 }, choiceCount = 2) > 3.84)
    }
}

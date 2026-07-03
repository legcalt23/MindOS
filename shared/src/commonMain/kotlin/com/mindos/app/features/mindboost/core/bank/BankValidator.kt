@file:Suppress("MagicNumber") // 0.0 baselines in chi-square / inter-rater fallback are domain neutrals.

package com.mindos.app.features.mindboost.core.bank

/** A specific way a profile-updating bank fails the INFRA-04 contract. */
sealed interface BankViolation {
    data class TooSmall(val effectiveSize: Int, val minSize: Int) : BankViolation
    data object NoHoldoutServed : BankViolation
    data class PositionImbalance(val chiSquare: Double, val threshold: Double) : BankViolation
}

/** Result of validating a bank. [isValid] iff no violations. */
data class BankValidationResult(val violations: List<BankViolation>) {
    val isValid: Boolean get() = violations.isEmpty()
}

/**
 * INFRA-04 · Pure bank validator. A bank backing a profile-updating exercise must, AFTER excluding
 * judgment items below inter-rater: meet [BankRequirements.minBankSize]; serve a HOLDOUT partition;
 * and (for choice banks) keep the correct-answer position distribution balanced (chi-square ≤ threshold,
 * which kills "always answer b" exploits). Low-inter-rater items are simply excluded from the effective
 * bank — a judgment bank with too few high-agreement items then fails as [BankViolation.TooSmall].
 */
object BankValidator {

    fun validate(bank: ItemBank): BankValidationResult {
        val effective = effectiveItems(bank)
        val req = bank.requirements
        val violations = buildList {
            if (effective.size < req.minBankSize) {
                add(BankViolation.TooSmall(effective.size, req.minBankSize))
            }
            if (req.requiresHoldout && effective.none { it.partition == ItemPartition.HOLDOUT }) {
                add(BankViolation.NoHoldoutServed)
            }
            positionImbalance(effective, req)?.let(::add)
        }
        return BankValidationResult(violations)
    }

    /** Items that count: all of them, or (for judgment banks) those clearing inter-rater. */
    fun effectiveItems(bank: ItemBank): List<BankItem> =
        if (!bank.requirements.requiresInterRater) {
            bank.items
        } else {
            bank.items.filter { (it.interRaterAgreement ?: 0.0) >= bank.requirements.minInterRater }
        }

    private fun positionImbalance(items: List<BankItem>, req: BankRequirements): BankViolation.PositionImbalance? {
        val choiceCount = req.choiceCount ?: return null
        val positions = items.mapNotNull { it.correctAnswerPosition }
        val chi = chiSquareUniform(positions, choiceCount)
        return if (positions.isNotEmpty() && chi > req.maxPositionChiSquare) {
            BankViolation.PositionImbalance(chi, req.maxPositionChiSquare)
        } else {
            null
        }
    }

    /** Chi-square goodness-of-fit of [positions] against a uniform distribution over [choiceCount] slots. */
    fun chiSquareUniform(positions: List<Int>, choiceCount: Int): Double {
        if (choiceCount <= 0 || positions.isEmpty()) return 0.0
        val expected = positions.size.toDouble() / choiceCount
        var chi = 0.0
        for (slot in 0 until choiceCount) {
            val diff = positions.count { it == slot }.toDouble() - expected
            chi += diff * diff / expected
        }
        return chi
    }
}

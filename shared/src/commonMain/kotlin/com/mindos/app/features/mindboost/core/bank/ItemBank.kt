package com.mindos.app.features.mindboost.core.bank

/** Train/eval split of an item. HOLDOUT/UNTRAINED must be SERVED in evaluation, not merely reserved. */
enum class ItemPartition { TRAINING, HOLDOUT, UNTRAINED }

/**
 * INFRA-04 · Metadata every bank item carries so a bank can be validated and rotated honestly.
 * [correctAnswerPosition] is the 0-based correct option index for CHOICE banks (null otherwise);
 * [interRaterAgreement] is required (≥ [BankRequirements.minInterRater]) for JUDGMENT banks (null
 * otherwise); [culturalLoad] is 0..1 for textual banks (null otherwise).
 */
interface BankItem {
    val id: String
    val difficultyLevel: Int
    val constructTag: String
    val partition: ItemPartition
    val correctAnswerPosition: Int?
    val interRaterAgreement: Double?
    val priorKnowledge: Boolean
    val culturalLoad: Double?
}

/** What a bank must satisfy to back a profile-updating exercise (the build gate checks these). */
data class BankRequirements(
    val minBankSize: Int,
    val requiresHoldout: Boolean,
    val requiresInterRater: Boolean,
    val choiceCount: Int?,
    val maxPositionChiSquare: Double,
    val minInterRater: Double = MIN_INTER_RATER,
) {
    companion object {
        /** Minimum inter-rater agreement for a judgment item to count toward the effective bank. */
        const val MIN_INTER_RATER: Double = 0.80
    }
}

/** A content bank. [profileUpdating] banks are subject to the INFRA-04 build gate. */
interface ItemBank {
    val bankId: String
    val items: List<BankItem>
    val requirements: BankRequirements
    val profileUpdating: Boolean
}

package com.mindos.app.features.mindboost.core.bank

/** Test-only bank builders for exercising the [BankIntegrityAuthority]-derived paths (B-1). */
object BankTestFixtures {

    private data class Item(
        override val id: String,
        override val partition: ItemPartition,
        override val difficultyLevel: Int = 3,
        override val constructTag: String = "tag",
        override val correctAnswerPosition: Int? = null,
        override val interRaterAgreement: Double? = null,
        override val priorKnowledge: Boolean = false,
        override val culturalLoad: Double? = null,
    ) : BankItem

    private data class Bank(
        override val bankId: String,
        override val items: List<BankItem>,
        override val requirements: BankRequirements,
        override val kind: BankKind,
        override val profileUpdating: Boolean = true,
    ) : ItemBank

    private val nonChoiceReq = BankRequirements(
        minBankSize = 20,
        requiresHoldout = true,
        requiresInterRater = false,
        choiceCount = null,
        maxPositionChiSquare = 3.84,
    )

    /** A REGISTERED + VALID bank of the given [kind]: 20 items, 4 holdout served, no choice-position check. */
    fun validBank(bankId: String, kind: BankKind): ItemBank = Bank(
        bankId = bankId,
        items = (0 until 20).map { Item("$bankId-$it", if (it < 4) ItemPartition.HOLDOUT else ItemPartition.TRAINING) },
        requirements = nonChoiceReq,
        kind = kind,
    )

    /** A REGISTERED but INVALID bank (too small for [nonChoiceReq]) of the given [kind]. */
    fun invalidBank(bankId: String, kind: BankKind): ItemBank = Bank(
        bankId = bankId,
        items = (0 until 3).map { Item("$bankId-$it", ItemPartition.TRAINING) },
        requirements = nonChoiceReq,
        kind = kind,
    )
}

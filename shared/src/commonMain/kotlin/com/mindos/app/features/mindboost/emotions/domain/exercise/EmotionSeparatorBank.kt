package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.core.bank.BankIntegrityAuthority
import com.mindos.app.features.mindboost.core.bank.BankItem
import com.mindos.app.features.mindboost.core.bank.BankKind
import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.bank.BankRequirements
import com.mindos.app.features.mindboost.core.bank.ItemBank
import com.mindos.app.features.mindboost.core.bank.ItemPartition
import com.mindos.app.features.mindboost.core.scoring.BankIntegrity

/**
 * Ola 4 · P1 — the EM3/EM10 classification bank expressed through the repo's ONE bank contract
 * (INFRA-04 `core/bank`) instead of a private, emotions-only notion of "eval item".
 *
 * Why this exists: `EmotionContent.SeparatorItem.isEval` reserved a held-out partition from day one, but
 * nothing ever SERVED it, and nothing ever checked whether the partition was big enough to certify
 * anything. `ItemPartition` states the rule this bank must obey — a held-out partition must be served in
 * evaluation, not merely reserved — and `BankValidator` is the existing gate that measures it. Routing the
 * bank through them makes the bank's maturity a DERIVED fact instead of a claim in a KDoc.
 *
 * Deliberately NOT registered in `ProfileUpdatingBanks`: Emociones does not update the cognitive profile,
 * and registering a bank is an explicit product+efficacy decision, never a side effect. The consequence is
 * mechanical and intended — [integrity] resolves through [BankIntegrityAuthority] against the production
 * registry, finds nothing, and fails closed to [BankIntegrity.SMALL_UNVALIDATED], which
 * `blocksProfileEligibility()`. That is the honest state of this content today.
 *
 * Declared, unmeasured gaps (they are why the bank cannot back a profile claim, and they are recorded here
 * rather than papered over with invented numbers):
 *  - [BankItem.interRaterAgreement] is `null` — no second rater has ever labelled these phrases, so the
 *    ground truth is single-author. [BankRequirements.requiresInterRater] is therefore `false`: the bank
 *    does not pretend to have cleared a bar it never took.
 *  - [BankItem.culturalLoad] is `null` — unmeasured. A fabricated 0.0 would read as "verified culture-free".
 */
internal object EmotionSeparatorBank : ItemBank {

    const val BANK_ID: String = "emotions_separator_v1"

    /**
     * Target size for the classification bank.
     *
     * Ola 2 · R3 · P2: this used to be a local `80` taken from an older test spec, while the R3 design asked
     * for ≥300 and the bank actually held 40 — three numbers for one requirement, none of which could be wrong
     * without the other two also being wrong. It now READS the factory's floor, so the bank, the validator and
     * the design cannot drift apart again.
     */
    const val DESIGN_MIN_BANK_SIZE: Int = SeparatorItemFactory.DESIGN_MIN_BANK_SIZE

    /** The four fixed category targets of the EM3/EM10 renderer, i.e. the answer positions to keep balanced. */
    const val CATEGORY_COUNT: Int = 4

    /** Chi-square critical value at df = [CATEGORY_COUNT] - 1 and alpha = 0.05; kills an "always tap A" exploit. */
    private const val POSITION_CHI_SQUARE_LIMIT: Double = 7.815

    override val bankId: String = BANK_ID

    override val kind: BankKind = BankKind.STATIC_FIXED

    /** Emociones contributes no cognitive-profile score, so this bank is not subject to the profile gate. */
    override val profileUpdating: Boolean = false

    override val requirements: BankRequirements = BankRequirements(
        minBankSize = DESIGN_MIN_BANK_SIZE,
        requiresHoldout = true,
        requiresInterRater = false,
        choiceCount = CATEGORY_COUNT,
        maxPositionChiSquare = POSITION_CHI_SQUARE_LIMIT,
    )

    override val items: List<BankItem> = SeparatorItemFactory.ALL.map(::SeparatorBankItem)

    /** Items reserved for evaluation. The probe draws ONLY from here; training generators exclude them. */
    val holdoutItems: List<EmotionContent.SeparatorItem> = SeparatorItemFactory.ALL.filter { it.isEval }

    /**
     * Runtime integrity, DERIVED from the production registry — never a literal at a call site (the bypass
     * INFRA-04 exists to close). Unregistered today, so this is [BankIntegrity.SMALL_UNVALIDATED].
     */
    val integrity: BankIntegrity get() = BankIntegrityAuthority.integrityFor(BankRef(BANK_ID))
}

/**
 * A [BankItem] view of an authored phrase. The id IS the phrase: exposure in this module is already keyed by
 * phrase ([EmotionExposureGuard]), and a positional id would silently re-point every user's exposure history
 * the first time someone inserts an item mid-list. The phrase is authored, non-personal content.
 */
private class SeparatorBankItem(item: EmotionContent.SeparatorItem) : BankItem {
    override val id: String = item.phrase
    override val difficultyLevel: Int = item.ambiguity
    override val constructTag: String = "emotion_thought_separation"
    override val partition: ItemPartition =
        if (item.isEval) ItemPartition.HOLDOUT else ItemPartition.TRAINING
    override val correctAnswerPosition: Int = item.category.ordinal
    override val interRaterAgreement: Double? = null
    override val priorKnowledge: Boolean = false
    override val culturalLoad: Double? = null
}
